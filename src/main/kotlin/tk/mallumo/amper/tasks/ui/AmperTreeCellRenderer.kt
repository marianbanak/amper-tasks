package tk.mallumo.amper.tasks.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import tk.mallumo.amper.tasks.model.TaskGroup
import javax.swing.JTree

class AmperTreeCellRenderer : ColoredTreeCellRenderer() {

    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        when (value) {
            is AmperTreeNode.ProjectNode -> {
                icon = AmperIcons.ToolWindow
                append(value.title, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            }

            is AmperTreeNode.ModuleNode -> {
                icon = AllIcons.Nodes.Module
                append(value.title, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                if (value.module.platforms.isNotEmpty()) {
                    append(
                        " [${value.module.platforms.joinToString(", ")}]",
                        SimpleTextAttributes.GRAYED_ATTRIBUTES
                    )
                }
            }

            is AmperTreeNode.TasksRootNode -> {
                icon = AllIcons.Nodes.Folder
                append(value.title, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }

            is AmperTreeNode.GroupNode -> {
                icon = when (value.group) {
                    TaskGroup.BUILD -> AllIcons.Actions.Compile
                    TaskGroup.VERIFICATION -> AllIcons.General.InspectionsOK
                    TaskGroup.PUBLISHING -> AllIcons.Actions.Upload
                    TaskGroup.RUN -> AllIcons.Actions.Execute
                    TaskGroup.HELP -> AllIcons.Actions.Help
                    TaskGroup.OTHER -> AllIcons.Nodes.Folder
                }
                append(value.title, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }

            is AmperTreeNode.TaskNode -> {
                icon = AllIcons.Actions.RunAll
                append(value.title, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                toolTipText = "${value.task.description ?: value.title} (amper ${
                    value.task.commandArgs.joinToString(" ")
                })"
            }

            else -> {
                append(value?.toString() ?: "", SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }
        }
    }
}
