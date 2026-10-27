package tk.mallumo.amper.tasks.ui

import com.intellij.ide.CommonActionsManager
import com.intellij.ide.DefaultTreeExpander
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import tk.mallumo.amper.tasks.actions.RefreshAmperTasksAction
import tk.mallumo.amper.tasks.actions.RunSelectedAmperTaskAction
import tk.mallumo.amper.tasks.execution.AmperTaskExecutor
import tk.mallumo.amper.tasks.model.AmperProject
import tk.mallumo.amper.tasks.model.AmperProjectScanner
import tk.mallumo.amper.tasks.model.AmperTask
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.SwingConstants
import javax.swing.tree.DefaultTreeModel

class AmperTreePanel(private val project: Project) : JPanel(BorderLayout()) {

    private val tree = Tree()
    private val cardLayout = CardLayout()
    private val contentPanel = JPanel(cardLayout)
    private val emptyLabel = JBLabel(
        "<html><center>No Amper project configuration found.<br/>(Looking for project.yaml, module.yaml, or ./kotlin)</center></html>",
        SwingConstants.CENTER
    )

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

        tree.registerKeyboardAction(
            { runSelectedTask() },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_FOCUSED
        )
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
        val rootNode = AmperTreeNode.ProjectNode(amperProject)

        // 1. Project level Tasks
        if (amperProject.projectTasks.isNotEmpty()) {
            val tasksRoot = AmperTreeNode.TasksRootNode()
            val groups = amperProject.projectTasks.groupBy { it.group }
            for ((group, tasks) in groups) {
                val groupNode = AmperTreeNode.GroupNode(group)
                for (task in tasks) {
                    groupNode.add(AmperTreeNode.TaskNode(task))
                }
                tasksRoot.add(groupNode)
            }
            rootNode.add(tasksRoot)
        }

        // 2. Modules
        for (module in amperProject.modules) {
            val moduleNode = AmperTreeNode.ModuleNode(module)
            val modTasks = amperProject.moduleTasks[module] ?: emptyList()
            if (modTasks.isNotEmpty()) {
                val groups = modTasks.groupBy { it.group }
                for ((group, tasks) in groups) {
                    val groupNode = AmperTreeNode.GroupNode(group)
                    for (task in tasks) {
                        groupNode.add(AmperTreeNode.TaskNode(task))
                    }
                    moduleNode.add(groupNode)
                }
            }
            rootNode.add(moduleNode)
        }

        tree.model = DefaultTreeModel(rootNode)

        // Expand root and project tasks
        tree.expandRow(0)
        if (tree.rowCount > 1) {
            tree.expandRow(1)
        }
    }
}
