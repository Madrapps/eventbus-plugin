package com.madrapps.eventbus

import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.table.JBTable
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.UIUtil
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Insets
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer
import kotlin.math.max


internal fun showSubscribeUsages(usages: List<Usage>, relativePoint: RelativePoint) {
    val columnInfo = arrayOf(
        MyColumnInfo { "" },
        MyColumnInfo { it.toUElement()?.getParentOfTypeCallExpression()?.sourcePsi?.text ?: "" },
        MyColumnInfo { ((it as? UsageInfo2UsageAdapter)?.line)?.plus(1)?.toString() ?: "" },
        MyColumnInfo { it.getType<UClass>()?.name ?: "" })
    showTablePopUp(usages, columnInfo).createPopup().show(relativePoint)
}

internal fun showPostUsages(usages: List<Usage>, relativePoint: RelativePoint) {
    val columnInfo = arrayOf(
        MyColumnInfo { "" },
        MyColumnInfo { it.getType<UMethod>()?.name ?: "" },
        MyColumnInfo { (it as? UsageInfo2UsageAdapter)?.line?.plus(1)?.toString() ?: "" },
        MyColumnInfo { it.getType<UClass>()?.name ?: "" })
    showTablePopUp(usages, columnInfo).createPopup().show(relativePoint)
}

private fun showTablePopUp(usages: List<Usage>, columnInfos: Array<MyColumnInfo>): PopupChooserBuilder<*> {
    val table = JBTable()
    ScrollingUtil.installActions(table)
    table.model = ListTableModel(columnInfos, usages)
    table.tableHeader = null
    table.showHorizontalLines = false
    table.showVerticalLines = false
    table.intercellSpacing = Dimension(0, 0)
    table.rowHeight = PlatformIcons.METHOD_ICON.iconHeight + 2
    table.setShowGrid(false)
    table.columnModel.getColumn(0).cellRenderer = CellRenderer()
    resizeColumnWidth(table)
    table.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN

    val rightRenderer = DefaultTableCellRenderer()
    rightRenderer.horizontalAlignment = JLabel.RIGHT
    table.columnModel.getColumn(3).cellRenderer = rightRenderer

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
        val panel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 0))

        val bg = UIUtil.getListSelectionBackground()
        val fg = UIUtil.getListSelectionForeground()
        panel.background = if (isSelected) bg else list.background
        panel.foreground = if (isSelected) fg else list.foreground

        val textChunks = SimpleColoredComponent()
        textChunks.ipad = Insets(0, 2, 0, 0)
        textChunks.icon = PlatformIcons.METHOD_ICON
        textChunks.border = null
        textChunks.size = Dimension(PlatformIcons.METHOD_ICON.iconWidth, PlatformIcons.METHOD_ICON.iconHeight)
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
