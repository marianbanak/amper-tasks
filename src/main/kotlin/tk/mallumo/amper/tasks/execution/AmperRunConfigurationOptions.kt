package tk.mallumo.amper.tasks.execution

import com.intellij.execution.configurations.LocatableRunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

class AmperRunConfigurationOptions : LocatableRunConfigurationOptions() {
    private val myCommandArgs: StoredProperty<String?> =
        string("").provideDelegate(this, "commandArgs")
    private val myModuleName: StoredProperty<String?> =
        string("").provideDelegate(this, "moduleName")

    var commandArgs: String?
        get() = myCommandArgs.getValue(this)
        set(value) = myCommandArgs.setValue(this, value)

    var moduleName: String?
        get() = myModuleName.getValue(this)
        set(value) = myModuleName.setValue(this, value)
}
