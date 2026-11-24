const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const CHARLIE_UID = "charlie_uid_789";
const ADMIN_UID = "admin_uid_001";
const ADMIN_EMAIL = "shehzadawrites5@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(ms = 2000) {
  return new Date(Date.now() - ms);
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("jobs").where("visibility", "==", "active").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("posts").doc("p1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorUsername: "alice",
      caption: "Hello",
      category: "General",
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Private user PII and subcollections are isolated to owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Ayesha Khan",
      username: "ayesha_k",
      email: "alice@example.com",
      city: "Lahore",
      bio: "Student",
      language: "en",
      themeMode: "system",
      notificationsEnabled: true,
      profileCompleted: true,
      blockedUserIds: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Ghost field & role escalation rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({ enabled: true })
  );

  // Cross-user PII read/write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: pastDate(1000),
    })
  );

  // Owner notes & todos succeed; cross-user fails
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "FSc Physics",
      content: "Chapter 3 notes",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notes").get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("todos").doc("todo_1").set({
      userId: ALICE_UID,
      title: "Revise Urdu essay",
      reminderLabel: "Today 6:00 PM",
      completed: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("todos").get());
});

test("Social posts: create, filtered list, like, comment, and ownership enforcement", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  // Valid Kotlin-aligned create (no @DocumentId id field)
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Ayesha",
      authorUsername: "ayesha",
      caption: "Assalam-o-Alaikum from Lahore!",
      category: "Community",
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject @DocumentId "id" key in body & reject future timestamps
  await assertFails(
    aliceDb.collection("posts").doc("post_bad").set({
      id: "post_bad",
      authorId: ALICE_UID,
      authorName: "Ayesha",
      authorUsername: "ayesha",
      caption: "Bad",
      category: "Community",
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("posts").doc("post_future").set({
      authorId: ALICE_UID,
      authorName: "Ayesha",
      authorUsername: "ayesha",
      caption: "Future",
      category: "Community",
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Filtered query succeeds; unfiltered query fails
  await assertSucceeds(
    bobDb.collection("posts").where("visibility", "==", "public").get()
  );
  await assertFails(bobDb.collection("posts").get());

  // Bob cannot edit or delete Alice's post caption
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      caption: "Hijacked",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(bobDb.collection("posts").doc("post_1").delete());

  // Bob can like & increment likesCount by 1
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("likes").doc(BOB_UID).set({
      parentId: "post_1",
      userId: BOB_UID,
      createdAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 1,
      updatedAt: pastDate(1000),
    })
  );

  // Bob cannot jump likesCount by +100
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 100,
      updatedAt: pastDate(1000),
    })
  );

  // Bob can comment and query comments with parentId filter
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c_1").set({
      parentId: "post_1",
      authorId: BOB_UID,
      authorName: "Bilal",
      text: "Walaikum Assalam!",
      createdAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").collection("comments").where("parentId", "==", "post_1").get()
  );
});

test("Chats & messages: strictly isolated to participants", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastDate(5000);
  const chatId = `${ALICE_UID}_${BOB_UID}`;

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Ayesha", [BOB_UID]: "Bilal" },
      lastMessage: "Salam!",
      lastSenderId: ALICE_UID,
      unreadBy: [BOB_UID],
      hiddenFor: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("m_1").set({
      chatId,
      senderId: ALICE_UID,
      senderName: "Ayesha",
      text: "Salam!",
      participantIds: [ALICE_UID, BOB_UID],
      status: "sent",
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("chats").where("participantIds", "array-contains", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").where("participantIds", "array-contains", ALICE_UID).get()
  );

  // Outsider Charlie cannot read chat, read message, query unfiltered, or query Alice's messages
  await assertFails(charlieDb.collection("chats").doc(chatId).get());
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("messages").doc("m_1").get());
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("messages").get());
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").where("participantIds", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").doc("m_2").set({
      chatId,
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      text: "Intrusion",
      participantIds: [ALICE_UID, BOB_UID],
      status: "sent",
      createdAt: ts,
    })
  );
});

test("Marketplace & local services: owner-scoped writes and active-only queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("marketplace_listings").doc("item_1").set({
      sellerId: ALICE_UID,
      sellerName: "Ayesha",
      title: "Used Laptop i5",
      description: "Clean condition, Lahore pickup only",
      category: "Computers",
      pricePkr: 65000,
      city: "Lahore",
      condition: "Used",
      visibility: "active",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("marketplace_listings").where("visibility", "==", "active").get()
  );
  await assertFails(bobDb.collection("marketplace_listings").get());
  await assertFails(
    bobDb.collection("marketplace_listings").doc("item_1").update({
      pricePkr: 1,
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("service_listings").doc("srv_1").set({
      providerId: ALICE_UID,
      providerName: "Ayesha",
      title: "FSc Physics Tutoring",
      category: "Education and tutoring",
      city: "Lahore",
      description: "Evening online and home tutoring",
      priceEstimate: "PKR 5,000 / month",
      visibility: "active",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("service_listings").where("visibility", "==", "active").get()
  );
  await assertFails(bobDb.collection("service_listings").doc("srv_1").delete());
});

test("Admin & email spoofing protections on jobs, announcements, and reports", async () => {
  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const realAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(5000);

  const jobPayload = {
    publisherId: ADMIN_UID,
    title: "Junior Android Developer",
    company: "Lahore Tech Studio",
    city: "Lahore",
    category: "Private",
    qualification: "BS Computer Science",
    salaryRange: "PKR 80,000 - 120,000",
    description: "Kotlin and Jetpack Compose role",
    applicationInstructions: "Apply via company portal",
    sampleListing: true,
    reviewStatus: "sample_demo",
    visibility: "active",
    createdAt: ts,
    updatedAt: ts,
  };

  // Unverified email spoof fails
  await assertFails(spoofedAdminDb.collection("jobs").doc("job_1").set(jobPayload));
  // Regular user fails
  await assertFails(aliceDb.collection("jobs").doc("job_1").set(jobPayload));
  // Verified admin succeeds
  await assertSucceeds(realAdminDb.collection("jobs").doc("job_1").set(jobPayload));
  // Regular user can list active jobs
  await assertSucceeds(aliceDb.collection("jobs").where("visibility", "==", "active").get());

  // Reports: regular user can create, cannot list all reports; admin can list/update
  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      reporterId: ALICE_UID,
      targetType: "post",
      targetId: "post_1",
      reason: "Spam link",
      status: "open",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(aliceDb.collection("reports").get());
  await assertSucceeds(realAdminDb.collection("reports").get());
});
