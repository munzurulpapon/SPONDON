package com.example.spondon;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BloodDonationActivity extends AppCompatActivity {

    private TextView tvDonorsCount;
    private LinearLayout layoutDonorsContainer;
    private Button btnRegisterDonor, btnBackBlood;

    private Button btnBgAll, btnBgAPos, btnBgANeg, btnBgBPos, btnBgBNeg, btnBgOPos, btnBgONeg, btnBgABPos, btnBgABNeg;

    private DatabaseHelper databaseHelper;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private final List<DonorRecord> donorList = new ArrayList<>();
    private String selectedGroup = "All";

    private final String[] bloodGroups = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blood_donation);

        databaseHelper = new DatabaseHelper(this);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvDonorsCount = findViewById(R.id.tvDonorsCount);
        layoutDonorsContainer = findViewById(R.id.layoutDonorsContainer);
        btnRegisterDonor = findViewById(R.id.btnRegisterDonor);
        btnBackBlood = findViewById(R.id.btnBackBlood);

        btnBgAll = findViewById(R.id.btnBgAll);
        btnBgAPos = findViewById(R.id.btnBgAPos);
        btnBgANeg = findViewById(R.id.btnBgANeg);
        btnBgBPos = findViewById(R.id.btnBgBPos);
        btnBgBNeg = findViewById(R.id.btnBgBNeg);
        btnBgOPos = findViewById(R.id.btnBgOPos);
        btnBgONeg = findViewById(R.id.btnBgONeg);
        btnBgABPos = findViewById(R.id.btnBgABPos);
        btnBgABNeg = findViewById(R.id.btnBgABNeg);

        btnBackBlood.setOnClickListener(v -> finish());
        btnRegisterDonor.setOnClickListener(v -> showRegisterDonorDialog());

        setupGroupFilters();
        loadDonors();
    }

    private void setupGroupFilters() {
        btnBgAll.setOnClickListener(v -> filterGroup("All"));
        btnBgAPos.setOnClickListener(v -> filterGroup("A+"));
        btnBgANeg.setOnClickListener(v -> filterGroup("A-"));
        btnBgBPos.setOnClickListener(v -> filterGroup("B+"));
        btnBgBNeg.setOnClickListener(v -> filterGroup("B-"));
        btnBgOPos.setOnClickListener(v -> filterGroup("O+"));
        btnBgONeg.setOnClickListener(v -> filterGroup("O-"));
        btnBgABPos.setOnClickListener(v -> filterGroup("AB+"));
        btnBgABNeg.setOnClickListener(v -> filterGroup("AB-"));
    }

    private void filterGroup(String group) {
        selectedGroup = group;
        displayDonors();
    }

    private void loadDonors() {
        donorList.clear();

        // Institutional Blood Banks
        donorList.add(new DonorRecord("Sandhani KMCH Blood Bank Unit", "ALL GROUPS", "041-760350", "Boyra Main Road, Khulna", true));
        donorList.add(new DonorRecord("Red Crescent Blood Center", "ALL GROUPS", "01711-223344", "Red Crescent Bhaban, Khulna", true));
        donorList.add(new DonorRecord("Badhan Blood Donation Voluntary (KUET Zone)", "ALL GROUPS", "01912-889900", "KUET Campus / Phulbarigate", true));

        // Sample Verified Voluntary Donors (Demo Data)
        donorList.add(new DonorRecord("Tanvir Ahmed", "O+", "01711-456789", "Sonadanga, Khulna", false));
        donorList.add(new DonorRecord("Mahmudul Hasan", "A+", "01822-567890", "Shibbari, Khulna", false));
        donorList.add(new DonorRecord("Nusrat Jahan", "B+", "01933-678901", "Boyra, Khulna", false));
        donorList.add(new DonorRecord("Rafiqul Islam", "O-", "01744-789012", "Khalishpur, Khulna", false));
        donorList.add(new DonorRecord("Sabbir Hossain", "AB+", "01655-890123", "Daulatpur, Khulna", false));
        donorList.add(new DonorRecord("Fatema Akter", "A-", "01566-901234", "Gollamari, Khulna", false));
        donorList.add(new DonorRecord("Ashraful Alam", "B-", "01777-012345", "Rupsha, Khulna", false));
        donorList.add(new DonorRecord("Imran Khan", "AB-", "01888-123456", "Phulbarigate, Khulna", false));

        // Load local voluntary registrations from SQLite
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_BLOOD_DONORS + " ORDER BY id DESC", null);
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String bg = cursor.getString(cursor.getColumnIndexOrThrow("blood_group"));
            String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
            String area = cursor.getString(cursor.getColumnIndexOrThrow("area"));
            donorList.add(0, new DonorRecord(name + " (Local Donor)", bg, phone, area, false));
        }
        cursor.close();
        db.close();

        displayDonors();
    }

    private void displayDonors() {
        layoutDonorsContainer.removeAllViews();
        int count = 0;

        for (DonorRecord donor : donorList) {
            boolean matches = "All".equalsIgnoreCase(selectedGroup)
                    || "ALL GROUPS".equalsIgnoreCase(donor.bloodGroup)
                    || donor.bloodGroup.equalsIgnoreCase(selectedGroup);

            if (matches) {
                addDonorCard(donor);
                count++;
            }
        }

        tvDonorsCount.setText("Available Donors & Banks (" + count + ")");
    }

    private void addDonorCard(DonorRecord donor) {
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

        // Header
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView bgTag = new TextView(this);
        bgTag.setText(donor.bloodGroup);
        bgTag.setTextColor(Color.parseColor("#991B1B"));
        bgTag.setTextSize(13);
        bgTag.setTypeface(null, Typeface.BOLD);
        bgTag.setBackgroundColor(Color.parseColor("#FEE2E2"));
        bgTag.setPadding(dp(10), dp(4), dp(10), dp(4));

        TextView typeTag = new TextView(this);
        typeTag.setText(donor.isInstitutional ? "HOSPITAL BLOOD BANK" : "VOLUNTARY DONOR");
        typeTag.setTextColor(donor.isInstitutional ? Color.parseColor("#1D4ED8") : Color.parseColor("#047857"));
        typeTag.setTextSize(11);
        typeTag.setTypeface(null, Typeface.BOLD);
        typeTag.setPadding(dp(12), dp(4), 0, 0);

        top.addView(bgTag);
        top.addView(typeTag);
        card.addView(top);

        // Name
        TextView name = new TextView(this);
        name.setText(donor.name);
        name.setTextSize(17);
        name.setTextColor(Color.parseColor("#0F172A"));
        name.setTypeface(null, Typeface.BOLD);
        name.setPadding(0, dp(6), 0, dp(2));
        card.addView(name);

        // Area
        TextView area = new TextView(this);
        area.setText("Location: " + donor.area);
        area.setTextSize(13);
        area.setTextColor(Color.parseColor("#475569"));
        card.addView(area);

        // Call Button
        Button btnCall = new Button(this);
        btnCall.setText("Call (" + donor.phone + ")");
        btnCall.setTextSize(12);
        btnCall.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#B91C1C")));
        btnCall.setTextColor(Color.WHITE);
        btnCall.setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + donor.phone.replace("-", "")));
            startActivity(callIntent);
        });
        card.addView(btnCall);

        layoutDonorsContainer.addView(card);
    }

    private void showRegisterDonorDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(10), dp(20), 0);

        EditText etName = new EditText(this);
        etName.setHint("Full Name");
        layout.addView(etName);

        Spinner spGroup = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGroup.setAdapter(adapter);
        layout.addView(spGroup);

        EditText etPhone = new EditText(this);
        etPhone.setHint("Phone Number");
        layout.addView(etPhone);

        EditText etArea = new EditText(this);
        etArea.setHint("Your Area (e.g. Sonadanga, Boyra)");
        layout.addView(etArea);

        new AlertDialog.Builder(this)
                .setTitle("Register as Voluntary Donor")
                .setMessage("Your contact info will be accessible to citizens in emergency need.")
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Register", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String bg = spGroup.getSelectedItem().toString();
                    String phone = etPhone.getText().toString().trim();
                    String area = etArea.getText().toString().trim();

                    if (name.isEmpty() || phone.isEmpty() || area.isEmpty()) {
                        Toast.makeText(this, "Please fill in all details.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Save to SQLite
                    SQLiteDatabase db = databaseHelper.getWritableDatabase();
                    ContentValues cv = new ContentValues();
                    cv.put("name", name);
                    cv.put("blood_group", bg);
                    cv.put("phone", phone);
                    cv.put("area", area);
                    db.insert(DatabaseHelper.TABLE_BLOOD_DONORS, null, cv);
                    db.close();

                    // Also save to Firestore if online
                    Map<String, Object> doc = new HashMap<>();
                    doc.put("name", name);
                    doc.put("bloodGroup", bg);
                    doc.put("phone", phone);
                    doc.put("area", area);
                    doc.put("registeredAt", FieldValue.serverTimestamp());
                    if (auth.getCurrentUser() != null) {
                        doc.put("uid", auth.getCurrentUser().getUid());
                    }
                    firestore.collection("blood_donors").add(doc);

                    Toast.makeText(this, "Thank you! You are now registered as a voluntary blood donor.", Toast.LENGTH_LONG).show();
                    loadDonors();
                })
                .show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }

    private static class DonorRecord {
        String name, bloodGroup, phone, area;
        boolean isInstitutional;

        DonorRecord(String name, String bloodGroup, String phone, String area, boolean isInstitutional) {
            this.name = name;
            this.bloodGroup = bloodGroup;
            this.phone = phone;
            this.area = area;
            this.isInstitutional = isInstitutional;
        }
    }
}
