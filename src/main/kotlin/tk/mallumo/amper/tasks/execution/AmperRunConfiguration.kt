package tk.mallumo.amper.tasks.execution

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

class AmperRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : LocatableConfigurationBase<AmperRunConfigurationOptions>(project, factory, name) {

    override fun getOptions(): AmperRunConfigurationOptions =
        super.getOptions() as AmperRunConfigurationOptions

    var commandArgsList: List<String>
        get() = options.commandArgs?.split("\u0000")?.filter { it.isNotEmpty() } ?: emptyList()
        set(value) {
            options.commandArgs = value.joinToString("\u0000")
        }

    var moduleName: String?
        get() = options.moduleName
        set(value) {
            options.moduleName = value
        }

    var isCustom: Boolean
        get() = options.isCustom
        set(value) {
            options.isCustom = value
        }

    var rawCommandLine: String?
        get() = options.rawCommandLine
        set(value) {
            options.rawCommandLine = value
        }

    var workingDir: String?
        get() = options.workingDir
        set(value) {
            options.workingDir = value
        }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return object : CommandLineState(environment) {
            @Throws(ExecutionException::class)
            override fun startProcess(): ProcessHandler {
                val basePath = environment.project.basePath ?: "."
                val projectRoot = File(basePath)
                val isWindows = SystemInfo.isWindows

                val cmd = GeneralCommandLine()
                val workDir = if (!workingDir.isNullOrBlank()) {
                    File(workingDir!!).takeIf { it.exists() } ?: projectRoot
                } else {
                    projectRoot
                }
                cmd.withWorkDirectory(workDir)
                cmd.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)

                if (isCustom && !rawCommandLine.isNullOrBlank()) {
                    if (isWindows) {
                        cmd.exePath = "cmd.exe"
                        cmd.addParameters("/c", rawCommandLine!!)
                    } else {
                        cmd.exePath = "/bin/sh"
                        cmd.addParameters("-c", rawCommandLine!!)
                    }
                } else {
                    val kotlinFile = if (isWindows) {
                        File(projectRoot, "kotlin.bat").takeIf { it.exists() }
                            ?: File(projectRoot, "kotlin.cmd").takeIf { it.exists() }
                    } else {
                        File(projectRoot, "kotlin").takeIf { it.exists() }
                    }

                    if (kotlinFile != null) {
                        if (!isWindows && !kotlinFile.canExecute()) {
                            kotlinFile.setExecutable(true)
                        }
                        cmd.exePath = kotlinFile.absolutePath
                    } else {
                        cmd.exePath = if (isWindows) "amper.bat" else "amper"
                    }

                    cmd.addParameters(commandArgsList)
                }

                val processHandler = KillableColoredProcessHandler(cmd)
                ProcessTerminatedListener.attach(processHandler)
                return processHandler
            }
        }.apply {
            consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project)
        }
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return object : SettingsEditor<AmperRunConfiguration>() {
            override fun resetEditorFrom(s: AmperRunConfiguration) {}
            override fun applyEditorTo(s: AmperRunConfiguration) {}
            override fun createEditor(): JComponent = JPanel()
        }
    }
}
