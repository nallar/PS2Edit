package net.ps2stats.edit

import com.google.common.base.Charsets
import com.google.common.io.Files
import java.io.File

object ClientConfigUpdater {
	/* Not used - we never do these replacements on live
    private val liveReplacements = checkLengths(mapOf(
            "Address=recap.daybreakgames.com:15081\r\n" to "Address=127.0.0.1:15081   \r\nEnabled=0\r\n",
            "IncludeIndirectMemory=1" to "IncludeIndirectMemory=0",
            "http://www.planetside2.com/error/game.action?code=G" to "https://ps2stats.net/crash/?padding=00000000&code=G",
            "LocalLogLevel=0" to "LocalLogLevel=5"
    ))
    */
	private val replacements = checkLengths(mapOf(
		"Address=recap.daybreakgames.com:15081\r\n" to "Address=127.0.0.1:15081   \r\nEnabled=0\r\n",
		"IncludeIndirectMemory=1" to "IncludeIndirectMemory=0",
		"https://www.planetside2.com/error/game.action?code=G" to "https://ps2stats.net/crash/?padding=000000000&code=G",
		"LocalLogLevel=0" to "LocalLogLevel=5"
	))
	private val inverse = {
		val t = mutableMapOf<String, String>()
		replacements.forEach { t[it.value] = it.key }
		t
	}()

	private fun checkLengths(map: Map<String, String>): Map<String, String> {
		for ((k, v) in map)
			if (k.length != v.length)
				error("Lengths of replacements must match. Got ${k.length} and ${v.length}\n$k\n$v")
		return map
	}

	fun revertChanges(clientConfig: File) {
		replaceWithoutModified(clientConfig, inverse, true)
	}

	fun applyChanges(clientConfig: File) {
		replaceWithoutModified(clientConfig, replacements, false)
	}

	private fun replaceWithoutModified(file: File, replacements: Map<String, String>, allowMissing: Boolean) {
		var content = Files.toString(file, Charsets.UTF_8)
		replacements.forEach {
			if (!allowMissing && !content.contains(it.key))
				error("Couldn't find '${it.key}' in client config file")

			content = content.replace(it.key, it.value)
		}
		val lastModified = file.lastModified()
		val lastLength = file.length()
		Files.write(content, file, Charsets.UTF_8)
		if (!file.setLastModified(lastModified) || file.length() != lastLength)
			error("Failed to unmark file $file as modified.")
	}
}
