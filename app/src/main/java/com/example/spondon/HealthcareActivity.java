package com.example.spondon;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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

public class HealthcareActivity extends AppCompatActivity {

    private EditText etSearchHealthcare;
    private TextView tvHealthCount, tvNoHealthResults;
    private LinearLayout layoutHealthContainer;
    private Button btnBackHealthcare;

    private Button btnHealthAll, btnHealthHospitals, btnHealthClinics, btnHealthPharmacies, btnHealthHotlines;

    private final List<HealthFacility> facilityList = new ArrayList<>();
    private String selectedCategory = "All";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_healthcare);

        etSearchHealthcare = findViewById(R.id.etSearchHealthcare);
        tvHealthCount = findViewById(R.id.tvHealthCount);
        tvNoHealthResults = findViewById(R.id.tvNoHealthResults);
        layoutHealthContainer = findViewById(R.id.layoutHealthContainer);
        btnBackHealthcare = findViewById(R.id.btnBackHealthcare);

        btnHealthAll = findViewById(R.id.btnHealthAll);
        btnHealthHospitals = findViewById(R.id.btnHealthHospitals);
        btnHealthClinics = findViewById(R.id.btnHealthClinics);
        btnHealthPharmacies = findViewById(R.id.btnHealthPharmacies);
        btnHealthHotlines = findViewById(R.id.btnHealthHotlines);

        btnBackHealthcare.setOnClickListener(v -> finish());

        TextView tvBloodDonationLink = findViewById(R.id.tvBloodDonationLink);
        if (tvBloodDonationLink != null) {
            tvBloodDonationLink.setOnClickListener(v -> {
                startActivity(new Intent(HealthcareActivity.this, BloodDonationActivity.class));
            });
        }

        setupFilters();
        setupSearch();
        loadHealthcareData();
        displayFacilities();
    }

    private void setupFilters() {
        btnHealthAll.setOnClickListener(v -> setCategory("All"));
        btnHealthHospitals.setOnClickListener(v -> setCategory("Hospital"));
        btnHealthClinics.setOnClickListener(v -> setCategory("Clinic"));
        btnHealthPharmacies.setOnClickListener(v -> setCategory("Pharmacy"));
        btnHealthHotlines.setOnClickListener(v -> setCategory("Emergency"));
    }

    private void setCategory(String category) {
        selectedCategory = category;
        displayFacilities();
    }

    private void setupSearch() {
        etSearchHealthcare.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                displayFacilities();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadHealthcareData() {
        facilityList.clear();

        // Hospitals
        facilityList.add(new HealthFacility("Khulna Medical College Hospital (KMCH)", "Hospital",
                "Boyra Main Road, Khulna", "041-760350", "24/7 Emergency",
                "ICU, CCU, Burn Unit, Blood Bank, Trauma Center, 24/7 Ambulance", 22.8436, 89.5411));

        facilityList.add(new HealthFacility("Khulna City Medical College & Hospital", "Hospital",
                "33 KDA Avenue, Khulna", "041-722115", "24/7 Emergency",
                "Specialized Surgery, Cardiology, General Ward, Oxygen Supply", 22.8250, 89.5520));

        facilityList.add(new HealthFacility("Gazi Medical College Hospital", "Hospital",
                "Sonadanga Bypass, Khulna", "041-731380", "24/7 Emergency",
                "Emergency Care, Radiology, Diagnostic Lab, Pediatric Ward", 22.8185, 89.5450));

        // Clinics & Diagnostics
        facilityList.add(new HealthFacility("Sandhani Diagnostic & Consultation", "Clinic",
                "Shibbari Mor, Khulna", "01712-345678", "08:00 AM - 10:00 PM",
                "Pathology, Digital X-Ray, Ultrasonography, Specialist Chambers", 22.8290, 89.5535));

        facilityList.add(new HealthFacility("Labaid Diagnostic Center Khulna", "Clinic",
                "Majid Sarani, Sonadanga, Khulna", "01766-662888", "07:00 AM - 11:00 PM",
                "MRI, CT Scan, Cardiac Echo, Automated Blood Biochemistry", 22.8210, 89.5480));

        facilityList.add(new HealthFacility("Popular Diagnostic Centre", "Clinic",
                "Shamsur Rahman Road, Khulna", "09613-787814", "07:00 AM - 11:00 PM",
                "Hormone Testing, Endoscopy, Consultation, Sample Collection", 22.8150, 89.5590));

        // Pharmacies
        facilityList.add(new HealthFacility("Lazz Pharma Khulna (24/7 Model Pharmacy)", "Pharmacy",
                "KDA Avenue Near Shibbari, Khulna", "01711-889900", "24 Hours Open",
                "Refrigerated Vaccines, Emergency Lifesaving Medicines, Medical Devices", 22.8285, 89.5540));

        facilityList.add(new HealthFacility("Medex Emergency Pharmacy", "Pharmacy",
                "Opposite KMCH Gate 1, Boyra, Khulna", "01923-445566", "24 Hours Open",
                "Surgical Supplies, Inhalers, Oxygen Cylinder Refill Support", 22.8440, 89.5415));

        facilityList.add(new HealthFacility("Green Life 24-Hour Medicine Store", "Pharmacy",
                "Sonadanga Bus Terminal Entrance, Khulna", "01815-667788", "24 Hours Open",
                "First-Aid Kits, Prescription Dispensing, Baby Care", 22.8190, 89.5460));

        // Emergency Hotlines
        facilityList.add(new HealthFacility("National Emergency Helpline (Ambulance / Police / Fire)", "Emergency",
                "Nationwide Emergency Dispatch Center", "999", "24/7 Toll-Free",
                "Emergency dispatch for life support ambulance, fire service and rescue.", 22.8456, 89.5403));

        facilityList.add(new HealthFacility("Khulna Civil Surgeon Ambulance Service", "Emergency",
                "Civil Surgeon Office, Khulna", "041-761530", "24/7 Emergency Dispatch",
                "Government subsidized patient transfer and ICU ambulance transfer.", 22.8310, 89.5490));

        facilityList.add(new HealthFacility("Bangladesh Red Crescent Society Khulna", "Emergency",
                "Red Crescent Bhaban, Khulna", "041-721200", "24/7 Disaster Support",
                "First aid response, emergency humanitarian support and blood donation coordination.", 22.8200, 89.5510));
    }

    private void displayFacilities() {
        layoutHealthContainer.removeAllViews();
        int count = 0;

        for (HealthFacility facility : facilityList) {
            boolean matchesCat = "All".equalsIgnoreCase(selectedCategory)
                    || facility.category.equalsIgnoreCase(selectedCategory);

            boolean matchesSearch = facility.name.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || facility.address.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || facility.services.toLowerCase(Locale.ROOT).contains(searchQuery);

            if (matchesCat && matchesSearch) {
                addFacilityCard(facility);
                count++;
            }
        }

        tvHealthCount.setText("Healthcare Facilities (" + count + ")");
        tvNoHealthResults.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    private void addFacilityCard(HealthFacility facility) {
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

        // Header: Category & Demo Tag
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView tag = new TextView(this);
        tag.setText(facility.category.toUpperCase(Locale.ROOT));
        tag.setTextColor(Color.parseColor("#991B1B"));
        tag.setTextSize(11);
        tag.setTypeface(null, Typeface.BOLD);
        tag.setBackgroundColor(Color.parseColor("#FEE2E2"));
        tag.setPadding(dp(8), dp(3), dp(8), dp(3));

        TextView demoTag = new TextView(this);
        demoTag.setText("DEMO DATA");
        demoTag.setTextColor(Color.parseColor("#B45309"));
        demoTag.setTextSize(10);
        demoTag.setTypeface(null, Typeface.BOLD);
        demoTag.setPadding(dp(10), dp(3), 0, 0);

        top.addView(tag);
        top.addView(demoTag);
        card.addView(top);

        // Name
        TextView name = new TextView(this);
        name.setText(facility.name);
        name.setTextSize(17);
        name.setTextColor(Color.parseColor("#0F172A"));
        name.setTypeface(null, Typeface.BOLD);
        name.setPadding(0, dp(6), 0, dp(2));
        card.addView(name);

        // Address & Hours
        TextView address = new TextView(this);
        address.setText("Location: " + facility.address);
        address.setTextSize(13);
        address.setTextColor(Color.parseColor("#475569"));
        card.addView(address);

        TextView hours = new TextView(this);
        hours.setText("Hours: " + facility.hours);
        hours.setTextSize(13);
        hours.setTextColor(Color.parseColor("#0F766E"));
        hours.setTypeface(null, Typeface.BOLD);
        hours.setPadding(0, dp(2), 0, dp(4));
        card.addView(hours);

        TextView services = new TextView(this);
        services.setText("Services: " + facility.services);
        services.setTextSize(12);
        services.setTextColor(Color.parseColor("#64748B"));
        services.setPadding(0, 0, 0, dp(8));
        card.addView(services);

        // Action Buttons Row
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button btnCall = new Button(this);
        btnCall.setText("Call (" + facility.phone + ")");
        btnCall.setTextSize(12);
        btnCall.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#991B1B")));
        btnCall.setTextColor(Color.WHITE);
        btnCall.setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + facility.phone.replace("-", "")));
            startActivity(callIntent);
        });

        Button btnMap = new Button(this);
        btnMap.setText("View on Map");
        btnMap.setTextSize(12);
        btnMap.setOnClickListener(v -> {
            Intent mapIntent = new Intent(HealthcareActivity.this, CityMapActivity.class);
            mapIntent.putExtra("DEST_LAT", facility.latitude);
            mapIntent.putExtra("DEST_LON", facility.longitude);
            mapIntent.putExtra("DEST_NAME", facility.name);
            startActivity(mapIntent);
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnCall.setLayoutParams(btnParams);
        btnMap.setLayoutParams(btnParams);

        actions.addView(btnCall);
        actions.addView(btnMap);
        card.addView(actions);

        layoutHealthContainer.addView(card);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class HealthFacility {
        String name, category, address, phone, hours, services;
        double latitude, longitude;

        HealthFacility(String name, String category, String address, String phone,
                       String hours, String services, double latitude, double longitude) {
            this.name = name;
            this.category = category;
            this.address = address;
            this.phone = phone;
            this.hours = hours;
            this.services = services;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
