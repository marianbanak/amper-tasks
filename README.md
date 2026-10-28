# Amper Tasks for IntelliJ IDEA

> **A native, Gradle-style task tool window for Kotlin Amper projects.**

[![JetBrains Marketplace](https://img.shields.io/badge/JetBrains%20Marketplace-Amper%20Tasks-orange?logo=jetbrains)](https://plugins.jetbrains.com)
[![Compatible IDEs](https://img.shields.io/badge/IntelliJ%20IDEA-2024.2+-blue?logo=intellijidea)](https://plugins.jetbrains.com)
[![Amper Support](https://img.shields.io/badge/Amper-Ready-purple)](https://github.com/JetBrains/amper)
![repsy.io](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.repsy.io%2Fuser42380946%2Fmaven%2Ftk%2Fmallumo%2Famper-tasks%2Fmaven-metadata.xml)
---

Working with [Kotlin Amper](https://github.com/JetBrains/amper) in IntelliJ IDEA? Missing the
familiar, convenient side tool window from Gradle or Maven?

**Amper Tasks** introduces a dedicated **Amper** sidebar window to IntelliJ IDEA. It automatically
scans your project and its modules, groups tasks into clear categories, and lets you execute any
Amper task with a single double-click.

---

## ✨ Why Amper Tasks?

- ⚡ **Zero Configuration**: Open any project with `project.yaml`, `module.yaml`, or `./kotlin`
  wrapper — your tasks are ready instantly.
- 🎯 **Familiar Workflow**: Replicates the intuitive Gradle sidebar experience you already know and
  love.
- 🖱️ **Double-Click Execution**: Double-click any task to run it inside IntelliJ's native **Run**
  console with ANSI colors and stop (`■`) controls.
- 📦 **Smart Multi-Module & Directory Hierarchy**: Run tasks across the entire project or target
  individual submodules (`-m <module>`). Intermediate folders (such as `www-portal`) are cleanly
  organized into directories.
- 🛠️ **Custom Command Groups & Shell Commands**: Add custom groups and commands to any project root,
  directory, or module with multiline shell scripts, custom working directories, and one-click
  execution.
- 👁️ **Hide & Declutter**: Hide individual tasks or entire groups via right-click to keep your
  workspace focused, with a toolbar toggle to view hidden items in gray whenever needed.
- 🚀 **Application Run**: Automatically detects `product: app/...` modules and adds ready-to-run
  execution tasks.
- ☁️ **Auto-Discovered Publishing**: Reads repository definitions in your module files and
  automatically creates publish tasks for `mavenLocal`, Repsy, or custom Maven endpoints.
- 🔍 **Help & Inspection**: Quickly run `show modules`, `show tasks`, `show dependencies`, and
  `show settings` right from the tree.

---

## 🗂️ Task Tree Overview

Amper Tasks organizes your tasks and custom scripts cleanly without cluttered intermediate folders:

```text
Amper (Tool Window)
└── [Project Name]
    ├── Tasks                       <-- Runs across the entire project / all modules
    │   ├── build
    │   │   ├── build               (./kotlin build)
    │   │   └── clean               (./kotlin clean)
    │   ├── verification
    │   │   ├── test                (./kotlin test)
    │   │   └── check               (./kotlin check)
    │   ├── publishing
    │   │   ├── publish mavenLocal  (./kotlin publish mavenLocal)
    │   │   └── publish [repo]      (./kotlin publish [repo])
    │   └── help
    │       ├── show modules        (./kotlin show modules)
    │       ├── show tasks          (./kotlin show tasks)
    │       ├── show dependencies   (./kotlin show dependencies)
    │       └── show settings       (./kotlin show settings)
    │
    ├── [Directory: www-portal]      <-- Multi-module intermediate directory
    │   ├── deploy                  <-- Custom command group (working dir: ./www-portal)
    │   │   └── build-web           (npm run build)
    │   ├── [Module: common]
    │   │   └── build
    │   │       └── build           (./kotlin build -m common)
    │   └── [Module: js]
    │       └── build
    │           └── build           (./kotlin build -m js)
    │
    └── [Module: core]               <-- Direct module tasks (one-click access)
        ├── build
        │   ├── build               (./kotlin build -m core)
        │   └── clean               (./kotlin clean -m core)
        ├── custom-scripts          <-- Custom module commands
        │   └── watch-logs          (tail -f logs/app.log)
        └── verification
            └── test                (./kotlin test -m core)
```

---

## 🚀 How to Use

### 1. Running Built-in Tasks

- **Open Tool Window**: Click the **Amper** tab on the right sidebar strip, or go to **View** →
  **Tool Windows** → **Amper**.
- **Run a Task**: Double-click any task in the tree, select a task and press **Enter**, or select a
  task and click the **Run** (▶️) button on the toolbar.
- **View Output**: The task runs inside IntelliJ's standard **Run** tool window, complete with ANSI
  colored logs and process controls.
- **Reload**: Whenever you edit `project.yaml` or `module.yaml`, click the **Reload** (🔄) button on
  the toolbar to re-scan.

### 2. Custom Groups & Shell Commands

- **Add a Group**: Right-click the project root, any intermediate directory (e.g., `www-portal`), or
  any module, and select **Add Group...**.
- **Add a Command**: Right-click any group (built-in or custom) and select **Add Command...**:
    - **Command Name**: Friendly display title in the tree.
    - **CLI / Shell Command**: Multiline shell script or terminal command (3 visible lines with word
      wrapping).
    - **Working Directory**: Auto-defaults to the target module or directory folder, with a browse
      button for custom paths.
- **Edit / Delete**: Right-click any custom command to edit its command or working directory, or
  delete it when no longer needed.
- **Delete Group**: Right-click any custom group to delete the group along with its commands.

### 3. Hiding & Filtering Tasks

- **Hide Tasks**: Right-click any task and select **Hide Task**.
- **Hide Groups / Directories**: Right-click any group, directory node, or the root `Tasks` node and
  select **Hide Nodes**.
- **Toggle Visibility**: Click the **Show / Hide Hidden Items** (👁️) button on the toolbar. When
  enabled, hidden items appear in gray text; when disabled, they are cleanly filtered out from the
  tree.

---

## 🛠️ Toolbar Controls

| Button | Action                       | Description                                                   |
|:------:|------------------------------|---------------------------------------------------------------|
|   ▶️    | **Run Amper Task**           | Executes the selected task in the tree                        |
|   🔄   | **Reload Amper Tasks**       | Re-scans project and module configurations                    |
|   👁️   | **Show / Hide Hidden Items** | Toggles visibility of hidden tasks & nodes (grayed out state) |
|   ⤢    | **Expand All**               | Expands all tree nodes                                        |
|   ⤡    | **Collapse All**             | Collapses all tree nodes                                      |

---

## 💻 Compatibility & Requirements

- **Supported IDEs**: IntelliJ IDEA (Community & Ultimate) **2024.2** and newer.
- **Supported OS**: Linux, macOS, and Windows.
- **Requirements**: Any Kotlin Amper project (uses project `./kotlin`, `kotlin.bat`, or system
  `amper` command).

---

## 📥 Installation

### From JetBrains Marketplace *(Recommended)*

1. Open IntelliJ IDEA.
2. Go to **Settings** / **Preferences** (`Ctrl + Alt + S` / `Cmd + ,`) → **Plugins**.
3. Select the **Marketplace** tab.
4. Search for **Amper Tasks** and click **Install**.
5. Restart your IDE.

### From Disk (Manual ZIP)

1. Download the latest `amper-tasks-*.zip` release.
2. In IntelliJ IDEA, go to **Settings** → **Plugins** → Gear icon (⚙️) → **Install Plugin from
   Disk...**.
3. Select the ZIP file and restart the IDE.

---

## 💬 Feedback & Issues

Have a feature request, question, or bug report? Contributions and feedback are always welcome
on [GitHub](https://github.com/marianbanak/amper-tasks).
