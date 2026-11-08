// Run from this folder:  npm install && npm test
// (needs the Firebase CLI: npm i -g firebase-tools, and Java for the emulator)
const test = require('node:test');
const fs = require('node:fs');
const path = require('node:path');
const {
  initializeTestEnvironment, assertSucceeds, assertFails,
} = require('@firebase/rules-unit-testing');
const { doc, setDoc, writeBatch, serverTimestamp } = require('firebase/firestore');

let env;
const UID = 'alice';
const OTHER = 'bob';

test.before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'spondon-test',
    firestore: { rules: fs.readFileSync(path.join(__dirname, '..', 'firestore.rules'), 'utf8') },
  });
});
test.after(async () => env.cleanup());
test.beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'reports/r1'), { uid: UID, title: 'mine' });
    await setDoc(doc(db, 'reports/r2'), { uid: OTHER, title: 'not mine' });
  });
});

const alice = () => env.authenticatedContext(UID).firestore();

const wallet = (balance, earned, spent, type, id) => ({
  uid: UID, balance, totalEarned: earned, totalSpent: spent,
  lastEventType: type, lastEventId: id, updatedAt: serverTimestamp(),
});
const credit = (kind, sourceId, points) => ({
  uid: UID, kind, sourceId, points, createdAt: serverTimestamp(),
});
const redemption = (perkId, cost, before, after) => ({
  uid: UID, perkId, perkName: perkId, cost, couponCode: 'SPON-1234',
  balanceBefore: before, balanceAfter: after, redeemedAt: serverTimestamp(),
});

// Seeds a wallet that already holds `balance` points (bypasses rules).
async function seedWallet(balance) {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), `civic_wallets/${UID}`), {
      uid: UID, balance, totalEarned: balance, totalSpent: 0,
      lastEventType: 'credit', lastEventId: 'welcome', updatedAt: new Date(),
    });
    await setDoc(doc(ctx.firestore(), `civic_wallets/${UID}/credits/welcome`),
      { uid: UID, kind: 'welcome', sourceId: 'welcome', points: balance, createdAt: new Date() });
  });
}

async function batchOf(db, ops) {
  const b = writeBatch(db);
  ops(b);
  return b.commit();
}

// ---------------- earning ----------------
test('welcome credit + wallet in one batch is accepted', async () => {
  const db = alice();
  await assertSucceeds(batchOf(db, (b) => {
    b.set(doc(db, `civic_wallets/${UID}/credits/welcome`), credit('welcome', 'welcome', 50));
    b.set(doc(db, `civic_wallets/${UID}`), wallet(50, 50, 0, 'credit', 'welcome'));
  }));
});

test('credit with inflated points is rejected', async () => {
  const db = alice();
  await assertFails(batchOf(db, (b) => {
    b.set(doc(db, `civic_wallets/${UID}/credits/welcome`), credit('welcome', 'welcome', 5000));
    b.set(doc(db, `civic_wallets/${UID}`), wallet(5000, 5000, 0, 'credit', 'welcome'));
  }));
});

test('credit for own report is accepted, for someone else\'s report rejected', async () => {
  const db = alice();
  await assertSucceeds(batchOf(db, (b) => {
    b.set(doc(db, `civic_wallets/${UID}/credits/report_r1`), credit('report', 'r1', 50));
    b.set(doc(db, `civic_wallets/${UID}`), wallet(50, 50, 0, 'credit', 'report_r1'));
  }));
  await assertFails(batchOf(db, (b) => {
    b.set(doc(db, `civic_wallets/${UID}/credits/report_r2`), credit('report', 'r2', 50));
    b.set(doc(db, `civic_wallets/${UID}`), wallet(100, 100, 0, 'credit', 'report_r2'));
  }));
});

test('same source cannot be credited twice', async () => {
  const db = alice();
  await seedWallet(50);
  await assertFails(batchOf(db, (b) => {
    b.set(doc(db, `civic_wallets/${UID}/credits/welcome`), credit('welcome', 'welcome', 50));
    b.set(doc(db, `civic_wallets/${UID}`), wallet(100, 100, 0, 'credit', 'welcome'));
  }));
});

test('balance cannot be edited directly', async () => {
  const db = alice();
  await seedWallet(50);
  await assertFails(setDoc(doc(db, `civic_wallets/${UID}`), wallet(9999, 9999, 0, 'credit', 'welcome')));
});

// ---------------- redeeming ----------------
test('library_pass deducts exactly 100', async () => {
  const db = alice();
  await seedWallet(150);
  await assertSucceeds(batchOf(db, (b) => {
    b.set(doc(db, `civic_wallets/${UID}/redemptions/red1`), redemption('library_pass', 100, 150, 50));
    b.set(doc(db, `civic_wallets/${UID}`), wallet(50, 150, 100, 'redeem', 'red1'));
  }));
});

test('redeeming without enough balance is rejected', async () => {
  const db = alice();
  await seedWallet(80);
  await assertFails(batchOf(db, (b) => {
    b.set(doc(db, `civic_wallets/${UID}/redemptions/red1`), redemption('library_pass', 100, 80, -20));
    b.set(doc(db, `civic_wallets/${UID}`), wallet(-20, 80, 100, 'redeem', 'red1'));
  }));
});

test('wrong price for a voucher is rejected', async () => {
  const db = alice();
  await seedWallet(500);
  await assertFails(batchOf(db, (b) => {          // library pass for 10 points
    b.set(doc(db, `civic_wallets/${UID}/redemptions/red1`), redemption('library_pass', 10, 500, 490));
    b.set(doc(db, `civic_wallets/${UID}`), wallet(490, 500, 10, 'redeem', 'red1'));
  }));
  await assertFails(batchOf(db, (b) => {          // deduct 100 but record cost 150? mismatch
    b.set(doc(db, `civic_wallets/${UID}/redemptions/red2`), redemption('kda_parking', 150, 500, 350));
    b.set(doc(db, `civic_wallets/${UID}`), wallet(400, 500, 100, 'redeem', 'red2'));
  }));
});

test('two redemptions in one request are rejected', async () => {
  const db = alice();
  await seedWallet(500);
  await assertFails(batchOf(db, (b) => {
    b.set(doc(db, `civic_wallets/${UID}/redemptions/a`), redemption('library_pass', 100, 500, 400));
    b.set(doc(db, `civic_wallets/${UID}/redemptions/b`), redemption('library_pass', 100, 500, 400));
    b.set(doc(db, `civic_wallets/${UID}`), wallet(400, 500, 100, 'redeem', 'b'));
  }));
});

test('replaying a redemption id is rejected', async () => {
  const db = alice();
  await seedWallet(500);
  await assertSucceeds(batchOf(db, (b) => {
    b.set(doc(db, `civic_wallets/${UID}/redemptions/red1`), redemption('library_pass', 100, 500, 400));
    b.set(doc(db, `civic_wallets/${UID}`), wallet(400, 500, 100, 'redeem', 'red1'));
  }));
  await assertFails(batchOf(db, (b) => {
    b.set(doc(db, `civic_wallets/${UID}`), wallet(300, 500, 200, 'redeem', 'red1'));
  }));
});

test('history is append-only and other users cannot read it', async () => {
  const db = alice();
  await seedWallet(500);
  await assertSucceeds(batchOf(db, (b) => {
    b.set(doc(db, `civic_wallets/${UID}/redemptions/red1`), redemption('library_pass', 100, 500, 400));
    b.set(doc(db, `civic_wallets/${UID}`), wallet(400, 500, 100, 'redeem', 'red1'));
  }));
  await assertFails(setDoc(doc(db, `civic_wallets/${UID}/redemptions/red1`),
    redemption('library_pass', 1, 500, 499)));
  const bob = env.authenticatedContext(OTHER).firestore();
  await assertFails(require('firebase/firestore').getDoc(doc(bob, `civic_wallets/${UID}`)));
});
