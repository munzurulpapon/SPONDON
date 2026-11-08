package com.example.spondon;

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

public class PublicUtilitiesActivity extends AppCompatActivity {

    private EditText etSearchUtilities;
    private TextView tvUtilCount, tvNoUtilResults;
    private LinearLayout layoutUtilContainer;
    private Button btnBackUtilities;

    private Button btnUtilAll, btnUtilWater, btnUtilElectricity, btnUtilWaste, btnUtilGas;

    private final List<UtilityItem> utilityList = new ArrayList<>();
    private String selectedCategory = "All";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_utilities);

        etSearchUtilities = findViewById(R.id.etSearchUtilities);
        tvUtilCount = findViewById(R.id.tvUtilCount);
        tvNoUtilResults = findViewById(R.id.tvNoUtilResults);
        layoutUtilContainer = findViewById(R.id.layoutUtilContainer);
        btnBackUtilities = findViewById(R.id.btnBackUtilities);

        btnUtilAll = findViewById(R.id.btnUtilAll);
        btnUtilWater = findViewById(R.id.btnUtilWater);
        btnUtilElectricity = findViewById(R.id.btnUtilElectricity);
        btnUtilWaste = findViewById(R.id.btnUtilWaste);
        btnUtilGas = findViewById(R.id.btnUtilGas);

        btnBackUtilities.setOnClickListener(v -> finish());

        setupFilters();
        setupSearch();
        loadUtilityData();
        displayUtilities();
    }

    private void setupFilters() {
        btnUtilAll.setOnClickListener(v -> setCategory("All"));
        btnUtilWater.setOnClickListener(v -> setCategory("Water"));
        btnUtilElectricity.setOnClickListener(v -> setCategory("Electricity"));
        btnUtilWaste.setOnClickListener(v -> setCategory("Waste"));
        btnUtilGas.setOnClickListener(v -> setCategory("Gas"));
    }

    private void setCategory(String category) {
        selectedCategory = category;
        displayUtilities();
    }

    private void setupSearch() {
        etSearchUtilities.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                displayUtilities();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUtilityData() {
        utilityList.clear();

        // 1. Water Supply (Khulna WASA - KWASA)
        utilityList.add(new UtilityItem(
                "KWASA Zonal Piped Water Supply & Rationing",
                "Water",
                "Khulna WASA Bhaban, KDA Avenue, Khulna",
                "Morning: 06:00 AM - 09:00 AM • Evening: 04:30 PM - 07:30 PM",
                "Deep tubewell and surface water treatment supply. For new connections or billing queries, visit WASA customer portal or nearest zonal pump station.",
                "041-760880",
                false, false, 0, 0
        ));

        utilityList.add(new UtilityItem(
                "Emergency Mobile Water Tanker Delivery Service",
                "Water",
                "KWASA Central Vehicle Yard, Boyra, Khulna",
                "24/7 Hotline for Emergency Potable Water Truck Dispatch",
                "Rapid dispatch of 3,000L and 5,000L clean drinking water tankers for areas experiencing pipe breakdown or acute scarcity. Tariff: ৳600 per tanker.",
                "01711-889900",
                false, false, 0, 0
        ));

        utilityList.add(new UtilityItem(
                "Report Water Pipe Leakage or Dirty Supply",
                "Water",
                "All Municipal Water Network Wards",
                "Instant Civic Issue Submission",
                "Notice roadside water gushing, burst distribution mains, or murky tap water? Submit a photo and GPS location directly to the KWASA rapid repair team.",
                "041-760881",
                true, false, 0, 0
        ));

        // 2. Electricity (WZPDCL)
        utilityList.add(new UtilityItem(
                "WZPDCL 24/7 Power Outage & Fault Helpline (16117)",
                "Electricity",
                "West Zone Power Distribution Bhaban, Boyra Main Road",
                "24 Hours Call Center • Rapid Fuse Gang Deployment",
                "Report localized transformer blowouts, line sparking, loose high-voltage cables, or unscheduled blackout issues directly to Central Control.",
                "16117",
                false, false, 0, 0
        ));

        utilityList.add(new UtilityItem(
                "Feeder Line Maintenance & Load Advisories",
                "Electricity",
                "Sonadanga, Shibbari, Boyra & Daulatpur Sub-stations",
                "Weekly Maintenance Schedule (Friday & Saturday Mornings)",
                "Planned substation transformer maintenance and tree-trimming shutdowns are notified in advance. Peak load conservation hours: 06:00 PM - 11:00 PM.",
                "041-762100",
                false, false, 0, 0
        ));

        utilityList.add(new UtilityItem(
                "WZPDCL Smart Pre-Paid Meter Customer Care Center",
                "Electricity",
                "KDA Avenue Customer Service Hub, Khulna",
                "09:00 AM - 05:00 PM (Govt Working Days)",
                "Prepaid token generation, card recharge assistance, meter tampering diagnostics, and tariff advisory.",
                "041-761550",
                false, true, 22.8250, 89.5510
        ));

        // 3. Waste Management (Khulna City Corporation - KCC)
        utilityList.add(new UtilityItem(
                "KCC Door-to-Door Household Waste Collection",
                "Waste",
                "All 31 Municipal Wards, Khulna",
                "Daily Morning Collection: 07:00 AM - 11:00 AM",
                "Ward-wise rickshaw-van waste collectors collect segregated household organic and inorganic waste directly from households. Monthly fee: ৳50 - ৳100.",
                "041-720133",
                false, false, 0, 0
        ));

        utilityList.add(new UtilityItem(
                "Secondary Transfer Stations (STS) & Compactor Centers",
                "Waste",
                "Sonadanga Bypass & Khalishpur STS Hubs, Khulna",
                "Continuous Compaction: 08:00 AM - 08:00 PM",
                "Enclosed modern compactor stations preventing roadside odor and litter scattering. Waste is transferred to Rajbandh central sanitary landfill.",
                "041-720134",
                false, true, 22.8210, 89.5390
        ));

        utilityList.add(new UtilityItem(
                "Report Illegal Garbage Dumping / Overflowing Bins",
                "Waste",
                "All Ward Streets & Community Drains",
                "Direct Civic Issue Reporting",
                "Take a photo and tag GPS location of open garbage piles, clogged drainage culverts, or neglected bins for immediate municipal truck dispatch.",
                "041-720135",
                true, false, 0, 0
        ));

        // 4. Gas & Energy (Sundarban Gas Company Ltd - SGCL)
        utilityList.add(new UtilityItem(
                "SGCL Emergency Gas Leakage & Pipeline Hotline (16496)",
                "Gas",
                "Sundarban Gas Company Central Control, Khulna",
                "24/7 Emergency Gas Control Room",
                "If you smell strong gas odor in your kitchen or along street distribution pipelines, do not operate electrical switches. Ventilate rooms and call 16496 immediately.",
                "16496",
                false, false, 0, 0
        ));
    }

    private void displayUtilities() {
        layoutUtilContainer.removeAllViews();
        int visibleCount = 0;

        for (UtilityItem item : utilityList) {
            boolean matchesCategory = selectedCategory.equals("All")
                    || item.category.equalsIgnoreCase(selectedCategory);

            boolean matchesSearch = searchQuery.isEmpty()
                    || item.name.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || item.authority.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || item.details.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || item.category.toLowerCase(Locale.ROOT).contains(searchQuery);

            if (matchesCategory && matchesSearch) {
                renderUtilityCard(item);
                visibleCount++;
            }
        }

        tvUtilCount.setText("Available Civic Utility Desks (" + visibleCount + " entries)");

        if (visibleCount == 0) {
            tvNoUtilResults.setVisibility(View.VISIBLE);
        } else {
            tvNoUtilResults.setVisibility(View.GONE);
        }
    }

    private void renderUtilityCard(UtilityItem item) {
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

        // Header
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView tag = new TextView(this);
        tag.setText(item.category.toUpperCase(Locale.ROOT));
        tag.setTextSize(11);
        tag.setTypeface(null, Typeface.BOLD);
        tag.setPadding(dp(8), dp(3), dp(8), dp(3));

        GradientDrawable tagBg = new GradientDrawable();
        tagBg.setCornerRadius(dp(4));

        if (item.category.equalsIgnoreCase("Water")) {
            tagBg.setColor(Color.parseColor("#E0F2FE"));
            tag.setTextColor(Color.parseColor("#0369A1"));
        } else if (item.category.equalsIgnoreCase("Electricity")) {
            tagBg.setColor(Color.parseColor("#FEF3C7"));
            tag.setTextColor(Color.parseColor("#B45309"));
        } else if (item.category.equalsIgnoreCase("Waste")) {
            tagBg.setColor(Color.parseColor("#DCFCE7"));
            tag.setTextColor(Color.parseColor("#15803D"));
        } else {
            tagBg.setColor(Color.parseColor("#EDE9FE"));
            tag.setTextColor(Color.parseColor("#6D28D9"));
        }
        tag.setBackground(tagBg);

        TextView demoTag = new TextView(this);
        demoTag.setText("DEMO DATA");
        demoTag.setTextColor(Color.parseColor("#B45309"));
        demoTag.setTextSize(10);
        demoTag.setTypeface(null, Typeface.BOLD);
        demoTag.setPadding(dp(10), dp(3), 0, 0);

        top.addView(tag);
        top.addView(demoTag);
        card.addView(top);

        // Title
        TextView name = new TextView(this);
        name.setText(item.name);
        name.setTextSize(16);
        name.setTextColor(Color.parseColor("#0F172A"));
        name.setTypeface(null, Typeface.BOLD);
        name.setPadding(0, dp(6), 0, dp(2));
        card.addView(name);

        // Authority & Schedule
        TextView auth = new TextView(this);
        auth.setText("Authority / Center: " + item.authority);
        auth.setTextSize(13);
        auth.setTextColor(Color.parseColor("#475569"));
        card.addView(auth);

        TextView hrs = new TextView(this);
        hrs.setText("Operating Hours: " + item.timings);
        hrs.setTextSize(12);
        hrs.setTextColor(Color.parseColor("#0F766E"));
        hrs.setTypeface(null, Typeface.BOLD);
        hrs.setPadding(0, dp(2), 0, dp(4));
        card.addView(hrs);

        TextView det = new TextView(this);
        det.setText(item.details);
        det.setTextSize(12);
        det.setTextColor(Color.parseColor("#334155"));
        det.setPadding(0, 0, 0, dp(8));
        card.addView(det);

        // Action Buttons Row
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        if (item.isReportShortcut) {
            Button btnReport = new Button(this);
            btnReport.setText("Report Issue (Photo & GPS)");
            btnReport.setTextSize(12);
            btnReport.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1D4ED8")));
            btnReport.setTextColor(Color.WHITE);
            btnReport.setOnClickListener(v -> {
                startActivity(new Intent(PublicUtilitiesActivity.this, ReportActivity.class));
            });
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            btnReport.setLayoutParams(btnParams);
            actions.addView(btnReport);
        }

        if (item.hasMapLocation) {
            Button btnMap = new Button(this);
            btnMap.setText("View on Map");
            btnMap.setTextSize(12);
            btnMap.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0284C7")));
            btnMap.setTextColor(Color.WHITE);
            btnMap.setOnClickListener(v -> {
                Intent mapIntent = new Intent(PublicUtilitiesActivity.this, CityMapActivity.class);
                mapIntent.putExtra("DEST_LAT", item.latitude);
                mapIntent.putExtra("DEST_LON", item.longitude);
                mapIntent.putExtra("DEST_NAME", item.name);
                startActivity(mapIntent);
            });
            LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            btnMap.setLayoutParams(mapParams);
            actions.addView(btnMap);
        }

        if (item.phone != null && !item.phone.isEmpty()) {
            Button btnCall = new Button(this);
            btnCall.setText("Call (" + item.phone + ")");
            btnCall.setTextSize(12);
            btnCall.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1E293B")));
            btnCall.setTextColor(Color.WHITE);
            btnCall.setOnClickListener(v -> {
                String cleanPhone = item.phone.replaceAll("[^0-9]", "");
                Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + cleanPhone));
                startActivity(callIntent);
            });
            LinearLayout.LayoutParams callParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            btnCall.setLayoutParams(callParams);
            actions.addView(btnCall);
        }

        card.addView(actions);
        layoutUtilContainer.addView(card);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class UtilityItem {
        String name, category, authority, timings, details, phone;
        boolean isReportShortcut, hasMapLocation;
        double latitude, longitude;

        UtilityItem(String name, String category, String authority, String timings,
                    String details, String phone, boolean isReportShortcut,
                    boolean hasMapLocation, double latitude, double longitude) {
            this.name = name;
            this.category = category;
            this.authority = authority;
            this.timings = timings;
            this.details = details;
            this.phone = phone;
            this.isReportShortcut = isReportShortcut;
            this.hasMapLocation = hasMapLocation;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
