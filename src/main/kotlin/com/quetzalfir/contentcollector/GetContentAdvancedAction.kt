package com.quetzalfir.contentcollector

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.DumbAware
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
 * Grupo raíz «Get Content (Advanced)».
 * popup="true" se define en plugin.xml.
 */
class GetContentAdvancedAction : ActionGroup(), DumbAware {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible =
            e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.isNotEmpty() == true
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        if (e == null) return emptyArray()

        val editConfigs = object : AnAction("Edit Configs…"), DumbAware {
            override fun getActionUpdateThread() = ActionUpdateThread.BGT
            override fun actionPerformed(ev: AnActionEvent) {
                OptionsDialog().showAndGet()
            }
        }

        val separator = Separator.getInstance()

        val presetActions = ConfigManager.list().map { entry ->
            object : AnAction(entry.name, entry.desc, null), DumbAware {
                override fun getActionUpdateThread() = ActionUpdateThread.BGT
                override fun actionPerformed(ev: AnActionEvent) {
                    val roots = ev.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
                    val out = StringBuilder()
                    val visited: MutableSet<String> = HashSet()

                    val exts = entry.exts.split(',')
                        .map { it.trim().removePrefix(".").lowercase() }
                        .filter { it.isNotEmpty() }
                        .toSet()

                    val excl = entry.excl.split('\n')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    roots.forEach {
                        CollectionUtils.collect(
                            it, out,
                            exts, excl,
                            ev.project ?: return,
                            visited,
                            entry.useGit
                        )
                    }

                    CopyPasteManager.getInstance()
                        .setContents(StringSelection(out.trimEnd('\n').toString()))

                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("contentcollector")
                        .createNotification("Contenido copiado con «${entry.name}»",
                            NotificationType.INFORMATION)
                        .notify(ev.project)
                }
            }
        }

        return (listOf(editConfigs, separator) + presetActions).toTypedArray()
    }

    /* ---------------- diálogo de gestión de presets ------------------ */
    private class OptionsDialog : DialogWrapper(true) {

        private val extField = JBTextField()
        private val exclArea = JTextArea(4, 30).apply {
            lineWrap = true; wrapStyleWord = true
        }
        private val nameField = JBTextField()
        private val descField = JBTextField()
        private val gitChk = JCheckBox("Use .gitignore patterns", true)

        private val listModel = DefaultListModel<ConfigManager.Entry>()
        private val list = JList(listModel).apply {
            cellRenderer = Renderer()
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            addListSelectionListener(SelectionListener())
        }

        init {
            title = "Manage Get Content Presets"
            refreshList(null)
            init()
        }

        override fun createCenterPanel(): JComponent {
            val root = JPanel(BorderLayout(8, 8))
            root.add(JBScrollPane(list), BorderLayout.WEST)
            root.add(buildEditorPanel(), BorderLayout.CENTER)
            root.add(buildButtonsPanel(), BorderLayout.SOUTH)
            return root
        }

        /* ---------------- editor ---------------- */
        private fun buildEditorPanel(): JPanel {
            val grid = JPanel(GridBagLayout())
            val c = GridBagConstraints().apply {
                fill = GridBagConstraints.HORIZONTAL
                gridx = 0; gridy = 0; weightx = 0.0
            }

            fun label(txt: String) = JLabel(txt).also { grid.add(it, c) }
            fun nextRow() { c.gridx = 0; c.gridy++ }

            label("Name:"); c.gridx = 1; c.weightx = 1.0; grid.add(nameField, c); nextRow()
            label("Description:"); c.gridx = 1; grid.add(descField, c); nextRow()
            label("Exts (e.g. .kt,.java):"); c.gridx = 1; grid.add(extField, c); nextRow()
            label("Exclude paths (one per line):"); c.gridx = 1
            grid.add(JBScrollPane(exclArea), c); nextRow()
            c.gridx = 1; grid.add(gitChk, c); nextRow()

            /* explorador de carpetas */
            c.gridx = 1
            grid.add(JButton("Browse…").apply {
                addActionListener {
                    val descriptor = FileChooserDescriptorFactory.createMultipleFoldersDescriptor()
                        .apply { title = "Select Paths to Exclude" }
                    FileChooser.chooseFiles(descriptor, null, null) { files ->
                        files.forEach {
                            if (!exclArea.text.endsWith('\n') && exclArea.text.isNotEmpty())
                                exclArea.append("\n")
                            exclArea.append(it.path)
                        }
                    }
                }
            }, c)

            return grid
        }

        /* ---------------- botones inferiores ---------------- */
        private fun buildButtonsPanel(): JPanel = JPanel().apply {
            /* save */
            add(JButton("+").apply {
                addActionListener {
                    val entry = ConfigManager.Entry(
                        nameField.text.trim().ifBlank { "Unnamed" },
                        extField.text.trim(),
                        exclArea.text.trim(),
                        descField.text.trim(),
                        gitChk.isSelected
                    )
                    ConfigManager.add(entry)
                    refreshList(entry.name)
                }
            })
            /* delete */
            add(JButton("–").apply {
                addActionListener {
                    val sel = list.selectedValue ?: return@addActionListener
                    ConfigManager.remove(sel.name)
                    refreshList(null)
                }
            })
            /* export */
            add(JButton("Export").apply {
                addActionListener {
                    val sel = list.selectedValue ?: return@addActionListener
                    CopyPasteManager.getInstance().setContents(StringSelection(sel.toJson()))
                    notify("Preset «${sel.name}» copied to clipboard")
                }
            })
            /* import */
            add(JButton("Import").apply {
                addActionListener {
                    val txt = CopyPasteManager.getInstance().contents
                        ?.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor)
                        ?.toString() ?: return@addActionListener
                    ConfigManager.Entry.fromJson(txt)?.let {
                        ConfigManager.add(it)
                        refreshList(it.name)
                        fillForm(it)
                        notify("Imported preset «${it.name}»")
                    }
                }
            })
        }

        /* ---------------- helper funcs ---------------- */
        private fun refreshList(selectName: String?) {
            listModel.clear()
            ConfigManager.list().forEach { listModel.addElement(it) }
            if (selectName != null) {
                for (i in 0 until listModel.size) {
                    if (listModel.get(i).name == selectName) {
                        list.selectedIndex = i
                        break
                    }
                }
            } else list.clearSelection()
        }

        private fun fillForm(e: ConfigManager.Entry) {
            nameField.text = e.name
            descField.text = e.desc
            extField.text = e.exts
            exclArea.text = e.excl
            gitChk.isSelected = e.useGit
        }

        private fun notify(msg: String) =
            NotificationGroupManager.getInstance()
                .getNotificationGroup("contentcollector")
                .createNotification(msg, NotificationType.INFORMATION)
                .notify(null)

        /* renderer con tooltip */
        private class Renderer : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): java.awt.Component {
                val c = super.getListCellRendererComponent(
                    list,
                    (value as? ConfigManager.Entry)?.name ?: "",
                    index,
                    isSelected,
                    cellHasFocus
                )
                toolTipText = (value as? ConfigManager.Entry)?.desc
                return c
            }
        }

        private inner class SelectionListener : ListSelectionListener {
            override fun valueChanged(e: ListSelectionEvent) {
                if (e.valueIsAdjusting) return
                list.selectedValue?.let { fillForm(it) }
            }
        }
    }
}
