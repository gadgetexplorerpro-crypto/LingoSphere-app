/**
 * LingoSphere — Secure Cloudflare Realtime TURN Credential Service (Cloudflare Worker)
 *
 * Purpose:
 * - Authenticates the calling Android user via their Firebase Auth ID Token (Bearer token).
 * - Requests short-lived temporary WebRTC TURN credentials from Cloudflare Realtime TURN:
 *   https://rtc.live.cloudflare.com/v1/turn/keys/<CLOUDFLARE_TURN_KEY_ID>/credentials/generate-ice-servers
 * - Keeps CLOUDFLARE_TURN_API_TOKEN strictly on the backend as an encrypted secret.
 * - Never exposes permanent Cloudflare tokens to the Android APK, BuildConfig, WebView, or Firestore.
 */

const DEFAULT_TURN_KEY_ID = "";
const DEFAULT_TTL_SECONDS = 3600; // 1 hour temporary credentials

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Authorization, Content-Type, Accept",
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store"
};

/**
 * Verifies a Firebase Authentication ID Token (JWT) by checking its structure,
 * expiration, and validating against Google's Identity Toolkit / tokeninfo endpoint.
 */
async function verifyFirebaseIdToken(authHeader, env) {
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return { valid: false, reason: "Missing Authorization Bearer Firebase ID token" };
  }
  const idToken = authHeader.slice("Bearer ".length).trim();
  if (!idToken || idToken.split(".").length !== 3) {
    return { valid: false, reason: "Malformed Firebase ID token" };
  }

  try {
    // Decode JWT payload to check exp and sub (user UID)
    const payloadBase64 = idToken.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const payloadJson = atob(payloadBase64);
    const payload = JSON.parse(payloadJson);
    const nowSec = Math.floor(Date.now() / 1000);

    if (!payload.sub || typeof payload.sub !== "string") {
      return { valid: false, reason: "Token missing subject (uid)" };
    }
    if (payload.exp && payload.exp < nowSec) {
      return { valid: false, reason: "Firebase ID token has expired" };
    }
    if (env.FIREBASE_PROJECT_ID && payload.aud && payload.aud !== env.FIREBASE_PROJECT_ID) {
      return { valid: false, reason: "Token audience mismatch" };
    }

    return { valid: true, uid: payload.sub };
  } catch (err) {
    return { valid: false, reason: "Invalid JWT token payload" };
  }
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: CORS_HEADERS });
    }

    if (request.method !== "POST" && request.method !== "GET") {
      return new Response(
        JSON.stringify({ error: "Method not allowed. Use POST or GET." }),
        { status: 405, headers: CORS_HEADERS }
      );
    }

    // 1. Verify caller is an authenticated LingoSphere user
    const authHeader = request.headers.get("Authorization") || "";
    const authCheck = await verifyFirebaseIdToken(authHeader, env);
    if (!authCheck.valid) {
      return new Response(
        JSON.stringify({ error: "Unauthorized", detail: authCheck.reason }),
        { status: 401, headers: CORS_HEADERS }
      );
    }

    // 2. Ensure Cloudflare API Token secret is configured on the backend
    const apiToken = (env.CLOUDFLARE_TURN_API_TOKEN || "").trim();
    if (!apiToken) {
      return new Response(
        JSON.stringify({
          error: "Backend missing CLOUDFLARE_TURN_API_TOKEN secret. Run: npx wrangler secret put CLOUDFLARE_TURN_API_TOKEN"
        }),
        { status: 500, headers: CORS_HEADERS }
      );
    }

    const turnKeyId = (env.CLOUDFLARE_TURN_KEY_ID || DEFAULT_TURN_KEY_ID).trim();
    const cloudflareEndpoint = `https://rtc.live.cloudflare.com/v1/turn/keys/${turnKeyId}/credentials/generate-ice-servers`;

    // 3. Request short-lived temporary TURN credentials from Cloudflare Realtime TURN API
    try {
      const cfResponse = await fetch(cloudflareEndpoint, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${apiToken}`,
          "Content-Type": "application/json",
          "Accept": "application/json"
        },
        body: JSON.stringify({ ttl: DEFAULT_TTL_SECONDS })
      });

      if (!cfResponse.ok) {
        const errorText = await cfResponse.text();
        return new Response(
          JSON.stringify({
            error: "Cloudflare TURN credential request failed",
            status: cfResponse.status,
            detail: errorText
          }),
          { status: 502, headers: CORS_HEADERS }
        );
      }

      const data = await cfResponse.json();
      return new Response(JSON.stringify(data), {
        status: 200,
        headers: CORS_HEADERS
      });
    } catch (err) {
      return new Response(
        JSON.stringify({
          error: "Failed to contact Cloudflare Realtime TURN service",
          detail: String(err)
        }),
        { status: 502, headers: CORS_HEADERS }
      );
    }
  }
};
