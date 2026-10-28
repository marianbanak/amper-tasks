package tk.mallumo.amper.tasks.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
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

            is AmperTreeNode.DirectoryNode -> {
                val baseIcon = AllIcons.Nodes.Folder
                if (value.isHidden) {
                    icon = IconLoader.getDisabledIcon(baseIcon) ?: baseIcon
                    append(value.title, SimpleTextAttributes.GRAYED_ATTRIBUTES)
                } else {
                    icon = baseIcon
                    append(value.title, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
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
                val baseIcon = AllIcons.Nodes.Folder
                if (value.isHidden) {
                    icon = IconLoader.getDisabledIcon(baseIcon) ?: baseIcon
                    append(value.title, SimpleTextAttributes.GRAYED_ATTRIBUTES)
                } else {
                    icon = baseIcon
                    append(value.title, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }

            is AmperTreeNode.GroupNode -> {
                val baseIcon = when (value.groupName.lowercase()) {
                    "build" -> AllIcons.Actions.Compile
                    "verification" -> AllIcons.General.InspectionsOK
                    "publishing" -> AllIcons.Actions.Upload
                    "run" -> AllIcons.Actions.Execute
                    "help" -> AllIcons.Actions.Help
                    else -> AllIcons.Nodes.Folder
                }
                if (value.isHidden) {
                    icon = IconLoader.getDisabledIcon(baseIcon) ?: baseIcon
                    append(value.title, SimpleTextAttributes.GRAYED_ATTRIBUTES)
                } else {
                    icon = baseIcon
                    append(value.title, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }

            is AmperTreeNode.TaskNode -> {
                val baseIcon = AllIcons.Actions.RunAll
                if (value.isHidden) {
                    icon = IconLoader.getDisabledIcon(baseIcon) ?: baseIcon
                    append(value.title, SimpleTextAttributes.GRAYED_ATTRIBUTES)
                } else {
                    icon = baseIcon
                    append(value.title, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
                val cmdInfo = if (value.task.isCustom) {
                    "${value.task.name} (${value.task.rawCommandLine})"
                } else {
                    "${value.task.description ?: value.title} (amper ${
                        value.task.commandArgs.joinToString(
                            " "
                        )
                    })"
                }
                toolTipText = if (value.isHidden) "[Hidden] $cmdInfo" else cmdInfo
            }

            else -> {
                append(value?.toString() ?: "", SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }
        }
    }
}
