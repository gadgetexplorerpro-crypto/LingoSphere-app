# LingoSphere — AI & Infrastructure Cost Model (`docs/COST_MODEL.md`)

## 1. Subscription Plans & Net Revenue Economics

LingoSphere offers strictly **two plans**: **Free** (`$0 forever`) and **LingoSphere Pro** (Monthly `$9.99` / Yearly `$79.99`, ~33% off).

### Google Play Net Revenue & Target Margin
- **Monthly Pro Retail Price**: `$9.99 / month`
  - Google Play Subscription Fee (15%): `-$1.50`
  - **Net Play Revenue**: `$8.49 / month`
  - Infrastructure & Firestore/Signaling Reserve (~10%): `-$0.85`
  - Target Gross Margin ($\ge 50\%$): `$\ge $4.25 / month`
  - **Maximum Allowed Worst-Case AI Spend per Pro User**: **$\le \$3.00 / \text{month}$** (`$2.85` configured hard cap)
  - **Expected AI Spend at 30% Typical Active Usage**: **`~$0.76 / month`** (~89% gross margin after Play fee)
- **Yearly Pro Retail Price**: `$79.99 / year` (`$6.67 / month` equivalent)
  - Google Play Subscription Fee (15%): `-$12.00 / year` (`-$1.00 / month`)
  - **Net Play Revenue**: `$67.99 / year` (`$5.67 / month`)
  - Infrastructure Reserve (~10%): `-$0.57 / month`
  - **Maximum Allowed Worst-Case AI Spend per Yearly User**: **$\le \$2.80 / \text{month}$** (Target $\ge 50\%$ margin)
  - **Expected AI Spend at 30% Typical Active Usage**: **`~$0.76 / month`** (~86% gross margin)
- **Free Plan (`$0 forever`)**:
  - **Zero Free AI Usage**: All AI tutoring, roleplay, AI studios, AI assessments, and structured Pro lessons are gated strictly behind LingoSphere Pro (**`$0.00 / day`** AI spend per Free user).
  - **Free Non-AI Features**: Up to **5 distinct public-domain books/audiobooks per calendar month**, saved flashcard SRS review (no AI), human Language Exchange, and non-AI listening/basic games.

---

## 2. Official Model Pricing Reference (`config/pricing`)

All rates are stored in Firestore at `config/pricing` (in USD micros per unit, where $1.00 = 1,000,000$ micros) and loaded by the backend `aiGateway` without requiring an app update.

| Model / Service | Role in LingoSphere | Input Cost | Output Cost | Audio / Minute Cost |
| :--- | :--- | :--- | :--- | :--- |
| **`gemini-2.5-flash-lite`** | Pro text chat, grammar corrections, translation, vocabulary, quizzes, relevance pre-checks | `$0.10 / 1M tokens` (`0.1` micros/1k) | `$0.40 / 1M tokens` (`0.4` micros/1k) | Audio In: `$0.0005 / min` |
| **`gemini-2.5-flash`** | Pro complex roleplay, CEFR exam rubric evaluation, custom lesson/story generation, book summaries | `$0.30 / 1M tokens` (`0.3` micros/1k) | `$2.50 / 1M tokens` (`2.5` micros/1k) | Audio In: `$0.0015 / min` |
| **`gpt-4o-mini`** | Pro conversational tutor fallback & structured roleplay | `$0.15 / 1M tokens` | `$0.60 / 1M tokens` | N/A |
| **Voice / Live Tutor Pipeline** | STT + LLM (`gemini-2.5-flash-lite` / Realtime mini) + TTS (`SpeechManager` / Cloud TTS) | Combined Input/STT | Combined Output/TTS | **`$0.0095 / minute`** (`158.3` micros/sec) |

### Cost Formula Used by `aiGateway`
$$\text{Cost}_{\text{text}} = \left(\frac{\text{InputTokens}}{1{,}000{,}000} \times P_{\text{in}}\right) + \left(\frac{\text{OutputTokens}}{1{,}000{,}000} \times P_{\text{out}}\right)$$

$$\text{Cost}_{\text{voice}} = \text{DurationSeconds} \times \frac{P_{\text{voice\_per\_min}}}{60}$$

- **Strict Token & History Caps**:
  - Conversation history sent to the model is capped at the **last 4–6 turns** (older turns are summarized locally), bounding input tokens to $\le 850$ tokens per request.
  - Output tokens are hard-capped at `maxOutputTokens = 350` for chat/corrections and `maxOutputTokens = 750` for structured stories/lessons.
- **Content-Hash Caching (`aiCache/{sha256}`)**:
  - Word explanations, grammar corrections, translations, TTS audio snippets, and book summaries are cached by `SHA-256(feature + content + language + level)`.
  - **Cache hits cost `$0.00`** and do not invoke the upstream LLM (Pro entitlement verified first).

---

## 3. Feature-by-Feature Cost Table (Free vs. LingoSphere Pro)

| Feature (`featureKey`) | Free Limit | Pro Limit | Primary Model | Est. Unit Cost (USD) | Free Worst-Case Daily Cost | Pro Worst-Case Daily Cost | Pro Worst-Case Monthly Cost (Capped) | Pro Expected Monthly Cost (30% Usage) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **AI Voice Tutor** (`ai.voice.seconds`) | Locked (`0s/day`) | 15 min/day & **150 min/mo** (`9,000s`) | Voice Pipeline (`flash-lite` + TTS) | `$0.0095 / min` | `$0.0000 / day` | `$0.1425 / day` | **`$1.425 / mo`** (capped at 150 min/mo) | `$0.428 / mo` (45 min/mo) |
| **AI Text Chat** (`ai.text.messages`) | Locked (`0/day`) | 150 msgs/day | `gemini-2.5-flash-lite` (700 in / 250 out) | `$0.00017 / msg` | `$0.0000 / day` | `$0.0255 / day` | **`$0.255 / mo`** (1,500 msgs/mo cap) | `$0.076 / mo` |
| **AI Roleplay & Lessons** (`ai.roleplay.sessions`, `lessons.*`) | Locked (`0/day`) | Unlimited lessons, 8 roleplay/day | `gemini-2.5-flash-lite` / `flash` | `$0.00180 / session` | `$0.0000 / day` | `$0.0144 / day` | **`$0.180 / mo`** | `$0.054 / mo` |
| **Grammar & Writing Corrections** (`ai.corrections`) | Locked (`0/day`) | 50 / day | `gemini-2.5-flash-lite` + `aiCache` | `$0.00014 / check` | `$0.0000 / day` | `$0.0070 / day` | **`$0.105 / mo`** | `$0.031 / mo` |
| **Personalized AI Material** (`personalizedMaterial`) | Locked (`0/day`) | 10 / day | `gemini-2.5-flash` (900 in / 600 out) | `$0.00177 / item` | `$0.0000 / day` | `$0.0177 / day` | **`$0.177 / mo`** | `$0.053 / mo` |
| **Intelligent Studio, Labs & Exams** (`studio`, `labs`) | Locked (`0/day`) | Full Pro access (fair cap) | `gemini-2.5-flash-lite` | `$0.00025 / run` | `$0.0000 / day` | `$0.0050 / day` | **`$0.075 / mo`** | `$0.022 / mo` |
| **Books & Audiobooks** (`books.audio`, `books.reading`) | **5 distinct books/month** (no AI) | Unlimited catalog + cached AI summaries | Public-domain streams + cached `aiCache` | `$0.0000` (public domain) | `$0.0000 / day` | `$0.0010 / day` | **`$0.015 / mo`** | `$0.004 / mo` |
| **Human Calls & Rooms** (`calls.audio`, `calls.video`, `rooms`) | 20m/day audio, 10m/day video | 120m/day audio, 60m/day video, host rooms | P2P WebRTC + Cloudflare TURN | `$0.0002 / min` (TURN relay share) | `$0.0012 / day` | `$0.0060 / day` | **`$0.090 / mo`** | `$0.027 / mo` |
| **TOTAL** | **`$0.00` AI cost** | **150m/mo voice + daily caps** | **Hybrid Tiered Routing** | — | **`$0.000 / day` AI (`$0.0012` TURN)** | **`$0.219 / day` peak** | **`$2.32 / mo` hard cap ($\le \$3.00$)** | **`$0.70 / mo` expected** |

---

## 4. Circuit Breakers & Multi-Layer Cost Protection

1. **Atomic Pre-Call Quota Reservation**:
   - `aiGateway` executes a Firestore transaction on `usage/{uid}/daily/{yyyy-mm-dd}` and `usage/{uid}/monthly/{yyyy-mm}` **before** invoking Gemini or OpenAI. Parallel bursts cannot race past a user's daily or monthly cap.
2. **80% Soft Warning & 100% Hard Stop**:
   - At 80% of any daily or monthly allowance, `aiGateway` and `GeminiCostAndQuotaManager` return `softWarning80Percent = true` with a non-intrusive reminder banner.
   - At 100%, requests are blocked with a friendly "Limit Reached" sheet displaying the user's local midnight reset time.
3. **15-Second Live Voice Heartbeat Metering (`aiVoiceHeartbeat`)**:
   - Active AI Voice Tutor sessions send a heartbeat every 15 seconds to deduct 15 seconds of `ai.voice.seconds`. If remaining quota reaches `0`, the backend returns `terminateSession: true` and the client closes the voice stream gracefully.
4. **Global Daily Budget Circuit Breaker**:
   - When total daily platform AI spend in `usageEvents` crosses 85% of the configured daily budget, `aiGateway` automatically downgrades all Free requests to cached/lite responses first, preserving Pro user capacity.
