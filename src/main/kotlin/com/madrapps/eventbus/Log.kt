package com.madrapps.eventbus

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import org.apache.commons.lang.exception.ExceptionUtils
import java.time.LocalDateTime

val notify = NotificationGroup("EventBus", NotificationDisplayType.NONE, false)

fun blog(msg: String) {
    val timedMsg = "${LocalDateTime.now()} : $msg"
    println(timedMsg)
    ApplicationManager.getApplication().invokeLater {
        Notifications.Bus.notify(notify.createNotification(timedMsg, NotificationType.INFORMATION))
    }
}

fun errLog(msg: String) {
    val timedMsg = "${LocalDateTime.now()} : $msg"
    println(timedMsg)
    ApplicationManager.getApplication().invokeLater {
        Notifications.Bus.notify(notify.createNotification(timedMsg, NotificationType.ERROR))
    }
}

fun errLog(e: Throwable) {
    val timedMsg = "${LocalDateTime.now()} : ${e.message}"
    println(timedMsg)
    ApplicationManager.getApplication().invokeLater {
        Notifications.Bus.notify(notify.createNotification(timedMsg, NotificationType.ERROR))
        Notifications.Bus.notify(notify.createNotification(ExceptionUtils.getFullStackTrace(e), NotificationType.ERROR))
    }
}