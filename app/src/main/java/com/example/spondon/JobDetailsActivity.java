package com.example.spondon;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class JobDetailsActivity extends AppCompatActivity {

    private TextView tvJobDetailTitle, tvJobDetailCompany, tvJobDetailTypeLoc;
    private TextView tvJobDetailSalary, tvJobDetailDeadline, tvJobDetailDesc, tvJobDetailReqs;
    private Button btnJobApplyDirect, btnBackJobDetail;

    private String jobId, title, company, category, type, salary, location, deadline, reqs, desc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_details);

        tvJobDetailTitle = findViewById(R.id.tvJobDetailTitle);
        tvJobDetailCompany = findViewById(R.id.tvJobDetailCompany);
        tvJobDetailTypeLoc = findViewById(R.id.tvJobDetailTypeLoc);
        tvJobDetailSalary = findViewById(R.id.tvJobDetailSalary);
        tvJobDetailDeadline = findViewById(R.id.tvJobDetailDeadline);
        tvJobDetailDesc = findViewById(R.id.tvJobDetailDesc);
        tvJobDetailReqs = findViewById(R.id.tvJobDetailReqs);

        btnJobApplyDirect = findViewById(R.id.btnJobApplyDirect);
        btnBackJobDetail = findViewById(R.id.btnBackJobDetail);

        jobId = getIntent().getStringExtra("JOB_ID");
        title = getIntent().getStringExtra("JOB_TITLE");
        company = getIntent().getStringExtra("JOB_COMPANY");
        category = getIntent().getStringExtra("JOB_CATEGORY");
        type = getIntent().getStringExtra("JOB_TYPE");
        salary = getIntent().getStringExtra("JOB_SALARY");
        location = getIntent().getStringExtra("JOB_LOCATION");
        deadline = getIntent().getStringExtra("JOB_DEADLINE");
        reqs = getIntent().getStringExtra("JOB_REQS");
        desc = getIntent().getStringExtra("JOB_DESC");

        if (title != null) tvJobDetailTitle.setText(title);
        if (company != null) tvJobDetailCompany.setText(company);
        if (type != null && location != null) tvJobDetailTypeLoc.setText("Type: " + type + " • Location: " + location);
        if (salary != null) tvJobDetailSalary.setText("Remuneration: " + salary);
        if (deadline != null) tvJobDetailDeadline.setText("Application Deadline: " + deadline);
        if (desc != null) tvJobDetailDesc.setText(desc);
        if (reqs != null) tvJobDetailReqs.setText(reqs);

        btnBackJobDetail.setOnClickListener(v -> finish());

        btnJobApplyDirect.setOnClickListener(v -> {
            Intent intent = new Intent(JobDetailsActivity.this, ApplyJobActivity.class);
            intent.putExtra("JOB_ID", jobId);
            intent.putExtra("JOB_TITLE", title);
            intent.putExtra("JOB_COMPANY", company);
            startActivity(intent);
        });
    }
}
