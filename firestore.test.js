const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
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

test("Unauthenticated user: cannot read users profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      rating: 1200,
      gamesPlayed: 0,
      wins: 0,
      losses: 0,
      draws: 0,
      puzzlesSolved: 0,
      dailyStreak: 0,
      bio: "Chess lover",
      achievements: []
    })
  );
});

test("Authenticated user: cannot create profile for another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      rating: 1200
    })
  );
});

test("Authenticated user: can create matchmaking queue entry for self", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("matchmaking_queue").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      rating: 1200,
      status: "waiting"
    })
  );
});

test("Community posts: authenticated users can create and read posts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("community_posts").doc("post_1").set({
      postId: "post_1",
      authorId: ALICE_UID,
      authorName: "Alice",
      title: "Great game today!",
      content: "Queen sacrifice on h7 led to mate in 3.",
      likesCount: 0
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("community_posts").doc("post_1").get()
  );
});
