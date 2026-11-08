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

public class CommunicationCenterActivity extends AppCompatActivity {

    private EditText etSearchContacts;
    private TextView tvCommCount, tvNoCommResults;
    private LinearLayout layoutCommContainer;
    private Button btnBackCommCenter;

    private Button btnCommAll, btnCommKCC, btnCommAdmin, btnCommPolice, btnCommConsumer, btnCommRTI;

    private final List<ContactDesk> contactList = new ArrayList<>();
    private String selectedCategory = "All";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication_center);

        etSearchContacts = findViewById(R.id.etSearchContacts);
        tvCommCount = findViewById(R.id.tvCommCount);
        tvNoCommResults = findViewById(R.id.tvNoCommResults);
        layoutCommContainer = findViewById(R.id.layoutCommContainer);
        btnBackCommCenter = findViewById(R.id.btnBackCommCenter);

        btnCommAll = findViewById(R.id.btnCommAll);
        btnCommKCC = findViewById(R.id.btnCommKCC);
        btnCommAdmin = findViewById(R.id.btnCommAdmin);
        btnCommPolice = findViewById(R.id.btnCommPolice);
        btnCommConsumer = findViewById(R.id.btnCommConsumer);
        btnCommRTI = findViewById(R.id.btnCommRTI);

        btnBackCommCenter.setOnClickListener(v -> finish());

        setupFilters();
        setupSearch();
        loadContactData();
        displayContacts();
    }

    private void setupFilters() {
        btnCommAll.setOnClickListener(v -> setCategory("All"));
        btnCommKCC.setOnClickListener(v -> setCategory("KCC"));
        btnCommAdmin.setOnClickListener(v -> setCategory("Admin"));
        btnCommPolice.setOnClickListener(v -> setCategory("Police"));
        btnCommConsumer.setOnClickListener(v -> setCategory("Consumer"));
        btnCommRTI.setOnClickListener(v -> setCategory("RTI"));
    }

    private void setCategory(String category) {
        selectedCategory = category;
        displayContacts();
    }

    private void setupSearch() {
        etSearchContacts.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                displayContacts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadContactData() {
        contactList.clear();

        // 1. City Corporation (KCC)
        contactList.add(new ContactDesk(
                "Mayor's Public Grievance & Citizen Cell",
                "KCC",
                "Nagar Bhaban, KDA Avenue, Khulna",
                "Mayor's Executive Liaison Desk",
                "Direct citizen grievance reception desk for unresolved ward municipal complaints, civic utility delays, and community memorandums.",
                "041-720100",
                "mayor@kcc.gov.bd"
        ));

        contactList.add(new ContactDesk(
                "KCC Chief Executive Officer (CEO) Secretariat",
                "KCC",
                "Nagar Bhaban (Level 3), Khulna",
                "Executive Municipal Management",
                "Administrative oversight for municipal tender inquiries, road rehabilitation approvals, and trade licensing appeals.",
                "041-720440",
                "ceo@kcc.gov.bd"
        ));

        // 2. DC Administration
        contactList.add(new ContactDesk(
                "Khulna Deputy Commissioner (DC) Office",
                "Admin",
                "Collectorate Bhaban, Court Road, Khulna",
                "District Magistrate & Revenue Secretariat",
                "District administrative liaison, land mutation grievance cell, executive magistrate inquiries, and public welfare petitions.",
                "041-720150",
                "dc.khulna@mopa.gov.bd"
        ));

        contactList.add(new ContactDesk(
                "Citizen Charter & One-Stop Service Counter",
                "Admin",
                "Deputy Commissioner Office Ground Floor, Khulna",
                "Public Documentation & Verification",
                "Fast-track processing for citizenship certificates, freedom fighter allowances, character verifications, and public notary services.",
                "041-721880",
                "onestop.khulna@mopa.gov.bd"
        ));

        // 3. Police & Legal Aid
        contactList.add(new ContactDesk(
                "Khulna Metropolitan Police (KMP) Control Room",
                "Police",
                "KMP Police Headquarters, Sonadanga, Khulna",
                "24/7 Central Law Enforcement Dispatch",
                "Round-the-clock emergency crime reporting, quick response team (QRT) dispatch, patrol van assistance, and GD follow-up.",
                "041-760333",
                "kmpcontrol@police.gov.bd"
        ));

        contactList.add(new ContactDesk(
                "National Women & Children Helpline (109)",
                "Police",
                "Ministry of Women and Children Affairs Liaison, Khulna",
                "Toll-Free 24 Hours Emergency Service",
                "Immediate counseling, police rescue coordination, and shelter assistance for victims of domestic violence, harassment, or child marriage.",
                "109",
                "helpline109@mowca.gov.bd"
        ));

        contactList.add(new ContactDesk(
                "District Legal Aid Office (Judicial Support)",
                "Police",
                "District & Sessions Judge Court Building, Khulna",
                "Government Free Legal Aid Service",
                "Free legal advice, panel lawyers, and court fee assistance for indigent citizens, workers, and underprivileged families.",
                "041-722110",
                "legalaid.khulna@mojpd.gov.bd"
        ));

        // 4. Consumer Rights
        contactList.add(new ContactDesk(
                "Directorate of National Consumer Rights Protection (DNCRP)",
                "Consumer",
                "Divisional Office, KDA Commercial Area, Khulna",
                "Consumer Grievance Hotline: 16121",
                "File formal complaints against commodity overpricing, counterfeit medicines, adulterated food, or refusal of warranty services. 25% fine share awarded to complainant upon conviction.",
                "16121",
                "ad-khulna@dncrp.gov.bd"
        ));

        // 5. Right to Information (RTI) Desk
        contactList.add(new ContactDesk(
                "Right to Information (RTI) Designated Officer Desk",
                "RTI",
                "Nagar Bhaban Public Relations Dept, Khulna",
                "Section 8, RTI Act 2009 Compliance Desk",
                "Citizens can formally request official information regarding municipal budgets, public project expenditures, contractor allocations, and ward plans.",
                "041-721330",
                "info.officer@kcc.gov.bd"
        ));
    }

    private void displayContacts() {
        layoutCommContainer.removeAllViews();
        int visibleCount = 0;

        for (ContactDesk desk : contactList) {
            boolean matchesCategory = selectedCategory.equals("All")
                    || desk.category.equalsIgnoreCase(selectedCategory);

            boolean matchesSearch = searchQuery.isEmpty()
                    || desk.name.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || desk.department.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || desk.location.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || desk.details.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || desk.category.toLowerCase(Locale.ROOT).contains(searchQuery);

            if (matchesCategory && matchesSearch) {
                renderContactCard(desk);
                visibleCount++;
            }
        }

        tvCommCount.setText("Available Communication Desks (" + visibleCount + " entries)");

        if (visibleCount == 0) {
            tvNoCommResults.setVisibility(View.VISIBLE);
        } else {
            tvNoCommResults.setVisibility(View.GONE);
        }
    }

    private void renderContactCard(ContactDesk desk) {
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

        // Header: Category Badge + DEMO tag
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView tag = new TextView(this);
        tag.setText(desk.category.toUpperCase(Locale.ROOT));
        tag.setTextSize(11);
        tag.setTypeface(null, Typeface.BOLD);
        tag.setPadding(dp(8), dp(3), dp(8), dp(3));

        GradientDrawable tagBg = new GradientDrawable();
        tagBg.setCornerRadius(dp(4));

        if (desk.category.equalsIgnoreCase("KCC")) {
            tagBg.setColor(Color.parseColor("#DBEAFE"));
            tag.setTextColor(Color.parseColor("#1E40AF"));
        } else if (desk.category.equalsIgnoreCase("Police")) {
            tagBg.setColor(Color.parseColor("#FEE2E2"));
            tag.setTextColor(Color.parseColor("#991B1B"));
        } else if (desk.category.equalsIgnoreCase("Admin")) {
            tagBg.setColor(Color.parseColor("#FEF3C7"));
            tag.setTextColor(Color.parseColor("#92400E"));
        } else {
            tagBg.setColor(Color.parseColor("#EDE9FE"));
            tag.setTextColor(Color.parseColor("#6D28D9"));
        }
        tag.setBackground(tagBg);

        TextView demoTag = new TextView(this);
        demoTag.setText("OFFICIAL DESK");
        demoTag.setTextColor(Color.parseColor("#0F766E"));
        demoTag.setTextSize(10);
        demoTag.setTypeface(null, Typeface.BOLD);
        demoTag.setPadding(dp(10), dp(3), 0, 0);

        top.addView(tag);
        top.addView(demoTag);
        card.addView(top);

        // Name
        TextView name = new TextView(this);
        name.setText(desk.name);
        name.setTextSize(16);
        name.setTextColor(Color.parseColor("#0F172A"));
        name.setTypeface(null, Typeface.BOLD);
        name.setPadding(0, dp(6), 0, dp(2));
        card.addView(name);

        // Department & Location
        TextView dept = new TextView(this);
        dept.setText(desk.department + " • " + desk.location);
        dept.setTextSize(13);
        dept.setTextColor(Color.parseColor("#475569"));
        card.addView(dept);

        TextView det = new TextView(this);
        det.setText(desk.details);
        det.setTextSize(12);
        det.setTextColor(Color.parseColor("#334155"));
        det.setPadding(0, dp(4), 0, dp(8));
        card.addView(det);

        // Action Buttons Row: Call & Email
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        if (desk.phone != null && !desk.phone.isEmpty()) {
            Button btnCall = new Button(this);
            btnCall.setText("Call (" + desk.phone + ")");
            btnCall.setTextSize(12);
            btnCall.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1E293B")));
            btnCall.setTextColor(Color.WHITE);
            btnCall.setOnClickListener(v -> {
                String cleanNum = desk.phone.replaceAll("[^0-9]", "");
                Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + cleanNum));
                startActivity(callIntent);
            });
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            btnCall.setLayoutParams(btnParams);
            actions.addView(btnCall);
        }

        if (desk.email != null && !desk.email.isEmpty()) {
            Button btnEmail = new Button(this);
            btnEmail.setText("Email Inquiry");
            btnEmail.setTextSize(12);
            btnEmail.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0284C7")));
            btnEmail.setTextColor(Color.WHITE);
            btnEmail.setOnClickListener(v -> {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:" + desk.email));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Citizen Inquiry - SPONDON Smart City");
                try {
                    startActivity(emailIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "No email client found", Toast.LENGTH_SHORT).show();
                }
            });
            LinearLayout.LayoutParams emailParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            emailParams.setMarginStart(dp(6));
            btnEmail.setLayoutParams(emailParams);
            actions.addView(btnEmail);
        }

        card.addView(actions);
        layoutCommContainer.addView(card);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class ContactDesk {
        String name, category, location, department, details, phone, email;

        ContactDesk(String name, String category, String location, String department,
                    String details, String phone, String email) {
            this.name = name;
            this.category = category;
            this.location = location;
            this.department = department;
            this.details = details;
            this.phone = phone;
            this.email = email;
        }
    }
}
