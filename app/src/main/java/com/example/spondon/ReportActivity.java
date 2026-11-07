
package com.example.spondon;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int LOCATION_PERMISSION_REQUEST = 101;

    private Spinner spIssueCategory;

    private EditText etIssueTitle;
    private EditText etIssueDescription;
    private EditText etIssueLocation;

    private Button btnSubmitReport;
    private Button btnChooseImage;
    private Button btnGetCurrentLocation;

    private ImageView ivReportPreview;
    private TextView tvLocationCoordinates;

    private DatabaseHelper databaseHelper;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private Uri selectedImageUri = null;

    private double latitude = 0;
    private double longitude = 0;

    private boolean hasCoordinates = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        spIssueCategory = findViewById(R.id.spIssueCategory);

        etIssueTitle = findViewById(R.id.etIssueTitle);
        etIssueDescription = findViewById(R.id.etIssueDescription);
        etIssueLocation = findViewById(R.id.etIssueLocation);

        btnSubmitReport = findViewById(R.id.btnSubmitReport);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnGetCurrentLocation = findViewById(R.id.btnGetCurrentLocation);

        ivReportPreview = findViewById(R.id.ivReportPreview);
        tvLocationCoordinates = findViewById(R.id.tvLocationCoordinates);

        databaseHelper = new DatabaseHelper(this);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        locationManager = (LocationManager)
                getSystemService(LOCATION_SERVICE);

        String[] categories = {
                "Road Damage",
                "Broken Streetlight",
                "Drainage Problem",
                "Garbage Problem",
                "Waterlogging",
                "Mosquito Breeding Site",
                "Stagnant Water",
                "Dengue Hotspot",
                "Other"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spIssueCategory.setAdapter(adapter);

        btnChooseImage.setOnClickListener(v -> chooseImage());

        btnGetCurrentLocation.setOnClickListener(v -> getCurrentLocation());

        btnSubmitReport.setOnClickListener(v -> submitReport());

        createLocationListener();
    }

    private void createLocationListener() {

        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(@NonNull Location location) {
                updateLocationDetails(location);
                stopLocationUpdates();
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
            }
        };
    }

    private void getCurrentLocation() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST
            );

            return;
        }

        try {

            String provider = LocationManager.GPS_PROVIDER;

            if (!locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
            )) {

                if (locationManager.isProviderEnabled(
                        LocationManager.NETWORK_PROVIDER
                )) {
                    provider = LocationManager.NETWORK_PROVIDER;
                } else {
                    Toast.makeText(
                            this,
                            "Please enable GPS or Location services",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }
            }

            tvLocationCoordinates.setText(
                    "Getting your current location..."
            );

            btnGetCurrentLocation.setEnabled(false);

            Location lastLocation =
                    locationManager.getLastKnownLocation(provider);

            if (lastLocation != null) {
                updateLocationDetails(lastLocation);
            }

            locationManager.requestLocationUpdates(
                    provider,
                    1000,
                    1,
                    locationListener
            );

        } catch (SecurityException e) {

            btnGetCurrentLocation.setEnabled(true);

            Toast.makeText(
                    this,
                    "Location permission is required",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void updateLocationDetails(Location location) {

        latitude = location.getLatitude();
        longitude = location.getLongitude();

        hasCoordinates = true;

        String coordinates = String.format(
                Locale.US,
                "Latitude: %.6f\nLongitude: %.6f",
                latitude,
                longitude
        );

        tvLocationCoordinates.setText(coordinates);

        btnGetCurrentLocation.setEnabled(true);

        Toast.makeText(
                this,
                "GPS location received!",
                Toast.LENGTH_SHORT
        ).show();

        getReadableAddress(latitude, longitude);
    }

    private void getReadableAddress(double lat, double lon) {

        new Thread(() -> {

            String addressText = "";

            try {

                Geocoder geocoder = new Geocoder(
                        ReportActivity.this,
                        Locale.getDefault()
                );

                List<Address> addresses =
                        geocoder.getFromLocation(lat, lon, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    addressText = addresses.get(0)
                            .getAddressLine(0);
                }

            } catch (IOException | IllegalArgumentException e) {

                addressText = "";
            }

            String finalAddress = addressText;

            runOnUiThread(() -> {

                if (!finalAddress.isEmpty()) {

                    etIssueLocation.setText(finalAddress);

                } else if (etIssueLocation.getText().toString()
                        .trim().isEmpty()) {

                    etIssueLocation.setText(
                            String.format(
                                    Locale.US,
                                    "%.6f, %.6f",
                                    lat,
                                    lon
                            )
                    );
                }
            });

        }).start();
    }

    private void stopLocationUpdates() {

        if (locationManager != null && locationListener != null) {

            try {

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) {

                    locationManager.removeUpdates(locationListener);
                }

            } catch (SecurityException e) {
                // Permission may have changed.
            }
        }

        if (btnGetCurrentLocation != null) {
            btnGetCurrentLocation.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_PERMISSION_REQUEST) {

            boolean granted = false;

            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (granted) {
                getCurrentLocation();
            } else {

                Toast.makeText(
                        this,
                        "Location permission denied",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void chooseImage() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.setType("image/*");

        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(
                intent,
                PICK_IMAGE_REQUEST
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            selectedImageUri = data.getData();

            try {

                getContentResolver().takePersistableUriPermission(
                        selectedImageUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );

                ivReportPreview.setImageURI(selectedImageUri);

                ivReportPreview.setVisibility(ImageView.VISIBLE);

                Toast.makeText(
                        this,
                        "Photo selected successfully!",
                        Toast.LENGTH_SHORT
                ).show();

            } catch (SecurityException e) {

                selectedImageUri = null;

                Toast.makeText(
                        this,
                        "Unable to access selected photo",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private void submitReport() {

        String category =
                spIssueCategory.getSelectedItem().toString();

        String title =
                etIssueTitle.getText().toString().trim();

        String description =
                etIssueDescription.getText().toString().trim();

        String location =
                etIssueLocation.getText().toString().trim();

        if (title.isEmpty()) {
            etIssueTitle.setError("Enter problem title");
            etIssueTitle.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etIssueDescription.setError(
                    "Enter problem description"
            );
            etIssueDescription.requestFocus();
            return;
        }

        if (location.isEmpty()) {
            etIssueLocation.setError(
                    "Enter problem location"
            );
            etIssueLocation.requestFocus();
            return;
        }

        if (firebaseAuth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Please login first",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();

        btnSubmitReport.setEnabled(false);

        // Save report locally
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("category", category);
        values.put("title", title);
        values.put("description", description);
        values.put("location", location);
        values.put("status", "Submitted");

        if (selectedImageUri != null) {
            values.put("image_uri", selectedImageUri.toString());
        } else {
            values.putNull("image_uri");
        }

        if (hasCoordinates) {
            values.put("latitude", latitude);
            values.put("longitude", longitude);
        } else {
            values.putNull("latitude");
            values.putNull("longitude");
        }

        long localResult = db.insert(
                DatabaseHelper.TABLE_REPORTS,
                null,
                values
        );

        db.close();

        if (localResult == -1) {

            btnSubmitReport.setEnabled(true);

            Toast.makeText(
                    this,
                    "Failed to save report locally!",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // Prepare Firebase data
        Map<String, Object> report = new HashMap<>();

        report.put("uid", userId);
        report.put("category", category);
        report.put("title", title);
        report.put("description", description);
        report.put("location", location);
        report.put("status", "Submitted");
        report.put("createdAt", FieldValue.serverTimestamp());

        if (hasCoordinates) {
            report.put("latitude", latitude);
            report.put("longitude", longitude);
        }

        // Upload to Firestore
        firestore.collection("reports")
                .add(report)
                .addOnSuccessListener(documentReference -> {

                    String firebaseId = documentReference.getId();

                    SQLiteDatabase updateDb =
                            databaseHelper.getWritableDatabase();

                    ContentValues updateValues = new ContentValues();

                    updateValues.put("firebase_id", firebaseId);

                    updateDb.update(
                            DatabaseHelper.TABLE_REPORTS,
                            updateValues,
                            "id = ?",
                            new String[]{
                                    String.valueOf(localResult)
                            }
                    );

                    updateDb.close();

                    btnSubmitReport.setEnabled(true);

                    Toast.makeText(
                            ReportActivity.this,
                            "Report saved and synced successfully!",
                            Toast.LENGTH_LONG
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    btnSubmitReport.setEnabled(true);

                    Toast.makeText(
                            ReportActivity.this,
                            "Report saved locally, but Firebase sync failed: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                    finish();
                });
    }

    @Override
    protected void onDestroy() {

        stopLocationUpdates();

        if (databaseHelper != null) {
            databaseHelper.close();
        }

        super.onDestroy();
    }
}
