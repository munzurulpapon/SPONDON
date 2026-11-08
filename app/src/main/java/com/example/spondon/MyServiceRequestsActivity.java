package com.example.spondon;

import android.app.AlertDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyServiceRequestsActivity extends AppCompatActivity {

    private TextView tvBookingCount, tvNoBookings;
    private ProgressBar progressMyBookings;
    private LinearLayout layoutBookingsContainer;
    private Button btnBackMyBookings;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private DatabaseHelper databaseHelper;
    private ListenerRegistration requestsListener;

    private final List<ServiceBooking> bookingList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_service_requests);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        databaseHelper = new DatabaseHelper(this);

        tvBookingCount = findViewById(R.id.tvBookingCount);
        tvNoBookings = findViewById(R.id.tvNoBookings);
        progressMyBookings = findViewById(R.id.progressMyBookings);
        layoutBookingsContainer = findViewById(R.id.layoutBookingsContainer);
        btnBackMyBookings = findViewById(R.id.btnBackMyBookings);

        btnBackMyBookings.setOnClickListener(v -> finish());

        loadMyBookings();
    }

    private void loadMyBookings() {
        if (firebaseAuth.getCurrentUser() == null) {
            loadFromSQLite();
            return;
        }

        progressMyBookings.setVisibility(View.VISIBLE);
        String uid = firebaseAuth.getCurrentUser().getUid();

        requestsListener = firestore.collection("service_requests")
                .whereEqualTo("citizenUid", uid)
                .addSnapshotListener((value, error) -> {
                    progressMyBookings.setVisibility(View.GONE);

                    if (error != null || value == null) {
                        loadFromSQLite();
                        return;
                    }

                    bookingList.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ServiceBooking sb = new ServiceBooking();
                        sb.id = doc.getId();
                        sb.serviceName = doc.getString("serviceName");
                        sb.category = doc.getString("category");
                        sb.address = doc.getString("address");
                        sb.phone = doc.getString("citizenPhone");
                        sb.preferredDate = doc.getString("preferredDate");
                        sb.preferredTime = doc.getString("preferredTime");
                        sb.notes = doc.getString("notes");
                        sb.status = doc.getString("status");
                        sb.providerName = doc.getString("providerName");

                        Object rObj = doc.get("rating");
                        if (rObj instanceof Number) sb.rating = ((Number) rObj).intValue();
                        sb.feedback = doc.getString("feedback");

                        if (sb.serviceName == null) sb.serviceName = "Service Request";
                        if (sb.status == null) sb.status = "Pending";

                        bookingList.add(sb);
                    }

                    displayBookings();
                });
    }

    private void loadFromSQLite() {
        bookingList.clear();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_SERVICE_REQUESTS + " ORDER BY id DESC", null);

        while (c.moveToNext()) {
            ServiceBooking sb = new ServiceBooking();
            sb.id = c.getString(c.getColumnIndexOrThrow("firebase_id"));
            sb.serviceName = c.getString(c.getColumnIndexOrThrow("service_name"));
            sb.category = c.getString(c.getColumnIndexOrThrow("category"));
            sb.address = c.getString(c.getColumnIndexOrThrow("address"));
            sb.phone = c.getString(c.getColumnIndexOrThrow("phone"));
            sb.preferredDate = c.getString(c.getColumnIndexOrThrow("preferred_date"));
            sb.preferredTime = c.getString(c.getColumnIndexOrThrow("preferred_time"));
            sb.notes = c.getString(c.getColumnIndexOrThrow("notes"));
            sb.status = c.getString(c.getColumnIndexOrThrow("status"));
            bookingList.add(sb);
        }
        c.close();
        db.close();

        displayBookings();
    }

    private void displayBookings() {
        layoutBookingsContainer.removeAllViews();
        tvBookingCount.setText("Total Bookings: " + bookingList.size());

        if (bookingList.isEmpty()) {
            tvNoBookings.setVisibility(View.VISIBLE);
            return;
        }

        tvNoBookings.setVisibility(View.GONE);

        for (ServiceBooking booking : bookingList) {
            addBookingCard(booking);
        }
    }

    private void addBookingCard(ServiceBooking booking) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        card.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(params);

        // Header: Category & Status Badge
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView catView = new TextView(this);
        catView.setText(booking.category != null ? booking.category.toUpperCase() : "SERVICE");
        catView.setTextColor(Color.parseColor("#475569"));
        catView.setTextSize(11);
        catView.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams catParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        catView.setLayoutParams(catParams);

        TextView statusView = new TextView(this);
        statusView.setText(booking.status.toUpperCase());
        statusView.setTextSize(12);
        statusView.setTypeface(null, Typeface.BOLD);
        statusView.setPadding(dp(8), dp(4), dp(8), dp(4));

        if ("Completed".equalsIgnoreCase(booking.status)) {
            statusView.setTextColor(Color.parseColor("#15803D"));
            statusView.setBackgroundColor(Color.parseColor("#DCFCE7"));
        } else if ("Accepted".equalsIgnoreCase(booking.status) || "In Progress".equalsIgnoreCase(booking.status)) {
            statusView.setTextColor(Color.parseColor("#1D4ED8"));
            statusView.setBackgroundColor(Color.parseColor("#DBEAFE"));
        } else if ("Cancelled".equalsIgnoreCase(booking.status)) {
            statusView.setTextColor(Color.parseColor("#B91C1C"));
            statusView.setBackgroundColor(Color.parseColor("#FEE2E2"));
        } else {
            statusView.setTextColor(Color.parseColor("#B45309"));
            statusView.setBackgroundColor(Color.parseColor("#FEF3C7"));
        }

        top.addView(catView);
        top.addView(statusView);
        card.addView(top);

        // Service Name
        TextView name = new TextView(this);
        name.setText(booking.serviceName);
        name.setTextSize(17);
        name.setTextColor(Color.parseColor("#0F172A"));
        name.setTypeface(null, Typeface.BOLD);
        name.setPadding(0, dp(8), 0, dp(4));
        card.addView(name);

        // Details
        TextView schedule = new TextView(this);
        schedule.setText("Schedule: " + (booking.preferredDate != null ? booking.preferredDate : "ASAP")
                + " • " + (booking.preferredTime != null ? booking.preferredTime : "Standard"));
        schedule.setTextSize(13);
        schedule.setTextColor(Color.parseColor("#475569"));
        card.addView(schedule);

        TextView addr = new TextView(this);
        addr.setText("Location: " + (booking.address != null ? booking.address : "City Area"));
        addr.setTextSize(13);
        addr.setTextColor(Color.parseColor("#64748B"));
        addr.setPadding(0, dp(2), 0, dp(6));
        card.addView(addr);

        if (booking.notes != null && !booking.notes.isEmpty()) {
            TextView notesView = new TextView(this);
            notesView.setText("Notes: " + booking.notes);
            notesView.setTextSize(12);
            notesView.setTextColor(Color.parseColor("#64748B"));
            notesView.setPadding(0, 0, 0, dp(8));
            card.addView(notesView);
        }

        // Action Buttons Row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(8), 0, 0);

        if ("Completed".equalsIgnoreCase(booking.status)) {
            Button btnRate = new Button(this);
            btnRate.setText(booking.rating > 0 ? "Rated: " + booking.rating + " ★" : "Rate & Feedback");
            btnRate.setTextSize(12);
            btnRate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D97706")));
            btnRate.setTextColor(Color.WHITE);
            btnRate.setOnClickListener(v -> showRatingDialog(booking));
            btnRow.addView(btnRate);
        } else if ("Pending".equalsIgnoreCase(booking.status)) {
            Button btnCancel = new Button(this);
            btnCancel.setText("Cancel Request");
            btnCancel.setTextSize(12);
            btnCancel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DC2626")));
            btnCancel.setTextColor(Color.WHITE);
            btnCancel.setOnClickListener(v -> cancelRequest(booking));
            btnRow.addView(btnCancel);
        }

        if (btnRow.getChildCount() > 0) {
            card.addView(btnRow);
        }

        layoutBookingsContainer.addView(card);
    }

    private void showRatingDialog(ServiceBooking booking) {
        if (booking.id == null || booking.id.isEmpty()) {
            Toast.makeText(this, "Cannot submit review for local-only booking.", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(20), dp(10), dp(20), 0);

        TextView prompt = new TextView(this);
        prompt.setText("How was your service experience?");
        prompt.setTextSize(14);
        prompt.setTextColor(Color.BLACK);
        dialogLayout.addView(prompt);

        RatingBar ratingBar = new RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1.0f);
        ratingBar.setRating(booking.rating > 0 ? booking.rating : 5);
        dialogLayout.addView(ratingBar);

        EditText etFeedback = new EditText(this);
        etFeedback.setHint("Write a quick feedback or comment...");
        if (booking.feedback != null) etFeedback.setText(booking.feedback);
        dialogLayout.addView(etFeedback);

        new AlertDialog.Builder(this)
                .setTitle("Rate & Review")
                .setView(dialogLayout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Submit Review", (dialog, which) -> {
                    int stars = (int) ratingBar.getRating();
                    String fb = etFeedback.getText().toString().trim();

                    Map<String, Object> update = new HashMap<>();
                    update.put("rating", stars);
                    update.put("feedback", fb);

                    firestore.collection("service_requests").document(booking.id)
                            .update(update)
                            .addOnSuccessListener(u -> Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Rating failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .show();
    }

    private void cancelRequest(ServiceBooking booking) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Request")
                .setMessage("Are you sure you want to cancel this service booking?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    if (booking.id != null && !booking.id.isEmpty()) {
                        firestore.collection("service_requests").document(booking.id)
                                .update("status", "Cancelled")
                                .addOnSuccessListener(u -> Toast.makeText(this, "Booking cancelled.", Toast.LENGTH_SHORT).show());
                    }
                })
                .show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (requestsListener != null) {
            requestsListener.remove();
        }
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }

    private static class ServiceBooking {
        String id;
        String serviceName;
        String category;
        String address;
        String phone;
        String preferredDate;
        String preferredTime;
        String notes;
        String status;
        String providerName;
        int rating = 0;
        String feedback = "";
    }
}
