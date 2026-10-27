package tk.mallumo.amper.tasks.model

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
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
}
