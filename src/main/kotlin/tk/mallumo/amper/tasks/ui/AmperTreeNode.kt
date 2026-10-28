package tk.mallumo.amper.tasks.ui

import tk.mallumo.amper.tasks.model.AmperModule
import tk.mallumo.amper.tasks.model.AmperProject
import tk.mallumo.amper.tasks.model.AmperTask
import tk.mallumo.amper.tasks.model.TaskGroup
import javax.swing.tree.DefaultMutableTreeNode

sealed class AmperTreeNode(val title: String) : DefaultMutableTreeNode() {
    class ProjectNode(val amperProject: AmperProject) : AmperTreeNode(amperProject.name)
    class DirectoryNode(val dirName: String) : AmperTreeNode(dirName)
    class ModuleNode(val module: AmperModule, title: String = module.name.substringAfterLast('/')) :
        AmperTreeNode(title)

    class TasksRootNode : AmperTreeNode("Tasks")
    class GroupNode(val group: TaskGroup) : AmperTreeNode(group.displayName)
    class TaskNode(val task: AmperTask) : AmperTreeNode(task.name)

    override fun toString(): String = title

    companion object {
        fun buildTree(amperProject: AmperProject): ProjectNode {
            val rootNode = ProjectNode(amperProject)

            // 1. Project level Tasks
            if (amperProject.projectTasks.isNotEmpty()) {
                val tasksRoot = TasksRootNode()
                val groups = amperProject.projectTasks.groupBy { it.group }
                for ((group, tasks) in groups) {
                    val groupNode = GroupNode(group)
                    for (task in tasks) {
                        groupNode.add(TaskNode(task))
                    }
                    tasksRoot.add(groupNode)
                }
                rootNode.add(tasksRoot)
            }

            // 2. Modules with directory hierarchy
            val dirNodes = mutableMapOf<String, DirectoryNode>()

            for (module in amperProject.modules) {
                val normalizedPath = module.path.trim().trim('/').replace('\\', '/')
                val segments = normalizedPath.split('/').filter { it.isNotEmpty() }

                val parentNode: DefaultMutableTreeNode = if (segments.size > 1) {
                    var currentParent: DefaultMutableTreeNode = rootNode
                    var currentPath = ""
                    for (dirSegment in segments.subList(0, segments.size - 1)) {
                        currentPath =
                            if (currentPath.isEmpty()) dirSegment else "$currentPath/$dirSegment"
                        val dirNode = dirNodes.getOrPut(currentPath) {
                            val node = DirectoryNode(dirSegment)
                            currentParent.add(node)
                            node
                        }
                        currentParent = dirNode
                    }
                    currentParent
                } else {
                    rootNode
                }

                val moduleTitle =
                    if (segments.isNotEmpty() && segments.last() != ".") segments.last() else module.name
                val moduleNode = ModuleNode(module, moduleTitle)
                val modTasks = amperProject.moduleTasks[module] ?: emptyList()
                if (modTasks.isNotEmpty()) {
                    val groups = modTasks.groupBy { it.group }
                    for ((group, tasks) in groups) {
                        val groupNode = GroupNode(group)
                        for (task in tasks) {
                            groupNode.add(TaskNode(task))
                        }
                        moduleNode.add(groupNode)
                    }
                }
                parentNode.add(moduleNode)
            }

            return rootNode
        }
    }
}
