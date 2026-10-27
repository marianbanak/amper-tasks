package tk.mallumo.amper.tasks.model

import com.intellij.openapi.vfs.VirtualFile

enum class TaskGroup(val displayName: String) {
    BUILD("build"),
    VERIFICATION("verification"),
    PUBLISHING("publishing"),
    RUN("run"),
    HELP("help"),
    OTHER("other")
}

data class AmperTask(
    val name: String,
    val group: TaskGroup,
    val commandArgs: List<String>,
    val description: String? = null,
    val module: AmperModule? = null
)

data class AmperModule(
    val name: String,
    val path: String,
    val moduleDir: VirtualFile,
    val platforms: List<String> = emptyList(),
    val isApplication: Boolean = false,
    val publishableRepositories: List<String> = emptyList()
)

data class AmperProject(
    val name: String,
    val rootDir: VirtualFile,
    val projectTasks: List<AmperTask>,
    val modules: List<AmperModule>,
    val moduleTasks: Map<AmperModule, List<AmperTask>>
)
