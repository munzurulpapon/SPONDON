package com.example.spondon;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CityMapActivity extends AppCompatActivity
        implements SensorEventListener {

    private static final String TAG = "SPONDON_MAP";
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final double SEARCH_RADIUS = 2000;
    private static final String PREFS = "spondon_map_preferences";

    private MapView mapView;
    private LocationManager locationManager;
    private ExecutorService executorService;

    private FirebaseFirestore firestore;
    private ListenerRegistration reportsListener;

    private Marker currentMarker;
    private Marker searchMarker;
    private Polyline routeLine;

    private Button btnAll, btnHospital, btnPolice, btnFire;
    private Button btnATM, btnPharmacy, btnRestaurant, btnUniversity;
    private Button btnBusStop, btnFuel, btnHotel;
    private Button btnMyLocation, btnSearch, btnClearRoute, btnDirections;
    private Button btnSavePlace, btnSavedPlaces, btnRecentSearches, btnDarkMode;

    private EditText etMapSearch;
    private LinearLayout layoutRouteInfo;

    private TextView tvRouteDistance, tvRouteDuration;
    private TextView tvRemainingDistance, tvRemainingTime;
    private TextView tvNextInstruction, tvCompass;

    private final ArrayList<Marker> nearbyMarkers = new ArrayList<>();
    private final ArrayList<Marker> issueMarkers = new ArrayList<>();

    private final ArrayList<String> currentDirections = new ArrayList<>();
    private final ArrayList<GeoPoint> activeRoutePoints = new ArrayList<>();
    private final ArrayList<RouteOption> routeOptions = new ArrayList<>();

    private double currentLatitude = 22.8456;
    private double currentLongitude = 89.5403;

    private double destinationLatitude;
    private double destinationLongitude;
    private String destinationName = "";

    private double totalRouteDistanceMeters;
    private double totalRouteDurationSeconds;

    private boolean hasCurrentLocation = false;
    private boolean nearbyLoaded = false;
    private boolean darkMapEnabled = false;
    private boolean rerouteInProgress = false;

    private long lastRerouteTime = 0;
    private int offRouteCount = 0;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravityValues;
    private float[] magneticValues;
    private float currentAzimuth = 0;

    private boolean pendingIncomingRoute = false;
    private String activeFilter = "All";

    // Smart Issue Filters
    private String selectedIssueCategory = "All";
    private String selectedIssueStatus = "All";
    private QuerySnapshot latestReportsSnapshot;

    private static class RouteOption {
        ArrayList<GeoPoint> points = new ArrayList<>();
        ArrayList<String> directions = new ArrayList<>();
        double distance;
        double duration;
    }

    private static class PlaceItem {
        String name;
        String address;
        double latitude;
        double longitude;

        PlaceItem(String name, String address,
                  double latitude, double longitude) {
            this.name = name;
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    // =========================================================
    // ACTIVITY CREATION
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Configuration.getInstance().load(
                    getApplicationContext(),
                    android.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
            // Keep map tiles in app-private storage (no storage permission needed on Android 10+)
            java.io.File base = new java.io.File(getCacheDir(), "osmdroid");
            java.io.File tiles = new java.io.File(base, "tiles");
            tiles.mkdirs();
            Configuration.getInstance().setOsmdroidBasePath(base);
            Configuration.getInstance().setOsmdroidTileCache(tiles);
        } catch (Exception ignored) { }
        Configuration.getInstance().setUserAgentValue(
                "SPONDON/1.0 (Android; com.example.spondon)"
        );

        setContentView(R.layout.activity_city_map);

        executorService = Executors.newSingleThreadExecutor();

        initializeViews();
        initializeButtons();
        initializeMap();
        initializeCompass();

        locationManager = (LocationManager)
                getSystemService(LOCATION_SERVICE);

        firestore = FirebaseFirestore.getInstance();
        loadCitizenIssueMarkers();

        checkLocationPermission();
        applyIncomingDestination();
    }

    /** Opens the map on a destination passed by another screen (e.g. a disaster shelter). */
    private void applyIncomingDestination() {
        try {
            android.content.Intent in = getIntent();
            if (in == null || !in.hasExtra("DEST_LAT") || !in.hasExtra("DEST_LON")) return;
            double lat = in.getDoubleExtra("DEST_LAT", 0);
            double lon = in.getDoubleExtra("DEST_LON", 0);
            String name = in.getStringExtra("DEST_NAME");
            if (lat == 0 && lon == 0) return;
            pendingIncomingRoute = !hasCurrentLocation;
            selectDestination(name != null ? name : "Destination", "", lat, lon);
        } catch (Exception e) {
            Log.w(TAG, "Could not apply incoming destination", e);
        }
    }

    private void initializeViews() {

        View btnBack = findViewById(R.id.btnBackMap);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        mapView = findViewById(R.id.osmMap);

        btnAll = findViewById(R.id.btnAll);
        btnHospital = findViewById(R.id.btnHospital);
        btnPolice = findViewById(R.id.btnPolice);
        btnFire = findViewById(R.id.btnFire);

        btnATM = findViewById(R.id.btnATM);
        btnPharmacy = findViewById(R.id.btnPharmacy);
        btnRestaurant = findViewById(R.id.btnRestaurant);
        btnUniversity = findViewById(R.id.btnUniversity);
        btnBusStop = findViewById(R.id.btnBusStop);
        btnFuel = findViewById(R.id.btnFuel);
        btnHotel = findViewById(R.id.btnHotel);

        btnMyLocation = findViewById(R.id.btnMyLocation);
        btnSearch = findViewById(R.id.btnSearch);
        btnClearRoute = findViewById(R.id.btnClearRoute);
        btnDirections = findViewById(R.id.btnDirections);

        btnSavePlace = findViewById(R.id.btnSavePlace);
        btnSavedPlaces = findViewById(R.id.btnSavedPlaces);
        btnRecentSearches = findViewById(R.id.btnRecentSearches);
        btnDarkMode = findViewById(R.id.btnDarkMode);

        etMapSearch = findViewById(R.id.etMapSearch);
        layoutRouteInfo = findViewById(R.id.layoutRouteInfo);

        tvRouteDistance = findViewById(R.id.tvRouteDistance);
        tvRouteDuration = findViewById(R.id.tvRouteDuration);
        tvRemainingDistance = findViewById(R.id.tvRemainingDistance);
        tvRemainingTime = findViewById(R.id.tvRemainingTime);
        tvNextInstruction = findViewById(R.id.tvNextInstruction);
        tvCompass = findViewById(R.id.tvCompass);

        layoutRouteInfo.setVisibility(View.GONE);
        btnDirections.setEnabled(false);
    }

    private void initializeButtons() {

        btnAll.setOnClickListener(v -> filterMarkers("All"));
        btnHospital.setOnClickListener(v -> filterMarkers("Hospital"));
        btnPolice.setOnClickListener(v -> filterMarkers("Police Station"));
        btnFire.setOnClickListener(v -> filterMarkers("Fire Station"));

        btnATM.setOnClickListener(v -> filterMarkers("ATM"));
        btnPharmacy.setOnClickListener(v -> filterMarkers("Pharmacy"));
        btnRestaurant.setOnClickListener(v -> filterMarkers("Restaurant"));
        btnUniversity.setOnClickListener(v -> filterMarkers("University"));
        btnBusStop.setOnClickListener(v -> filterMarkers("Bus Stop"));
        btnFuel.setOnClickListener(v -> filterMarkers("Fuel Station"));
        btnHotel.setOnClickListener(v -> filterMarkers("Hotel"));

        // Long press All to open citizen report filters
        btnAll.setOnLongClickListener(v -> {
            showIssueCategoryFilter();
            return true;
        });

        btnMyLocation.setOnClickListener(v -> goToCurrentLocation());
        btnSearch.setOnClickListener(v -> searchLocation());

        btnClearRoute.setOnClickListener(v -> clearRoute());

        btnDirections.setOnClickListener(v ->
                showDirectionsDialog(currentDirections));

        btnSavePlace.setOnClickListener(v -> saveCurrentPlace());
        btnSavedPlaces.setOnClickListener(v -> showStoredPlaces(false));
        btnRecentSearches.setOnClickListener(v -> showStoredPlaces(true));

        btnDarkMode.setOnClickListener(v -> toggleDarkMap());

        etMapSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation();
                return true;
            }
            return false;
        });
    }

    // =========================================================
    // MAP
    // =========================================================

    private void initializeMap() {

        setMapTileSource();

        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);
        mapView.getController().setZoom(14.0);

        GeoPoint khulna = new GeoPoint(currentLatitude, currentLongitude);
        mapView.getController().setCenter(khulna);

        Marker cityMarker = new Marker(mapView);
        cityMarker.setPosition(khulna);
        cityMarker.setTitle("SPONDON - Khulna");

        mapView.getOverlays().add(cityMarker);

        ScaleBarOverlay scaleBar = new ScaleBarOverlay(mapView);
        mapView.getOverlays().add(scaleBar);
    }

    private void setMapTileSource() {

        final String style = darkMapEnabled
                ? "toner-v2"
                : "streets-v4";

        mapView.setTileSource(new OnlineTileSourceBase(
                "MapTiler-" + style,
                1,
                20,
                256,
                ".png",
                new String[]{
                        "https://api.maptiler.com/maps/" + style + "/256/"
                }
        ) {
            @Override
            public String getTileURLString(long tileIndex) {

                return getBaseUrl()
                        + MapTileIndex.getZoom(tileIndex) + "/"
                        + MapTileIndex.getX(tileIndex) + "/"
                        + MapTileIndex.getY(tileIndex)
                        + ".png?key="
                        + CityMapActivity.this.getString(
                        R.string.maptiler_api_key
                );
            }
        });

        mapView.invalidate();
    }

    private void toggleDarkMap() {

        darkMapEnabled = !darkMapEnabled;
        setMapTileSource();

        btnDarkMode.setText(
                darkMapEnabled ? "Light Map" : "Dark Map"
        );

        Toast.makeText(
                this,
                darkMapEnabled ? "Dark map enabled" : "Light map enabled",
                Toast.LENGTH_SHORT
        ).show();
    }

    // =========================================================
    // COMPASS
    // =========================================================

    private void initializeCompass() {

        sensorManager = (SensorManager)
                getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager == null) return;

        accelerometer = sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
        );

        magnetometer = sensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD
        );

        if (accelerometer == null || magnetometer == null) {
            tvCompass.setText("Compass unavailable");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravityValues = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticValues = event.values.clone();
        }

        if (gravityValues == null || magneticValues == null) return;

        float[] rotationMatrix = new float[9];
        float[] orientation = new float[3];

        boolean success = SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                gravityValues,
                magneticValues
        );

        if (success) {

            SensorManager.getOrientation(rotationMatrix, orientation);

            currentAzimuth = (float) Math.toDegrees(orientation[0]);
            currentAzimuth = (currentAzimuth + 360) % 360;

            String direction = getCompassDirection(currentAzimuth);

            tvCompass.setText(String.format(
                    Locale.getDefault(),
                    "Compass: %s  %.0f°",
                    direction,
                    currentAzimuth
            ));
        }
    }

    private String getCompassDirection(float degrees) {

        if (degrees >= 337.5 || degrees < 22.5) return "N";
        if (degrees < 67.5) return "NE";
        if (degrees < 112.5) return "E";
        if (degrees < 157.5) return "SE";
        if (degrees < 202.5) return "S";
        if (degrees < 247.5) return "SW";
        if (degrees < 292.5) return "W";

        return "NW";
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // =========================================================
    // LOCATION SEARCH
    // =========================================================

    private void searchLocation() {

        String query = etMapSearch.getText().toString().trim();

        if (query.isEmpty()) {
            etMapSearch.setError("Enter a location");
            return;
        }

        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(
                    etMapSearch.getWindowToken(), 0
            );
        }

        btnSearch.setEnabled(false);
        btnSearch.setText("Searching...");

        executorService.execute(() -> {

            HttpURLConnection connection = null;

            try {

                String encoded = URLEncoder.encode(query, "UTF-8");

                URL url = new URL(
                        "https://photon.komoot.io/api/?q="
                                + encoded + "&limit=8"
                );

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("User-Agent", "SPONDON/1.0");

                if (connection.getResponseCode() != 200) {
                    throw new Exception(
                            "Search server error: "
                                    + connection.getResponseCode()
                    );
                }

                JSONObject json = new JSONObject(readResponse(connection));
                JSONArray features = json.getJSONArray("features");

                ArrayList<PlaceItem> results = new ArrayList<>();

                for (int i = 0; i < features.length(); i++) {

                    JSONObject feature = features.getJSONObject(i);
                    JSONObject properties = feature.getJSONObject("properties");

                    JSONArray coordinates = feature.getJSONObject("geometry")
                            .getJSONArray("coordinates");

                    String name = properties.optString(
                            "name", "Unnamed location"
                    );

                    String city = properties.optString("city", "");
                    String state = properties.optString("state", "");
                    String country = properties.optString("country", "");

                    String address = name;

                    if (!city.isEmpty()) address += ", " + city;
                    if (!state.isEmpty()) address += ", " + state;
                    if (!country.isEmpty()) address += ", " + country;

                    results.add(new PlaceItem(
                            name,
                            address,
                            coordinates.getDouble(1),
                            coordinates.getDouble(0)
                    ));
                }

                runOnUiThread(() -> {

                    btnSearch.setEnabled(true);
                    btnSearch.setText("Search");

                    if (results.isEmpty()) {
                        Toast.makeText(
                                this,
                                "No locations found",
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        showSearchResults(results);
                    }
                });

            } catch (Exception e) {

                Log.e(TAG, "Search failed", e);

                runOnUiThread(() -> {

                    btnSearch.setEnabled(true);
                    btnSearch.setText("Search");

                    Toast.makeText(
                            this,
                            "Search failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });

            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void showSearchResults(ArrayList<PlaceItem> results) {

        String[] items = new String[results.size()];

        for (int i = 0; i < results.size(); i++) {
            items[i] = results.get(i).name
                    + "\n" + results.get(i).address;
        }

        new AlertDialog.Builder(this)
                .setTitle("Search Results")
                .setItems(items, (dialog, which) -> {

                    PlaceItem place = results.get(which);

                    selectDestination(
                            place.name,
                            place.address,
                            place.latitude,
                            place.longitude
                    );

                    addRecentSearch(place);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void selectDestination(
            String name,
            String address,
            double latitude,
            double longitude) {

        destinationName = name;
        destinationLatitude = latitude;
        destinationLongitude = longitude;

        GeoPoint point = new GeoPoint(latitude, longitude);

        mapView.getController().animateTo(point);
        mapView.getController().setZoom(16.0);

        if (searchMarker != null) {
            mapView.getOverlays().remove(searchMarker);
        }

        searchMarker = new Marker(mapView);
        searchMarker.setPosition(point);
        searchMarker.setTitle(name);
        searchMarker.setSnippet(address);

        mapView.getOverlays().add(searchMarker);
        mapView.invalidate();

        if (hasCurrentLocation) {
            loadRoute(
                    currentLatitude,
                    currentLongitude,
                    latitude,
                    longitude,
                    false
            );
        } else {
            Toast.makeText(
                    this,
                    "Current location is not available",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // ALTERNATIVE ROUTES
    // =========================================================

    private void loadRoute(
            double startLat,
            double startLon,
            double endLat,
            double endLon,
            boolean automaticReroute) {

        if (executorService == null || executorService.isShutdown()) return;

        if (automaticReroute) rerouteInProgress = true;

        executorService.execute(() -> {

            HttpURLConnection connection = null;

            try {

                String urlString =
                        "https://router.project-osrm.org/route/v1/driving/"
                                + startLon + "," + startLat
                                + ";" + endLon + "," + endLat
                                + "?overview=full&geometries=geojson"
                                + "&steps=true&alternatives=true";

                connection = (HttpURLConnection)
                        new URL(urlString).openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(25000);
                connection.setRequestProperty("User-Agent", "SPONDON/1.0");

                if (connection.getResponseCode() != 200) {
                    throw new Exception(
                            "Route server error: "
                                    + connection.getResponseCode()
                    );
                }

                JSONObject json = new JSONObject(readResponse(connection));

                if (!"Ok".equalsIgnoreCase(json.optString("code"))) {
                    throw new Exception("No route available");
                }

                JSONArray routes = json.getJSONArray("routes");
                ArrayList<RouteOption> loadedRoutes = new ArrayList<>();

                for (int r = 0; r < routes.length(); r++) {

                    JSONObject route = routes.getJSONObject(r);
                    JSONArray coordinates = route.getJSONObject("geometry")
                            .getJSONArray("coordinates");

                    RouteOption option = new RouteOption();

                    for (int i = 0; i < coordinates.length(); i++) {

                        JSONArray coordinate = coordinates.getJSONArray(i);

                        option.points.add(new GeoPoint(
                                coordinate.getDouble(1),
                                coordinate.getDouble(0)
                        ));
                    }

                    option.distance = route.optDouble("distance", 0);
                    option.duration = route.optDouble("duration", 0);
                    option.directions = getTurnByTurnDirections(route);

                    if (!option.points.isEmpty()) {
                        loadedRoutes.add(option);
                    }
                }

                runOnUiThread(() -> {

                    rerouteInProgress = false;

                    if (isFinishing() || isDestroyed()) return;

                    if (loadedRoutes.isEmpty()) {
                        Toast.makeText(
                                this,
                                "No route found",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    routeOptions.clear();
                    routeOptions.addAll(loadedRoutes);

                    if (automaticReroute) {

                        lastRerouteTime = System.currentTimeMillis();
                        offRouteCount = 0;

                        displayRoute(routeOptions.get(0));

                        Toast.makeText(
                                this,
                                "Route updated",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {
                        showAlternativeRoutes();
                    }
                });

            } catch (Exception e) {

                Log.e(TAG, "Route loading failed", e);

                runOnUiThread(() -> {

                    rerouteInProgress = false;

                    if (isFinishing() || isDestroyed()) return;

                    Toast.makeText(
                            this,
                            "Unable to load route: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });

            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void showAlternativeRoutes() {

        String[] choices = new String[routeOptions.size()];

        for (int i = 0; i < routeOptions.size(); i++) {

            RouteOption route = routeOptions.get(i);

            choices[i] = String.format(
                    Locale.getDefault(),
                    "Route %d  •  %.2f km  •  %.0f min",
                    i + 1,
                    route.distance / 1000.0,
                    route.duration / 60.0
            );
        }

        new AlertDialog.Builder(this)
                .setTitle("Choose a Route")
                .setItems(choices, (dialog, which) ->
                        displayRoute(routeOptions.get(which)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void displayRoute(RouteOption route) {

        activeRoutePoints.clear();
        activeRoutePoints.addAll(route.points);

        totalRouteDistanceMeters = route.distance;
        totalRouteDurationSeconds = route.duration;

        currentDirections.clear();
        currentDirections.addAll(route.directions);

        if (routeLine != null) {
            mapView.getOverlays().remove(routeLine);
        }

        routeLine = new Polyline(mapView);
        routeLine.setPoints(route.points);
        routeLine.setColor(Color.BLUE);
        routeLine.setWidth(10f);

        mapView.getOverlays().add(routeLine);

        mapView.zoomToBoundingBox(
                BoundingBox.fromGeoPoints(route.points),
                true
        );

        tvRouteDistance.setText(String.format(
                Locale.getDefault(),
                "Distance: %.2f km",
                route.distance / 1000.0
        ));

        tvRouteDuration.setText(String.format(
                Locale.getDefault(),
                "Estimated time: %.0f min",
                route.duration / 60.0
        ));

        btnDirections.setEnabled(!currentDirections.isEmpty());
        layoutRouteInfo.setVisibility(View.VISIBLE);

        offRouteCount = 0;
        updateNavigationProgress();
        mapView.invalidate();
    }

    private ArrayList<String> getTurnByTurnDirections(JSONObject route)
            throws Exception {

        ArrayList<String> directions = new ArrayList<>();
        JSONArray legs = route.getJSONArray("legs");

        for (int i = 0; i < legs.length(); i++) {

            JSONArray steps = legs.getJSONObject(i).getJSONArray("steps");

            for (int j = 0; j < steps.length(); j++) {

                JSONObject step = steps.getJSONObject(j);
                JSONObject maneuver = step.getJSONObject("maneuver");

                String type = maneuver.optString("type", "");
                String modifier = maneuver.optString("modifier", "");
                String roadName = step.optString("name", "");

                double distance = step.optDouble("distance", 0) / 1000.0;

                String instruction;

                switch (type) {
                    case "depart":
                        instruction = "Start your journey";
                        break;
                    case "arrive":
                        instruction = "Arrive at destination";
                        break;
                    case "turn":
                        instruction = "Turn " + modifier;
                        break;
                    case "continue":
                        instruction = "Continue straight";
                        break;
                    case "merge":
                        instruction = "Merge";
                        break;
                    case "fork":
                        instruction = "Keep " + modifier + " at the fork";
                        break;
                    case "roundabout":
                    case "rotary":
                        instruction = "Enter the roundabout";
                        break;
                    case "off ramp":
                        instruction = "Take the exit ramp";
                        break;
                    case "on ramp":
                        instruction = "Take the ramp";
                        break;
                    case "uturn":
                        instruction = "Make a U-turn";
                        break;
                    default:
                        instruction = type.isEmpty()
                                ? "Continue"
                                : type.replace("_", " ");
                        break;
                }

                if (!roadName.isEmpty()) {
                    instruction += " onto " + roadName;
                }

                instruction += String.format(
                        Locale.getDefault(),
                        " (%.2f km)",
                        distance
                );

                directions.add(instruction);
            }
        }

        return directions;
    }

    private void showDirectionsDialog(ArrayList<String> directions) {

        if (directions == null || directions.isEmpty()) {
            Toast.makeText(
                    this,
                    "Directions unavailable",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Turn-by-Turn Directions")
                .setItems(directions.toArray(new String[0]), null)
                .setPositiveButton("Close", null)
                .show();
    }

    // =========================================================
    // NAVIGATION + REROUTING
    // =========================================================

    private void updateNavigationProgress() {

        if (activeRoutePoints.isEmpty()
                || totalRouteDistanceMeters <= 0) {
            return;
        }

        int nearestIndex = 0;
        float nearestDistance = Float.MAX_VALUE;
        float[] result = new float[1];

        for (int i = 0; i < activeRoutePoints.size(); i++) {

            GeoPoint point = activeRoutePoints.get(i);

            Location.distanceBetween(
                    currentLatitude,
                    currentLongitude,
                    point.getLatitude(),
                    point.getLongitude(),
                    result
            );

            if (result[0] < nearestDistance) {
                nearestDistance = result[0];
                nearestIndex = i;
            }
        }

        double remainingDistance = 0;

        for (int i = nearestIndex;
             i < activeRoutePoints.size() - 1; i++) {

            GeoPoint a = activeRoutePoints.get(i);
            GeoPoint b = activeRoutePoints.get(i + 1);

            float[] segment = new float[1];

            Location.distanceBetween(
                    a.getLatitude(),
                    a.getLongitude(),
                    b.getLatitude(),
                    b.getLongitude(),
                    segment
            );

            remainingDistance += segment[0];
        }

        remainingDistance = Math.min(
                remainingDistance,
                totalRouteDistanceMeters
        );

        double remainingSeconds = totalRouteDurationSeconds
                * (remainingDistance / totalRouteDistanceMeters);

        tvRemainingDistance.setText(String.format(
                Locale.getDefault(),
                "Remaining distance: %.2f km",
                remainingDistance / 1000.0
        ));

        tvRemainingTime.setText(String.format(
                Locale.getDefault(),
                "Remaining time: %d min",
                (int) Math.ceil(remainingSeconds / 60.0)
        ));

        if (!currentDirections.isEmpty()) {
            tvNextInstruction.setText(
                    "Next: " + currentDirections.get(0)
            );
        } else {
            tvNextInstruction.setText("Next: Follow the route");
        }

        checkOffRoute(nearestDistance);
    }

    private void checkOffRoute(float distanceFromRoute) {

        if (destinationName.isEmpty()
                || !hasCurrentLocation
                || rerouteInProgress
                || activeRoutePoints.isEmpty()) {
            return;
        }

        if (distanceFromRoute > 80) {
            offRouteCount++;
        } else {
            offRouteCount = 0;
        }

        long now = System.currentTimeMillis();

        if (offRouteCount >= 2
                && now - lastRerouteTime > 25000) {

            offRouteCount = 0;
            lastRerouteTime = now;

            Toast.makeText(
                    this,
                    "You are off route. Finding a new route...",
                    Toast.LENGTH_LONG
            ).show();

            loadRoute(
                    currentLatitude,
                    currentLongitude,
                    destinationLatitude,
                    destinationLongitude,
                    true
            );
        }
    }

    // =========================================================
    // CLEAR ROUTE
    // =========================================================

    private void clearRoute() {

        if (routeLine != null) {
            mapView.getOverlays().remove(routeLine);
            routeLine = null;
        }

        if (searchMarker != null) {
            mapView.getOverlays().remove(searchMarker);
            searchMarker = null;
        }

        activeRoutePoints.clear();
        currentDirections.clear();
        routeOptions.clear();

        totalRouteDistanceMeters = 0;
        totalRouteDurationSeconds = 0;

        destinationName = "";
        destinationLatitude = 0;
        destinationLongitude = 0;

        offRouteCount = 0;
        rerouteInProgress = false;

        btnDirections.setEnabled(false);

        tvRouteDistance.setText("Distance: --");
        tvRouteDuration.setText("Estimated time: --");
        tvRemainingDistance.setText("Remaining distance: --");
        tvRemainingTime.setText("Remaining time: --");
        tvNextInstruction.setText("Next: --");

        layoutRouteInfo.setVisibility(View.GONE);
        mapView.invalidate();

        Toast.makeText(
                this,
                "Route cleared",
                Toast.LENGTH_SHORT
        ).show();
    }

    // =========================================================
    // MY LOCATION
    // =========================================================

    private void goToCurrentLocation() {

        if (!hasCurrentLocation) {
            Toast.makeText(
                    this,
                    "Current location not available yet",
                    Toast.LENGTH_SHORT
            ).show();

            checkLocationPermission();
            return;
        }

        GeoPoint point = new GeoPoint(
                currentLatitude,
                currentLongitude
        );

        mapView.getController().animateTo(point);
        mapView.getController().setZoom(16.0);
        mapView.invalidate();
    }

    // =========================================================
    // LOCATION PERMISSION
    // =========================================================

    private void checkLocationPermission() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

                && ActivityCompat.checkSelfPermission(
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

        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

                && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {

            locationManager.removeUpdates(locationListener);

            Location lastLocation = null;

            try {
                lastLocation = locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                );
            } catch (Exception e) {
                Log.e(TAG, "GPS location error", e);
            }

            if (lastLocation == null) {
                try {
                    lastLocation = locationManager.getLastKnownLocation(
                            LocationManager.NETWORK_PROVIDER
                    );
                } catch (Exception e) {
                    Log.e(TAG, "Network location error", e);
                }
            }

            if (lastLocation != null) {
                updateCurrentLocation(lastLocation);
            }

            boolean gpsEnabled = locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
            );

            String provider = gpsEnabled
                    ? LocationManager.GPS_PROVIDER
                    : LocationManager.NETWORK_PROVIDER;

            locationManager.requestLocationUpdates(
                    provider,
                    5000,
                    10,
                    locationListener
            );

        } catch (Exception e) {
            Log.e(TAG, "Location update error", e);
        }
    }

    private final LocationListener locationListener =
            new LocationListener() {

                @Override
                public void onLocationChanged(@NonNull Location location) {
                    updateCurrentLocation(location);
                }
            };

    private void updateCurrentLocation(Location location) {

        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        hasCurrentLocation = true;

        GeoPoint point = new GeoPoint(
                currentLatitude,
                currentLongitude
        );

        runOnUiThread(() -> {

            if (isFinishing() || isDestroyed()) return;

            if (currentMarker == null) {
                currentMarker = new Marker(mapView);
                currentMarker.setTitle("Your Current Location");
                mapView.getOverlays().add(currentMarker);
            }

            currentMarker.setPosition(point);

            if (!activeRoutePoints.isEmpty()) {
                updateNavigationProgress();
            }

            mapView.invalidate();
        });

        if (!nearbyLoaded) {
            nearbyLoaded = true;
            loadNearbyPlaces(currentLatitude, currentLongitude);
        }

        // A destination handed in by another screen (e.g. a shelter) had no GPS fix yet: route now.
        if (pendingIncomingRoute && !destinationName.isEmpty()) {
            pendingIncomingRoute = false;
            loadRoute(currentLatitude, currentLongitude,
                    destinationLatitude, destinationLongitude, false);
        }
    }

    // =========================================================
    // CITIZEN ISSUE MARKERS - FIRESTORE
    // =========================================================

    private void loadCitizenIssueMarkers() {

        reportsListener = firestore.collection("reports")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Log.e(TAG, "Reports loading failed", error);

                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(
                                    this,
                                    "Unable to load citizen reports",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                        return;
                    }

                    if (snapshots == null) return;
                    if (isFinishing() || isDestroyed()) return;

                    displayCitizenIssueMarkers(snapshots);
                });
    }

    private void displayCitizenIssueMarkers(QuerySnapshot snapshots) {

        // Keep latest Firestore data for filter changes
        latestReportsSnapshot = snapshots;

        for (Marker marker : issueMarkers) {
            mapView.getOverlays().remove(marker);
        }

        issueMarkers.clear();

        int totalReports = 0;

        for (DocumentSnapshot document : snapshots.getDocuments()) {

            Object latitudeObject = document.get("latitude");
            Object longitudeObject = document.get("longitude");

            if (!(latitudeObject instanceof Number)
                    || !(longitudeObject instanceof Number)) {
                continue;
            }

            double latitude =
                    ((Number) latitudeObject).doubleValue();

            double longitude =
                    ((Number) longitudeObject).doubleValue();

            if (latitude < -90 || latitude > 90
                    || longitude < -180 || longitude > 180) {
                continue;
            }

            String title = document.getString("title");
            String category = document.getString("category");
            String description = document.getString("description");
            String location = document.getString("location");
            String status = document.getString("status");

            if (title == null || title.trim().isEmpty()) {
                title = "Citizen Report";
            }

            if (category == null) category = "Other";
            if (description == null) description = "No description";
            if (location == null) location = "Location unavailable";
            if (status == null) status = "Submitted";

            // Category filter
            if (!selectedIssueCategory.equalsIgnoreCase("All")
                    && !category.trim().equalsIgnoreCase(
                    selectedIssueCategory)) {
                continue;
            }

            // Status filter
            if (!selectedIssueStatus.equalsIgnoreCase("All")
                    && !status.trim().equalsIgnoreCase(
                    selectedIssueStatus)) {
                continue;
            }

            addIssueMarker(
                    latitude,
                    longitude,
                    title,
                    category,
                    description,
                    location,
                    status
            );

            totalReports++;
        }

        mapView.invalidate();

        Log.d(
                TAG,
                "Filtered citizen issue markers: " + totalReports
        );
    }

    private void addIssueMarker(
            double latitude,
            double longitude,
            String title,
            String category,
            String description,
            String location,
            String status) {

        GeoPoint point = new GeoPoint(latitude, longitude);
        Marker marker = new Marker(mapView);

        marker.setPosition(point);
        marker.setTitle(title);
        marker.setSnippet(category + " | " + status);

        // Status-based marker color
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(getIssueMarkerColor(status));
        drawable.setStroke(4, Color.WHITE);
        drawable.setSize(42, 42);

        marker.setIcon(drawable);
        marker.setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_CENTER
        );

        marker.setOnMarkerClickListener((clickedMarker, map) -> {

            String details =
                    "Category: " + category
                            + "\n\nDescription: " + description
                            + "\n\nLocation: " + location
                            + "\n\nStatus: " + status
                            + "\n\nCoordinates: "
                            + latitude + ", " + longitude;

            new AlertDialog.Builder(CityMapActivity.this)
                    .setTitle(title)
                    .setMessage(details)
                    .setPositiveButton("Get Route", (dialog, which) -> {

                        destinationName = title;
                        destinationLatitude = latitude;
                        destinationLongitude = longitude;

                        if (hasCurrentLocation) {
                            loadRoute(
                                    currentLatitude,
                                    currentLongitude,
                                    latitude,
                                    longitude,
                                    false
                            );
                        } else {
                            Toast.makeText(
                                    CityMapActivity.this,
                                    "Current location not available",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    })
                    .setNegativeButton("Close", null)
                    .show();

            return true;
        });

        issueMarkers.add(marker);
        mapView.getOverlays().add(marker);
    }

    // Status color mapping
    private int getIssueMarkerColor(String status) {

        if (status == null) return Color.GRAY;

        switch (status.toLowerCase(Locale.ROOT).trim()) {

            case "submitted":
                return Color.RED;

            case "in progress":
                return Color.rgb(255, 152, 0);

            case "resolved":
                return Color.GREEN;

            case "rejected":
                return Color.GRAY;

            default:
                return Color.MAGENTA;
        }
    }

    // =========================================================
    // SMART ISSUE FILTER DIALOGS
    // =========================================================

    private void showIssueCategoryFilter() {

        String[] categories = {
                "All",
                "Road Damage",
                "Broken Streetlight",
                "Drainage Problem",
                "Garbage Problem",
                "Waterlogging",
                "Other"
        };

        new AlertDialog.Builder(this)
                .setTitle("Filter Reports by Category")
                .setSingleChoiceItems(
                        categories,
                        getSelectedIndex(
                                categories,
                                selectedIssueCategory
                        ),
                        (dialog, which) -> {

                            selectedIssueCategory = categories[which];
                            dialog.dismiss();

                            showIssueStatusFilter();
                        }
                )
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Reset Filters", (dialog, which) -> {

                    selectedIssueCategory = "All";
                    selectedIssueStatus = "All";

                    refreshIssueMarkers();
                })
                .show();
    }

    private void showIssueStatusFilter() {

        String[] statuses = {
                "All",
                "Submitted",
                "In Progress",
                "Resolved",
                "Rejected"
        };

        new AlertDialog.Builder(this)
                .setTitle("Filter Reports by Status")
                .setSingleChoiceItems(
                        statuses,
                        getSelectedIndex(
                                statuses,
                                selectedIssueStatus
                        ),
                        (dialog, which) -> {

                            selectedIssueStatus = statuses[which];
                            dialog.dismiss();

                            refreshIssueMarkers();
                        }
                )
                .setNegativeButton("Back", (dialog, which) ->
                        showIssueCategoryFilter()
                )
                .setNeutralButton("Reset All", (dialog, which) -> {

                    selectedIssueCategory = "All";
                    selectedIssueStatus = "All";

                    refreshIssueMarkers();
                })
                .show();
    }

    private int getSelectedIndex(String[] values, String selected) {

        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(selected)) {
                return i;
            }
        }

        return 0;
    }

    private void refreshIssueMarkers() {

        if (latestReportsSnapshot != null) {
            displayCitizenIssueMarkers(latestReportsSnapshot);
        }

        Toast.makeText(
                this,
                "Category: " + selectedIssueCategory
                        + "\nStatus: " + selectedIssueStatus,
                Toast.LENGTH_SHORT
        ).show();
    }

    // =========================================================
    // NEARBY PLACES
    // =========================================================

    private void loadNearbyPlaces(double latitude, double longitude) {

        executorService.execute(() -> {

            String[][] categories = {
                    {"Hospital", "hospital", "amenity:hospital"},
                    {"Police Station", "police station", "amenity:police"},
                    {"Fire Station", "fire station", "amenity:fire_station"},
                    {"ATM", "atm", "amenity:atm"},
                    {"Pharmacy", "pharmacy", "amenity:pharmacy"},
                    {"Restaurant", "restaurant", "amenity:restaurant"},
                    {"University", "university", "amenity:university"},
                    {"Bus Stop", "bus stop", "highway:bus_stop"},
                    {"Fuel Station", "fuel", "amenity:fuel"},
                    {"Hotel", "hotel", "tourism:hotel"}
            };

            int totalFound = 0;
            String lastError = "";

            for (int i = 0; i < categories.length; i++) {

                try {

                    if (i > 0) Thread.sleep(1100);

                    totalFound += searchPhoton(
                            latitude,
                            longitude,
                            categories[i][0],
                            categories[i][1],
                            categories[i][2]
                    );

                } catch (Exception e) {

                    lastError = e.getMessage();

                    Log.e(
                            TAG,
                            categories[i][0] + " search failed",
                            e
                    );
                }
            }

            final int count = totalFound;
            final String error = lastError;

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) return;

                mapView.invalidate();

                String message = count > 0
                        ? count + " nearby places found"
                        : (error.isEmpty()
                        ? "No nearby places found"
                        : "Nearby search failed: " + error);

                Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_LONG
                ).show();
            });
        });
    }

    private int searchPhoton(
            double latitude,
            double longitude,
            String category,
            String searchText,
            String osmTag) throws Exception {

        String urlString = "https://photon.komoot.io/reverse?"
                + "lon=" + longitude
                + "&lat=" + latitude
                + "&limit=20"
                + "&osm_tag=" + URLEncoder.encode(osmTag, "UTF-8");

        HttpURLConnection connection = null;

        try {

            connection = (HttpURLConnection)
                    new URL(urlString).openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("User-Agent", "SPONDON/1.0");

            if (connection.getResponseCode() != 200) {
                throw new Exception(
                        "Server response " + connection.getResponseCode()
                );
            }

            JSONObject json = new JSONObject(readResponse(connection));
            JSONArray features = json.getJSONArray("features");

            int count = 0;

            for (int i = 0; i < features.length(); i++) {

                JSONObject feature = features.getJSONObject(i);

                JSONArray coordinates = feature.getJSONObject("geometry")
                        .getJSONArray("coordinates");

                double placeLon = coordinates.getDouble(0);
                double placeLat = coordinates.getDouble(1);

                float[] distance = new float[1];

                Location.distanceBetween(
                        latitude,
                        longitude,
                        placeLat,
                        placeLon,
                        distance
                );

                if (distance[0] > SEARCH_RADIUS) continue;

                JSONObject properties = feature.getJSONObject("properties");

                String name = properties.optString(
                        "name",
                        "Unnamed " + category
                );

                String address = properties.optString("street", "");

                addPlaceMarker(
                        placeLat,
                        placeLon,
                        name,
                        category,
                        address
                );

                count++;
            }

            return count;

        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void addPlaceMarker(
            double latitude,
            double longitude,
            String name,
            String category,
            String address) {

        runOnUiThread(() -> {

            GeoPoint point = new GeoPoint(latitude, longitude);
            Marker marker = new Marker(mapView);

            marker.setPosition(point);
            marker.setTitle(name);

            String details = category;

            if (address != null && !address.isEmpty()) {
                details += "\n" + address;
            }

            marker.setSnippet(details);

            if (category.equalsIgnoreCase("Hospital")) {

                marker.setIcon(
                        getResources().getDrawable(
                                R.drawable.marker_hospital
                        )
                );

            } else if (category.equalsIgnoreCase("Police Station")) {

                marker.setIcon(
                        getResources().getDrawable(
                                R.drawable.marker_police
                        )
                );

            } else if (category.equalsIgnoreCase("Fire Station")) {

                marker.setIcon(
                        getResources().getDrawable(
                                R.drawable.marker_fire
                        )
                );
            }

            marker.setOnMarkerClickListener((clickedMarker, map) -> {

                new AlertDialog.Builder(CityMapActivity.this)
                        .setTitle(clickedMarker.getTitle())
                        .setMessage("Do you want directions to this place?")
                        .setPositiveButton("Get Route", (dialog, which) -> {

                            GeoPoint destination = clickedMarker.getPosition();

                            destinationName = clickedMarker.getTitle();
                            destinationLatitude = destination.getLatitude();
                            destinationLongitude = destination.getLongitude();

                            if (hasCurrentLocation) {

                                loadRoute(
                                        currentLatitude,
                                        currentLongitude,
                                        destinationLatitude,
                                        destinationLongitude,
                                        false
                                );

                            } else {

                                Toast.makeText(
                                        this,
                                        "Current location not available",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();

                return true;
            });

            marker.setVisible(
                    activeFilter.equals("All")
                            || category.equalsIgnoreCase(activeFilter)
            );

            nearbyMarkers.add(marker);
            mapView.getOverlays().add(marker);

            mapView.invalidate();
        });
    }

    // =========================================================
    // FILTER NEARBY PLACES
    // =========================================================

    private void filterMarkers(String category) {

        activeFilter = category;

        for (Marker marker : nearbyMarkers) {

            String snippet = marker.getSnippet();

            marker.setVisible(
                    category.equals("All")
                            || (snippet != null
                            && snippet.toLowerCase(Locale.ROOT)
                            .contains(category.toLowerCase(Locale.ROOT)))
            );
        }

        Button[] buttons = {
                btnAll, btnHospital, btnPolice, btnFire,
                btnATM, btnPharmacy, btnRestaurant, btnUniversity,
                btnBusStop, btnFuel, btnHotel
        };

        String[] names = {
                "All", "Hospital", "Police Station", "Fire Station",
                "ATM", "Pharmacy", "Restaurant", "University",
                "Bus Stop", "Fuel Station", "Hotel"
        };

        for (int i = 0; i < buttons.length; i++) {

            boolean selected = names[i].equals(category);

            buttons[i].setBackgroundTintList(
                    ColorStateList.valueOf(
                            selected
                                    ? Color.rgb(25, 118, 210)
                                    : Color.rgb(220, 220, 220)
                    )
            );

            buttons[i].setTextColor(
                    selected ? Color.WHITE : Color.BLACK
            );
        }

        mapView.invalidate();
    }

    // =========================================================
    // SAVED PLACES
    // =========================================================

    private void saveCurrentPlace() {

        String name;
        double latitude;
        double longitude;

        if (!destinationName.isEmpty()) {

            name = destinationName;
            latitude = destinationLatitude;
            longitude = destinationLongitude;

        } else if (hasCurrentLocation) {

            name = "Current Location";
            latitude = currentLatitude;
            longitude = currentLongitude;

        } else {

            Toast.makeText(
                    this,
                    "Select a place or wait for GPS",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        final String placeName = name;
        final double placeLat = latitude;
        final double placeLon = longitude;

        String[] labels = {
                "Home",
                "University",
                "Favorite"
        };

        new AlertDialog.Builder(this)
                .setTitle("Save Place")
                .setItems(labels, (dialog, which) -> {

                    String label = labels[which];

                    try {

                        SharedPreferences prefs =
                                getSharedPreferences(PREFS, MODE_PRIVATE);

                        JSONArray saved = new JSONArray(
                                prefs.getString("saved_places", "[]")
                        );

                        JSONObject item = new JSONObject();

                        item.put("label", label);
                        item.put("name", placeName);
                        item.put("lat", placeLat);
                        item.put("lon", placeLon);

                        saved.put(item);

                        prefs.edit()
                                .putString("saved_places", saved.toString())
                                .apply();

                        Toast.makeText(
                                this,
                                label + " saved",
                                Toast.LENGTH_SHORT
                        ).show();

                    } catch (Exception e) {

                        Toast.makeText(
                                this,
                                "Unable to save place",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addRecentSearch(PlaceItem place) {

        try {

            SharedPreferences prefs =
                    getSharedPreferences(PREFS, MODE_PRIVATE);

            JSONArray old = new JSONArray(
                    prefs.getString("recent_searches", "[]")
            );

            JSONArray updated = new JSONArray();

            JSONObject item = new JSONObject();

            item.put("label", place.name);
            item.put("name", place.address);
            item.put("lat", place.latitude);
            item.put("lon", place.longitude);

            updated.put(item);

            for (int i = 0;
                 i < old.length() && updated.length() < 10;
                 i++) {

                JSONObject previous = old.getJSONObject(i);

                if (previous.optDouble("lat") != place.latitude
                        || previous.optDouble("lon") != place.longitude) {
                    updated.put(previous);
                }
            }

            prefs.edit()
                    .putString("recent_searches", updated.toString())
                    .apply();

        } catch (Exception e) {
            Log.e(TAG, "Recent search save failed", e);
        }
    }

    private void showStoredPlaces(boolean recent) {

        try {

            SharedPreferences prefs =
                    getSharedPreferences(PREFS, MODE_PRIVATE);

            String key = recent ? "recent_searches" : "saved_places";

            JSONArray stored = new JSONArray(
                    prefs.getString(key, "[]")
            );

            if (stored.length() == 0) {

                Toast.makeText(
                        this,
                        recent ? "No recent searches" : "No saved places",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            ArrayList<JSONObject> items = new ArrayList<>();
            ArrayList<String> labels = new ArrayList<>();

            for (int i = 0; i < stored.length(); i++) {

                JSONObject item = stored.getJSONObject(i);
                items.add(item);

                labels.add(
                        item.optString("label", "Place")
                                + " - "
                                + item.optString("name", "Unknown")
                );
            }

            new AlertDialog.Builder(this)
                    .setTitle(recent ? "Recent Searches" : "Saved Places")
                    .setItems(
                            labels.toArray(new String[0]),
                            (dialog, which) -> {

                                try {

                                    JSONObject item = items.get(which);

                                    String name = item.optString(
                                            "name",
                                            "Saved Place"
                                    );

                                    double lat = item.getDouble("lat");
                                    double lon = item.getDouble("lon");

                                    selectDestination(
                                            name,
                                            name,
                                            lat,
                                            lon
                                    );

                                } catch (Exception e) {

                                    Toast.makeText(
                                            this,
                                            "Unable to open place",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            }
                    )
                    .setNegativeButton("Close", null)
                    .show();

        } catch (Exception e) {
            Log.e(TAG, "Stored places error", e);
        }
    }

    // =========================================================
    // NETWORK RESPONSE
    // =========================================================

    private String readResponse(HttpURLConnection connection)
            throws Exception {

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
        );

        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();

        return response.toString();
    }

    // =========================================================
    // PERMISSION RESULT
    // =========================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

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
                startLocationUpdates();
            } else {
                Toast.makeText(
                        this,
                        "Location permission required",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    // =========================================================
    // ACTIVITY LIFECYCLE
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (mapView != null) {
            mapView.onResume();
        }

        if (sensorManager != null) {

            if (accelerometer != null) {
                sensorManager.registerListener(
                        this,
                        accelerometer,
                        SensorManager.SENSOR_DELAY_UI
                );
            }

            if (magnetometer != null) {
                sensorManager.registerListener(
                        this,
                        magnetometer,
                        SensorManager.SENSOR_DELAY_UI
                );
            }
        }

        if (locationManager != null) {
            checkLocationPermission();
        }
    }

    @Override
    protected void onPause() {

        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        if (mapView != null) {
            mapView.onPause();
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        if (reportsListener != null) {
            reportsListener.remove();
            reportsListener = null;
        }

        if (executorService != null) {
            executorService.shutdownNow();
        }

        super.onDestroy();
    }
}