package com.madrapps.eventbus.post

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.RIGHT
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionStatement
import com.intellij.ui.awt.RelativePoint
import com.intellij.usageView.UsageInfo
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.util.concurrency.AppExecutorUtil
import com.madrapps.eventbus.getCallExpression
import com.madrapps.eventbus.getParentOfTypeCallExpression
import com.madrapps.eventbus.search
import com.madrapps.eventbus.showPostUsages
import com.madrapps.eventbus.subscribe.isSubscribe
import org.jetbrains.uast.*

class PostLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val uElement = element.toUElement() ?: return null
        if (element !is PsiExpressionStatement) {
            val uCallExpression = uElement.getCallExpression()
            if (uCallExpression != null && uCallExpression.isPost()) {
                val psiIdentifier = uCallExpression.methodIdentifier?.sourcePsi ?: return null
                return PostLineMarkerInfo(psiIdentifier)
            }
        }
        return null
    }
}

internal fun UsageInfo.isPost(): Boolean {
    val uElement = element.toUElement()
    if (uElement != null) {
        if (uElement.getParentOfType<UImportStatement>() == null) {
            return uElement.getParentOfTypeCallExpression()?.isPost() == true
        }
    }
    return false
}

private fun UCallExpression.isPost(): Boolean {
    return receiverType?.canonicalText == "org.greenrobot.eventbus.EventBus"
            && (methodName == "post" || methodName == "postSticky")
}

private class PostLineMarkerInfo(
    psiElement: PsiElement
) : LineMarkerInfo<PsiElement>(
    psiElement,
    psiElement.textRange,
    IconLoader.getIcon("/icons/greenrobot.png"),
    null,
    { event, element ->
        ReadAction.nonBlocking {
            var usages = emptyList<UsageInfo2UsageAdapter>()
            val uElement = element.toUElement()?.getParentOfType<UCallExpression>()
            if (uElement != null) {
                val argument = uElement.valueArguments.firstOrNull()
                val elementsToSearch: List<PsiElement> = if (argument is UQualifiedReferenceExpression) {
                    val sourcePsi = (argument.receiver as USimpleNameReferenceExpression).sourcePsi
                    sourcePsi?.references?.mapNotNull { it.resolve() } ?: emptyList()
                } else {
                    val resolve = (argument?.getExpressionType() as PsiClassType).resolve()
                    if (resolve != null) {
                        listOf(resolve)
                    } else emptyList()
                }
                val collection = search(elementsToSearch)
                usages = collection
                    .filter(UsageInfo::isSubscribe)
                    .map(::UsageInfo2UsageAdapter)
            }
            ApplicationManager.getApplication().invokeLater {
                if (usages.size == 1) {
                    usages.first().navigate(true)
                } else {
                    showPostUsages(usages, RelativePoint(event))
                }
            }
        }.inSmartMode(element.project).submit(AppExecutorUtil.getAppExecutorService())
    },
    RIGHT
)
