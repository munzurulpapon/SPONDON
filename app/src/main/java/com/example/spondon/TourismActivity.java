package com.example.spondon;

import android.content.ContentValues;
import android.content.Intent;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TourismActivity extends AppCompatActivity {

    private EditText etSearchTourism;
    private TextView tvTourismCount, tvNoTourism;
    private LinearLayout layoutTourismContainer;
    private Button btnBackTourism;

    private Button btnTourAll, btnTourHistory, btnTourParks, btnTourAttractions, btnTourFavs;

    private DatabaseHelper databaseHelper;

    private final List<TouristSpot> spotList = new ArrayList<>();
    private final Set<String> favouriteIds = new HashSet<>();
    private String selectedCategory = "All";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tourism);

        databaseHelper = new DatabaseHelper(this);

        etSearchTourism = findViewById(R.id.etSearchTourism);
        tvTourismCount = findViewById(R.id.tvTourismCount);
        tvNoTourism = findViewById(R.id.tvNoTourism);
        layoutTourismContainer = findViewById(R.id.layoutTourismContainer);
        btnBackTourism = findViewById(R.id.btnBackTourism);

        btnTourAll = findViewById(R.id.btnTourAll);
        btnTourHistory = findViewById(R.id.btnTourHistory);
        btnTourParks = findViewById(R.id.btnTourParks);
        btnTourAttractions = findViewById(R.id.btnTourAttractions);
        btnTourFavs = findViewById(R.id.btnTourFavs);

        btnBackTourism.setOnClickListener(v -> finish());

        setupFilters();
        setupSearch();
        loadSpots();
        loadFavourites();
        displaySpots();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavourites();
        displaySpots();
    }

    private void setupFilters() {
        btnTourAll.setOnClickListener(v -> setCategory("All"));
        btnTourHistory.setOnClickListener(v -> setCategory("Historical"));
        btnTourParks.setOnClickListener(v -> setCategory("Parks"));
        btnTourAttractions.setOnClickListener(v -> setCategory("Attractions"));
        btnTourFavs.setOnClickListener(v -> setCategory("Favourites"));
    }

    private void setCategory(String category) {
        selectedCategory = category;
        displaySpots();
    }

    private void setupSearch() {
        etSearchTourism.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                displaySpots();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadSpots() {
        spotList.clear();

        spotList.add(new TouristSpot("tour_1", "Khan Jahan Ali Bridge (Rupsha Bridge)", "Attractions",
                "Iconic cable-stayed bridge over Rupsha River. Popular for scenic river breeze, boat views and sunset.",
                "Rupsha Toll Plaza, Khulna", "Open 24 Hours", "Free (Pedestrian)", 22.7844, 89.5847,
                "Khan Jahan Ali Bridge connects Khulna city with southern districts. Constructed with modern engineering, it offers a breathtaking panorama of country boats, cargo vessels and mangrove riverbeds. Best visited in the late afternoon."));

        spotList.add(new TouristSpot("tour_2", "Shaheed Hadis Park", "Parks",
                "Centrally located historic park with a peaceful lake, lush trees, walking track and Shahid Minar monument.",
                "Babu Khan Road, Khulna Sadar", "06:00 AM - 09:00 PM", "Free Entry", 22.8125, 89.5620,
                "Named in honor of Hadisur Rahman, this park has been the cultural and political heartbeat of Khulna for decades. Features lighted fountains, senior citizen walking tracks and family seating."));

        spotList.add(new TouristSpot("tour_3", "Khulna Divisional Museum", "Historical",
                "Archaeological and regional museum showcasing ancient terracotta, Sultanate coins, medieval sculptures and folklore.",
                "Boyra Main Road, Khulna", "10:00 AM - 05:00 PM (Closed Sundays)", "৳20 Citizens, ৳100 Foreigners", 22.8360, 89.5440,
                "Established by the Department of Archaeology, the museum houses historical artifacts from across southern Bengal, including relics from Bagerhat's Sixty Dome Mosque and ancient Khulna civilizations."));

        spotList.add(new TouristSpot("tour_4", "Gollamari Memorial Monument (Badhya Bhumi)", "Historical",
                "Historic national monument commemorating the martyred freedom fighters and intellectuals of the 1971 Liberation War.",
                "Gollamari, Khulna", "Open Daily", "Free Entry", 22.8020, 89.5350,
                "A poignant national heritage site. During 1971, this area was the site of supreme sacrifice by heroic freedom fighters. A soaring star-shaped monument stands surrounded by reflection pools and gardens."));

        spotList.add(new TouristSpot("tour_5", "Rupsha Ghat & Riverside Promenade", "Attractions",
                "Vibrant traditional river terminal with local wooden trawlers, fish markets and fresh evening breeze.",
                "East Rupsha Ghat, Khulna", "05:00 AM - 11:00 PM", "Free / ৳5 Trawler crossing", 22.8080, 89.5780,
                "A bustling hub of riverine life. Travelers can hire traditional wooden country boats for sunset cruises on the Rupsha River, experience fresh local river fish cuisine and witness busy cargo operations."));

        spotList.add(new TouristSpot("tour_6", "Jahanabad Cantonment Deer Park", "Parks",
                "Eco-friendly deer sanctuary and botanical recreation park with spotted deer, peacocks and children playzone.",
                "Jahanabad Cantt, Gilatala, Khulna", "09:00 AM - 06:00 PM", "৳30 per person", 22.9150, 89.5120,
                "Surrounded by pine and mahogany trees, this well-maintained family park hosts dozens of friendly chital (spotted deer) in natural grasslands. Ideal for weekend family picnics."));

        spotList.add(new TouristSpot("tour_7", "Sundarbans Eco-Tourism Gateway Center", "Attractions",
                "Official briefing center and departure port for UNESCO World Heritage Sundarbans mangrove forest expeditions.",
                "Customs Ghat Road, Khulna", "08:00 AM - 06:00 PM", "Permit assistance available", 22.7950, 89.5650,
                "Khulna is the premier departure gateway to the Sundarbans. The center offers exhibits on the Royal Bengal Tiger, mangrove botany, tourist boat licensing and guided forest expeditions."));

        spotList.add(new TouristSpot("tour_8", "Shiromoni Historical Battlefield Memorial", "Historical",
                "Historic Tank-Battle site of December 1971 where allied freedom fighters defeated occupation armored forces.",
                "Shiromoni, Fulbarigate, Khulna", "Open Daily", "Free Entry", 22.9050, 89.5080,
                "The Battle of Shiromoni was one of the fiercest tank confrontations of the Liberation War, fighting until December 17. The preserved memorial stands on the historic battlefield."));
    }

    private void loadFavourites() {
        favouriteIds.clear();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT item_id FROM " + DatabaseHelper.TABLE_FAVOURITES, null);
        while (cursor.moveToNext()) {
            favouriteIds.add(cursor.getString(0));
        }
        cursor.close();
        db.close();
    }

    private void toggleFavourite(TouristSpot spot) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        if (favouriteIds.contains(spot.id)) {
            db.delete(DatabaseHelper.TABLE_FAVOURITES, "item_id = ?", new String[]{spot.id});
            favouriteIds.remove(spot.id);
            Toast.makeText(this, "Removed from Favourites", Toast.LENGTH_SHORT).show();
        } else {
            ContentValues cv = new ContentValues();
            cv.put("item_id", spot.id);
            cv.put("title", spot.name);
            cv.put("category", spot.category);
            cv.put("location", spot.location);
            cv.put("description", spot.description);
            db.insert(DatabaseHelper.TABLE_FAVOURITES, null, cv);
            favouriteIds.add(spot.id);
            Toast.makeText(this, "Saved to Favourites!", Toast.LENGTH_SHORT).show();
        }
        db.close();
        displaySpots();
    }

    private void displaySpots() {
        layoutTourismContainer.removeAllViews();
        int count = 0;

        for (TouristSpot spot : spotList) {
            boolean matchesCat;
            if ("Favourites".equalsIgnoreCase(selectedCategory)) {
                matchesCat = favouriteIds.contains(spot.id);
            } else if ("All".equalsIgnoreCase(selectedCategory)) {
                matchesCat = true;
            } else {
                matchesCat = spot.category.equalsIgnoreCase(selectedCategory);
            }

            boolean matchesSearch = spot.name.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || spot.description.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || spot.location.toLowerCase(Locale.ROOT).contains(searchQuery);

            if (matchesCat && matchesSearch) {
                addSpotCard(spot);
                count++;
            }
        }

        tvTourismCount.setText("Landmarks & Attractions (" + count + ")");
        tvNoTourism.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    private void addSpotCard(TouristSpot spot) {
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

        // Header: Category & Favourite Button
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView tag = new TextView(this);
        tag.setText(spot.category.toUpperCase(Locale.ROOT));
        tag.setTextColor(Color.parseColor("#0F766E"));
        tag.setTextSize(11);
        tag.setTypeface(null, Typeface.BOLD);
        tag.setBackgroundColor(Color.parseColor("#CCFBF1"));
        tag.setPadding(dp(8), dp(3), dp(8), dp(3));
        LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        tag.setLayoutParams(tagParams);

        Button btnFav = new Button(this);
        boolean isFav = favouriteIds.contains(spot.id);
        btnFav.setText(isFav ? "♥ Saved" : "♡ Save");
        btnFav.setTextSize(11);
        btnFav.setBackgroundColor(Color.TRANSPARENT);
        btnFav.setTextColor(isFav ? Color.parseColor("#DC2626") : Color.parseColor("#64748B"));
        btnFav.setOnClickListener(v -> toggleFavourite(spot));

        top.addView(tag);
        top.addView(btnFav);
        card.addView(top);

        // Name
        TextView name = new TextView(this);
        name.setText(spot.name);
        name.setTextSize(17);
        name.setTextColor(Color.parseColor("#0F172A"));
        name.setTypeface(null, Typeface.BOLD);
        name.setPadding(0, dp(4), 0, dp(2));
        card.addView(name);

        // Location & Hours
        TextView location = new TextView(this);
        location.setText("Location: " + spot.location);
        location.setTextSize(13);
        location.setTextColor(Color.parseColor("#475569"));
        card.addView(location);

        TextView hours = new TextView(this);
        hours.setText("Visiting Hours: " + spot.hours + " • Fee: " + spot.entryFee);
        hours.setTextSize(12);
        hours.setTextColor(Color.parseColor("#64748B"));
        hours.setPadding(0, dp(2), 0, dp(6));
        card.addView(hours);

        TextView desc = new TextView(this);
        desc.setText(spot.description);
        desc.setTextSize(13);
        desc.setTextColor(Color.parseColor("#334155"));
        desc.setPadding(0, 0, 0, dp(10));
        card.addView(desc);

        // Action Buttons Row
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button btnDetails = new Button(this);
        btnDetails.setText("View Details");
        btnDetails.setTextSize(12);
        btnDetails.setOnClickListener(v -> {
            Intent intent = new Intent(TourismActivity.this, TourismDetailsActivity.class);
            intent.putExtra("SPOT_NAME", spot.name);
            intent.putExtra("SPOT_CATEGORY", spot.category);
            intent.putExtra("SPOT_LOCATION", spot.location);
            intent.putExtra("SPOT_HOURS", spot.hours);
            intent.putExtra("SPOT_FEE", spot.entryFee);
            intent.putExtra("SPOT_LAT", spot.latitude);
            intent.putExtra("SPOT_LON", spot.longitude);
            intent.putExtra("SPOT_FULL_DESC", spot.fullDescription);
            startActivity(intent);
        });

        Button btnNavigate = new Button(this);
        btnNavigate.setText("Navigate on Map");
        btnNavigate.setTextSize(12);
        btnNavigate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0F766E")));
        btnNavigate.setTextColor(Color.WHITE);
        btnNavigate.setOnClickListener(v -> {
            Intent mapIntent = new Intent(TourismActivity.this, CityMapActivity.class);
            mapIntent.putExtra("DEST_LAT", spot.latitude);
            mapIntent.putExtra("DEST_LON", spot.longitude);
            mapIntent.putExtra("DEST_NAME", spot.name);
            startActivity(mapIntent);
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnDetails.setLayoutParams(btnParams);
        btnNavigate.setLayoutParams(btnParams);

        actions.addView(btnDetails);
        actions.addView(btnNavigate);
        card.addView(actions);

        layoutTourismContainer.addView(card);
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

    public static class TouristSpot {
        public String id, name, category, description, location, hours, entryFee, fullDescription;
        public double latitude, longitude;

        public TouristSpot(String id, String name, String category, String description,
                           String location, String hours, String entryFee,
                           double latitude, double longitude, String fullDescription) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.description = description;
            this.location = location;
            this.hours = hours;
            this.entryFee = entryFee;
            this.latitude = latitude;
            this.longitude = longitude;
            this.fullDescription = fullDescription;
        }
    }
}
