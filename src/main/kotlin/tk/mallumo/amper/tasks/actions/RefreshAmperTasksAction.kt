package tk.mallumo.amper.tasks.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import tk.mallumo.amper.tasks.ui.AmperTreePanel

class RefreshAmperTasksAction(private val panel: AmperTreePanel) :
    AnAction(
        "Reload Amper Tasks",
        "Scan project and reload Amper tasks",
        AllIcons.Actions.Refresh
    ) {

    override fun actionPerformed(e: AnActionEvent) {
        panel.refresh()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
