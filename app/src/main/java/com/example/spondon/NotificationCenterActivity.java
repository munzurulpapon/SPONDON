package com.example.spondon;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationCenterActivity extends AppCompatActivity {

    private EditText etSearchNotifications;
    private TextView tvNotifSummary, tvNoNotifications;
    private LinearLayout layoutNotificationContainer;
    private Button btnMarkAllRead, btnClearAllNotifs, btnBackNotification;

    private Button btnFilterAll, btnFilterUnread, btnFilterReports, btnFilterProjects, btnFilterAlerts;

    private DatabaseHelper databaseHelper;
    private final List<NotificationItem> notificationList = new ArrayList<>();
    private String selectedFilter = "All";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);

        databaseHelper = new DatabaseHelper(this);

        etSearchNotifications = findViewById(R.id.etSearchNotifications);
        tvNotifSummary = findViewById(R.id.tvNotifSummary);
        tvNoNotifications = findViewById(R.id.tvNoNotifications);
        layoutNotificationContainer = findViewById(R.id.layoutNotificationContainer);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        btnClearAllNotifs = findViewById(R.id.btnClearAllNotifs);
        btnBackNotification = findViewById(R.id.btnBackNotification);

        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterUnread = findViewById(R.id.btnFilterUnread);
        btnFilterReports = findViewById(R.id.btnFilterReports);
        btnFilterProjects = findViewById(R.id.btnFilterProjects);
        btnFilterAlerts = findViewById(R.id.btnFilterAlerts);

        btnBackNotification.setOnClickListener(v -> finish());
        btnMarkAllRead.setOnClickListener(v -> markAllNotificationsRead());
        btnClearAllNotifs.setOnClickListener(v -> confirmClearHistory());

        setupFilters();
        setupSearch();
        seedNotificationsIfEmpty();
        loadNotifications();
    }

    private void setupFilters() {
        btnFilterAll.setOnClickListener(v -> setFilter("All"));
        btnFilterUnread.setOnClickListener(v -> setFilter("Unread"));
        btnFilterReports.setOnClickListener(v -> setFilter("Civic Reports"));
        btnFilterProjects.setOnClickListener(v -> setFilter("Public Projects"));
        btnFilterAlerts.setOnClickListener(v -> setFilter("City Alerts"));
    }

    private void setFilter(String filter) {
        selectedFilter = filter;
        updateFilterButtonStyles();
        displayNotifications();
    }

    private void updateFilterButtonStyles() {
        resetFilterBtn(btnFilterAll, "All");
        resetFilterBtn(btnFilterUnread, "Unread");
        resetFilterBtn(btnFilterReports, "Civic Reports");
        resetFilterBtn(btnFilterProjects, "Public Projects");
        resetFilterBtn(btnFilterAlerts, "City Alerts");
    }

    private void resetFilterBtn(Button btn, String filterName) {
        if (selectedFilter.equalsIgnoreCase(filterName)) {
            btn.setBackgroundColor(Color.parseColor("#1E3A8A"));
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setBackgroundColor(Color.parseColor("#E2E8F0"));
            btn.setTextColor(Color.parseColor("#1E293B"));
        }
    }

    private void setupSearch() {
        etSearchNotifications.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                displayNotifications();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void seedNotificationsIfEmpty() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_NOTIFICATIONS, null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }

        if (count == 0) {
            SQLiteDatabase writeDb = databaseHelper.getWritableDatabase();

            insertNotification(writeDb,
                    "Civic Report Status: In Progress",
                    "Your report #104 (Damaged Streetlight at Shibbari) has been inspected and assigned to the KCC Electrical Department for repair.",
                    "Civic Reports",
                    0,
                    "Today, 09:30 AM");

            insertNotification(writeDb,
                    "Scheduled Water Supply Maintenance",
                    "Khulna WASA notice: Khalishpur Zone line maintenance will occur on Thursday from 10:00 AM to 02:00 PM. Please store sufficient water.",
                    "City Alerts",
                    0,
                    "Yesterday, 04:15 PM");

            insertNotification(writeDb,
                    "Rupsha Riverfront Project Milestone",
                    "Phase 2 development of the Rupsha River Walkway has crossed 65% completion. Paving and eco-lighting work underway.",
                    "Public Projects",
                    1,
                    "2 days ago");

            insertNotification(writeDb,
                    "Monsoon Drainage Clearing Alert",
                    "Municipal teams are clearing roadside drainage culverts along Mujgunni Main Road. Please avoid parking over drain slabs.",
                    "City Alerts",
                    0,
                    "3 days ago");

            insertNotification(writeDb,
                    "Green City Tree Plantation Drive",
                    "KCC Environmental Wing is distributing free saplings at Shahid Hadis Park this weekend. Citizen volunteers welcome.",
                    "Public Projects",
                    1,
                    "5 days ago");

            insertNotification(writeDb,
                    "Civic Report Resolved: Road Pothole",
                    "Pothole repair on Sonadanga Bus Terminal bypass road has been completed. Thank you for your active civic participation!",
                    "Civic Reports",
                    1,
                    "1 week ago");
        }
    }

    private void insertNotification(SQLiteDatabase db, String title, String message, String category, int isRead, String createdAt) {
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("message", message);
        cv.put("category", category);
        cv.put("is_read", isRead);
        cv.put("created_at", createdAt);
        db.insert(DatabaseHelper.TABLE_NOTIFICATIONS, null, cv);
    }

    private void loadNotifications() {
        notificationList.clear();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, title, message, category, is_read, created_at FROM "
                        + DatabaseHelper.TABLE_NOTIFICATIONS
                        + " ORDER BY id DESC",
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String title = cursor.getString(1);
                String message = cursor.getString(2);
                String category = cursor.getString(3);
                int isRead = cursor.getInt(4);
                String createdAt = cursor.getString(5);

                notificationList.add(new NotificationItem(id, title, message, category, isRead == 1, createdAt));
            }
            cursor.close();
        }

        displayNotifications();
    }

    private void displayNotifications() {
        layoutNotificationContainer.removeAllViews();

        int unreadCount = 0;
        int totalCount = 0;

        for (NotificationItem item : notificationList) {
            if (!item.isRead) {
                unreadCount++;
            }

            boolean matchesFilter = true;
            if (selectedFilter.equalsIgnoreCase("Unread")) {
                matchesFilter = !item.isRead;
            } else if (!selectedFilter.equalsIgnoreCase("All")) {
                matchesFilter = item.category.equalsIgnoreCase(selectedFilter);
            }

            boolean matchesSearch = searchQuery.isEmpty()
                    || item.title.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || item.message.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || item.category.toLowerCase(Locale.ROOT).contains(searchQuery);

            if (matchesFilter && matchesSearch) {
                totalCount++;
                layoutNotificationContainer.addView(createNotificationCard(item));
            }
        }

        tvNotifSummary.setText("Showing " + totalCount + " notification" + (totalCount == 1 ? "" : "s") + " (" + unreadCount + " unread)");
        tvNoNotifications.setVisibility(totalCount == 0 ? View.VISIBLE : View.GONE);
    }

    private View createNotificationCard(NotificationItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(16);
        if (item.isRead) {
            bg.setColor(Color.parseColor("#FFFFFF"));
            bg.setStroke(2, Color.parseColor("#E2E8F0"));
        } else {
            bg.setColor(Color.parseColor("#EFF6FF")); // Soft highlight for unread
            bg.setStroke(3, Color.parseColor("#3B82F6"));
        }
        card.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);
        card.setLayoutParams(params);
        card.setPadding(32, 28, 32, 28);

        // Header Row: Category Badge + Status / Time
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvCategory = new TextView(this);
        tvCategory.setText(item.category.toUpperCase(Locale.ROOT));
        tvCategory.setTextSize(11);
        tvCategory.setTypeface(null, Typeface.BOLD);
        tvCategory.setPadding(16, 6, 16, 6);

        GradientDrawable catBg = new GradientDrawable();
        catBg.setCornerRadius(10);
        if ("Civic Reports".equalsIgnoreCase(item.category)) {
            catBg.setColor(Color.parseColor("#DBEAFE"));
            tvCategory.setTextColor(Color.parseColor("#1E40AF"));
        } else if ("City Alerts".equalsIgnoreCase(item.category)) {
            catBg.setColor(Color.parseColor("#FEE2E2"));
            tvCategory.setTextColor(Color.parseColor("#991B1B"));
        } else {
            catBg.setColor(Color.parseColor("#DCFCE7"));
            tvCategory.setTextColor(Color.parseColor("#166534"));
        }
        tvCategory.setBackground(catBg);

        TextView tvDate = new TextView(this);
        tvDate.setText(item.createdAt);
        tvDate.setTextSize(11);
        tvDate.setTextColor(Color.parseColor("#64748B"));
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        dateParams.setMarginStart(16);
        tvDate.setLayoutParams(dateParams);

        TextView tvBadge = new TextView(this);
        if (!item.isRead) {
            tvBadge.setText("• NEW");
            tvBadge.setTextColor(Color.parseColor("#2563EB"));
            tvBadge.setTextSize(11);
            tvBadge.setTypeface(null, Typeface.BOLD);
        } else {
            tvBadge.setText("Read");
            tvBadge.setTextColor(Color.parseColor("#94A3B8"));
            tvBadge.setTextSize(11);
        }

        topRow.addView(tvCategory);
        topRow.addView(tvDate);
        topRow.addView(tvBadge);
        card.addView(topRow);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(item.title);
        tvTitle.setTextSize(15);
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        tvTitle.setTypeface(null, item.isRead ? Typeface.NORMAL : Typeface.BOLD);
        tvTitle.setPadding(0, 16, 0, 8);
        card.addView(tvTitle);

        // Message Preview
        TextView tvMsg = new TextView(this);
        tvMsg.setText(item.message);
        tvMsg.setTextSize(13);
        tvMsg.setTextColor(Color.parseColor("#475569"));
        tvMsg.setLineSpacing(4, 1.1f);
        card.addView(tvMsg);

        // Click to view and mark as read
        card.setOnClickListener(v -> showNotificationDetails(item));

        return card;
    }

    private void showNotificationDetails(NotificationItem item) {
        if (!item.isRead) {
            markSingleAsRead(item.id);
            item.isRead = true;
            displayNotifications();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.title);
        builder.setMessage("Category: " + item.category + "\nTime: " + item.createdAt + "\n\n" + item.message);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void markSingleAsRead(long id) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_read", 1);
        db.update(DatabaseHelper.TABLE_NOTIFICATIONS, cv, "id = ?", new String[]{String.valueOf(id)});
    }

    private void markAllNotificationsRead() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_read", 1);
        db.update(DatabaseHelper.TABLE_NOTIFICATIONS, cv, null, null);

        for (NotificationItem item : notificationList) {
            item.isRead = true;
        }

        displayNotifications();
        Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
    }

    private void confirmClearHistory() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Notification History")
                .setMessage("Are you sure you want to clear all notifications from history?")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    SQLiteDatabase db = databaseHelper.getWritableDatabase();
                    db.delete(DatabaseHelper.TABLE_NOTIFICATIONS, null, null);
                    notificationList.clear();
                    displayNotifications();
                    Toast.makeText(this, "Notification history cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static class NotificationItem {
        long id;
        String title;
        String message;
        String category;
        boolean isRead;
        String createdAt;

        NotificationItem(long id, String title, String message, String category, boolean isRead, String createdAt) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.category = category;
            this.isRead = isRead;
            this.createdAt = createdAt;
        }
    }
}
