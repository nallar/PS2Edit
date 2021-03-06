package net.ps2stats.locale

import java.io.File

class LocaleReplacements {
	val textSubstitutions = mutableMapOf<String, String>()
	val textSubstitutionsCaseSensitive = mutableMapOf<String, String>()
	val textSubstitutionsExact = mutableMapOf<String, String>()
	val idSubstitutions = mutableMapOf<Long, String>()

	fun load(file: File) {
		val default = File(file, "default.cfg")
		if (!default.exists())
			default.writeText("""#Locale Substitutions
#Format:
#ID:text
# OR
#sub:
#from
#to

""")

		val files = file.listFiles(File::isFile)
		files.forEach { file ->
			var inSub = 0
			var caseSensitive = false
			var caseSensitiveAuto = false
			var isExact = false
			var from = ""
			file.forEachLine {
				var line = it.trim('\t')
				val trimmed = line.trim()
				if (trimmed.isEmpty() || trimmed[0] == '#')
					return@forEachLine

				if (inSub > 0) {
					line = line.replace("\\n", "\r\n")
					if (inSub == 2) {
						from = line
					} else {
						if (caseSensitiveAuto) {
							textSubstitutionsCaseSensitive[from] = line
							textSubstitutionsCaseSensitive[from.toUpperCase()] = line.toUpperCase()
							textSubstitutionsCaseSensitive[from.toLowerCase()] = line.toLowerCase()
						} else {
							(if (caseSensitive)
								textSubstitutionsCaseSensitive
							else if (isExact)
								textSubstitutionsExact
							else textSubstitutions
								)[from] = line
						}
						from = ""
					}
					if (--inSub == 0) {
						caseSensitive = false
						caseSensitiveAuto = false
						isExact = false
					}

					return@forEachLine
				}
				if (trimmed == "sub:") {
					inSub = 2
					return@forEachLine
				}
				if (trimmed == "cssub:") {
					inSub = 2
					caseSensitive = true
					return@forEachLine
				}
				if (trimmed == "casub:") {
					inSub = 2
					caseSensitiveAuto = true
					return@forEachLine
				}
				if (trimmed == "exsub:") {
					inSub = 2
					isExact = true
					return@forEachLine
				}
				val index = line.indexOf(':')
				if (index == -1)
					error("Expected ':' in $line")

				val id = line.substring(0, index)
				val replacement = line.substring(index + 1)
				idSubstitutions[id.toLong()] = replacement.replace("\\n", "\r\n")
			}
			if (inSub > 0)
				error("Unmatched substitution")
		}
	}

	fun save(file: File) {
		file.bufferedWriter().use { writer ->
			textSubstitutions.forEach {
				writer.write("sub:\r\n\t${it.key.replace("\r\n", "\\n")}\r\n\t${it.value.replace("\r\n", "\\n")}\r\n")
			}
			textSubstitutionsCaseSensitive.forEach {
				writer.write("cssub:\r\n\t${it.key.replace("\r\n", "\\n")}\r\n\t${it.value.replace("\r\n", "\\n")}\r\n")
			}
			idSubstitutions.forEach {
				writer.write("${it.key}:${it.value.replace("\r\n", "\\n")}\r\n")
			}
		}
	}

	fun replace(localeFile: File) {
		val locale = Locale()
		locale.load(localeFile)
		locale.entries.forEach { entry ->
			val text = entry.text
			var newText = text
			textSubstitutionsExact.forEach {
				if (newText == it.key)
					newText = it.value
			}
			textSubstitutions.forEach {
				newText = newText.replace(it.key, it.value, true)
			}
			textSubstitutionsCaseSensitive.forEach {
				newText = newText.replace(it.key, it.value)
			}
			if (newText != text) {
				println("Changed from $text to $newText")
				entry.text = newText
			}
		}
		idSubstitutions.forEach {
			val entry = locale.entriesById[it.key] ?: error("Couldn't find entry with ID ${it.key}")
			entry.text = it.value
		}
		locale.save(localeFile)
	}
}
