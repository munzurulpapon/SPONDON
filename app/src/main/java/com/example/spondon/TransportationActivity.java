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

public class TransportationActivity extends AppCompatActivity {

    private EditText etSearchTransport;
    private TextView tvTransCount, tvNoTransResults;
    private LinearLayout layoutTransContainer;
    private Button btnBackTransport;

    private Button btnTransAll, btnTransCityBus, btnTransIntercity, btnTransTrain, btnTransFerry;

    private final List<TransportItem> transportList = new ArrayList<>();
    private String selectedCategory = "All";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transportation);

        etSearchTransport = findViewById(R.id.etSearchTransport);
        tvTransCount = findViewById(R.id.tvTransCount);
        tvNoTransResults = findViewById(R.id.tvNoTransResults);
        layoutTransContainer = findViewById(R.id.layoutTransContainer);
        btnBackTransport = findViewById(R.id.btnBackTransport);

        btnTransAll = findViewById(R.id.btnTransAll);
        btnTransCityBus = findViewById(R.id.btnTransCityBus);
        btnTransIntercity = findViewById(R.id.btnTransIntercity);
        btnTransTrain = findViewById(R.id.btnTransTrain);
        btnTransFerry = findViewById(R.id.btnTransFerry);

        btnBackTransport.setOnClickListener(v -> finish());

        setupFilters();
        setupSearch();
        loadTransportData();
        displayTransport();
    }

    private void setupFilters() {
        btnTransAll.setOnClickListener(v -> setCategory("All"));
        btnTransCityBus.setOnClickListener(v -> setCategory("City Bus"));
        btnTransIntercity.setOnClickListener(v -> setCategory("Intercity"));
        btnTransTrain.setOnClickListener(v -> setCategory("Train"));
        btnTransFerry.setOnClickListener(v -> setCategory("Ferry"));
    }

    private void setCategory(String category) {
        selectedCategory = category;
        displayTransport();
    }

    private void setupSearch() {
        etSearchTransport.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                displayTransport();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadTransportData() {
        transportList.clear();

        // 1. City Bus Routes
        transportList.add(new TransportItem(
                "Nagar Paribahan Route 1 (Rupsha to Phulbarigate)",
                "City Bus",
                "Rupsha Traffic Mor Bus Bay, Khulna",
                "06:00 AM - 10:00 PM • Every 10 mins",
                "৳15 - ৳35 depending on distance",
                "Stops: Rupsha → Royal Mor → Shibbari → Daulatpur → KUET Gate → Phulbarigate.",
                "041-720880",
                22.8055, 89.5740
        ));

        transportList.add(new TransportItem(
                "City Circular Bus Route 2 (Sonadanga to Boyra)",
                "City Bus",
                "Sonadanga Central Bus Terminal, Khulna",
                "06:30 AM - 09:30 PM • Every 15 mins",
                "৳15 - ৳25 Flat Fare",
                "Stops: Sonadanga → New Market → Shibbari → Moylapota → KMCH Hospital → Boyra.",
                "041-730150",
                22.8195, 89.5445
        ));

        transportList.add(new TransportItem(
                "Dakbangla - Gollamari University Feeder Route 3",
                "City Bus",
                "Dakbangla Central Roundabout, Khulna",
                "07:00 AM - 09:00 PM • Every 12 mins",
                "৳10 - ৳20 Student Concession Available",
                "Stops: Dakbangla → Moilapota → Sonadanga Bypass → Khulna University Main Gate → Gollamari.",
                "041-721400",
                22.8160, 89.5590
        ));

        // 2. Intercity Coaches (Sonadanga Hub)
        transportList.add(new TransportItem(
                "Dhaka Express Coaches (via Padma Bridge)",
                "Intercity",
                "Sonadanga Inter-district Coach Terminal, Khulna",
                "Departures: Every 30 mins (05:00 AM - 11:45 PM)",
                "AC: ৳900 - ৳1300 • Non-AC: ৳600 - ৳700",
                "Travel Time: ~3.5 to 4 hours via Padma Multipurpose Bridge. Operators: Green Line, Shohagh, Eagle, Hanif, Tungipara Express.",
                "01711-123456",
                22.8195, 89.5445
        ));

        transportList.add(new TransportItem(
                "Khulna - Chittagong Direct Super Coach",
                "Intercity",
                "KDA Avenue Counter Hub, Khulna",
                "Daily Departures: 07:00 AM, 08:30 PM, 10:00 PM",
                "AC Slepper: ৳1600 • Regular AC: ৳1250",
                "Route: Khulna → Gopalganj → Dhaka Ring Road → Comilla → Chittagong Dampara. Operator: Hanif / Shohagh.",
                "01912-345678",
                22.8250, 89.5510
        ));

        transportList.add(new TransportItem(
                "Khulna - Barisal Coastal Route Coaches",
                "Intercity",
                "Rupsha Bus Stand, East Bank, Khulna",
                "Daily Departures: 06:00 AM - 06:00 PM (Every 1 hour)",
                "৳350 - ৳450",
                "Route: Khulna → Bagerhat → Pirojpur → Jhalakathi → Barisal Central Rupatoli.",
                "01819-876543",
                22.8090, 89.5780
        ));

        // 3. Railway (Trains)
        transportList.add(new TransportItem(
                "Sundarban Express (Train No: 765/766) - Dhaka Route",
                "Train",
                "Khulna Central Railway Station, Station Road",
                "Departs Khulna: 10:15 PM • Arrives Kamalapur: 05:10 AM",
                "Shovon: ৳420 • Snigdha (AC): ৳805 • AC Berth: ৳1210",
                "Days Off: Tuesday from Khulna. Features modern broad-gauge vacuum-braked air-conditioned coaches.",
                "041-721200",
                22.8152, 89.5623
        ));

        transportList.add(new TransportItem(
                "Chitra Express (Train No: 763/764) - Morning Intercity",
                "Train",
                "Khulna Central Railway Station, Station Road",
                "Departs Khulna: 09:00 AM • Arrives Dhaka: 05:30 PM",
                "Shovon: ৳420 • Snigdha (AC): ৳805",
                "Days Off: Monday from Khulna. Major stops: Jessore, Kotchandpur, Chuadanga, Poradaha, Ishwardi Bypass, Bangabandhu Bridge.",
                "041-721200",
                22.8152, 89.5623
        ));

        transportList.add(new TransportItem(
                "Benapole Express (Train No: 795) - Land Port Connector",
                "Train",
                "Khulna Central Railway Station, Station Road",
                "Daily Departures: 06:30 AM & 04:45 PM",
                "Fare: ৳85 - ৳160",
                "Direct commuter link from Khulna to India-Bangladesh Benapole International Land Checkpost border.",
                "041-721200",
                22.8152, 89.5623
        ));

        // 4. River & Ferry Terminals
        transportList.add(new TransportItem(
                "Rupsha River Vehicle & Commuter Ferry",
                "Ferry",
                "Rupsha Ferry Ghat, West & East Banks, Khulna",
                "24 Hours Continuous Service • Ferry every 15 mins",
                "Pedestrian: ৳5 • Motorbike: ৳15 • Auto/Car: ৳60",
                "Connects Khulna Metropolitan city with Rupsha East Industrial and Bagerhat highway bypass.",
                "041-760111",
                22.8060, 89.5765
        ));

        transportList.add(new TransportItem(
                "Khulna 4 No. Launch Terminal (Sundarbans & Coastal)",
                "Ferry",
                "Bhairab River Ghat No. 4, Khulna",
                "Daily Departures: 07:30 AM (Dacope), 12:00 PM (Koyra), 03:30 PM (Mongla)",
                "Deck: ৳60 - ৳150 • Cabin: ৳400 - ৳700",
                "Passenger motor launches connecting southern riverine delta, Dacope, Chalna port, and Sundarbans waterways.",
                "041-723344",
                22.8210, 89.5660
        ));
    }

    private void displayTransport() {
        layoutTransContainer.removeAllViews();
        int visibleCount = 0;

        for (TransportItem item : transportList) {
            boolean matchesCategory = selectedCategory.equals("All")
                    || (selectedCategory.equals("City Bus") && item.mode.equalsIgnoreCase("City Bus"))
                    || (selectedCategory.equals("Intercity") && item.mode.equalsIgnoreCase("Intercity"))
                    || (selectedCategory.equals("Train") && item.mode.equalsIgnoreCase("Train"))
                    || (selectedCategory.equals("Ferry") && item.mode.equalsIgnoreCase("Ferry"));

            boolean matchesSearch = searchQuery.isEmpty()
                    || item.name.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || item.terminal.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || item.stops.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || item.mode.toLowerCase(Locale.ROOT).contains(searchQuery);

            if (matchesCategory && matchesSearch) {
                renderTransportCard(item);
                visibleCount++;
            }
        }

        tvTransCount.setText("Available Routes & Schedules (" + visibleCount + " options)");

        if (visibleCount == 0) {
            tvNoTransResults.setVisibility(View.VISIBLE);
        } else {
            tvNoTransResults.setVisibility(View.GONE);
        }
    }

    private void renderTransportCard(TransportItem item) {
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

        // Header: Mode Tag + DEMO tag
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView tag = new TextView(this);
        tag.setText(item.mode.toUpperCase(Locale.ROOT));
        tag.setTextSize(11);
        tag.setTypeface(null, Typeface.BOLD);
        tag.setPadding(dp(8), dp(3), dp(8), dp(3));

        GradientDrawable tagBg = new GradientDrawable();
        tagBg.setCornerRadius(dp(4));

        if (item.mode.equalsIgnoreCase("City Bus")) {
            tagBg.setColor(Color.parseColor("#E0F2FE"));
            tag.setTextColor(Color.parseColor("#0369A1"));
        } else if (item.mode.equalsIgnoreCase("Intercity")) {
            tagBg.setColor(Color.parseColor("#EDE9FE"));
            tag.setTextColor(Color.parseColor("#6D28D9"));
        } else if (item.mode.equalsIgnoreCase("Train")) {
            tagBg.setColor(Color.parseColor("#FEF3C7"));
            tag.setTextColor(Color.parseColor("#B45309"));
        } else {
            tagBg.setColor(Color.parseColor("#CCFBF1"));
            tag.setTextColor(Color.parseColor("#0F766E"));
        }
        tag.setBackground(tagBg);

        TextView demoTag = new TextView(this);
        demoTag.setText("DEMO SCHEDULE");
        demoTag.setTextColor(Color.parseColor("#B45309"));
        demoTag.setTextSize(10);
        demoTag.setTypeface(null, Typeface.BOLD);
        demoTag.setPadding(dp(10), dp(3), 0, 0);

        top.addView(tag);
        top.addView(demoTag);
        card.addView(top);

        // Route Name
        TextView name = new TextView(this);
        name.setText(item.name);
        name.setTextSize(16);
        name.setTextColor(Color.parseColor("#0F172A"));
        name.setTypeface(null, Typeface.BOLD);
        name.setPadding(0, dp(6), 0, dp(2));
        card.addView(name);

        // Terminal & Timings
        TextView terminal = new TextView(this);
        terminal.setText("Departure Hub: " + item.terminal);
        terminal.setTextSize(13);
        terminal.setTextColor(Color.parseColor("#475569"));
        card.addView(terminal);

        TextView timing = new TextView(this);
        timing.setText("Schedule: " + item.timings);
        timing.setTextSize(13);
        timing.setTextColor(Color.parseColor("#0284C7"));
        timing.setTypeface(null, Typeface.BOLD);
        timing.setPadding(0, dp(2), 0, dp(2));
        card.addView(timing);

        TextView fare = new TextView(this);
        fare.setText("Ticket Fare: " + item.fare);
        fare.setTextSize(12);
        fare.setTextColor(Color.parseColor("#059669"));
        fare.setTypeface(null, Typeface.BOLD);
        card.addView(fare);

        TextView stops = new TextView(this);
        stops.setText(item.stops);
        stops.setTextSize(12);
        stops.setTextColor(Color.parseColor("#64748B"));
        stops.setPadding(0, dp(4), 0, dp(10));
        card.addView(stops);

        // Action Buttons Row
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button btnMap = new Button(this);
        btnMap.setText("View Route / Station on Map");
        btnMap.setTextSize(12);
        btnMap.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0284C7")));
        btnMap.setTextColor(Color.WHITE);
        btnMap.setOnClickListener(v -> {
            Intent mapIntent = new Intent(TransportationActivity.this, CityMapActivity.class);
            mapIntent.putExtra("DEST_LAT", item.latitude);
            mapIntent.putExtra("DEST_LON", item.longitude);
            mapIntent.putExtra("DEST_NAME", item.name);
            startActivity(mapIntent);
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnMap.setLayoutParams(btnParams);
        actions.addView(btnMap);

        if (item.helpline != null && !item.helpline.isEmpty()) {
            Button btnCall = new Button(this);
            btnCall.setText("Call (" + item.helpline + ")");
            btnCall.setTextSize(12);
            btnCall.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#334155")));
            btnCall.setTextColor(Color.WHITE);
            btnCall.setOnClickListener(v -> {
                Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + item.helpline.replace("-", "")));
                startActivity(callIntent);
            });
            LinearLayout.LayoutParams callParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            btnCall.setLayoutParams(callParams);
            actions.addView(btnCall);
        }

        card.addView(actions);
        layoutTransContainer.addView(card);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class TransportItem {
        String name, mode, terminal, timings, fare, stops, helpline;
        double latitude, longitude;

        TransportItem(String name, String mode, String terminal, String timings,
                      String fare, String stops, String helpline, double latitude, double longitude) {
            this.name = name;
            this.mode = mode;
            this.terminal = terminal;
            this.timings = timings;
            this.fare = fare;
            this.stops = stops;
            this.helpline = helpline;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
