package com.quetzalfir.contentcollector

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.DumbAware
import java.awt.datatransfer.StringSelection
import java.util.*

/**
 * Acción básica: sin filtros; excluye automáticamente carpetas amarillas.
 */
class GetContentAction : AnAction(), DumbAware {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible =
            e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.isNotEmpty() == true
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val roots = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val out = StringBuilder()
        val visited: MutableSet<String> = HashSet()

        roots.forEach {
            CollectionUtils.collect(
                it, out,
                extensions = emptySet(),
                userExcludes = emptyList(),
                project = e.project ?: return,
                visited = visited
            )
        }

        CopyPasteManager.getInstance().setContents(StringSelection(out.trimEnd('\n').toString()))

        NotificationGroupManager.getInstance()
            .getNotificationGroup("contentcollector")
            .createNotification("Contenido copiado al portapapeles", NotificationType.INFORMATION)
            .notify(e.project)
    }
}
