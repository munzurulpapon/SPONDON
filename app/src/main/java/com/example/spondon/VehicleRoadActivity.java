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

public class VehicleRoadActivity extends AppCompatActivity {

    private EditText etSearchRoad;
    private TextView tvVehicleCount, tvNoRoadResults;
    private LinearLayout layoutVehicleContainer;
    private Button btnQuickReportRoad, btnBackVehicleRoad;
    private Button btnRoadAll, btnRoadRepairs, btnRoadParking, btnRoadTraffic;

    private final List<RoadServiceItem> items = new ArrayList<>();
    private String selectedFilter = "All";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_road);

        etSearchRoad = findViewById(R.id.etSearchRoad);
        tvVehicleCount = findViewById(R.id.tvVehicleCount);
        tvNoRoadResults = findViewById(R.id.tvNoRoadResults);
        layoutVehicleContainer = findViewById(R.id.layoutVehicleContainer);
        btnQuickReportRoad = findViewById(R.id.btnQuickReportRoad);
        btnBackVehicleRoad = findViewById(R.id.btnBackVehicleRoad);

        btnRoadAll = findViewById(R.id.btnRoadAll);
        btnRoadRepairs = findViewById(R.id.btnRoadRepairs);
        btnRoadParking = findViewById(R.id.btnRoadParking);
        btnRoadTraffic = findViewById(R.id.btnRoadTraffic);

        btnBackVehicleRoad.setOnClickListener(v -> finish());

        // Quick report road issue launches ReportActivity directly!
        btnQuickReportRoad.setOnClickListener(v -> {
            startActivity(new Intent(VehicleRoadActivity.this, ReportActivity.class));
        });

        setupFilters();
        setupSearch();
        loadData();
        displayItems();
    }

    private void setupFilters() {
        btnRoadAll.setOnClickListener(v -> setFilter("All"));
        btnRoadRepairs.setOnClickListener(v -> setFilter("Repairs"));
        btnRoadParking.setOnClickListener(v -> setFilter("Parking"));
        btnRoadTraffic.setOnClickListener(v -> setFilter("Traffic"));
    }

    private void setFilter(String filter) {
        selectedFilter = filter;
        displayItems();
    }

    private void setupSearch() {
        etSearchRoad.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                displayItems();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadData() {
        items.clear();

        // 1. Workshops & Towing
        items.add(new RoadServiceItem("Khulna Auto Care & 24/7 Breakdown Towing", "Repairs",
                "Majid Sarani, Sonadanga, Khulna", "01711-998877", "24 Hours Breakdown Service",
                "Hydraulic recovery towing, engine diagnostic, battery jumpstart, computer scanner.", 22.8220, 89.5470));

        items.add(new RoadServiceItem("SpeedZone Motorcycle Servicing & Tune-up", "Repairs",
                "KDA New Market Road, Khulna", "01922-334455", "08:30 AM - 09:30 PM",
                "Fuel injection tuning, brake pad overhaul, tire replacement, computerized wheel alignment.", 22.8190, 89.5530));

        items.add(new RoadServiceItem("City Rapid Puncture & Tubeless Fix", "Repairs",
                "Rupsha Traffic Mor, Khulna", "01833-445566", "24/7 Emergency",
                "Tubeless tire patching, nitrogen inflation, high-pressure tire inflation for cars and trucks.", 22.8050, 89.5750));

        items.add(new RoadServiceItem("EcoShine Water-Recycling Car Wash", "Repairs",
                "Boyra Main Road, Khulna", "01755-667788", "08:00 AM - 08:00 PM",
                "High-pressure foam wash, interior vacuuming, ceramic coating and underbody anti-rust wash.", 22.8420, 89.5390));

        // 2. Parking Facilities
        items.add(new RoadServiceItem("KDA Multi-Level Smart Parking Complex", "Parking",
                "Shibbari Mor, KDA Avenue, Khulna", "041-720100", "24 Hours Open",
                "Capacity: 250 Cars, 400 Bikes • Rate: ৳20/hr Cars, ৳10/hr Bikes • [Live Status: 85 Spaces Available]", 22.8280, 89.5540));

        items.add(new RoadServiceItem("Sonadanga Bus Terminal Designated Vehicle Parking", "Parking",
                "Sonadanga Central Terminal, Khulna", "041-730200", "24 Hours Open",
                "Capacity: 150 Vehicles • Overnight secure parking with CCTV surveillance • [Live Status: Spaces Available]", 22.8195, 89.5445));

        items.add(new RoadServiceItem("Khulna New Market Authorized Parking Zone", "Parking",
                "KDA New Market East Gate, Khulna", "01700-112233", "09:00 AM - 10:00 PM",
                "Capacity: 120 Bikes, 40 Cars • Rate: ৳15 Flat Bike Parking • [Live Status: Limited Spaces]", 22.8200, 89.5550));

        items.add(new RoadServiceItem("Railway Station East Gate Public Parking", "Parking",
                "Khulna Railway Station Approach, Khulna", "041-721200", "24 Hours Open",
                "Capacity: 80 Cars, 200 Bikes • Secure commuter parking • [Live Status: 30 Spaces Available]", 22.8150, 89.5620));

        // 3. Traffic & Road Maintenance Alerts
        items.add(new RoadServiceItem("Road Maintenance: Khan Jahan Ali Road Drainage Overhaul", "Traffic",
                "Khan Jahan Ali Road (Rupsha to Royal Mor)", "N/A", "Active Project • Expected completion in 3 weeks",
                "One lane closed for storm drainage installation. Heavy vehicles diverted via Bypass Road.", 22.8110, 89.5610));

        items.add(new RoadServiceItem("Traffic Notice: Sonadanga Circular Diversion for Flyover Construction", "Traffic",
                "Sonadanga Bypass Intersection", "N/A", "Peak Hours: 08:30 AM - 11:00 AM & 05:00 PM - 08:00 PM",
                "Slow-moving traffic due to heavy machinery. Traffic police deployed for manual signaling.", 22.8240, 89.5380));

        items.add(new RoadServiceItem("Road Hazard Alert: Waterlogging after Heavy Rainfall", "Traffic",
                "Gollamari Bridge Approach Lowland", "N/A", "Temporary Weather Advisory",
                "Clogged roadside culvert. Municipal pump teams deployed. Motorists advised to reduce speed.", 22.8020, 89.5390));
    }

    private void displayItems() {
        layoutVehicleContainer.removeAllViews();
        int visibleCount = 0;

        for (RoadServiceItem item : items) {
            boolean matchesCategory = selectedFilter.equals("All") || item.category.equalsIgnoreCase(selectedFilter);
            boolean matchesSearch = searchQuery.isEmpty()
                    || item.name.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || item.address.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || item.details.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || item.category.toLowerCase(Locale.ROOT).contains(searchQuery);

            if (matchesCategory && matchesSearch) {
                renderCard(item);
                visibleCount++;
            }
        }

        tvVehicleCount.setText("Road & Mobility Directory (" + visibleCount + " items)");

        if (visibleCount == 0) {
            tvNoRoadResults.setVisibility(View.VISIBLE);
        } else {
            tvNoRoadResults.setVisibility(View.GONE);
        }
    }

    private void renderCard(RoadServiceItem item) {
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

        // Header Row: Category Badge + DEMO tag
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView tag = new TextView(this);
        tag.setText(item.category.toUpperCase(Locale.ROOT));
        tag.setTextSize(11);
        tag.setTypeface(null, Typeface.BOLD);
        tag.setPadding(dp(8), dp(3), dp(8), dp(3));

        GradientDrawable tagBg = new GradientDrawable();
        tagBg.setCornerRadius(dp(4));

        if (item.category.equalsIgnoreCase("Repairs")) {
            tagBg.setColor(Color.parseColor("#FEF3C7"));
            tag.setTextColor(Color.parseColor("#92400E"));
        } else if (item.category.equalsIgnoreCase("Parking")) {
            tagBg.setColor(Color.parseColor("#DBEAFE"));
            tag.setTextColor(Color.parseColor("#1E40AF"));
        } else {
            tagBg.setColor(Color.parseColor("#FEE2E2"));
            tag.setTextColor(Color.parseColor("#991B1B"));
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

        // Name
        TextView name = new TextView(this);
        name.setText(item.name);
        name.setTextSize(16);
        name.setTextColor(Color.parseColor("#0F172A"));
        name.setTypeface(null, Typeface.BOLD);
        name.setPadding(0, dp(6), 0, dp(2));
        card.addView(name);

        // Location & Hours
        TextView loc = new TextView(this);
        loc.setText("Location: " + item.address);
        loc.setTextSize(13);
        loc.setTextColor(Color.parseColor("#475569"));
        card.addView(loc);

        TextView hrs = new TextView(this);
        hrs.setText("Operational Hours: " + item.hours);
        hrs.setTextSize(12);
        hrs.setTextColor(Color.parseColor("#0F766E"));
        hrs.setTypeface(null, Typeface.BOLD);
        hrs.setPadding(0, dp(2), 0, dp(4));
        card.addView(hrs);

        TextView det = new TextView(this);
        det.setText(item.details);
        det.setTextSize(12);
        det.setTextColor(Color.parseColor("#64748B"));
        det.setPadding(0, 0, 0, dp(8));
        card.addView(det);

        // Action Buttons Row
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        if (!item.phone.equals("N/A")) {
            Button btnCall = new Button(this);
            btnCall.setText("Call (" + item.phone + ")");
            btnCall.setTextSize(12);
            btnCall.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1E293B")));
            btnCall.setTextColor(Color.WHITE);
            btnCall.setOnClickListener(v -> {
                Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + item.phone.replace("-", "")));
                startActivity(callIntent);
            });
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            btnCall.setLayoutParams(btnParams);
            actions.addView(btnCall);
        }

        Button btnMap = new Button(this);
        btnMap.setText("View on City Map");
        btnMap.setTextSize(12);
        btnMap.setOnClickListener(v -> {
            Intent mapIntent = new Intent(VehicleRoadActivity.this, CityMapActivity.class);
            mapIntent.putExtra("DEST_LAT", item.latitude);
            mapIntent.putExtra("DEST_LON", item.longitude);
            mapIntent.putExtra("DEST_NAME", item.name);
            startActivity(mapIntent);
        });
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnMap.setLayoutParams(mapParams);
        actions.addView(btnMap);

        card.addView(actions);
        layoutVehicleContainer.addView(card);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class RoadServiceItem {
        String name, category, address, phone, hours, details;
        double latitude, longitude;

        RoadServiceItem(String name, String category, String address, String phone,
                        String hours, String details, double latitude, double longitude) {
            this.name = name;
            this.category = category;
            this.address = address;
            this.phone = phone;
            this.hours = hours;
            this.details = details;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
