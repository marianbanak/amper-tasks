package tk.mallumo.amper.tasks.execution

import com.intellij.execution.configurations.LocatableRunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

class AmperRunConfigurationOptions : LocatableRunConfigurationOptions() {
    private val myCommandArgs: StoredProperty<String?> =
        string("").provideDelegate(this, "commandArgs")
    private val myModuleName: StoredProperty<String?> =
        string("").provideDelegate(this, "moduleName")
    private val myIsCustom: StoredProperty<Boolean> =
        property(false).provideDelegate(this, "isCustom")
    private val myRawCommandLine: StoredProperty<String?> =
        string("").provideDelegate(this, "rawCommandLine")
    private val myWorkingDir: StoredProperty<String?> =
        string("").provideDelegate(this, "workingDir")

    var commandArgs: String?
        get() = myCommandArgs.getValue(this)
        set(value) = myCommandArgs.setValue(this, value)

    var moduleName: String?
        get() = myModuleName.getValue(this)
        set(value) = myModuleName.setValue(this, value)

    var isCustom: Boolean
        get() = myIsCustom.getValue(this)
        set(value) = myIsCustom.setValue(this, value)

    var rawCommandLine: String?
        get() = myRawCommandLine.getValue(this)
        set(value) = myRawCommandLine.setValue(this, value)

    var workingDir: String?
        get() = myWorkingDir.getValue(this)
        set(value) = myWorkingDir.setValue(this, value)
}
