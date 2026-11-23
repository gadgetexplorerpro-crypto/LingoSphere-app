package com.example.speech

/**
 * Sanitizes literary, Markdown, HTML, and underscored strings strictly for spoken TTS output
 * so screen/reader text stays intact visually while TTS never reads symbols like "underscore"
 * or "asterisk" aloud.
 */
object NarrationTextCleaner {
  private val dividerRegex = Regex("""(?m)^\s*([_\-*=~])\1{2,}\s*$""")
  private val inlineDividerRegex = Regex("""_{2,}|-{3,}|\*{3,}""")
  private val fencedCodeRegex = Regex("""```[\s\S]*?```""")
  private val markdownHeadingRegex = Regex("""(?m)^\s{0,3}#{1,6}\s+""")
  private val blockquoteRegex = Regex("""(?m)^\s{0,3}>\s?""")
  private val bulletRegex = Regex("""(?m)^\s*[-*•]\s+""")
  private val markdownLinkRegex = Regex("""\[([^\]]+)]\(([^)]+)\)""")
  private val htmlTagRegex = Regex("""<[^>]+>""")
  private val boldItalicAsteriskRegex = Regex("""\*{1,3}([^*\n]+?)\*{1,3}""")
  private val boldUnderlineRegex = Regex("""__([^_\n]+?)__""")
  private val italicUnderlineRegex = Regex("""(?<![A-Za-z0-9])_([^_\n]+?)_(?![A-Za-z0-9])""")
  private val strikeRegex = Regex("""~~([^~\n]+?)~~""")
  private val inlineCodeRegex = Regex("""`([^`]+?)`""")
  private val snakeCaseUnderscoreRegex = Regex("""(?<=[A-Za-z0-9])_(?=[A-Za-z0-9])""")
  private val remainingUnderscoresRegex = Regex("""_+""")
  private val strayMarkdownSymbolsRegex = Regex("""[*`~]+""")
  private val multiHorizontalWhitespaceRegex = Regex("""[ \t]{2,}""")
  private val multiNewlineRegex = Regex("""\s*\n+\s*""")

  private val bracketFootnoteRegex = Regex("""\[\s*\d{1,4}\s*]""")
  private val doubleHyphenRegex = Regex("""\s*--\s*""")
  private val numericHtmlEntityRegex = Regex("""&#(x?[0-9A-Fa-f]+);""")

  fun cleanForTts(rawText: String): String {
    if (rawText.isBlank()) return ""
    var cleaned = rawText
      .replace("\r\n", "\n")
      .replace("\r", "\n")

    // 1. Decode common HTML entities and numeric entities before tag stripping
    cleaned = cleaned
      .replace("&nbsp;", " ")
      .replace("&amp;", "&")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&quot;", "\"")
      .replace("&#39;", "'")
      .replace("&apos;", "'")
      .replace("&rsquo;", "’")
      .replace("&lsquo;", "‘")
      .replace("&rdquo;", "”")
      .replace("&ldquo;", "“")
      .replace("&mdash;", " — ")
      .replace("&ndash;", " – ")
      .replace("&hellip;", "...")
    cleaned = numericHtmlEntityRegex.replace(cleaned) { match ->
      val code = match.groupValues[1]
      val cp = if (code.startsWith("x", ignoreCase = true)) {
        code.drop(1).toIntOrNull(16)
      } else {
        code.toIntOrNull(10)
      }
      if (cp != null && cp in 32..0x10FFFF) String(Character.toChars(cp)) else " "
    }

    // 2. Remove HTML tags, footnote brackets [12], & decorative dividers
    cleaned = htmlTagRegex.replace(cleaned, " ")
    cleaned = bracketFootnoteRegex.replace(cleaned, " ")
    cleaned = dividerRegex.replace(cleaned, " ")

    // 3. Remove Markdown wrappers while keeping readable words
    cleaned = fencedCodeRegex.replace(cleaned) { m ->
      m.value.removePrefix("```").removeSuffix("```").trim()
    }
    cleaned = markdownLinkRegex.replace(cleaned, "$1")
    cleaned = markdownHeadingRegex.replace(cleaned, "")
    cleaned = blockquoteRegex.replace(cleaned, "")
    cleaned = bulletRegex.replace(cleaned, "")
    cleaned = inlineCodeRegex.replace(cleaned, "$1")
    cleaned = boldItalicAsteriskRegex.replace(cleaned, "$1")
    cleaned = boldUnderlineRegex.replace(cleaned, "$1")
    cleaned = italicUnderlineRegex.replace(cleaned, "$1")
    cleaned = strikeRegex.replace(cleaned, "$1")

    // 4. Remove inline divider runs, convert "--" pauses & snake_case underscores cleanly
    cleaned = inlineDividerRegex.replace(cleaned, " ")
    cleaned = doubleHyphenRegex.replace(cleaned, " — ")
    cleaned = snakeCaseUnderscoreRegex.replace(cleaned, " ")
    cleaned = remainingUnderscoresRegex.replace(cleaned, " ")
    cleaned = strayMarkdownSymbolsRegex.replace(cleaned, "")

    // 5. Collapse whitespace cleanly while preserving sentence punctuation & math symbols (+, =, %)
    cleaned = multiNewlineRegex.replace(cleaned, " ")
    cleaned = multiHorizontalWhitespaceRegex.replace(cleaned, " ")
    return cleaned.trim()
  }
}
