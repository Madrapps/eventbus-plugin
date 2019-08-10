package com.madrapps.eventbus.subscribe

import com.intellij.ui.SimpleColoredComponent
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Insets
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

internal class CellRenderer : TableCellRenderer {

    override fun getTableCellRendererComponent(
        list: JTable,
        value: Any,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))

        val bg = UIUtil.getListSelectionBackground()
        val fg = UIUtil.getListSelectionForeground()
        panel.background = if (isSelected) bg else list.background
        panel.foreground = if (isSelected) fg else list.foreground

        val textChunks = SimpleColoredComponent()
        textChunks.ipad = Insets(0, 3, 0, 0)
        textChunks.icon = PlatformIcons.METHOD_ICON
        textChunks.border = null
        textChunks.size = Dimension(PlatformIcons.METHOD_ICON.iconWidth, PlatformIcons.METHOD_ICON.iconHeight)
        panel.add(textChunks)
        return panel
    }

}