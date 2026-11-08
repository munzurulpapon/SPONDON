# SPONDON — Smart City Super App
## Demonstration Requirements & Modernized Blue-and-White Production UI Handoff Document

**Generated / Updated:** November 7, 2026  
**Application Name:** SPONDON (Smart City Super App)  
**Package Name:** `com.example.spondon`  
**Compatibility:** Android Studio Chipmunk (2021.2.1 RC 2) / Java 8 / AGP 7.2.0-rc02 / Gradle 7.3.3  
**Target / Compile SDK:** 32 | **Min SDK:** 21  
**Build Status:** **BUILD SUCCESSFUL (0 ERRORS, 0 WARNINGS)**  
**Generated Debug APK:** `app/build/outputs/apk/debug/app-debug.apk` (10,134,485 bytes / ~10.13 MB, 9:25 PM)  
**Deliverable Archive:** `SPONDON_Phase1_Updated.zip`  

---

## 1. Modern Blue-and-White UI/UX Implementation (Based on Reference Design)

The entire presentation tier of SPONDON has been overhauled with a modern, high-contrast **blue-and-white visual identity** inspired by the provided 10-screen UI reference specification:

### Design Tokens & Palette
* **Primary Deep Royal Blue:** `#0B4F9C` (Headers, hero brand sections, primary action buttons)
* **Primary Dark:** `#083B75` (Status bar tinting)
* **Soft Blue Surface / Accent:** `#E8F1FA` & `#00B4D8` (Photo upload container, badges, secondary pills)
* **Card & Neutral Backgrounds:** Background `#F8FAFC`, Pure White Cards `#FFFFFF` with subtle stroke `#E2E8F0` and `14dp`–`16dp` corner radiuses.
* **Typography:** Clean high-contrast typography (`#0F172A` title, `#475569` subtitle, `#94A3B8` hints).
* **Dynamic Status Badges:**
  * **Pending:** `#FEF3C7` background, `#D97706` text
  * **In Progress:** `#DBEAFE` background, `#2563EB` text
  * **Resolved:** `#DCFCE7` background, `#16A34A` text
  * **Emergency SOS:** `#EF4444` / `#DC2626` background `#FEE2E2`

### 10 Reference Screens Implemented
1. **Splash Screen (`activity_splash.xml`):**
   * High-contrast vibrant blue background with geometric skyscraper logo mark (`ic_spondon_logo_mark`).
   * "SPONDON" bold title, subtitle "Smart City • Safe City • Better Tomorrow".
   * Lower section: "Together for a Smarter Khulna" with circular progress spinner and smooth wave footer.
2. **Login Screen (`activity_login.xml` & `res/layout-land/activity_login.xml`):**
   * Crisp white background, SPONDON logo with skyline arches.
   * "Welcome Back!" heading, "Sign in to continue" subtitle.
   * Segmented role selector pills (Citizen / Provider / Admin).
   * Rounded input fields (`bg_edit_text`) with leading icons (Email envelope, Padlock).
   * Rounded deep-blue "Login" button, "OR" divider, "Create New Account" button, and "Forgot Password?" link.
   * Waterfront skyline illustration at footer.
3. **Citizen Dashboard (`activity_dashboard.xml` & `res/layout-land/activity_dashboard.xml`):**
   * Top app bar: Hamburger menu, "SPONDON / Khulna Smart City", Notification Bell with badge.
   * Hero greeting card: "Hello, Citizen 👋 / Let's build a safer Khulna together!".
   * City Safety Score card: Teal-to-emerald gradient card with green shield check icon, "92% Excellent ↗", clickable to City Pulse.
   * 3x3 Primary Quick Action Grid: Report Problem, Emergency SOS, City Services, Maps, Healthcare, Tourism, Jobs, Community, Utilities.
   * Bottom Navigation Bar: Home, Reports, Notifications, Profile.
4. **Report Problem Screen (`activity_report_issue.xml`):**
   * Deep blue action bar with back arrow and "Report Problem".
   * Large dashed/soft-blue photo upload card: "Tap to add photo" with camera icon.
   * Category dropdown card, Problem Title field, Description multiline card.
   * Location field: "Use Current Location" GPS button with live coordinate preview.
   * Wide deep-blue rounded "Submit Report" button.
5. **My Reports Screen (`activity_my_reports.xml` & `item_report_card.xml`):**
   * Deep blue action bar: back arrow, "My Reports", report count badge.
   * Rounded search input bar.
   * Horizontal filter pills: "All", "Pending", "In Progress", "Resolved".
   * Card items: Rounded thumbnail on left, dark bold title, location subtitle, relative time, and colored status badge pill on right.
6. **Khulna Map Screen (`activity_city_map.xml`):**
   * Deep blue action bar: back arrow, "Khulna Map", search icon.
   * Rounded search field and category filter pills (Hospital, Police, Fire, ATM, Pharmacy, etc.).
   * OSMDroid MapView with compass, directions, and place bookmarking.
7. **SOS Emergency Request Screen (`activity_emergency_request.xml`):**
   * Curved red hero banner: radiating emergency beacon icon, bold "SOS", "Emergency Request".
   * "Your location will be shared with emergency services."
   * Current location card with blue pin and live coordinates.
   * Bold red rounded "Send Emergency Alert" button.
   * "Add Contact" and "Import from Phone Contacts" buttons.
   * Direct hotlines (999, Fire 16163, Ambulance 16263).
8. **Disaster Guidelines Screen (`activity_disaster_management.xml`):**
   * Deep blue action bar: back arrow, "Disaster Guidelines", items count badge.
   * Horizontal filter pills: Cyclone, Flood, Fire, Earthquake, Shelters.
   * Featured Cyclone Preparedness card with banner, checkmarks (Keep kit ready, Stay informed, Move to shelter), and "View More Guidelines" button.
   * Official emergency guidelines & municipal shelters directory.
9. **City Services Screen (`activity_services.xml`):**
   * Deep blue action bar: back arrow, "City Services", services count.
   * Hero banner: "Smart Services for a Smart Khulna".
   * Featured sectors in rounded white cards with icons and chevrons: Healthcare, Transportation, Public Utilities.
   * Verified local expert filters (Electrician, Plumbing, Cleaning, Repairs) with booking actions.
10. **Citizen Profile Screen (`ProfileActivity.java` & `activity_profile.xml`):**
    * Circular avatar placeholder with blue person silhouette.
    * User name ("Citizen User"), email (`citizen@spondon.bd` / Firebase Auth email).
    * Rounded white cards: My Reports, Notifications (with red unread count badge), Rewards & Points, Settings, Help & Support, About SPONDON.
    * Rounded outline "Logout" button.

### Web & WebView Technical Applicability
* Added `app/src/main/assets/css/spondon_theme.css`: Clean, modern CSS stylesheet declaring the SPONDON blue-and-white theme, rounded card containers, status badges, typography, and responsive grid layout for WebViews.

---

## 2. Demonstration Audit Matrix

| # | Demonstration Requirement | Status | Exact Java / XML Implementation Files | Key Methods / Elements |
|---|---|---|---|---|
| **1** | **View Binding** | **FULLY IMPLEMENTED** | [app/build.gradle](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/build.gradle)<br>[LoginActivity.java](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/java/com/example/spondon/LoginActivity.java)<br>[CityIntelligenceActivity.java](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/java/com/example/spondon/CityIntelligenceActivity.java) | `buildFeatures { viewBinding true }`<br>`ActivityLoginBinding.inflate(getLayoutInflater())`<br>`binding.btnLogin.setOnClickListener(...)`<br>`ActivityCityIntelligenceBinding.inflate(getLayoutInflater())` |
| **2** | **RecyclerView, Custom Adapter & ViewHolder** | **FULLY IMPLEMENTED** | [ReportItem.java](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/java/com/example/spondon/ReportItem.java)<br>[item_report_card.xml](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/res/layout/item_report_card.xml)<br>[ReportAdapter.java](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/java/com/example/spondon/ReportAdapter.java)<br>[MyReportsActivity.java](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/java/com/example/spondon/MyReportsActivity.java) | `public static class ReportViewHolder extends RecyclerView.ViewHolder`<br>`onCreateViewHolder()`, `onBindViewHolder()`<br>`rvReports.setLayoutManager(new LinearLayoutManager(this))`<br>`rvReports.setAdapter(reportAdapter)` |
| **3** | **Firebase Storage Image Uploads** | **FULLY IMPLEMENTED** | [ReportActivity.java](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/java/com/example/spondon/ReportActivity.java) | `FirebaseStorage.getInstance().getReference().child("reports/" + UUID)`<br>`uploadTask.continueWithTask(...)`<br>`report.put("imageUrl", downloadUri.toString())`<br>Resilient offline fallback to local SQLite URI |
| **4** | **Broadcast Receivers (Airplane Mode & Battery Low)** | **FULLY IMPLEMENTED** | [SystemAlertReceiver.java](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/java/com/example/spondon/SystemAlertReceiver.java)<br>[AndroidManifest.xml](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/AndroidManifest.xml)<br>[DashboardActivity.java](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/java/com/example/spondon/DashboardActivity.java) | Handles `ACTION_AIRPLANE_MODE_CHANGED`, `ACTION_BATTERY_LOW`, and `ACTION_BATTERY_OKAY`<br>Broadcast receiver registered statically in manifest and dynamically in `DashboardActivity#onStart()` & `onStop()` |
| **5** | **Foreground Service with Persistent Notification** | **FULLY IMPLEMENTED** | [CivicMonitoringService.java](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/java/com/example/spondon/CivicMonitoringService.java)<br>[AndroidManifest.xml](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/AndroidManifest.xml)<br>[DashboardActivity.java](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/java/com/example/spondon/DashboardActivity.java) | Extends `android.app.Service`<br>`createNotificationChannel("spondon_civic_foreground")`<br>`startForeground(2026, notification)` with PendingIntent actions<br>Started on citizen dashboard login, stopped on logout |
| **6** | **Content Provider (Android ContactsContract)** | **FULLY IMPLEMENTED** | [EmergencyRequestActivity.java](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/java/com/example/spondon/EmergencyRequestActivity.java)<br>[activity_emergency_request.xml](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/res/layout/activity_emergency_request.xml)<br>[AndroidManifest.xml](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/AndroidManifest.xml) | `<uses-permission android:name="android.permission.READ_CONTACTS" />`<br>Runtime permission check `ActivityCompat.requestPermissions`<br>`Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)`<br>`getContentResolver().query(...)`<br>Inserts into SQLite `emergency_contacts` |
| **7** | **Runtime XML Parsing (XmlPullParser)** | **FULLY IMPLEMENTED** | [khulna_disaster_protocols.xml](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/res/xml/khulna_disaster_protocols.xml)<br>[DisasterManagementActivity.java](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/java/com/example/spondon/DisasterManagementActivity.java) | `XmlResourceParser parser = getResources().getXml(R.xml.khulna_disaster_protocols)`<br>`parser.getEventType()`, `START_TAG`, `TEXT`, `END_TAG`<br>Dynamically loads protocols tagged with `[XML PARSED]` |
| **8** | **ConstraintLayout & RelativeLayout** | **FULLY IMPLEMENTED** | [activity_splash.xml](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/res/layout/activity_splash.xml)<br>[item_report_card.xml](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/res/layout/item_report_card.xml)<br>[activity_my_reports.xml](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/res/layout/activity_my_reports.xml) | `activity_splash.xml`: `<androidx.constraintlayout.widget.ConstraintLayout>` with guidelines and chaining.<br>`item_report_card.xml`: `ConstraintLayout` with thumbnail and badge constraints.<br>`activity_my_reports.xml`: Top navigation header in `<RelativeLayout>`. |
| **9** | **Responsive Layouts & Landscape Support** | **FULLY IMPLEMENTED** | [res/layout-land/activity_login.xml](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/res/layout-land/activity_login.xml)<br>[res/layout-land/activity_dashboard.xml](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/res/layout-land/activity_dashboard.xml) | Dedicated `layout-land` directory for landscape orientation: Side-by-side branding and form split in Login; dual-column quick action hero + scrollable services directory in Citizen Dashboard. All buttons use `wrap_content` and `minHeight="48dp"`. |
| **10** | **State Preservation on Auto Rotation** | **FULLY IMPLEMENTED** | [ReportActivity.java](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/java/com/example/spondon/ReportActivity.java)<br>[MyReportsActivity.java](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/java/com/example/spondon/MyReportsActivity.java)<br>[EmergencyRequestActivity.java](file:///c:/Users/Papon/AndroidStudioProjects/SPONDON2%20-%20Copy%20(2)/app/src/main/java/com/example/spondon/EmergencyRequestActivity.java) | Overridden `onSaveInstanceState(Bundle outState)` and `onRestoreInstanceState(Bundle savedInstanceState)` to safeguard draft titles, descriptions, coordinates, selected image URIs, search queries, and filter chips across device rotation. |

---

## 3. Build & Delivery Verification

```
BUILD SUCCESSFUL in 53s
35 actionable tasks: 16 executed, 19 up-to-date
Output: app/build/outputs/apk/debug/app-debug.apk (10,134,485 bytes / ~10.13 MB)
Archive: SPONDON_Phase1_Updated.zip
```
