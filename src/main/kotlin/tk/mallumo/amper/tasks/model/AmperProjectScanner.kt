package tk.mallumo.amper.tasks.model

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

object AmperProjectScanner {

    fun isAmperProject(project: Project): Boolean {
        val rootDir = project.guessProjectDir() ?: return false
        return isAmperProjectDir(rootDir)
    }

    fun isAmperProjectDir(rootDir: VirtualFile): Boolean {
        if (!rootDir.isValid) return false
        return rootDir.findChild("kotlin") != null ||
                rootDir.findChild("kotlin.bat") != null ||
                rootDir.findChild("project.yaml") != null ||
                rootDir.findChild("module.yaml") != null
    }

    fun scan(project: Project): AmperProject? {
        val rootDir = project.guessProjectDir() ?: return null
        if (!isAmperProjectDir(rootDir)) return null

        val modules = scanModules(rootDir)
        val allPublishRepos = modules.flatMap { it.publishableRepositories }.distinct()

        val projectTasks = buildProjectTasks(allPublishRepos)
        val moduleTasks = modules.associateWith { module ->
            buildModuleTasks(module)
        }

        return AmperProject(
            name = project.name,
            rootDir = rootDir,
            projectTasks = projectTasks,
            modules = modules,
            moduleTasks = moduleTasks
        )
    }

    private fun scanModules(rootDir: VirtualFile): List<AmperModule> {
        val modules = mutableListOf<AmperModule>()
        val projectYamlFile = rootDir.findChild("project.yaml")

        val yaml = Yaml(SafeConstructor(LoaderOptions()))

        if (projectYamlFile != null && projectYamlFile.isValid) {
            try {
                val content = String(projectYamlFile.contentsToByteArray(), Charsets.UTF_8)
                val data = yaml.load<Any>(content)
                if (data is Map<*, *>) {
                    val modulesList = data["modules"]
                    if (modulesList is List<*>) {
                        for (modEntry in modulesList) {
                            val modPathStr = modEntry?.toString()?.trim() ?: continue
                            val modDir = rootDir.findFileByRelativePath(modPathStr)
                            if (modDir != null && modDir.isDirectory) {
                                val modYamlFile = modDir.findChild("module.yaml")
                                val (platforms, isApp, publishRepos) = if (modYamlFile != null) {
                                    parseModuleYaml(modYamlFile, yaml)
                                } else {
                                    Triple(emptyList(), false, emptyList())
                                }
                                modules.add(
                                    AmperModule(
                                        name = modPathStr,
                                        path = modPathStr,
                                        moduleDir = modDir,
                                        platforms = platforms,
                                        isApplication = isApp,
                                        publishableRepositories = publishRepos
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore parse errors, fallback to discovery
            }
        }

        // If no modules found via project.yaml, discover by searching for module.yaml
        if (modules.isEmpty()) {
            val rootModuleYaml = rootDir.findChild("module.yaml")
            if (rootModuleYaml != null) {
                val (platforms, isApp, publishRepos) = parseModuleYaml(rootModuleYaml, yaml)
                modules.add(
                    AmperModule(
                        name = rootDir.name,
                        path = ".",
                        moduleDir = rootDir,
                        platforms = platforms,
                        isApplication = isApp,
                        publishableRepositories = publishRepos
                    )
                )
            }

            VfsUtilCore.visitChildrenRecursively(rootDir, object : VirtualFileVisitor<Void>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    if (file == rootDir) return true
                    val name = file.name
                    if (name.startsWith(".") || name == "build" || name == "gradle" || name == "target") {
                        return false
                    }
                    if (file.isDirectory) {
                        val modYaml = file.findChild("module.yaml")
                        if (modYaml != null) {
                            val relPath = VfsUtilCore.getRelativePath(file, rootDir) ?: file.name
                            if (modules.none { it.path == relPath }) {
                                val (platforms, isApp, publishRepos) = parseModuleYaml(
                                    modYaml,
                                    yaml
                                )
                                modules.add(
                                    AmperModule(
                                        name = relPath,
                                        path = relPath,
                                        moduleDir = file,
                                        platforms = platforms,
                                        isApplication = isApp,
                                        publishableRepositories = publishRepos
                                    )
                                )
                            }
                            return false // Don't look for nested modules inside a module
                        }
                    }
                    return true
                }
            })
        }

        return modules
    }

    private fun parseModuleYaml(
        file: VirtualFile,
        yaml: Yaml
    ): Triple<List<String>, Boolean, List<String>> {
        var platforms = emptyList<String>()
        var isApp = false
        val publishRepos = mutableListOf<String>()

        try {
            val content = String(file.contentsToByteArray(), Charsets.UTF_8)
            val data = yaml.load<Any>(content)
            if (data is Map<*, *>) {
                val product = data["product"]
                if (product != null) {
                    val prodStr = product.toString().lowercase()
                    if (prodStr.contains("app")) {
                        isApp = true
                    }
                    if (product is Map<*, *>) {
                        val type = product["type"]?.toString()?.lowercase() ?: ""
                        if (type.contains("app")) isApp = true
                        val platformsList = product["platforms"]
                        if (platformsList is List<*>) {
                            platforms = platformsList.mapNotNull { it?.toString() }
                        }
                    }
                }

                // Check repositories for publishing
                val repos = data["repositories"]
                if (repos is List<*>) {
                    for (repo in repos) {
                        if (repo is Map<*, *>) {
                            val publish = repo["publish"]
                            val isPublish = publish == true || publish?.toString()
                                .equals("true", ignoreCase = true)
                            if (isPublish) {
                                val id = repo["id"]?.toString() ?: repo["url"]?.toString()
                                if (!id.isNullOrBlank()) {
                                    publishRepos.add(id)
                                }
                            }
                        }
                    }
                } else if (repos is Map<*, *>) {
                    for ((k, v) in repos) {
                        if (v is Map<*, *>) {
                            val publish = v["publish"]
                            val isPublish = publish == true || publish?.toString()
                                .equals("true", ignoreCase = true)
                            if (isPublish) {
                                publishRepos.add(k.toString())
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }

        return Triple(platforms, isApp, publishRepos)
    }

    private fun buildProjectTasks(allPublishRepos: List<String>): List<AmperTask> {
        val tasks = mutableListOf<AmperTask>()

        // Build
        tasks.add(AmperTask("build", TaskGroup.BUILD, listOf("build"), "Build the entire project"))
        tasks.add(AmperTask("clean", TaskGroup.BUILD, listOf("clean"), "Clean build outputs"))

        // Verification
        tasks.add(
            AmperTask(
                "test",
                TaskGroup.VERIFICATION,
                listOf("test"),
                "Run tests for all modules"
            )
        )
        tasks.add(
            AmperTask(
                "check",
                TaskGroup.VERIFICATION,
                listOf("check"),
                "Run verification checks"
            )
        )

        // Publishing
        val publishTargets =
            if (allPublishRepos.isNotEmpty()) allPublishRepos else listOf("mavenLocal")
        for (repo in publishTargets) {
            tasks.add(
                AmperTask(
                    name = "publish $repo",
                    group = TaskGroup.PUBLISHING,
                    commandArgs = listOf("publish", repo),
                    description = "Publish project artifacts to $repo"
                )
            )
        }

        // Help
        tasks.add(
            AmperTask(
                "show modules",
                TaskGroup.HELP,
                listOf("show", "modules"),
                "Display project modules"
            )
        )
        tasks.add(
            AmperTask(
                "show tasks",
                TaskGroup.HELP,
                listOf("show", "tasks"),
                "Display available tasks"
            )
        )
        tasks.add(
            AmperTask(
                "show settings",
                TaskGroup.HELP,
                listOf("show", "settings"),
                "Display project settings"
            )
        )
        tasks.add(
            AmperTask(
                "show dependencies",
                TaskGroup.HELP,
                listOf("show", "dependencies"),
                "Display project dependencies"
            )
        )

        return tasks
    }

    private fun buildModuleTasks(module: AmperModule): List<AmperTask> {
        val tasks = mutableListOf<AmperTask>()
        val modArg = if (module.path == ".") emptyList() else listOf("-m", module.path)

        // Build
        tasks.add(
            AmperTask(
                "build",
                TaskGroup.BUILD,
                listOf("build") + modArg,
                "Build module ${module.name}",
                module
            )
        )
        tasks.add(
            AmperTask(
                "clean",
                TaskGroup.BUILD,
                listOf("clean") + modArg,
                "Clean module ${module.name}",
                module
            )
        )

        // Verification
        tasks.add(
            AmperTask(
                "test",
                TaskGroup.VERIFICATION,
                listOf("test") + modArg,
                "Run tests in module ${module.name}",
                module
            )
        )
        tasks.add(
            AmperTask(
                "check",
                TaskGroup.VERIFICATION,
                listOf("check") + modArg,
                "Run checks in module ${module.name}",
                module
            )
        )

        // Run & Package (if application)
        if (module.isApplication) {
            tasks.add(
                AmperTask(
                    "run",
                    TaskGroup.RUN,
                    listOf("run") + modArg,
                    "Run application in module ${module.name}",
                    module
                )
            )
            tasks.add(
                AmperTask(
                    "package",
                    TaskGroup.BUILD,
                    listOf("package") + modArg,
                    "Package application in module ${module.name} into an executable JAR",
                    module
                )
            )
        }

        // Publishing
        val pubRepos =
            if (module.publishableRepositories.isNotEmpty()) module.publishableRepositories else listOf(
                "mavenLocal"
            )
        for (repo in pubRepos) {
            tasks.add(
                AmperTask(
                    name = "publish $repo",
                    group = TaskGroup.PUBLISHING,
                    commandArgs = listOf("publish") + modArg + listOf(repo),
                    description = "Publish module ${module.name} to $repo",
                    module = module
                )
            )
        }

        // Help
        tasks.add(
            AmperTask(
                "show dependencies",
                TaskGroup.HELP,
                listOf("show", "dependencies") + modArg,
                "Display dependencies for ${module.name}",
                module
            )
        )
        tasks.add(
            AmperTask(
                "show settings",
                TaskGroup.HELP,
                listOf("show", "settings") + modArg,
                "Display settings for ${module.name}",
                module
            )
        )

        return tasks
    }
}
