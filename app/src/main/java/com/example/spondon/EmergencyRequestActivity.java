package com.example.spondon;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmergencyRequestActivity extends AppCompatActivity implements LocationListener {

    private static final int LOCATION_PERMISSION_REQUEST = 101;
    private static final int CONTACTS_PERMISSION_REQUEST = 102;
    private static final int PICK_CONTACT_REQUEST = 201;

    private TextView tvLiveGpsCoords, tvLiveAddress;
    private Button btnRefreshGps, btnAutoFillLocation;

    private Button btnAddEmergencyContact, btnImportPhoneContact;
    private LinearLayout layoutContactsContainer;

    private EditText etEmergencyName, etEmergencyPhone;
    private EditText etEmergencyLocation, etEmergencyDescription;
    private Spinner spEmergencyType;

    private Button btnCall999, btnCallFire, btnCallAmbulance;
    private Button btnSubmitEmergency, btnViewMyEmergencies;
    // ROOT CAUSE FIX: layout declares btnBackEmergency as an <ImageView>. Typing it as Button made
    // findViewById() throw ClassCastException inside onCreate() => crash as soon as SOS opened.
    private View btnBackEmergency;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private DatabaseHelper databaseHelper;

    private LocationManager locationManager;
    private double currentLatitude = 22.8456;
    private double currentLongitude = 89.5403;
    private String currentAddressText = "Khulna, Bangladesh";
    private boolean locationDetected = false;
    private volatile boolean geocodeInFlight = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final String[] emergencyTypes = {
            "Medical Emergency",
            "Fire Emergency",
            "Road Accident",
            "Police Assistance",
            "Rescue Assistance",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_request);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        databaseHelper = new DatabaseHelper(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        tvLiveGpsCoords = findViewById(R.id.tvLiveGpsCoords);
        tvLiveAddress = findViewById(R.id.tvLiveAddress);
        btnRefreshGps = findViewById(R.id.btnRefreshGps);
        btnAutoFillLocation = findViewById(R.id.btnAutoFillLocation);

        btnAddEmergencyContact = findViewById(R.id.btnAddEmergencyContact);
        btnImportPhoneContact = findViewById(R.id.btnImportPhoneContact);
        layoutContactsContainer = findViewById(R.id.layoutContactsContainer);

        etEmergencyName = findViewById(R.id.etEmergencyName);
        etEmergencyPhone = findViewById(R.id.etEmergencyPhone);
        etEmergencyLocation = findViewById(R.id.etEmergencyLocation);
        etEmergencyDescription = findViewById(R.id.etEmergencyDescription);
        spEmergencyType = findViewById(R.id.spEmergencyType);

        // Restore screen state on rotation (Requirement 10)
        if (savedInstanceState != null) {
            etEmergencyName.setText(savedInstanceState.getString("KEY_EM_NAME", ""));
            etEmergencyPhone.setText(savedInstanceState.getString("KEY_EM_PHONE", ""));
            etEmergencyLocation.setText(savedInstanceState.getString("KEY_EM_LOC", ""));
            etEmergencyDescription.setText(savedInstanceState.getString("KEY_EM_DESC", ""));
        }

        btnCall999 = findViewById(R.id.btnCall999);
        btnCallFire = findViewById(R.id.btnCallFire);
        btnCallAmbulance = findViewById(R.id.btnCallAmbulance);

        btnSubmitEmergency = findViewById(R.id.btnSubmitEmergency);
        btnViewMyEmergencies = findViewById(R.id.btnViewMyEmergencies);
        btnBackEmergency = findViewById(R.id.btnBackEmergency);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                emergencyTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEmergencyType.setAdapter(adapter);
        if (savedInstanceState != null) {
            int pos = savedInstanceState.getInt("KEY_EM_TYPE_POS", 0);
            if (pos >= 0 && pos < emergencyTypes.length) spEmergencyType.setSelection(pos);
        }

        // Pre-fill user profile if available
        if (firebaseAuth.getCurrentUser() != null) {
            String uid = firebaseAuth.getCurrentUser().getUid();
            firestore.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                if (doc != null && doc.exists()) {
                    String name = doc.getString("name");
                    String phone = doc.getString("phone");
                    if (name != null && etEmergencyName.getText().toString().isEmpty()) {
                        etEmergencyName.setText(name);
                    }
                    if (phone != null && etEmergencyPhone.getText().toString().isEmpty()) {
                        etEmergencyPhone.setText(phone);
                    }
                }
            });
        }

        // Hotlines
        btnCall999.setOnClickListener(v -> dialNumber("999"));
        btnCallFire.setOnClickListener(v -> dialNumber("16163"));
        btnCallAmbulance.setOnClickListener(v -> dialNumber("16263"));

        // GPS
        btnRefreshGps.setOnClickListener(v -> checkLocationPermissionAndDetect());
        btnAutoFillLocation.setOnClickListener(v -> {
            if (locationDetected) {
                etEmergencyLocation.setText(currentAddressText + " (" + String.format(Locale.ROOT, "%.4f, %.4f", currentLatitude, currentLongitude) + ")");
                Toast.makeText(this, "Location auto-filled!", Toast.LENGTH_SHORT).show();
            } else {
                checkLocationPermissionAndDetect();
                Toast.makeText(this, "Detecting GPS, please wait...", Toast.LENGTH_SHORT).show();
            }
        });

        // Trusted Contacts
        btnAddEmergencyContact.setOnClickListener(v -> showAddContactDialog());
        btnImportPhoneContact.setOnClickListener(v -> checkContactsPermissionAndPick());
        loadTrustedContacts();

        // Submit & History
        btnSubmitEmergency.setOnClickListener(v -> submitEmergency());
        btnViewMyEmergencies.setOnClickListener(v -> {
            startActivity(new Intent(EmergencyRequestActivity.this, MyEmergencyRequestsActivity.class));
        });

        btnBackEmergency.setOnClickListener(v -> finish());

        checkLocationPermissionAndDetect();
    }

    private void dialNumber(String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to launch phone dialer", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Location Management ---
    private void checkLocationPermissionAndDetect() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            startLocationUpdates();
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationUpdates() {
        if (locationManager == null || !hasLocationPermission()) return;
        tvLiveGpsCoords.setText("GPS: Acquiring satellite fix...");
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        // Each provider is isolated so one failure (no permission for GPS, provider missing,
        // device without GPS) can never take down the SOS screen.
        if (fine) {
            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
                }
            } catch (Exception ignored) {
            }
        }
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, this);
            }
        } catch (Exception ignored) {
        }

        Location lastKnown = null;
        try {
            if (fine) lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {
        }
        try {
            if (lastKnown == null) lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }
        if (lastKnown != null) {
            updateLocationDisplay(lastKnown);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (location == null || isFinishing()) return;
        updateLocationDisplay(location);
    }

    private void updateLocationDisplay(Location loc) {
        currentLatitude = loc.getLatitude();
        currentLongitude = loc.getLongitude();
        locationDetected = true;

        tvLiveGpsCoords.setText(String.format(Locale.ROOT, "GPS: %.5f° N, %.5f° E (Accuracy: ±%.1fm)",
                currentLatitude, currentLongitude, loc.getAccuracy()));

        // Reverse geocode on a worker thread: Geocoder does a blocking network call and
        // running it on the UI thread freezes (ANR) / crashes the SOS screen.
        if (geocodeInFlight) return;
        geocodeInFlight = true;
        final double lat = currentLatitude;
        final double lon = currentLongitude;
        new Thread(() -> {
            String resolved;
            try {
                if (!Geocoder.isPresent()) throw new IllegalStateException("No geocoder");
                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty() && addresses.get(0) != null) {
                    Address addr = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    if (addr.getThoroughfare() != null) sb.append(addr.getThoroughfare()).append(", ");
                    if (addr.getSubLocality() != null) sb.append(addr.getSubLocality()).append(", ");
                    if (addr.getLocality() != null) sb.append(addr.getLocality());
                    resolved = sb.toString();
                    if (resolved.isEmpty()) {
                        resolved = addr.getAddressLine(0);
                    }
                    if (resolved == null || resolved.isEmpty()) {
                        resolved = "Khulna Metropolitan Area";
                    }
                } else {
                    resolved = "Khulna Metropolitan Area";
                }
            } catch (Exception e) {
                resolved = "Lat: " + String.format(Locale.ROOT, "%.4f", lat) + ", Lon: " + String.format(Locale.ROOT, "%.4f", lon);
            }
            final String addressResult = resolved;
            geocodeInFlight = false;
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                currentAddressText = addressResult;
                tvLiveAddress.setText("Address: " + currentAddressText);
            });
        }, "spondon-geocode").start();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}

    private void checkContactsPermissionAndPick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    CONTACTS_PERMISSION_REQUEST
            );
        } else {
            openPhoneContactPicker();
        }
    }

    private void openPhoneContactPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(intent, PICK_CONTACT_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "No contacts app available on this device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            if (contactUri != null) {
                String[] projection = new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                };
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(contactUri, projection, null, null, null);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to read the selected contact", Toast.LENGTH_SHORT).show();
                }
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            int nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                            int phoneCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            String name = nameCol != -1 ? cursor.getString(nameCol) : "Contact";
                            String phone = phoneCol != -1 ? cursor.getString(phoneCol) : "";

                            if (phone != null && !phone.isEmpty()) {
                                SQLiteDatabase db = databaseHelper.getWritableDatabase();
                                ContentValues cv = new ContentValues();
                                cv.put("name", name);
                                cv.put("phone", phone);
                                cv.put("relationship", "Phone Contact");
                                // NOTE: do not db.close() here. DatabaseHelper hands out one shared
                                // connection; closing it broke later writes (e.g. the SOS submit).
                                long rowId = db.insert(DatabaseHelper.TABLE_EMERGENCY_CONTACTS, null, cv);

                                if (rowId != -1) {
                                    Toast.makeText(this, "Imported " + name + " to trusted contacts!", Toast.LENGTH_SHORT).show();
                                    loadTrustedContacts();
                                }
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Unable to import this contact", Toast.LENGTH_SHORT).show();
                    } finally {
                        cursor.close();
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            boolean granted = false;
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_GRANTED) { granted = true; break; }
            }
            if (granted) {
                startLocationUpdates();
            } else {
                tvLiveGpsCoords.setText("GPS: Location permission denied - enter the location manually");
                Toast.makeText(this, "Location permission denied. You can still type the location and send SOS.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == CONTACTS_PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openPhoneContactPicker();
        } else if (requestCode == CONTACTS_PERMISSION_REQUEST) {
            Toast.makeText(this, "Permission to read contacts was denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (etEmergencyName != null) outState.putString("KEY_EM_NAME", etEmergencyName.getText().toString());
        if (etEmergencyPhone != null) outState.putString("KEY_EM_PHONE", etEmergencyPhone.getText().toString());
        if (etEmergencyLocation != null) outState.putString("KEY_EM_LOC", etEmergencyLocation.getText().toString());
        if (etEmergencyDescription != null) outState.putString("KEY_EM_DESC", etEmergencyDescription.getText().toString());
        if (spEmergencyType != null) outState.putInt("KEY_EM_TYPE_POS", spEmergencyType.getSelectedItemPosition());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(this);
            }
        } catch (Exception ignored) {
        }
    }

    // --- Trusted Contacts (SQLite) ---
    private void showAddContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Trusted Emergency Contact");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(10), dp(20), dp(10));

        final EditText etName = new EditText(this);
        etName.setHint("Contact Name (e.g., Brother, Doctor)");
        layout.addView(etName);

        final EditText etPhone = new EditText(this);
        etPhone.setHint("Phone Number (e.g., 01711...)");
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        layout.addView(etPhone);

        final EditText etRelation = new EditText(this);
        etRelation.setHint("Relationship (e.g., Family, Neighbor)");
        layout.addView(etRelation);

        builder.setView(layout);

        builder.setPositiveButton("Save Contact", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String rel = etRelation.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Name and Phone are required", Toast.LENGTH_SHORT).show();
                return;
            }

            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("name", name);
            cv.put("phone", phone);
            cv.put("relationship", rel.isEmpty() ? "Emergency Contact" : rel);

            long rowId = db.insert(DatabaseHelper.TABLE_EMERGENCY_CONTACTS, null, cv);
            if (rowId != -1) {
                Toast.makeText(this, "Contact saved locally!", Toast.LENGTH_SHORT).show();
                loadTrustedContacts();
            } else {
                Toast.makeText(this, "Failed to save contact", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadTrustedContacts() {
        layoutContactsContainer.removeAllViews();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT id, name, phone, relationship FROM " + DatabaseHelper.TABLE_EMERGENCY_CONTACTS + " ORDER BY id DESC", null);
        } catch (Exception e) {
            android.util.Log.e("EmergencyRequest", "Unable to load trusted contacts", e);
        }

        int count = 0;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                count++;
                final int id = cursor.getInt(0);
                final String name = cursor.getString(1) != null ? cursor.getString(1) : "Contact";
                final String phone = cursor.getString(2) != null ? cursor.getString(2) : "";
                final String rel = cursor.getString(3) != null ? cursor.getString(3) : "Emergency Contact";

                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.HORIZONTAL);
                item.setPadding(dp(10), dp(8), dp(10), dp(8));
                item.setGravity(android.view.Gravity.CENTER_VERTICAL);

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.parseColor("#F8FAFC"));
                bg.setCornerRadius(dp(6));
                bg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
                item.setBackground(bg);

                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                p.setMargins(0, 0, 0, dp(6));
                item.setLayoutParams(p);

                LinearLayout info = new LinearLayout(this);
                info.setOrientation(LinearLayout.VERTICAL);
                info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                TextView tvName = new TextView(this);
                tvName.setText(name + " (" + rel + ")");
                tvName.setTextSize(13);
                tvName.setTypeface(null, Typeface.BOLD);
                tvName.setTextColor(Color.parseColor("#1E293B"));
                info.addView(tvName);

                TextView tvP = new TextView(this);
                tvP.setText(phone);
                tvP.setTextSize(12);
                tvP.setTextColor(Color.parseColor("#0284C7"));
                info.addView(tvP);

                item.addView(info);

                Button btnCall = new Button(this);
                btnCall.setText("Call");
                btnCall.setTextSize(11);
                btnCall.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#059669")));
                btnCall.setTextColor(Color.WHITE);
                btnCall.setOnClickListener(v -> dialNumber(phone));
                item.addView(btnCall);

                Button btnDel = new Button(this);
                btnDel.setText("✕");
                btnDel.setTextSize(11);
                btnDel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444")));
                btnDel.setTextColor(Color.WHITE);
                btnDel.setOnClickListener(v -> {
                    SQLiteDatabase wdb = databaseHelper.getWritableDatabase();
                    wdb.delete(DatabaseHelper.TABLE_EMERGENCY_CONTACTS, "id=?", new String[]{String.valueOf(id)});
                    loadTrustedContacts();
                });
                item.addView(btnDel);

                layoutContactsContainer.addView(item);
            }
            cursor.close();
        }

        if (count == 0) {
            TextView empty = new TextView(this);
            empty.setText("No personal emergency contacts added yet. Tap '+ Add Contact' to save trusted family or doctors.");
            empty.setTextSize(12);
            empty.setTextColor(Color.parseColor("#94A3B8"));
            empty.setPadding(0, dp(4), 0, dp(4));
            layoutContactsContainer.addView(empty);
        }
    }

    // --- Submit Emergency (SQLite + Cloud Firestore) ---
    private void submitEmergency() {
        String name = etEmergencyName.getText().toString().trim();
        String phone = etEmergencyPhone.getText().toString().trim();
        Object selectedType = spEmergencyType.getSelectedItem();
        String type = selectedType != null ? selectedType.toString() : emergencyTypes[0];
        String location = etEmergencyLocation.getText().toString().trim();
        String description = etEmergencyDescription.getText().toString().trim();

        if (name.isEmpty()) {
            etEmergencyName.setError("Enter your name");
            etEmergencyName.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            etEmergencyPhone.setError("Enter your phone number");
            etEmergencyPhone.requestFocus();
            return;
        }

        if (location.isEmpty()) {
            etEmergencyLocation.setError("Enter incident location");
            etEmergencyLocation.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etEmergencyDescription.setError("Describe the emergency");
            etEmergencyDescription.requestFocus();
            return;
        }

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitEmergency.setEnabled(false);

        final String uid = firebaseAuth.getCurrentUser().getUid();

        // 1. Save locally into SQLite immediately (never allowed to crash the SOS flow)
        long savedId = -1;
        try {
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("uid", uid);
            cv.put("name", name);
            cv.put("phone", phone);
            cv.put("type", type);
            cv.put("location", location);
            cv.put("latitude", currentLatitude);
            cv.put("longitude", currentLongitude);
            cv.put("description", description);
            cv.put("status", "Pending");
            savedId = db.insert(DatabaseHelper.TABLE_EMERGENCY_REQUESTS, null, cv);
        } catch (Exception e) {
            android.util.Log.e("EmergencyRequest", "Local SOS save failed", e);
        }
        final long localId = savedId;

        // 2. Broadcast to Cloud Firestore
        final Map<String, Object> emergency = new HashMap<>();
        emergency.put("uid", uid);
        emergency.put("name", name);
        emergency.put("phone", phone);
        emergency.put("type", type);
        emergency.put("location", location);
        emergency.put("latitude", currentLatitude);
        emergency.put("longitude", currentLongitude);
        emergency.put("description", description);
        emergency.put("status", "Pending");
        emergency.put("createdAt", FieldValue.serverTimestamp());

        // Firestore only acknowledges once it reaches the server. If the phone is offline the
        // write is queued by the SDK and sent automatically later, but no callback fires now.
        // This fallback un-sticks the (disabled) button and tells the user what happened.
        final boolean[] finished = {false};
        final Runnable offlineFallback = () -> {
            if (finished[0]) return;
            finished[0] = true;
            if (isFinishing() || isDestroyed()) return;
            btnSubmitEmergency.setEnabled(true);
            Toast.makeText(
                    EmergencyRequestActivity.this,
                    "No connection. SOS saved on this device and will be sent automatically when you are back online.",
                    Toast.LENGTH_LONG
            ).show();
            finish();
        };
        mainHandler.postDelayed(offlineFallback, 15000);

        try {
            firestore.collection("emergencies")
                    .add(emergency)
                    .addOnSuccessListener(documentReference -> {
                        mainHandler.removeCallbacks(offlineFallback);
                        final String docId = documentReference.getId();

                        // Always finish the data work, even if the screen is already gone.
                        RealtimeDbHelper.publishEmergency(docId, emergency);
                        if (localId != -1) {
                            try {
                                // Re-acquire the DB: a handle captured earlier may have been closed.
                                SQLiteDatabase wdb = databaseHelper.getWritableDatabase();
                                ContentValues ucv = new ContentValues();
                                ucv.put("firebase_id", docId);
                                wdb.update(DatabaseHelper.TABLE_EMERGENCY_REQUESTS, ucv, "id=?",
                                        new String[]{String.valueOf(localId)});
                            } catch (Exception e) {
                                android.util.Log.w("EmergencyRequest", "Could not store firebase_id", e);
                            }
                        }

                        if (finished[0]) return; // fallback already informed the user and closed the screen
                        finished[0] = true;
                        if (isFinishing() || isDestroyed()) return;
                        btnSubmitEmergency.setEnabled(true);
                        Toast.makeText(
                                EmergencyRequestActivity.this,
                                "Emergency request broadcast and saved locally!",
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        mainHandler.removeCallbacks(offlineFallback);
                        if (finished[0]) return;
                        finished[0] = true;
                        if (isFinishing() || isDestroyed()) return;
                        btnSubmitEmergency.setEnabled(true);
                        Toast.makeText(
                                EmergencyRequestActivity.this,
                                "Saved locally, but offline sync failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                    });
        } catch (Exception e) {
            // Firestore could not even start the write (e.g. not initialised)
            mainHandler.removeCallbacks(offlineFallback);
            finished[0] = true;
            btnSubmitEmergency.setEnabled(true);
            Toast.makeText(this, "Saved locally, but could not reach the server: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}