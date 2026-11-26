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
    lastShift: "Pagi",
    lastOperator: "Basri",
    dateKey: "2026-11-26",
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
    shift: "Pagi",
    operatorName: "Basri",
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
    shift: "Pagi",
    operatorName: "Arif, Basri",
    nbNote: "Transfer berjalan normal",
    totalAwalKg: 120000,
    totalAkhirKg: 125000,
    transferCount: 2,
    draftJson: '{"dateLabel":"Kamis, 26 Nov 2026","shift":"PAGI","tankRows":[]}',
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

test("current_tank_stocks: create, cross-shift read/list, update, and schema enforcement", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  const bobDb = testEnv.authenticatedContext("bob").firestore();

  // Create valid tank stock by Alice (Shift Pagi)
  await assertSucceeds(
    aliceDb.collection("current_tank_stocks").doc("T_101").set(validTankStock("alice"))
  );

  // Bob (Shift Sore) can list and read with plantUnit filter
  await assertSucceeds(
    bobDb
      .collection("current_tank_stocks")
      .where("plantUnit", "==", PLANT_UNIT)
      .get()
  );
  await assertSucceeds(bobDb.collection("current_tank_stocks").doc("T_101").get());

  // Unfiltered list must fail (zero-trust query constraint)
  await assertFails(bobDb.collection("current_tank_stocks").get());

  // Bob updates T_101 on Shift Sore with his own UID
  await assertSucceeds(
    bobDb
      .collection("current_tank_stocks")
      .doc("T_101")
      .set(
        validTankStock("bob", {
          lastShift: "Sore",
          lastOperator: "Arif",
          totalStockKg: 61200,
          tonase: 61.2,
        })
      )
  );

  // Reject spoofing updatedByUid
  await assertFails(
    bobDb
      .collection("current_tank_stocks")
      .doc("T_101")
      .set(validTankStock("alice", { lastShift: "Sore" }))
  );

  // Reject mutating tankId on update
  await assertFails(
    bobDb
      .collection("current_tank_stocks")
      .doc("T_101")
      .set(validTankStock("bob", { tankId: "T.102" }))
  );

  // Reject invalid shift enum
  await assertFails(
    aliceDb
      .collection("current_tank_stocks")
      .doc("T_102")
      .set(validTankStock("alice", { tankId: "T.102", lastShift: "Subuh" }))
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

  // Reject delete on current_tank_stocks
  await assertFails(aliceDb.collection("current_tank_stocks").doc("T_101").delete());
});

test("sounding_history: create, cross-shift list, immutable update denied, author-only delete", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  const bobDb = testEnv.authenticatedContext("bob").firestore();

  // Alice saves sounding record
  await assertSucceeds(
    aliceDb.collection("sounding_history").doc("entry_1").set(validHistoryEntry("alice"))
  );

  // Bob reads cross-shift history with plantUnit filter
  await assertSucceeds(
    bobDb
      .collection("sounding_history")
      .where("plantUnit", "==", PLANT_UNIT)
      .get()
  );

  // Unfiltered history list must fail
  await assertFails(bobDb.collection("sounding_history").get());

  // Updating an existing history record is denied (immutable log)
  await assertFails(
    aliceDb
      .collection("sounding_history")
      .doc("entry_1")
      .update({ totalStockKg: 99999 })
  );

  // Bob cannot delete Alice's record
  await assertFails(bobDb.collection("sounding_history").doc("entry_1").delete());

  // Alice can delete her own record
  await assertSucceeds(aliceDb.collection("sounding_history").doc("entry_1").delete());

  // Spoofed userId on create is rejected
  await assertFails(
    bobDb.collection("sounding_history").doc("entry_2").set(validHistoryEntry("alice"))
  );

  // Wrong plantUnit is rejected
  await assertFails(
    bobDb
      .collection("sounding_history")
      .doc("entry_3")
      .set(validHistoryEntry("bob", { plantUnit: "OTHER-UNIT" }))
  );
});

test("shift_reports: create, cross-shift monitor list, update, and field protection", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  const bobDb = testEnv.authenticatedContext("bob").firestore();

  // Alice creates Shift Pagi report
  await assertSucceeds(
    aliceDb
      .collection("shift_reports")
      .doc("2026-11-26_PAGI")
      .set(validShiftReport("alice"))
  );

  // Bob monitors reports across shifts with plantUnit filter
  await assertSucceeds(
    bobDb
      .collection("shift_reports")
      .where("plantUnit", "==", PLANT_UNIT)
      .get()
  );

  // Unfiltered list must fail
  await assertFails(bobDb.collection("shift_reports").get());

  // Bob updates Shift Pagi report (e.g. handover revision) keeping dateKey & shift immutable
  await assertSucceeds(
    bobDb
      .collection("shift_reports")
      .doc("2026-11-26_PAGI")
      .set(
        validShiftReport("bob", {
          nbNote: "Diserahkan ke shift sore",
          totalAkhirKg: 130000,
        })
      )
  );

  // Reject mutating shift or dateKey on existing shift_report
  await assertFails(
    bobDb
      .collection("shift_reports")
      .doc("2026-11-26_PAGI")
      .set(validShiftReport("bob", { shift: "Sore" }))
  );

  // Reject delete on shift_reports
  await assertFails(aliceDb.collection("shift_reports").doc("2026-11-26_PAGI").delete());
});
