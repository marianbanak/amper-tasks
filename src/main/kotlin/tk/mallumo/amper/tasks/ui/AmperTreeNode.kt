package tk.mallumo.amper.tasks.ui

import tk.mallumo.amper.tasks.model.AmperModule
import tk.mallumo.amper.tasks.model.AmperProject
import tk.mallumo.amper.tasks.model.AmperTask
import tk.mallumo.amper.tasks.model.TaskGroup
import javax.swing.tree.DefaultMutableTreeNode

sealed class AmperTreeNode(val title: String) : DefaultMutableTreeNode() {
    class ProjectNode(val amperProject: AmperProject) : AmperTreeNode(amperProject.name)
    class ModuleNode(val module: AmperModule) : AmperTreeNode(module.name)
    class TasksRootNode : AmperTreeNode("Tasks")
    class GroupNode(val group: TaskGroup) : AmperTreeNode(group.displayName)
    class TaskNode(val task: AmperTask) : AmperTreeNode(task.name)

    override fun toString(): String = title
}
