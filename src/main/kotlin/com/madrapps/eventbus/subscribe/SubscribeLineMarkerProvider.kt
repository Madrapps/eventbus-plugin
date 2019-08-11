package com.madrapps.eventbus.subscribe

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.RIGHT
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.ui.awt.RelativePoint
import com.intellij.usageView.UsageInfo
import com.intellij.usages.UsageInfo2UsageAdapter
import com.madrapps.eventbus.post.isPost
import com.madrapps.eventbus.search
import com.madrapps.eventbus.showSubscribeUsages
import org.jetbrains.uast.*
import org.jetbrains.uast.UastVisibility.PUBLIC
import java.awt.event.MouseEvent

class SubscribeLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val uElement = element.toUElement() ?: return null
        val uMethod = uElement.getSubscribeMethod()
        if (uMethod != null) {
            val psiElement = uMethod.uastAnchor?.sourcePsi
            if (psiElement != null) {
                return SubscribeLineMarkerInfo(psiElement, uMethod)
            }
        }
        return null
    }
}

internal fun UsageInfo.isSubscribe(): Boolean {
    val uElement = element.toUElement()
    if (uElement != null) {
        if (uElement.getParentOfType<UImportStatement>() == null) {
            return uElement.getParentOfType<UMethod>()?.getSubscribeMethod() != null
        }
    }
    return false
}

private fun UElement.getSubscribeMethod(): UMethod? {
    if (this is UMethod) {
        annotations.find { it.qualifiedName == "org.greenrobot.eventbus.Subscribe" } ?: return null
        if (visibility == PUBLIC && uastParameters.size == 1) {
            return this
        }
    }
    return null
}

private class SubscribeLineMarkerInfo(
    psiElement: PsiElement,
    private val uElement: UMethod
) : LineMarkerInfo<PsiElement>(
    psiElement,
    psiElement.textRange,
    IconLoader.getIcon("/icons/greenrobot.png"),
    null,
    null,
    RIGHT
) {
    override fun createGutterRenderer(): GutterIconRenderer? {
        return object : LineMarkerGutterIconRenderer<PsiElement>(this) {
            override fun getClickAction(): AnAction? {
                return object : AnAction() {
                    override fun actionPerformed(e: AnActionEvent) {

                        val elementToSearch =
                            (uElement.uastParameters[0].type as PsiClassReferenceType).reference.resolve()
                        if (elementToSearch != null) {
                            val usages = search(elementToSearch)
                                .filter(UsageInfo::isPost)
                                .map(::UsageInfo2UsageAdapter)
                            if (usages.size == 1) {
                                usages.first().navigate(true)
                            } else {
                                showSubscribeUsages(usages, RelativePoint(e.inputEvent as MouseEvent))
                            }
                        }
                    }
                }
            }
        }
    }
}
