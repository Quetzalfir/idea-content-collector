# Idea Content Collector – Changelog
All notable changes to **Idea Content Collector** will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
and the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format.

---

## [Unreleased]
### Added
- **Settings auto‑save indicator** – the **Save** button is now highlighted only when the selected preset has unsaved changes.
- `.gitignore` **recursive support** – patterns from every `.gitignore` file in the project tree are now honoured, not just the root one.
- Convenience shortcut for **“Get Content (last preset)”** *(default: none, configure in Keymap)*.

### Changed
- Preset **Save** now updates the currently selected entry instead of creating a duplicate.
- Exported preset JSON is now indented for readability.
- Internal refactor: unified path handling via `CollectionUtils.absolutetoRel`.

### Fixed
- Endless loop on circular symlinks on some Windows set‑ups.
- Preset import edge‑case when clipboard contained trailing newline.
- Minor UI glitches under the new UI theme (2024.3).

---

## [1.0.0] – 2025‑08‑06
### Added
- **Get Content** action – instant clipboard dump of every selected file/directory (relative paths, ANSI UTF‑8).
- **Get Content (Advanced)** action with **named presets**:
    - Filter by **file extension list** (e.g. `.kt,.java`).
    - Additional **exclude paths** (one per line, relative).
    - Optional **global .gitignore evaluation** (all nested `.gitignore` files).
    - GUI manager with **import/export JSON**, add/remove, clipboard integration.
- Automatic exclusion of common build folders (`.idea`, `.gradle`, `node_modules`, `build`, `out`, `dist`, `venv`, etc.).
- Cross‑platform compatibility: Windows, macOS & Linux.
- CI/CD: GitHub Actions pipeline with tests, Qodana analysis, Plugin Verifier, signed release to JetBrains Marketplace.

