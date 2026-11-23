/**
 * LingoSphere — Server-side OpenAI + Gemini Intelligent Router & Failover Core
 * Never logs or exposes API keys to clients. Supports configurable models,
 * bounded timeouts, transient retries, and full conversation-history preservation on failover.
 */

function resolvePrimaryProviderForTask(_taskType, requestedProvider) {
  const pref = String(requestedProvider || "").trim().toLowerCase();
  if (pref === "gemini") return "GEMINI";
  if (pref === "openai") return "OPENAI";
  // Shared LingoSphere policy: OpenAI is primary for all supported AI features,
  // with Google Gemini acting as automatic secondary failover.
  return "OPENAI";
}

function normalizeHistoryTurns(rawHistory) {
  if (!Array.isArray(rawHistory)) return [];
  return rawHistory
    .slice(-12)
    .map((turn) => {
      if (!turn || typeof turn !== "object") return null;
      const roleRaw = String(turn.role || "user").toLowerCase();
      const role =
        roleRaw === "assistant" || roleRaw === "tutor" || roleRaw === "model" || roleRaw === "emma"
          ? "assistant"
          : "user";
      const content = String(turn.text || turn.content || "").trim().slice(0, 1500);
      if (!content) return null;
      return { role, content };
    })
    .filter(Boolean);
}

function extractOpenAiResponseText(data) {
  if (!data || typeof data !== "object") return "";
  if (typeof data.output_text === "string" && data.output_text.trim().length > 0) {
    return data.output_text.trim();
  }
  if (Array.isArray(data.output)) {
    const chunks = [];
    for (const item of data.output) {
      if (item && Array.isArray(item.content)) {
        for (const block of item.content) {
          if (block && typeof block.text === "string" && block.text.trim()) {
            chunks.push(block.text.trim());
          } else if (block && typeof block.transcript === "string" && block.transcript.trim()) {
            chunks.push(block.transcript.trim());
          }
        }
      }
    }
    if (chunks.length > 0) {
      return chunks.join("\n").trim();
    }
  }
  return "";
}

function extractGeminiResponseText(data) {
  if (!data || typeof data !== "object") return "";
  const candidates = Array.isArray(data.candidates) ? data.candidates : [];
  for (const cand of candidates) {
    const parts = cand?.content?.parts;
    if (Array.isArray(parts)) {
      const text = parts
        .map((p) => (typeof p?.text === "string" ? p.text : ""))
        .join("")
        .trim();
      if (text) return text;
    }
  }
  return "";
}

function buildGeminiGenerateContentPayload({
  instructions,
  historyTurns,
  userPrompt,
  temperature,
  maxOutputTokens
}) {
  const contents = [];
  for (const turn of historyTurns) {
    contents.push({
      role: turn.role === "assistant" ? "model" : "user",
      parts: [{ text: turn.content }]
    });
  }
  contents.push({
    role: "user",
    parts: [{ text: userPrompt }]
  });

  const payload = {
    contents,
    generationConfig: {
      temperature,
      maxOutputTokens
    }
  };
  if (instructions && instructions.trim().length > 0) {
    payload.systemInstruction = {
      parts: [{ text: instructions.trim() }]
    };
  }
  return payload;
}

function redactSecret(text, secrets) {
  let out = String(text || "");
  for (const sec of secrets) {
    if (sec && sec.length >= 6) {
      out = out.split(sec).join("[REDACTED]");
    }
  }
  return out;
}

async function fetchWithRetry({ url, options, fetchImpl, timeoutMs = 20000, maxRetries = 1 }) {
  let lastError = null;
  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      const signal =
        typeof AbortSignal !== "undefined" && typeof AbortSignal.timeout === "function"
          ? AbortSignal.timeout(timeoutMs)
          : undefined;
      const response = await fetchImpl(url, { ...options, ...(signal ? { signal } : {}) });
      if (response.ok) {
        return response;
      }
      const errText = await response.text().catch(() => "");
      const retryable = response.status === 429 || response.status >= 500;
      lastError = new Error(`HTTP ${response.status}: ${errText.slice(0, 240)}`);
      lastError.statusCode = response.status;
      if (!retryable || attempt === maxRetries) {
        throw lastError;
      }
    } catch (err) {
      lastError = err;
      const status = Number(err?.statusCode || 0);
      const isTransient = status === 0 || status === 429 || status >= 500;
      if (!isTransient || attempt === maxRetries) {
        throw err;
      }
    }
  }
  throw lastError || new Error("Request failed");
}

async function callOpenAiResponses({
  apiKey,
  model,
  instructions,
  historyTurns,
  userPrompt,
  temperature,
  maxOutputTokens,
  fetchImpl,
  timeoutMs
}) {
  if (!apiKey || !apiKey.trim()) {
    const err = new Error("OPENAI_API_KEY secret is not configured");
    err.code = "OPENAI_SECRET_MISSING";
    err.statusCode = 503;
    throw err;
  }

  const inputMessages = historyTurns.map((t) => ({ role: t.role, content: t.content }));
  inputMessages.push({ role: "user", content: userPrompt });

  const resp = await fetchWithRetry({
    url: "https://api.openai.com/v1/responses",
    options: {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey.trim()}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model,
        instructions,
        input: inputMessages,
        temperature,
        max_output_tokens: maxOutputTokens
      })
    },
    fetchImpl,
    timeoutMs
  });

  const data = await resp.json();
  const outputText = extractOpenAiResponseText(data);
  if (!outputText) {
    const err = new Error("Empty response from OpenAI Responses API");
    err.statusCode = 502;
    throw err;
  }
  return {
    provider: "OPENAI",
    model,
    outputText,
    responseId: data.id || "",
    usage: data.usage || {}
  };
}

async function streamOpenAiResponses({
  apiKey,
  model,
  instructions,
  historyTurns,
  userPrompt,
  temperature,
  maxOutputTokens,
  fetchImpl,
  timeoutMs,
  onDelta
}) {
  if (!apiKey || !apiKey.trim()) {
    const err = new Error("OPENAI_API_KEY secret is not configured");
    err.code = "OPENAI_SECRET_MISSING";
    err.statusCode = 503;
    throw err;
  }

  const inputMessages = historyTurns.map((t) => ({ role: t.role, content: t.content }));
  inputMessages.push({ role: "user", content: userPrompt });

  const resp = await fetchWithRetry({
    url: "https://api.openai.com/v1/responses",
    options: {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey.trim()}`,
        "Content-Type": "application/json",
        Accept: "text/event-stream"
      },
      body: JSON.stringify({
        model,
        instructions,
        input: inputMessages,
        temperature,
        max_output_tokens: maxOutputTokens,
        stream: true
      })
    },
    fetchImpl,
    timeoutMs,
    maxRetries: 0
  });

  if (!resp.body || typeof resp.body.getReader !== "function") {
    const data = await resp.json();
    const full = extractOpenAiResponseText(data);
    if (!full) {
      const err = new Error("Empty response from OpenAI Responses API");
      err.statusCode = 502;
      throw err;
    }
    if (onDelta) onDelta(full);
    return {
      provider: "OPENAI",
      model,
      outputText: full,
      responseId: data.id || "",
      usage: data.usage || {}
    };
  }

  const reader = resp.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";
  let accumulated = "";
  let responseId = "";
  let usage = {};

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    let newlineIndex = buffer.indexOf("\n");
    while (newlineIndex >= 0) {
      const rawLine = buffer.slice(0, newlineIndex).trim();
      buffer = buffer.slice(newlineIndex + 1);
      newlineIndex = buffer.indexOf("\n");
      if (!rawLine.startsWith("data:")) continue;
      const dataStr = rawLine.slice(5).trim();
      if (!dataStr || dataStr === "[DONE]") continue;
      try {
        const evt = JSON.parse(dataStr);
        if (evt.type === "response.output_text.delta" && typeof evt.delta === "string") {
          accumulated += evt.delta;
          if (onDelta) onDelta(evt.delta);
        } else if (evt.type === "response.completed" && evt.response) {
          responseId = evt.response.id || responseId;
          usage = evt.response.usage || usage;
          if (!accumulated) {
            const fullText = extractOpenAiResponseText(evt.response);
            if (fullText) {
              accumulated = fullText;
              if (onDelta) onDelta(fullText);
            }
          }
        }
      } catch (_) {
        // Ignore non-JSON heartbeat lines
      }
    }
  }

  const finalText = accumulated.trim();
  if (!finalText) {
    const err = new Error("Empty streaming response from OpenAI Responses API");
    err.statusCode = 502;
    throw err;
  }

  return {
    provider: "OPENAI",
    model,
    outputText: finalText,
    responseId,
    usage
  };
}

async function callGeminiDeveloperApi({
  apiKey,
  model,
  instructions,
  historyTurns,
  userPrompt,
  temperature,
  maxOutputTokens,
  fetchImpl,
  timeoutMs
}) {
  if (!apiKey || !apiKey.trim()) {
    const err = new Error("GEMINI_API_KEY secret is not configured");
    err.code = "GEMINI_SECRET_MISSING";
    err.statusCode = 503;
    throw err;
  }

  const payload = buildGeminiGenerateContentPayload({
    instructions,
    historyTurns,
    userPrompt,
    temperature,
    maxOutputTokens
  });

  const encodedModel = encodeURIComponent(model);
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodedModel}:generateContent`;
  const resp = await fetchWithRetry({
    url,
    options: {
      method: "POST",
      headers: {
        "x-goog-api-key": apiKey.trim(),
        "Content-Type": "application/json"
      },
      body: JSON.stringify(payload)
    },
    fetchImpl,
    timeoutMs
  });

  const data = await resp.json();
  const outputText = extractGeminiResponseText(data);
  if (!outputText) {
    const err = new Error("Empty response from Gemini API");
    err.statusCode = 502;
    throw err;
  }
  return {
    provider: "GEMINI",
    model,
    outputText,
    responseId: data.responseId || "",
    usage: data.usageMetadata || {}
  };
}

/**
 * Executes a single primary provider request and only fails over to the secondary provider
 * when the primary fails or lacks configuration, preserving full conversation history.
 */
async function routeAiRequest({
  body = {},
  openAiKey = "",
  geminiKey = "",
  isPremium = false,
  fetchImpl = globalThis.fetch,
  env = process.env
}) {
  const userPrompt = String(body.input || body.userMessage || body.prompt || "").trim().slice(0, 6000);
  if (!userPrompt) {
    return {
      status: 400,
      payload: {
        ok: false,
        code: "INVALID_INPUT",
        error: "Missing input prompt"
      }
    };
  }

  const instructions = String(
    body.instructions ||
      body.systemInstructions ||
      "You are a warm, intelligent human language tutor in LingoSphere."
  ).slice(0, 8000);
  const historyTurns = normalizeHistoryTurns(body.history || body.conversationHistory);
  const temperature = Math.min(1.0, Math.max(0.1, Number(body.temperature ?? 0.65)));
  const maxOutputTokens = Math.min(1400, Math.max(120, Number(body.maxOutputTokens ?? 520)));

  const defaultOpenAiModel = String(env.OPENAI_DEFAULT_MODEL || "gpt-4o-mini").trim();
  const requestedOpenAiModel = String(body.openAiModel || body.model || defaultOpenAiModel).trim();
  const allowedOpenAiModels = isPremium
    ? [defaultOpenAiModel, "gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "gpt-4.1"]
    : [defaultOpenAiModel, "gpt-4o-mini", "gpt-4.1-mini"];
  const selectedOpenAiModel = allowedOpenAiModels.includes(requestedOpenAiModel)
    ? requestedOpenAiModel
    : defaultOpenAiModel;

  const defaultGeminiModel = String(env.GEMINI_DEFAULT_MODEL || "gemini-2.5-flash").trim();
  const requestedGeminiModel = String(body.geminiModel || body.model || defaultGeminiModel).trim();
  const allowedGeminiModels = [
    defaultGeminiModel,
    "gemini-3.8-flash",
    "gemini-3.5-flash-lite",
    "gemini-2.5-flash",
    "gemini-2.5-flash-lite",
    "gemini-2.5-pro",
    "gemini-2.0-flash"
  ];
  const selectedGeminiModel = allowedGeminiModels.includes(requestedGeminiModel)
    ? requestedGeminiModel
    : defaultGeminiModel;

  const primaryProvider = resolvePrimaryProviderForTask(body.taskType, body.preferredProvider);
  const secondaryProvider = primaryProvider === "OPENAI" ? "GEMINI" : "OPENAI";
  const allowFallback = body.allowFallback !== false;
  const timeoutMs = Number(env.AI_ROUTER_TIMEOUT_MS || 22000);

  const invokeProvider = (providerName) => {
    if (providerName === "OPENAI") {
      return callOpenAiResponses({
        apiKey: openAiKey,
        model: selectedOpenAiModel,
        instructions,
        historyTurns,
        userPrompt,
        temperature,
        maxOutputTokens,
        fetchImpl,
        timeoutMs
      });
    }
    return callGeminiDeveloperApi({
      apiKey: geminiKey,
      model: selectedGeminiModel,
      instructions,
      historyTurns,
      userPrompt,
      temperature,
      maxOutputTokens,
      fetchImpl,
      timeoutMs
    });
  };

  let primaryError = null;
  try {
    const primaryRes = await invokeProvider(primaryProvider);
    return {
      status: 200,
      payload: {
        ok: true,
        provider: primaryRes.provider,
        primaryProviderAttempted: primaryProvider,
        fallbackUsed: false,
        preservedHistoryTurnCount: historyTurns.length,
        model: primaryRes.model,
        responseId: primaryRes.responseId,
        text: primaryRes.outputText,
        outputText: primaryRes.outputText,
        usage: primaryRes.usage
      }
    };
  } catch (err) {
    primaryError = err;
  }

  let secondaryError = null;
  if (allowFallback) {
    try {
      const secondaryRes = await invokeProvider(secondaryProvider);
      return {
        status: 200,
        payload: {
          ok: true,
          provider: secondaryRes.provider,
          primaryProviderAttempted: primaryProvider,
          fallbackUsed: true,
          preservedHistoryTurnCount: historyTurns.length,
          model: secondaryRes.model,
          responseId: secondaryRes.responseId,
          text: secondaryRes.outputText,
          outputText: secondaryRes.outputText,
          usage: secondaryRes.usage
        }
      };
    } catch (err) {
      secondaryError = err;
    }
  }

  const sanitizedPrimary = redactSecret(primaryError?.message || "Primary provider failed", [openAiKey, geminiKey]);
  const sanitizedSecondary = allowFallback
    ? redactSecret(secondaryError?.message || "Secondary provider failed", [openAiKey, geminiKey])
    : "Fallback disabled by request";

  return {
    status: 503,
    payload: {
      ok: false,
      code: "AI_SERVICE_UNAVAILABLE",
      primaryProviderAttempted: primaryProvider,
      fallbackAttempted: allowFallback ? secondaryProvider : null,
      error: "AI service is currently unavailable. Please try again shortly.",
      detail: `${primaryProvider}: ${sanitizedPrimary} | ${secondaryProvider}: ${sanitizedSecondary}`.slice(0, 400)
    }
  };
}

/**
 * Verifies backend provider secret configuration and optional live reachability
 * without ever exposing API keys or sensitive credentials.
 */
async function checkProvidersHealth({
  openAiKey = "",
  geminiKey = "",
  probeRemote = false,
  fetchImpl = globalThis.fetch,
  env = process.env,
  timeoutMs = 8000
} = {}) {
  const cleanOpenAiKey = String(openAiKey || "").trim();
  const cleanGeminiKey = String(geminiKey || "").trim();
  const openAiModel = String(env.OPENAI_DEFAULT_MODEL || "gpt-4o-mini").trim();
  const geminiModel = String(env.GEMINI_DEFAULT_MODEL || "gemini-2.5-flash").trim();

  const openAiHealth = {
    configured: cleanOpenAiKey.length > 0,
    model: openAiModel,
    status: cleanOpenAiKey.length > 0 ? "configured" : "missing_secret"
  };

  const geminiHealth = {
    configured: cleanGeminiKey.length > 0,
    model: geminiModel,
    status: cleanGeminiKey.length > 0 ? "configured" : "missing_secret"
  };

  if (probeRemote && cleanOpenAiKey) {
    const startMs = Date.now();
    try {
      const resp = await fetchWithRetry({
        url: `https://api.openai.com/v1/models/${encodeURIComponent(openAiModel)}`,
        options: {
          method: "GET",
          headers: { Authorization: `Bearer ${cleanOpenAiKey}` }
        },
        fetchImpl,
        timeoutMs,
        maxRetries: 0
      });
      openAiHealth.reachable = resp.ok;
      openAiHealth.status = resp.ok ? "healthy" : `http_${resp.status}`;
      openAiHealth.latencyMs = Date.now() - startMs;
    } catch (err) {
      openAiHealth.reachable = false;
      openAiHealth.status = "unreachable";
      openAiHealth.latencyMs = Date.now() - startMs;
      openAiHealth.error = redactSecret(err?.message || "Probe failed", [cleanOpenAiKey, cleanGeminiKey]).slice(0, 180);
    }
  }

  if (probeRemote && cleanGeminiKey) {
    const startMs = Date.now();
    try {
      const resp = await fetchWithRetry({
        url: `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(geminiModel)}`,
        options: {
          method: "GET",
          headers: { "x-goog-api-key": cleanGeminiKey }
        },
        fetchImpl,
        timeoutMs,
        maxRetries: 0
      });
      geminiHealth.reachable = resp.ok;
      geminiHealth.status = resp.ok ? "healthy" : `http_${resp.status}`;
      geminiHealth.latencyMs = Date.now() - startMs;
    } catch (err) {
      geminiHealth.reachable = false;
      geminiHealth.status = "unreachable";
      geminiHealth.latencyMs = Date.now() - startMs;
      geminiHealth.error = redactSecret(err?.message || "Probe failed", [cleanOpenAiKey, cleanGeminiKey]).slice(0, 180);
    }
  }

  const anyReady = probeRemote
    ? Boolean(openAiHealth.reachable || geminiHealth.reachable)
    : Boolean(openAiHealth.configured || geminiHealth.configured);

  return {
    ok: anyReady,
    checkedAtEpochMs: Date.now(),
    probeRemote: Boolean(probeRemote),
    openAi: openAiHealth,
    gemini: geminiHealth,
    routingPolicy: {
      primaryProvider: "OPENAI",
      secondaryProvider: "GEMINI",
      conversationPrimary: "OPENAI",
      educationalContentPrimary: "OPENAI",
      fallbackEnabled: true
    }
  };
}

module.exports = {
  resolvePrimaryProviderForTask,
  normalizeHistoryTurns,
  extractOpenAiResponseText,
  extractGeminiResponseText,
  buildGeminiGenerateContentPayload,
  callOpenAiResponses,
  streamOpenAiResponses,
  callGeminiDeveloperApi,
  routeAiRequest,
  checkProvidersHealth
};
