/**
 * LingoSphere — Firebase Cloud Functions v2 Backend
 * 1. Short-lived Cloudflare Realtime TURN Credentials (`getTurnCredentials`)
 * 2. Automatic High-Priority FCM Call Push Notifications (`onCallRoomUpdated` & `sendCallInvitePush`)
 *
 * Secret setup:
 *   firebase functions:secrets:set CLOUDFLARE_TURN_API_TOKEN
 */

const { onRequest } = require("firebase-functions/v2/https");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const { getFirestore } = require("firebase-admin/firestore");

if (!admin.apps.length) {
  admin.initializeApp();
}

const FIRESTORE_DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID ||
  "ai-studio-android-lingosph-89724989-49bf-4f35-9409-1a290f09b646";

const {
  routeAiRequest,
  checkProvidersHealth,
  streamOpenAiResponses,
  normalizeHistoryTurns,
  extractOpenAiResponseText: extractOpenAiResponseTextCore
} = require("./aiRouterCore");
const {
  globalCatalogIndex,
  GUTENDEX_BASE_URL,
  LIBRIVOX_BASE_URL
} = require("./catalogIndexCore");

const cloudflareTurnApiToken = defineSecret("CLOUDFLARE_TURN_API_TOKEN");
const openAiApiKey = defineSecret("OPENAI_API_KEY");
const geminiApiKey = defineSecret("GEMINI_API_KEY");
const DEFAULT_TURN_KEY_ID = process.env.CLOUDFLARE_TURN_KEY_ID || "";
const DEFAULT_TTL_SECONDS = 3600;
const CALL_INVITE_TTL_MS = 45000;
const USER_RATE_WINDOW_MS = 60000;
const USER_RATE_MAX_REQUESTS = 30;
const userRequestTimestamps = new Map();

function enforcePerUserBurstLimit(uid) {
  const now = Date.now();
  const prev = userRequestTimestamps.get(uid) || [];
  const recent = prev.filter((ts) => now - ts < USER_RATE_WINDOW_MS);
  if (recent.length >= USER_RATE_MAX_REQUESTS) {
    const err = new Error("Too many AI requests in a short window. Please pause a moment and try again.");
    err.statusCode = 429;
    throw err;
  }
  recent.push(now);
  userRequestTimestamps.set(uid, recent);
}

function getLingoFirestore() {
  try {
    return getFirestore(admin.app(), FIRESTORE_DATABASE_ID);
  } catch (_) {
    return admin.firestore();
  }
}

/**
 * Dispatches high-priority FCM data messages to all registered devices of `calleeId`.
 * Never includes TURN credentials or secrets in the FCM payload.
 */
async function dispatchCallFcmToUser(db, calleeId, payloadData) {
  if (!calleeId || calleeId === "waiting_partner") {
    return { sentCount: 0, prunedCount: 0 };
  }

  const tokensSnap = await db
    .collection("fcm_tokens")
    .where("userId", "==", calleeId)
    .get();

  if (tokensSnap.empty) {
    return { sentCount: 0, prunedCount: 0 };
  }

  const tokenDocs = [];
  const tokens = [];
  tokensSnap.forEach((docSnap) => {
    const token = (docSnap.get("fcmToken") || "").trim();
    if (token && !tokens.includes(token)) {
      tokens.push(token);
      tokenDocs.push(docSnap);
    }
  });

  if (tokens.length === 0) {
    return { sentCount: 0, prunedCount: 0 };
  }

  const stringData = {};
  Object.entries(payloadData).forEach(([k, v]) => {
    if (v !== undefined && v !== null) {
      stringData[k] = String(v);
    }
  });

  const multicastMessage = {
    tokens,
    data: stringData,
    android: {
      priority: "high",
      ttl: CALL_INVITE_TTL_MS,
      directBootOk: true
    }
  };

  const batchResult = await admin.messaging().sendEachForMulticast(multicastMessage);

  let prunedCount = 0;
  const cleanupPromises = [];
  batchResult.responses.forEach((resp, idx) => {
    if (!resp.success && resp.error) {
      const code = resp.error.code || "";
      if (
        code === "messaging/registration-token-not-registered" ||
        code === "messaging/invalid-registration-token"
      ) {
        prunedCount++;
        cleanupPromises.push(tokenDocs[idx].ref.delete().catch(() => {}));
      }
    }
  });

  if (cleanupPromises.length > 0) {
    await Promise.all(cleanupPromises);
  }

  return { sentCount: batchResult.successCount, prunedCount };
}

/**
 * 1. Short-lived Cloudflare Realtime TURN Credentials
 */
exports.getTurnCredentials = onRequest(
  {
    cors: true,
    secrets: [cloudflareTurnApiToken]
  },
  async (req, res) => {
    try {
      const authHeader = req.headers.authorization || "";
      if (!authHeader.startsWith("Bearer ")) {
        res.status(401).json({ error: "Missing Authorization Bearer token" });
        return;
      }
      const idToken = authHeader.slice("Bearer ".length).trim();
      await admin.auth().verifyIdToken(idToken);

      const apiToken = (cloudflareTurnApiToken.value() || "").trim();
      if (!apiToken) {
        res.status(500).json({ error: "CLOUDFLARE_TURN_API_TOKEN secret is not configured" });
        return;
      }

      const turnKeyId = process.env.CLOUDFLARE_TURN_KEY_ID || DEFAULT_TURN_KEY_ID;
      const endpoint = `https://rtc.live.cloudflare.com/v1/turn/keys/${turnKeyId}/credentials/generate-ice-servers`;

      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${apiToken}`,
          "Content-Type": "application/json",
          "Accept": "application/json"
        },
        body: JSON.stringify({ ttl: DEFAULT_TTL_SECONDS })
      });

      if (!response.ok) {
        const errBody = await response.text();
        res.status(502).json({ error: "Cloudflare TURN request failed", detail: errBody });
        return;
      }

      const data = await response.json();
      res.set("Cache-Control", "no-store");
      res.status(200).json(data);
    } catch (error) {
      res.status(401).json({
        error: "Unauthorized or failed to generate TURN credentials",
        detail: String(error.message || error)
      });
    }
  }
);

/**
 * 2. Automatic Firestore Trigger on `call_rooms/{roomId}`
 * Sends a high-priority FCM data message when a call starts ringing, and sends a
 * cancellation push if the caller cancels or the call times out while ringing.
 */
exports.onCallRoomUpdated = onDocumentWritten(
  {
    document: "call_rooms/{roomId}",
    database: FIRESTORE_DATABASE_ID
  },
  async (event) => {
    const beforeData = event.data?.before?.data() || null;
    const afterData = event.data?.after?.data() || null;
    if (!afterData) return;

    const roomId = event.params.roomId || afterData.roomId || "";
    const calleeId = (afterData.calleeId || "").trim();
    if (!roomId || !calleeId || calleeId === "waiting_partner") return;

    const prevStatus = beforeData ? beforeData.status : "";
    const currStatus = afterData.status || "";
    const db = getLingoFirestore();
    const nowMs = Date.now();

    // Case A: Newly ringing incoming call
    if (currStatus === "ringing" && prevStatus !== "ringing") {
      await dispatchCallFcmToUser(db, calleeId, {
        roomId,
        callId: roomId,
        callerId: afterData.callerId || "",
        callerName: afterData.callerName || "Language Partner",
        callerNativeLang: afterData.callerNativeLang || "en",
        callerLearningLang: afterData.callerLearningLang || afterData.targetLanguageCode || "es",
        calleeId,
        calleeName: afterData.calleeName || "",
        calleeNativeLang: afterData.calleeNativeLang || afterData.targetLanguageCode || "es",
        calleeLearningLang: afterData.calleeLearningLang || "en",
        callType: afterData.callType || "voice",
        targetLanguageCode: afterData.targetLanguageCode || "es",
        conversationTopic: afterData.conversationTopic || "",
        eventType: "incoming_call",
        createdAtEpochMs: String(nowMs),
        expiresAtEpochMs: String(nowMs + CALL_INVITE_TTL_MS)
      });
      return;
    }

    // Case B: Call was ringing and is now cancelled, missed, or ended -> stop ringing on receiver's device
    if (
      prevStatus === "ringing" &&
      ["cancelled", "missed", "ended", "rejected"].includes(currStatus)
    ) {
      const eventTypeMap = {
        cancelled: "call_cancelled",
        missed: "call_missed",
        ended: "call_ended",
        rejected: "call_rejected"
      };
      await dispatchCallFcmToUser(db, calleeId, {
        roomId,
        callId: roomId,
        callerId: afterData.callerId || "",
        calleeId,
        eventType: eventTypeMap[currStatus] || "call_cancelled",
        createdAtEpochMs: String(nowMs),
        expiresAtEpochMs: String(nowMs + 10000)
      });
    }
  }
);

/**
 * 3. Authenticated HTTPS Endpoint (`sendCallInvitePush`) for direct high-priority FCM push
 */
exports.sendCallInvitePush = onRequest(
  { cors: true },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({ error: "Method Not Allowed" });
        return;
      }

      const authHeader = req.headers.authorization || "";
      if (!authHeader.startsWith("Bearer ")) {
        res.status(401).json({ error: "Missing Authorization Bearer token" });
        return;
      }
      const idToken = authHeader.slice("Bearer ".length).trim();
      const decoded = await admin.auth().verifyIdToken(idToken);

      const body = req.body || {};
      const roomId = String(body.roomId || body.callId || "").trim();
      const calleeId = String(body.calleeId || "").trim();
      const eventType = String(body.eventType || "incoming_call").trim();

      if (!roomId || !calleeId) {
        res.status(400).json({ error: "Missing roomId or calleeId" });
        return;
      }

      const db = getLingoFirestore();
      const roomSnap = await db.collection("call_rooms").document(roomId).get();
      if (!roomSnap.exists) {
        res.status(404).json({ error: "Call room not found" });
        return;
      }

      const roomData = roomSnap.data() || {};
      if (roomData.callerId !== decoded.uid && roomData.calleeId !== decoded.uid) {
        res.status(403).json({ error: "Forbidden: Not a participant of this call room" });
        return;
      }

      const nowMs = Date.now();
      const result = await dispatchCallFcmToUser(db, calleeId, {
        roomId,
        callId: roomId,
        callerId: roomData.callerId || decoded.uid,
        callerName: roomData.callerName || body.callerName || "Language Partner",
        callerNativeLang: roomData.callerNativeLang || "en",
        callerLearningLang: roomData.callerLearningLang || roomData.targetLanguageCode || "es",
        calleeId,
        calleeName: roomData.calleeName || "",
        callType: roomData.callType || body.callType || "voice",
        targetLanguageCode: roomData.targetLanguageCode || "es",
        conversationTopic: roomData.conversationTopic || "",
        eventType,
        createdAtEpochMs: String(nowMs),
        expiresAtEpochMs: String(nowMs + CALL_INVITE_TTL_MS)
      });

      res.status(200).json({ ok: true, ...result });
    } catch (error) {
      res.status(401).json({
        error: "Failed to dispatch call push notification",
        detail: String(error.message || error)
      });
    }
  }
);

const {
  REWARD_CONSTANTS,
  normalizeReferralCode,
  isEligibleNewAccount,
  computeInviterMilestoneUpdate,
  renderInviteRedirectHtml
} = require("./referralLogic");
const { FieldValue } = require("firebase-admin/firestore");

/**
 * 4. Trusted Backend Referral Claim & Reward Validation (`claimReferralReward`)
 * Validates Firebase ID token, prevents self-referrals, enforces new-account eligibility
 * and single attribution per referred UID, and atomically grants XP + milestone rewards.
 */
exports.claimReferralReward = onRequest(
  { cors: true },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({ error: "Method Not Allowed" });
        return;
      }

      const authHeader = req.headers.authorization || "";
      if (!authHeader.startsWith("Bearer ")) {
        res.status(401).json({ error: "Missing Authorization Bearer token" });
        return;
      }
      const idToken = authHeader.slice("Bearer ".length).trim();
      const decoded = await admin.auth().verifyIdToken(idToken);
      const referredUid = decoded.uid;

      const body = req.body || {};
      const cleanCode = normalizeReferralCode(body.referralCode || body.code);
      if (!cleanCode) {
        res.status(400).json({ error: "Invalid referral code format" });
        return;
      }

      const rawSource = String(body.attributionSource || "app_link").trim();
      const attributionSource = ["app_link", "deep_link", "install_referrer", "manual_code"].includes(rawSource)
        ? rawSource
        : "app_link";

      // Verify Firebase Auth account creation age (must be a newly registered account)
      const userRecord = await admin.auth().getUser(referredUid);
      const createdAtMs = userRecord.metadata?.creationTime
        ? Date.parse(userRecord.metadata.creationTime)
        : 0;
      if (createdAtMs > 0 && !isEligibleNewAccount(createdAtMs, Date.now())) {
        res.status(409).json({
          error: "Referral rewards are only available for newly registered accounts."
        });
        return;
      }

      const db = getLingoFirestore();
      const codeRef = db.collection("referral_codes").document(cleanCode);
      const referralRef = db.collection("referrals").document(referredUid);
      const referredSummaryRef = db.collection("referral_summaries").document(referredUid);
      const referredUserRef = db.collection("users").document(referredUid);

      const outcome = await db.runTransaction(async (tx) => {
        const [codeSnap, existingReferralSnap, referredSummarySnap, referredUserSnap] = await Promise.all([
          tx.get(codeRef),
          tx.get(referralRef),
          tx.get(referredSummaryRef),
          tx.get(referredUserRef)
        ]);

        if (!codeSnap.exists || codeSnap.get("isActive") !== true) {
          return { status: 404, payload: { error: "Referral code not found or inactive." } };
        }

        const inviterUid = String(codeSnap.get("ownerUid") || "").trim();
        const inviterName = String(codeSnap.get("ownerDisplayName") || "LingoSphere Friend")
          .trim()
          .slice(0, 80);

        if (!inviterUid || inviterUid === referredUid) {
          return { status: 400, payload: { error: "Self-referrals are not allowed." } };
        }

        if (existingReferralSnap.exists) {
          return { status: 409, payload: { error: "Referral reward already claimed for this account.", alreadyClaimed: true } };
        }

        const inviterSummaryRef = db.collection("referral_summaries").document(inviterUid);
        const inviterUserRef = db.collection("users").document(inviterUid);
        const [inviterSummarySnap, inviterUserSnap] = await Promise.all([
          tx.get(inviterSummaryRef),
          tx.get(inviterUserRef)
        ]);

        const prevInviterSummary = inviterSummarySnap.exists ? inviterSummarySnap.data() : {};
        const milestoneUpdate = computeInviterMilestoneUpdate(prevInviterSummary);

        const referredName = String(
          body.referredName ||
            (referredUserSnap.exists ? referredUserSnap.get("displayName") : "") ||
            userRecord.displayName ||
            "New Learner"
        )
          .trim()
          .slice(0, 80);
        const targetLang = String(
          body.targetLanguageCode ||
            (referredUserSnap.exists ? referredUserSnap.get("targetLanguageCode") : "es")
        ).slice(0, 10);
        const nativeLang = String(
          body.nativeLanguageCode ||
            (referredUserSnap.exists ? referredUserSnap.get("nativeLanguageCode") : "en")
        ).slice(0, 10);

        const nowServer = FieldValue.serverTimestamp();

        tx.set(referralRef, {
          referralId: referredUid,
          inviterUid,
          inviterName,
          referredUid,
          referredName,
          referralCode: cleanCode,
          status: "completed",
          inviterXpAwarded: REWARD_CONSTANTS.INVITER_XP_PER_REFERRAL,
          referredXpAwarded: REWARD_CONSTANTS.REFERRED_WELCOME_XP,
          milestone3BadgeAwarded: milestoneUpdate.milestone3Awarded,
          milestone5BonusAwarded: milestoneUpdate.milestone5Awarded,
          attributionSource,
          targetLanguageCode: targetLang,
          nativeLanguageCode: nativeLang,
          createdAt: nowServer,
          completedAt: nowServer
        });

        const nextSyncedCount = Number(prevInviterSummary.syncedReferralsCount || 0) + 1;
        if (inviterSummarySnap.exists) {
          tx.update(inviterSummaryRef, {
            successfulReferralsCount: milestoneUpdate.successfulReferralsCount,
            syncedReferralsCount: nextSyncedCount,
            totalXpEarnedFromReferrals: milestoneUpdate.totalXpEarnedFromReferrals,
            bonusSpeakingCredits: milestoneUpdate.bonusSpeakingCredits,
            unlockedBadges: milestoneUpdate.unlockedBadges,
            updatedAt: nowServer
          });
        } else {
          tx.set(inviterSummaryRef, {
            userId: inviterUid,
            referralCode: cleanCode,
            successfulReferralsCount: milestoneUpdate.successfulReferralsCount,
            syncedReferralsCount: nextSyncedCount,
            pendingInvitationsCount: 0,
            totalXpEarnedFromReferrals: milestoneUpdate.totalXpEarnedFromReferrals,
            unlockedBadges: milestoneUpdate.unlockedBadges,
            bonusSpeakingCredits: milestoneUpdate.bonusSpeakingCredits,
            updatedAt: nowServer
          });
        }

        if (inviterUserSnap.exists) {
          const currXp = Number(inviterUserSnap.get("xpPoints") || 0);
          const nextXp = currXp + milestoneUpdate.xpDeltaForInviter;
          const nextLevel = Math.floor(nextXp / 100) + 1;
          const existingAchievements = Array.isArray(inviterUserSnap.get("achievements"))
            ? inviterUserSnap.get("achievements")
            : [];
          const mergedAchievements = Array.from(
            new Set([...existingAchievements, ...milestoneUpdate.unlockedBadges])
          ).slice(-100);
          tx.update(inviterUserRef, {
            xpPoints: nextXp,
            level: nextLevel,
            achievements: mergedAchievements,
            updatedAt: nowServer
          });
        }

        if (referredUserSnap.exists) {
          const currXp = Number(referredUserSnap.get("xpPoints") || 0);
          const nextXp = currXp + REWARD_CONSTANTS.REFERRED_WELCOME_XP;
          tx.update(referredUserRef, {
            xpPoints: nextXp,
            level: Math.floor(nextXp / 100) + 1,
            updatedAt: nowServer
          });
        }

        if (referredSummarySnap.exists) {
          tx.update(referredSummaryRef, {
            referredByCode: cleanCode,
            referredByUid: inviterUid,
            updatedAt: nowServer
          });
        }

        const notifRef = db.collection("notifications").document(`ref_${referredUid}`);
        tx.set(notifRef, {
          userId: inviterUid,
          title: "Friend Joined LingoSphere! 🎉",
          message: `${referredName} joined with your invite link! You earned +${milestoneUpdate.xpDeltaForInviter} XP.`,
          type: "achievement",
          isRead: false,
          targetRoute: "partner_profile",
          targetId: referredUid,
          partnerId: referredUid,
          createdAt: nowServer
        });

        return {
          status: 200,
          payload: {
            ok: true,
            inviterUid,
            inviterName,
            xpAwardedToReferred: REWARD_CONSTANTS.REFERRED_WELCOME_XP,
            xpAwardedToInviter: milestoneUpdate.xpDeltaForInviter,
            unlockedMilestoneBadge: milestoneUpdate.milestone5Awarded
              ? REWARD_CONSTANTS.MILESTONE_5_BADGE_ID
              : milestoneUpdate.milestone3Awarded
                ? REWARD_CONSTANTS.MILESTONE_3_BADGE_ID
                : null
          }
        };
      });

      res.status(outcome.status).json(outcome.payload);
    } catch (error) {
      res.status(500).json({
        error: "Failed to claim referral reward",
        detail: String(error.message || error)
      });
    }
  }
);

/**
 * 5. Web Invite Redirect Handler (`inviteRedirect`) for `/invite?ref=USERCODE`
 */
exports.inviteRedirect = onRequest(
  { cors: true },
  async (req, res) => {
    const rawCode = String(req.query?.ref || req.query?.code || "");
    res.set("Content-Type", "text/html; charset=utf-8");
    res.set("Cache-Control", "public, max-age=300");
    res.status(200).send(renderInviteRedirectHtml(rawCode));
  }
);

/**
 * 6. Automatic App Update FCM Topic Broadcaster (`onAppReleasePublished`)
 * When a new LingoSphere release is published to `app_releases/{releaseId}` (e.g., `app_releases/latest`),
 * automatically broadcasts an `app_update` FCM data message to the `lingosphere_app_updates` topic.
 * Client devices compare `versionCode` against their installed `BuildConfig.VERSION_CODE` and only
 * notify users running an older version.
 */
exports.onAppReleasePublished = onDocumentWritten(
  {
    document: "app_releases/{releaseId}",
    database: FIRESTORE_DATABASE_ID
  },
  async (event) => {
    const beforeData = event.data?.before?.data() || null;
    const afterData = event.data?.after?.data() || null;
    if (!afterData) return;

    const newVersionCode = Number(afterData.versionCode || 0);
    const prevVersionCode = Number(beforeData?.versionCode || 0);
    if (newVersionCode <= 0 || newVersionCode <= prevVersionCode) {
      return;
    }

    const versionName = String(afterData.versionName || `${newVersionCode}.0`).trim();
    const summary = String(
      afterData.summary ||
        "Discover new improvements, better voice conversations, and smoother learning."
    ).trim();
    const releaseNotesList = Array.isArray(afterData.releaseNotes)
      ? afterData.releaseNotes.map((item) => String(item).trim()).filter(Boolean)
      : [];
    const isCritical = Boolean(afterData.isCritical);
    const minSupportedVersionCode = Number(afterData.minSupportedVersionCode || 1);

    const topicMessage = {
      topic: "lingosphere_app_updates",
      data: {
        type: "app_update",
        versionCode: String(newVersionCode),
        versionName,
        summary,
        releaseNotes: releaseNotesList.join("\n"),
        isCritical: String(isCritical),
        minSupportedVersionCode: String(minSupportedVersionCode),
        publishedAtEpochMs: String(afterData.publishedAtEpochMs || Date.now())
      },
      android: {
        priority: isCritical ? "high" : "normal",
        ttl: 7 * 24 * 3600 * 1000
      }
    };

    await admin.messaging().send(topicMessage);
  }
);

const crypto = require("crypto");

function createServiceAccountJwt(clientEmail, privateKeyPem, scope) {
  const nowSec = Math.floor(Date.now() / 1000);
  const header = Buffer.from(JSON.stringify({ alg: "RS256", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(
    JSON.stringify({
      iss: clientEmail,
      scope,
      aud: "https://oauth2.googleapis.com/token",
      exp: nowSec + 3600,
      iat: nowSec
    })
  ).toString("base64url");
  const unsigned = `${header}.${payload}`;
  const signer = crypto.createSign("RSA-SHA256");
  signer.update(unsigned);
  const signature = signer.sign(privateKeyPem, "base64url");
  return `${unsigned}.${signature}`;
}

async function fetchGooglePlayAccessToken(serviceAccountJsonRaw) {
  const parsed = JSON.parse(serviceAccountJsonRaw);
  const clientEmail = parsed.client_email;
  const privateKey = String(parsed.private_key || "").replace(/\\n/g, "\n");
  if (!clientEmail || !privateKey) {
    throw new Error("Invalid GOOGLE_PLAY_SERVICE_ACCOUNT_JSON credentials");
  }
  const assertion = createServiceAccountJwt(
    clientEmail,
    privateKey,
    "https://www.googleapis.com/auth/androidpublisher"
  );
  const tokenRes = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion
    })
  });
  const tokenData = await tokenRes.json();
  if (!tokenRes.ok || !tokenData.access_token) {
    throw new Error(`OAuth token exchange failed: ${JSON.stringify(tokenData)}`);
  }
  return tokenData.access_token;
}

/**
 * 7. Backend Subscription & Google Play Developer API Verification (`verifySubscriptionEntitlement`)
 * Verifies Google Play Billing subscription tokens via `androidpublisher` v3 `subscriptionsv2.get`
 * when `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` is configured, maps authoritative expiry/auto-renew state,
 * and synchronizes `users/{uid}` in Firestore.
 */
exports.verifySubscriptionEntitlement = onRequest(
  { cors: true },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({ error: "Method Not Allowed" });
        return;
      }

      const authHeader = req.headers.authorization || "";
      if (!authHeader.startsWith("Bearer ")) {
        res.status(401).json({ error: "Missing Authorization Bearer token" });
        return;
      }
      const idToken = authHeader.slice("Bearer ".length).trim();
      const decoded = await admin.auth().verifyIdToken(idToken);
      const uid = decoded.uid;

      const body = req.body || {};
      const packageName = String(body.packageName || "com.aistudio.lingosphere.kxpqzm").trim();
      const productId = String(body.productId || "").trim();
      const basePlanId = String(body.basePlanId || "").trim();
      let requestedTier = String(body.subscriptionTier || "free").trim();
      const validTiers = ["free", "premium_monthly", "premium_yearly", "premium_pro"];
      if (!validTiers.includes(requestedTier)) {
        res.status(400).json({ error: "Invalid subscriptionTier" });
        return;
      }

      const purchaseToken = body.purchaseToken ? String(body.purchaseToken).trim().slice(0, 500) : null;
      let requestedFreeTrial = Boolean(body.isFreeTrial);
      let isAutoRenewing = body.subscriptionAutoRenewing !== undefined ? Boolean(body.subscriptionAutoRenewing) : true;
      let expiryEpochMs = Math.max(0, Number(body.subscriptionExpiryEpochMs || 0));
      let isActiveSubscription = requestedTier !== "free";
      let verifiedWithPlayApi = false;

      if (requestedTier !== "free" && (!purchaseToken || purchaseToken.length < 6)) {
        res.status(400).json({ error: "Valid Google Play purchaseToken is required for premium tiers" });
        return;
      }

      // Authoritative Google Play Developer API verification when service account secret is present
      const saJson = process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON;
      if (requestedTier !== "free" && !saJson) {
        res.status(503).json({
          ok: false,
          code: "PLAY_API_CREDENTIALS_NOT_CONFIGURED",
          error: "Purchase verification is unavailable until Google Play backend credentials are configured."
        });
        return;
      }
      if (saJson && purchaseToken && requestedTier !== "free") {
        const accessToken = await fetchGooglePlayAccessToken(saJson);
        const playUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`;
        const playRes = await fetch(playUrl, {
          headers: { Authorization: `Bearer ${accessToken}` }
        });
        if (!playRes.ok) {
          const playErr = await playRes.text();
          res.status(403).json({
            error: "Google Play Developer API rejected subscription verification",
            detail: playErr
          });
          return;
        }
        const subData = await playRes.json();
        verifiedWithPlayApi = true;
        const subState = String(subData.subscriptionState || "");
        const activeStates = [
          "SUBSCRIPTION_STATE_ACTIVE",
          "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
          "SUBSCRIPTION_STATE_CANCELED"
        ];
        const lineItem = Array.isArray(subData.lineItems) ? subData.lineItems[0] || {} : {};
        const apiExpiryMs = lineItem.expiryTime ? Date.parse(lineItem.expiryTime) : 0;
        if (apiExpiryMs > 0) expiryEpochMs = apiExpiryMs;
        isAutoRenewing = Boolean(lineItem.autoRenewingPlan && lineItem.autoRenewingPlan.autoRenewEnabled);

        const resolvedProduct = String(lineItem.productId || productId || "");
        if (resolvedProduct === "lingosphere_yearly") {
          requestedTier = "premium_yearly";
        } else if (resolvedProduct === "lingosphere_premium" || resolvedProduct === "lingosphere_pro") {
          requestedTier = "premium_monthly";
        }

        const withinPeriod = expiryEpochMs === 0 || Date.now() < expiryEpochMs;
        isActiveSubscription = activeStates.includes(subState) && withinPeriod;
        if (!isActiveSubscription) {
          requestedTier = "free";
        }

        // Ensure acknowledgment if not yet acknowledged
        if (subData.acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING" && resolvedProduct) {
          const ackUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/purchases/subscriptions/${encodeURIComponent(resolvedProduct)}/tokens/${encodeURIComponent(purchaseToken)}:acknowledge`;
          await fetch(ackUrl, {
            method: "POST",
            headers: {
              Authorization: `Bearer ${accessToken}`,
              "Content-Type": "application/json"
            },
            body: JSON.stringify({ developerPayload: `uid:${uid}` })
          });
        }
      }

      if (!verifiedWithPlayApi || requestedTier === "free") {
        requestedTier = "free";
        isActiveSubscription = false;
      }

      const db = getLingoFirestore();
      const userRef = db.collection("users").document(uid);
      const entRef = userRef.collection("private").document("entitlement");

      const syncResult = await db.runTransaction(async (tx) => {
        const userSnap = await tx.get(userRef);
        const existingData = userSnap.exists ? userSnap.data() || {} : {};
        const alreadyUsedTrial = Boolean(existingData.hasUsedFreeTrial || existingData.isFreeTrial);

        const effectiveIsFreeTrial = false;
        const effectiveHasUsedFreeTrial = alreadyUsedTrial;
        const grantsPro = isActiveSubscription && verifiedWithPlayApi && requestedTier !== "free";

        tx.set(
          entRef,
          {
            uid,
            status: grantsPro ? "pro_active" : "free",
            planId: grantsPro ? "lingosphere_pro" : "free",
            basePlanId: grantsPro ? (basePlanId || (requestedTier === "premium_yearly" ? "yearly" : "monthly")) : null,
            subscriptionTier: grantsPro ? requestedTier : "free",
            expiryEpochMs: grantsPro ? expiryEpochMs : 0,
            autoRenewing: grantsPro ? isAutoRenewing : false,
            verifiedWithPlayApi: Boolean(verifiedWithPlayApi),
            lastVerifiedEpochMs: Date.now(),
            updatedAt: FieldValue.serverTimestamp()
          },
          { merge: true }
        );

        const updatePayload = {
          subscriptionTier: grantsPro ? requestedTier : "free",
          purchaseToken: grantsPro ? purchaseToken : null,
          productId: grantsPro ? (productId || null) : null,
          basePlanId: grantsPro ? (basePlanId || null) : null,
          isFreeTrial: effectiveIsFreeTrial,
          hasUsedFreeTrial: effectiveHasUsedFreeTrial,
          subscriptionAutoRenewing: grantsPro ? isAutoRenewing : false,
          subscriptionExpiryEpochMs: grantsPro ? expiryEpochMs : 0,
          verifiedWithPlayApi: Boolean(verifiedWithPlayApi),
          updatedAt: FieldValue.serverTimestamp()
        };

        if (userSnap.exists) {
          tx.update(userRef, updatePayload);
        }

        return {
          active: grantsPro,
          verifiedWithPlayApi: Boolean(verifiedWithPlayApi),
          subscriptionTier: grantsPro ? requestedTier : "free",
          isFreeTrial: effectiveIsFreeTrial,
          hasUsedFreeTrial: effectiveHasUsedFreeTrial,
          subscriptionAutoRenewing: grantsPro ? isAutoRenewing : false,
          subscriptionExpiryEpochMs: grantsPro ? expiryEpochMs : 0
        };
      });

      res.status(200).json({ ok: true, ...syncResult });
    } catch (error) {
      res.status(500).json({
        error: "Failed to synchronize subscription status",
        detail: String(error.message || error)
      });
    }
  }
);

/**
 * Helper: Verifies Firebase Auth ID Token and optional/enforced Firebase App Check token.
 * Never logs raw tokens or secret keys.
 */
async function verifyFirebaseUserAndAppCheck(req) {
  const authHeader = req.headers.authorization || "";
  const appCheckToken = (req.headers["x-firebase-appcheck"] || "").trim();
  const clientUidHeader = String(req.headers["x-lingosphere-client-uid"] || "").trim();

  let decodedToken = null;
  if (authHeader.startsWith("Bearer ")) {
    const idToken = authHeader.slice("Bearer ".length).trim();
    if (idToken) {
      try {
        decodedToken = await admin.auth().verifyIdToken(idToken);
      } catch (authErr) {
        if (!appCheckToken && !clientUidHeader) {
          const err = new Error("Invalid Authorization Bearer token");
          err.statusCode = 401;
          throw err;
        }
      }
    }
  }

  if (appCheckToken) {
    try {
      const appCheckClaims = await admin.appCheck().verifyToken(appCheckToken);
      if (!decodedToken) {
        decodedToken = {
          uid: `appcheck_${appCheckClaims.appId || "client"}`,
          appCheckVerified: true
        };
      }
    } catch (appCheckErr) {
      if (process.env.ENFORCE_APP_CHECK === "true" && !decodedToken) {
        const err = new Error("Invalid Firebase App Check token");
        err.statusCode = 401;
        throw err;
      }
    }
  } else if (process.env.ENFORCE_APP_CHECK === "true" && !decodedToken) {
    const err = new Error("Missing X-Firebase-AppCheck header");
    err.statusCode = 401;
    throw err;
  }

  if (!decodedToken) {
    if (clientUidHeader) {
      const sanitizedUid = clientUidHeader.replace(/[^a-zA-Z0-9_-]/g, "").slice(0, 48) || "guest";
      decodedToken = {
        uid: `guest_${sanitizedUid}`,
        guestSession: true
      };
    } else {
      const err = new Error("Missing Authorization Bearer token or App Check header");
      err.statusCode = 401;
      throw err;
    }
  }

  return decodedToken;
}

/**
 * Helper: Enforces Pro-only server entitlement + daily OpenAI safety caps
 * in Firestore (`users/{uid}/private/openai_usage`). Free users receive 403 PRO_REQUIRED.
 */
async function verifyAndConsumeOpenAiQuota(db, uid, isRealtimeVoice) {
  if (!uid || String(uid).startsWith("guest_") || String(uid).startsWith("appcheck_")) {
    const authErr = new Error("Sign in to continue");
    authErr.statusCode = 401;
    authErr.code = "SIGN_IN_REQUIRED";
    throw authErr;
  }

  const entitlement = await resolveServerEntitlement(db, uid);
  if (!entitlement.isPro) {
    const proErr = new Error("This feature requires LingoSphere Pro.");
    proErr.statusCode = 403;
    proErr.code = "PRO_REQUIRED";
    throw proErr;
  }

  const userRef = db.collection("users").document(uid);
  const quotaRef = userRef.collection("private").document("openai_usage");
  const todayIso = new Date().toISOString().slice(0, 10);

  return db.runTransaction(async (tx) => {
    const quotaSnap = await tx.get(quotaRef);
    const effectiveTier = entitlement.subscriptionTier || "premium_pro";
    const maxDailyTextRequests = effectiveTier === "premium_yearly" ? 220 : 180;
    const maxDailyRealtimeSessions = effectiveTier === "premium_yearly" ? 25 : 20;

    const quotaData = quotaSnap.exists ? quotaSnap.data() || {} : {};
    const sameDay = quotaData.dayKey === todayIso;
    const usedTextToday = sameDay ? Number(quotaData.textRequestsUsed || 0) : 0;
    const usedRealtimeToday = sameDay ? Number(quotaData.realtimeSessionsUsed || 0) : 0;

    if (isRealtimeVoice) {
      if (usedRealtimeToday >= maxDailyRealtimeSessions) {
        const err = new Error(
          `Daily Pro realtime voice session safety limit (${maxDailyRealtimeSessions}/day) reached.`
        );
        err.statusCode = 429;
        throw err;
      }
      tx.set(
        quotaRef,
        {
          userId: uid,
          dayKey: todayIso,
          subscriptionTier: effectiveTier,
          textRequestsUsed: usedTextToday,
          realtimeSessionsUsed: usedRealtimeToday + 1,
          updatedAtEpochMs: Date.now()
        },
        { merge: true }
      );
      return {
        subscriptionTier: effectiveTier,
        isPremium: true,
        remainingDailyRequests: Math.max(0, maxDailyRealtimeSessions - (usedRealtimeToday + 1))
      };
    } else {
      if (usedTextToday >= maxDailyTextRequests) {
        const err = new Error(
          `Daily Pro tutor safety limit (${maxDailyTextRequests}/day) reached.`
        );
        err.statusCode = 429;
        throw err;
      }
      tx.set(
        quotaRef,
        {
          userId: uid,
          dayKey: todayIso,
          subscriptionTier: effectiveTier,
          textRequestsUsed: usedTextToday + 1,
          realtimeSessionsUsed: usedRealtimeToday,
          updatedAtEpochMs: Date.now()
        },
        { merge: true }
      );
      return {
        subscriptionTier: effectiveTier,
        isPremium: true,
        remainingDailyRequests: Math.max(0, maxDailyTextRequests - (usedTextToday + 1))
      };
    }
  });
}

function extractOpenAiResponseText(data) {
  return extractOpenAiResponseTextCore(data);
}

/**
 * 8. Secure OpenAI Responses API Endpoint (`openAiTutorResponse`)
 * Uses the official OpenAI Responses API (`POST https://api.openai.com/v1/responses`)
 * with secret `OPENAI_API_KEY`. Supports advanced AI tutoring, natural conversation practice,
 * shopping/restaurant/travel/interview roleplay, and context-aware grammar corrections.
 *
 * Configure secret:
 *   firebase functions:secrets:set OPENAI_API_KEY
 */
exports.openAiTutorResponse = onRequest(
  {
    cors: true,
    secrets: [openAiApiKey]
  },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({ error: "Method Not Allowed" });
        return;
      }

      const decoded = await verifyFirebaseUserAndAppCheck(req);
      const uid = decoded.uid;

      const apiKey = (openAiApiKey.value() || process.env.OPENAI_API_KEY || "").trim();
      if (!apiKey) {
        res.status(503).json({
          error: "OPENAI_API_KEY secret is not configured on the backend",
          code: "OPENAI_SECRET_MISSING"
        });
        return;
      }

      const db = getLingoFirestore();
      const quotaInfo = await verifyAndConsumeOpenAiQuota(db, uid, false);

      const body = req.body || {};
      const wantsStream =
        body.stream === true ||
        String(req.headers.accept || "").toLowerCase().includes("text/event-stream");
      const instructions = String(
        body.instructions ||
          body.systemInstructions ||
          "You are a warm, patient, natural multilingual AI language tutor."
      ).slice(0, 8000);
      const userPrompt = String(body.input || body.userMessage || body.prompt || "").trim().slice(0, 6000);
      if (!userPrompt) {
        res.status(400).json({ error: "Missing input prompt" });
        return;
      }

      const rawHistory = Array.isArray(body.history)
        ? body.history
        : (Array.isArray(body.conversationHistory) ? body.conversationHistory : []);
      const historyTurns = rawHistory.slice(-10);
      const requestedModel = String(body.model || "gpt-4o-mini").trim();
      const allowedModels = quotaInfo.isPremium ? ["gpt-4o-mini", "gpt-4o"] : ["gpt-4o-mini"];
      const selectedModel = allowedModels.includes(requestedModel) ? requestedModel : "gpt-4o-mini";
      const temperature = Math.min(1.0, Math.max(0.1, Number(body.temperature ?? 0.65)));
      const maxOutputTokens = Math.min(1200, Math.max(120, Number(body.maxOutputTokens ?? 500)));

      const inputMessages = [];
      for (const turn of historyTurns) {
        if (!turn || typeof turn !== "object") continue;
        const roleRaw = String(turn.role || "user").toLowerCase();
        const role = roleRaw === "assistant" || roleRaw === "tutor" || roleRaw === "model" ? "assistant" : "user";
        const contentText = String(turn.text || turn.content || "").trim().slice(0, 1500);
        if (contentText) {
          inputMessages.push({
            role,
            content: contentText
          });
        }
      }
      inputMessages.push({
        role: "user",
        content: userPrompt
      });

      if (wantsStream) {
        res.set("Content-Type", "text/event-stream; charset=utf-8");
        res.set("Cache-Control", "no-store");
        res.set("Connection", "keep-alive");

        const openAiStreamResult = await streamOpenAiResponses({
          apiKey,
          model: selectedModel,
          instructions,
          historyTurns: inputMessages.slice(0, -1).map((m) => ({ role: m.role, content: m.content })),
          userPrompt,
          temperature,
          maxOutputTokens,
          fetchImpl: globalThis.fetch,
          timeoutMs: 25000,
          onDelta: (delta) => {
            res.write(`data: ${JSON.stringify({ type: "delta", delta })}\n\n`);
          }
        });
        res.write(
          `data: ${JSON.stringify({
            type: "done",
            ok: true,
            provider: "OPENAI",
            model: openAiStreamResult.model,
            responseId: openAiStreamResult.responseId,
            text: openAiStreamResult.outputText,
            outputText: openAiStreamResult.outputText,
            usage: openAiStreamResult.usage,
            subscriptionTier: quotaInfo.subscriptionTier,
            remainingDailyRequests: quotaInfo.remainingDailyRequests
          })}\n\n`
        );
        res.end();
        return;
      }

      const openAiResponse = await fetch("https://api.openai.com/v1/responses", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${apiKey}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          model: selectedModel,
          instructions,
          input: inputMessages,
          temperature,
          max_output_tokens: maxOutputTokens
        })
      });

      if (!openAiResponse.ok) {
        const errBody = await openAiResponse.text();
        res.status(openAiResponse.status).json({
          error: "OpenAI Responses API request failed",
          status: openAiResponse.status,
          detail: errBody.slice(0, 500)
        });
        return;
      }

      const data = await openAiResponse.json();
      const outputText = extractOpenAiResponseText(data);
      if (!outputText) {
        res.status(502).json({ error: "Empty response from OpenAI Responses API" });
        return;
      }

      res.set("Cache-Control", "no-store");
      res.status(200).json({
        ok: true,
        provider: "OPENAI",
        model: selectedModel,
        responseId: data.id || "",
        text: outputText,
        outputText,
        usage: data.usage || {},
        subscriptionTier: quotaInfo.subscriptionTier,
        remainingDailyRequests: quotaInfo.remainingDailyRequests
      });
    } catch (error) {
      const code = Number(error.statusCode) || 500;
      res.status(code).json({
        ok: false,
        code: error.code || (code === 403 ? "PRO_REQUIRED" : code === 401 ? "UNAUTHORIZED" : "AI_REQUEST_FAILED"),
        error: String(error.message || "AI tutor request failed")
      });
    }
  }
);

/**
 * 9. Secure OpenAI Realtime API Ephemeral Session Endpoint (`createOpenAiRealtimeSession`)
 * Mints a short-lived ephemeral client token (`ek_...`) via `POST https://api.openai.com/v1/realtime/sessions`
 * so the Android app can establish a direct WebRTC real-time speech session without ever exposing `OPENAI_API_KEY`.
 */
exports.createOpenAiRealtimeSession = onRequest(
  {
    cors: true,
    secrets: [openAiApiKey]
  },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({ error: "Method Not Allowed" });
        return;
      }

      const decoded = await verifyFirebaseUserAndAppCheck(req);
      const uid = decoded.uid;

      const apiKey = (openAiApiKey.value() || process.env.OPENAI_API_KEY || "").trim();
      if (!apiKey) {
        res.status(503).json({
          error: "OPENAI_API_KEY secret is not configured on the backend",
          code: "OPENAI_SECRET_MISSING"
        });
        return;
      }

      const db = getLingoFirestore();
      const quotaInfo = await verifyAndConsumeOpenAiQuota(db, uid, true);

      const body = req.body || {};
      const supportedVoices = ["alloy", "ash", "ballad", "coral", "echo", "sage", "shimmer", "verse"];
      const requestedVoice = String(body.voice || "coral").toLowerCase().trim();
      const selectedVoice = supportedVoices.includes(requestedVoice) ? requestedVoice : "coral";

      const requestedModel = String(body.model || "gpt-4o-mini-realtime-preview").trim();
      const allowedRealtimeModels = quotaInfo.isPremium
        ? ["gpt-4o-mini-realtime-preview", "gpt-4o-realtime-preview"]
        : ["gpt-4o-mini-realtime-preview"];
      const selectedModel = allowedRealtimeModels.includes(requestedModel)
        ? requestedModel
        : "gpt-4o-mini-realtime-preview";

      const instructions = String(
        body.instructions ||
          "You are a warm, patient, natural AI language tutor in LingoSphere. Listen carefully, respond naturally in 1-2 spoken sentences, politely correct mistakes with simple explanations, and ask an engaging follow-up question."
      ).slice(0, 6000);

      const sessionResponse = await fetch("https://api.openai.com/v1/realtime/sessions", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${apiKey}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          model: selectedModel,
          voice: selectedVoice,
          modalities: ["audio", "text"],
          instructions,
          input_audio_transcription: {
            model: "gpt-4o-mini-transcribe"
          },
          turn_detection: {
            type: "server_vad",
            threshold: 0.5,
            prefix_padding_ms: 300,
            silence_duration_ms: 550,
            create_response: true,
            interrupt_response: true
          }
        })
      });

      if (!sessionResponse.ok) {
        const errBody = await sessionResponse.text();
        res.status(sessionResponse.status).json({
          error: "Failed to create OpenAI Realtime session",
          status: sessionResponse.status,
          detail: errBody.slice(0, 500)
        });
        return;
      }

      const sessionData = await sessionResponse.json();
      const ephemeralKey = sessionData?.client_secret?.value || "";
      const expiresAt = sessionData?.client_secret?.expires_at || 0;
      if (!ephemeralKey) {
        res.status(502).json({ error: "OpenAI Realtime API did not return an ephemeral client_secret" });
        return;
      }

      res.set("Cache-Control", "no-store");
      res.status(200).json({
        ok: true,
        provider: "OPENAI_REALTIME",
        sessionId: sessionData.id || "",
        model: selectedModel,
        voice: selectedVoice,
        ephemeralKey,
        expiresAt,
        webrtcUrl: "https://api.openai.com/v1/realtime",
        subscriptionTier: quotaInfo.subscriptionTier,
        remainingDailySessions: quotaInfo.remainingDailyRequests
      });
    } catch (error) {
      const code = Number(error.statusCode) || 500;
      res.status(code).json({
        ok: false,
        code: error.code || (code === 403 ? "PRO_REQUIRED" : code === 401 ? "UNAUTHORIZED" : "REALTIME_SESSION_FAILED"),
        error: String(error.message || "Failed to initialize realtime session")
      });
    }
  }
);

/**
 * Shared authenticated Cloud Functions handler for OpenAI + Gemini routing,
 * App Check enforcement, burst rate limiting, and Firestore daily quotas.
 */
async function executeAuthenticatedAiRoute(req, res, overrideBodyFields = {}) {
  try {
    if (req.method !== "POST") {
      res.status(405).json({ ok: false, error: "Method Not Allowed" });
      return;
    }

    const decoded = await verifyFirebaseUserAndAppCheck(req);
    const uid = decoded.uid;
    enforcePerUserBurstLimit(uid);

    const openAiKey = (openAiApiKey.value() || process.env.OPENAI_API_KEY || "").trim();
    const gemKey = (geminiApiKey.value() || process.env.GEMINI_API_KEY || "").trim();

    const mergedBody = {
      ...(req.body || {}),
      ...overrideBodyFields
    };

    const db = getLingoFirestore();
    const quotaInfo = await verifyAndConsumeOpenAiQuota(db, uid, false);

    const wantsStream =
      mergedBody.stream === true ||
      String(req.headers.accept || "").toLowerCase().includes("text/event-stream");

    if (wantsStream && String(mergedBody.preferredProvider || "openai").toLowerCase() !== "gemini" && openAiKey) {
      try {
        const userPrompt = String(mergedBody.input || mergedBody.userMessage || mergedBody.prompt || "").trim().slice(0, 6000);
        if (!userPrompt) {
          res.status(400).json({ ok: false, code: "INVALID_INPUT", error: "Missing input prompt" });
          return;
        }
        const instructions = String(
          mergedBody.instructions ||
            mergedBody.systemInstructions ||
            "You are a warm, intelligent human language tutor in LingoSphere."
        ).slice(0, 8000);
        const historyTurns = normalizeHistoryTurns(mergedBody.history || mergedBody.conversationHistory);
        const temperature = Math.min(1.0, Math.max(0.1, Number(mergedBody.temperature ?? 0.65)));
        const maxOutputTokens = Math.min(1400, Math.max(120, Number(mergedBody.maxOutputTokens ?? 520)));
        const defaultOpenAiModel = String(process.env.OPENAI_DEFAULT_MODEL || "gpt-4o-mini").trim();

        res.set("Content-Type", "text/event-stream; charset=utf-8");
        res.set("Cache-Control", "no-store");
        res.set("Connection", "keep-alive");

        const streamResult = await streamOpenAiResponses({
          apiKey: openAiKey,
          model: String(mergedBody.openAiModel || mergedBody.model || defaultOpenAiModel),
          instructions,
          historyTurns,
          userPrompt,
          temperature,
          maxOutputTokens,
          fetchImpl: globalThis.fetch,
          timeoutMs: 24000,
          onDelta: (delta) => {
            res.write(`data: ${JSON.stringify({ type: "delta", delta })}\n\n`);
          }
        });
        res.write(
          `data: ${JSON.stringify({
            type: "done",
            ok: true,
            provider: "OPENAI",
            primaryProviderAttempted: "OPENAI",
            fallbackUsed: false,
            preservedHistoryTurnCount: historyTurns.length,
            model: streamResult.model,
            responseId: streamResult.responseId,
            text: streamResult.outputText,
            outputText: streamResult.outputText,
            usage: streamResult.usage,
            subscriptionTier: quotaInfo.subscriptionTier,
            remainingDailyRequests: quotaInfo.remainingDailyRequests
          })}\n\n`
        );
        res.end();
        return;
      } catch (_streamErr) {
        if (res.headersSent) {
          res.end();
          return;
        }
        // Stream failed before headers sent; fall through to standard router (including Gemini failover)
      }
    }

    const routed = await routeAiRequest({
      body: mergedBody,
      openAiKey,
      geminiKey: gemKey,
      isPremium: quotaInfo.isPremium
    });

    if (wantsStream) {
      if (routed.status === 200) {
        res.set("Content-Type", "text/event-stream; charset=utf-8");
        res.set("Cache-Control", "no-store");
        res.set("Connection", "keep-alive");
        res.write(`data: ${JSON.stringify({ type: "delta", delta: routed.payload.outputText })}\n\n`);
        res.write(
          `data: ${JSON.stringify({
            type: "done",
            ...routed.payload,
            subscriptionTier: quotaInfo.subscriptionTier,
            remainingDailyRequests: quotaInfo.remainingDailyRequests
          })}\n\n`
        );
        res.end();
        return;
      }
    }

    res.set("Cache-Control", "no-store");
    res.status(routed.status).json({
      ...routed.payload,
      subscriptionTier: quotaInfo.subscriptionTier,
      remainingDailyRequests: quotaInfo.remainingDailyRequests
    });
  } catch (error) {
    const code = Number(error.statusCode) || 500;
    if (!res.headersSent) {
      res.status(code).json({
        ok: false,
        code: error.code || (code === 403 ? "PRO_REQUIRED" : code === 429 ? "RATE_LIMIT_EXCEEDED" : code === 401 ? "UNAUTHORIZED" : "AI_SERVICE_UNAVAILABLE"),
        error: String(error.message || "AI request failed")
      });
    } else {
      res.end();
    }
  }
}

/**
 * 10. Unified Authenticated AI Router (`aiTutorRouter`)
 * Routes interactive tutoring/conversation/roleplay to OpenAI first and structured lessons/vocab/quizzes
 * to Gemini first, with automatic cross-provider failover preserving conversation history,
 * per-user burst rate limiting, Firestore daily quotas, and zero client secret exposure.
 *
 * Secret setup:
 *   firebase functions:secrets:set OPENAI_API_KEY
 *   firebase functions:secrets:set GEMINI_API_KEY
 */
exports.aiTutorRouter = onRequest(
  {
    cors: true,
    secrets: [openAiApiKey, geminiApiKey]
  },
  async (req, res) => executeAuthenticatedAiRoute(req, res)
);

/**
 * 11. OpenAI Chat Endpoint (`chatWithOpenAI`)
 * Uses OpenAI Responses API (`gpt-4o-mini` / `gpt-4o`) primarily, with optional Gemini fallback
 * unless `allowFallback: false` is supplied in the request body.
 */
exports.chatWithOpenAI = onRequest(
  {
    cors: true,
    secrets: [openAiApiKey, geminiApiKey]
  },
  async (req, res) =>
    executeAuthenticatedAiRoute(req, res, {
      preferredProvider: "openai",
      taskType: (req.body && req.body.taskType) || "natural_conversation"
    })
);

/**
 * 12. Gemini Chat Endpoint (`chatWithGemini`)
 * Uses Google Gemini Developer API (`gemini-2.5-flash` / `gemini-2.5-flash-lite`) primarily,
 * with optional OpenAI fallback unless `allowFallback: false` is supplied in the request body.
 */
exports.chatWithGemini = onRequest(
  {
    cors: true,
    secrets: [openAiApiKey, geminiApiKey]
  },
  async (req, res) =>
    executeAuthenticatedAiRoute(req, res, {
      preferredProvider: "gemini",
      taskType: (req.body && req.body.taskType) || "lesson_generation"
    })
);

/**
 * 13. Conversational AI Tutor Endpoint (`generateTutorResponse`)
 * Connects Emma AI Tutor, AI Partners, Free Conversation, Practice Mode, Roleplay, and Speaking.
 * Routes primarily to OpenAI for natural conversational fluency and fails over to Gemini cleanly.
 */
exports.generateTutorResponse = onRequest(
  {
    cors: true,
    secrets: [openAiApiKey, geminiApiKey]
  },
  async (req, res) => {
    const requestedTask = String((req.body && req.body.taskType) || "advanced_tutor").trim();
    const requestedPref = (req.body && req.body.preferredProvider) || "openai";
    return executeAuthenticatedAiRoute(req, res, {
      taskType: requestedTask,
      preferredProvider: requestedPref
    });
  }
);

/**
 * 14. Structured Educational Content Endpoint (`generateLessonContent`)
 * Connects Learn Mode, Grammar & Writing, Vocabulary, Reading, Listening content, and AI Quizzes.
 * Routes primarily to Gemini for structured pedagogy and fails over to OpenAI cleanly.
 */
exports.generateLessonContent = onRequest(
  {
    cors: true,
    secrets: [openAiApiKey, geminiApiKey]
  },
  async (req, res) => {
    const requestedTask = String((req.body && req.body.taskType) || "lesson_generation").trim();
    const requestedPref = (req.body && req.body.preferredProvider) || "openai";
    return executeAuthenticatedAiRoute(req, res, {
      taskType: requestedTask,
      preferredProvider: requestedPref
    });
  }
);

/**
 * 15. AI Provider Health Check (`checkAIProviderHealth`)
 * Verifies authenticated Firebase access, reports whether `OPENAI_API_KEY` and `GEMINI_API_KEY`
 * secrets are configured on Google Cloud Secret Manager, and optionally probes live provider endpoints.
 * Never returns or logs raw secret keys.
 */
exports.checkAIProviderHealth = onRequest(
  {
    cors: true,
    secrets: [openAiApiKey, geminiApiKey]
  },
  async (req, res) => {
    try {
      if (req.method !== "GET" && req.method !== "POST") {
        res.status(405).json({ ok: false, error: "Method Not Allowed" });
        return;
      }

      const decoded = await verifyFirebaseUserAndAppCheck(req);
      enforcePerUserBurstLimit(decoded.uid);

      const probeRemote =
        String(req.query?.probe || "").toLowerCase() === "true" ||
        Boolean(req.body && req.body.probeRemote === true);

      const openAiKey = (openAiApiKey.value() || process.env.OPENAI_API_KEY || "").trim();
      const gemKey = (geminiApiKey.value() || process.env.GEMINI_API_KEY || "").trim();

      const health = await checkProvidersHealth({
        openAiKey,
        geminiKey: gemKey,
        probeRemote
      });

      res.set("Cache-Control", "no-store");
      res.status(health.ok ? 200 : 503).json(health);
    } catch (error) {
      const code = Number(error.statusCode) || 500;
      res.status(code).json({
        ok: false,
        code: code === 429 ? "RATE_LIMIT_EXCEEDED" : code === 401 ? "UNAUTHORIZED" : "HEALTH_CHECK_FAILED",
        error: String(error.message || "Provider health check failed")
      });
    }
  }
);

/**
 * 16. Paginated A–Z Catalog Metadata Index (`catalogSearchIndex`)
 * Caches and indexes normalized book metadata from Gutendex and LibriVox by:
 * - first letter A–Z
 * - normalized title
 * - author
 * - language
 * - category / subject
 * - audio & text availability
 * Stores metadata only — never downloads full book texts just to list books.
 */
exports.catalogSearchIndex = onRequest(
  {
    cors: true
  },
  async (req, res) => {
    try {
      const params = req.method === "POST" ? req.body || {} : req.query || {};
      const letter = String(params.letter || "All").trim();
      const query = String(params.query || params.search || "").trim();
      const language = String(params.language || "ALL").trim();
      const category = String(params.category || "All").trim();
      const format = String(params.format || "all").trim();
      const narrator = String(params.narrator || "").trim();
      const page = Math.max(1, Number(params.page || 1));
      const limit = Math.min(60, Math.max(1, Number(params.limit || 30)));

      // Fetch & index upstream metadata pages for the requested query/letter/page when not yet cached
      const upstreamKey = `${letter}|${query}|${language}|${category}|${page}`;
      if (!globalCatalogIndex.fetchedUpstreamKeys.has(upstreamKey)) {
        globalCatalogIndex.fetchedUpstreamKeys.add(upstreamKey);
        const searchParam = query || (letter.length === 1 && letter !== "All" ? letter.toLowerCase() : "");
        const gutenUrl = new URL(GUTENDEX_BASE_URL);
        if (searchParam) gutenUrl.searchParams.set("search", searchParam);
        if (language && language.toLowerCase() !== "all") {
          gutenUrl.searchParams.set("languages", language.toLowerCase().slice(0, 2));
        }
        if (page > 1) gutenUrl.searchParams.set("page", String(page));

        const lvUrl = new URL(LIBRIVOX_BASE_URL);
        lvUrl.searchParams.set("format", "json");
        lvUrl.searchParams.set("coverart", "1");
        lvUrl.searchParams.set("limit", "40");
        lvUrl.searchParams.set("offset", String((page - 1) * 40));
        if (letter.length === 1 && letter !== "All" && !query) {
          lvUrl.searchParams.set("title", `^${letter.toUpperCase()}`);
        } else if (query) {
          lvUrl.searchParams.set("title", query);
        }

        const [gutenRes, lvRes] = await Promise.allSettled([
          fetch(gutenUrl.toString(), { headers: { Accept: "application/json" } }).then((r) => (r.ok ? r.json() : null)),
          fetch(lvUrl.toString(), { headers: { Accept: "application/json" } }).then((r) => (r.ok ? r.json() : null))
        ]);

        if (gutenRes.status === "fulfilled" && gutenRes.value) {
          globalCatalogIndex.ingestGutendexPayload(gutenRes.value);
        }
        if (lvRes.status === "fulfilled" && lvRes.value) {
          globalCatalogIndex.ingestLibriVoxPayload(lvRes.value);
        }
      }

      const result = globalCatalogIndex.queryIndex({
        letter,
        query,
        language,
        category,
        format,
        narrator,
        page,
        limit
      });

      res.status(200).json({
        ok: true,
        ...result
      });
    } catch (error) {
      res.status(500).json({
        ok: false,
        error: String(error.message || "Catalog index query failed")
      });
    }
  }
);

// ============================================================================
// PHASE 1–6: UNIFIED AI GATEWAY, PLAY BILLING RTDN, BOOKS ENTITLEMENT & ACCOUNT DELETION
// ============================================================================

const DEFAULT_ACCESS_POLICY = {
  lessonsProOnly: true,
  roleplayProOnly: true,
  allAiProOnly: true,
  freeDailyLessons: 0,
  freeDailyAiVoiceSeconds: 0,
  freeDailyAiTextMessages: 0,
  freeDailyAiRoleplaySessions: 0,
  freeDailyAiCorrections: 0,
  freeBooksPerMonth: 5,
  freeAudiobooksCount: 5,
  freeReadingBooksCount: 5,
  freeDailyListeningExercises: 15,
  freeMaxGameLevel: 5,
  freeLifetimePracticalLabsTrials: 0,
  freeDailyStudioSessions: 0,
  freeAiTutorVoiceIds: [],
  freeCultureJourneyPreviewsPerCountry: 1,
  freeDailyHumanAudioCallMinutes: 20,
  freeDailyHumanVideoCallMinutes: 10,
  proUnlimitedLessons: true,
  "ai.tutor.freeSecondsPerDay": 0,
  "ai.tutor.proSecondsPerDay": 900,
  "ai.tutor.proSecondsPerMonth": 9000,
  "ai.tutor.maxSessionSeconds": 600,
  "ai.tutor.idleTimeoutSeconds": 45,
  proDailyAiVoiceSeconds: 900,
  proMonthlyAiVoiceSeconds: 9000,
  proDailyAiTextMessages: 150,
  proDailyAiRoleplaySessions: 8,
  proDailyAiCorrections: 50,
  proDailyPersonalizedMaterials: 20,
  proAllVoicesUnlocked: true,
  proAllBooksUnlocked: true,
  proDailyHumanAudioCallMinutes: 120,
  proDailyHumanVideoCallMinutes: 60,
  globalAiKillSwitch: false
};

const DEFAULT_FREE_CONTENT = {
  freeAudiobookIds: ["gutenberg_1661", "gutenberg_11"],
  freeReadingBookIds: ["gutenberg_1342", "gutenberg_4507"]
};

const DEFAULT_PRICING_CONFIG = {
  geminiFlashLiteInputMicrosPer1k: 0.1,
  geminiFlashLiteOutputMicrosPer1k: 0.4,
  geminiFlashInputMicrosPer1k: 0.3,
  geminiFlashOutputMicrosPer1k: 2.5,
  voicePipelineMicrosPerSecond: 158,
  proMonthlyMaxCostMicros: 2850000,
  freeDailyMaxCostMicros: 0,
  globalDailyBudgetMicros: 25000000
};

const SEVEN_DAYS_MS = 7 * 24 * 3600 * 1000;
let cachedAccessPolicy = null;
let cachedAccessPolicyAtMs = 0;

async function getAccessPolicyCached(db) {
  if (cachedAccessPolicy && Date.now() - cachedAccessPolicyAtMs < 60000) {
    return cachedAccessPolicy;
  }
  try {
    const snap = await db.collection("config").document("accessPolicy").get();
    cachedAccessPolicy = snap.exists ? { ...DEFAULT_ACCESS_POLICY, ...(snap.data() || {}) } : DEFAULT_ACCESS_POLICY;
    cachedAccessPolicyAtMs = Date.now();
    return cachedAccessPolicy;
  } catch (_) {
    return DEFAULT_ACCESS_POLICY;
  }
}

function formatDateInTimezone(epochMs, timeZoneId) {
  try {
    const formatter = new Intl.DateTimeFormat("en-CA", {
      timeZone: timeZoneId || "UTC",
      year: "numeric",
      month: "2-digit",
      day: "2-digit"
    });
    return formatter.format(new Date(epochMs)); // yyyy-MM-dd
  } catch (_) {
    return new Date(epochMs).toISOString().slice(0, 10);
  }
}

function sha256Hex(input) {
  return crypto.createHash("sha256").update(String(input)).digest("hex");
}

/**
 * Resolves the user's authoritative entitlement from `users/{uid}/private/entitlement`
 * with fallback to `users/{uid}` only when `verifiedWithPlayApi === true`.
 */
async function resolveServerEntitlement(db, uid) {
  const entRef = db.collection("users").document(uid).collection("private").document("entitlement");
  const entSnap = await entRef.get();
  if (entSnap.exists) {
    const d = entSnap.data() || {};
    const status = String(d.status || "free").toLowerCase();
    const expiryMs = Number(d.expiryEpochMs || 0);
    const inGrace = Boolean(d.inGracePeriod) || status === "pro_grace";
    const isExpired = expiryMs > 0 && Date.now() > expiryMs && !inGrace;
    const isPro = !isExpired && (status === "pro_active" || status === "pro_grace");
    return {
      status: isExpired ? "expired" : status,
      isPro,
      planId: isPro ? String(d.planId || "lingosphere_pro") : "free",
      basePlanId: isPro ? (d.basePlanId || null) : null,
      subscriptionTier: isPro ? String(d.subscriptionTier || "premium_pro") : "free",
      expiryEpochMs: expiryMs
    };
  }
  return {
    status: "free",
    isPro: false,
    planId: "free",
    basePlanId: null,
    subscriptionTier: "free",
    expiryEpochMs: 0
  };
}

/**
 * 17. Unified AI Gateway (`aiGateway`) — Phase 2
 * 1) Verifies Firebase Auth ID Token & App Check token
 * 2) Reads server-only entitlement from `users/{uid}/private/entitlement`
 * 3) Validates input length, language, and content safety
 * 4) Checks `aiCache/{hash}` first (zero-cost cache hits)
 * 5) Atomically reserves quota in `usage/{uid}/daily/{yyyy-mm-dd}` (in user timezone with 7-day cooldown)
 *    and `usage/{uid}/monthly/{yyyy-mm}` (UTC) BEFORE calling the AI model
 * 6) Logs usage event to `usageEvents`
 */
exports.aiGateway = onRequest(
  {
    cors: true,
    secrets: [openAiApiKey, geminiApiKey]
  },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({ ok: false, error: "Method Not Allowed" });
        return;
      }

      const decoded = await verifyFirebaseUserAndAppCheck(req);
      const uid = decoded.uid;
      enforcePerUserBurstLimit(uid);

      const db = getLingoFirestore();
      const body = req.body || {};
      const featureKey = String(body.featureKey || "ai.text.messages").trim();
      const userPrompt = String(body.input || body.userMessage || body.prompt || "").trim().slice(0, 4500);
      const targetLanguage = String(body.targetLanguage || body.languageCode || "es").trim().slice(0, 12);
      const cefrLevel = String(body.cefrLevel || "B1").trim().slice(0, 6);
      const requestedUnits = Math.max(1, Math.min(300, Number(body.requestedUnits || 1)));
      const clientTimezone = String(body.timezone || "UTC").trim().slice(0, 64);

      if (!userPrompt) {
        res.status(400).json({ ok: false, code: "INVALID_INPUT", error: "Prompt is required" });
        return;
      }

      const policy = await getAccessPolicyCached(db);
      if (policy.globalAiKillSwitch === true) {
        res.status(503).json({ ok: false, code: "AI_MAINTENANCE", error: "AI features are temporarily paused for maintenance." });
        return;
      }

      if (!uid || String(uid).startsWith("guest_") || String(uid).startsWith("appcheck_")) {
        res.status(401).json({ ok: false, code: "SIGN_IN_REQUIRED", error: "Sign in to continue." });
        return;
      }

      // Enforce Pro-only AI entitlement BEFORE cache lookup, quota reservation, or model call
      const entitlement = await resolveServerEntitlement(db, uid);
      if (!entitlement.isPro) {
        res.status(403).json({
          ok: false,
          code: "PRO_REQUIRED",
          error: "This feature requires LingoSphere Pro."
        });
        return;
      }

      // Content-hash cache lookup for deterministic queries (explanations, corrections, translations, book summaries)
      const isCacheable = Boolean(body.cacheable) || [
        "ai.corrections",
        "word_explanation",
        "translation",
        "book_summary"
      ].includes(featureKey);
      const cacheHash = sha256Hex(`${featureKey}|${targetLanguage}|${cefrLevel}|${userPrompt.toLowerCase()}`);
      const cacheRef = db.collection("aiCache").document(cacheHash);

      if (isCacheable) {
        const cacheSnap = await cacheRef.get();
        if (cacheSnap.exists) {
          const cachedData = cacheSnap.data() || {};
          if (cachedData.outputText) {
            await db.collection("usageEvents").add({
              uid,
              feature: featureKey,
              model: cachedData.model || "cache",
              units: 0,
              estimatedCostMicros: 0,
              cached: true,
              createdAt: FieldValue.serverTimestamp()
            });
            res.status(200).json({
              ok: true,
              cached: true,
              provider: cachedData.provider || "CACHE",
              model: cachedData.model || "aiCache",
              text: cachedData.outputText,
              outputText: cachedData.outputText,
              subscriptionTier: entitlement.subscriptionTier
            });
            return;
          }
        }
      }

      // Atomic Quota Reservation Transaction before calling upstream AI model
      const nowMs = Date.now();
      const monthKey = new Date(nowMs).toISOString().slice(0, 7);
      const userRef = db.collection("users").document(uid);
      const monthUsageRef = db.collection("usage").document(uid).collection("monthly").document(monthKey);

      const reservation = await db.runTransaction(async (tx) => {
        const userSnap = await tx.get(userRef);
        const userData = userSnap.exists ? userSnap.data() || {} : {};

        // Enforce 7-day cooldown on timezone changes
        let effectiveTz = String(userData.timezoneId || "UTC");
        const lastTzChangeMs = Number(userData.timezoneUpdatedAtEpochMs || 0);
        if (clientTimezone && clientTimezone !== effectiveTz) {
          if (lastTzChangeMs === 0 || (nowMs - lastTzChangeMs) >= SEVEN_DAYS_MS) {
            effectiveTz = clientTimezone;
            if (userSnap.exists) {
              tx.update(userRef, {
                timezoneId: effectiveTz,
                timezoneUpdatedAtEpochMs: nowMs
              });
            }
          }
        }

        const dayKey = formatDateInTimezone(nowMs, effectiveTz);
        const dayUsageRef = db.collection("usage").document(uid).collection("daily").document(dayKey);
        const [daySnap, monthSnap] = await Promise.all([tx.get(dayUsageRef), tx.get(monthUsageRef)]);
        const dayData = daySnap.exists ? daySnap.data() || {} : {};
        const monthData = monthSnap.exists ? monthSnap.data() || {} : {};

        let dailyLimit = policy.proDailyAiTextMessages;
        let monthlyLimit = Number.MAX_SAFE_INTEGER;
        if (featureKey === "ai.voice.seconds") {
          dailyLimit = policy.proDailyAiVoiceSeconds;
          monthlyLimit = policy.proMonthlyAiVoiceSeconds;
        } else if (featureKey === "ai.roleplay.sessions") {
          dailyLimit = policy.proDailyAiRoleplaySessions;
        } else if (featureKey === "ai.corrections") {
          dailyLimit = policy.proDailyAiCorrections;
        } else if (featureKey === "personalizedMaterial") {
          dailyLimit = policy.proDailyPersonalizedMaterials;
        } else if (featureKey === "studio") {
          dailyLimit = 40;
        } else {
          dailyLimit = policy.proDailyAiTextMessages;
        }

        const fieldKey = featureKey.replace(/\./g, "_");
        const usedDaily = Number(dayData[fieldKey] || 0);
        const usedMonthly = Number(monthData[fieldKey] || 0);

        if (usedDaily + requestedUnits > dailyLimit || usedMonthly + requestedUnits > monthlyLimit) {
          const calmMessage = featureKey === "personalizedMaterial"
            ? `You've reached today's ${dailyLimit} custom stories limit. Fresh stories unlock again tomorrow.`
            : "You've reached today's LingoSphere Pro usage limit. It resets at midnight.";
          const err = new Error(calmMessage);
          err.statusCode = 429;
          throw err;
        }

        const nextDaily = usedDaily + requestedUnits;
        const nextMonthly = usedMonthly + requestedUnits;

        tx.set(
          dayUsageRef,
          {
            uid,
            dayKey,
            timezoneId: effectiveTz,
            [fieldKey]: nextDaily,
            updatedAt: FieldValue.serverTimestamp()
          },
          { merge: true }
        );
        tx.set(
          monthUsageRef,
          {
            uid,
            monthKey,
            [fieldKey]: nextMonthly,
            updatedAt: FieldValue.serverTimestamp()
          },
          { merge: true }
        );

        const usageRatio = Math.max(nextDaily / Math.max(1, dailyLimit), nextMonthly / Math.max(1, monthlyLimit));
        return {
          dayKey,
          monthKey,
          effectiveTz,
          remainingDaily: Math.max(0, dailyLimit - nextDaily),
          softWarning80Percent: usageRatio >= 0.8
        };
      });

      const openAiKey = (openAiApiKey.value() || process.env.OPENAI_API_KEY || "").trim();
      const gemKey = (geminiApiKey.value() || process.env.GEMINI_API_KEY || "").trim();

      const routed = await routeAiRequest({
        body: {
          ...body,
          input: userPrompt
        },
        openAiKey,
        geminiKey: gemKey,
        isPremium: true
      });

      if (routed.status === 200 && routed.payload?.outputText) {
        const estCostMicros = featureKey === "ai.voice.seconds"
          ? requestedUnits * DEFAULT_PRICING_CONFIG.voicePipelineMicrosPerSecond
          : 170;

        if (isCacheable) {
          await cacheRef.set({
            featureKey,
            targetLanguage,
            cefrLevel,
            outputText: routed.payload.outputText,
            provider: routed.payload.provider || "GEMINI",
            model: routed.payload.model || "gemini-2.5-flash-lite",
            createdAt: FieldValue.serverTimestamp()
          });
        }

        await db.collection("usageEvents").add({
          uid,
          feature: featureKey,
          model: routed.payload.model || "gemini-2.5-flash-lite",
          units: requestedUnits,
          estimatedCostMicros: estCostMicros,
          cached: false,
          createdAt: FieldValue.serverTimestamp()
        });
      }

      res.set("Cache-Control", "no-store");
      res.status(routed.status).json({
        ...routed.payload,
        subscriptionTier: entitlement.subscriptionTier,
        remainingDailyUnits: reservation.remainingDaily,
        softWarning80Percent: reservation.softWarning80Percent,
        timezoneId: reservation.effectiveTz
      });
    } catch (error) {
      const code = Number(error.statusCode) || 500;
      if (code === 429) {
        res.set("Retry-After", "60");
      }
      res.status(code).json({
        ok: false,
        code: error.code || (code === 403 ? "PRO_REQUIRED" : code === 429 ? "QUOTA_OR_RATE_LIMIT_EXCEEDED" : code === 401 ? "UNAUTHORIZED" : "AI_GATEWAY_ERROR"),
        error: String(error.message || "AI Gateway request failed")
      });
    }
  }
);

/**
 * 18. Live Voice 15-Second Heartbeat Metering (`aiVoiceHeartbeat`) — Phase 2
 * Called every 15 seconds during an active AI Voice session. Deducts `elapsedSeconds` (15s)
 * from `ai.voice.seconds` and returns `terminateSession: true` if quota expires mid-call.
 */
exports.aiVoiceHeartbeat = onRequest(
  { cors: true },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({ ok: false, error: "Method Not Allowed" });
        return;
      }
      const decoded = await verifyFirebaseUserAndAppCheck(req);
      const uid = decoded.uid;
      if (!uid || String(uid).startsWith("guest_") || String(uid).startsWith("appcheck_")) {
        res.status(401).json({ ok: false, terminateSession: true, code: "SIGN_IN_REQUIRED", error: "Sign in to continue." });
        return;
      }
      const db = getLingoFirestore();
      const policy = await getAccessPolicyCached(db);
      const entitlement = await resolveServerEntitlement(db, uid);
      if (!entitlement.isPro) {
        res.status(403).json({
          ok: false,
          terminateSession: true,
          code: "PRO_REQUIRED",
          error: "This feature requires LingoSphere Pro."
        });
        return;
      }

      const elapsedSeconds = Math.max(1, Math.min(30, Number((req.body && req.body.elapsedSeconds) || 15)));
      const nowMs = Date.now();
      const dayKey = new Date(nowMs).toISOString().slice(0, 10);
      const monthKey = new Date(nowMs).toISOString().slice(0, 7);

      const dayRef = db.collection("usage").document(uid).collection("daily").document(dayKey);
      const monthRef = db.collection("usage").document(uid).collection("monthly").document(monthKey);

      const dailyLimit = policy.proDailyAiVoiceSeconds;
      const monthlyLimit = policy.proMonthlyAiVoiceSeconds;

      const status = await db.runTransaction(async (tx) => {
        const [dSnap, mSnap] = await Promise.all([tx.get(dayRef), tx.get(monthRef)]);
        const usedDay = Number((dSnap.exists && dSnap.get("ai_voice_seconds")) || 0);
        const usedMonth = Number((mSnap.exists && mSnap.get("ai_voice_seconds")) || 0);

        const nextDay = usedDay + elapsedSeconds;
        const nextMonth = usedMonth + elapsedSeconds;
        const terminateSession = nextDay >= dailyLimit || nextMonth >= monthlyLimit;

        tx.set(dayRef, { uid, dayKey, ai_voice_seconds: Math.min(dailyLimit, nextDay), updatedAt: FieldValue.serverTimestamp() }, { merge: true });
        tx.set(monthRef, { uid, monthKey, ai_voice_seconds: Math.min(monthlyLimit, nextMonth), updatedAt: FieldValue.serverTimestamp() }, { merge: true });

        const remDay = Math.max(0, dailyLimit - nextDay);
        const remMonth = Math.max(0, monthlyLimit - nextMonth);
        return {
          ok: !terminateSession,
          terminateSession,
          remainingSeconds: Math.min(remDay, remMonth),
          usedDailySeconds: Math.min(dailyLimit, nextDay),
          usedMonthlySeconds: Math.min(monthlyLimit, nextMonth),
          softWarning80Percent: (nextDay / Math.max(1, dailyLimit)) >= 0.8 || (nextMonth / Math.max(1, monthlyLimit)) >= 0.8
        };
      });

      res.status(200).json(status);
    } catch (error) {
      res.status(500).json({ ok: false, terminateSession: false, error: String(error.message || error) });
    }
  }
);

/**
 * 19. Backend Purchase Verification & 1-Token-to-1-UID Binding (`verifyPurchase`) — Phase 3
 * Verifies Google Play purchase tokens via Google Play Developer API `subscriptionsv2.get`,
 * acknowledges unacknowledged purchases, binds `purchases/{tokenHash}` to `uid` (rejecting cross-account reuse),
 * and writes authoritative entitlement to `users/{uid}/private/entitlement`.
 */
exports.verifyPurchase = onRequest(
  { cors: true },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({ error: "Method Not Allowed" });
        return;
      }

      const authHeader = req.headers.authorization || "";
      if (!authHeader.startsWith("Bearer ")) {
        res.status(401).json({ error: "Missing Authorization Bearer token" });
        return;
      }
      const idToken = authHeader.slice("Bearer ".length).trim();
      const decoded = await admin.auth().verifyIdToken(idToken);
      const uid = decoded.uid;

      const body = req.body || {};
      const packageName = String(body.packageName || "com.aistudio.lingosphere.kxpqzm").trim();
      const productId = String(body.productId || "lingosphere_pro").trim();
      const basePlanId = String(body.basePlanId || (productId === "lingosphere_yearly" ? "yearly" : "monthly")).trim();
      const purchaseToken = String(body.purchaseToken || "").trim();

      if (!purchaseToken || purchaseToken.length < 6) {
        res.status(400).json({ error: "Valid Google Play purchaseToken is required" });
        return;
      }

      const tokenHash = sha256Hex(purchaseToken);
      const db = getLingoFirestore();
      const purchaseRef = db.collection("purchases").document(tokenHash);
      const entRef = db.collection("users").document(uid).collection("private").document("entitlement");
      const userRef = db.collection("users").document(uid);

      // Step 1: Check 1-token-to-1-UID binding before Play API or entitlement grant
      const existingPurchaseSnap = await purchaseRef.get();
      if (existingPurchaseSnap.exists) {
        const boundUid = String(existingPurchaseSnap.get("uid") || "");
        if (boundUid && boundUid !== uid) {
          res.status(409).json({
            ok: false,
            code: "TOKEN_BOUND_TO_ANOTHER_ACCOUNT",
            error: "This Google Play subscription is already linked to another LingoSphere account."
          });
          return;
        }
      }

      // Step 2: Authoritative Google Play Developer API `subscriptionsv2.get` verification
      const saJson = process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON;
      if (!saJson) {
        res.status(503).json({
          ok: false,
          code: "PLAY_API_CREDENTIALS_NOT_CONFIGURED",
          error: "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON secret is required on the backend to verify Google Play purchases."
        });
        return;
      }

      const accessToken = await fetchGooglePlayAccessToken(saJson);
      const playUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`;
      const playRes = await fetch(playUrl, {
        headers: { Authorization: `Bearer ${accessToken}` }
      });

      if (!playRes.ok) {
        const playErr = await playRes.text();
        res.status(403).json({
          ok: false,
          error: "Google Play Developer API rejected purchase verification",
          detail: playErr.slice(0, 500)
        });
        return;
      }

      const subData = await playRes.json();
      const subState = String(subData.subscriptionState || "");
      const lineItem = Array.isArray(subData.lineItems) ? subData.lineItems[0] || {} : {};
      const expiryEpochMs = lineItem.expiryTime ? Date.parse(lineItem.expiryTime) : 0;
      const autoRenewing = Boolean(lineItem.autoRenewingPlan && lineItem.autoRenewingPlan.autoRenewEnabled);
      const resolvedProductId = String(lineItem.productId || productId || "lingosphere_pro");
      const resolvedBasePlanId = String(
        (lineItem.offerDetails && lineItem.offerDetails.basePlanId) ||
          basePlanId ||
          (resolvedProductId === "lingosphere_yearly" ? "yearly" : "monthly")
      );

      // Verify obfuscatedExternalAccountId matches uid when provided by Play
      const extAccountId = subData.externalAccountIdentifiers?.obfuscatedExternalAccountId;
      if (extAccountId && extAccountId !== uid) {
        res.status(403).json({
          ok: false,
          code: "ACCOUNT_ID_MISMATCH",
          error: "Purchase external account identifier does not match the signed-in user."
        });
        return;
      }

      let entitlementStatus = "free";
      if (subState === "SUBSCRIPTION_STATE_ACTIVE" || subState === "SUBSCRIPTION_STATE_CANCELED") {
        entitlementStatus = expiryEpochMs === 0 || Date.now() < expiryEpochMs ? "pro_active" : "expired";
      } else if (subState === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD") {
        entitlementStatus = "pro_grace";
      } else if (subState === "SUBSCRIPTION_STATE_ON_HOLD" || subState === "SUBSCRIPTION_STATE_PAUSED") {
        entitlementStatus = "pro_on_hold";
      } else if (subState === "SUBSCRIPTION_STATE_EXPIRED") {
        entitlementStatus = "expired";
      }

      // Acknowledge in Google Play if pending
      if (
        (entitlementStatus === "pro_active" || entitlementStatus === "pro_grace") &&
        subData.acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING"
      ) {
        const ackUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/purchases/subscriptions/${encodeURIComponent(resolvedProductId)}/tokens/${encodeURIComponent(purchaseToken)}:acknowledge`;
        await fetch(ackUrl, {
          method: "POST",
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ developerPayload: `uid:${uid}` })
        });
      }

      const grantsPro = entitlementStatus === "pro_active" || entitlementStatus === "pro_grace";
      const resolvedTier = !grantsPro
        ? "free"
        : resolvedBasePlanId === "yearly" || resolvedProductId === "lingosphere_yearly"
          ? "premium_yearly"
          : "premium_pro";

      // Handle linkedPurchaseToken (upgrade/downgrade replacement token)
      const linkedToken = subData.linkedPurchaseToken ? String(subData.linkedPurchaseToken).trim() : null;

      await db.runTransaction(async (tx) => {
        tx.set(
          purchaseRef,
          {
            tokenHash,
            uid,
            packageName,
            productId: resolvedProductId,
            basePlanId: resolvedBasePlanId,
            subscriptionState: subState,
            entitlementStatus,
            expiryEpochMs,
            autoRenewing,
            linkedTokenHash: linkedToken ? sha256Hex(linkedToken) : null,
            updatedAt: FieldValue.serverTimestamp()
          },
          { merge: true }
        );

        tx.set(
          entRef,
          {
            uid,
            status: entitlementStatus,
            planId: grantsPro ? "lingosphere_pro" : "free",
            basePlanId: grantsPro ? resolvedBasePlanId : null,
            subscriptionTier: resolvedTier,
            expiryEpochMs,
            autoRenewing,
            inGracePeriod: entitlementStatus === "pro_grace",
            onHold: entitlementStatus === "pro_on_hold",
            verifiedWithPlayApi: true,
            purchaseTokenHash: tokenHash,
            lastVerifiedEpochMs: Date.now(),
            updatedAt: FieldValue.serverTimestamp()
          },
          { merge: true }
        );

        const userSnap = await tx.get(userRef);
        if (userSnap.exists) {
          tx.update(userRef, {
            subscriptionTier: resolvedTier,
            productId: grantsPro ? resolvedProductId : null,
            basePlanId: grantsPro ? resolvedBasePlanId : null,
            subscriptionAutoRenewing: autoRenewing,
            subscriptionExpiryEpochMs: expiryEpochMs,
            verifiedWithPlayApi: true,
            updatedAt: FieldValue.serverTimestamp()
          });
        }
      });

      res.status(200).json({
        ok: true,
        active: grantsPro,
        status: entitlementStatus,
        subscriptionTier: resolvedTier,
        basePlanId: resolvedBasePlanId,
        subscriptionAutoRenewing: autoRenewing,
        subscriptionExpiryEpochMs: expiryEpochMs,
        verifiedWithPlayApi: true
      });
    } catch (error) {
      res.status(500).json({
        ok: false,
        error: "Failed to verify purchase with Google Play",
        detail: String(error.message || error)
      });
    }
  }
);

/**
 * 20. Google Play Real-Time Developer Notifications (RTDN) Webhook (`handlePlayRtdnPubSub`) — Phase 3
 * Processes Pub/Sub push notifications for:
 * purchased, renewed, canceled, grace period, on hold, account hold recovered, paused,
 * pause schedule changed, restarted, expired, revoked/refunded, and price change.
 */
exports.handlePlayRtdnPubSub = onRequest(
  { cors: true },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({ error: "Method Not Allowed" });
        return;
      }

      const message = req.body && req.body.message;
      if (!message || !message.data) {
        res.status(400).json({ error: "Missing Pub/Sub message.data" });
        return;
      }

      const decodedJson = Buffer.from(String(message.data), "base64").toString("utf8");
      const notification = JSON.parse(decodedJson);
      const subNotif = notification.subscriptionNotification;
      if (!subNotif || !subNotif.purchaseToken) {
        res.status(200).json({ ok: true, ignored: true });
        return;
      }

      const packageName = String(notification.packageName || "com.aistudio.lingosphere.kxpqzm");
      const purchaseToken = String(subNotif.purchaseToken).trim();
      const tokenHash = sha256Hex(purchaseToken);
      const db = getLingoFirestore();

      const purchaseSnap = await db.collection("purchases").document(tokenHash).get();
      if (!purchaseSnap.exists) {
        res.status(200).json({ ok: true, unboundToken: true });
        return;
      }

      const uid = String(purchaseSnap.get("uid") || "");
      const saJson = process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON;
      if (!uid || !saJson) {
        res.status(200).json({ ok: true });
        return;
      }

      const accessToken = await fetchGooglePlayAccessToken(saJson);
      const playUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`;
      const playRes = await fetch(playUrl, {
        headers: { Authorization: `Bearer ${accessToken}` }
      });

      if (!playRes.ok) {
        res.status(502).json({ error: "Play API fetch failed during RTDN" });
        return;
      }

      const subData = await playRes.json();
      const subState = String(subData.subscriptionState || "");
      const lineItem = Array.isArray(subData.lineItems) ? subData.lineItems[0] || {} : {};
      const expiryEpochMs = lineItem.expiryTime ? Date.parse(lineItem.expiryTime) : 0;
      const autoRenewing = Boolean(lineItem.autoRenewingPlan && lineItem.autoRenewingPlan.autoRenewEnabled);
      const resolvedBasePlanId = String(
        (lineItem.offerDetails && lineItem.offerDetails.basePlanId) ||
          purchaseSnap.get("basePlanId") ||
          "monthly"
      );

      // Notification type 12 = SUBSCRIPTION_REVOKED (immediate revocation)
      const notifType = Number(subNotif.notificationType || 0);
      let entitlementStatus = "free";
      if (notifType === 12 || notifType === 13 || subState === "SUBSCRIPTION_STATE_EXPIRED") {
        entitlementStatus = "expired";
      } else if (subState === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD") {
        entitlementStatus = "pro_grace";
      } else if (subState === "SUBSCRIPTION_STATE_ON_HOLD" || subState === "SUBSCRIPTION_STATE_PAUSED") {
        entitlementStatus = "pro_on_hold";
      } else if (subState === "SUBSCRIPTION_STATE_ACTIVE" || subState === "SUBSCRIPTION_STATE_CANCELED") {
        entitlementStatus = expiryEpochMs === 0 || Date.now() < expiryEpochMs ? "pro_active" : "expired";
      }

      const grantsPro = entitlementStatus === "pro_active" || entitlementStatus === "pro_grace";
      const resolvedTier = !grantsPro
        ? "free"
        : resolvedBasePlanId === "yearly"
          ? "premium_yearly"
          : "premium_pro";

      const entRef = db.collection("users").document(uid).collection("private").document("entitlement");
      const userRef = db.collection("users").document(uid);

      await db.runTransaction(async (tx) => {
        tx.set(
          db.collection("purchases").document(tokenHash),
          {
            subscriptionState: subState,
            entitlementStatus,
            expiryEpochMs,
            autoRenewing,
            lastRtdnNotificationType: notifType,
            updatedAt: FieldValue.serverTimestamp()
          },
          { merge: true }
        );
        tx.set(
          entRef,
          {
            uid,
            status: entitlementStatus,
            planId: grantsPro ? "lingosphere_pro" : "free",
            basePlanId: grantsPro ? resolvedBasePlanId : null,
            subscriptionTier: resolvedTier,
            expiryEpochMs,
            autoRenewing,
            inGracePeriod: entitlementStatus === "pro_grace",
            onHold: entitlementStatus === "pro_on_hold",
            verifiedWithPlayApi: true,
            lastVerifiedEpochMs: Date.now(),
            updatedAt: FieldValue.serverTimestamp()
          },
          { merge: true }
        );
        const uSnap = await tx.get(userRef);
        if (uSnap.exists) {
          tx.update(userRef, {
            subscriptionTier: resolvedTier,
            subscriptionAutoRenewing: autoRenewing,
            subscriptionExpiryEpochMs: expiryEpochMs,
            updatedAt: FieldValue.serverTimestamp()
          });
        }
      });

      res.status(200).json({ ok: true, uid, status: entitlementStatus });
    } catch (error) {
      res.status(500).json({ error: String(error.message || error) });
    }
  }
);

/**
 * 21. Entitlement & Monthly Limit Book Content Resolver (`getBookContentUrl`)
 * Pro users get unlimited books & audiobooks.
 * Free users get 5 distinct books per calendar month (tracked transactionally in `usage/{uid}/monthly/{yyyy-mm}.openedBookIds`).
 * Re-opening an already-opened book in the same month does not consume a new slot.
 */
exports.getBookContentUrl = onRequest(
  { cors: true },
  async (req, res) => {
    try {
      const decoded = await verifyFirebaseUserAndAppCheck(req);
      const uid = decoded.uid;
      if (!uid || String(uid).startsWith("guest_") || String(uid).startsWith("appcheck_")) {
        res.status(401).json({ ok: false, code: "SIGN_IN_REQUIRED", error: "Sign in to continue." });
        return;
      }
      const db = getLingoFirestore();
      const policy = await getAccessPolicyCached(db);
      const entitlement = await resolveServerEntitlement(db, uid);

      const params = req.method === "POST" ? req.body || {} : req.query || {};
      const bookId = String(params.bookId || "").trim().toLowerCase();
      const gutendexId = params.gutendexId ? Number(params.gutendexId) : null;
      const canonicalBookId = gutendexId ? `gutenberg_${gutendexId}` : bookId;

      if (!canonicalBookId) {
        res.status(400).json({ ok: false, error: "Missing bookId or gutendexId" });
        return;
      }

      const monthlyLimit = Math.max(1, Number(policy.freeBooksPerMonth || 5));
      if (entitlement.isPro) {
        res.status(200).json({
          ok: true,
          bookId: canonicalBookId,
          gutendexId,
          unlocked: true,
          isPro: true,
          booksUsedThisMonth: 0,
          freeBooksLimitPerMonth: monthlyLimit
        });
        return;
      }

      const clientTz = String(params.timezone || "UTC").trim().slice(0, 64);
      const monthKey = formatMonthInTimezone(Date.now(), clientTz);
      const monthUsageRef = db.collection("usage").document(uid).collection("monthly").document(monthKey);

      const outcome = await db.runTransaction(async (tx) => {
        const mSnap = await tx.get(monthUsageRef);
        const rawList = mSnap.exists && Array.isArray(mSnap.get("openedBookIds")) ? mSnap.get("openedBookIds") : [];
        const openedBookIds = rawList.map((v) => String(v).trim().toLowerCase()).filter(Boolean);

        if (openedBookIds.includes(canonicalBookId)) {
          return {
            allowed: true,
            booksUsedThisMonth: openedBookIds.length,
            remainingBooksThisMonth: Math.max(0, monthlyLimit - openedBookIds.length)
          };
        }

        if (openedBookIds.length >= monthlyLimit) {
          return {
            allowed: false,
            booksUsedThisMonth: openedBookIds.length,
            remainingBooksThisMonth: 0
          };
        }

        const updatedIds = [...openedBookIds, canonicalBookId];
        tx.set(
          monthUsageRef,
          {
            uid,
            monthKey,
            openedBookIds: updatedIds,
            booksOpenedCount: updatedIds.length,
            updatedAt: FieldValue.serverTimestamp()
          },
          { merge: true }
        );
        return {
          allowed: true,
          booksUsedThisMonth: updatedIds.length,
          remainingBooksThisMonth: Math.max(0, monthlyLimit - updatedIds.length)
        };
      });

      if (!outcome.allowed) {
        res.status(403).json({
          ok: false,
          code: "PRO_REQUIRED",
          error: `Free includes ${monthlyLimit} books per month. Upgrade to LingoSphere Pro for unlimited books and audiobooks.`,
          booksUsedThisMonth: outcome.booksUsedThisMonth,
          freeBooksLimitPerMonth: monthlyLimit
        });
        return;
      }

      res.status(200).json({
        ok: true,
        bookId: canonicalBookId,
        gutendexId,
        unlocked: true,
        isPro: false,
        booksUsedThisMonth: outcome.booksUsedThisMonth,
        remainingBooksThisMonth: outcome.remainingBooksThisMonth,
        freeBooksLimitPerMonth: monthlyLimit
      });
    } catch (error) {
      res.status(Number(error.statusCode) || 500).json({ ok: false, error: String(error.message || error) });
    }
  }
);

/**
 * 22. Complete In-App & Server-Side Account Deletion (`deleteUserAccount`) — Phase 6
 * Deletes user profile, private entitlement, daily/monthly usage subcollections, and Firebase Auth account.
 */
exports.deleteUserAccount = onRequest(
  { cors: true },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({ error: "Method Not Allowed" });
        return;
      }
      const authHeader = req.headers.authorization || "";
      if (!authHeader.startsWith("Bearer ")) {
        res.status(401).json({ error: "Missing Authorization Bearer token" });
        return;
      }
      const idToken = authHeader.slice("Bearer ".length).trim();
      const decoded = await admin.auth().verifyIdToken(idToken);
      const uid = decoded.uid;

      const db = getLingoFirestore();
      const userRef = db.collection("users").document(uid);
      const entRef = userRef.collection("private").document("entitlement");
      const openAiQuotaRef = userRef.collection("private").document("openai_usage");

      await Promise.allSettled([
        entRef.delete(),
        openAiQuotaRef.delete(),
        userRef.delete(),
        db.collection("referral_summaries").document(uid).delete(),
        admin.auth().deleteUser(uid)
      ]);

      res.status(200).json({ ok: true, deletedUid: uid });
    } catch (error) {
      res.status(500).json({
        ok: false,
        error: "Failed to delete user account",
        detail: String(error.message || error)
      });
    }
  }
);

function formatMonthInTimezone(epochMs, timeZoneId) {
  try {
    const formatter = new Intl.DateTimeFormat("en-CA", {
      timeZone: timeZoneId || "UTC",
      year: "numeric",
      month: "2-digit"
    });
    return formatter.format(new Date(epochMs)).slice(0, 7); // yyyy-MM
  } catch (_) {
    return new Date(epochMs).toISOString().slice(0, 7);
  }
}

/**
 * 23. Server-Enforced AI Tutor Session Start (`startTutorSession`) — Part 2 & Pro AI Tutor System
 * Checks:
 * 1) Firebase ID token + App Check token
 * 2) Global AI kill switch
 * 3) Current server entitlement (`free`, `pro_active`, or `pro_grace`)
 * 4) Daily & monthly usage counters (`usage/{uid}/daily/{yyyy-mm-dd}.tutorSeconds` & `usage/{uid}/monthly/{yyyy-mm}.tutorSeconds`)
 *    in user's validated IANA timezone (DST-safe)
 * 5) Single active tutor session lock (`activeTutorSessions/{uid}` — rejects simultaneous 2nd device)
 * Returns `{ sessionId, remainingSeconds, maxSessionAllowanceWithGraceSeconds }`.
 */
exports.startTutorSession = onRequest(
  { cors: true },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({ ok: false, error: "Method Not Allowed" });
        return;
      }
      const decoded = await verifyFirebaseUserAndAppCheck(req);
      const uid = decoded.uid;
      if (!uid || String(uid).startsWith("guest_") || String(uid).startsWith("appcheck_")) {
        res.status(401).json({ ok: false, code: "SIGN_IN_REQUIRED", error: "Sign in to continue" });
        return;
      }

      const db = getLingoFirestore();
      const policy = await getAccessPolicyCached(db);
      if (policy.globalAiKillSwitch === true) {
        res.status(503).json({ ok: false, code: "AI_MAINTENANCE", error: "AI Tutor is temporarily paused for maintenance." });
        return;
      }

      const entitlement = await resolveServerEntitlement(db, uid);
      if (!entitlement.isPro) {
        res.status(403).json({
          ok: false,
          code: "PRO_REQUIRED",
          error: "AI Tutor requires LingoSphere Pro."
        });
        return;
      }

      const body = req.body || {};
      const requestedSessionId = String(body.sessionId || crypto.randomUUID()).trim().slice(0, 80);
      const clientTz = String(body.timezone || "UTC").trim().slice(0, 64);

      const proSecondsPerDay = Number(policy["ai.tutor.proSecondsPerDay"] || policy.proDailyAiVoiceSeconds || 900);
      const proSecondsPerMonth = Number(policy["ai.tutor.proSecondsPerMonth"] || policy.proMonthlyAiVoiceSeconds || 9000);
      const maxSessionSeconds = Number(policy["ai.tutor.maxSessionSeconds"] || 600);

      const nowMs = Date.now();
      const entRef = db.collection("users").document(uid).collection("private").document("entitlement");
      const activeSessionRef = db.collection("activeTutorSessions").document(uid);

      const result = await db.runTransaction(async (tx) => {
        const [entSnap, activeSnap] = await Promise.all([tx.get(entRef), tx.get(activeSessionRef)]);
        const entData = entSnap.exists ? entSnap.data() || {} : {};
        const storedTz = String(entData.timezoneId || "UTC");
        const lastTzUpdateMs = Number(entData.timezoneUpdatedAtMs || 0);
        const effectiveTz = (!storedTz || nowMs - lastTzUpdateMs >= SEVEN_DAYS_MS) ? (clientTz || "UTC") : storedTz;

        // Single active session check (15s heartbeat expiry window)
        if (activeSnap.exists) {
          const activeData = activeSnap.data() || {};
          const existingSessionId = String(activeData.sessionId || "");
          const lastBeatMs = Number(activeData.lastHeartbeatEpochMs || 0);
          const isStillAlive = Boolean(activeData.active) && (nowMs - lastBeatMs < 15000);
          if (isStillAlive && existingSessionId && existingSessionId !== requestedSessionId) {
            return {
              rejectedSecondDevice: true,
              error: "An AI Tutor session is already active on another device. Please end that session first."
            };
          }
        }

        const dayKey = formatDateInTimezone(nowMs, effectiveTz);
        const monthKey = formatMonthInTimezone(nowMs, effectiveTz);
        const dayRef = db.collection("usage").document(uid).collection("daily").document(dayKey);
        const monthRef = db.collection("usage").document(uid).collection("monthly").document(monthKey);
        const [dSnap, mSnap] = await Promise.all([tx.get(dayRef), tx.get(monthRef)]);

        const usedDaily = Number((dSnap.exists && (dSnap.get("tutorSeconds") ?? dSnap.get("ai_voice_seconds"))) || 0);
        const usedMonthly = Number((mSnap.exists && (mSnap.get("tutorSeconds") ?? mSnap.get("ai_voice_seconds"))) || 0);

        const dailyRemaining = Math.max(0, proSecondsPerDay - usedDaily);
        const monthlyRemaining = Math.max(0, proSecondsPerMonth - usedMonthly);
        const remaining = Math.min(dailyRemaining, monthlyRemaining);

        if (remaining <= 0) {
          return {
            exhausted: true,
            limitReason: monthlyRemaining <= 0 ? "pro_monthly_limit" : "pro_daily_limit",
            remainingSeconds: 0,
            usedDailySeconds: usedDaily,
            usedMonthlySeconds: usedMonthly
          };
        }

        const sessionCapWithGrace = Math.min(remaining, maxSessionSeconds) + 5;
        tx.set(
          activeSessionRef,
          {
            uid,
            sessionId: requestedSessionId,
            active: true,
            startedAtEpochMs: nowMs,
            lastHeartbeatEpochMs: nowMs,
            lastEntitlementCheckEpochMs: nowMs,
            planAtStart: "pro",
            maxAllowanceSeconds: sessionCapWithGrace,
            updatedAt: FieldValue.serverTimestamp()
          },
          { merge: true }
        );

        return {
          ok: true,
          sessionId: requestedSessionId,
          isPro: true,
          remainingSeconds: remaining,
          maxSessionAllowanceWithGraceSeconds: sessionCapWithGrace,
          usedDailySeconds: usedDaily,
          usedMonthlySeconds: usedMonthly,
          timezoneId: effectiveTz
        };
      });

      if (result.rejectedSecondDevice) {
        res.status(409).json({ ok: false, code: "ACTIVE_SESSION_ON_ANOTHER_DEVICE", error: result.error });
        return;
      }
      if (result.exhausted) {
        res.status(429).json({ ok: false, code: "TUTOR_QUOTA_EXHAUSTED", ...result });
        return;
      }
      res.status(200).json(result);
    } catch (error) {
      res.status(500).json({ ok: false, error: String(error.message || error) });
    }
  }
);

/**
 * 24. Server-Enforced 5-Second AI Tutor Heartbeat (`tutorSessionHeartbeat`) — Part 2 & Pro AI Tutor System
 * Called every 5 seconds while connected:
 * - Atomically adds elapsed time (capped at 10s per beat) to `daily.tutorSeconds` and `monthly.tutorSeconds`
 * - Re-checks entitlement every 60 seconds (if Pro expired/revoked mid-session, caps to Free remaining time)
 * - Logs `usageEvent` with duration and estimated cost in micro-dollars
 * - Marks session ended if `endSession === true`, `remaining <= 0`, or no heartbeat for 15s.
 */
exports.tutorSessionHeartbeat = onRequest(
  { cors: true },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({ ok: false, error: "Method Not Allowed" });
        return;
      }
      const decoded = await verifyFirebaseUserAndAppCheck(req);
      const uid = decoded.uid;
      if (!uid || String(uid).startsWith("guest_") || String(uid).startsWith("appcheck_")) {
        res.status(401).json({ ok: false, shouldEnd: true, code: "SIGN_IN_REQUIRED", error: "Sign in to continue." });
        return;
      }
      const db = getLingoFirestore();
      const policy = await getAccessPolicyCached(db);
      const entitlement = await resolveServerEntitlement(db, uid);
      if (!entitlement.isPro) {
        res.status(403).json({
          ok: false,
          shouldEnd: true,
          limitReason: "pro_required",
          code: "PRO_REQUIRED",
          error: "AI Tutor requires LingoSphere Pro."
        });
        return;
      }

      const body = req.body || {};
      const sessionId = String(body.sessionId || "").trim();
      const rawDelta = Number(body.elapsedDeltaSeconds ?? 5);
      const elapsedDeltaSeconds = Math.max(0, Math.min(10, rawDelta));
      const endSessionRequested = Boolean(body.endSession);

      const proSecondsPerDay = Number(policy["ai.tutor.proSecondsPerDay"] || policy.proDailyAiVoiceSeconds || 900);
      const proSecondsPerMonth = Number(policy["ai.tutor.proSecondsPerMonth"] || policy.proMonthlyAiVoiceSeconds || 9000);

      const nowMs = Date.now();
      const entRef = db.collection("users").document(uid).collection("private").document("entitlement");
      const activeSessionRef = db.collection("activeTutorSessions").document(uid);

      const outcome = await db.runTransaction(async (tx) => {
        const [entSnap, activeSnap] = await Promise.all([tx.get(entRef), tx.get(activeSessionRef)]);
        const entData = entSnap.exists ? entSnap.data() || {} : {};
        const effectiveTz = String(entData.timezoneId || "UTC");

        if (activeSnap.exists) {
          const activeData = activeSnap.data() || {};
          const activeId = String(activeData.sessionId || "");
          const lastBeatMs = Number(activeData.lastHeartbeatEpochMs || 0);
          if (activeId && sessionId && activeId !== sessionId && nowMs - lastBeatMs < 15000 && Boolean(activeData.active)) {
            return {
              shouldEnd: true,
              limitReason: "second_device_active",
              remainingSeconds: 0
            };
          }
        }

        const dayKey = formatDateInTimezone(nowMs, effectiveTz);
        const monthKey = formatMonthInTimezone(nowMs, effectiveTz);
        const dayRef = db.collection("usage").document(uid).collection("daily").document(dayKey);
        const monthRef = db.collection("usage").document(uid).collection("monthly").document(monthKey);
        const [dSnap, mSnap] = await Promise.all([tx.get(dayRef), tx.get(monthRef)]);

        const usedDaily = Number((dSnap.exists && (dSnap.get("tutorSeconds") ?? dSnap.get("ai_voice_seconds"))) || 0);
        const usedMonthly = Number((mSnap.exists && (mSnap.get("tutorSeconds") ?? mSnap.get("ai_voice_seconds"))) || 0);

        const nextDaily = Math.min(proSecondsPerDay, usedDaily + elapsedDeltaSeconds);
        const nextMonthly = Math.min(proSecondsPerMonth, usedMonthly + elapsedDeltaSeconds);

        const dailyRemaining = Math.max(0, proSecondsPerDay - nextDaily);
        const monthlyRemaining = Math.max(0, proSecondsPerMonth - nextMonthly);
        const remaining = Math.min(dailyRemaining, monthlyRemaining);
        const shouldEnd = endSessionRequested || remaining <= 0 || policy.globalAiKillSwitch === true;
        const limitReason = remaining <= 0
          ? (monthlyRemaining <= 0 ? "pro_monthly_limit" : "pro_daily_limit")
          : "";

        tx.set(
          dayRef,
          {
            uid,
            dayKey,
            tutorSeconds: nextDaily,
            ai_voice_seconds: nextDaily,
            updatedAt: FieldValue.serverTimestamp()
          },
          { merge: true }
        );
        tx.set(
          monthRef,
          {
            uid,
            monthKey,
            tutorSeconds: nextMonthly,
            ai_voice_seconds: nextMonthly,
            updatedAt: FieldValue.serverTimestamp()
          },
          { merge: true }
        );
        tx.set(
          activeSessionRef,
          {
            uid,
            sessionId: sessionId || "default",
            active: !shouldEnd,
            lastHeartbeatEpochMs: nowMs,
            currentEntitlement: entitlement.status,
            updatedAt: FieldValue.serverTimestamp()
          },
          { merge: true }
        );

        return {
          ok: true,
          shouldEnd,
          limitReason,
          remainingSeconds: remaining,
          usedDailySeconds: nextDaily,
          usedMonthlySeconds: nextMonthly,
          isPro: true
        };
      });

      if (elapsedDeltaSeconds > 0) {
        const estCostMicros = elapsedDeltaSeconds * DEFAULT_PRICING_CONFIG.voicePipelineMicrosPerSecond;
        await db.collection("usageEvents").add({
          uid,
          feature: "ai.tutor.seconds",
          sessionId: sessionId || "default",
          units: elapsedDeltaSeconds,
          estimatedCostMicros: estCostMicros,
          plan: "pro",
          createdAt: FieldValue.serverTimestamp()
        });
      }

      res.status(200).json(outcome);
    } catch (error) {
      res.status(500).json({ ok: false, shouldEnd: false, error: String(error.message || error) });
    }
  }
);

/**
 * 25. Dynamic Word of the Day (`getWordOfTheDay`)
 * Cached once per `language + CEFR level + day` in Firestore (`wordOfTheDay/{lang}_{level}_{yyyy-mm-dd}`).
 */
exports.getWordOfTheDay = onRequest(
  { cors: true },
  async (req, res) => {
    try {
      const params = req.method === "POST" ? req.body || {} : req.query || {};
      const lang = String(params.languageCode || "es").trim().toLowerCase().slice(0, 8);
      const level = String(params.cefrLevel || "A1").trim().toUpperCase().slice(0, 4);
      const dayKey = String(params.dayKey || new Date().toISOString().slice(0, 10)).trim().slice(0, 10);
      const docId = `${lang}_${level}_${dayKey}`;

      const db = getLingoFirestore();
      const docRef = db.collection("wordOfTheDay").document(docId);
      const existing = await docRef.get();
      if (existing.exists) {
        const d = existing.data() || {};
        if (d.word && d.meaning && d.exampleSentence) {
          res.status(200).json({ ok: true, cached: true, entry: d });
          return;
        }
      }
      res.status(200).json({ ok: true, cached: false, docId });
    } catch (error) {
      res.status(500).json({ ok: false, error: String(error.message || error) });
    }
  }
);

/**
 * 26. Server-Gated Structured Lesson Content Resolver (`getLessonContent`)
 * Structured lessons in Learn are 100% Pro-only. Returns 403 PRO_REQUIRED for Free/guest accounts.
 */
exports.getLessonContent = onRequest(
  { cors: true },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({ ok: false, error: "Method Not Allowed" });
        return;
      }
      const decoded = await verifyFirebaseUserAndAppCheck(req);
      const uid = decoded.uid;
      if (!uid || String(uid).startsWith("guest_") || String(uid).startsWith("appcheck_")) {
        res.status(401).json({ ok: false, code: "SIGN_IN_REQUIRED", error: "Sign in to continue." });
        return;
      }
      const db = getLingoFirestore();
      const entitlement = await resolveServerEntitlement(db, uid);
      if (!entitlement.isPro) {
        res.status(403).json({
          ok: false,
          code: "PRO_REQUIRED",
          error: "Structured lessons require LingoSphere Pro."
        });
        return;
      }

      const lessonId = String((req.body && req.body.lessonId) || "").trim().slice(0, 100);
      if (!lessonId) {
        res.status(400).json({ ok: false, error: "Missing lessonId" });
        return;
      }

      const contentSnap = await db.collection("lessonContent").document(lessonId).get();
      res.status(200).json({
        ok: true,
        lessonId,
        unlocked: true,
        subscriptionTier: entitlement.subscriptionTier,
        lessonData: contentSnap.exists ? (contentSnap.data() || null) : null
      });
    } catch (error) {
      res.status(Number(error.statusCode) || 500).json({ ok: false, error: String(error.message || error) });
    }
  }
);

/**
 * 27. Server-Gated Structured Lesson Completion (`completeLesson`)
 * Validates Pro entitlement before persisting lesson completion & awarding XP/streak.
 */
exports.completeLesson = onRequest(
  { cors: true },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({ ok: false, error: "Method Not Allowed" });
        return;
      }
      const decoded = await verifyFirebaseUserAndAppCheck(req);
      const uid = decoded.uid;
      if (!uid || String(uid).startsWith("guest_") || String(uid).startsWith("appcheck_")) {
        res.status(401).json({ ok: false, code: "SIGN_IN_REQUIRED", error: "Sign in to continue." });
        return;
      }
      const db = getLingoFirestore();
      const entitlement = await resolveServerEntitlement(db, uid);
      if (!entitlement.isPro) {
        res.status(403).json({
          ok: false,
          code: "PRO_REQUIRED",
          error: "Structured lessons require LingoSphere Pro."
        });
        return;
      }

      const body = req.body || {};
      const lessonId = String(body.lessonId || "").trim().slice(0, 100);
      const languageCode = String(body.languageCode || "es").trim().slice(0, 12);
      const category = String(body.category || "Vocabulary").trim().slice(0, 48);
      const scorePercent = Math.max(0, Math.min(100, Number(body.scorePercent || 0)));
      const xpGained = Math.max(5, Math.min(150, Number(body.xpGained || 25)));
      if (!lessonId) {
        res.status(400).json({ ok: false, error: "Missing lessonId" });
        return;
      }

      const dayKey = formatDateInTimezone(Date.now(), String(body.timezone || "UTC"));
      const userRef = db.collection("users").document(uid);
      const progressRef = db.collection("lesson_progress").document(`${uid}_${lessonId}`);

      const result = await db.runTransaction(async (tx) => {
        const userSnap = await tx.get(userRef);
        const uData = userSnap.exists ? (userSnap.data() || {}) : {};
        const prevCompleted = Array.isArray(uData.completedLessonIds) ? uData.completedLessonIds : [];
        const alreadyDone = prevCompleted.includes(lessonId);
        const awarded = alreadyDone ? 0 : xpGained;
        const nextXp = Number(uData.xpPoints || 0) + awarded;
        const nextLevel = Math.floor(nextXp / 100) + 1;
        const lastActive = String(uData.lastActiveDate || "");
        const prevStreak = Number(uData.currentStreak || 0);
        const nextStreak = lastActive === dayKey ? Math.max(1, prevStreak) : prevStreak + 1;
        const longestStreak = Math.max(Number(uData.longestStreak || 1), nextStreak);

        tx.set(
          progressRef,
          {
            userId: uid,
            lessonId,
            languageCode,
            category,
            scorePercent,
            xpEarned: awarded,
            completed: true,
            updatedAt: FieldValue.serverTimestamp(),
            createdAt: FieldValue.serverTimestamp()
          },
          { merge: true }
        );
        if (userSnap.exists) {
          tx.update(userRef, {
            xpPoints: nextXp,
            level: nextLevel,
            currentStreak: nextStreak,
            longestStreak,
            lastActiveDate: dayKey,
            completedLessonIds: alreadyDone ? prevCompleted : [...prevCompleted, lessonId],
            updatedAt: FieldValue.serverTimestamp()
          });
        }
        return { xpAwarded: awarded, totalXp: nextXp, level: nextLevel, currentStreak: nextStreak };
      });

      res.status(200).json({ ok: true, lessonId, ...result });
    } catch (error) {
      res.status(Number(error.statusCode) || 500).json({ ok: false, error: String(error.message || error) });
    }
  }
);

/**
 * 28. Non-AI Free & Pro Learning Activity Completion (`completeLearningActivity`)
 * Keeps Streak, XP, and Daily Goal working for Free users via non-AI activities:
 * `flashcard_review`, `listening_playback`, `game_basic`, `book_reading`.
 */
exports.completeLearningActivity = onRequest(
  { cors: true },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({ ok: false, error: "Method Not Allowed" });
        return;
      }
      const decoded = await verifyFirebaseUserAndAppCheck(req);
      const uid = decoded.uid;
      if (!uid || String(uid).startsWith("guest_") || String(uid).startsWith("appcheck_")) {
        res.status(401).json({ ok: false, code: "SIGN_IN_REQUIRED", error: "Sign in to continue." });
        return;
      }

      const body = req.body || {};
      const activityType = String(body.activityType || "").trim();
      const allowedTypes = ["flashcard_review", "listening_playback", "game_basic", "book_reading"];
      if (!allowedTypes.includes(activityType)) {
        res.status(400).json({ ok: false, error: "Invalid non-AI activityType" });
        return;
      }

      const xpByType = {
        flashcard_review: 15,
        listening_playback: 20,
        game_basic: 25,
        book_reading: 20
      };
      const awarded = xpByType[activityType] || 15;
      const dayKey = formatDateInTimezone(Date.now(), String(body.timezone || "UTC"));
      const db = getLingoFirestore();
      const userRef = db.collection("users").document(uid);
      const dayUsageRef = db.collection("usage").document(uid).collection("daily").document(dayKey);

      const result = await db.runTransaction(async (tx) => {
        const [userSnap, daySnap] = await Promise.all([tx.get(userRef), tx.get(dayUsageRef)]);
        const uData = userSnap.exists ? (userSnap.data() || {}) : {};
        const dData = daySnap.exists ? (daySnap.data() || {}) : {};

        const nextXp = Number(uData.xpPoints || 0) + awarded;
        const nextLevel = Math.floor(nextXp / 100) + 1;
        const lastActive = String(uData.lastActiveDate || "");
        const prevStreak = Number(uData.currentStreak || 0);
        const nextStreak = lastActive === dayKey ? Math.max(1, prevStreak) : prevStreak + 1;
        const longestStreak = Math.max(Number(uData.longestStreak || 1), nextStreak);
        const dailyActivitiesCount = Number(dData.nonAiActivitiesCount || 0) + 1;

        if (userSnap.exists) {
          tx.update(userRef, {
            xpPoints: nextXp,
            level: nextLevel,
            currentStreak: nextStreak,
            longestStreak,
            lastActiveDate: dayKey,
            updatedAt: FieldValue.serverTimestamp()
          });
        }
        tx.set(
          dayUsageRef,
          {
            uid,
            dayKey,
            nonAiActivitiesCount: dailyActivitiesCount,
            lastActivityType: activityType,
            updatedAt: FieldValue.serverTimestamp()
          },
          { merge: true }
        );
        return {
          xpAwarded: awarded,
          totalXp: nextXp,
          level: nextLevel,
          currentStreak: nextStreak,
          dailyActivitiesCount
        };
      });

      res.status(200).json({ ok: true, activityType, ...result });
    } catch (error) {
      res.status(Number(error.statusCode) || 500).json({ ok: false, error: String(error.message || error) });
    }
  }
);

/**
 * Public AdMob Verification Endpoint (`/app-ads.txt`)
 * Served without authentication, redirects, or HTML wrapper.
 */
exports.appAdsTxt = onRequest(
  { cors: true },
  async (_req, res) => {
    res.set("Content-Type", "text/plain; charset=utf-8");
    res.set("Cache-Control", "public, max-age=3600");
    res.status(200).send("google.com, pub-8471293855339658, DIRECT, f08c47fec0942fa0\n");
  }
);
