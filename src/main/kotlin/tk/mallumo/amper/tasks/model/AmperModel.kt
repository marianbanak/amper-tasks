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
    val groupName: String,
    val commandArgs: List<String> = emptyList(),
    val description: String? = null,
    val module: AmperModule? = null,
    val isCustom: Boolean = false,
    val customCommandId: String? = null,
    val rawCommandLine: String? = null,
    val workingDir: String? = null,
    val targetPath: String = module?.path ?: ""
) {
    constructor(
        name: String,
        group: TaskGroup,
        commandArgs: List<String>,
        description: String? = null,
        module: AmperModule? = null
    ) : this(
        name = name,
        groupName = group.displayName,
        commandArgs = commandArgs,
        description = description,
        module = module,
        isCustom = false,
        targetPath = module?.path ?: ""
    )

    val group: TaskGroup
        get() = TaskGroup.values().find { it.displayName.equals(groupName, ignoreCase = true) }
            ?: TaskGroup.OTHER
}

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
    val moduleTasks: Map<AmperModule, List<AmperTask>>,
    val customGroups: Map<String, List<String>> = emptyMap(),
    val directoryTasks: Map<String, List<AmperTask>> = emptyMap()
)
