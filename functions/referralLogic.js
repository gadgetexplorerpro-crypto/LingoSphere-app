/**
 * LingoSphere — Referral & Rewards Domain Logic (Pure + Transactional Helpers)
 */

const APP_DOMAIN_URL =
  "https://ais-pre-smnebhnagujwa3llo3lj4b-861509315491.asia-east1.run.app";
const ANDROID_PACKAGE_NAME = "com.aistudio.lingosphere.kxpqzm";

const REWARD_CONSTANTS = Object.freeze({
  INVITER_XP_PER_REFERRAL: 100,
  REFERRED_WELCOME_XP: 100,
  MILESTONE_3_THRESHOLD: 3,
  MILESTONE_3_BADGE_ID: "community_builder_3",
  MILESTONE_3_BADGE_TITLE: "Language Ambassador",
  MILESTONE_5_THRESHOLD: 5,
  MILESTONE_5_BONUS_XP: 250,
  MILESTONE_5_BADGE_ID: "lingo_mentor_5",
  MILESTONE_5_BADGE_TITLE: "Lingo Mentor",
  MILESTONE_5_SPEAKING_CREDITS: 3,
  NEW_ACCOUNT_WINDOW_MS: 24 * 60 * 60 * 1000
});

const CODE_REGEX = /^[A-Z0-9]{6,16}$/;

function normalizeReferralCode(raw) {
  if (typeof raw !== "string") return null;
  const cleaned = raw
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, "");
  return CODE_REGEX.test(cleaned) ? cleaned : null;
}

function isValidReferralCode(raw) {
  return normalizeReferralCode(raw) !== null;
}

function buildInviteLink(code) {
  const clean = normalizeReferralCode(code) || String(code || "").trim().toUpperCase();
  return `${APP_DOMAIN_URL}/invite?ref=${encodeURIComponent(clean)}`;
}

function buildPlayStoreReferralUrl(code) {
  const clean = normalizeReferralCode(code) || String(code || "").trim().toUpperCase();
  const referrerParam = encodeURIComponent(`ref=${clean}`);
  return `https://play.google.com/store/apps/details?id=${ANDROID_PACKAGE_NAME}&referrer=${referrerParam}`;
}

function isEligibleNewAccount(accountCreatedAtMs, nowMs = Date.now()) {
  if (!Number.isFinite(accountCreatedAtMs) || accountCreatedAtMs <= 0) {
    return true;
  }
  return nowMs - accountCreatedAtMs <= REWARD_CONSTANTS.NEW_ACCOUNT_WINDOW_MS;
}

function computeInviterMilestoneUpdate(currentSummary = {}) {
  const prevCount = Number(currentSummary.successfulReferralsCount || 0);
  const newCount = prevCount + 1;
  const prevXp = Number(currentSummary.totalXpEarnedFromReferrals || 0);
  const prevCredits = Number(currentSummary.bonusSpeakingCredits || 0);
  const prevBadges = Array.isArray(currentSummary.unlockedBadges)
    ? [...currentSummary.unlockedBadges]
    : [];

  let xpEarnedThisReferral = REWARD_CONSTANTS.INVITER_XP_PER_REFERRAL;
  let creditsEarned = 0;
  let milestone3Awarded = false;
  let milestone5Awarded = false;

  if (
    newCount >= REWARD_CONSTANTS.MILESTONE_3_THRESHOLD &&
    !prevBadges.includes(REWARD_CONSTANTS.MILESTONE_3_BADGE_ID)
  ) {
    prevBadges.push(REWARD_CONSTANTS.MILESTONE_3_BADGE_ID);
    milestone3Awarded = true;
  }

  if (
    newCount >= REWARD_CONSTANTS.MILESTONE_5_THRESHOLD &&
    !prevBadges.includes(REWARD_CONSTANTS.MILESTONE_5_BADGE_ID)
  ) {
    prevBadges.push(REWARD_CONSTANTS.MILESTONE_5_BADGE_ID);
    xpEarnedThisReferral += REWARD_CONSTANTS.MILESTONE_5_BONUS_XP;
    creditsEarned += REWARD_CONSTANTS.MILESTONE_5_SPEAKING_CREDITS;
    milestone5Awarded = true;
  }

  return {
    successfulReferralsCount: newCount,
    totalXpEarnedFromReferrals: prevXp + xpEarnedThisReferral,
    bonusSpeakingCredits: prevCredits + creditsEarned,
    unlockedBadges: prevBadges.slice(0, 20),
    milestone3Awarded,
    milestone5Awarded,
    xpDeltaForInviter: xpEarnedThisReferral
  };
}

function renderInviteRedirectHtml(rawCode) {
  const cleanCode = normalizeReferralCode(rawCode) || "";
  const safeCode = cleanCode.replace(/[^A-Z0-9]/g, "");
  const deepLink = safeCode
    ? `lingosphere://invite?ref=${safeCode}`
    : "lingosphere://invite";
  const playStoreUrl = safeCode
    ? buildPlayStoreReferralUrl(safeCode)
    : `https://play.google.com/store/apps/details?id=${ANDROID_PACKAGE_NAME}`;

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Join LingoSphere</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background:#070D1E; color:#F8FAFC; margin:0; padding:24px; display:flex; align-items:center; justify-content:center; min-height:100vh; }
    .card { max-width:440px; width:100%; background:#0F172A; border:1px solid #334155; border-radius:20px; padding:28px; text-align:center; }
    .code { display:inline-block; background:#1E293B; color:#22D3EE; font-weight:800; letter-spacing:2px; padding:8px 16px; border-radius:10px; margin:12px 0; }
    .btn { display:block; background:#06B6D4; color:#04101D; font-weight:800; text-decoration:none; padding:14px 18px; border-radius:12px; margin-top:14px; }
    .btn.secondary { background:#1E293B; color:#E2E8F0; border:1px solid #334155; }
  </style>
</head>
<body>
  <div class="card">
    <h2>You're Invited to LingoSphere 🌍🗣️</h2>
    <p>Learn languages together. Join me and claim your +100 XP welcome bonus!</p>
    ${safeCode ? `<div class="code">${safeCode}</div>` : ""}
    <a class="btn" href="${deepLink}">Open LingoSphere App</a>
    <a class="btn secondary" href="${playStoreUrl}">Get LingoSphere on Google Play</a>
  </div>
  <script>
    setTimeout(function() { window.location.href = "${deepLink}"; }, 150);
  </script>
</body>
</html>`;
}

module.exports = {
  APP_DOMAIN_URL,
  ANDROID_PACKAGE_NAME,
  REWARD_CONSTANTS,
  CODE_REGEX,
  normalizeReferralCode,
  isValidReferralCode,
  buildInviteLink,
  buildPlayStoreReferralUrl,
  isEligibleNewAccount,
  computeInviterMilestoneUpdate,
  renderInviteRedirectHtml
};
