const test = require("node:test");
const assert = require("node:assert/strict");
const {
  resolvePrimaryProviderForTask,
  streamOpenAiResponses,
  routeAiRequest,
  checkProvidersHealth
} = require("./aiRouterCore");

test("routes all LingoSphere AI tasks to OpenAI as primary and honors explicit gemini preference", () => {
  assert.equal(resolvePrimaryProviderForTask("advanced_tutor"), "OPENAI");
  assert.equal(resolvePrimaryProviderForTask("natural_conversation"), "OPENAI");
  assert.equal(resolvePrimaryProviderForTask("roleplay"), "OPENAI");
  assert.equal(resolvePrimaryProviderForTask("lesson_generation"), "OPENAI");
  assert.equal(resolvePrimaryProviderForTask("vocabulary_training"), "OPENAI");
  assert.equal(resolvePrimaryProviderForTask("dynamic_quiz"), "OPENAI");
  assert.equal(resolvePrimaryProviderForTask("lesson_generation", "gemini"), "GEMINI");
});

test("fails over from OpenAI to Gemini while preserving full conversation history", async () => {
  const calls = [];
  const mockFetch = async (url, options) => {
    calls.push({ url, headers: options.headers, body: JSON.parse(options.body) });
    if (url.includes("api.openai.com")) {
      return {
        ok: false,
        status: 503,
        text: async () => "OpenAI temporarily down"
      };
    }
    return {
      ok: true,
      status: 200,
      json: async () => ({
        responseId: "gem_1",
        candidates: [
          {
            content: {
              parts: [
                {
                  text: "REPLY: My name is Emma! What's your name?\nPHONETICS: my naym iz EM-uh\nTRANSLATION: My name is Emma! What's your name?\nCORRECTION: Optional: \"What is your name?\""
                }
              ]
            }
          }
        ]
      })
    };
  };

  const result = await routeAiRequest({
    body: {
      taskType: "advanced_tutor",
      instructions: "You are Emma.",
      input: "What is your ne?",
      history: [
        { role: "user", text: "Hi" },
        { role: "assistant", text: "Hi there! How can I help today?" }
      ]
    },
    openAiKey: process.env.TEST_OPENAI_KEY || "mock_openai_unit_test_key_9999",
    geminiKey: process.env.TEST_GEMINI_KEY || "mock_gemini_unit_test_key_8888",
    fetchImpl: mockFetch,
    env: { AI_ROUTER_TIMEOUT_MS: "5000" }
  });

  assert.equal(result.status, 200);
  assert.equal(result.payload.ok, true);
  assert.equal(result.payload.provider, "GEMINI");
  assert.equal(result.payload.primaryProviderAttempted, "OPENAI");
  assert.equal(result.payload.fallbackUsed, true);
  assert.equal(result.payload.preservedHistoryTurnCount, 2);
  assert.match(result.payload.outputText, /My name is Emma/);

  // Verify secondary Gemini request received preserved 2 history turns + new turn (3 contents)
  const geminiCall = calls.find((c) => c.url.includes("generativelanguage.googleapis.com"));
  assert.ok(geminiCall);
  assert.equal(geminiCall.body.contents.length, 3);
  assert.equal(geminiCall.body.contents[0].parts[0].text, "Hi");
  assert.equal(geminiCall.body.contents[1].role, "model");
  assert.equal(geminiCall.body.contents[2].parts[0].text, "What is your ne?");
});

test("fails over from explicit Gemini request to OpenAI when Gemini secret is missing", async () => {
  const calls = [];
  const mockFetch = async (url, options) => {
    calls.push({ url, body: JSON.parse(options.body) });
    return {
      ok: true,
      status: 200,
      json: async () => ({
        id: "resp_123",
        output_text: "Here is your A1 vocabulary lesson."
      })
    };
  };

  const result = await routeAiRequest({
    body: {
      taskType: "lesson_generation",
      preferredProvider: "gemini",
      input: "Generate an A1 greetings lesson"
    },
    openAiKey: process.env.TEST_OPENAI_KEY || "mock_openai_unit_test_key_9999",
    geminiKey: "",
    fetchImpl: mockFetch
  });

  assert.equal(result.status, 200);
  assert.equal(result.payload.provider, "OPENAI");
  assert.equal(result.payload.fallbackUsed, true);
  assert.equal(calls.length, 1);
  assert.ok(calls[0].url.includes("api.openai.com/v1/responses"));
});

test("streams OpenAI Responses SSE deltas when body reader is available", async () => {
  const chunks = [
    'data: {"type":"response.output_text.delta","delta":"Hello "}\n\n',
    'data: {"type":"response.output_text.delta","delta":"from OpenAI!"}\n\n',
    'data: {"type":"response.completed","response":{"id":"resp_stream_1","usage":{"total_tokens":12}}}\n\n'
  ];
  const encoder = new TextEncoder();
  let idx = 0;
  const mockFetch = async () => ({
    ok: true,
    status: 200,
    body: {
      getReader: () => ({
        read: async () => {
          if (idx >= chunks.length) return { done: true, value: undefined };
          const val = encoder.encode(chunks[idx++]);
          return { done: false, value: val };
        }
      })
    }
  });

  const deltas = [];
  const streamed = await streamOpenAiResponses({
    apiKey: process.env.TEST_OPENAI_KEY || "mock_openai_stream_test_key",
    model: "gpt-4o-mini",
    instructions: "Be helpful",
    historyTurns: [],
    userPrompt: "Hi",
    temperature: 0.6,
    maxOutputTokens: 120,
    fetchImpl: mockFetch,
    timeoutMs: 5000,
    onDelta: (d) => deltas.push(d)
  });

  assert.deepEqual(deltas, ["Hello ", "from OpenAI!"]);
  assert.equal(streamed.outputText, "Hello from OpenAI!");
  assert.equal(streamed.responseId, "resp_stream_1");
});

test("respects allowFallback=false for strict single-provider requests", async () => {
  let calledUrl = "";
  const mockFetch = async (url) => {
    calledUrl = url;
    return { ok: false, status: 500, text: async () => "OpenAI down" };
  };

  const result = await routeAiRequest({
    body: {
      preferredProvider: "openai",
      allowFallback: false,
      input: "Hello"
    },
    openAiKey: process.env.TEST_OPENAI_KEY || "mock_openai_unit_test_key_9999",
    geminiKey: process.env.TEST_GEMINI_KEY || "mock_gemini_unit_test_key_8888",
    fetchImpl: mockFetch
  });

  assert.equal(result.status, 503);
  assert.equal(result.payload.fallbackAttempted, null);
  assert.ok(calledUrl.includes("api.openai.com"));
});

test("returns honest 503 error and redacts secrets when both providers fail", async () => {
  const secretO = process.env.TEST_OPENAI_KEY || "mock_super_secret_value_123";
  const secretG = process.env.TEST_GEMINI_KEY || "mock_gemini_super_secret_456";
  const mockFetch = async () => ({
    ok: false,
    status: 500,
    text: async () => `Upstream error referencing ${secretO} and ${secretG}`
  });

  const result = await routeAiRequest({
    body: {
      taskType: "advanced_tutor",
      input: "How are you?"
    },
    openAiKey: secretO,
    geminiKey: secretG,
    fetchImpl: mockFetch
  });

  assert.equal(result.status, 503);
  assert.equal(result.payload.ok, false);
  assert.equal(result.payload.code, "AI_SERVICE_UNAVAILABLE");
  assert.equal(JSON.stringify(result.payload).includes(secretO), false);
  assert.equal(JSON.stringify(result.payload).includes(secretG), false);
});

test("checkProvidersHealth verifies secrets and remote reachability without leaking secrets", async () => {
  const secretO = process.env.TEST_OPENAI_KEY || "mock_health_secret_openai_xyz";
  const secretG = process.env.TEST_GEMINI_KEY || "mock_health_secret_gemini_abc";

  const mockFetch = async (url) => {
    if (url.includes("api.openai.com")) {
      return { ok: true, status: 200 };
    }
    return {
      ok: false,
      status: 403,
      text: async () => `Permission denied for key ${secretG}`
    };
  };

  const health = await checkProvidersHealth({
    openAiKey: secretO,
    geminiKey: secretG,
    probeRemote: true,
    fetchImpl: mockFetch
  });

  assert.equal(health.ok, true);
  assert.equal(health.openAi.configured, true);
  assert.equal(health.openAi.reachable, true);
  assert.equal(health.openAi.status, "healthy");
  assert.equal(health.gemini.configured, true);
  assert.equal(health.gemini.reachable, false);
  assert.equal(JSON.stringify(health).includes(secretO), false);
  assert.equal(JSON.stringify(health).includes(secretG), false);
});

test("CatalogMetadataIndex indexes Gutendex and LibriVox metadata by firstLetter, deduplicates, and stores metadata only", () => {
  const {
    CatalogMetadataIndex,
    normalizeGutendexRecord,
    normalizeLibriVoxRecord
  } = require("./catalogIndexCore");

  const index = new CatalogMetadataIndex();
  const gutEntry = normalizeGutendexRecord({
    id: 2641,
    title: "A Room with a View",
    authors: [{ name: "Forster, E. M." }],
    languages: ["en"],
    subjects: ["British in Italy -- Fiction", "Romance"],
    formats: {
      "text/plain; charset=utf-8": "https://www.gutenberg.org/ebooks/2641.txt.utf-8",
      "image/jpeg": "https://www.gutenberg.org/cache/epub/2641/pg2641.cover.medium.jpg"
    }
  });
  const lvEntry = normalizeLibriVoxRecord({
    id: "26410",
    title: "Room with a View",
    language: "English",
    url_iarchive: "https://archive.org/details/room_with_a_view_lv",
    url_text_source: "https://www.gutenberg.org/ebooks/2641",
    authors: [{ first_name: "E. M.", last_name: "Forster" }],
    genres: [{ name: "Romance" }],
    narrator: "Elizabeth Klett"
  });
  const mEntry = normalizeGutendexRecord({
    id: 2680,
    title: "Meditations",
    authors: [{ name: "Marcus Aurelius" }],
    languages: ["en"],
    subjects: ["Stoics", "Philosophy"],
    formats: { "text/plain; charset=utf-8": "https://www.gutenberg.org/ebooks/2680.txt.utf-8" }
  });

  index.upsertEntry(gutEntry);
  index.upsertEntry(lvEntry);
  index.upsertEntry(mEntry);

  // Deduplicated into 2 unique books (gutenberg_2641 merged with LibriVox + gutenberg_2680)
  assert.equal(index.entriesById.size, 2);

  const letterAResults = index.queryIndex({ letter: "A", language: "en" });
  assert.equal(letterAResults.totalIndexedMatching, 1);
  const merged = letterAResults.results[0];
  assert.equal(merged.sourceId, "gutenberg_2641");
  assert.equal(merged.hasFullText, true);
  assert.equal(merged.hasHumanAudio, true);
  assert.equal("fullBookText" in merged, false);
});


