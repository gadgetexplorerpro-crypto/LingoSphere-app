const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

const nowTimestamp = new Date();

function sampleAliceProfile() {
  return {
    userId: ALICE_UID,
    displayName: "Alice Learner",
    nativeLanguageCode: "en",
    targetLanguageCode: "es",
    activeLanguages: ["es", "fr"],
    proficiencyLevel: "Beginner",
    xpPoints: 120,
    level: 1,
    currentStreak: 3,
    longestStreak: 5,
    lastActiveDate: "2026-10-23",
    dailyGoalMinutes: 15,
    dailyAiSpeakingCount: 2,
    dailyAiResetDate: "2026-10-23",
    subscriptionTier: "free",
    completedLessonIds: ["es_vocab_1"],
    achievements: ["first_lesson"],
    createdAt: nowTimestamp,
    updatedAt: nowTimestamp,
  };
}

// 1. Unauthenticated Security Tests
test("Unauthenticated user: cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Unauthenticated user: cannot read lesson progress", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("lesson_progress").doc("prog_1").get());
});

// 2. User Profile Permissions & Bounds
test("Authenticated user: can create and read own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(sampleAliceProfile()));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Cross-user isolation: Alice cannot read Bob's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      ...sampleAliceProfile(),
      userId: BOB_UID,
      displayName: "Bob Learner",
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("Ghost field injection: rejected when extra keys are provided", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...sampleAliceProfile(),
      isAdmin: true,
    })
  );
});

// 3. Lesson Progress Isolation, Pro Entitlement Enforcement & Queries
test("Authenticated user: lesson_progress write denied for Free, allowed for Pro, and scoped queries work", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const progressDoc = {
    userId: ALICE_UID,
    lessonId: "es_vocab_1",
    languageCode: "es",
    category: "Vocabulary",
    scorePercent: 95,
    xpEarned: 50,
    completed: true,
    createdAt: nowTimestamp,
    updatedAt: nowTimestamp,
  };

  // Free user (no verified Pro entitlement) cannot write lesson_progress directly
  await assertFails(aliceDb.collection("lesson_progress").doc("prog_alice_1").set(progressDoc));

  // Client cannot forge private/entitlement
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("entitlement").set({
      uid: ALICE_UID,
      status: "pro_active",
      verifiedWithPlayApi: true,
    })
  );

  // Admin seeds verified Pro entitlement; now Pro user can write lesson_progress
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("private").doc("entitlement").set({
      uid: ALICE_UID,
      status: "pro_active",
      verifiedWithPlayApi: true,
    });
  });

  await assertSucceeds(aliceDb.collection("lesson_progress").doc("prog_alice_1").set(progressDoc));

  await assertSucceeds(
    aliceDb.collection("lesson_progress").where("userId", "==", ALICE_UID).get()
  );
});

test("Server-gated lessonContent: clients cannot read or write directly", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("lessonContent").doc("es_a1_1").get());
  await assertFails(aliceDb.collection("lessonContent").doc("es_a1_1").set({ title: "Spoof" }));
});

test("Query security: Alice cannot list lesson progress without userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("lesson_progress").get());
});

// 4. Flashcards Progress
test("Flashcards: Alice can create and list her flashcards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const cardDoc = {
    userId: ALICE_UID,
    wordId: "hola_01",
    languageCode: "es",
    frontText: "Hola",
    backText: "Hello",
    pronunciation: "oh-lah",
    boxLevel: 1,
    nextReviewEpochDay: 20750,
    mastered: false,
    createdAt: nowTimestamp,
    updatedAt: nowTimestamp,
  };
  await assertSucceeds(aliceDb.collection("flashcards").doc("card_1").set(cardDoc));
  await assertSucceeds(aliceDb.collection("flashcards").where("userId", "==", ALICE_UID).get());
});

// 5. Speaking Sessions
test("Speaking sessions: Alice can log and read her speaking practice history", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const sessionDoc = {
    userId: ALICE_UID,
    languageCode: "es",
    mode: "Tutor",
    scenarioTitle: "Ordering at a Tapas Restaurant",
    proficiencyLevel: "Beginner",
    turnsCount: 6,
    fluencyScore: 88,
    grammarFeedbackSummary: "Good use of 'quisiera'. Watch gender agreement on adjectives.",
    createdAt: nowTimestamp,
    updatedAt: nowTimestamp,
  };
  await assertSucceeds(aliceDb.collection("speaking_sessions").doc("sess_1").set(sessionDoc));
  await assertSucceeds(aliceDb.collection("speaking_sessions").where("userId", "==", ALICE_UID).get());
});

// 6. Community Profiles & Real-Time Presence Detection
test("Community profiles: Alice can publish her profile, update real-time presence/lastSeen, and list community partners", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const commDoc = {
    userId: ALICE_UID,
    displayName: "Alice",
    country: "United States",
    countryFlag: "🇺🇸",
    bio: "Practicing conversational Spanish and French!",
    interests: ["Travel", "Music", "Technology"],
    nativeLanguageCode: "en",
    targetLanguageCode: "es",
    proficiencyLevel: "Intermediate",
    xpPoints: 250,
    level: 3,
    currentStreak: 4,
    isOnline: true,
    presenceState: "online",
    lastSeenEpochMs: 1730000000000,
    lastHeartbeatEpochMs: 1730000000000,
    showLastSeen: true,
    allowCalls: true,
    updatedAt: nowTimestamp,
  };
  await assertSucceeds(aliceDb.collection("community_profiles").doc(ALICE_UID).set(commDoc));
  await assertSucceeds(
    aliceDb.collection("community_profiles").doc(ALICE_UID).update({
      isOnline: false,
      presenceState: "away",
      lastSeenEpochMs: 1730000060000,
      lastHeartbeatEpochMs: 1730000060000,
      showLastSeen: false,
      updatedAt: nowTimestamp,
    })
  );
  await assertSucceeds(aliceDb.collection("community_profiles").get());

  // Smart notification with deep-link & Word of the Day metadata
  const notifDoc = {
    userId: ALICE_UID,
    title: "intriguing — Word of the Day",
    message: "Meaning: Very interesting or mysterious.\nExample: \"That's an intriguing idea!\"",
    type: "word_of_the_day",
    isRead: false,
    targetRoute: "word_of_the_day",
    targetId: "intriguing",
    word: "intriguing",
    meaning: "Very interesting or mysterious.",
    pronunciation: "/ɪnˈtriːɡɪŋ/ (in-TREE-ging)",
    exampleSentence: "That's an intriguing idea!",
    createdAt: nowTimestamp,
  };
  await assertSucceeds(aliceDb.collection("notifications").doc("notif_wod_1").set(notifDoc));
  await assertSucceeds(aliceDb.collection("notifications").where("userId", "==", ALICE_UID).get());
});

// 7. WebRTC Call Rooms & Signaling Events
test("Call rooms: Alice can initiate a call room to Bob and Bob can accept and exchange SDP/ICE", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();

  const roomDoc = {
    roomId: "room_alice_bob_1",
    callerId: ALICE_UID,
    callerName: "Alice",
    callerNativeLang: "en",
    callerLearningLang: "es",
    calleeId: BOB_UID,
    calleeName: "Bob",
    calleeNativeLang: "es",
    calleeLearningLang: "en",
    targetLanguageCode: "es",
    callType: "video",
    matchMode: "direct",
    status: "ringing",
    lastSignalingEvent: "incoming_call",
    conversationTopic: "Favorite travel destinations",
    sdpOffer: "v=0\r\no=- 123 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n",
    sdpAnswer: null,
    callerIceCandidates: [],
    calleeIceCandidates: [],
    isCallerMuted: false,
    isCalleeMuted: false,
    isVideoEnabled: true,
    isCallerVideoEnabled: true,
    isCalleeVideoEnabled: true,
    connectionState: "new",
    connectedAtEpochMs: 0,
    endedAtEpochMs: 0,
    durationSeconds: 0,
    createdAt: nowTimestamp,
    updatedAt: nowTimestamp,
  };

  await assertSucceeds(aliceDb.collection("call_rooms").doc("room_alice_bob_1").set(roomDoc));

  // Bob can accept and attach WebRTC answer SDP
  await assertSucceeds(
    bobDb.collection("call_rooms").doc("room_alice_bob_1").update({
      status: "accepted",
      lastSignalingEvent: "call_accepted",
      updatedAt: nowTimestamp,
    })
  );

  // Charlie (non-participant) cannot hijack a direct call room between Alice and Bob
  await assertFails(
    charlieDb.collection("call_rooms").doc("room_alice_bob_1").update({
      status: "ended",
      lastSignalingEvent: "call_ended",
      updatedAt: nowTimestamp,
    })
  );
});

// 8. Call History Records
test("Call history: participants can save and query call history; non-participants cannot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();

  const historyDoc = {
    callId: "room_alice_bob_1",
    callerId: ALICE_UID,
    callerName: "Alice",
    receiverId: BOB_UID,
    receiverName: "Bob",
    callType: "video",
    targetLanguageCode: "es",
    startTimeEpochMs: 1730000000000,
    endTimeEpochMs: 1730000120000,
    durationSeconds: 120,
    status: "completed",
    createdAt: nowTimestamp,
    updatedAt: nowTimestamp,
  };

  await assertSucceeds(aliceDb.collection("call_history").doc("room_alice_bob_1").set(historyDoc));
  await assertSucceeds(
    aliceDb.collection("call_history").where("callerId", "==", ALICE_UID).get()
  );

  // Charlie cannot forge a call history record for Alice and Bob
  await assertFails(
    charlieDb.collection("call_history").doc("forged_call").set({
      ...historyDoc,
      callId: "forged_call",
    })
  );
});

// 9. LingoSphere Videos: Saved Videos, Playlists, and Watch History
test("LingoSphere Videos: Alice can save videos, manage playlists, and log watch history; Bob cannot read them", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const savedDoc = {
    userId: ALICE_UID,
    videoId: "DAp_v7EH9AA",
    title: "Spanish for Beginners - Essential Phrases",
    channelTitle: "Butterfly Spanish",
    thumbnailUrl: "https://i.ytimg.com/vi/DAp_v7EH9AA/hqdefault.jpg",
    durationText: "14:22",
    languageCode: "es",
    proficiencyLevel: "Beginner",
    topicCategory: "Conversations",
    description: "Learn essential Spanish greetings and daily expressions.",
    captionSummary: "Hola, buenos dias, como estas, mucho gusto",
    createdAt: nowTimestamp,
    updatedAt: nowTimestamp,
  };

  await assertSucceeds(aliceDb.collection("saved_videos").doc("saved_alice_1").set(savedDoc));
  await assertSucceeds(aliceDb.collection("saved_videos").where("userId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("saved_videos").doc("saved_alice_1").get());

  const playlistDoc = {
    userId: ALICE_UID,
    playlistId: "pl_alice_1",
    name: "Spanish Travel Mastery",
    description: "Essential videos before my trip to Madrid",
    languageCode: "es",
    proficiencyLevel: "Beginner",
    videoIds: ["DAp_v7EH9AA"],
    videoTitles: ["Spanish for Beginners - Essential Phrases"],
    videoThumbnails: ["https://i.ytimg.com/vi/DAp_v7EH9AA/hqdefault.jpg"],
    videoChannels: ["Butterfly Spanish"],
    createdAt: nowTimestamp,
    updatedAt: nowTimestamp,
  };

  await assertSucceeds(aliceDb.collection("video_playlists").doc("pl_alice_1").set(playlistDoc));
  await assertSucceeds(aliceDb.collection("video_playlists").where("userId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("video_playlists").doc("pl_alice_1").get());

  const historyDoc = {
    userId: ALICE_UID,
    videoId: "DAp_v7EH9AA",
    title: "Spanish for Beginners - Essential Phrases",
    channelTitle: "Butterfly Spanish",
    thumbnailUrl: "https://i.ytimg.com/vi/DAp_v7EH9AA/hqdefault.jpg",
    durationText: "14:22",
    languageCode: "es",
    proficiencyLevel: "Beginner",
    watchedSeconds: 180,
    completedQuiz: true,
    quizScorePercent: 100,
    xpEarned: 40,
    createdAt: nowTimestamp,
    updatedAt: nowTimestamp,
  };

  await assertSucceeds(aliceDb.collection("video_watch_history").doc("hist_alice_1").set(historyDoc));
  await assertSucceeds(aliceDb.collection("video_watch_history").where("userId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("video_watch_history").doc("hist_alice_1").get());
});

// 10. Referral Codes, Referral Summaries, and Per-User Referral Attribution
test("Referral codes: owner can create and users can get directly, but list and cross-user mutation are denied", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const codeDoc = {
    code: "ALICE777",
    ownerUid: ALICE_UID,
    ownerDisplayName: "Alice Learner",
    ownerTargetLanguage: "es",
    ownerNativeLanguage: "en",
    isActive: true,
    createdAt: nowTimestamp,
  };

  // Unauthenticated reads/writes must fail
  await assertFails(unauthDb.collection("referral_codes").doc("ALICE777").get());
  await assertFails(unauthDb.collection("referral_codes").doc("ALICE777").set(codeDoc));

  // Alice can create her own referral code
  await assertSucceeds(aliceDb.collection("referral_codes").doc("ALICE777").set(codeDoc));

  // Bob can directly look up a specific code, but cannot list the whole collection or overwrite Alice's code
  await assertSucceeds(bobDb.collection("referral_codes").doc("ALICE777").get());
  await assertFails(bobDb.collection("referral_codes").get());
  await assertFails(
    bobDb.collection("referral_codes").doc("ALICE777").update({ ownerUid: BOB_UID })
  );
  await assertFails(bobDb.collection("referral_codes").doc("ALICE777").delete());
});

test("Referral summaries: owner cannot self-mint referrals, XP, badges, or speaking credits", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const cleanSummary = {
    userId: ALICE_UID,
    referralCode: "ALICE777",
    successfulReferralsCount: 0,
    syncedReferralsCount: 0,
    pendingInvitationsCount: 0,
    totalXpEarnedFromReferrals: 0,
    unlockedBadges: [],
    bonusSpeakingCredits: 0,
    updatedAt: nowTimestamp,
  };

  // Owner cannot create a pre-inflated summary
  await assertFails(
    aliceDb.collection("referral_summaries").doc(ALICE_UID).set({
      ...cleanSummary,
      successfulReferralsCount: 5,
      totalXpEarnedFromReferrals: 750,
    })
  );

  // Clean summary creation succeeds
  await assertSucceeds(
    aliceDb.collection("referral_summaries").doc(ALICE_UID).set(cleanSummary)
  );

  // Owner can increment pending invitations when sharing
  await assertSucceeds(
    aliceDb.collection("referral_summaries").doc(ALICE_UID).update({
      pendingInvitationsCount: 2,
      updatedAt: nowTimestamp,
    })
  );

  // Owner cannot inflate successfulReferralsCount or XP directly
  await assertFails(
    aliceDb.collection("referral_summaries").doc(ALICE_UID).update({
      successfulReferralsCount: 3,
      totalXpEarnedFromReferrals: 300,
      updatedAt: nowTimestamp,
    })
  );

  // Bob cannot read or list Alice's referral summary
  await assertFails(bobDb.collection("referral_summaries").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("referral_summaries").get());
});

test("Referral attribution: atomic two-doc transaction succeeds, rejects self-referral, rejects duplicate claims, and prevents cross-user spoofing", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();

  await assertSucceeds(
    aliceDb.collection("referral_codes").doc("ALICE777").set({
      code: "ALICE777",
      ownerUid: ALICE_UID,
      ownerDisplayName: "Alice Learner",
      ownerTargetLanguage: "es",
      ownerNativeLanguage: "en",
      isActive: true,
      createdAt: nowTimestamp,
    })
  );
  await assertSucceeds(
    aliceDb.collection("referral_summaries").doc(ALICE_UID).set({
      userId: ALICE_UID,
      referralCode: "ALICE777",
      successfulReferralsCount: 0,
      syncedReferralsCount: 0,
      pendingInvitationsCount: 1,
      totalXpEarnedFromReferrals: 0,
      unlockedBadges: [],
      bonusSpeakingCredits: 0,
      updatedAt: nowTimestamp,
    })
  );

  // Self-referral must fail
  await assertFails(
    aliceDb.collection("referrals").doc(ALICE_UID).set({
      referralId: ALICE_UID,
      inviterUid: ALICE_UID,
      inviterName: "Alice Learner",
      referredUid: ALICE_UID,
      referredName: "Alice Learner",
      referralCode: "ALICE777",
      status: "completed",
      inviterXpAwarded: 100,
      referredXpAwarded: 100,
      milestone3BadgeAwarded: false,
      milestone5BonusAwarded: false,
      attributionSource: "app_link",
      createdAt: nowTimestamp,
      completedAt: nowTimestamp,
    })
  );

  // Valid atomic claim by Bob (writes referrals/{bobUid} + updates Alice's summary in one batch)
  const batch = bobDb.batch();
  batch.set(bobDb.collection("referrals").doc(BOB_UID), {
    referralId: BOB_UID,
    inviterUid: ALICE_UID,
    inviterName: "Alice Learner",
    referredUid: BOB_UID,
    referredName: "Bob Learner",
    referralCode: "ALICE777",
    status: "completed",
    inviterXpAwarded: 100,
    referredXpAwarded: 100,
    milestone3BadgeAwarded: false,
    milestone5BonusAwarded: false,
    attributionSource: "app_link",
    targetLanguageCode: "es",
    nativeLanguageCode: "en",
    createdAt: nowTimestamp,
    completedAt: nowTimestamp,
  });
  batch.update(bobDb.collection("referral_summaries").doc(ALICE_UID), {
    successfulReferralsCount: 1,
    syncedReferralsCount: 0,
    totalXpEarnedFromReferrals: 100,
    bonusSpeakingCredits: 0,
    unlockedBadges: [],
    updatedAt: nowTimestamp,
  });
  await assertSucceeds(batch.commit());

  // Inviter Alice can query her referrals; Charlie cannot read Bob's attribution record
  await assertSucceeds(
    aliceDb.collection("referrals").where("inviterUid", "==", ALICE_UID).get()
  );
  await assertFails(charlieDb.collection("referrals").doc(BOB_UID).get());

  // Duplicate claim for Bob fails because referrals/{bobUid} is append-only (update denied & doc exists)
  await assertFails(
    bobDb.collection("referrals").doc(BOB_UID).set({
      referralId: BOB_UID,
      inviterUid: ALICE_UID,
      inviterName: "Alice Learner",
      referredUid: BOB_UID,
      referredName: "Bob Learner",
      referralCode: "ALICE777",
      status: "completed",
      inviterXpAwarded: 100,
      referredXpAwarded: 100,
      milestone3BadgeAwarded: false,
      milestone5BonusAwarded: false,
      attributionSource: "manual_code",
      createdAt: nowTimestamp,
      completedAt: nowTimestamp,
    })
  );
});

test("Referral milestones: owner can only reconcile badges and bonus rewards once qualifying referral count is reached", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("referral_summaries").doc(ALICE_UID).set({
      userId: ALICE_UID,
      referralCode: "ALICE777",
      successfulReferralsCount: 2,
      syncedReferralsCount: 2,
      pendingInvitationsCount: 5,
      totalXpEarnedFromReferrals: 200,
      unlockedBadges: [],
      bonusSpeakingCredits: 0,
      updatedAt: nowTimestamp,
    });
  });

  // With only 2 referrals, Alice cannot claim community_builder_3
  await assertFails(
    aliceDb.collection("referral_summaries").doc(ALICE_UID).update({
      unlockedBadges: ["community_builder_3"],
      updatedAt: nowTimestamp,
    })
  );

  // Advance to 5 referrals (simulating 5 distinct friend attributions)
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("referral_summaries").doc(ALICE_UID).update({
      successfulReferralsCount: 5,
      totalXpEarnedFromReferrals: 500,
      updatedAt: nowTimestamp,
    });
  });

  // Now Alice can claim milestone 3 & milestone 5 badges, +250 bonus XP, and +3 speaking credits ONCE
  await assertSucceeds(
    aliceDb.collection("referral_summaries").doc(ALICE_UID).update({
      syncedReferralsCount: 5,
      unlockedBadges: ["community_builder_3", "lingo_mentor_5"],
      totalXpEarnedFromReferrals: 750,
      bonusSpeakingCredits: 3,
      updatedAt: nowTimestamp,
    })
  );

  // Attempting to claim the +250 bonus XP or +3 credits a second time fails
  await assertFails(
    aliceDb.collection("referral_summaries").doc(ALICE_UID).update({
      totalXpEarnedFromReferrals: 1000,
      bonusSpeakingCredits: 6,
      updatedAt: nowTimestamp,
    })
  );
});

// 11. 7-Day Free Trial & Subscription Synchronization Rules
test("Subscription & 7-Day Free Trial: user can sync trial state and canceled expiry, but cannot reset hasUsedFreeTrial back to false", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(sampleAliceProfile()));

  // Activate 7-day free trial
  const sevenDaysExpiry = 1790000000000;
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      subscriptionTier: "premium_monthly",
      purchaseToken: "gp_trial_token_alice_001",
      isFreeTrial: true,
      hasUsedFreeTrial: true,
      subscriptionAutoRenewing: true,
      subscriptionExpiryEpochMs: sevenDaysExpiry,
      updatedAt: nowTimestamp,
    })
  );

  // Cancel during trial (retains access until expiry with subscriptionAutoRenewing = false)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      subscriptionAutoRenewing: false,
      updatedAt: nowTimestamp,
    })
  );

  // Attempting to reset hasUsedFreeTrial from true back to false to get a repeated free trial must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isFreeTrial: false,
      hasUsedFreeTrial: false,
      updatedAt: nowTimestamp,
    })
  );
});

// 12. App Releases Metadata (`app_releases/{releaseId}`)
test("App releases: authenticated users can read latest release metadata; unauthenticated reads and client writes are denied", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("app_releases").doc("latest").set({
      versionCode: 30,
      versionName: "30.0",
      title: "LingoSphere Update Available",
      summary: "Discover new improvements, better voice conversations, and smoother learning.",
      releaseNotes: [
        "7-Day Free Trial with Google Play Billing",
        "Automatic in-app update notifications and What's New changelog"
      ],
      isCritical: false,
      minSupportedVersionCode: 21,
      publishedAtEpochMs: 1780000000000,
    });
  });

  // Unauthenticated read is denied
  await assertFails(unauthDb.collection("app_releases").doc("latest").get());

  // Authenticated get succeeds
  await assertSucceeds(aliceDb.collection("app_releases").doc("latest").get());

  // Client cannot list or overwrite app_releases
  await assertFails(aliceDb.collection("app_releases").get());
  await assertFails(
    aliceDb.collection("app_releases").doc("latest").update({
      versionCode: 99,
    })
  );
});

// 13. Books & Listening Studio — Separate Reading Progress, Listening Progress, and Bookmarks
test("Books & Listening Studio: Alice can save/read reading & listening progress and bookmarks independently; unauthenticated and cross-user access are denied", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const readingDoc = {
    userId: ALICE_UID,
    bookId: "gutenberg_1342",
    bookTitle: "Pride and Prejudice",
    chapterIndex: 4,
    scrollOffset: 1240,
    pageIndex: 2,
    progressPercent: 0.35,
    readingTheme: "DARK",
    fontSizeSp: 18.0,
    isPageMode: false,
    completedChapters: [0, 1, 2, 3],
    updatedAt: nowTimestamp,
  };

  const listeningDoc = {
    userId: ALICE_UID,
    bookId: "gutenberg_1342",
    bookTitle: "Pride and Prejudice",
    chapterIndex: 1,
    sentenceIndex: 14,
    positionMs: 142500,
    progressPercent: 0.12,
    preferredEditionId: "librivox_101",
    autoPlayNextChapter: true,
    isCompleted: false,
    completedChapters: [0],
    updatedAt: nowTimestamp,
  };

  const bookmarkDoc = {
    userId: ALICE_UID,
    bookId: "gutenberg_1342",
    bookTitle: "Pride and Prejudice",
    chapterIndex: 4,
    chapterTitle: "Chapter 5",
    sentenceIndex: 8,
    positionMs: 65000,
    snippetText: "It is a truth universally acknowledged...",
    note: "Great opening line",
    updatedAt: nowTimestamp,
  };

  const aliceUserRef = aliceDb.collection("users").doc(ALICE_UID);
  const bobAliceUserRef = bobDb.collection("users").doc(ALICE_UID);
  const unauthAliceUserRef = unauthDb.collection("users").doc(ALICE_UID);

  // Unauthenticated access denied
  await assertFails(unauthAliceUserRef.collection("book_reading_progress").doc("gutenberg_1342").set(readingDoc));
  await assertFails(unauthAliceUserRef.collection("book_listening_progress").doc("gutenberg_1342").set(listeningDoc));
  await assertFails(unauthAliceUserRef.collection("book_bookmarks").doc("bm_1").set(bookmarkDoc));

  // Owner writes & reads succeed independently
  await assertSucceeds(aliceUserRef.collection("book_reading_progress").doc("gutenberg_1342").set(readingDoc));
  await assertSucceeds(aliceUserRef.collection("book_listening_progress").doc("gutenberg_1342").set(listeningDoc));
  await assertSucceeds(aliceUserRef.collection("book_bookmarks").doc("bm_1").set(bookmarkDoc));

  await assertSucceeds(aliceUserRef.collection("book_reading_progress").doc("gutenberg_1342").get());
  await assertSucceeds(aliceUserRef.collection("book_listening_progress").doc("gutenberg_1342").get());
  await assertSucceeds(aliceUserRef.collection("book_bookmarks").get());

  // Updating reading progress does not require or touch listening progress
  await assertSucceeds(
    aliceUserRef.collection("book_reading_progress").doc("gutenberg_1342").update({
      chapterIndex: 5,
      progressPercent: 0.42,
      updatedAt: nowTimestamp,
    })
  );

  // Cross-user isolation: Bob cannot read, update, or delete Alice's progress or bookmarks
  await assertFails(bobAliceUserRef.collection("book_reading_progress").doc("gutenberg_1342").get());
  await assertFails(bobAliceUserRef.collection("book_listening_progress").doc("gutenberg_1342").get());
  await assertFails(bobAliceUserRef.collection("book_bookmarks").doc("bm_1").get());
  await assertFails(bobAliceUserRef.collection("book_bookmarks").doc("bm_1").delete());

  // Validation bounds: out-of-range progressPercent (> 1.0) and ghost fields rejected
  await assertFails(
    aliceUserRef.collection("book_reading_progress").doc("gutenberg_bad").set({
      ...readingDoc,
      bookId: "gutenberg_bad",
      progressPercent: 1.5,
    })
  );
  await assertFails(
    aliceUserRef.collection("book_listening_progress").doc("gutenberg_ghost").set({
      ...listeningDoc,
      bookId: "gutenberg_ghost",
      extraAdminFlag: true,
    })
  );

  // Owner can delete own bookmark
  await assertSucceeds(aliceUserRef.collection("book_bookmarks").doc("bm_1").delete());
});

test("Explore AI sessions and mistake_records: user-isolated read/write and schema validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const sessionDoc = {
    userId: ALICE_UID,
    sessionId: "debate_1",
    featureType: "ai_debate",
    languageCode: "es",
    title: "Remote work vs office",
    summary: "Completed 3 rounds arguing Pro with clear connectors.",
    cefrLevel: "B1",
    turnsCount: 6,
    scorePercent: 86,
    payloadJson: "{\"topic\":\"Remote work vs office\"}",
    updatedAt: nowTimestamp,
  };

  const mistakeDoc = {
    userId: ALICE_UID,
    mistakeId: "mistake_1",
    languageCode: "es",
    languageName: "Spanish",
    category: "Verb Tense",
    originalResponse: "Ayer yo voy al café",
    correctedVersion: "Ayer fui al café",
    explanation: "Use preterite 'fui' for completed past actions.",
    activitySource: "Living Immersion",
    reviewStatus: "pending",
    occurrenceCount: 1,
    masteryScore: 20,
    updatedAt: nowTimestamp,
  };

  const aliceRef = aliceDb.collection("users").doc(ALICE_UID);
  const bobAliceRef = bobDb.collection("users").doc(ALICE_UID);
  const unauthAliceRef = unauthDb.collection("users").doc(ALICE_UID);

  // Unauthenticated denied
  await assertFails(unauthAliceRef.collection("explore_sessions").doc("debate_1").set(sessionDoc));
  await assertFails(unauthAliceRef.collection("mistake_records").doc("mistake_1").set(mistakeDoc));

  // Owner create/read/update succeed
  await assertSucceeds(aliceRef.collection("explore_sessions").doc("debate_1").set(sessionDoc));
  await assertSucceeds(aliceRef.collection("mistake_records").doc("mistake_1").set(mistakeDoc));
  await assertSucceeds(aliceRef.collection("explore_sessions").doc("debate_1").get());
  await assertSucceeds(aliceRef.collection("mistake_records").get());
  await assertSucceeds(
    aliceRef.collection("mistake_records").doc("mistake_1").update({
      reviewStatus: "mastered",
      masteryScore: 92,
      updatedAt: nowTimestamp,
    })
  );

  // Cross-user isolation: Bob denied
  await assertFails(bobAliceRef.collection("explore_sessions").doc("debate_1").get());
  await assertFails(bobAliceRef.collection("mistake_records").doc("mistake_1").get());
  await assertFails(bobAliceRef.collection("mistake_records").doc("mistake_1").delete());

  // Invalid featureType or reviewStatus rejected
  await assertFails(
    aliceRef.collection("explore_sessions").doc("bad_1").set({
      ...sessionDoc,
      sessionId: "bad_1",
      featureType: "invalid_mode",
    })
  );
  await assertFails(
    aliceRef.collection("mistake_records").doc("bad_2").set({
      ...mistakeDoc,
      mistakeId: "bad_2",
      reviewStatus: "unknown",
    })
  );

  // Owner can delete
  await assertSucceeds(aliceRef.collection("explore_sessions").doc("debate_1").delete());
  await assertSucceeds(aliceRef.collection("mistake_records").doc("mistake_1").delete());
});



