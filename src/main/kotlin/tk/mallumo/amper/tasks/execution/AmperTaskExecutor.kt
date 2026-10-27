package tk.mallumo.amper.tasks.execution

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import tk.mallumo.amper.tasks.model.AmperTask
import tk.mallumo.amper.tasks.ui.AmperIcons
import java.awt.BorderLayout
import java.io.File
import javax.swing.JPanel

object AmperTaskExecutor {

    fun execute(project: Project, task: AmperTask) {
        val basePath = project.basePath ?: return
        val projectRoot = File(basePath)
        val isWindows = SystemInfo.isWindows

        val kotlinFile = if (isWindows) {
            File(projectRoot, "kotlin.bat").takeIf { it.exists() }
                ?: File(projectRoot, "kotlin.cmd").takeIf { it.exists() }
        } else {
            File(projectRoot, "kotlin").takeIf { it.exists() }
        }

        val commandLine = GeneralCommandLine()
        commandLine.withWorkDirectory(projectRoot)
        commandLine.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)

        if (kotlinFile != null) {
            if (!isWindows && !kotlinFile.canExecute()) {
                kotlinFile.setExecutable(true)
            }
            commandLine.exePath = kotlinFile.absolutePath
            commandLine.addParameters(task.commandArgs)
        } else {
            commandLine.exePath = if (isWindows) "amper.bat" else "amper"
            commandLine.addParameters(task.commandArgs)
        }

        ApplicationManager.getApplication().invokeLater {
            try {
                val executor = DefaultRunExecutor.getRunExecutorInstance()
                val consoleView =
                    TextConsoleBuilderFactory.getInstance().createBuilder(project).console
                val processHandler = KillableColoredProcessHandler(commandLine)
                ProcessTerminatedListener.attach(processHandler)
                consoleView.attachToProcess(processHandler)

                val panel = JPanel(BorderLayout())
                panel.add(consoleView.component, BorderLayout.CENTER)

                val tabTitle = buildTabTitle(task)
                val descriptor = RunContentDescriptor(
                    consoleView,
                    processHandler,
                    panel,
                    tabTitle,
                    AmperIcons.ToolWindow
                )
                Disposer.register(project, descriptor)

                RunContentManager.getInstance(project).showRunContent(executor, descriptor)

                consoleView.print(
                    "> amper ${task.commandArgs.joinToString(" ")}\n\n",
                    ConsoleViewContentType.SYSTEM_OUTPUT
                )

                processHandler.startNotify()
            } catch (e: Exception) {
                val consoleView =
                    TextConsoleBuilderFactory.getInstance().createBuilder(project).console
                val panel = JPanel(BorderLayout())
                panel.add(consoleView.component, BorderLayout.CENTER)
                val descriptor = RunContentDescriptor(
                    consoleView,
                    null,
                    panel,
                    "Amper Error",
                    AmperIcons.ToolWindow
                )
                Disposer.register(project, descriptor)
                RunContentManager.getInstance(project).showRunContent(
                    DefaultRunExecutor.getRunExecutorInstance(),
                    descriptor
                )
                consoleView.print(
                    "Failed to start Amper task: ${e.message}\n",
                    ConsoleViewContentType.ERROR_OUTPUT
                )
            }
        }
    }

    private fun buildTabTitle(task: AmperTask): String {
        return if (task.module != null && task.module.path != ".") {
            "${task.module.name}:${task.name}"
        } else {
            task.name
        }
    }
}
