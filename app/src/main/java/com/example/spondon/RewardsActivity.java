package com.example.spondon;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Civic Rewards (server-enforced).
 *
 * Nothing the app computes is trusted. The Firestore rules (see
 * firestore.rules) are what keep the balance honest:
 *
 *   civic_wallets/{uid}                         authoritative wallet
 *       uid, balance, totalEarned, totalSpent, lastEventType, lastEventId, updatedAt
 *   civic_wallets/{uid}/credits/{creditId}      one doc per point award (create-only)
 *       uid, kind, sourceId, points, createdAt
 *       creditId is deterministic ("welcome", "report_<docId>", ...), so a source
 *       can only ever be credited once. The rules check that the source document
 *       (reports / blood_donors / job_applications / community_posts) exists and
 *       belongs to this user, and that the points match the kind.
 *   civic_wallets/{uid}/redemptions/{id}        append-only redemption history
 *       uid, perkId, perkName, cost, couponCode, balanceBefore, balanceAfter, redeemedAt
 *
 * Every wallet change is ONE atomic transaction (wallet + its credit/redemption
 * doc). The rules reject it unless
 *   credit : balance == oldBalance + credit.points
 *   redeem : oldBalance >= cost, balance == oldBalance - cost, and
 *            (perkId, cost) is exactly (library_pass,100) or (kda_parking,150).
 */
public class RewardsActivity extends AppCompatActivity {

    private static final String TAG = "RewardsActivity";

    private static final String WALLETS = "civic_wallets";
    private static final String CREDITS = "credits";
    private static final String REDEMPTIONS = "redemptions";

    // Must match validPerk() in firestore.rules.
    private static final String PERK1_ID = "library_pass";
    private static final String PERK2_ID = "kda_parking";
    private static final int PERK1_COST = 100;
    private static final int PERK2_COST = 150;

    // Point values per credit kind. Must match validCredit() in firestore.rules.
    private static final int PTS_WELCOME = 50;
    private static final int PTS_REPORT = 50;
    private static final int PTS_BLOOD = 100;
    private static final int PTS_JOB = 30;
    private static final int PTS_POST = 20;

    private TextView tvCivicLevelTitle, tvTotalPoints, tvPointsBreakdown;
    private TextView tvNoHistory;
    private LinearLayout layoutBadgesContainer, layoutHistoryContainer;
    private Button btnRedeemPerk1, btnRedeemPerk2, btnBackRewards;

    private FirebaseFirestore firestore;
    private String uid;
    private DocumentReference walletRef;

    // Wallet state, straight from Firestore.
    private long walletBalance = 0;
    private long totalEarned = 0;
    private long totalSpent = 0;

    // Credit counts by kind, derived from the credit documents in Firestore.
    private boolean welcomeCredited = false;
    private int reportsCount, bloodCount, jobsCount, postsCount;

    private boolean walletLoaded = false;
    private boolean creditsLoaded = false;
    private boolean syncFinished = false;
    private boolean redeeming = false;

    /**
     * Unfinished redemption attempts, perkId -> {redemptionId, couponCode}.
     * If an attempt ends with an unknown outcome (connection dropped before the
     * reply) the retry re-uses the same id, so it can never be charged twice.
     */
    private final Map<String, String[]> pendingAttempts = new HashMap<>();

    private ListenerRegistration walletListener;
    private ListenerRegistration creditsListener;
    private ListenerRegistration historyListener;

    private static class RedeemResult {
        boolean duplicate;
        boolean insufficient;
        long newBalance;
        String couponCode;
    }

    private static class CreditSpec {
        final String id, kind, sourceId;
        final int points;

        CreditSpec(String id, String kind, String sourceId, int points) {
            this.id = id;
            this.kind = kind;
            this.sourceId = sourceId;
            this.points = points;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rewards);

        firestore = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user == null ? null : user.getUid();

        tvCivicLevelTitle = findViewById(R.id.tvCivicLevelTitle);
        tvTotalPoints = findViewById(R.id.tvTotalPoints);
        tvPointsBreakdown = findViewById(R.id.tvPointsBreakdown);
        layoutBadgesContainer = findViewById(R.id.layoutBadgesContainer);
        layoutHistoryContainer = findViewById(R.id.layoutHistoryContainer);
        tvNoHistory = findViewById(R.id.tvNoHistory);

        btnRedeemPerk1 = findViewById(R.id.btnRedeemPerk1);
        btnRedeemPerk2 = findViewById(R.id.btnRedeemPerk2);
        btnBackRewards = findViewById(R.id.btnBackRewards);

        btnBackRewards.setOnClickListener(v -> finish());

        btnRedeemPerk1.setOnClickListener(v -> redeemPerk(PERK1_ID, "Library 1-Year Pass", PERK1_COST));
        btnRedeemPerk2.setOnClickListener(v -> redeemPerk(PERK2_ID, "KDA Parking 50% Off Voucher", PERK2_COST));

        renderRewards();

        if (uid == null) {
            Toast.makeText(this, "Please log in to view and redeem your points.", Toast.LENGTH_LONG).show();
            return;
        }

        walletRef = firestore.collection(WALLETS).document(uid);

        listenToWallet();
        listenToCredits();
        listenToHistory();
        syncCredits();
    }

    @Override
    protected void onDestroy() {
        if (walletListener != null) walletListener.remove();
        if (creditsListener != null) creditsListener.remove();
        if (historyListener != null) historyListener.remove();
        super.onDestroy();
    }

    // ------------------------------------------------------------------
    // Live data from Firestore
    // ------------------------------------------------------------------

    private void listenToWallet() {
        walletListener = walletRef.addSnapshotListener((snap, e) -> {
            if (e != null) {
                Log.w(TAG, "Wallet listener error", e);
                return;
            }
            walletBalance = nz(snap == null ? null : snap.getLong("balance"));
            totalEarned = nz(snap == null ? null : snap.getLong("totalEarned"));
            totalSpent = nz(snap == null ? null : snap.getLong("totalSpent"));
            walletLoaded = true;
            renderRewards();
        });
    }

    private void listenToCredits() {
        creditsListener = walletRef.collection(CREDITS).addSnapshotListener((snap, e) -> {
            if (e != null) {
                Log.w(TAG, "Credits listener error", e);
                return;
            }
            if (snap == null) return;
            welcomeCredited = false;
            reportsCount = bloodCount = jobsCount = postsCount = 0;
            for (DocumentSnapshot d : snap.getDocuments()) {
                String kind = d.getString("kind");
                if (kind == null) continue;
                switch (kind) {
                    case "welcome": welcomeCredited = true; break;
                    case "report": reportsCount++; break;
                    case "blood": bloodCount++; break;
                    case "job": jobsCount++; break;
                    case "post": postsCount++; break;
                    default: break;
                }
            }
            creditsLoaded = true;
            renderRewards();
        });
    }

    private void listenToHistory() {
        historyListener = walletRef.collection(REDEMPTIONS)
                .orderBy("redeemedAt", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.w(TAG, "History listener error", e);
                        return;
                    }
                    if (snap == null) return;
                    renderHistory(snap.getDocuments());
                });
    }

    private static long nz(@Nullable Long v) {
        return v == null ? 0 : v;
    }

    // ------------------------------------------------------------------
    // Earning: turn the user's real Firestore activity into verified credits
    // ------------------------------------------------------------------

    /**
     * Finds the user's source documents that have not been credited yet and
     * credits them one at a time (each credit is its own atomic transaction,
     * which is what the rules verify). Safe to run repeatedly: a credit that
     * already exists is skipped, and the rules refuse a second one anyway.
     */
    private void syncCredits() {
        final Task<QuerySnapshot> tCredits = walletRef.collection(CREDITS).get();
        final Task<QuerySnapshot> tReports = firestore.collection("reports")
                .whereEqualTo("uid", uid).get();
        final Task<QuerySnapshot> tBlood = firestore.collection("blood_donors")
                .whereEqualTo("uid", uid).get();
        final Task<QuerySnapshot> tJobs = firestore.collection("job_applications")
                .whereEqualTo("applicantUid", uid).get();
        final Task<QuerySnapshot> tPosts = firestore.collection("community_posts")
                .whereEqualTo("authorUid", uid).get();

        Tasks.whenAllSuccess(tCredits, tReports, tBlood, tJobs, tPosts)
                .addOnSuccessListener(ignored -> {
                    Set<String> existing = new HashSet<>();
                    for (DocumentSnapshot d : tCredits.getResult().getDocuments()) {
                        existing.add(d.getId());
                    }

                    Deque<CreditSpec> queue = new ArrayDeque<>();
                    if (!existing.contains("welcome")) {
                        queue.add(new CreditSpec("welcome", "welcome", "welcome", PTS_WELCOME));
                    }
                    enqueue(queue, existing, tReports.getResult(), "report", PTS_REPORT);
                    enqueue(queue, existing, tBlood.getResult(), "blood", PTS_BLOOD);
                    enqueue(queue, existing, tJobs.getResult(), "job", PTS_JOB);
                    enqueue(queue, existing, tPosts.getResult(), "post", PTS_POST);

                    processNextCredit(queue);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Credit sync could not load data", e);
                    Toast.makeText(this, "Could not update your points: " + describeError(e),
                            Toast.LENGTH_LONG).show();
                    finishSync();
                });
    }

    private void enqueue(Deque<CreditSpec> queue, Set<String> existing, QuerySnapshot src,
                         String kind, int points) {
        for (DocumentSnapshot d : src.getDocuments()) {
            String creditId = kind + "_" + d.getId();
            if (!existing.contains(creditId)) {
                queue.add(new CreditSpec(creditId, kind, d.getId(), points));
            }
        }
    }

    private void processNextCredit(Deque<CreditSpec> queue) {
        if (isFinishing() || isDestroyed()) return;

        final CreditSpec spec = queue.poll();
        if (spec == null) {
            finishSync();
            return;
        }

        final DocumentReference creditRef = walletRef.collection(CREDITS).document(spec.id);

        firestore.runTransaction((Transaction.Function<Boolean>) tx -> {
            DocumentSnapshot credit = tx.get(creditRef);
            DocumentSnapshot wallet = tx.get(walletRef);
            if (credit.exists()) return false; // already credited, nothing to do

            long balance = nz(wallet.getLong("balance"));
            long earned = nz(wallet.getLong("totalEarned"));
            long spent = nz(wallet.getLong("totalSpent"));

            Map<String, Object> creditData = new HashMap<>();
            creditData.put("uid", uid);
            creditData.put("kind", spec.kind);
            creditData.put("sourceId", spec.sourceId);
            creditData.put("points", (long) spec.points);
            creditData.put("createdAt", FieldValue.serverTimestamp());
            tx.set(creditRef, creditData);

            tx.set(walletRef, walletMap(balance + spec.points, earned + spec.points, spent,
                    "credit", spec.id));
            return true;
        }).addOnSuccessListener(done -> processNextCredit(queue))
          .addOnFailureListener(e -> {
              Log.w(TAG, "Credit " + spec.id + " failed", e);
              Toast.makeText(this, "Some points could not be added yet: " + describeError(e),
                      Toast.LENGTH_LONG).show();
              finishSync();
          });
    }

    private void finishSync() {
        syncFinished = true;
        renderRewards();
    }

    private Map<String, Object> walletMap(long balance, long earned, long spent,
                                          String eventType, String eventId) {
        Map<String, Object> w = new HashMap<>();
        w.put("uid", uid);
        w.put("balance", balance);
        w.put("totalEarned", earned);
        w.put("totalSpent", spent);
        w.put("lastEventType", eventType);
        w.put("lastEventId", eventId);
        w.put("updatedAt", FieldValue.serverTimestamp());
        return w;
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    private boolean pointsReady() {
        return uid != null && walletLoaded && creditsLoaded && syncFinished;
    }

    private void renderRewards() {
        tvTotalPoints.setText(pointsReady() ? walletBalance + " PTS" : "… PTS");

        StringBuilder sb = new StringBuilder();
        if (welcomeCredited) sb.append("• Base Citizen Bonus: +").append(PTS_WELCOME).append(" pts\n");
        if (reportsCount > 0) sb.append("• Reported Civic Issues (").append(reportsCount).append("): +").append(reportsCount * PTS_REPORT).append(" pts\n");
        if (bloodCount > 0) sb.append("• Blood Donor Registration: +").append(bloodCount * PTS_BLOOD).append(" pts\n");
        if (jobsCount > 0) sb.append("• Job Applications (").append(jobsCount).append("): +").append(jobsCount * PTS_JOB).append(" pts\n");
        if (postsCount > 0) sb.append("• Community Discussions (").append(postsCount).append("): +").append(postsCount * PTS_POST).append(" pts\n");
        if (totalSpent > 0) sb.append("• Redeemed Vouchers: −").append(totalSpent).append(" pts\n");
        tvPointsBreakdown.setText(sb.length() == 0 ? "Calculating participation activity..." : sb.toString().trim());

        // Level and badges follow lifetime points, so spending points never lowers them.
        if (totalEarned >= 500) {
            tvCivicLevelTitle.setText("LEVEL 4: DISTINGUISHED SUPER CITIZEN 🏆");
        } else if (totalEarned >= 250) {
            tvCivicLevelTitle.setText("LEVEL 3: COMMUNITY GUARDIAN ⭐");
        } else if (totalEarned >= 100) {
            tvCivicLevelTitle.setText("LEVEL 2: ACTIVE CITY HELPER 🛡️");
        } else {
            tvCivicLevelTitle.setText("LEVEL 1: CIVIC EXPLORER 🌱");
        }

        renderBadges(reportsCount, bloodCount, jobsCount, postsCount);

        boolean canTap = pointsReady() && !redeeming;
        btnRedeemPerk1.setEnabled(canTap);
        btnRedeemPerk2.setEnabled(canTap);
    }

    private void renderHistory(List<DocumentSnapshot> docs) {
        layoutHistoryContainer.removeAllViews();
        tvNoHistory.setVisibility(docs.isEmpty() ? View.VISIBLE : View.GONE);

        SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        for (DocumentSnapshot d : docs) {
            String perk = d.getString("perkName");
            Long cost = d.getLong("cost");
            String coupon = d.getString("couponCode");
            Timestamp ts = d.getTimestamp("redeemedAt",
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);

            TextView tv = new TextView(this);
            tv.setText((perk == null ? "Voucher" : perk) + "  −" + (cost == null ? 0 : cost) + " pts\n"
                    + "Code " + (coupon == null ? "-" : coupon)
                    + (ts == null ? "" : "  •  " + fmt.format(ts.toDate())));
            tv.setTextSize(12);
            tv.setTextColor(Color.parseColor("#334155"));
            tv.setPadding(dp(12), dp(8), dp(12), dp(8));

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(dp(8));
            bg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
            tv.setBackground(bg);

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, 0, 0, dp(8));
            tv.setLayoutParams(p);
            layoutHistoryContainer.addView(tv);
        }
    }

    private void renderBadges(int reports, int blood, int jobs, int posts) {
        layoutBadgesContainer.removeAllViews();

        addBadgeCard("🛡️ Civic Sentinel", "Reported civic infrastructure problems to city municipal authorities.", reports > 0);
        addBadgeCard("🩸 Life Saver", "Voluntarily registered in the emergency municipal blood donation directory.", blood > 0);
        addBadgeCard("💼 Career Seeker", "Actively applied for municipal & regional career opportunities.", jobs > 0);
        addBadgeCard("💬 Community Voice", "Participated in public civic announcements and discussions.", posts > 0);
        addBadgeCard("🌳 Eco Guardian", "Committed to Clean Khulna green city guidelines and zero plastic waste.", true);
        addBadgeCard("⭐ Super Citizen", "Achieved over 500 cumulative civic participation points.", totalEarned >= 500);
    }

    private void addBadgeCard(String name, String desc, boolean isUnlocked) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isUnlocked ? Color.WHITE : Color.parseColor("#F1F5F9"));
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), isUnlocked ? Color.parseColor("#FCD34D") : Color.parseColor("#E2E8F0"));
        card.setBackground(bg);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(p);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(15);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(isUnlocked ? Color.parseColor("#1E293B") : Color.parseColor("#94A3B8"));
        textLayout.addView(tvName);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(desc);
        tvDesc.setTextSize(12);
        tvDesc.setTextColor(isUnlocked ? Color.parseColor("#64748B") : Color.parseColor("#94A3B8"));
        textLayout.addView(tvDesc);

        card.addView(textLayout);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(isUnlocked ? "UNLOCKED" : "LOCKED");
        tvStatus.setTextSize(11);
        tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setPadding(dp(8), dp(4), dp(8), dp(4));

        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setCornerRadius(dp(4));
        statusBg.setColor(isUnlocked ? Color.parseColor("#FEF3C7") : Color.parseColor("#E2E8F0"));
        tvStatus.setBackground(statusBg);
        tvStatus.setTextColor(isUnlocked ? Color.parseColor("#92400E") : Color.parseColor("#64748B"));

        card.addView(tvStatus);
        layoutBadgesContainer.addView(card);
    }

    // ------------------------------------------------------------------
    // Redemption
    // ------------------------------------------------------------------

    private void redeemPerk(String perkId, String perkName, int cost) {
        if (redeeming) return; // one redemption at a time

        if (uid == null) {
            Toast.makeText(this, "Please log in to redeem vouchers.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pointsReady()) {
            Toast.makeText(this, "Your points are still loading. Please try again in a moment.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Friendly early exit only. The real check is in the transaction and in the rules.
        if (walletBalance < cost) {
            Toast.makeText(this, "Need " + cost + " points to redeem this perk. You have "
                    + walletBalance + " pts.", Toast.LENGTH_LONG).show();
            return;
        }

        redeeming = true;
        renderRewards(); // disables both redeem buttons

        // Re-use the id of an unfinished attempt for this perk (see pendingAttempts).
        String[] attempt = pendingAttempts.get(perkId);
        if (attempt == null) {
            attempt = new String[]{UUID.randomUUID().toString(),
                    "SPON-" + (1000 + new Random().nextInt(9000))};
            pendingAttempts.put(perkId, attempt);
        }
        final String redemptionId = attempt[0];
        final String newCoupon = attempt[1];

        final DocumentReference historyRef = walletRef.collection(REDEMPTIONS).document(redemptionId);

        firestore.runTransaction((Transaction.Function<RedeemResult>) tx -> {
            // All reads first, then all writes.
            DocumentSnapshot existing = tx.get(historyRef);
            DocumentSnapshot wallet = tx.get(walletRef);

            long balance = nz(wallet.getLong("balance"));
            long earned = nz(wallet.getLong("totalEarned"));
            long spent = nz(wallet.getLong("totalSpent"));

            RedeemResult r = new RedeemResult();

            // 1) Already processed -> never charge again.
            if (existing.exists()) {
                r.duplicate = true;
                r.newBalance = balance;
                r.couponCode = existing.getString("couponCode");
                return r;
            }

            // 2) Sufficient balance (the rules enforce this again on the server).
            if (balance < cost) {
                r.insufficient = true;
                r.newBalance = balance;
                return r;
            }

            // 3) Deduct exactly `cost`; wallet + history are written atomically.
            long newBalance = balance - cost;

            tx.set(walletRef, walletMap(newBalance, earned, spent + cost, "redeem", redemptionId));

            Map<String, Object> history = new HashMap<>();
            history.put("uid", uid);
            history.put("perkId", perkId);
            history.put("perkName", perkName);
            history.put("cost", (long) cost);
            history.put("couponCode", newCoupon);
            history.put("balanceBefore", balance);
            history.put("balanceAfter", newBalance);
            history.put("redeemedAt", FieldValue.serverTimestamp());
            tx.set(historyRef, history);

            r.newBalance = newBalance;
            r.couponCode = newCoupon;
            return r;
        }).addOnSuccessListener(r -> {
            redeeming = false;
            pendingAttempts.remove(perkId); // definite answer received
            renderRewards();

            if (r.insufficient) {
                Toast.makeText(this, "Need " + cost + " points to redeem this perk. You have "
                        + r.newBalance + " pts.", Toast.LENGTH_LONG).show();
            } else {
                showRedeemedDialog(perkName, r.couponCode, r.newBalance, cost, r.duplicate);
            }
        }).addOnFailureListener(e -> {
            // Outcome unknown: keep the attempt id so a retry cannot double-charge.
            redeeming = false;
            renderRewards();
            Log.w(TAG, "Redemption failed", e);
            Toast.makeText(this, "Redemption could not be completed: " + describeError(e)
                    + " Check your balance, then try again.", Toast.LENGTH_LONG).show();
        });
    }

    private void showRedeemedDialog(String perkName, @Nullable String coupon, long newBalance,
                                    int cost, boolean wasAlreadyProcessed) {
        if (isFinishing()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Voucher Redeemed! [DEMO]");
        builder.setMessage((wasAlreadyProcessed
                ? "This redemption was already completed, so no extra points were taken.\n\n"
                : "Congratulations!\n\n")
                + "Voucher: " + perkName
                + "\nCoupon Code: " + (coupon == null ? "-" : coupon)
                + "\n\n" + cost + " Civic Points " + (wasAlreadyProcessed ? "were deducted." : "deducted.")
                + "\nNew balance: " + newBalance + " pts"
                + "\n\nShow this code at the municipal desk to claim your civic reward.");
        builder.setPositiveButton("Awesome", null);
        builder.show();
    }

    private static String describeError(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) e).getCode();
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return "no internet connection.";
            }
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "rejected by the server (balance or rules check failed).";
            }
        }
        return e.getMessage() == null ? "unknown error." : e.getMessage();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
