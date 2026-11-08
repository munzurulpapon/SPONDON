package com.example.spondon;

import android.content.Intent;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServicesActivity extends AppCompatActivity {

    private EditText etSearchServices;
    private TextView tvServicesCount, tvNoServices;
    private ProgressBar progressServices;
    private LinearLayout layoutServicesContainer;

    private Button btnCatAll, btnCatElectrician, btnCatPlumbing, btnCatCleaning, btnCatRepair;

    private FirebaseFirestore firestore;
    private ListenerRegistration servicesListener;

    private final List<ServiceItem> allServices = new ArrayList<>();
    private String selectedCategory = "All";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services);

        firestore = FirebaseFirestore.getInstance();

        initializeViews();
        setupFilters();
        setupSearch();

        // Check if an initial category filter was passed in the intent
        String initialFilter = getIntent().getStringExtra("FILTER_CATEGORY");
        if (initialFilter != null) {
            selectedCategory = initialFilter;
        }

        loadSeedServices();
        displayServices();   // show the built-in services immediately, even offline
        listenToFirestoreServices();
    }

    private void initializeViews() {
        etSearchServices = findViewById(R.id.etSearchServices);
        tvServicesCount = findViewById(R.id.tvServicesCount);
        tvNoServices = findViewById(R.id.tvNoServices);
        progressServices = findViewById(R.id.progressServices);
        layoutServicesContainer = findViewById(R.id.layoutServicesContainer);

        btnCatAll = findViewById(R.id.btnCatAll);
        btnCatElectrician = findViewById(R.id.btnCatElectrician);
        btnCatPlumbing = findViewById(R.id.btnCatPlumbing);
        btnCatCleaning = findViewById(R.id.btnCatCleaning);
        btnCatRepair = findViewById(R.id.btnCatRepair);

        View btnBack = findViewById(R.id.btnBackServices);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View cardHealth = findViewById(R.id.cardCatHealthcare);
        if (cardHealth != null) {
            ClickGuard.bind(this, cardHealth, null, HealthcareActivity.class);
        }

        View cardTransit = findViewById(R.id.cardCatTransportation);
        if (cardTransit != null) {
            ClickGuard.bind(this, cardTransit, null, TransportationActivity.class);
        }

        View cardUtils = findViewById(R.id.cardCatUtilities);
        if (cardUtils != null) {
            ClickGuard.bind(this, cardUtils, null, PublicUtilitiesActivity.class);
        }
    }

    private void setupFilters() {
        btnCatAll.setOnClickListener(v -> setFilter("All"));
        btnCatElectrician.setOnClickListener(v -> setFilter("Electrician"));
        btnCatPlumbing.setOnClickListener(v -> setFilter("Plumbing"));
        btnCatCleaning.setOnClickListener(v -> setFilter("Cleaning"));
        btnCatRepair.setOnClickListener(v -> setFilter("Repairs"));
    }

    private void setFilter(String category) {
        selectedCategory = category;
        displayServices();
    }

    private void setupSearch() {
        etSearchServices.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                displayServices();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadSeedServices() {
        allServices.clear();

        allServices.add(new ServiceItem("srv_1", "Home & Office Wiring Inspection", "Electrician",
                "Full inspection of main breaker, switchboards, load balancing and short-circuit prevention.",
                "Khulna Power Tech", "01711-234567", "৳500", "4.8 ★", "98% On-Time"));

        allServices.add(new ServiceItem("srv_2", "Emergency Circuit & Fuse Repair", "Electrician",
                "24/7 fast dispatch for sudden power trips, burned fuses and sparking wire emergencies.",
                "City Electric Care", "01912-345678", "৳400", "4.9 ★", "Within 30 mins"));

        allServices.add(new ServiceItem("srv_3", "Pipe Leakage & Drainage Unclogging", "Plumbing",
                "Rapid repair for burst pipes, kitchen sink clogs, bathroom drainage and pressure valves.",
                "Quick Flow Plumbers", "01819-456789", "৳450", "4.7 ★", "Certified Tools"));

        allServices.add(new ServiceItem("srv_4", "Water Tank & Pump Installation", "Plumbing",
                "Submersible pump setup, water supply pipeline repair, overflow sensor configuration.",
                "HydroPro Services", "01723-567890", "৳800", "4.9 ★", "1 Month Warranty"));

        allServices.add(new ServiceItem("srv_5", "Deep Home & Floor Sanitization", "Cleaning",
                "Eco-friendly antibacterial deep cleaning for apartments, tile scrub and kitchen grease removal.",
                "EcoClean Bangladesh", "01634-678901", "৳1,200", "4.8 ★", "Verified Staff"));

        allServices.add(new ServiceItem("srv_6", "Water Tank Cleaning & Treatment", "Cleaning",
                "Underground and rooftop water reservoir chlorine disinfection and sediment sludge evacuation.",
                "PureWater Hygiene", "01545-789012", "৳900", "4.6 ★", "Pure Quality"));

        allServices.add(new ServiceItem("srv_7", "AC Servicing, Gas Refill & Cooling Fix", "Repairs",
                "Full AC jet pump wash, indoor/outdoor coil cleanup, refrigerant top-up and PCB diagnostics.",
                "CoolBreeze AC Solutions", "01756-890123", "৳750", "4.9 ★", "Same Day Service"));

        allServices.add(new ServiceItem("srv_8", "Refrigerator & Appliance Diagnostics", "Repairs",
                "Repair and maintenance for fridges, washing machines, microwaves and water heaters.",
                "Smart Fix Appliance Care", "01867-901234", "৳600", "4.7 ★", "Original Parts"));
    }

    private void listenToFirestoreServices() {
        progressServices.setVisibility(View.VISIBLE);

        servicesListener = firestore.collection("services")
                .addSnapshotListener((value, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    progressServices.setVisibility(View.GONE);

                    if (error != null || value == null) {
                        displayServices();
                        return;
                    }

                    // Re-seed base services first
                    loadSeedServices();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String name = doc.getString("name");
                        String category = doc.getString("category");
                        String desc = doc.getString("description");

                        if (name != null) {
                            allServices.add(new ServiceItem(
                                    doc.getId(),
                                    name,
                                    category != null ? category : "General",
                                    desc != null ? desc : "City municipal certified service.",
                                    "Municipal Service Partner",
                                    "01700-000000",
                                    "Standard Rate",
                                    "4.8 ★",
                                    "Verified"
                            ));
                        }
                    }

                    displayServices();
                });
    }

    private void displayServices() {
        layoutServicesContainer.removeAllViews();
        int visibleCount = 0;

        for (ServiceItem item : allServices) {
            boolean matchesCat = "All".equalsIgnoreCase(selectedCategory)
                    || item.category.toLowerCase(Locale.ROOT).contains(selectedCategory.toLowerCase(Locale.ROOT))
                    || selectedCategory.toLowerCase(Locale.ROOT).contains(item.category.toLowerCase(Locale.ROOT));

            boolean matchesSearch = item.name.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || item.description.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || item.category.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || item.providerName.toLowerCase(Locale.ROOT).contains(searchQuery);

            if (matchesCat && matchesSearch) {
                addServiceCard(item);
                visibleCount++;
            }
        }

        tvServicesCount.setText("Available Services (" + visibleCount + ")");
        tvNoServices.setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
    }

    private void addServiceCard(ServiceItem service) {
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

        // Category Tag & Rating row
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView tag = new TextView(this);
        tag.setText(service.category.toUpperCase(Locale.ROOT));
        tag.setTextColor(Color.parseColor("#0F766E"));
        tag.setTextSize(11);
        tag.setTypeface(null, Typeface.BOLD);
        tag.setBackgroundColor(Color.parseColor("#CCFBF1"));
        tag.setPadding(dp(8), dp(4), dp(8), dp(4));

        TextView rating = new TextView(this);
        rating.setText(service.rating);
        rating.setTextColor(Color.parseColor("#B45309"));
        rating.setTextSize(12);
        rating.setTypeface(null, Typeface.BOLD);
        rating.setPadding(dp(12), dp(2), 0, 0);

        topRow.addView(tag);
        topRow.addView(rating);
        card.addView(topRow);

        // Title
        TextView title = new TextView(this);
        title.setText(service.name);
        title.setTextSize(17);
        title.setTextColor(Color.parseColor("#0F172A"));
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, dp(8), 0, dp(4));
        card.addView(title);

        // Provider Name & Badge
        TextView provider = new TextView(this);
        provider.setText("By " + service.providerName + " • " + service.badge);
        provider.setTextSize(13);
        provider.setTextColor(Color.parseColor("#64748B"));
        provider.setPadding(0, 0, 0, dp(6));
        card.addView(provider);

        // Description
        TextView desc = new TextView(this);
        desc.setText(service.description);
        desc.setTextSize(13);
        desc.setTextColor(Color.parseColor("#475569"));
        desc.setPadding(0, 0, 0, dp(10));
        card.addView(desc);

        // Pricing & Action Buttons
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView price = new TextView(this);
        price.setText(service.price);
        price.setTextSize(16);
        price.setTextColor(Color.parseColor("#147D64"));
        price.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        price.setLayoutParams(priceParams);

        Button btnDetails = new Button(this);
        btnDetails.setText("Details");
        btnDetails.setTextSize(12);
        btnDetails.setOnClickListener(v -> {
            Intent intent = new Intent(ServicesActivity.this, ServiceDetailsActivity.class);
            intent.putExtra("SERVICE_ID", service.id);
            intent.putExtra("SERVICE_NAME", service.name);
            intent.putExtra("SERVICE_CATEGORY", service.category);
            intent.putExtra("SERVICE_DESC", service.description);
            intent.putExtra("SERVICE_PROVIDER", service.providerName);
            intent.putExtra("SERVICE_PHONE", service.phone);
            intent.putExtra("SERVICE_PRICE", service.price);
            intent.putExtra("SERVICE_RATING", service.rating);
            ClickGuard.open(ServicesActivity.this, intent);
        });

        Button btnBook = new Button(this);
        btnBook.setText("Book Now");
        btnBook.setTextSize(12);
        btnBook.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#147D64")));
        btnBook.setTextColor(Color.WHITE);
        btnBook.setOnClickListener(v -> {
            Intent intent = new Intent(ServicesActivity.this, BookServiceActivity.class);
            intent.putExtra("SERVICE_ID", service.id);
            intent.putExtra("SERVICE_NAME", service.name);
            intent.putExtra("SERVICE_CATEGORY", service.category);
            intent.putExtra("SERVICE_PRICE", service.price);
            ClickGuard.open(ServicesActivity.this, intent);
        });

        actionRow.addView(price);
        actionRow.addView(btnDetails);
        actionRow.addView(btnBook);
        card.addView(actionRow);

        layoutServicesContainer.addView(card);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (servicesListener != null) {
            servicesListener.remove();
        }
        super.onDestroy();
    }

    public static class ServiceItem {
        public String id, name, category, description, providerName, phone, price, rating, badge;

        public ServiceItem(String id, String name, String category, String description,
                           String providerName, String phone, String price, String rating, String badge) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.description = description;
            this.providerName = providerName;
            this.phone = phone;
            this.price = price;
            this.rating = rating;
            this.badge = badge;
        }
    }
}
