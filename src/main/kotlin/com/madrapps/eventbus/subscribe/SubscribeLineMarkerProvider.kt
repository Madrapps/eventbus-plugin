package com.madrapps.eventbus.subscribe

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.RIGHT
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.table.JBTable
import com.intellij.usageView.UsageInfo
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.madrapps.eventbus.post.isPostMethod
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.uast.*
import org.jetbrains.uast.UastVisibility.PUBLIC
import java.awt.Dimension
import java.awt.event.MouseEvent
import javax.swing.JTable
import kotlin.math.max


class SubscribeLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val uElement = element.toUElement()
        if (uElement is UMethod) {
            val annotation = uElement.annotations.find { it.qualifiedName == "org.greenrobot.eventbus.Subscribe" }
            if (annotation != null) {
                if (uElement.visibility == PUBLIC && uElement.uastParameters.size == 1) {
                    val psiElement = uElement.uastAnchor?.sourcePsi
                    if (psiElement != null) {
                        return SubscribeLineMarkerInfo(psiElement, "post for ---", uElement)
                    }
                }
            }
        }
        return null
    }
}

private class SubscribeLineMarkerInfo(
    psiElement: PsiElement,
    private val message: String,
    private val uElement: UMethod
) : LineMarkerInfo<PsiElement>(
    psiElement,
    psiElement.textRange,
    AllIcons.General.ArrowLeft,
    { message },
    null,
    RIGHT
) {

    override fun createGutterRenderer(): GutterIconRenderer? {
        return object : LineMarkerGutterIconRenderer<PsiElement>(this) {
            override fun getClickAction(): AnAction? {
                return object : AnAction(message) {
                    override fun actionPerformed(e: AnActionEvent) {
                        val relativePoint = RelativePoint(e.inputEvent as MouseEvent)

                        val elementToSearch =
                            (uElement.uastParameters[0].type as PsiClassReferenceType).reference.resolve()
                        if (elementToSearch != null) {
                            val collection = search(elementToSearch)

                            val usages = collection.filter {
                                isPost(it)
                            }.map(::UsageInfo2UsageAdapter)

                            showUsages(usages, relativePoint)
                        }
                    }
                }
            }
        }
    }

    private fun showUsages(
        usages: List<Usage>,
        relativePoint: RelativePoint
    ) {
        val baseListPopupStep = object : BaseListPopupStep<Usage>("Usages", usages) {
            override fun onChosen(selectedValue: Usage?, finalChoice: Boolean): PopupStep<*>? {
                selectedValue?.navigate(true)
                return super.onChosen(selectedValue, finalChoice)
            }

            override fun getTextFor(value: Usage): String {
                val sourcePsi = psiElement(value)
                if (sourcePsi != null) {
                    return "${sourcePsi.getLineNumber()}  ${sourcePsi.text}  in ${sourcePsi.containingFile.name}"
                }
                return value.toString()
            }
        }
//        val createListPopup = JBPopupFactory.getInstance().createListPopup(baseListPopupStep, 10)
//        createListPopup.show(relativePoint)


        showTablePopUp(usages).createPopup().show(relativePoint)


//
//        val toArray = usages.toArray(arrayOfNulls<Usage>(usages.size))
//        val usageViewPresentation = UsageViewPresentation()
//        usageViewPresentation.tabText = "Type"
//        usageViewPresentation.isOpenInNewTab = false
//        usageViewPresentation.isCodeUsages = false
//        usageViewPresentation.isUsageTypeFilteringAvailable = false
//        usageViewPresentation.codeUsagesString = "codeUsagesString"
//        usageViewPresentation.contextText = "contextText"
//        usageViewPresentation.nonCodeUsagesString = "nonCodeUsagesString"
//        usageViewPresentation.scopeText = "scopeTest"
//        usageViewPresentation.targetsNodeText = "targetsNodeText"
//        val instance = UsageViewManager.getInstance(element?.project!!)
//        instance.showUsages(
//            UsageTarget.EMPTY_ARRAY,
//            toArray,
//            usageViewPresentation
//        )
    }


    private fun isPost(usageInfo: UsageInfo): Boolean {
        val uElement = usageInfo.element.toUElement()
        if (uElement != null) {
            if (uElement.getParentOfType<UImportStatement>() == null) {
                val parent = uElement.getParentOfType<UQualifiedReferenceExpression>()
                if (parent != null) {
                    return isPostMethod(parent)
                }
            }
        }
        return false
    }
}

private fun psiElement(value: Usage): PsiElement? {
    val uElement = (value as? UsageInfo2UsageAdapter)?.usageInfo?.element?.toUElement()
    val sourcePsi = uElement?.getParentOfType<UQualifiedReferenceExpression>()?.sourcePsi
    return sourcePsi
}

fun search(element: PsiElement): Collection<UsageInfo> {
    val references = ReferencesSearch.search(element).findAll()
    return references.map(::UsageInfo)
}

private fun showTablePopUp(usages: List<Usage>): PopupChooserBuilder<*> {
    val table = MyTable()
    ScrollingUtil.installActions(table)
    val columnInfos = arrayOf(
        MyColumnInfo {
            ""
        },
        MyColumnInfo {
            psiElement(it)?.getLineNumber()?.toString() ?: ""
        }, MyColumnInfo {
            psiElement(it)?.text ?: ""
        }, MyColumnInfo {
            psiElement(it)?.containingFile?.name ?: ""
        })
    table.model = ListTableModel(columnInfos, usages)
    table.tableHeader = null
    table.showHorizontalLines = false
    table.showVerticalLines = false
    table.intercellSpacing = Dimension(0, 0)
    table.setShowGrid(false)
    table.columnModel.getColumn(0).cellRenderer = CellRenderer()
    resizeColumnWidth(table)
    table.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN


    return JBPopupFactory.getInstance().createPopupChooserBuilder(table)
        .setTitle("Find Usages")
        .setMovable(true)
        .setResizable(true)
        .setItemChoosenCallback {
            val selectedRow = table.selectedRow

            val valueAt = (table.model as? ListTableModel<Usage>)?.getItem(selectedRow)
            valueAt?.navigate(true)
        }
}

class MyTable : JBTable() {

}

class MyColumnInfo(val value: (Usage) -> String) : ColumnInfo<Usage, String>("") {
    override fun valueOf(item: Usage?): String? {
        if (item != null) {
            return value(item)
        }
        return item?.toString()
    }
}

fun resizeColumnWidth(table: JTable) {
    val columnModel = table.columnModel
    for (column in 0 until table.columnCount) {
        var width = 16
        for (row in 0 until table.rowCount) {
            val renderer = table.getCellRenderer(row, column)
            val comp = table.prepareRenderer(renderer, row, column)
            width = max(comp.preferredSize.width + 1, width)
        }
        if (width > 300)
            width = 300
        columnModel.getColumn(column).preferredWidth = width
    }
}