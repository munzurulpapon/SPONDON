// Rules tests for Admin -> Publish Job and Admin -> Monitor Citizen Service Requests.
// Run from this folder:  npm install && npm run test:admin
// (needs the Firebase CLI: npm i -g firebase-tools, and Java for the emulator)
const test = require('node:test');
const fs = require('node:fs');
const path = require('node:path');
const {
  initializeTestEnvironment, assertSucceeds, assertFails,
} = require('@firebase/rules-unit-testing');
const {
  doc, setDoc, getDoc, getDocs, collection, updateDoc, deleteDoc, serverTimestamp,
} = require('firebase/firestore');

let env;
const ADMIN = 'admin1';
const CITIZEN = 'citizen1';

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
    // The app logs admins in only when users/{uid}.role is exactly "Admin".
    await setDoc(doc(db, `users/${ADMIN}`), { role: 'Admin', email: 'admin@spondon.bd' });
    await setDoc(doc(db, `users/${CITIZEN}`), { role: 'Citizen', email: 'c@spondon.bd' });
    await setDoc(doc(db, 'service_requests/sr1'), {
      serviceName: 'Electrician', citizenUid: CITIZEN, status: 'Pending', createdAt: new Date(),
    });
    await setDoc(doc(db, 'jobs/j1'), { title: 'Seed', company: 'KCC', location: 'Khulna' });
  });
});

const asAdmin = () => env.authenticatedContext(ADMIN).firestore();
const asCitizen = () => env.authenticatedContext(CITIZEN).firestore();
const signedOut = () => env.unauthenticatedContext().firestore();

// Exactly the document ManageJobsActivity.publishNewJob() builds.
const job = (over = {}) => ({
  title: 'Traffic Systems Engineer', company: 'KCC', category: 'IT & Tech', jobType: 'Full-Time',
  salary: 'Negotiable', location: 'Khulna', deadline: 'Rolling', description: '', requirements: '',
  createdAt: serverTimestamp(), status: 'Open', postedBy: ADMIN, ...over,
});

// ---------------- Publish Job ----------------
test('admin can publish a job (the exact payload the app sends)', async () => {
  await assertSucceeds(setDoc(doc(asAdmin(), 'jobs/new1'), job()));
});

test('publishing the same posting twice rewrites it (retry is safe)', async () => {
  const db = asAdmin();
  await assertSucceeds(setDoc(doc(db, 'jobs/new1'), job()));
  await assertSucceeds(setDoc(doc(db, 'jobs/new1'), job()));
});

test('citizen cannot publish, edit or delete jobs', async () => {
  const db = asCitizen();
  await assertFails(setDoc(doc(db, 'jobs/hack'), job()));
  await assertFails(updateDoc(doc(db, 'jobs/j1'), { title: 'Hijacked' }));
  await assertFails(deleteDoc(doc(db, 'jobs/j1')));
});

test('signed-out user cannot publish a job', async () => {
  await assertFails(setDoc(doc(signedOut(), 'jobs/hack'), job()));
});

test('job without title/company/location is rejected', async () => {
  const db = asAdmin();
  await assertFails(setDoc(doc(db, 'jobs/bad1'), job({ title: '' })));
  await assertFails(setDoc(doc(db, 'jobs/bad2'), job({ company: '' })));
  await assertFails(setDoc(doc(db, 'jobs/bad3'), job({ location: 5 })));
});

test('any signed-in user can read jobs (Jobs Opportunities), signed-out cannot', async () => {
  await assertSucceeds(getDoc(doc(asCitizen(), 'jobs/j1')));
  await assertSucceeds(getDocs(collection(asCitizen(), 'jobs')));
  await assertFails(getDocs(collection(signedOut(), 'jobs')));
});

// ---------------- Monitor Citizen Service Requests ----------------
test('admin can list ALL service requests (unfiltered query)', async () => {
  await assertSucceeds(getDocs(collection(asAdmin(), 'service_requests')));
});

test('admin can override a request status', async () => {
  await assertSucceeds(updateDoc(doc(asAdmin(), 'service_requests/sr1'),
    { status: 'In Progress', updatedAt: serverTimestamp() }));
});

test('signed-out user cannot read service requests', async () => {
  await assertFails(getDocs(collection(signedOut(), 'service_requests')));
});

test('admin can still read job applications (Job Management candidate list)', async () => {
  await assertSucceeds(getDocs(collection(asAdmin(), 'job_applications')));
});
