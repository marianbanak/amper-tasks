package tk.mallumo.amper.tasks.model

import com.intellij.testFramework.LightVirtualFile
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import tk.mallumo.amper.tasks.ui.AmperTreeNode
import kotlin.test.Test
import kotlin.test.assertEquals
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
            name = "commons-pub/common-s-files",
            path = "commons-pub/common-s-files",
            moduleDir = mod1Dir,
            platforms = listOf("jvm")
        )
        val mod2 = AmperModule(
            name = "commons-pub/common-serializer",
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
                        listOf("build", "-m", mod1.path),
                        module = mod1
                    )
                ),
                mod2 to listOf(
                    AmperTask(
                        "build",
                        TaskGroup.BUILD,
                        listOf("build", "-m", mod2.path),
                        module = mod2
                    )
                ),
                mod3 to listOf(
                    AmperTask(
                        "build",
                        TaskGroup.BUILD,
                        listOf("build", "-m", mod3.path),
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
}
