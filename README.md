# Amper Tasks for IntelliJ IDEA

> **A native, Gradle-style task tool window for Kotlin Amper projects.**

[![JetBrains Marketplace](https://img.shields.io/badge/JetBrains%20Marketplace-Amper%20Tasks-orange?logo=jetbrains)](https://plugins.jetbrains.com)
[![Compatible IDEs](https://img.shields.io/badge/IntelliJ%20IDEA-2024.2+-blue?logo=intellijidea)](https://plugins.jetbrains.com)
[![Amper Support](https://img.shields.io/badge/Amper-Ready-purple)](https://github.com/JetBrains/amper)

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
- 📦 **Smart Multi-Module Support**: Run tasks across the entire project or target individual
  submodules (`-m <module>`) with zero typing.
- 🚀 **Application Run**: Automatically detects `product: app/...` modules and adds ready-to-run
  execution tasks.
- ☁️ **Auto-Discovered Publishing**: Reads repository definitions in your module files and
  automatically creates publish tasks for `mavenLocal`, Repsy, or custom Maven endpoints.
- 🔍 **Help & Inspection**: Quickly run `show modules`, `show tasks`, `show dependencies`, and
  `show settings` right from the tree.

---

## 🗂️ Task Tree Overview

Amper Tasks organizes your tasks cleanly without cluttered intermediate folders:

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
    ├── [Module: core]               <-- Direct module tasks (one-click access)
    │   ├── build
    │   │   ├── build               (./kotlin build -m core)
    │   │   └── clean               (./kotlin clean -m core)
    │   ├── verification
    │   │   └── test                (./kotlin test -m core)
    │   └── help
    │       └── show dependencies   (./kotlin show dependencies -m core)
    │
    └── [Module: app]
        ├── build
        │   └── build               (./kotlin build -m app)
        ├── run
        │   └── run                 (./kotlin run -m app)
        └── verification
            └── test                (./kotlin test -m app)
```

---

## 🚀 How to Use

1. **Open the Amper Tool Window**:
    - Click the **Amper** tab on the right sidebar strip, or go to **View** → **Tool Windows** →
      **Amper**.
2. **Run a Task**:
    - **Double-click** any task in the tree, OR
    - Select a task and press **Enter**, OR
    - Select a task and click the **Run** (▶️) button on the toolbar.
3. **View Output**:
    - The task runs inside IntelliJ's standard **Run** tool window, complete with full ANSI colored
      logs, links, and termination buttons.
4. **Refresh**:
    - Whenever you edit `project.yaml` or `module.yaml`, click the **Reload** (🔄) button on the
      toolbar to re-scan.

---

## 🛠️ Toolbar Controls

| Button | Action                 | Description                                |
|:------:|------------------------|--------------------------------------------|
|   ▶️    | **Run Amper Task**     | Executes the selected task in the tree     |
|   🔄   | **Reload Amper Tasks** | Re-scans project and module configurations |
|   ⤢    | **Expand All**         | Expands all tree nodes                     |
|   ⤡    | **Collapse All**       | Collapses all tree nodes                   |

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
on [GitHub](https://github.com/mallumo/amper-tasks).
