package tk.mallumo.amper.tasks.execution

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import tk.mallumo.amper.tasks.ui.AmperIcons
import javax.swing.Icon

class AmperRunConfigurationType : ConfigurationType {
    companion object {
        const val ID = "AmperTaskRunConfiguration"
    }

    override fun getDisplayName(): String = "Amper Task"
    override fun getConfigurationTypeDescription(): String = "Kotlin Amper task execution"
    override fun getIcon(): Icon = AmperIcons.ToolWindow
    override fun getId(): String = ID

    override fun getConfigurationFactories(): Array<ConfigurationFactory> = arrayOf(
        object : ConfigurationFactory(this) {
            override fun getId(): String = "AmperTask"
            override fun createTemplateConfiguration(project: Project): RunConfiguration {
                return AmperRunConfiguration(project, this, "Amper Task")
            }

            override fun getOptionsClass(): Class<out AmperRunConfigurationOptions> {
                return AmperRunConfigurationOptions::class.java
            }
        }
    )
}
