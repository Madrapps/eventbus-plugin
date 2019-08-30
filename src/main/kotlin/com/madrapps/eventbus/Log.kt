package com.madrapps.eventbus

import com.intellij.notification.NotificationDisplayType.NONE
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationType.ERROR
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import org.apache.commons.lang.exception.ExceptionUtils
import java.time.LocalDateTime

val notify = NotificationGroup("GreenRobot EventBus", NONE, false)

fun blog(msg: String) = log(msg, INFORMATION)

fun errLog(msg: String) = log(msg, ERROR)

private fun log(msg: String, type: NotificationType) {
    val timedMsg = "${LocalDateTime.now()} : $msg"
    println(timedMsg)
    ApplicationManager.getApplication().invokeLater {
        Notifications.Bus.notify(notify.createNotification(timedMsg, type))
    }
}

fun errLog(e: Throwable) {
    val timedMsg = "${LocalDateTime.now()} : ${e.message}"
    println(timedMsg)
    ApplicationManager.getApplication().invokeLater {
        Notifications.Bus.notify(notify.createNotification(timedMsg, ERROR))
        Notifications.Bus.notify(notify.createNotification(ExceptionUtils.getFullStackTrace(e), ERROR))
    }
}