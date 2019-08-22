package com.madrapps.eventbus

import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.table.JBTable
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.UIUtil
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getIoFile
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Insets
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.TableCellRenderer
import kotlin.math.max


internal fun showSubscribeUsages(usages: List<Usage>, relativePoint: RelativePoint) {
    val columnInfo = arrayOf(
        MyColumnInfo { "" },
        MyColumnInfo { it.toUElement()?.getParentOfTypeCallExpression()?.sourcePsi?.text ?: "" },
        MyColumnInfo { ((it as? UsageInfo2UsageAdapter)?.line)?.plus(1)?.toString() ?: "" },
        MyColumnInfo(::className)
    )
    showTablePopUp(usages, columnInfo).createPopup().show(relativePoint)
}

internal fun showPostUsages(usages: List<Usage>, relativePoint: RelativePoint) {
    val columnInfo = arrayOf(
        MyColumnInfo { "" },
        MyColumnInfo { it.getType<UMethod>()?.name ?: "" },
        MyColumnInfo { (it as? UsageInfo2UsageAdapter)?.line?.plus(1)?.toString() ?: "" },
        MyColumnInfo(::className)
    )
    showTablePopUp(usages, columnInfo).createPopup().show(relativePoint)
}

private fun className(it: Usage): String {
    val className = it.getType<UClass>()?.name
    val fileName = it.getType<UFile>()?.getIoFile()?.nameWithoutExtension
    return className ?: fileName ?: ""
}

private fun showTablePopUp(usages: List<Usage>, columnInfos: Array<MyColumnInfo>): PopupChooserBuilder<*> {
    val table = JBTable()
    ScrollingUtil.installActions(table)
    table.model = ListTableModel(columnInfos, usages)
    table.tableHeader = null
    table.showHorizontalLines = false
    table.showVerticalLines = false
    table.intercellSpacing = Dimension(0, 0)
    table.setShowGrid(false)
    table.columnModel.getColumn(0).cellRenderer = CellRenderer()
    table.columnModel.getColumn(2).cellRenderer = CellRenderer()
    table.columnModel.getColumn(3).cellRenderer = CellRenderer()
    resizeColumnWidth(table)
    table.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN

    return JBPopupFactory.getInstance().createPopupChooserBuilder(table)
        .setTitle(getTitle(usages))
        .setMovable(true)
        .setResizable(true)
        .setItemChoosenCallback {
            val selectedRow = table.selectedRow
            @Suppress("UNCHECKED_CAST")
            val usage = (table.model as? ListTableModel<Usage>)?.getItem(selectedRow)
            usage?.navigate(true)
        }
}

private fun getTitle(usages: List<Usage>): String {
    return if (usages.isEmpty()) "No usages found" else "Find Usages"
}

private class CellRenderer : TableCellRenderer {

    override fun getTableCellRendererComponent(
        list: JTable,
        value: Any,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))

        val bg = if (isSelected) UIUtil.getListSelectionBackground() else list.background
        val fg = if (isSelected) UIUtil.getListSelectionForeground() else list.foreground
        panel.background = bg
        panel.foreground = fg

        val textChunks = SimpleColoredComponent()
        textChunks.ipad = Insets(0, 3, 0, 0)

        when (column) {
            0 -> {
                textChunks.icon = PlatformIcons.METHOD_ICON
                textChunks.border = null
                textChunks.size = Dimension(PlatformIcons.METHOD_ICON.iconWidth, PlatformIcons.METHOD_ICON.iconHeight)
            }
            2 -> {
                val attributes = SimpleTextAttributes(bg, fg, fg, SimpleTextAttributes.STYLE_ITALIC)
                textChunks.append(value.toString(), attributes)
            }
            3 -> {
                textChunks.append(value.toString())
                (panel.layout as FlowLayout).alignment = FlowLayout.RIGHT
            }
        }

        panel.add(textChunks)
        return panel
    }
}

private fun resizeColumnWidth(table: JTable) {
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

private class MyColumnInfo(val value: (Usage) -> String) : ColumnInfo<Usage, String>("") {
    override fun valueOf(item: Usage?): String? {
        if (item != null) {
            return value(item)
        }
        return item?.toString()
    }
}


private fun showInFindUsages() {
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
