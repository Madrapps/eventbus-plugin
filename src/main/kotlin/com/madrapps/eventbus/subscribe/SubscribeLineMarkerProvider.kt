package com.madrapps.eventbus.subscribe

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.RIGHT
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.ui.awt.RelativePoint
import com.intellij.usageView.UsageInfo
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.util.concurrency.AppExecutorUtil
import com.madrapps.eventbus.blog
import com.madrapps.eventbus.post.isPost
import com.madrapps.eventbus.search
import com.madrapps.eventbus.showSubscribeUsages
import org.jetbrains.uast.*
import org.jetbrains.uast.UastVisibility.PUBLIC

class SubscribeLineMarkerProvider : LineMarkerProvider {

    override fun collectSlowLineMarkers(
        elements: MutableList<PsiElement>,
        result: MutableCollection<LineMarkerInfo<PsiElement>>
    ) = Unit

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val uElement = element.toUElement() ?: return null
        val uMethod = uElement.getSubscribeMethod()
        if (uMethod != null) {
            val psiElement = uMethod.uastAnchor?.sourcePsi
            if (psiElement != null) {
                return SubscribeLineMarkerInfo(psiElement)
            }
        }
        return null
    }
}

internal fun UsageInfo.isSubscribe(): Boolean {
    val uElement = element.toUElement()
    if (uElement != null) {
        if (uElement.getParentOfType<UImportStatement>() == null) {
            val uMethod = uElement.getParentOfType<UMethod>()?.getSubscribeMethod()
            if (uMethod != null) {
                val qualifiedName =
                    uMethod.uastParameters.firstOrNull()?.type?.canonicalText ?: return false
                val qualifiedName1 =
                    uElement.getParentOfType<UTypeReferenceExpression>()?.getQualifiedName() ?: return false
                return qualifiedName == qualifiedName1
            }
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
    psiElement: PsiElement
) : LineMarkerInfo<PsiElement>(
    psiElement,
    psiElement.textRange,
    IconLoader.getIcon("/icons/greenrobot.png"),
    Pass.LINE_MARKERS,
    null,
    { event, element ->
        ReadAction.nonBlocking {
            var usages = emptyList<UsageInfo2UsageAdapter>()
            val uElement = element.toUElement()?.getParentOfType<UMethod>()
            if (uElement != null) {
                val elementToSearch =
                    (uElement.uastParameters[0].type as PsiClassReferenceType).reference.resolve()
                if (elementToSearch != null) {
                    val psiClassElement = elementToSearch.toUElement()
                    usages = if((psiClassElement as? PsiClass)?.isEnum == true) {
                        val elementsToSearch = psiClassElement.allFields.filterIsInstance<PsiEnumConstant>()
                        search(elementsToSearch)
                            .filter(UsageInfo::isPost)
                            .map(::UsageInfo2UsageAdapter)
                    } else {
                        search(elementToSearch)
                            .filter(UsageInfo::isPost)
                            .map(::UsageInfo2UsageAdapter)
                    }
                }
            }
            blog("SubscribeLineMarker - ${usages.size} usages found")
            ApplicationManager.getApplication().invokeLater {
                if (usages.size == 1) {
                    usages.first().navigate(true)
                } else {
                    showSubscribeUsages(usages, RelativePoint(event))
                }
            }
        }.inSmartMode(element.project).submit(AppExecutorUtil.getAppExecutorService())
    },
    RIGHT
)