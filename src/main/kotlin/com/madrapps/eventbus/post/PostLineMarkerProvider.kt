package com.madrapps.eventbus.post

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.RIGHT
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionStatement
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.ui.awt.RelativePoint
import com.intellij.usageView.UsageInfo
import com.intellij.usages.UsageInfo2UsageAdapter
import com.madrapps.eventbus.search
import com.madrapps.eventbus.showPostUsages
import com.madrapps.eventbus.subscribe.isSubscribe
import org.jetbrains.uast.*
import java.awt.event.MouseEvent

class PostLineMarkerProvider : LineMarkerProvider {

    override fun collectSlowLineMarkers(
        elements: MutableList<PsiElement>,
        result: MutableCollection<LineMarkerInfo<PsiElement>>
    ) = Unit

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val uElement = element.toUElement() ?: return null
        if (element !is PsiExpressionStatement) {
            val uCallExpression = uElement.getPostCallExpression()
            if (uCallExpression != null) {
                val psiIdentifier = uCallExpression.methodIdentifier?.sourcePsi ?: return null
                return PostLineMarkerInfo(psiIdentifier, uCallExpression)
            }
        }
        return null
    }
}

internal fun UsageInfo.isPost(): Boolean {
    val uElement = element.toUElement()
    if (uElement != null) {
        if (uElement.getParentOfType<UImportStatement>() == null) {
            return uElement.getParentOfType<UQualifiedReferenceExpression>()?.getPostCallExpression() != null
        }
    }
    return false
}

private fun UElement.getPostCallExpression(): UCallExpression? {
    if (this is UQualifiedReferenceExpression) {
        val uCallExpression = selector as? UCallExpression ?: return null
        if (uCallExpression.receiverType?.canonicalText == "org.greenrobot.eventbus.EventBus"
            && (uCallExpression.methodName == "post" || uCallExpression.methodName == "postSticky")
        ) {
            return uCallExpression
        }
    }
    return null
}

private class PostLineMarkerInfo(
    psiElement: PsiElement,
    private val uElement: UCallExpression
) : LineMarkerInfo<PsiElement>(
    psiElement,
    psiElement.textRange,
    IconLoader.getIcon("/icons/greenrobot.png"),
    Pass.LINE_MARKERS,
    null,
    null,
    RIGHT
) {
    override fun createGutterRenderer(): GutterIconRenderer? {
        return object : LineMarkerInfo.LineMarkerGutterIconRenderer<PsiElement>(this) {
            override fun getClickAction(): AnAction? = object : AnAction() {
                override fun actionPerformed(e: AnActionEvent) {

                    val elementToSearch = (uElement.valueArguments.firstOrNull()
                        ?.getExpressionType() as PsiClassReferenceType).resolve()
                    if (elementToSearch != null) {
                        val usages = search(elementToSearch)
                            .filter(UsageInfo::isSubscribe)
                            .map(::UsageInfo2UsageAdapter)
                        if (usages.size == 1) {
                            usages.first().navigate(true)
                        } else {
                            showPostUsages(usages, RelativePoint(e.inputEvent as MouseEvent))
                        }
                    }
                }
            }
        }
    }
}
