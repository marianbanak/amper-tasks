package tk.mallumo.amper.tasks.model

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

data class CustomCommandData(
    var id: String = "",
    var name: String = "",
    var command: String = "",
    var groupName: String = "",
    var modulePath: String = "",
    var workingDir: String = ""
)

data class CustomGroupData(
    var name: String = "",
    var modulePath: String = ""
)

class CustomTasksState {
    var groups: MutableList<CustomGroupData> = mutableListOf()
    var commands: MutableList<CustomCommandData> = mutableListOf()
    var hiddenKeys: MutableSet<String> = mutableSetOf()
    var showHidden: Boolean = false
}

@Service(Service.Level.PROJECT)
@State(
    name = "AmperCustomTasksService",
    storages = [Storage("amperCustomTasks.xml")]
)
class AmperCustomTasksService(val project: Project) : PersistentStateComponent<CustomTasksState> {

    private var myState = CustomTasksState()

    override fun getState(): CustomTasksState = myState

    override fun loadState(state: CustomTasksState) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    fun getAllCustomGroups(): Map<String, List<String>> {
        return myState.groups.groupBy({ it.modulePath }, { it.name })
    }

    fun getCommands(modulePath: String): List<CustomCommandData> {
        return myState.commands.filter { it.modulePath == modulePath }
    }

    fun getAllCommands(): List<CustomCommandData> {
        return myState.commands.toList()
    }

    fun addGroup(modulePath: String, groupName: String) {
        val trimmed = groupName.trim()
        if (trimmed.isEmpty()) return
        if (myState.groups.none {
                it.modulePath == modulePath && it.name.equals(
                    trimmed,
                    ignoreCase = true
                )
            }) {
            myState.groups.add(CustomGroupData(name = trimmed, modulePath = modulePath))
        }
    }

    fun removeGroup(modulePath: String, groupName: String) {
        myState.groups.removeIf {
            it.modulePath == modulePath && it.name.equals(
                groupName,
                ignoreCase = true
            )
        }
        myState.commands.removeIf {
            it.modulePath == modulePath && it.groupName.equals(
                groupName,
                ignoreCase = true
            )
        }
    }

    fun addOrUpdateCommand(command: CustomCommandData) {
        val existingIndex = myState.commands.indexOfFirst { it.id == command.id }
        if (existingIndex >= 0) {
            myState.commands[existingIndex] = command
        } else {
            myState.commands.add(command)
        }
    }

    fun removeCommand(commandId: String) {
        myState.commands.removeIf { it.id == commandId }
    }

    var showHidden: Boolean
        get() = myState.showHidden
        set(value) {
            myState.showHidden = value
        }

    fun getHiddenKeys(): Set<String> {
        return myState.hiddenKeys.toSet()
    }

    fun isHidden(key: String): Boolean {
        return myState.hiddenKeys.contains(key)
    }

    fun setHidden(key: String, hidden: Boolean) {
        if (hidden) {
            myState.hiddenKeys.add(key)
        } else {
            myState.hiddenKeys.remove(key)
        }
    }

    fun toggleHidden(key: String): Boolean {
        val next = !isHidden(key)
        setHidden(key, next)
        return next
    }

    companion object {
        fun getInstance(project: Project): AmperCustomTasksService {
            return project.getService(AmperCustomTasksService::class.java)
        }
    }
}
