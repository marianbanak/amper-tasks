import java.util.Properties
import java.util.zip.ZipOutputStream

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.2.1"
    `maven-publish`
}

group = "tk.mallumo"
version = "1.0.2"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2.4")
        bundledPlugin("com.intellij.java")
    }

    testImplementation(kotlin("test"))
}

intellijPlatform {
    buildSearchableOptions = false
    pluginConfiguration {
        id = "tk.mallumo.amper.tasks"
        name = "Amper Tasks"
        version = project.version.toString()
        vendor {
            name = "mallumo"
            url = "https://github.com/mallumo"
        }
        ideaVersion {
            sinceBuild = "242"
            untilBuild = provider { null }
        }
    }
}


val dummySearchableOptions = tasks.register("dummySearchableOptions") {
    val file =
        layout.buildDirectory.file("libs/${project.name}-${project.version}-searchableOptions.jar")
    outputs.file(file)
    doLast {
        val target = file.get().asFile
        if (!target.exists()) {
            target.parentFile.mkdirs()
            ZipOutputStream(target.outputStream()).close()
        }
    }
}

tasks.named("buildPlugin") {
    dependsOn(dummySearchableOptions)
}

tasks.matching {
    it.name.startsWith("publish") || it.name.startsWith("generateMetadata") || it.name.startsWith("generatePom")
}.configureEach {
    dependsOn(dummySearchableOptions)
}

kotlin {
    jvmToolchain(21)
}

publishing {
    val rUrl = propertiesLocal["repsy.url"]
    val rUser = propertiesLocal["repsy.user"]
    val rToken = propertiesLocal["repsy.token"]

    repositories {
        mavenLocal()
        if (!rUrl.isNullOrBlank() && !rUser.isNullOrBlank() && !rToken.isNullOrBlank()) {
            maven {
                name = "repsy.io"
                url = uri(rUrl)
                credentials {
                    username = rUser
                    password = rToken
                }
            }
        }
    }

    publications {
        create<MavenPublication>("plugin") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
            artifact(tasks.named("buildPlugin")) {
                classifier = ""
                extension = "zip"
            }
        }
    }
}

val Project.propertiesLocal: LocalProperties get() = LocalProperties.get(this)

class LocalProperties private constructor(private val project: Project) {
    val prop = Properties().apply {
        val f = project.rootProject.file("local.properties")
        if (f.exists()) {
            f.reader().use { load(it) }
        }
    }

    companion object {
        private lateinit var instance: LocalProperties
        internal fun get(project: Project): LocalProperties {
            if (!::instance.isInitialized) {
                instance = LocalProperties(project)
            }
            return instance
        }
    }

    operator fun get(key: String): String? = prop[key] as? String
}
