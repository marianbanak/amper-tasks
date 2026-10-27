package tk.mallumo.amper.tasks.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import tk.mallumo.amper.tasks.ui.AmperTreePanel

class RunSelectedAmperTaskAction(private val panel: AmperTreePanel) :
    AnAction("Run Amper Task", "Execute selected Amper task", AllIcons.Actions.Execute) {

    override fun actionPerformed(e: AnActionEvent) {
        panel.runSelectedTask()
    }

    override fun update(e: AnActionEvent) {
        val selectedTask = panel.getSelectedTask()
        e.presentation.isEnabled = selectedTask != null
        if (selectedTask != null) {
            e.presentation.text = "Run '${selectedTask.name}'"
        } else {
            e.presentation.text = "Run Amper Task"
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
