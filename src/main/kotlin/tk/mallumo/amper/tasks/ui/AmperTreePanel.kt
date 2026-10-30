package tk.mallumo.amper.tasks.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.DefaultTreeExpander
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.ui.Messages
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import tk.mallumo.amper.tasks.actions.RefreshAmperTasksAction
import tk.mallumo.amper.tasks.actions.RunSelectedAmperTaskAction
import tk.mallumo.amper.tasks.actions.ToggleShowHiddenTasksAction
import tk.mallumo.amper.tasks.execution.AmperTaskExecutor
import tk.mallumo.amper.tasks.model.*
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

class AmperTreePanel(val project: Project) : JPanel(BorderLayout()) {

    private val tree = Tree()
    private val cardLayout = CardLayout()
    private val contentPanel = JPanel(cardLayout)
    private val emptyLabel = JBLabel(
        "<html><center>No Amper project configuration found.<br/>(Looking for project.yaml, module.yaml, or ./kotlin)</center></html>",
        SwingConstants.CENTER
    )

    private var isFirstLoad = true
    private var isUpdatingTreeModel = false

    companion object {
        private const val CARD_TREE = "TREE"
        private const val CARD_EMPTY = "EMPTY"
    }

    init {
        setupTree()
        setupUI()
        refresh()
    }

    private fun setupTree() {
        tree.cellRenderer = AmperTreeCellRenderer()
        tree.isRootVisible = true
        tree.showsRootHandles = true

        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                val path = tree.getPathForLocation(event.x, event.y) ?: return false
                val node = path.lastPathComponent
                if (node is AmperTreeNode.TaskNode) {
                    AmperTaskExecutor.execute(project, node.task)
                    return true
                }
                return false
            }
        }.installOn(tree)

        tree.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val path = tree.getPathForLocation(e.x, e.y)
                    if (path != null) {
                        tree.selectionPath = path
                        showContextMenu(e.component, e.x, e.y, path.lastPathComponent)
                    }
                }
            }
        })

        tree.registerKeyboardAction(
            { runSelectedTask() },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_FOCUSED
        )

        tree.addTreeExpansionListener(object : TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent) {
                if (!isUpdatingTreeModel) {
                    saveCurrentExpandedState()
                }
            }

            override fun treeCollapsed(event: TreeExpansionEvent) {
                if (!isUpdatingTreeModel) {
                    saveCurrentExpandedState()
                }
            }
        })
    }

    private fun showContextMenu(invoker: Component, x: Int, y: Int, node: Any?) {
        val menu = JBPopupMenu()

        when (node) {
            is AmperTreeNode.DirectoryNode -> {
                val addGroupItem = JBMenuItem("Add Group...", AllIcons.General.Add)
                addGroupItem.addActionListener {
                    promptAddGroup(node.dirPath)
                }
                menu.add(addGroupItem)

                menu.addSeparator()
                val isHidden = node.isHidden
                val hideItem = JBMenuItem(
                    if (isHidden) "Unhide Nodes" else "Hide Nodes",
                    AllIcons.Actions.Show
                )
                hideItem.addActionListener {
                    AmperCustomTasksService.getInstance(project).toggleHidden(node.key)
                    refresh()
                }
                menu.add(hideItem)
            }

            is AmperTreeNode.ModuleNode -> {
                val module = node.module
                val addGroupItem = JBMenuItem("Add Group...", AllIcons.General.Add)
                addGroupItem.addActionListener {
                    promptAddGroup(module.path)
                }
                menu.add(addGroupItem)
            }

            is AmperTreeNode.ProjectNode -> {
                val addGroupItem = JBMenuItem("Add Group...", AllIcons.General.Add)
                addGroupItem.addActionListener {
                    promptAddGroup("")
                }
                menu.add(addGroupItem)
            }

            is AmperTreeNode.TasksRootNode -> {
                val addGroupItem = JBMenuItem("Add Group...", AllIcons.General.Add)
                addGroupItem.addActionListener {
                    promptAddGroup("")
                }
                menu.add(addGroupItem)

                menu.addSeparator()
                val isHidden = node.isHidden
                val hideItem = JBMenuItem(
                    if (isHidden) "Unhide Nodes" else "Hide Nodes",
                    AllIcons.Actions.Show
                )
                hideItem.addActionListener {
                    AmperCustomTasksService.getInstance(project).toggleHidden(node.key)
                    refresh()
                }
                menu.add(hideItem)
            }

            is AmperTreeNode.GroupNode -> {
                val modulePath = node.targetPath
                val defaultWorkDir = if (node.module != null) {
                    node.module.moduleDir.path
                } else if (node.targetPath.isNotEmpty()) {
                    project.guessProjectDir()?.findFileByRelativePath(node.targetPath)?.path
                        ?: File(project.basePath ?: "", node.targetPath).absolutePath
                } else {
                    project.basePath ?: ""
                }
                val addCmdItem = JBMenuItem("Add Command...", AllIcons.General.Add)
                addCmdItem.addActionListener {
                    openAddCommandDialog(modulePath, node.groupName, defaultWorkDir)
                }
                menu.add(addCmdItem)

                menu.addSeparator()
                val isHidden = node.isHidden
                val hideItem = JBMenuItem(
                    if (isHidden) "Unhide Nodes" else "Hide Nodes",
                    AllIcons.Actions.Show
                )
                hideItem.addActionListener {
                    AmperCustomTasksService.getInstance(project).toggleHidden(node.key)
                    refresh()
                }
                menu.add(hideItem)

                if (node.isCustom) {
                    menu.addSeparator()
                    val delGroupItem = JBMenuItem("Delete Group", AllIcons.Actions.GC)
                    delGroupItem.addActionListener {
                        val confirm = Messages.showYesNoDialog(
                            project,
                            "Delete group '${node.groupName}' and all its commands?",
                            "Delete Group",
                            Messages.getQuestionIcon()
                        )
                        if (confirm == Messages.YES) {
                            AmperCustomTasksService.getInstance(project)
                                .removeGroup(modulePath, node.groupName)
                            refresh()
                        }
                    }
                    menu.add(delGroupItem)
                }
            }

            is AmperTreeNode.TaskNode -> {
                val task = node.task
                val runItem = JBMenuItem("Run '${task.name}'", AllIcons.Actions.Execute)
                runItem.addActionListener {
                    AmperTaskExecutor.execute(project, task)
                }
                menu.add(runItem)

                menu.addSeparator()
                val isHidden = node.isHidden
                val hideItem = JBMenuItem(
                    if (isHidden) "Unhide Task" else "Hide Task",
                    AllIcons.Actions.Show
                )
                hideItem.addActionListener {
                    AmperCustomTasksService.getInstance(project).toggleHidden(node.key)
                    refresh()
                }
                menu.add(hideItem)

                if (task.isCustom) {
                    menu.addSeparator()
                    val editItem = JBMenuItem("Edit Command...", AllIcons.Actions.Edit)
                    editItem.addActionListener {
                        openEditCommandDialog(task)
                    }
                    menu.add(editItem)

                    val deleteItem = JBMenuItem("Delete Command", AllIcons.Actions.GC)
                    deleteItem.addActionListener {
                        val confirm = Messages.showYesNoDialog(
                            project,
                            "Delete command '${task.name}'?",
                            "Delete Command",
                            Messages.getQuestionIcon()
                        )
                        if (confirm == Messages.YES) {
                            task.customCommandId?.let { id ->
                                AmperCustomTasksService.getInstance(project).removeCommand(id)
                                refresh()
                            }
                        }
                    }
                    menu.add(deleteItem)
                }
            }
        }

        if (menu.componentCount > 0) {
            menu.show(invoker, x, y)
        }
    }

    private fun promptAddGroup(modulePath: String) {
        val groupName = Messages.showInputDialog(
            project,
            "Enter new group name:",
            "Add Group",
            AllIcons.Nodes.Folder
        )
        if (!groupName.isNullOrBlank()) {
            AmperCustomTasksService.getInstance(project).addGroup(modulePath, groupName.trim())
            refresh()
        }
    }

    private fun openAddCommandDialog(
        modulePath: String,
        groupName: String,
        defaultWorkDir: String
    ) {
        val dialog = AddCommandDialog(project, groupName, defaultWorkDir)
        if (dialog.showAndGet()) {
            val newCmd = CustomCommandData(
                id = UUID.randomUUID().toString(),
                name = dialog.commandName,
                command = dialog.commandText,
                groupName = groupName,
                modulePath = modulePath,
                workingDir = dialog.workingDir
            )
            AmperCustomTasksService.getInstance(project).addOrUpdateCommand(newCmd)
            refresh()
        }
    }

    private fun openEditCommandDialog(task: AmperTask) {
        val cmdId = task.customCommandId ?: return
        val modulePath = task.targetPath
        val defaultWorkDir =
            task.workingDir ?: task.module?.moduleDir?.path ?: if (task.targetPath.isNotEmpty()) {
                project.guessProjectDir()?.findFileByRelativePath(task.targetPath)?.path
                    ?: File(project.basePath ?: "", task.targetPath).absolutePath
            } else {
                project.basePath ?: ""
            }
        val dialog = AddCommandDialog(
            myProject = project,
            groupName = task.groupName,
            defaultWorkingDir = defaultWorkDir,
            existingName = task.name,
            existingCommand = task.rawCommandLine ?: "",
            existingWorkingDir = task.workingDir ?: ""
        )
        if (dialog.showAndGet()) {
            val updated = CustomCommandData(
                id = cmdId,
                name = dialog.commandName,
                command = dialog.commandText,
                groupName = task.groupName,
                modulePath = modulePath,
                workingDir = dialog.workingDir
            )
            AmperCustomTasksService.getInstance(project).addOrUpdateCommand(updated)
            refresh()
        }
    }

    private fun setupUI() {
        val scrollPane = ScrollPaneFactory.createScrollPane(tree)
        contentPanel.add(scrollPane, CARD_TREE)

        emptyLabel.border = JBUI.Borders.empty(16)
        contentPanel.add(emptyLabel, CARD_EMPTY)

        val toolbar = createToolbar()
        add(toolbar.component, BorderLayout.NORTH)
        add(contentPanel, BorderLayout.CENTER)
    }

    private fun createToolbar(): ActionToolbar {
        val group = DefaultActionGroup()

        group.add(RunSelectedAmperTaskAction(this))
        group.add(RefreshAmperTasksAction(this))
        group.add(ToggleShowHiddenTasksAction(this))

        val treeExpander = DefaultTreeExpander(tree)
        val commonActionsManager = CommonActionsManager.getInstance()
        group.add(commonActionsManager.createExpandAllAction(treeExpander, tree))
        group.add(commonActionsManager.createCollapseAllAction(treeExpander, tree))

        val actionToolbar =
            ActionManager.getInstance().createActionToolbar("AmperToolbar", group, true)
        actionToolbar.targetComponent = this
        return actionToolbar
    }

    fun getSelectedTask(): AmperTask? {
        val node = tree.lastSelectedPathComponent
        return if (node is AmperTreeNode.TaskNode) node.task else null
    }

    fun runSelectedTask() {
        val task = getSelectedTask() ?: return
        AmperTaskExecutor.execute(project, task)
    }

    fun refresh() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val amperProject = AmperProjectScanner.scan(project)
            ApplicationManager.getApplication().invokeLater {
                if (amperProject == null) {
                    cardLayout.show(contentPanel, CARD_EMPTY)
                } else {
                    updateTreeModel(amperProject)
                    cardLayout.show(contentPanel, CARD_TREE)
                }
            }
        }
    }

    private fun updateTreeModel(amperProject: AmperProject) {
        val customTasksService = AmperCustomTasksService.getInstance(project)
        val savedExpanded = if (isFirstLoad) {
            isFirstLoad = false
            customTasksService.getExpandedPaths()
        } else {
            val current = saveExpandedPaths()
            customTasksService.setExpandedPaths(current)
            current
        }

        isUpdatingTreeModel = true
        try {
            val hiddenKeys = customTasksService.getHiddenKeys()
            val showHidden = customTasksService.showHidden
            val rootNode = AmperTreeNode.buildTree(amperProject, hiddenKeys, showHidden)
            tree.model = DefaultTreeModel(rootNode)
            if (savedExpanded.isEmpty() || savedExpanded.contains("project")) {
                tree.expandRow(0)
            }
            restoreExpandedPaths(savedExpanded)
        } finally {
            isUpdatingTreeModel = false
        }
    }

    private fun saveCurrentExpandedState() {
        val expanded = saveExpandedPaths()
        AmperCustomTasksService.getInstance(project).setExpandedPaths(expanded)
    }

    private fun saveExpandedPaths(): Set<String> {
        val expanded = mutableSetOf<String>()
        for (i in 0 until tree.rowCount) {
            val path = tree.getPathForRow(i)
            if (path != null && tree.isExpanded(path)) {
                expanded.add(pathToString(path))
            }
        }
        return expanded
    }

    private fun restoreExpandedPaths(saved: Set<String>) {
        if (saved.isEmpty()) {
            return
        }
        val root = tree.model.root as? TreeNode ?: return
        restoreExpandedRecursively(root, TreePath(root), saved)
    }

    private fun restoreExpandedRecursively(
        node: TreeNode,
        currentPath: TreePath,
        saved: Set<String>
    ) {
        try {
            val pathStr = pathToString(currentPath)
            if (saved.contains(pathStr)) {
                tree.expandPath(currentPath)
            }
            for (i in 0 until node.childCount) {
                val child = node.getChildAt(i) ?: continue
                restoreExpandedRecursively(child, currentPath.pathByAddingChild(child), saved)
            }
        } catch (_: Throwable) {
            // Safely ignore any errors if a node no longer exists
        }
    }

    private fun pathToString(path: TreePath): String {
        return path.path.mapNotNull { (it as? AmperTreeNode)?.key ?: it?.toString() }.joinToString("/")
    }
}

