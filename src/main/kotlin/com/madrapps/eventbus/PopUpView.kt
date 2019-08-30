package com.madrapps.eventbus

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.ui.breakpoints.BreakpointManager
import com.intellij.debugger.ui.breakpoints.BreakpointWithHighlighter
import com.intellij.icons.AllIcons
import com.intellij.icons.AllIcons.Toolwindows.ToolWindowFind
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces.USAGE_VIEW_TOOLBAR
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.IdeActions.ACTION_FIND_USAGES
import com.intellij.openapi.editor.Document
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.Cell
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES
import com.intellij.ui.SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.table.JBTable
import com.intellij.usages.*
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.UIUtil
import org.jetbrains.java.debugger.breakpoints.properties.JavaBreakpointProperties
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getIoFile
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Insets
import java.awt.event.InputEvent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.TableCellRenderer
import kotlin.math.max


internal fun showSubscribeUsages(usages: List<Usage>, relativePoint: RelativePoint) {
    val columnInfo = arrayOf(
        MyColumnInfo { it.toUElement()?.getParentOfTypeCallExpression()?.sourcePsi?.text ?: "" },
        MyColumnInfo { ((it as? UsageInfo2UsageAdapter)?.line)?.plus(1)?.toString() ?: "" },
        MyColumnInfo(::className)
    )
    showTablePopUp(usages, columnInfo).show(relativePoint)
}

internal fun showPostUsages(usages: List<Usage>, relativePoint: RelativePoint) {
    val columnInfo = arrayOf(
        MyColumnInfo { it.getType<UMethod>()?.name ?: "" },
        MyColumnInfo { (it as? UsageInfo2UsageAdapter)?.line?.plus(1)?.toString() ?: "" },
        MyColumnInfo(::className)
    )
    showTablePopUp(usages, columnInfo).show(relativePoint)
}

private fun className(it: Usage): String {
    val className = it.getType<UClass>()?.name
    val fileName = it.getType<UFile>()?.getIoFile()?.nameWithoutExtension
    return className ?: fileName ?: ""
}

private fun showTablePopUp(usages: List<Usage>, columnInfos: Array<MyColumnInfo>): JBPopup {
    var popUp: JBPopup? = null

    val table = JBTable().apply {
        ScrollingUtil.installActions(this)
        installSpeedSearch(this)
        model = ListTableModel(columnInfos, usages)
        tableHeader = null
        showHorizontalLines = false
        showVerticalLines = false
        intercellSpacing = Dimension(0, 0)
        setShowGrid(false)
        val cellRenderer = CellRenderer()
        columnModel.getColumn(0).cellRenderer = cellRenderer
        columnModel.getColumn(1).cellRenderer = cellRenderer
        columnModel.getColumn(2).cellRenderer = cellRenderer
        resizeColumnWidth(this)
        autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
    }

    val builder = JBPopupFactory.getInstance().createPopupChooserBuilder(table)
        .setTitle(getTitle(usages))
        .setMovable(true)
        .setResizable(true)
        .setItemChoosenCallback {
            val selectedRow = table.selectedRow
            @Suppress("UNCHECKED_CAST")
            val usage = (table.model as? ListTableModel<Usage>)?.getItem(selectedRow)
            usage?.navigate(true)
        }

    if (usages.isNotEmpty()) {
        val actionGroup = DefaultActionGroup()
        val closePopUp: (InputEvent) -> Unit = { popUp?.closeOk(it) }
        actionGroup.add(SetBreakpointAction(usages, closePopUp))
        actionGroup.add(RemoveBreakpointAction(usages, closePopUp))
        actionGroup.addSeparator()
        actionGroup.add(ShowUsagesAction(usages, closePopUp))
        val actionToolbar = ActionManager.getInstance().createActionToolbar(USAGE_VIEW_TOOLBAR, actionGroup, true)
        actionToolbar.setReservePlaceAutoPopupIcon(false)
        val toolBar = actionToolbar.component
        toolBar.isOpaque = false

        builder.setSettingButton(toolBar)
    }
    popUp = builder.createPopup()
    return popUp
}

private fun installSpeedSearch(table: JBTable) {
    TableSpeedSearch(table) { o: Any?, cell: Cell ->
        if (o != null && cell.column == 2) o.toString() else ""
    }
}

private fun getTitle(usages: List<Usage>) = if (usages.isEmpty()) "No usages found" else "Usages"

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
        val offset = 2
        textChunks.ipad = Insets(offset, offset, offset, 0)
        textChunks.size = Dimension(PlatformIcons.METHOD_ICON.iconWidth, PlatformIcons.METHOD_ICON.iconHeight)

        when (column) {
            0 -> {
                textChunks.icon = PlatformIcons.METHOD_ICON
                textChunks.ipad = Insets(offset, 0, offset, 15)
                val attributes = if (isSelected) REGULAR_ITALIC_ATTRIBUTES else REGULAR_ATTRIBUTES
                textChunks.append(value.toString(), attributes)
            }
            1 -> {
                val attributes = if (isSelected) REGULAR_ATTRIBUTES else REGULAR_ITALIC_ATTRIBUTES
                textChunks.ipad = Insets(offset, 0, offset, 15)
                textChunks.append(value.toString(), attributes)
            }
            2 -> {
                textChunks.append(value.toString())
                (panel.layout as FlowLayout).alignment = FlowLayout.RIGHT
            }
        }

        panel.add(textChunks)
        return panel
    }
}

private class ShowUsagesAction(
    private val usages: List<Usage>,
    private val closePopUp: (InputEvent) -> Unit
) : AnAction(
    "Open Find Usages",
    "Show all usages in a separate tool window",
    ToolWindowFind
) {
    init {
        shortcutSet = ActionManager.getInstance().getAction(ACTION_FIND_USAGES).shortcutSet
    }

    override fun actionPerformed(e: AnActionEvent) {
        closePopUp(e.inputEvent)
        val toArray = usages.toTypedArray()
        val usageViewPresentation = UsageViewPresentation()
        usageViewPresentation.tabText = "Type"
        usageViewPresentation.isOpenInNewTab = false
        val instance = UsageViewManager.getInstance(e.project!!)
        instance.showUsages(
            UsageTarget.EMPTY_ARRAY,
            toArray,
            usageViewPresentation
        )
    }
}

private class SetBreakpointAction(
    private val usages: List<Usage>,
    private val closePopUp: (InputEvent) -> Unit
) : AnAction(
    "Set Breakpoints",
    "Set breakpoints at all usages",
    AllIcons.Debugger.Db_set_breakpoint
) {
    override fun actionPerformed(e: AnActionEvent) {
        closePopUp(e.inputEvent)
        val breakpointManager = DebuggerManagerEx.getInstanceEx(e.project!!).breakpointManager
        usages.forEach {
            val containingFile = it.file()
            if (containingFile != null) {
                val document = PsiDocumentManager.getInstance(e.project!!).getDocument(containingFile)
                if (document != null) {
                    val isBreakpointAdded = addLineBreakpoint(it, breakpointManager, document)
                    if (!isBreakpointAdded) {
                        addMethodBreakpoint(it, breakpointManager, document)
                    }
                }
            }
        }
    }

    private fun addLineBreakpoint(
        it: Usage,
        breakpointManager: BreakpointManager,
        document: Document
    ): Boolean {
        val sourcePsi = it.getPostStatementSourcePsi()
        if (sourcePsi != null) {
            val lineNumber = sourcePsi.getLineNumber(true)
            breakpointManager.addLineBreakpoint(document, lineNumber)
        }
        return sourcePsi != null
    }

    private fun addMethodBreakpoint(
        it: Usage,
        breakpointManager: BreakpointManager,
        document: Document?
    ) {
        val source = it.getSubscribeMethodSourcePsi()
        if (source != null) {
            val lineNumber = source.getLineNumber(true)
            if (breakpointManager.findBreakpoint<BreakpointWithHighlighter<JavaBreakpointProperties<*>>>(
                    document,
                    source.startOffset,
                    null
                ) == null
            ) {
                breakpointManager.addMethodBreakpoint(document, lineNumber)
            }
        }
    }
}

private class RemoveBreakpointAction(
    private val usages: List<Usage>,
    private val closePopUp: (InputEvent) -> Unit
) : AnAction(
    "Remove Breakpoints",
    "Remove breakpoints at all usages",
    AllIcons.Debugger.MuteBreakpoints
) {
    override fun actionPerformed(e: AnActionEvent) {
        closePopUp(e.inputEvent)
        val project = e.project!!
        val breakpointManager = DebuggerManagerEx.getInstanceEx(project).breakpointManager
        val documentManager = PsiDocumentManager.getInstance(project)
        usages.forEach {
            val containingFile = it.file()
            if (containingFile != null) {
                val document = documentManager.getDocument(containingFile)
                val source = it.getPostStatementSourcePsi() ?: it.getSubscribeMethodSourcePsi()
                if (source != null && document != null) {
                    val breakPoint =
                        breakpointManager.findBreakpoint<BreakpointWithHighlighter<JavaBreakpointProperties<*>>>(
                            document,
                            source.startOffset,
                            null
                        )
                    breakpointManager.removeBreakpoint(breakPoint)
                }
            }
        }
    }
}