package com.quetzalfir.contentcollector

import com.intellij.ide.util.PropertiesComponent

/** Configuración persistente de presets. */
object ConfigManager {

    private const val KEY = "contentcollector.configs"

    data class Entry(
        var name: String = "",
        var exts: String = "",
        var excl: String = "",
        var desc: String = "",
        var useGit: Boolean = true          // ← flag .gitignore
    ) {
        fun toJson(): String =
            """{"name":"$name","exts":"$exts","excl":"$excl","desc":"$desc","git":$useGit}"""

        companion object {
            /** Convierte JSON plano → Entry (muy simple) */
            fun fromJson(raw: String): Entry? {
                return try {
                    val m = raw.trim()
                        .removePrefix("{").removeSuffix("}")
                        .split("\",\"")
                        .associate {
                            val parts = it.split("\":")
                            val k = parts[0].removePrefix("\"")
                            val v = parts[1].trim('"')
                            k to v
                        }

                    val name = m["name"] ?: return null
                    Entry(
                        name,
                        m["exts"] ?: "",
                        m["excl"] ?: "",
                        m["desc"] ?: "",
                        m["git"]?.toBoolean() ?: true
                    )
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    /* --- almacenamiento --- */
    private fun loadRaw(): MutableList<Entry> =
        PropertiesComponent.getInstance().getValue(KEY, "")
            .split("\u0000").mapNotNull { if (it.isBlank()) null else Entry.fromJson(it) }
            .toMutableList()

    private fun saveRaw(list: List<Entry>) =
        PropertiesComponent.getInstance()
            .setValue(KEY, list.joinToString("\u0000") { it.toJson() })

    /* --- API pública --- */
    fun list(): List<Entry> = loadRaw()

    fun add(e: Entry) {
        val l = loadRaw().filter { it.name != e.name }.toMutableList()
        l.add(e)
        saveRaw(l)
    }

    fun remove(name: String) {
        saveRaw(loadRaw().filter { it.name != name })
    }
}
