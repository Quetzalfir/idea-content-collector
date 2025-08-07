# Idea Content Collector

<!-- Plugin description -->
Idea Content Collector is a lightweight utility for all IntelliJ‑based IDEs that
**dumps the full text of any selected file / directory tree to the clipboard**
with one click.

* **Get Content** – quick action, no set‑up.  
  Recurses through the selected nodes, skips symlinks and well‑known build
  folders, and prepends every block with `Path: <relative/path>`.
* **Get Content (Advanced)** – sub‑menu with named *presets*  
  (filter by extension, extra exclusion paths, optional `.gitignore` handling).
  Presets are stored per IDE, can be imported/exported as JSON, and edited
  through a small GUI dialog.
* **.gitignore aware** – when enabled, every `.gitignore` in the project
  hierarchy contributes its rules, so generated artefacts, logs, etc. never
  reach your clipboard.
* Works on Windows, macOS & Linux, against any 2024.3+ IntelliJ Platform IDE
  (IDEA, PyCharm, WebStorm, PhpStorm, GoLand, CLion, Rider…).
<!-- Plugin description end -->

---

<table>
<tr><th>Latest version</th><td><code>1.0.0</code></td></tr>
<tr><th>IntelliJ Platform build</th><td>243.* (2024.3 family)</td></tr>
<tr><th>JDK&nbsp;target</th><td>21</td></tr>
<tr><th>License</th><td>MIT</td></tr>
</table>

---

## 1  Features

| Action                                       | Default shortcut | What it does                                                                                                            |
|----------------------------------------------|------------------|-------------------------------------------------------------------------------------------------------------------------|
| **Get Content**                              | none             | Dump all files beneath the selection. Skips symlinks, `.idea`, `node_modules`, `build`, <br>`out`, `dist`, `venv`, etc. |
| **Get Content (Advanced)** › *Foo*           | none             | Same, but runs through the *Foo* preset (extensions, extra excludes, optional `.gitignore` evaluation).                 |
| **Get Content (Advanced)** › *Edit Configs…* | – | GUI to add / remove / import / export presets. Live change detection enables the **Save** button only when required.    |

---

## 2  Installation

### From JetBrains Marketplace (recommended)

1. **Settings / Preferences ▸ Plugins ▸ Marketplace**.
2. Search for **“Idea Content Collector”**, click *Install* and restart your IDE.

### Manual install

```bash
git clone https://github.com/Quetzalfir/idea-content-collector.git
cd idea-content-collector
./gradlew buildPlugin
# resulting ZIP is under build/distributions/IdeaContentCollector-<ver>.zip
```

Then go to **Settings ▸ Plugins ▸ ⚙ ▸ Install Plugin from Disk…** and point to the ZIP.

---

## 3  Quick start

*Right‑click* any folder or a set of files in the **Project** tool‑window.

```
Get Content                → copies everything     (one‑off)
Get Content (Advanced) ─┐
                        ├─ Edit Configs…          → manage presets
                        ├─ Docs & Sources         → your custom preset
                        └─ just .ts               → another preset
```

The clipboard will contain:

```
Path: my‑project/src/Main.kt
<file content>
---
Path: my‑project/src/utils/Helpers.kt
<file content>
---
```

---

## 4  Presets

| Field              | Example                            | Notes                                                       |
|--------------------|------------------------------------|-------------------------------------------------------------|
| **Name**           | `Docs & Sources`                   | Shown in the sub‑menu.                                      |
| **Description**    | `Markdown + Java sources`          | Appears as tooltip / status bar.                            |
| **Exts**           | `.md,.java,.kt`                    | Leave empty to allow everything.                            |
| **Exclude paths**  | `build/\n.idea/inspectionProfiles` | One per line, relative to project root.                     |
| **Use .gitignore** | ☑️                                 | When ticked, all patterns from every `.gitignore` are used. |

### Import / Export

* **Export** – copies the selected preset as JSON to the clipboard.
* **Import** – reads JSON from the clipboard and adds it as a new preset.

---

## 5  Building / Running from source

```bash
# JDK 21+ and Git installed
./gradlew runIde            # launches IDE sandbox with the plugin
./gradlew buildPlugin       # assembles distributable ZIP
./gradlew verifyPlugin      # Plugin Verifier against all 2024.3 IDEs
./gradlew check             # + unit tests, coverage & static analysis
```

### Continuous Integration

* GitHub Actions workflow **Build** triggers on every push / pull‑request:
  * unit tests ➜ Qodana static analysis ➜ Plugin Verifier ➜ draft release
* When the GitHub release is published, **Release** workflow:
  * signs the plugin (certificate via secrets)
  * uploads to JetBrains Marketplace (token via secrets)
  * bumps the changelog automatically.

---

## 6  Development notes

| Stack element                       | Version / URL                                                                   |
|-------------------------------------|---------------------------------------------------------------------------------|
| IntelliJ Platform **Gradle plugin** | 1.18.+ → tasks `runIde`, `buildPlugin`, `verifyPlugin`                          |
| Kotlin                              | JVM 21 target; stdlib not re‑bundled (`kotlin.stdlib.default.dependency=false`) |
| Code coverage                       | [Kover](https://github.com/Kotlin/kotlinx-kover) + Codecov badge                |
| Static analysis                     | [Qodana](https://www.jetbrains.com/help/qodana/)                                |
| UI tests (optional)                 | [IntelliJ UI Test Robot](https://github.com/JetBrains/intellij-ui-test-robot)   |

---

## 7  Roadmap

- [ ] **Search within clipboard dump**: highlight matches quickly.
- [ ] **Preset sharing** via URL scheme.
- [ ] **Per‑language defaults** (e.g. auto‑create *“TypeScript”* preset in JS projects).  
  Contributions & ideas welcome – see **Contributing** below!

---

## 8  Contributing

1. Fork ➜ feature branch ➜ PR.
2. Run `./gradlew check` – CI will replicate this.
3. Ensure new features include tests or a short demo GIF in the PR.

All PRs must follow the [JetBrains Marketplace *Quality Guidelines*](https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html).

---

## 9  License

```
MIT License

Copyright (c) 2025 Fernando Valencia (Quetzalfir)
…
```

See [LICENSE](LICENSE) for the full text.

---

## 10  Acknowledgements

* Built with the terrific **[IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)**.
* Icons from JetBrains UI pack.

Happy copying! 🚀
