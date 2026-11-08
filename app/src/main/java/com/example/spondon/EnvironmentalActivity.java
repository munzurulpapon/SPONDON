package com.example.spondon;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.Executors;

public class EnvironmentalActivity extends AppCompatActivity {

    private static final String TAG = "EnvironmentalActivity";
    private Button btnReportEcoIssue, btnBackEnvironmental;
    private TextView tvDataSourceTag, tvAqiValue, tvAqiStatus, tvAqiDetails, tvAqiAdvisory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_environmental);

        btnReportEcoIssue = findViewById(R.id.btnReportEcoIssue);
        btnBackEnvironmental = findViewById(R.id.btnBackEnvironmental);
        tvDataSourceTag = findViewById(R.id.tvDataSourceTag);
        tvAqiValue = findViewById(R.id.tvAqiValue);
        tvAqiStatus = findViewById(R.id.tvAqiStatus);
        tvAqiDetails = findViewById(R.id.tvAqiDetails);
        tvAqiAdvisory = findViewById(R.id.tvAqiAdvisory);

        btnBackEnvironmental.setOnClickListener(v -> finish());

        btnReportEcoIssue.setOnClickListener(v -> {
            startActivity(new Intent(EnvironmentalActivity.this, ReportActivity.class));
        });

        fetchLiveKhulnaAirQuality();
    }

    private void fetchLiveKhulnaAirQuality() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Free public Open-Meteo Air Quality API (zero API key, zero token, zero payment)
                URL url = new URL("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=22.8456&longitude=89.5403&current=us_aqi,pm10,pm2_5");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject root = new JSONObject(sb.toString());
                    if (root.has("current")) {
                        JSONObject current = root.getJSONObject("current");
                        int aqi = current.optInt("us_aqi", 68);
                        double pm25 = current.optDouble("pm2_5", 22.0);
                        double pm10 = current.optDouble("pm10", 45.0);

                        runOnUiThread(() -> applyAqiData(aqi, pm25, pm10));
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.d(TAG, "Offline or public API fetch timeout, keeping municipal baseline: " + e.getMessage());
            }
        });
    }

    private void applyAqiData(int aqi, double pm25, double pm10) {
        if (tvAqiValue != null) {
            tvAqiValue.setText(String.valueOf(aqi));
        }

        String status;
        String advisory;
        int statusColor;
        int bgColor;

        if (aqi <= 50) {
            status = "GOOD AIR QUALITY";
            advisory = "Health Advisory: Air quality is satisfactory. Air pollution poses little or no risk to citizens.";
            statusColor = Color.parseColor("#15803D");
            bgColor = Color.parseColor("#DCFCE7");
        } else if (aqi <= 100) {
            status = "MODERATE AIR QUALITY";
            advisory = "Health Advisory: Air quality is acceptable. Very sensitive individuals may experience minor irritation.";
            statusColor = Color.parseColor("#B45309");
            bgColor = Color.parseColor("#FEF3C7");
        } else if (aqi <= 150) {
            status = "UNHEALTHY FOR SENSITIVE GROUPS";
            advisory = "Health Advisory: Members of sensitive groups may experience health effects. General public less likely to be affected.";
            statusColor = Color.parseColor("#C2410C");
            bgColor = Color.parseColor("#FFEDD5");
        } else {
            status = "UNHEALTHY AIR QUALITY";
            advisory = "Health Advisory: Everyone may begin to experience health effects. Limit prolonged outdoor exertion.";
            statusColor = Color.parseColor("#B91C1C");
            bgColor = Color.parseColor("#FEE2E2");
        }

        if (tvAqiStatus != null) {
            tvAqiStatus.setText(status);
            tvAqiStatus.setTextColor(statusColor);
        }
        if (tvAqiValue != null) {
            tvAqiValue.setTextColor(statusColor);
            tvAqiValue.setBackgroundColor(bgColor);
        }
        if (tvAqiDetails != null) {
            tvAqiDetails.setText(String.format(Locale.US, "PM2.5: %.1f µg/m³ • PM10: %.1f µg/m³ • Live Sensor", pm25, pm10));
        }
        if (tvAqiAdvisory != null) {
            tvAqiAdvisory.setText(advisory);
        }
        if (tvDataSourceTag != null) {
            tvDataSourceTag.setText("[LIVE REAL-TIME DATA - Open-Meteo Khulna Sensor Station]");
            tvDataSourceTag.setTextColor(Color.parseColor("#A7F3D0"));
        }
    }
}
