package tk.mallumo.amper.tasks.model

import com.intellij.testFramework.LightVirtualFile
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import tk.mallumo.amper.tasks.ui.AmperTreeNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AmperModelTest {

    @Test
    fun testYamlParsingProjectModules() {
        val yamlText = """
            modules:
              - core
              - app/android
              - app/shared
        """.trimIndent()

        val yaml = Yaml(SafeConstructor(LoaderOptions()))
        val data = yaml.load<Any>(yamlText) as Map<*, *>
        val modules = (data["modules"] as List<*>).map { it.toString() }

        assertEquals(listOf("core", "app/android", "app/shared"), modules)
    }

    @Test
    fun testYamlParsingModuleRepositories() {
        val yamlText = """
            product: lib
            repositories:
              - id: mavenLocal
                url: mavenLocal
                publish: true
              - id: repsy
                url: https://repo.repsy.io/mvn/mallumo/public
                publish: true
              - id: readOnlyRepo
                url: https://example.com/repo
                publish: false
        """.trimIndent()

        val yaml = Yaml(SafeConstructor(LoaderOptions()))
        val data = yaml.load<Any>(yamlText) as Map<*, *>
        val repos = data["repositories"] as List<*>

        val publishable = repos.mapNotNull { repo ->
            if (repo is Map<*, *>) {
                val publish = repo["publish"]
                val isPublish =
                    publish == true || publish?.toString().equals("true", ignoreCase = true)
                if (isPublish) (repo["id"] ?: repo["url"])?.toString() else null
            } else null
        }

        assertEquals(listOf("mavenLocal", "repsy"), publishable)
    }

    @Test
    fun testTaskCommandArgs() {
        val task = AmperTask("build", TaskGroup.BUILD, listOf("build", "-m", "core"))
        assertEquals("build", task.name)
        assertEquals(TaskGroup.BUILD, task.group)
        assertEquals(listOf("build", "-m", "core"), task.commandArgs)
    }

    @Test
    fun testTreeBuildingWithSharedParentDirectory() {
        val rootDir = LightVirtualFile("root")
        val mod1Dir = LightVirtualFile("common-s-files")
        val mod2Dir = LightVirtualFile("common-serializer")
        val mod3Dir = LightVirtualFile("server-files")

        val mod1 = AmperModule(
            name = "common-s-files",
            path = "commons-pub/common-s-files",
            moduleDir = mod1Dir,
            platforms = listOf("jvm")
        )
        val mod2 = AmperModule(
            name = "common-serializer",
            path = "commons-pub/common-serializer",
            moduleDir = mod2Dir,
            platforms = listOf("jvm")
        )
        val mod3 = AmperModule(
            name = "server-files",
            path = "server-files",
            moduleDir = mod3Dir,
            platforms = listOf("jvm")
        )

        val project = AmperProject(
            name = "s-files",
            rootDir = rootDir,
            projectTasks = listOf(
                AmperTask("build", TaskGroup.BUILD, listOf("build")),
                AmperTask("test", TaskGroup.VERIFICATION, listOf("test"))
            ),
            modules = listOf(mod1, mod2, mod3),
            moduleTasks = mapOf(
                mod1 to listOf(
                    AmperTask(
                        "build",
                        TaskGroup.BUILD,
                        listOf("build", "-m", mod1.name),
                        module = mod1
                    )
                ),
                mod2 to listOf(
                    AmperTask(
                        "build",
                        TaskGroup.BUILD,
                        listOf("build", "-m", mod2.name),
                        module = mod2
                    )
                ),
                mod3 to listOf(
                    AmperTask(
                        "build",
                        TaskGroup.BUILD,
                        listOf("build", "-m", mod3.name),
                        module = mod3
                    )
                )
            )
        )

        val treeRoot = AmperTreeNode.buildTree(project)
        assertEquals("s-files", treeRoot.title)
        // Root children: 1. Tasks, 2. commons-pub directory, 3. server-files module
        assertEquals(3, treeRoot.childCount)

        val tasksNode = treeRoot.getChildAt(0) as AmperTreeNode.TasksRootNode
        assertEquals("Tasks", tasksNode.title)

        val commonsPubDir = treeRoot.getChildAt(1) as AmperTreeNode.DirectoryNode
        assertEquals("commons-pub", commonsPubDir.title)
        assertEquals(2, commonsPubDir.childCount)

        val childMod1 = commonsPubDir.getChildAt(0) as AmperTreeNode.ModuleNode
        assertEquals("common-s-files", childMod1.title)
        assertEquals("commons-pub/common-s-files", childMod1.module.path)

        val childMod2 = commonsPubDir.getChildAt(1) as AmperTreeNode.ModuleNode
        assertEquals("common-serializer", childMod2.title)
        assertEquals("commons-pub/common-serializer", childMod2.module.path)

        val serverFilesNode = treeRoot.getChildAt(2) as AmperTreeNode.ModuleNode
        assertEquals("server-files", serverFilesNode.title)
        assertEquals("server-files", serverFilesNode.module.path)
    }

    @Test
    fun testCustomGroupsAndCommands() {
        val rootDir = LightVirtualFile("root")
        val modDir = LightVirtualFile("app")
        val mod = AmperModule(name = "app", path = "app", moduleDir = modDir)

        val customTask = AmperTask(
            name = "deploy-staging",
            groupName = "deploy",
            module = mod,
            isCustom = true,
            rawCommandLine = "./deploy.sh staging"
        )

        val project = AmperProject(
            name = "my-project",
            rootDir = rootDir,
            projectTasks = emptyList(),
            modules = listOf(mod),
            moduleTasks = mapOf(mod to listOf(customTask)),
            customGroups = mapOf("app" to listOf("deploy", "docker"))
        )

        val treeRoot = AmperTreeNode.buildTree(project)
        val modNode = treeRoot.getChildAt(0) as AmperTreeNode.ModuleNode
        assertEquals("app", modNode.title)
        assertEquals(2, modNode.childCount)

        val deployGroup = modNode.getChildAt(0) as AmperTreeNode.GroupNode
        assertEquals("deploy", deployGroup.groupName)
        assertTrue(deployGroup.isCustom)
        assertEquals(1, deployGroup.childCount)

        val taskNode = deployGroup.getChildAt(0) as AmperTreeNode.TaskNode
        assertEquals("deploy-staging", taskNode.title)
        assertTrue(taskNode.task.isCustom)
        assertEquals("./deploy.sh staging", taskNode.task.rawCommandLine)

        val dockerGroup = modNode.getChildAt(1) as AmperTreeNode.GroupNode
        assertEquals("docker", dockerGroup.groupName)
        assertTrue(dockerGroup.isCustom)
        assertEquals(0, dockerGroup.childCount)
    }

    @Test
    fun testHideTaskAndNode() {
        val rootDir = LightVirtualFile("root")
        val modDir = LightVirtualFile("app")
        val mod = AmperModule(name = "app", path = "app", moduleDir = modDir)

        val task1 = AmperTask("build", TaskGroup.BUILD, listOf("build", "-m", "app"), module = mod)
        val task2 = AmperTask("clean", TaskGroup.BUILD, listOf("clean", "-m", "app"), module = mod)
        val testTask =
            AmperTask("test", TaskGroup.VERIFICATION, listOf("test", "-m", "app"), module = mod)

        val project = AmperProject(
            name = "my-project",
            rootDir = rootDir,
            projectTasks = emptyList(),
            modules = listOf(mod),
            moduleTasks = mapOf(mod to listOf(task1, task2, testTask))
        )

        val hiddenCleanKey = "task:app:build:clean"
        val hiddenVerificationGroupKey = "group:app:verification"

        // Case 1: showHidden = false (default)
        val treeDefault = AmperTreeNode.buildTree(
            project,
            hiddenKeys = setOf(hiddenCleanKey, hiddenVerificationGroupKey),
            showHidden = false
        )
        val modDefault = treeDefault.getChildAt(0) as AmperTreeNode.ModuleNode
        assertEquals(1, modDefault.childCount)
        val buildGroupDefault = modDefault.getChildAt(0) as AmperTreeNode.GroupNode
        assertEquals(1, buildGroupDefault.childCount)
        val onlyBuildTask = buildGroupDefault.getChildAt(0) as AmperTreeNode.TaskNode
        assertEquals("build", onlyBuildTask.title)

        // Case 2: showHidden = true
        val treeShowHidden = AmperTreeNode.buildTree(
            project,
            hiddenKeys = setOf(hiddenCleanKey, hiddenVerificationGroupKey),
            showHidden = true
        )
        val modShowHidden = treeShowHidden.getChildAt(0) as AmperTreeNode.ModuleNode
        assertEquals(2, modShowHidden.childCount)

        val buildGroup = modShowHidden.getChildAt(0) as AmperTreeNode.GroupNode
        assertEquals(2, buildGroup.childCount)
        val visibleTask = buildGroup.getChildAt(0) as AmperTreeNode.TaskNode
        assertEquals("build", visibleTask.title)
        assertFalse(visibleTask.isHidden)

        val hiddenTask = buildGroup.getChildAt(1) as AmperTreeNode.TaskNode
        assertEquals("clean", hiddenTask.title)
        assertTrue(hiddenTask.isHidden)

        val verificationGroup = modShowHidden.getChildAt(1) as AmperTreeNode.GroupNode
        assertEquals("verification", verificationGroup.groupName)
        assertTrue(verificationGroup.isHidden)
        val childTest = verificationGroup.getChildAt(0) as AmperTreeNode.TaskNode
        assertTrue(childTest.isHidden)
    }

    @Test
    fun testDirectoryTasksAndCustomGroups() {
        val rootDir = LightVirtualFile("root")
        val mod1Dir = LightVirtualFile("common")
        val mod2Dir = LightVirtualFile("js")

        val mod1 = AmperModule(name = "common", path = "www-portal/common", moduleDir = mod1Dir)
        val mod2 = AmperModule(name = "js", path = "www-portal/js", moduleDir = mod2Dir)

        val portalTask = AmperTask(
            name = "portal-deploy",
            groupName = "deploy",
            isCustom = true,
            rawCommandLine = "./deploy-portal.sh",
            targetPath = "www-portal"
        )

        val project = AmperProject(
            name = "my-project",
            rootDir = rootDir,
            projectTasks = emptyList(),
            modules = listOf(mod1, mod2),
            moduleTasks = mapOf(
                mod1 to listOf(
                    AmperTask(
                        "build",
                        TaskGroup.BUILD,
                        listOf("build", "-m", "www-portal/common"),
                        module = mod1
                    )
                ),
                mod2 to listOf(
                    AmperTask(
                        "build",
                        TaskGroup.BUILD,
                        listOf("build", "-m", "www-portal/js"),
                        module = mod2
                    )
                )
            ),
            customGroups = mapOf("www-portal" to listOf("deploy")),
            directoryTasks = mapOf("www-portal" to listOf(portalTask))
        )

        val treeRoot = AmperTreeNode.buildTree(project)
        assertEquals(1, treeRoot.childCount)

        val dirNode = treeRoot.getChildAt(0) as AmperTreeNode.DirectoryNode
        assertEquals("www-portal", dirNode.dirName)
        assertEquals("www-portal", dirNode.dirPath)
        assertEquals("dir:www-portal", dirNode.key)

        // Children of www-portal: 1. deploy group, 2. common module, 3. js module
        assertEquals(3, dirNode.childCount)

        val deployGroup = dirNode.getChildAt(0) as AmperTreeNode.GroupNode
        assertEquals("deploy", deployGroup.groupName)
        assertEquals("www-portal", deployGroup.targetPath)
        assertEquals("group:www-portal:deploy", deployGroup.key)
        assertTrue(deployGroup.isCustom)
        assertEquals(1, deployGroup.childCount)

        val taskNode = deployGroup.getChildAt(0) as AmperTreeNode.TaskNode
        assertEquals("portal-deploy", taskNode.title)
        assertEquals("task:www-portal:deploy:portal-deploy", taskNode.key)
        assertEquals("./deploy-portal.sh", taskNode.task.rawCommandLine)
        assertEquals("www-portal", taskNode.task.targetPath)

        val mod1Node = dirNode.getChildAt(1) as AmperTreeNode.ModuleNode
        assertEquals("common", mod1Node.title)

        val mod2Node = dirNode.getChildAt(2) as AmperTreeNode.ModuleNode
        assertEquals("js", mod2Node.title)
    }
}
