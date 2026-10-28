package tk.mallumo.amper.tasks.execution

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import tk.mallumo.amper.tasks.model.AmperTask

object AmperTaskExecutor {

    fun execute(project: Project, task: AmperTask) {
        ApplicationManager.getApplication().invokeLater {
            val runManager = RunManager.getInstance(project)
            val configType =
                ConfigurationTypeUtil.findConfigurationType(AmperRunConfigurationType::class.java)
            val factory = configType.configurationFactories.first()

            val configName = buildConfigName(project, task)

            var settings = runManager.findConfigurationByTypeAndName(configType, configName)
            if (settings == null) {
                settings = runManager.createConfiguration(configName, factory)
                val config = settings.configuration as AmperRunConfiguration
                config.commandArgsList = task.commandArgs
                config.moduleName = task.module?.name
                runManager.addConfiguration(settings)
            } else {
                val config = settings.configuration as AmperRunConfiguration
                config.commandArgsList = task.commandArgs
                config.moduleName = task.module?.name
            }

            // Select in top toolbar dropdown
            runManager.selectedConfiguration = settings

            // Execute in standard IDE Run Console
            ProgramRunnerUtil.executeConfiguration(
                settings,
                DefaultRunExecutor.getRunExecutorInstance()
            )
        }
    }

    private fun buildConfigName(project: Project, task: AmperTask): String {
        return if (task.module != null && task.module.path != ".") {
            "${project.name} [${task.module.name}:${task.name}]"
        } else {
            "${project.name} [${task.name}]"
        }
    }
}
