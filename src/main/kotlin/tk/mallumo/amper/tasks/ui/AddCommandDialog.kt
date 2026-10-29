package tk.mallumo.amper.tasks.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

class AddCommandDialog(
    val myProject: Project,
    val groupName: String,
    val defaultWorkingDir: String,
    existingName: String = "",
    existingCommand: String = "",
    existingWorkingDir: String = ""
) : DialogWrapper(myProject) {

    private val nameField = JBTextField(existingName)
    private val commandField = JBTextArea(existingCommand, 3, 20).apply {
        lineWrap = true
        wrapStyleWord = true
        rows = 3
    }
    private val commandScrollPane = JBScrollPane(commandField)
    private val workingDirField = TextFieldWithBrowseButton().apply {
        text = existingWorkingDir.ifEmpty { defaultWorkingDir }
        addBrowseFolderListener(
            TextBrowseFolderListener(
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    .withTitle("Select Working Directory"),
                myProject
            )
        )
    }

    init {
        title = if (existingName.isEmpty()) "Add Command to '$groupName'" else "Edit Command"
        setOKButtonText(if (existingName.isEmpty()) "Add" else "Save")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val form = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Command Name:"), nameField, 1, false)
            .addLabeledComponent(JBLabel("CLI / Shell Command:"), commandScrollPane, 1, true)
            .addLabeledComponent(JBLabel("Working Directory:"), workingDirField, 1, false)
            .panel

        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(8)
        panel.add(form, BorderLayout.CENTER)
        panel.preferredSize = Dimension(520, 200)
        return panel
    }

    override fun getPreferredFocusedComponent(): JComponent = nameField

    override fun doValidate(): ValidationInfo? {
        if (nameField.text.trim().isEmpty()) {
            return ValidationInfo("Please enter a command name", nameField)
        }
        if (commandField.text.trim().isEmpty()) {
            return ValidationInfo("Please enter a CLI command", commandField)
        }
        return null
    }

    val commandName: String get() = nameField.text.trim()
    val commandText: String get() = commandField.text.trim()
    val workingDir: String get() = workingDirField.text.trim()
}
