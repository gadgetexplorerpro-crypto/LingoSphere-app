const test = require("node:test");
const assert = require("node:assert/strict");
const {
  APP_DOMAIN_URL,
  REWARD_CONSTANTS,
  normalizeReferralCode,
  isValidReferralCode,
  buildInviteLink,
  buildPlayStoreReferralUrl,
  isEligibleNewAccount,
  computeInviterMilestoneUpdate,
  renderInviteRedirectHtml
} = require("./referralLogic");

test("Referral codes normalize and validate cleanly", () => {
  assert.equal(normalizeReferralCode(" alice9x "), "ALICE9X");
  assert.equal(normalizeReferralCode("lingo-2026!"), "LINGO2026");
  assert.equal(normalizeReferralCode("ab1"), null);
  assert.equal(isValidReferralCode("ALEX4K8M"), true);
  assert.equal(isValidReferralCode("bad"), false);
});

test("Invite link and Play Store referrer URL use configured LingoSphere domain and package", () => {
  const link = buildInviteLink("alex4k8m");
  assert.equal(link, `${APP_DOMAIN_URL}/invite?ref=ALEX4K8M`);

  const playUrl = buildPlayStoreReferralUrl("alex4k8m");
  assert.ok(playUrl.includes("id=com.aistudio.lingosphere.kxpqzm"));
  assert.ok(playUrl.includes("referrer=ref%3DALEX4K8M"));
});

test("Only newly registered accounts within 24h window are eligible for signup referrals", () => {
  const now = 1_750_000_000_000;
  assert.equal(isEligibleNewAccount(now - 60_000, now), true);
  assert.equal(isEligibleNewAccount(now - REWARD_CONSTANTS.NEW_ACCOUNT_WINDOW_MS + 1000, now), true);
  assert.equal(isEligibleNewAccount(now - REWARD_CONSTANTS.NEW_ACCOUNT_WINDOW_MS - 1000, now), false);
});

test("Inviter milestone progression awards 100 XP, 3-ref badge, and 5-ref bonus + speaking credits once", () => {
  // Referral #1
  const r1 = computeInviterMilestoneUpdate({
    successfulReferralsCount: 0,
    totalXpEarnedFromReferrals: 0,
    bonusSpeakingCredits: 0,
    unlockedBadges: []
  });
  assert.equal(r1.successfulReferralsCount, 1);
  assert.equal(r1.xpDeltaForInviter, 100);
  assert.equal(r1.totalXpEarnedFromReferrals, 100);
  assert.equal(r1.milestone3Awarded, false);
  assert.equal(r1.milestone5Awarded, false);

  // Referral #3 -> unlocks Language Ambassador badge
  const r3 = computeInviterMilestoneUpdate({
    successfulReferralsCount: 2,
    totalXpEarnedFromReferrals: 200,
    bonusSpeakingCredits: 0,
    unlockedBadges: []
  });
  assert.equal(r3.successfulReferralsCount, 3);
  assert.equal(r3.milestone3Awarded, true);
  assert.ok(r3.unlockedBadges.includes("community_builder_3"));
  assert.equal(r3.xpDeltaForInviter, 100);

  // Referral #5 -> unlocks Lingo Mentor badge + 250 bonus XP + 3 AI speaking credits
  const r5 = computeInviterMilestoneUpdate({
    successfulReferralsCount: 4,
    totalXpEarnedFromReferrals: 400,
    bonusSpeakingCredits: 0,
    unlockedBadges: ["community_builder_3"]
  });
  assert.equal(r5.successfulReferralsCount, 5);
  assert.equal(r5.milestone5Awarded, true);
  assert.ok(r5.unlockedBadges.includes("lingo_mentor_5"));
  assert.equal(r5.xpDeltaForInviter, 350); // 100 base + 250 milestone bonus
  assert.equal(r5.totalXpEarnedFromReferrals, 750);
  assert.equal(r5.bonusSpeakingCredits, 3);

  // Referral #6 -> does not duplicate milestone 3 or 5 rewards
  const r6 = computeInviterMilestoneUpdate(r5);
  assert.equal(r6.successfulReferralsCount, 6);
  assert.equal(r6.milestone3Awarded, false);
  assert.equal(r6.milestone5Awarded, false);
  assert.equal(r6.xpDeltaForInviter, 100);
  assert.equal(r6.bonusSpeakingCredits, 3);
});

test("Web invite HTML redirect contains sanitized code, deep link, and Play Store fallback", () => {
  const html = renderInviteRedirectHtml("lingo88a");
  assert.ok(html.includes("LINGO88A"));
  assert.ok(html.includes("lingosphere://invite?ref=LINGO88A"));
  assert.ok(html.includes("com.aistudio.lingosphere.kxpqzm"));
});
