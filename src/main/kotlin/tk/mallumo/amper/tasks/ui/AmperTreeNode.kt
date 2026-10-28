package tk.mallumo.amper.tasks.ui

import tk.mallumo.amper.tasks.model.AmperModule
import tk.mallumo.amper.tasks.model.AmperProject
import tk.mallumo.amper.tasks.model.AmperTask
import tk.mallumo.amper.tasks.model.TaskGroup
import javax.swing.tree.DefaultMutableTreeNode

sealed class AmperTreeNode(val title: String, var isHidden: Boolean = false) :
    DefaultMutableTreeNode() {
    open val key: String get() = title

    class ProjectNode(val amperProject: AmperProject) : AmperTreeNode(amperProject.name) {
        override val key: String get() = "project"
    }

    class DirectoryNode(
        val dirName: String,
        val dirPath: String = dirName,
        isHidden: Boolean = false
    ) : AmperTreeNode(dirName, isHidden) {
        override val key: String get() = "dir:$dirPath"
    }

    class ModuleNode(
        val module: AmperModule,
        title: String = module.name.substringAfterLast('/'),
        isHidden: Boolean = false
    ) : AmperTreeNode(title, isHidden) {
        override val key: String get() = "module:${module.path}"
    }

    class TasksRootNode(isHidden: Boolean = false) : AmperTreeNode("Tasks", isHidden) {
        override val key: String get() = "tasksRoot:"
    }

    class GroupNode(
        val groupName: String,
        val module: AmperModule? = null,
        val isCustom: Boolean = false,
        val targetPath: String = module?.path ?: "",
        isHidden: Boolean = false
    ) : AmperTreeNode(groupName, isHidden) {
        override val key: String get() = "group:$targetPath:$groupName"
        val group: TaskGroup
            get() = TaskGroup.values().find { it.displayName.equals(groupName, ignoreCase = true) }
                ?: TaskGroup.OTHER

        constructor(group: TaskGroup, module: AmperModule? = null) : this(
            group.displayName,
            module,
            false,
            module?.path ?: "",
            false
        )
    }

    class TaskNode(
        val task: AmperTask,
        isHidden: Boolean = false,
        val targetPath: String = task.targetPath
    ) : AmperTreeNode(task.name, isHidden) {
        override val key: String get() = "task:$targetPath:${task.groupName}:${task.name}"
    }

    override fun toString(): String = title

    companion object {
        fun buildTree(
            amperProject: AmperProject,
            hiddenKeys: Set<String> = emptySet(),
            showHidden: Boolean = false
        ): ProjectNode {
            val rootNode = ProjectNode(amperProject)

            // 1. Project level Tasks
            val projectCustomGroups = amperProject.customGroups[""] ?: emptyList()
            if (amperProject.projectTasks.isNotEmpty() || projectCustomGroups.isNotEmpty()) {
                val tasksRootKey = "tasksRoot:"
                val tasksRootIsHidden = hiddenKeys.contains(tasksRootKey)

                if (!tasksRootIsHidden || showHidden) {
                    val tasksRoot = TasksRootNode(tasksRootIsHidden)
                    val tasksByGroup = amperProject.projectTasks.groupBy { it.groupName }
                    val builtInOrder = listOf("build", "verification", "publishing", "run", "help")
                    val allGroupNames = (builtInOrder.filter {
                        tasksByGroup.containsKey(it) || projectCustomGroups.contains(it)
                    } +
                            (tasksByGroup.keys + projectCustomGroups).filter {
                                !builtInOrder.contains(
                                    it
                                )
                            }).distinct()

                    for (grpName in allGroupNames) {
                        val grpKey = "group::$grpName"
                        val grpHidden = tasksRootIsHidden || hiddenKeys.contains(grpKey)
                        if (grpHidden && !showHidden) continue

                        val isCustom = !TaskGroup.values()
                            .any { it.displayName.equals(grpName, ignoreCase = true) }
                        val groupNode = GroupNode(
                            groupName = grpName,
                            module = null,
                            isCustom = isCustom,
                            targetPath = "",
                            isHidden = grpHidden
                        )

                        for (task in tasksByGroup[grpName] ?: emptyList()) {
                            val tKey = "task::${task.groupName}:${task.name}"
                            val tHidden = grpHidden || hiddenKeys.contains(tKey)
                            if (tHidden && !showHidden) continue
                            groupNode.add(TaskNode(task, tHidden, ""))
                        }

                        if (groupNode.childCount > 0 || groupNode.isCustom || showHidden) {
                            tasksRoot.add(groupNode)
                        }
                    }

                    if (tasksRoot.childCount > 0 || showHidden) {
                        rootNode.add(tasksRoot)
                    }
                }
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
                            val dirHidden = hiddenKeys.contains("dir:$currentPath")
                            val node = DirectoryNode(dirSegment, currentPath, dirHidden)
                            currentParent.add(node)
                            attachDirectoryTasksAndGroups(
                                node,
                                amperProject,
                                hiddenKeys,
                                showHidden
                            )
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
                val modCustomGroups = amperProject.customGroups[module.path] ?: emptyList()

                if (modTasks.isNotEmpty() || modCustomGroups.isNotEmpty()) {
                    val tasksByGroup = modTasks.groupBy { it.groupName }
                    val builtInOrder = listOf("build", "verification", "run", "publishing", "help")
                    val allGroupNames = (builtInOrder.filter {
                        tasksByGroup.containsKey(it) || modCustomGroups.contains(it)
                    } +
                            (tasksByGroup.keys + modCustomGroups).filter { !builtInOrder.contains(it) }).distinct()

                    for (grpName in allGroupNames) {
                        val grpKey = "group:${module.path}:$grpName"
                        val grpHidden = hiddenKeys.contains(grpKey)
                        if (grpHidden && !showHidden) continue

                        val isCustom = !TaskGroup.values()
                            .any { it.displayName.equals(grpName, ignoreCase = true) }
                        val groupNode = GroupNode(
                            groupName = grpName,
                            module = module,
                            isCustom = isCustom,
                            targetPath = module.path,
                            isHidden = grpHidden
                        )

                        for (task in tasksByGroup[grpName] ?: emptyList()) {
                            val tKey = "task:${module.path}:${task.groupName}:${task.name}"
                            val tHidden = grpHidden || hiddenKeys.contains(tKey)
                            if (tHidden && !showHidden) continue
                            groupNode.add(TaskNode(task, tHidden, module.path))
                        }

                        if (groupNode.childCount > 0 || groupNode.isCustom || showHidden) {
                            moduleNode.add(groupNode)
                        }
                    }
                }
                parentNode.add(moduleNode)
            }

            // 3. Any additional directory nodes with custom groups/tasks not covered by modules
            val allDirPaths = (amperProject.customGroups.keys + amperProject.directoryTasks.keys)
                .filter { it.isNotEmpty() && !amperProject.modules.any { m -> m.path == it } }
                .distinct()

            for (dirPath in allDirPaths) {
                if (!dirNodes.containsKey(dirPath)) {
                    val normalizedPath = dirPath.trim().trim('/').replace('\\', '/')
                    val segments = normalizedPath.split('/').filter { it.isNotEmpty() }
                    var currentParent: DefaultMutableTreeNode = rootNode
                    var currentPath = ""
                    for (dirSegment in segments) {
                        currentPath =
                            if (currentPath.isEmpty()) dirSegment else "$currentPath/$dirSegment"
                        val dirNode = dirNodes.getOrPut(currentPath) {
                            val dirHidden = hiddenKeys.contains("dir:$currentPath")
                            val node = DirectoryNode(dirSegment, currentPath, dirHidden)
                            currentParent.add(node)
                            attachDirectoryTasksAndGroups(
                                node,
                                amperProject,
                                hiddenKeys,
                                showHidden
                            )
                            node
                        }
                        currentParent = dirNode
                    }
                }
            }

            return rootNode
        }

        private fun attachDirectoryTasksAndGroups(
            dirNode: DirectoryNode,
            amperProject: AmperProject,
            hiddenKeys: Set<String>,
            showHidden: Boolean
        ) {
            val dirPath = dirNode.dirPath
            val dirTasks = amperProject.directoryTasks[dirPath] ?: emptyList()
            val dirCustomGroups = amperProject.customGroups[dirPath] ?: emptyList()

            if (dirTasks.isNotEmpty() || dirCustomGroups.isNotEmpty()) {
                val tasksByGroup = dirTasks.groupBy { it.groupName }
                val builtInOrder = listOf("build", "verification", "run", "publishing", "help")
                val allGroupNames = (builtInOrder.filter {
                    tasksByGroup.containsKey(it) || dirCustomGroups.contains(it)
                } +
                        (tasksByGroup.keys + dirCustomGroups).filter { !builtInOrder.contains(it) }).distinct()

                for (grpName in allGroupNames) {
                    val grpKey = "group:$dirPath:$grpName"
                    val grpHidden = dirNode.isHidden || hiddenKeys.contains(grpKey)
                    if (grpHidden && !showHidden) continue

                    val groupNode = GroupNode(
                        groupName = grpName,
                        module = null,
                        isCustom = true,
                        targetPath = dirPath,
                        isHidden = grpHidden
                    )

                    for (task in tasksByGroup[grpName] ?: emptyList()) {
                        val tKey = "task:$dirPath:${task.groupName}:${task.name}"
                        val tHidden = grpHidden || hiddenKeys.contains(tKey)
                        if (tHidden && !showHidden) continue
                        groupNode.add(TaskNode(task, tHidden, dirPath))
                    }

                    if (groupNode.childCount > 0 || groupNode.isCustom || showHidden) {
                        dirNode.add(groupNode)
                    }
                }
            }
        }
    }
}

