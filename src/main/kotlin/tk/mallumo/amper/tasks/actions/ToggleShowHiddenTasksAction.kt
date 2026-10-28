package tk.mallumo.amper.tasks.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import tk.mallumo.amper.tasks.model.AmperCustomTasksService
import tk.mallumo.amper.tasks.ui.AmperTreePanel

class ToggleShowHiddenTasksAction(private val panel: AmperTreePanel) :
    ToggleAction(
        "Show Hidden Tasks",
        "Show or hide hidden tasks and nodes",
        AllIcons.Actions.Show
    ) {

    override fun isSelected(e: AnActionEvent): Boolean {
        return AmperCustomTasksService.getInstance(panel.project).showHidden
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        AmperCustomTasksService.getInstance(panel.project).showHidden = state
        panel.refresh()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
