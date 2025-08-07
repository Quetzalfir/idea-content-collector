package com.quetzalfir.contentcollector

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.datatransfer.StringSelection
import java.util.*
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

/**
 * Sub‑menú «Get Content (Advanced)».
 */
class GetContentAdvancedAction : ActionGroup(), DumbAware {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible =
            e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.isNotEmpty() == true
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        if (e == null) return emptyArray()

        /* ---------- opción “Edit Configs…” ---------- */
        val editConfigs = object : AnAction("Edit Configs…"), DumbAware {
            override fun getActionUpdateThread() = ActionUpdateThread.BGT
            override fun actionPerformed(ev: AnActionEvent) {
                OptionsDialog(ev.project).showAndGet()
            }
        }

        /* ---------- presets existentes ---------- */
        val presets = ConfigManager.list().map { entry ->
            object : AnAction(entry.name, entry.desc, null), DumbAware {
                override fun getActionUpdateThread() = ActionUpdateThread.BGT
                override fun actionPerformed(ev: AnActionEvent) {
                    val roots = ev.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
                    val out = StringBuilder()
                    val visited: MutableSet<String> = HashSet()

                    val exts = entry.exts.split(',').map { it.trim().removePrefix(".").lowercase() }.filter { it.isNotEmpty() }.toSet()
                    val excl = entry.excl.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

                    roots.forEach {
                        CollectionUtils.collect(
                            it, out,
                            exts, excl,
                            ev.project ?: return,
                            visited,
                            entry.useGit
                        )
                    }

                    CopyPasteManager.getInstance().setContents(StringSelection(out.trimEnd('\n').toString()))
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("contentcollector")
                        .createNotification("Contenido copiado con «${entry.name}»", NotificationType.INFORMATION)
                        .notify(ev.project)
                }
            }
        }

        return (listOf(editConfigs, Separator.getInstance()) + presets).toTypedArray()
    }

    /* ====================================================================== */
    /* ===================  Diálogo de gestión de presets  ================== */
    /* ====================================================================== */
    /* ====================================================================== */
    /* ===================  Diálogo de gestión de presets  ================== */
    /* ====================================================================== */
    private class OptionsDialog(private val project: Project?) : DialogWrapper(true) {

        /* --------------- widgets y estado --------------- */
        private val listModel = DefaultListModel<ConfigManager.Entry>()
        private val list = JList(listModel).apply {
            cellRenderer = Renderer()
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            addListSelectionListener(SelectionListener())
        }

        private val nameField = JBTextField()
        private val descField = JBTextField()
        private val extField  = JBTextField()
        private val exclArea  = JTextArea(4, 30).apply { lineWrap = true; wrapStyleWord = true }
        private val gitChk    = JCheckBox("Use .gitignore patterns", true)

        /** copia de la entrada mostrada ‑ sirve para detectar cambios */
        private var original: ConfigManager.Entry? = null

        init {
            title = "Manage Get Content Presets"
            okAction.putValue(Action.NAME, "Save")          // ← botón OK → “Save”
            setOkState(false)                               // disabled hasta que haya cambios
            addChangeTrackers()
            refreshList(null)
            init()
        }

        /* ---------------- layout principal ---------------- */
        override fun createCenterPanel(): JComponent {
            val root = JPanel(BorderLayout(8, 8))
            root.add(buildLeftPanel(), BorderLayout.WEST)
            root.add(buildEditorPanel(), BorderLayout.CENTER)
            return root
        }

        /* ---------------- lógica de guardado ---------------- */
        override fun doOKAction() {
            val entry = currentEntry()

            /* si el nombre cambió, borra el preset original antes de agregar */
            if (original != null && original!!.name != entry.name) {
                ConfigManager.remove(original!!.name)
            }

            ConfigManager.add(entry)        // actualiza o crea
            original = entry.copy()          // nueva base para detectar cambios
            setOkState(false)                // gris nuevamente
            refreshList(entry.name)
        }

        /* ============ panel izquierdo (lista + toolbar) ============ */
        private fun buildLeftPanel(): JComponent {
            val left = JPanel(BorderLayout())
            left.add(JBScrollPane(list), BorderLayout.CENTER)

            val bar = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)

                /* + : nuevo preset vacío */
                add(JButton("+").apply {
                    addActionListener {
                        val candidate = generateName()
                        val e = ConfigManager.Entry(candidate, "", "", "", true)
                        ConfigManager.add(e)
                        refreshList(candidate)
                        fillForm(e)
                    }
                })

                /* – : borrar */
                add(JButton("–").apply {
                    addActionListener {
                        list.selectedValue?.let {
                            ConfigManager.remove(it.name)
                            refreshList(null)
                            clearForm()
                        }
                    }
                })

                /* ↗ export */
                add(JButton("↗").apply {
                    toolTipText = "Export preset to clipboard"
                    addActionListener {
                        list.selectedValue?.let {
                            CopyPasteManager.getInstance().setContents(StringSelection(it.toJson()))
                            notify("Preset «${it.name}» copied")
                        }
                    }
                })

                /* ↙ import */
                add(JButton("↙").apply {
                    toolTipText = "Import preset from clipboard"
                    addActionListener {
                        val txt = CopyPasteManager.getInstance().contents
                            ?.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor)
                            ?.toString() ?: return@addActionListener
                        ConfigManager.Entry.fromJson(txt)?.let {
                            ConfigManager.add(it)
                            refreshList(it.name)
                            fillForm(it)
                            notify("Imported «${it.name}»")
                        }
                    }
                })
            }
            left.add(bar, BorderLayout.NORTH)
            return left
        }

        /* ============ editor central ============ */
        private fun buildEditorPanel(): JPanel {
            val g = JPanel(GridBagLayout())
            val c = GridBagConstraints().apply { fill = GridBagConstraints.HORIZONTAL; gridx = 0; gridy = 0 }

            fun row(label: String, comp: JComponent) {
                g.add(JLabel(label), c)
                c.gridx = 1; c.weightx = 1.0
                g.add(comp, c)
                c.gridx = 0; c.gridy++; c.weightx = 0.0
            }

            row("Name:", nameField)
            row("Description:", descField)
            row("Exts (e.g. .kt,.java):", extField)

            g.add(JLabel("Exclude paths (one per line):"), c)
            c.gridx = 1; g.add(JBScrollPane(exclArea), c); c.gridx = 0; c.gridy++

            c.gridx = 1; g.add(gitChk, c); c.gridx = 0; c.gridy++

            /* Browse… */
            c.gridx = 1
            g.add(JButton("Browse…").apply {
                addActionListener {
                    val descr = FileChooserDescriptorFactory.createMultipleFoldersDescriptor()
                        .apply { title = "Select Paths to Exclude" }
                    FileChooser.chooseFiles(descr, project, null) { files ->
                        files.forEach {
                            val rel = project?.let { p -> CollectionUtils.absolutetoRel(p, it.path) } ?: it.path
                            if (exclArea.text.isNotEmpty() && !exclArea.text.endsWith('\n')) exclArea.append("\n")
                            exclArea.append(rel)
                        }
                    }
                }
            }, c)

            return g
        }

        /* -------------------- helpers -------------------- */
        private fun generateName(): String {
            val base = "Preset"
            var idx = 1
            var candidate = base
            while (ConfigManager.list().any { it.name == candidate }) { idx++; candidate = "$base $idx" }
            return candidate
        }

        private fun addChangeTrackers() {
            val l: () -> Unit = { setOkState(original == null || currentEntry() != original) }
            listOf(nameField, descField, extField).forEach { it.document.addDocumentListener(SimpleListener { l() }) }
            exclArea.document.addDocumentListener(SimpleListener { l() })
            gitChk.addChangeListener { l() }
        }

        private fun setOkState(enabled: Boolean) {
            okAction.isEnabled = enabled
        }

        private fun currentEntry() = ConfigManager.Entry(
            nameField.text.trim(),
            extField.text.trim(),
            exclArea.text.trim(),
            descField.text.trim(),
            gitChk.isSelected
        )

        private fun refreshList(selectName: String?) {
            listModel.removeAllElements()
            ConfigManager.list().forEach { listModel.addElement(it) }
            if (selectName != null) {
                (0 until listModel.size).firstOrNull { listModel[it].name == selectName }?.let { list.selectedIndex = it }
            } else list.clearSelection()
        }

        private fun fillForm(e: ConfigManager.Entry) {
            original = e.copy()
            nameField.text = e.name
            descField.text = e.desc
            extField.text  = e.exts
            exclArea.text  = e.excl
            gitChk.isSelected = e.useGit
            setOkState(false)
        }

        private fun clearForm() {
            original = null
            listOf(nameField, descField, extField).forEach { it.text = "" }
            exclArea.text = ""
            gitChk.isSelected = true
            setOkState(false)
        }

        private fun notify(msg: String) =
            NotificationGroupManager.getInstance()
                .getNotificationGroup("contentcollector")
                .createNotification(msg, NotificationType.INFORMATION)
                .notify(project)

        /* ---- renderer y listeners auxiliares ---- */
        private class Renderer : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): java.awt.Component {
                val c = super.getListCellRendererComponent(list, (value as? ConfigManager.Entry)?.name ?: "", index, isSelected, cellHasFocus)
                toolTipText = (value as? ConfigManager.Entry)?.desc
                return c
            }
        }
        private inner class SelectionListener : ListSelectionListener {
            override fun valueChanged(e: ListSelectionEvent) { if (!e.valueIsAdjusting) list.selectedValue?.let { fillForm(it) } }
        }
        private class SimpleListener(private val run: () -> Unit) : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent) = run()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent) = run()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent) = run()
        }
    }
}
