package com.quetzalfir.contentcollector

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Rutinas comunes para recorrer archivos.
 */
object CollectionUtils {

    /* --------- defaults --------- */
    private val DEFAULT_EXCLUDES = setOf(
        ".git", ".idea", ".gradle", "node_modules", "build", "out",
        "target", "dist", "generated", "venv"
    )

    /* --------- API principal --------- */
    fun collect(
        vf: VirtualFile,
        out: StringBuilder,
        extensions: Set<String>,
        userExcludes: List<String>,
        project: Project,
        visited: MutableSet<String>,
        useGitIgnores: Boolean = true
    ) {
        val path = vf.path
        if (!visited.add(path)) return
        if (Files.isSymbolicLink(Paths.get(path))) return

        val basePath = project.basePath ?: return
        val base: Path = Paths.get(basePath)
        val rel: Path = try { base.relativize(Paths.get(path)) } catch (_: Exception) { return }

        /* skip directorios */
        if (shouldSkip(rel, userExcludes, project, useGitIgnores)) return

        val okExt = extensions.isEmpty() ||
                extensions.contains(vf.extension?.lowercase())

        if (vf.isDirectory) {
            vf.children?.forEach {
                collect(it, out, extensions, userExcludes, project, visited, useGitIgnores)
            }
        } else if (okExt) {
            dumpFile("${project.name}/${rel.toString().replace('\\', '/')}", vf, out)
        }
    }

    /* --------- helpers internos --------- */

    private fun shouldSkip(
        rel: Path,
        userExcl: List<String>,
        project: Project,
        useGit: Boolean
    ): Boolean {
        val norm = rel.toString().replace('\\', '/')
        val first = norm.substringBefore('/')
        if (first in DEFAULT_EXCLUDES) return true
        if (userExcl.any { norm.startsWith(it.replace('\\', '/')) }) return true

        if (useGit) {
            val patterns = GitIgnoreCache.get(project)
            return patterns.any { GitIgnoreMatcher.matches(norm, it) }
        }
        return false
    }

    private fun dumpFile(displayPath: String, vf: VirtualFile, out: StringBuilder) {
        out.appendLine("Path: $displayPath")
        try {
            out.appendLine(String(vf.contentsToByteArray(), StandardCharsets.UTF_8))
        } catch (t: Throwable) {
            out.appendLine("[ERROR] Could not read $displayPath → ${t.message}")
        }
        out.appendLine("---")
    }

    fun absolutetoRel(project: Project, abs: String): String {
        val base = project.basePath ?: return abs
        return try {
            Paths.get(base).relativize(Paths.get(abs)).toString()
                .replace('\\', '/')
        } catch (_: Exception) {
            abs
        }
    }
}

/* ------------------------------------------------------------------------ */
/* Git‑ignore helpers (muy simple, suficiente para la mayoría de proyectos) */
/* ------------------------------------------------------------------------ */

private object GitIgnoreCache {
    /** patrones cacheados por proyecto */
    private val cache = mutableMapOf<String, Set<String>>()

    fun get(project: Project): Set<String> =
        cache.getOrPut(project.basePath ?: "") { load(project) }

    private fun load(project: Project): Set<String> {
        val root = VfsUtilCore.virtualToIoFile(project.baseDir)
        val basePath = root.toPath()
        val patterns = mutableSetOf<String>()

        root.walkTopDown()
            .filter { it.name == ".gitignore" }
            .forEach { gitFile ->
                val dirRel = basePath.relativize(gitFile.parentFile.toPath())
                    .toString()
                    .replace('\\', '/')
                    .let { if (it.isEmpty()) "" else "$it/" }

                gitFile.readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .forEach { line ->
                        // Prefija la ruta del .gitignore, excepto si la regla es absoluta
                        patterns += if (line.startsWith("/")) {
                            line.removePrefix("/").replace('\\', '/')
                        } else {
                            (dirRel + line).replace('\\', '/')
                        }
                    }
            }
        return patterns
    }
}

/** Comprobación muy básica : solo soporta prefijos y sufijos ‘*’. */
private object GitIgnoreMatcher {
    fun matches(path: String, pattern: String): Boolean {
        val p = pattern.replace("\\", "/")
        return when {
            p.startsWith("*") && p.endsWith("*") ->
                path.contains(p.removePrefix("*").removeSuffix("*"))
            p.startsWith("*") ->
                path.endsWith(p.removePrefix("*"))
            p.endsWith("*") ->
                path.startsWith(p.removeSuffix("*"))
            else -> path == p || path.startsWith("$p/")
        }
    }
}
