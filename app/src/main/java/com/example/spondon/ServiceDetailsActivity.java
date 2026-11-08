package com.example.spondon;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ServiceDetailsActivity extends AppCompatActivity {

    private TextView tvDetailServiceName, tvDetailServiceCategory, tvDetailServiceDesc;
    private TextView tvDetailServicePrice, tvDetailProviderName, tvDetailProviderPhone, tvDetailRating;
    private Button btnCallProvider, btnBookServiceDirect, btnBackServiceDetail;

    private String serviceId, serviceName, serviceCategory, serviceDesc, providerName, phone, price;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_details);

        tvDetailServiceName = findViewById(R.id.tvDetailServiceName);
        tvDetailServiceCategory = findViewById(R.id.tvDetailServiceCategory);
        tvDetailServiceDesc = findViewById(R.id.tvDetailServiceDesc);
        tvDetailServicePrice = findViewById(R.id.tvDetailServicePrice);
        tvDetailProviderName = findViewById(R.id.tvDetailProviderName);
        tvDetailProviderPhone = findViewById(R.id.tvDetailProviderPhone);
        tvDetailRating = findViewById(R.id.tvDetailRating);

        btnCallProvider = findViewById(R.id.btnCallProvider);
        btnBookServiceDirect = findViewById(R.id.btnBookServiceDirect);
        btnBackServiceDetail = findViewById(R.id.btnBackServiceDetail);

        serviceId = getIntent().getStringExtra("SERVICE_ID");
        serviceName = getIntent().getStringExtra("SERVICE_NAME");
        serviceCategory = getIntent().getStringExtra("SERVICE_CATEGORY");
        serviceDesc = getIntent().getStringExtra("SERVICE_DESC");
        providerName = getIntent().getStringExtra("SERVICE_PROVIDER");
        phone = getIntent().getStringExtra("SERVICE_PHONE");
        price = getIntent().getStringExtra("SERVICE_PRICE");
        String rating = getIntent().getStringExtra("SERVICE_RATING");

        if (serviceName != null) tvDetailServiceName.setText(serviceName);
        if (serviceCategory != null) tvDetailServiceCategory.setText(serviceCategory.toUpperCase());
        if (serviceDesc != null) tvDetailServiceDesc.setText(serviceDesc);
        if (price != null) tvDetailServicePrice.setText("Estimated Rate: " + price);
        if (providerName != null) tvDetailProviderName.setText(providerName);
        if (phone != null) tvDetailProviderPhone.setText("Helpline: " + phone);
        if (rating != null) tvDetailRating.setText("Verified Provider • " + rating + " Rating");

        btnBackServiceDetail.setOnClickListener(v -> finish());

        btnCallProvider.setOnClickListener(v -> {
            if (phone != null && !phone.isEmpty()) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                startActivity(callIntent);
            } else {
                Toast.makeText(this, "Phone number unavailable", Toast.LENGTH_SHORT).show();
            }
        });

        btnBookServiceDirect.setOnClickListener(v -> {
            Intent intent = new Intent(ServiceDetailsActivity.this, BookServiceActivity.class);
            intent.putExtra("SERVICE_ID", serviceId);
            intent.putExtra("SERVICE_NAME", serviceName);
            intent.putExtra("SERVICE_CATEGORY", serviceCategory);
            intent.putExtra("SERVICE_PRICE", price);
            startActivity(intent);
        });
    }
}
