package com.madrapps.eventbus.subscribe

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.RIGHT
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.ui.awt.RelativePoint
import com.intellij.usageView.UsageInfo
import com.intellij.usages.UsageInfo2UsageAdapter
import com.madrapps.eventbus.post.isPost
import com.madrapps.eventbus.post.isPostMethod
import com.madrapps.eventbus.search
import com.madrapps.eventbus.showUsages
import org.jetbrains.uast.*
import org.jetbrains.uast.UastVisibility.PUBLIC
import java.awt.event.MouseEvent


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

                        val elementToSearch =
                            (uElement.uastParameters[0].type as PsiClassReferenceType).reference.resolve()
                        if (elementToSearch != null) {
                            val collection = search(elementToSearch)

                            val usages = collection.filter {
                                it.isPost()
                            }.map(::UsageInfo2UsageAdapter)

                            showUsages(usages, RelativePoint(e.inputEvent as MouseEvent))
                        }
                    }
                }
            }
        }
    }
}
