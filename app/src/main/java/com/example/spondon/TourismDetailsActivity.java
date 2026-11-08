package com.example.spondon;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TourismDetailsActivity extends AppCompatActivity {

    private TextView tvTourDetailName, tvTourDetailCategory, tvTourDetailLocation, tvTourDetailHours, tvTourDetailFee, tvTourDetailFullDesc;
    private Button btnTourNavigateDirect, btnBackTourDetail;

    private String name, category, location, hours, fee, fullDesc;
    private double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tourism_details);

        tvTourDetailName = findViewById(R.id.tvTourDetailName);
        tvTourDetailCategory = findViewById(R.id.tvTourDetailCategory);
        tvTourDetailLocation = findViewById(R.id.tvTourDetailLocation);
        tvTourDetailHours = findViewById(R.id.tvTourDetailHours);
        tvTourDetailFee = findViewById(R.id.tvTourDetailFee);
        tvTourDetailFullDesc = findViewById(R.id.tvTourDetailFullDesc);

        btnTourNavigateDirect = findViewById(R.id.btnTourNavigateDirect);
        btnBackTourDetail = findViewById(R.id.btnBackTourDetail);

        name = getIntent().getStringExtra("SPOT_NAME");
        category = getIntent().getStringExtra("SPOT_CATEGORY");
        location = getIntent().getStringExtra("SPOT_LOCATION");
        hours = getIntent().getStringExtra("SPOT_HOURS");
        fee = getIntent().getStringExtra("SPOT_FEE");
        fullDesc = getIntent().getStringExtra("SPOT_FULL_DESC");
        latitude = getIntent().getDoubleExtra("SPOT_LAT", 22.8456);
        longitude = getIntent().getDoubleExtra("SPOT_LON", 89.5403);

        if (name != null) tvTourDetailName.setText(name);
        if (category != null) tvTourDetailCategory.setText(category.toUpperCase());
        if (location != null) tvTourDetailLocation.setText("Location: " + location);
        if (hours != null) tvTourDetailHours.setText("Visiting Hours: " + hours);
        if (fee != null) tvTourDetailFee.setText("Entry Fee: " + fee);
        if (fullDesc != null) tvTourDetailFullDesc.setText(fullDesc);

        btnBackTourDetail.setOnClickListener(v -> finish());

        btnTourNavigateDirect.setOnClickListener(v -> {
            Intent intent = new Intent(TourismDetailsActivity.this, CityMapActivity.class);
            intent.putExtra("DEST_LAT", latitude);
            intent.putExtra("DEST_LON", longitude);
            intent.putExtra("DEST_NAME", name);
            startActivity(intent);
        });
    }
}
