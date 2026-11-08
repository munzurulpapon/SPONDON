package com.example.spondon;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Citizen Community.
 *
 * Everything is stored in Cloud Firestore so that all users see the same data:
 *
 *   community_posts/{postId}
 *       title, category, author, authorUid, content, createdAt,
 *       upvotes (number), voterIds (array of uids), isDemo
 *   community_posts/{postId}/comments/{commentId}
 *       author, authorUid, text, createdAt
 *
 * - Posts and comments are shown through snapshot listeners, so a new comment
 *   or upvote appears immediately on every device that has the screen open.
 * - One upvote per user per post is enforced with a Firestore transaction that
 *   checks the post's voterIds array and adds the uid + increments the counter
 *   atomically. The vote lives in Firebase, so it survives app restarts and
 *   re-installs (as long as the user logs in with the same account).
 */
public class CommunityActivity extends AppCompatActivity {

    private static final String TAG = "CommunityActivity";
    private static final String POSTS = "community_posts";
    private static final String COMMENTS = "comments";

    private static final int VOTE_OK = 0;
    private static final int VOTE_ALREADY = 1;
    private static final int VOTE_POST_MISSING = 2;

    private EditText etSearchCommunity;
    private TextView tvCommunityCount, tvNoCommunity;
    private LinearLayout layoutCommunityContainer;
    private Button btnNewPost, btnBackCommunity;

    private Button btnComAll, btnComAnnounce, btnComDiscuss, btnComInitiative;

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    private ListenerRegistration postsListener;
    private ListenerRegistration commentsListener;
    private AlertDialog commentsDialog;

    private final List<PostItem> postList = new ArrayList<>();
    /** Post ids with a vote currently being written (blocks double taps). */
    private final Set<String> votesInFlight = new HashSet<>();

    private String selectedCategory = "All";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        etSearchCommunity = findViewById(R.id.etSearchCommunity);
        tvCommunityCount = findViewById(R.id.tvCommunityCount);
        tvNoCommunity = findViewById(R.id.tvNoCommunity);
        layoutCommunityContainer = findViewById(R.id.layoutCommunityContainer);
        btnNewPost = findViewById(R.id.btnNewPost);
        btnBackCommunity = findViewById(R.id.btnBackCommunity);

        btnComAll = findViewById(R.id.btnComAll);
        btnComAnnounce = findViewById(R.id.btnComAnnounce);
        btnComDiscuss = findViewById(R.id.btnComDiscuss);
        btnComInitiative = findViewById(R.id.btnComInitiative);

        btnBackCommunity.setOnClickListener(v -> finish());
        btnNewPost.setOnClickListener(v -> showCreatePostDialog());

        setupFilters();
        setupSearch();
        seedPostsIfEmpty();
        startPostsListener();
    }

    @Override
    protected void onDestroy() {
        if (postsListener != null) {
            postsListener.remove();
            postsListener = null;
        }
        removeCommentsListener();
        if (commentsDialog != null && commentsDialog.isShowing()) {
            commentsDialog.dismiss();
        }
        commentsDialog = null;
        super.onDestroy();
    }

    // ------------------------------------------------------------------
    // Filters / search
    // ------------------------------------------------------------------

    private void setupFilters() {
        btnComAll.setOnClickListener(v -> setCategory("All"));
        btnComAnnounce.setOnClickListener(v -> setCategory("Announcements"));
        btnComDiscuss.setOnClickListener(v -> setCategory("Discussions"));
        btnComInitiative.setOnClickListener(v -> setCategory("Civic Initiatives"));
    }

    private void setCategory(String cat) {
        selectedCategory = cat;
        displayPosts();
    }

    private void setupSearch() {
        etSearchCommunity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                displayPosts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // ------------------------------------------------------------------
    // Firestore: posts
    // ------------------------------------------------------------------

    /**
     * First run only: put the four demo posts into Firestore so the screen is
     * not empty. Ids are fixed (seed_1..seed_4) and written inside a
     * transaction that only creates a missing document, so several devices
     * opening the screen at the same time can never duplicate or overwrite
     * the posts (or their votes).
     */
    private void seedPostsIfEmpty() {
        if (firebaseAuth.getCurrentUser() == null) return;

        firestore.collection(POSTS).limit(1).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) return;

                    long now = System.currentTimeMillis();

                    seedPost("seed_1",
                            "Municipal Autumn Tree Plantation Campaign: 10,000 Saplings in KCC Wards",
                            "Announcements",
                            "KCC Environmental Directorate",
                            "Khulna City Corporation has commenced its annual green initiative to plant 10,000 fruit and timber saplings along Rupsha riverbanks, Sonadanga bypass, and public school grounds. Free seedlings are available for collection at Hadis Park nursery counter.",
                            42, now - 4 * 60000L);

                    seedPost("seed_2",
                            "Daulatpur Neighborhood Waste Segregation Discussion & Volunteer Drive",
                            "Civic Initiatives",
                            "Tanvir Ahmed (Ward 5 Resident)",
                            "Our neighborhood youth club is launching an awareness campaign this Friday to encourage separating biodegradable kitchen waste from dry plastic recyclables. We need 15 more student volunteers. Please join us at Daulatpur Main Gate at 09:30 AM.",
                            28, now - 3 * 60000L);

                    seedPost("seed_3",
                            "Community Solar Streetlights: Success in Ward 12 & Expansion Proposals",
                            "Discussions",
                            "Engr. Rafiqul Islam",
                            "The solar-powered streetlights pilot project in Ward 12 has reduced dark alley crime and lowered grid consumption by 35%. What other neighborhood intersections or school roads in Khulna should be prioritized for the upcoming solar expansion?",
                            35, now - 2 * 60000L);

                    seedPost("seed_4",
                            "Bhairab Riverfront Walkway Cleanup Volunteer Meeting this Saturday",
                            "Civic Initiatives",
                            "Clean Khulna Youth Forum",
                            "Join us this Saturday morning at 08:00 AM at 4 No. Ghat for our monthly riverfront litter cleanup. Garbage bags and protective gloves will be provided by KCC. Let's keep our river clean!",
                            51, now - 60000L);
                })
                .addOnFailureListener(e -> Log.w(TAG, "Seed check failed", e));
    }

    private void seedPost(String id, String title, String category, String author,
                          String content, int baseUpvotes, long createdAtMillis) {
        final DocumentReference ref = firestore.collection(POSTS).document(id);

        final Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("category", category);
        data.put("author", author);
        data.put("authorUid", "system");
        data.put("content", content);
        data.put("upvotes", (long) baseUpvotes);
        data.put("voterIds", new ArrayList<String>());
        data.put("createdAt", new Timestamp(new Date(createdAtMillis)));
        data.put("isDemo", true);

        firestore.runTransaction((Transaction.Function<Void>) tx -> {
            if (!tx.get(ref).exists()) {
                tx.set(ref, data);
            }
            return null;
        }).addOnFailureListener(e -> Log.w(TAG, "Could not seed " + id, e));
    }

    private void startPostsListener() {
        postsListener = firestore.collection(POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Posts listener error", e);
                        Toast.makeText(this, "Could not load community posts: " + describeError(e),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null) return;

                    String uid = currentUid();
                    postList.clear();
                    for (QueryDocumentSnapshot d : snap) {
                        postList.add(PostItem.from(d, uid));
                    }
                    displayPosts();
                });
    }

    private void showCreatePostDialog() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to post.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Community Discussion");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(10), dp(20), dp(10));

        final EditText etTitle = new EditText(this);
        etTitle.setHint("Discussion Title");
        layout.addView(etTitle);

        final Spinner spCat = new Spinner(this);
        String[] cats = {"Announcements", "Discussions", "Civic Initiatives"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cats);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCat.setAdapter(catAdapter);
        layout.addView(spCat);

        final EditText etAuthor = new EditText(this);
        etAuthor.setHint("Your Name (or Citizen ID)");
        etAuthor.setText(currentAuthorName());
        layout.addView(etAuthor);

        final EditText etMsg = new EditText(this);
        etMsg.setHint("Share details, suggestions, or initiative details...");
        etMsg.setLines(4);
        layout.addView(etMsg);

        builder.setView(layout);

        builder.setPositiveButton("Publish Post", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String category = spCat.getSelectedItem().toString();
            String author = etAuthor.getText().toString().trim();
            String content = etMsg.getText().toString().trim();

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Title and content cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = currentUid();
            if (uid == null) {
                Toast.makeText(this, "Please log in to post.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("category", category);
            data.put("author", author.isEmpty() ? "Citizen" : author);
            data.put("authorUid", uid);
            data.put("content", content);
            data.put("upvotes", 0L);
            data.put("voterIds", new ArrayList<String>());
            data.put("createdAt", FieldValue.serverTimestamp());
            data.put("isDemo", false);

            firestore.collection(POSTS).add(data)
                    .addOnSuccessListener(ref ->
                            Toast.makeText(this, "Discussion published!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Could not publish: " + describeError(e),
                                    Toast.LENGTH_LONG).show());
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ------------------------------------------------------------------
    // Upvotes (one per user per post)
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static List<String> stringList(@Nullable Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List) {
            for (Object item : (List<Object>) o) {
                if (item instanceof String) out.add((String) item);
            }
        }
        return out;
    }

    private void upvotePost(PostItem post) {
        final String uid = currentUid();
        if (uid == null) {
            Toast.makeText(this, "Please log in to upvote.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (post.votedByMe) {
            Toast.makeText(this, "You have already upvoted this post.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Ignore a second tap while the first vote is still being written.
        if (!votesInFlight.add(post.id)) return;

        final String postId = post.id;
        final DocumentReference ref = firestore.collection(POSTS).document(postId);

        firestore.runTransaction((Transaction.Function<Integer>) tx -> {
            DocumentSnapshot s = tx.get(ref);
            if (!s.exists()) return VOTE_POST_MISSING;

            // The server-side copy is the source of truth, not the local card.
            if (stringList(s.get("voterIds")).contains(uid)) return VOTE_ALREADY;

            tx.update(ref,
                    "voterIds", FieldValue.arrayUnion(uid),
                    "upvotes", FieldValue.increment(1));
            return VOTE_OK;
        }).addOnSuccessListener(code -> {
            votesInFlight.remove(postId);
            if (code == VOTE_OK) {
                Toast.makeText(this, "Upvoted!", Toast.LENGTH_SHORT).show();
            } else if (code == VOTE_ALREADY) {
                Toast.makeText(this, "You have already upvoted this post.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "This post no longer exists.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            votesInFlight.remove(postId);
            Toast.makeText(this, "Upvote not saved: " + describeError(e), Toast.LENGTH_LONG).show();
        });
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    private void displayPosts() {
        layoutCommunityContainer.removeAllViews();
        int visibleCount = 0;

        for (PostItem post : postList) {
            boolean matchesCat = selectedCategory.equals("All")
                    || post.category.equalsIgnoreCase(selectedCategory);

            boolean matchesSearch = searchQuery.isEmpty()
                    || post.title.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || post.content.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || post.author.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || post.category.toLowerCase(Locale.ROOT).contains(searchQuery);

            if (matchesCat && matchesSearch) {
                renderPostCard(post);
                visibleCount++;
            }
        }

        tvCommunityCount.setText("Recent Community Posts (" + visibleCount + " topics)");
        tvNoCommunity.setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
    }

    private void renderPostCard(PostItem post) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        card.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(params);

        // Header: Category Badge (+ DEMO tag on the seeded posts only)
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView tag = new TextView(this);
        tag.setText(post.category.toUpperCase(Locale.ROOT));
        tag.setTextSize(11);
        tag.setTypeface(null, Typeface.BOLD);
        tag.setPadding(dp(8), dp(3), dp(8), dp(3));

        GradientDrawable tagBg = new GradientDrawable();
        tagBg.setCornerRadius(dp(4));

        if (post.category.equalsIgnoreCase("Announcements")) {
            tagBg.setColor(Color.parseColor("#DBEAFE"));
            tag.setTextColor(Color.parseColor("#1E40AF"));
        } else if (post.category.equalsIgnoreCase("Discussions")) {
            tagBg.setColor(Color.parseColor("#EDE9FE"));
            tag.setTextColor(Color.parseColor("#6D28D9"));
        } else {
            tagBg.setColor(Color.parseColor("#DCFCE7"));
            tag.setTextColor(Color.parseColor("#15803D"));
        }
        tag.setBackground(tagBg);
        top.addView(tag);

        if (post.isDemo) {
            TextView demoTag = new TextView(this);
            demoTag.setText("DEMO POST");
            demoTag.setTextColor(Color.parseColor("#B45309"));
            demoTag.setTextSize(10);
            demoTag.setTypeface(null, Typeface.BOLD);
            demoTag.setPadding(dp(10), dp(3), 0, 0);
            top.addView(demoTag);
        }
        card.addView(top);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(post.title);
        tvTitle.setTextSize(16);
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setPadding(0, dp(6), 0, dp(2));
        card.addView(tvTitle);

        // Author & Date
        TextView tvMeta = new TextView(this);
        tvMeta.setText("By " + post.author + " • " + post.createdAt);
        tvMeta.setTextSize(12);
        tvMeta.setTextColor(Color.parseColor("#64748B"));
        tvMeta.setPadding(0, 0, 0, dp(6));
        card.addView(tvMeta);

        // Content
        TextView tvContent = new TextView(this);
        tvContent.setText(post.content);
        tvContent.setTextSize(13);
        tvContent.setTextColor(Color.parseColor("#334155"));
        tvContent.setPadding(0, 0, 0, dp(10));
        card.addView(tvContent);

        // Actions Row: Upvote & Comments
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button btnLike = new Button(this);
        btnLike.setText((post.votedByMe ? "👍 Upvoted (" : "👍 Upvote (") + post.upvotes + ")");
        btnLike.setTextSize(12);
        btnLike.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.parseColor(post.votedByMe ? "#C7D2FE" : "#EEF2FF")));
        btnLike.setTextColor(Color.parseColor("#4F46E5"));
        if (post.votedByMe) {
            btnLike.setAlpha(0.7f);
        }
        btnLike.setOnClickListener(v -> upvotePost(post));

        Button btnReply = new Button(this);
        btnReply.setText("💬 Comments");
        btnReply.setTextSize(12);
        btnReply.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
        btnReply.setTextColor(Color.parseColor("#334155"));
        btnReply.setOnClickListener(v -> showCommentsDialog(post));

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnParams.setMarginEnd(dp(6));
        btnLike.setLayoutParams(btnParams);

        LinearLayout.LayoutParams replyParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnReply.setLayoutParams(replyParams);

        actions.addView(btnLike);
        actions.addView(btnReply);
        card.addView(actions);

        layoutCommunityContainer.addView(card);
    }

    // ------------------------------------------------------------------
    // Comments (live from Firestore)
    // ------------------------------------------------------------------

    private void removeCommentsListener() {
        if (commentsListener != null) {
            commentsListener.remove();
            commentsListener = null;
        }
    }

    private void showCommentsDialog(PostItem post) {
        if (isFinishing()) return;

        final DocumentReference postRef = firestore.collection(POSTS).document(post.id);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(10), dp(16), dp(10));

        // Scrollable list of comments (updates live)
        final ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(240)));
        final LinearLayout commentsContainer = new LinearLayout(this);
        commentsContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(commentsContainer);
        layout.addView(scroll);

        final TextView loading = new TextView(this);
        loading.setText("Loading comments...");
        loading.setTextSize(12);
        loading.setTextColor(Color.parseColor("#94A3B8"));
        commentsContainer.addView(loading);

        // Reply input row
        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        final EditText etReply = new EditText(this);
        etReply.setHint("Write a reply...");
        etReply.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1000)});
        etReply.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        inputRow.addView(etReply);

        final Button btnSend = new Button(this);
        btnSend.setText("Post");
        btnSend.setTextSize(12);
        inputRow.addView(btnSend);
        layout.addView(inputRow);

        btnSend.setOnClickListener(v -> {
            final String text = etReply.getText().toString().trim();
            if (text.isEmpty()) return;

            final String uid = currentUid();
            if (uid == null) {
                Toast.makeText(this, "Please log in to comment.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("author", currentAuthorName());
            data.put("authorUid", uid);
            data.put("text", text);
            data.put("createdAt", FieldValue.serverTimestamp());

            // The snapshot listener below shows the comment instantly (marked
            // "sending…" until the server confirms it), so the box can clear now.
            etReply.setText("");
            postRef.collection(COMMENTS).add(data)
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Comment not saved: " + describeError(e),
                                Toast.LENGTH_LONG).show();
                        if (etReply.getText().length() == 0) etReply.setText(text);
                    });
        });

        removeCommentsListener();
        final ListenerRegistration reg = postRef.collection(COMMENTS)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Comments listener error", e);
                        commentsContainer.removeAllViews();
                        TextView err = new TextView(this);
                        err.setText("Could not load comments: " + describeError(e));
                        err.setTextSize(12);
                        err.setTextColor(Color.parseColor("#B91C1C"));
                        commentsContainer.addView(err);
                        return;
                    }
                    if (snap == null) return;

                    commentsContainer.removeAllViews();

                    if (snap.isEmpty()) {
                        TextView none = new TextView(this);
                        none.setText("No replies yet. Be the first to join this discussion!");
                        none.setTextSize(12);
                        none.setTextColor(Color.parseColor("#94A3B8"));
                        commentsContainer.addView(none);
                        return;
                    }

                    for (QueryDocumentSnapshot d : snap) {
                        String author = d.getString("author");
                        String text = d.getString("text");
                        boolean pending = d.getMetadata().hasPendingWrites();

                        TextView tv = new TextView(this);
                        tv.setText("• " + (author == null ? "Citizen" : author) + ": "
                                + (text == null ? "" : text) + (pending ? "  (sending…)" : ""));
                        tv.setTextSize(13);
                        tv.setTextColor(Color.parseColor(pending ? "#94A3B8" : "#334155"));
                        tv.setPadding(0, dp(3), 0, dp(3));
                        commentsContainer.addView(tv);
                    }
                    scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
                });
        commentsListener = reg;

        commentsDialog = new AlertDialog.Builder(this)
                .setTitle("Comments: " + post.title)
                .setView(layout)
                .setNegativeButton("Close", null)
                .create();
        commentsDialog.setOnDismissListener(d -> {
            reg.remove();
            if (commentsListener == reg) commentsListener = null;
        });
        if (commentsDialog.getWindow() != null) {
            commentsDialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        commentsDialog.show();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    @Nullable
    private String currentUid() {
        FirebaseUser u = firebaseAuth.getCurrentUser();
        return u == null ? null : u.getUid();
    }

    private String currentAuthorName() {
        FirebaseUser u = firebaseAuth.getCurrentUser();
        if (u != null && u.getEmail() != null && u.getEmail().contains("@")) {
            return u.getEmail().split("@")[0];
        }
        return "Citizen";
    }

    private static String describeError(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) e).getCode();
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return "no internet connection.";
            }
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "permission denied (check Firestore security rules).";
            }
        }
        return e.getMessage() == null ? "unknown error" : e.getMessage();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class PostItem {
        String id;
        int upvotes;
        boolean votedByMe;
        boolean isDemo;
        String title, category, author, content, createdAt;

        static PostItem from(DocumentSnapshot d, @Nullable String uid) {
            PostItem p = new PostItem();
            p.id = d.getId();
            p.title = nz(d.getString("title"));
            p.category = nz(d.getString("category"));
            p.author = nz(d.getString("author"));
            p.content = nz(d.getString("content"));

            Long up = d.getLong("upvotes");
            p.upvotes = up == null ? 0 : up.intValue();

            p.votedByMe = uid != null && stringList(d.get("voterIds")).contains(uid);
            p.isDemo = Boolean.TRUE.equals(d.getBoolean("isDemo"));

            Timestamp ts = d.getTimestamp("createdAt",
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
            p.createdAt = ts == null ? "" : new SimpleDateFormat("dd MMM yyyy, hh:mm a",
                    Locale.getDefault()).format(ts.toDate());
            return p;
        }

        private static String nz(@Nullable String s) {
            return s == null ? "" : s;
        }
    }
}
