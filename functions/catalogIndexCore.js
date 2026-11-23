"use strict";

/**
 * LingoSphere Backend Catalog Index Service
 *
 * Fetches, normalizes, deduplicates, and caches book metadata from:
 * - Project Gutenberg via Gutendex (https://gutendex.com/books/)
 * - LibriVox Public Domain Audiobooks (https://librivox.org/api/feed/audiobooks)
 *
 * Stores METADATA ONLY (never downloads full book texts just to list books).
 * Required index fields per entry:
 * - sourceId
 * - title
 * - author
 * - language
 * - subjects
 * - formats
 * - coverUrl
 * - hasFullText
 * - hasHumanAudio
 * - sourceUrl
 */

const GUTENDEX_BASE_URL = "https://gutendex.com/books/";
const LIBRIVOX_BASE_URL = "https://librivox.org/api/feed/audiobooks";

function cleanGutenbergTitle(rawTitle) {
  if (!rawTitle || typeof rawTitle !== "string") return "Untitled Book";
  return rawTitle
    .replace(/\r?\n/g, " ")
    .replace(/\s+/g, " ")
    .replace(/\s*:\s*\$b\s*/gi, ": ")
    .trim();
}

function cleanTitleForAlphabeticalSort(title) {
  if (!title || typeof title !== "string") return "";
  return title
    .trim()
    .replace(/^["'“‘(\[¡¿]+/, "")
    .trim();
}

function extractFirstLetter(title) {
  const cleaned = cleanTitleForAlphabeticalSort(title);
  if (!cleaned) return "#";
  const first = cleaned.charAt(0).toUpperCase();
  return first >= "A" && first <= "Z" ? first : "#";
}

function normalizeComparisonKey(text) {
  if (!text || typeof text !== "string") return "";
  return text
    .toLowerCase()
    .replace(/^(the|a|an)\s+/i, "")
    .replace(/[^a-z0-9]+/g, " ")
    .trim();
}

function formatGutendexAuthorName(rawName) {
  if (!rawName || typeof rawName !== "string") return "Classic Author";
  const parts = rawName.split(",").map((s) => s.trim()).filter(Boolean);
  if (parts.length === 2) {
    return `${parts[1]} ${parts[0]}`.trim();
  }
  return rawName.trim();
}

function selectPreferredTextUrl(gutendexId, formats) {
  if (formats && typeof formats === "object") {
    if (formats["text/plain; charset=utf-8"] && !formats["text/plain; charset=utf-8"].endsWith(".zip")) {
      return formats["text/plain; charset=utf-8"];
    }
    for (const [mime, url] of Object.entries(formats)) {
      if (typeof url === "string" && mime.toLowerCase().startsWith("text/plain") && !url.toLowerCase().endsWith(".zip")) {
        return url;
      }
    }
    if (typeof formats["application/epub+zip"] === "string") {
      return formats["application/epub+zip"];
    }
    for (const [mime, url] of Object.entries(formats)) {
      if (typeof url === "string" && mime.toLowerCase().startsWith("text/html") && !url.toLowerCase().endsWith(".zip")) {
        return url;
      }
    }
  }
  if (gutendexId && Number(gutendexId) > 0) {
    return `https://www.gutenberg.org/ebooks/${gutendexId}.txt.utf-8`;
  }
  return "";
}

function normalizeGutendexRecord(item) {
  if (!item || !item.id) return null;
  const gId = Number(item.id);
  const title = cleanGutenbergTitle(item.title || `Book ${gId}`);
  const firstAuthor = Array.isArray(item.authors) && item.authors.length > 0
    ? formatGutendexAuthorName(item.authors[0].name)
    : "Classic Author";
  const languages = Array.isArray(item.languages) && item.languages.length > 0
    ? item.languages.map((l) => String(l).toLowerCase())
    : ["en"];
  const subjects = [
    ...(Array.isArray(item.subjects) ? item.subjects : []),
    ...(Array.isArray(item.bookshelves) ? item.bookshelves : []),
  ].filter(Boolean);
  const formats = item.formats && typeof item.formats === "object" ? item.formats : {};
  const coverUrl =
    formats["image/jpeg"] ||
    `https://www.gutenberg.org/cache/epub/${gId}/pg${gId}.cover.medium.jpg`;
  const sourceUrl = selectPreferredTextUrl(gId, formats);
  const hasFullText = Boolean(sourceUrl) && item.copyright !== true;

  return {
    sourceId: `gutenberg_${gId}`,
    gutendexId: gId,
    title,
    normalizedTitle: cleanTitleForAlphabeticalSort(title).toLowerCase(),
    firstLetter: extractFirstLetter(title),
    author: firstAuthor,
    language: languages[0] || "en",
    languages,
    subjects,
    formats,
    coverUrl,
    hasFullText,
    hasHumanAudio: false,
    sourceUrl: sourceUrl || `https://www.gutenberg.org/ebooks/${gId}`,
    narratorName: "",
    downloadCount: Number(item.download_count || 0),
  };
}

function extractGutenbergIdFromUrl(url) {
  if (!url || typeof url !== "string") return null;
  const match = url.match(/gutenberg\.org\/(?:ebooks|files|etext)\/(\d+)/i);
  return match ? Number(match[1]) : null;
}

function normalizeLibriVoxRecord(item) {
  if (!item || !item.id) return null;
  const lvId = String(item.id).trim();
  const title = cleanGutenbergTitle(item.title || `Audiobook ${lvId}`);
  let author = "Classic Author";
  if (Array.isArray(item.authors) && item.authors.length > 0) {
    const a = item.authors[0];
    author = `${a.first_name || ""} ${a.last_name || ""}`.trim() || "Classic Author";
  }
  const rawLang = String(item.language || "English").toLowerCase();
  const langMap = {
    english: "en",
    spanish: "es",
    french: "fr",
    german: "de",
    italian: "it",
    portuguese: "pt",
    russian: "ru",
    chinese: "zh",
    japanese: "ja",
  };
  const language = langMap[rawLang] || "en";
  const subjects = Array.isArray(item.genres)
    ? item.genres.map((g) => (g && g.name ? String(g.name).trim() : "")).filter(Boolean)
    : [];
  const linkedGutenbergId = extractGutenbergIdFromUrl(item.url_text_source || "");
  const archiveUrl = String(item.url_iarchive || item.url_librivox || "").trim();
  let coverUrl = String(item.coverart_jpg || item.coverart_thumbnail || "").trim();
  if (!coverUrl && archiveUrl) {
    const identifier = archiveUrl.replace(/\/+$/, "").split("/").pop();
    if (identifier) {
      coverUrl = `https://archive.org/services/img/${identifier}`;
    }
  }
  if (!coverUrl && linkedGutenbergId) {
    coverUrl = `https://www.gutenberg.org/cache/epub/${linkedGutenbergId}/pg${linkedGutenbergId}.cover.medium.jpg`;
  }
  const hasFullText = Boolean(linkedGutenbergId);
  const fullTextUrl = linkedGutenbergId
    ? `https://www.gutenberg.org/ebooks/${linkedGutenbergId}.txt.utf-8`
    : "";
  const formats = {};
  if (archiveUrl) formats["audio/mpeg"] = archiveUrl;
  if (fullTextUrl) formats["text/plain; charset=utf-8"] = fullTextUrl;
  if (coverUrl) formats["image/jpeg"] = coverUrl;

  return {
    sourceId: linkedGutenbergId ? `gutenberg_${linkedGutenbergId}` : `librivox_${lvId}`,
    gutendexId: linkedGutenbergId,
    librivoxId: lvId,
    title,
    normalizedTitle: cleanTitleForAlphabeticalSort(title).toLowerCase(),
    firstLetter: extractFirstLetter(title),
    author,
    language,
    languages: [language],
    subjects,
    formats,
    coverUrl,
    hasFullText,
    hasHumanAudio: true,
    sourceUrl: archiveUrl || fullTextUrl || `https://librivox.org/api/feed/audiobooks/?id=${lvId}`,
    narratorName: String(item.narrator || item.reader || "LibriVox Narrator").trim(),
    downloadCount: 1000,
  };
}

class CatalogMetadataIndex {
  constructor() {
    this.entriesById = new Map();
    this.entriesByTitleAuthor = new Map();
    this.fetchedUpstreamKeys = new Set();
  }

  upsertEntry(entry) {
    if (!entry || !entry.sourceId) return;
    const dedupKey = `${normalizeComparisonKey(entry.title)}::${normalizeComparisonKey(entry.author)}`;
    const existingId = this.entriesById.get(entry.sourceId) || this.entriesByTitleAuthor.get(dedupKey);

    if (existingId) {
      const merged = {
        ...existingId,
        gutendexId: existingId.gutendexId || entry.gutendexId || null,
        librivoxId: existingId.librivoxId || entry.librivoxId || null,
        subjects: Array.from(new Set([...(existingId.subjects || []), ...(entry.subjects || [])])),
        formats: { ...(existingId.formats || {}), ...(entry.formats || {}) },
        coverUrl: existingId.coverUrl || entry.coverUrl || "",
        hasFullText: Boolean(existingId.hasFullText || entry.hasFullText),
        hasHumanAudio: Boolean(existingId.hasHumanAudio || entry.hasHumanAudio),
        sourceUrl: existingId.sourceUrl || entry.sourceUrl || "",
        narratorName: existingId.narratorName || entry.narratorName || "",
        downloadCount: Math.max(existingId.downloadCount || 0, entry.downloadCount || 0),
      };
      this.entriesById.set(merged.sourceId, merged);
      this.entriesByTitleAuthor.set(dedupKey, merged);
    } else {
      this.entriesById.set(entry.sourceId, entry);
      this.entriesByTitleAuthor.set(dedupKey, entry);
    }
  }

  ingestGutendexPayload(payload) {
    const results = payload && Array.isArray(payload.results) ? payload.results : [];
    const added = [];
    for (const item of results) {
      const norm = normalizeGutendexRecord(item);
      if (norm) {
        this.upsertEntry(norm);
        added.push(this.entriesById.get(norm.sourceId));
      }
    }
    return added;
  }

  ingestLibriVoxPayload(payload) {
    const books = payload && Array.isArray(payload.books) ? payload.books : [];
    const added = [];
    for (const item of books) {
      const norm = normalizeLibriVoxRecord(item);
      if (norm) {
        this.upsertEntry(norm);
        added.push(this.entriesById.get(norm.sourceId));
      }
    }
    return added;
  }

  queryIndex({
    letter = "All",
    query = "",
    language = "ALL",
    category = "All",
    format = "all",
    narrator = "",
    page = 1,
    limit = 30,
  } = {}) {
    const normLetter = String(letter || "All").trim().toUpperCase();
    const normQuery = String(query || "").trim().toLowerCase();
    const normLang = String(language || "ALL").trim().toLowerCase();
    const normCat = String(category || "All").trim().toLowerCase();
    const normFormat = String(format || "all").trim().toLowerCase();
    const normNarrator = String(narrator || "").trim().toLowerCase();

    const allEntries = Array.from(this.entriesById.values());
    const filtered = allEntries.filter((entry) => {
      if (normLetter.length === 1 && normLetter >= "A" && normLetter <= "Z") {
        if (entry.firstLetter !== normLetter) return false;
      }
      if (normLang && normLang !== "all") {
        const entryLangs = Array.isArray(entry.languages) ? entry.languages : [entry.language];
        if (!entryLangs.some((l) => String(l).toLowerCase().startsWith(normLang.slice(0, 2)))) {
          return false;
        }
      }
      if (normCat && normCat !== "all") {
        const subjectBlob = `${entry.title} ${(entry.subjects || []).join(" ")}`.toLowerCase();
        const catTokens = normCat.split(/\s+and\s+|\s+/).filter((t) => t.length >= 3);
        if (catTokens.length > 0 && !catTokens.some((tok) => subjectBlob.includes(tok))) {
          return false;
        }
      }
      if (normFormat === "human_audio" && !entry.hasHumanAudio) return false;
      if (normFormat === "full_books" && !entry.hasFullText) return false;
      if (normFormat === "ai_narration" && !entry.hasFullText) return false;
      if (normNarrator && normNarrator !== "all" && normNarrator !== "all narrators") {
        if (!String(entry.narratorName || "").toLowerCase().includes(normNarrator)) return false;
      }
      if (normQuery) {
        const haystack = `${entry.title} ${entry.author} ${(entry.subjects || []).join(" ")} ${entry.language}`.toLowerCase();
        const tokens = normQuery.split(/\s+/).filter(Boolean);
        if (!tokens.every((t) => haystack.includes(t))) return false;
      }
      return true;
    });

    filtered.sort((a, b) => {
      const cmp = a.normalizedTitle.localeCompare(b.normalizedTitle);
      if (cmp !== 0) return cmp;
      return String(a.author || "").localeCompare(String(b.author || ""));
    });

    const safePage = Math.max(1, Number(page) || 1);
    const safeLimit = Math.min(100, Math.max(1, Number(limit) || 30));
    const startIndex = (safePage - 1) * safeLimit;
    const slice = filtered.slice(startIndex, startIndex + safeLimit);

    return {
      totalIndexedMatching: filtered.length,
      totalCatalogIndexed: allEntries.length,
      page: safePage,
      limit: safeLimit,
      hasMoreInIndex: startIndex + safeLimit < filtered.length,
      results: slice.map((e) => ({
        sourceId: e.sourceId,
        gutendexId: e.gutendexId,
        librivoxId: e.librivoxId,
        title: e.title,
        author: e.author,
        language: e.language,
        subjects: e.subjects,
        formats: e.formats,
        coverUrl: e.coverUrl,
        hasFullText: e.hasFullText,
        hasHumanAudio: e.hasHumanAudio,
        sourceUrl: e.sourceUrl,
        narratorName: e.narratorName,
      })),
    };
  }
}

const globalCatalogIndex = new CatalogMetadataIndex();

module.exports = {
  GUTENDEX_BASE_URL,
  LIBRIVOX_BASE_URL,
  CatalogMetadataIndex,
  globalCatalogIndex,
  cleanGutenbergTitle,
  cleanTitleForAlphabeticalSort,
  extractFirstLetter,
  normalizeGutendexRecord,
  normalizeLibriVoxRecord,
};
