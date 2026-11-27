const test = require("node:test");
const fs = require("node:fs");
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");

const PROJECT_ID = "sounding-tangki-realtime";
const PLANT_UNIT = "FAC-TEXTURIZING";

let testEnv;

function pastTimestamp() {
  return new Date(Date.now() - 1000);
}

function validTankStock(uid, overrides = {}) {
  return {
    plantUnit: PLANT_UNIT,
    tankId: "T.101",
    product: "RBDPO",
    soundingCm: 401.0,
    tableLevelCm: 400.0,
    highLevelCm: 401.5,
    totalVolumeL: 65000.5,
    totalStockKg: 58500,
    tonase: 58.5,
    temp: 45.0,
    density: 0.9012,
    vcf: 0.9991,
    manualKg: false,
    note: "Ready",
    accountName: "basri",
    operatorName: "basri",
    dateKey: "2026-11-26",
    dateLabel: "Kam, 26 Nov 2026",
    timeLabel: "07:15",
    recordedAtEpochMs: Date.now() - 2000,
    updatedByUid: uid,
    updatedAt: pastTimestamp(),
    ...overrides,
  };
}

function validHistoryEntry(uid, overrides = {}) {
  return {
    plantUnit: PLANT_UNIT,
    userId: uid,
    dateKey: "2026-11-26",
    dateLabel: "Kam, 26 Nov 2026",
    timeLabel: "07:15",
    accountName: "basri",
    operatorName: "basri",
    tankId: "T.101",
    product: "RBDPO",
    soundingCm: 401.0,
    tableLevelCm: 400.0,
    highLevelCm: 401.5,
    temp: 45.0,
    totalVolumeL: 65000.5,
    density: 0.9012,
    vcf: 0.9991,
    totalStockKg: 58500,
    tonase: 58.5,
    manualKg: false,
    note: "",
    recordedAtEpochMs: Date.now() - 2000,
    createdAt: pastTimestamp(),
    ...overrides,
  };
}

function validShiftReport(uid, overrides = {}) {
  return {
    plantUnit: PLANT_UNIT,
    dateKey: "2026-11-26",
    dateLabel: "Kamis, 26 Nov 2026",
    timeLabel: "07:15",
    accountName: "basri",
    operatorName: "basri",
    nbNote: "Transfer berjalan normal",
    totalAwalKg: 120000,
    totalAkhirKg: 125000,
    transferCount: 2,
    draftJson: '{"dateLabel":"Kamis, 26 Nov 2026","timeLabel":"07:15","tankRows":[]}',
    recordedAtEpochMs: Date.now() - 2000,
    updatedByUid: uid,
    updatedAt: pastTimestamp(),
    ...overrides,
  };
}

test.before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085,
    },
  });
});

test.after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test.beforeEach(async () => {
  await testEnv.clearFirestore();
});

test("unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await assertFails(
    unauthDb
      .collection("current_tank_stocks")
      .where("plantUnit", "==", PLANT_UNIT)
      .get()
  );
  await assertFails(
    unauthDb
      .collection("current_tank_stocks")
      .doc("T_101")
      .set(validTankStock("u1"))
  );
  await assertFails(
    unauthDb
      .collection("sounding_history")
      .where("plantUnit", "==", PLANT_UNIT)
      .get()
  );
  await assertFails(
    unauthDb
      .collection("shift_reports")
      .where("plantUnit", "==", PLANT_UNIT)
      .get()
  );
  await assertFails(unauthDb.collection("unknown_collection").doc("x").get());
});

test("current_tank_stocks: create, shared read/list, update with accountName & timestamp, and schema enforcement", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  const bobDb = testEnv.authenticatedContext("bob").firestore();

  await assertSucceeds(
    aliceDb.collection("current_tank_stocks").doc("T_101").set(validTankStock("alice"))
  );

  await assertSucceeds(
    bobDb
      .collection("current_tank_stocks")
      .where("plantUnit", "==", PLANT_UNIT)
      .get()
  );
  await assertSucceeds(bobDb.collection("current_tank_stocks").doc("T_101").get());

  // Unfiltered list must fail
  await assertFails(bobDb.collection("current_tank_stocks").get());

  // Bob updates T_101 with his accountName and timestamp
  await assertSucceeds(
    bobDb
      .collection("current_tank_stocks")
      .doc("T_101")
      .set(
        validTankStock("bob", {
          accountName: "arif",
          operatorName: "arif",
          timeLabel: "15:20",
          totalStockKg: 61200,
          tonase: 61.2,
        })
      )
  );

  // Reject invalid accountName formats (uppercase, spaces, @shift suffix, empty)
  await assertFails(
    bobDb
      .collection("current_tank_stocks")
      .doc("T_102")
      .set(validTankStock("bob", { tankId: "T.102", accountName: "arif@shiftA" }))
  );
  await assertFails(
    bobDb
      .collection("current_tank_stocks")
      .doc("T_102")
      .set(validTankStock("bob", { tankId: "T.102", accountName: "Arif" }))
  );
  await assertFails(
    bobDb
      .collection("current_tank_stocks")
      .doc("T_102")
      .set(validTankStock("bob", { tankId: "T.102", accountName: "" }))
  );

  // Reject spoofing updatedByUid
  await assertFails(
    bobDb
      .collection("current_tank_stocks")
      .doc("T_101")
      .set(validTankStock("alice", { accountName: "arif" }))
  );

  // Reject mutating tankId on update
  await assertFails(
    bobDb
      .collection("current_tank_stocks")
      .doc("T_101")
      .set(validTankStock("bob", { tankId: "T.102" }))
  );

  // Reject extra unexpected field
  await assertFails(
    aliceDb
      .collection("current_tank_stocks")
      .doc("T_102")
      .set(validTankStock("alice", { tankId: "T.102", extraField: true }))
  );

  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection("current_tank_stocks")
      .doc("T_102")
      .set(
        validTankStock("alice", {
          tankId: "T.102",
          updatedAt: new Date(Date.now() + 60_000),
        })
      )
  );

  // Reject delete
  await assertFails(aliceDb.collection("current_tank_stocks").doc("T_101").delete());
});

test("sounding_history: create with accountName & timestamp, shared list, immutable update denied, author-only delete", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  const bobDb = testEnv.authenticatedContext("bob").firestore();

  await assertSucceeds(
    aliceDb.collection("sounding_history").doc("entry_1").set(validHistoryEntry("alice"))
  );

  await assertFails(
    aliceDb
      .collection("sounding_history")
      .doc("entry_bad_op")
      .set(validHistoryEntry("alice", { accountName: "basri spasi" }))
  );

  await assertSucceeds(
    bobDb
      .collection("sounding_history")
      .where("plantUnit", "==", PLANT_UNIT)
      .get()
  );

  await assertFails(bobDb.collection("sounding_history").get());

  await assertFails(
    aliceDb
      .collection("sounding_history")
      .doc("entry_1")
      .update({ totalStockKg: 99999 })
  );

  await assertFails(bobDb.collection("sounding_history").doc("entry_1").delete());
  await assertSucceeds(aliceDb.collection("sounding_history").doc("entry_1").delete());

  await assertFails(
    bobDb.collection("sounding_history").doc("entry_2").set(validHistoryEntry("alice"))
  );

  await assertFails(
    bobDb
      .collection("sounding_history")
      .doc("entry_3")
      .set(validHistoryEntry("bob", { plantUnit: "OTHER-UNIT" }))
  );
});

test("shift_reports: create, shared list, update with accountName & timestamp, and field protection", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  const bobDb = testEnv.authenticatedContext("bob").firestore();

  await assertSucceeds(
    aliceDb
      .collection("shift_reports")
      .doc("2026-11-26_basri")
      .set(validShiftReport("alice"))
  );

  await assertSucceeds(
    bobDb
      .collection("shift_reports")
      .where("plantUnit", "==", PLANT_UNIT)
      .get()
  );

  await assertFails(bobDb.collection("shift_reports").get());

  await assertSucceeds(
    bobDb
      .collection("shift_reports")
      .doc("2026-11-26_basri")
      .set(
        validShiftReport("bob", {
          accountName: "rizal",
          operatorName: "rizal",
          timeLabel: "16:10",
          nbNote: "Diperbarui oleh rizal",
          totalAkhirKg: 130000,
        })
      )
  );

  await assertFails(
    bobDb
      .collection("shift_reports")
      .doc("2026-11-26_rizal")
      .set(validShiftReport("bob", { accountName: "rizal@shiftB" }))
  );

  // Reject mutating dateKey on existing report
  await assertFails(
    bobDb
      .collection("shift_reports")
      .doc("2026-11-26_basri")
      .set(validShiftReport("bob", { dateKey: "2026-11-27" }))
  );

  await assertFails(aliceDb.collection("shift_reports").doc("2026-11-26_basri").delete());
});

test("device_licenses: ANDROID_ID trial sync, anti-reset & anti-extension protection, and permanent activation upgrade", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const devId = "and_9f8e7d6c5b4a";
  const sixtyDaysMs = 60 * 24 * 60 * 60 * 1000;
  const initialLaunch = Date.now() - 10 * 24 * 60 * 60 * 1000;
  const initialTrialEnds = initialLaunch + sixtyDaysMs;

  const baseLicense = (overrides = {}) => ({
    plantUnit: PLANT_UNIT,
    deviceId: devId,
    firstInstalledAt: initialLaunch,
    trialEndsAt: initialTrialEnds,
    isActivated: false,
    activationCode: "",
    activatedAtEpochMs: 0,
    lastUsername: "basri",
    updatedByUid: "alice",
    updatedAt: pastTimestamp(),
    ...overrides,
  });

  await assertFails(unauthDb.collection("device_licenses").doc(devId).get());
  await assertFails(unauthDb.collection("device_licenses").doc(devId).set(baseLicense()));

  await assertFails(
    aliceDb
      .collection("device_licenses")
      .doc(devId)
      .set(baseLicense({ trialEndsAt: initialLaunch + sixtyDaysMs + 1000 }))
  );

  await assertSucceeds(aliceDb.collection("device_licenses").doc(devId).set(baseLicense()));
  await assertSucceeds(aliceDb.collection("device_licenses").doc(devId).get());
  await assertFails(aliceDb.collection("device_licenses").get());

  await assertFails(
    aliceDb
      .collection("device_licenses")
      .doc(devId)
      .set(baseLicense({ firstInstalledAt: initialLaunch + 86_400_000, trialEndsAt: initialTrialEnds + 86_400_000 }))
  );

  await assertSucceeds(
    aliceDb
      .collection("device_licenses")
      .doc(devId)
      .set(
        baseLicense({
          isActivated: true,
          activationCode: "SPTF-8472",
          activatedAtEpochMs: Date.now() - 500,
          lastUsername: "arif",
        })
      )
  );

  await assertFails(
    aliceDb
      .collection("device_licenses")
      .doc(devId)
      .set(baseLicense({ isActivated: false, activationCode: "" }))
  );

  await assertFails(aliceDb.collection("device_licenses").doc(devId).delete());
});

test("activation_codes: single-use (used: false -> true) boundDeviceId claim, immutable overwrite denied, invalid code rejected", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  const bobDb = testEnv.authenticatedContext("bob").firestore();
  const devA = "and_device_aaa111";
  const devB = "and_device_bbb222";

  const claimedCodeDoc = (uid, code, deviceId) => ({
    plantUnit: PLANT_UNIT,
    code,
    used: true,
    boundDeviceId: deviceId,
    claimedByUsername: "basri",
    claimedByUid: uid,
    claimedAtEpochMs: Date.now() - 1000,
    claimedAt: pastTimestamp(),
  });

  await assertFails(aliceDb.collection("activation_codes").get());
  await assertFails(
    aliceDb.collection("activation_codes").doc("FAKE-1234").set(claimedCodeDoc("alice", "FAKE-1234", devA))
  );

  await assertSucceeds(
    aliceDb.collection("activation_codes").doc("SPTF-8472").set(claimedCodeDoc("alice", "SPTF-8472", devA))
  );

  await assertSucceeds(bobDb.collection("activation_codes").doc("SPTF-8472").get());
  await assertFails(
    bobDb.collection("activation_codes").doc("SPTF-8472").set(claimedCodeDoc("bob", "SPTF-8472", devB))
  );

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("activation_codes").doc("SPTF-3916").set({
      plantUnit: PLANT_UNIT,
      code: "SPTF-3916",
      used: false,
      boundDeviceId: "",
      claimedByUsername: "",
      claimedByUid: "",
      claimedAtEpochMs: 0,
      claimedAt: pastTimestamp(),
    });
  });

  await assertSucceeds(
    bobDb.collection("activation_codes").doc("SPTF-3916").set(claimedCodeDoc("bob", "SPTF-3916", devB))
  );
  await assertFails(
    aliceDb.collection("activation_codes").doc("SPTF-3916").set(claimedCodeDoc("alice", "SPTF-3916", devA))
  );

  await assertFails(aliceDb.collection("activation_codes").doc("SPTF-8472").delete());
});

test("users: verified profile create, direct lookup, self-scoped uid/email query, owner-only update, no enumeration", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext("alice", { email: "basri@permata.local" }).firestore();
  const bobDb = testEnv.authenticatedContext("bob", { email: "bob@permata.local" }).firestore();

  const validHash =
    "pbkdf2_sha256$120000$0123456789abcdef0123456789abcdef$0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

  const validUserDoc = (uid, username, overrides = {}) => ({
    plantUnit: PLANT_UNIT,
    username,
    email: `${username}@permata.local`,
    passwordHash: validHash,
    isVerified: true,
    uid,
    verifiedAtEpochMs: Date.now() - 2000,
    updatedAt: pastTimestamp(),
    ...overrides,
  });

  // Unauthenticated cannot write or list-enumerate users
  await assertFails(unauthDb.collection("users").doc("basri").set(validUserDoc("alice", "basri")));
  await assertFails(unauthDb.collection("users").get());

  // Direct get on non-existent username succeeds cleanly (returns exists == false, no PERMISSION_DENIED)
  await assertSucceeds(unauthDb.collection("users").doc("tidakada").get());

  // Unfiltered list enumeration always denied even when signed in
  await assertFails(aliceDb.collection("users").get());

  // Reject unverified flag, plaintext passwordHash, or forged UID
  await assertFails(
    aliceDb.collection("users").doc("basri").set(validUserDoc("alice", "basri", { isVerified: false }))
  );
  await assertFails(
    aliceDb.collection("users").doc("basri").set(validUserDoc("alice", "basri", { passwordHash: "plaintext123" }))
  );
  await assertFails(
    aliceDb.collection("users").doc("basri").set(validUserDoc("bob", "basri"))
  );

  // Valid verified user profile creation succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc("basri").set(validUserDoc("alice", "basri"))
  );

  // Direct single-doc get by exact username succeeds for login/reset lookup (unauth and auth)
  await assertSucceeds(unauthDb.collection("users").doc("basri").get());
  await assertSucceeds(bobDb.collection("users").doc("basri").get());

  // Self-scoped queries by own uid or own email succeed; cross-user query denied
  await assertSucceeds(
    aliceDb.collection("users").where("plantUnit", "==", PLANT_UNIT).where("uid", "==", "alice").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").where("plantUnit", "==", PLANT_UNIT).where("email", "==", "basri@permata.local").get()
  );
  await assertFails(
    bobDb.collection("users").where("plantUnit", "==", PLANT_UNIT).where("uid", "==", "alice").get()
  );
  await assertFails(
    bobDb.collection("users").where("plantUnit", "==", PLANT_UNIT).where("email", "==", "basri@permata.local").get()
  );

  // Cross-user update denied
  await assertFails(
    bobDb.collection("users").doc("basri").set(validUserDoc("bob", "basri"))
  );

  // Owner update succeeds
  const nextHash =
    "pbkdf2_sha256$120000$fedcba9876543210fedcba9876543210$fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";
  await assertSucceeds(
    aliceDb.collection("users").doc("basri").set(validUserDoc("alice", "basri", { passwordHash: nextHash }))
  );

  // Delete denied
  await assertFails(aliceDb.collection("users").doc("basri").delete());
});

test("negative tableLevel/highLevel are accepted, but out-of-range values are still rejected", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();

  // Valid negative tableLevelCm (-2.5) and highLevelCm (-1.5) with low soundingCm (1.0) are accepted
  await assertSucceeds(
    aliceDb
      .collection("current_tank_stocks")
      .doc("T_101")
      .set(
        validTankStock("alice", {
          tableLevelCm: -2.5,
          highLevelCm: -1.5,
          soundingCm: 1.0,
        })
      )
  );
  await assertSucceeds(
    aliceDb
      .collection("sounding_history")
      .doc("entry_neg_ok")
      .set(
        validHistoryEntry("alice", {
          tableLevelCm: -2.5,
          highLevelCm: -1.5,
          soundingCm: 1.0,
        })
      )
  );

  // Out-of-range tableLevelCm (-501) is still rejected
  await assertFails(
    aliceDb
      .collection("current_tank_stocks")
      .doc("T_101_bad_tbl")
      .set(validTankStock("alice", { tableLevelCm: -501 }))
  );
  await assertFails(
    aliceDb
      .collection("sounding_history")
      .doc("entry_bad_tbl")
      .set(validHistoryEntry("alice", { tableLevelCm: -501 }))
  );

  // Out-of-range highLevelCm (-1001) is still rejected
  await assertFails(
    aliceDb
      .collection("current_tank_stocks")
      .doc("T_101_bad_high")
      .set(validTankStock("alice", { highLevelCm: -1001 }))
  );
  await assertFails(
    aliceDb
      .collection("sounding_history")
      .doc("entry_bad_high")
      .set(validHistoryEntry("alice", { highLevelCm: -1001 }))
  );
});


