package com.maternaltracker.india;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class MainActivity extends Activity {
    private MaternalDbHelper db;
    private LinearLayout content;
    private TextView patientIdPill;

    private EditText patientId;
    private EditText patientName;
    private EditText mobile;
    private AutoCompleteTextView state;
    private AutoCompleteTextView district;
    private AutoCompleteTextView subdistrict;
    private AutoCompleteTextView localBodyType;
    private AutoCompleteTextView localBody;
    private AutoCompleteTextView ward;
    private AutoCompleteTextView village;
    private EditText lmpDate;
    private EditText eddDate;
    private AutoCompleteTextView motivator;
    private AutoCompleteTextView doctor;
    private EditText visit1;
    private EditText visit2;
    private EditText visit3;
    private EditText finalVisit;
    private EditText remarks;
    private String selectedStateCode;
    private String selectedDistrictCode;
    private String selectedSubdistrictCode;
    private String selectedLocalBodyCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new MaternalDbHelper(this);
        db.getWritableDatabase();
        LocationImporter.importBundledLgd(this, db);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(238, 243, 248));
        root.setPadding(dp(14), dp(14), dp(14), dp(14));
        setContentView(root);

        root.addView(header());
        root.addView(navBar());

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        showPatientForm();
    }

    private View header() {
        LinearLayout box = card();
        box.setPadding(dp(18), dp(14), dp(18), dp(14));
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        TextView title = label("Maternal Tracker India", 22, true);
        title.setTextColor(Color.rgb(31, 45, 61));
        TextView subtitle = label("All-India patient registration with LGD-ready location data", 12, false);
        subtitle.setTextColor(Color.rgb(93, 109, 126));
        text.addView(title);
        text.addView(subtitle);
        box.addView(text, new LinearLayout.LayoutParams(0, -2, 1));

        patientIdPill = label("New patient", 13, true);
        patientIdPill.setTextColor(Color.rgb(23, 107, 58));
        patientIdPill.setGravity(Gravity.CENTER);
        patientIdPill.setBackgroundColor(Color.rgb(237, 247, 241));
        patientIdPill.setPadding(dp(12), dp(8), dp(12), dp(8));
        box.addView(patientIdPill);
        return box;
    }

    private View navBar() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.addView(navButton("Patient Entry", v -> showPatientForm()));
        row.addView(navButton("Patients", v -> showPatientList()));
        row.addView(navButton("Refresh Location Data", v -> {
            LocationImporter.importBundledLgd(this, db);
            refreshLocationAdapters();
            Toast.makeText(this, "Location data refreshed", Toast.LENGTH_SHORT).show();
        }));
        scroll.addView(row);
        return scroll;
    }

    private void showPatientForm() {
        content.removeAllViews();
        selectedStateCode = null;
        selectedDistrictCode = null;
        selectedSubdistrictCode = null;
        selectedLocalBodyCode = null;
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(0, 0, 0, dp(20));
        scroll.addView(form);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));

        patientId = readOnlyInput(db.nextPatientId());
        patientIdPill.setText(patientId.getText().toString());
        patientName = input("");
        mobile = input("");
        state = auto(db.listStates());
        district = auto(db.listDistricts(selectedStateCode));
        subdistrict = auto(db.listSubdistricts(selectedDistrictCode));
        localBodyType = auto(list("Panchayat", "Municipality", "Municipal Corporation", "Nagar Panchayat", "Traditional Local Body"));
        localBody = auto(db.listLocalBodies(selectedDistrictCode));
        ward = auto(db.listWards(selectedLocalBodyCode));
        village = auto(db.listVillages(selectedDistrictCode, selectedSubdistrictCode, selectedLocalBodyCode));
        lmpDate = input(LocalDate.now().toString());
        eddDate = readOnlyInput(LocalDate.now().plusDays(280).toString());
        motivator = auto(db.listNames("custom_motivators"));
        doctor = auto(db.listNames("custom_doctors"));
        visit1 = readOnlyInput(LocalDate.now().toString());
        visit2 = input("");
        visit3 = input("");
        finalVisit = input("");
        remarks = input("");

        patientId.addTextChangedListener(simpleWatcher(s -> patientIdPill.setText(s)));
        lmpDate.addTextChangedListener(simpleWatcher(s -> updateEdd()));
        state.setOnItemClickListener((parent, view, position, id) -> refreshDistricts());
        district.setOnItemClickListener((parent, view, position, id) -> refreshDistrictChildren());
        subdistrict.setOnItemClickListener((parent, view, position, id) -> refreshVillageAdapter());
        localBody.setOnItemClickListener((parent, view, position, id) -> refreshWardAndVillageAdapters());

        form.addView(section("Patient Information",
                row("Patient ID", patientId),
                row("Patient Name *", patientName),
                row("Mobile Number *", mobile),
                row("State / UT *", state),
                row("District *", district),
                row("Sub-District", subdistrict),
                row("Panchayat / Municipality Type", localBodyType),
                row("Panchayat / Municipality", localBody),
                row("Ward", ward),
                row("Village", village),
                row("Motivator Name *", motivator),
                row("Doctor Name", doctor)
        ));
        form.addView(section("Pregnancy Dates",
                row("LMP Date *", lmpDate),
                row("EDD Date", eddDate),
                row("Entry / 1st Visit", visit1)
        ));
        form.addView(section("Visit Tracking",
                row("2nd Visit", visit2),
                row("3rd Visit", visit3),
                row("Final Visit", finalVisit),
                row("Remarks", remarks)
        ));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        actions.addView(button("Clear", v -> showPatientForm()));
        actions.addView(button("Save Patient", v -> savePatient()));
        form.addView(actions);
    }

    private void showPatientList() {
        content.removeAllViews();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        content.addView(root, new LinearLayout.LayoutParams(-1, -1));

        EditText search = input("");
        search.setHint("Search name, patient ID, or mobile");
        root.addView(search);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        Runnable reload = () -> {
            list.removeAllViews();
            List<Patient> patients = db.listPatients(search.getText().toString());
            TextView count = label(patients.size() + " patient(s)", 13, true);
            list.addView(count);
            for (Patient p : patients) {
                TextView item = label(
                        p.patientId + "  |  " + p.patientName + "\n" +
                                nullText(p.mobileNumber) + "  |  " + nullText(p.districtName) + ", " + nullText(p.stateName) + "\n" +
                                "Motivator: " + nullText(p.motivatorName) + "  Doctor: " + nullText(p.doctorName),
                        14,
                        false
                );
                item.setPadding(dp(12), dp(10), dp(12), dp(10));
                list.addView(item);
            }
        };
        search.addTextChangedListener(simpleWatcher(s -> reload.run()));
        reload.run();
    }

    private void savePatient() {
        Patient p = new Patient();
        p.patientId = text(patientId);
        p.patientName = text(patientName);
        p.mobileNumber = text(mobile);
        p.stateName = text(state);
        p.districtName = text(district);
        p.subdistrictName = text(subdistrict);
        p.localBodyType = text(localBodyType);
        p.localBodyName = text(localBody);
        p.wardName = text(ward);
        p.villageName = text(village);
        p.lmpDate = text(lmpDate);
        p.eddDate = text(eddDate);
        p.motivatorName = text(motivator);
        p.doctorName = text(doctor);
        p.visit1 = text(visit1);
        p.visit2 = text(visit2);
        p.visit3 = text(visit3);
        p.finalVisit = text(finalVisit);
        p.entryDate = LocalDate.now().toString();
        p.remarks = text(remarks);

        if (empty(p.patientName) || empty(p.mobileNumber) || empty(p.stateName) || empty(p.districtName) || empty(p.motivatorName)) {
            Toast.makeText(this, "Fill patient name, mobile, state, district, and motivator", Toast.LENGTH_LONG).show();
            return;
        }
        if (!validDate(p.lmpDate) || !validDate(p.visit1)) {
            Toast.makeText(this, "Use date format YYYY-MM-DD", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            db.savePatient(p);
            hideKeyboard();
            Toast.makeText(this, "Patient saved", Toast.LENGTH_SHORT).show();
            showPatientForm();
        } catch (Exception ex) {
            Toast.makeText(this, "Save failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void refreshLocationAdapters() {
        setAdapter(state, db.listStates());
        refreshDistricts();
    }

    private void refreshDistricts() {
        selectedStateCode = db.getCodeByName("states", text(state));
        selectedDistrictCode = null;
        selectedSubdistrictCode = null;
        selectedLocalBodyCode = null;
        setAdapter(district, db.listDistricts(selectedStateCode));
        district.setText("", false);
        refreshDistrictChildren();
    }

    private void refreshDistrictChildren() {
        selectedDistrictCode = db.getCodeByNameAndParent("districts", text(district), "state_code", selectedStateCode);
        selectedSubdistrictCode = null;
        selectedLocalBodyCode = null;
        setAdapter(subdistrict, db.listSubdistricts(selectedDistrictCode));
        setAdapter(localBody, db.listLocalBodies(selectedDistrictCode));
        subdistrict.setText("", false);
        localBody.setText("", false);
        refreshWardAndVillageAdapters();
    }

    private void refreshWardAndVillageAdapters() {
        selectedLocalBodyCode = db.getCodeByNameAndParent("local_bodies", text(localBody), "district_code", selectedDistrictCode);
        setAdapter(ward, db.listWards(selectedLocalBodyCode));
        ward.setText("", false);
        refreshVillageAdapter();
    }

    private void refreshVillageAdapter() {
        selectedSubdistrictCode = db.getCodeByNameAndParent("subdistricts", text(subdistrict), "district_code", selectedDistrictCode);
        setAdapter(village, db.listVillages(selectedDistrictCode, selectedSubdistrictCode, selectedLocalBodyCode));
        village.setText("", false);
    }

    private void updateEdd() {
        try {
            LocalDate lmp = LocalDate.parse(text(lmpDate));
            eddDate.setText(lmp.plusDays(280).toString());
        } catch (DateTimeParseException ignored) {
        }
    }

    private LinearLayout section(String title, View... rows) {
        LinearLayout box = card();
        TextView heading = label(title, 17, true);
        heading.setTextColor(Color.rgb(44, 123, 229));
        box.addView(heading);
        for (View row : rows) {
            box.addView(row);
        }
        return box;
    }

    private LinearLayout row(String label, View input) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView tv = label(label, 12, true);
        tv.setTextColor(Color.rgb(45, 52, 54));
        row.addView(tv);
        row.addView(input, new LinearLayout.LayoutParams(-1, dp(46)));
        return row;
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(Color.WHITE);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(12));
        box.setLayoutParams(lp);
        return box;
    }

    private TextView label(String text, int sp, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(sp);
        tv.setTextColor(Color.rgb(31, 45, 61));
        if (bold) {
            tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        }
        return tv;
    }

    private EditText input(String value) {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setText(value == null ? "" : value);
        edit.setTextSize(15);
        edit.setPadding(dp(10), 0, dp(10), 0);
        return edit;
    }

    private EditText readOnlyInput(String value) {
        EditText edit = input(value);
        edit.setEnabled(false);
        return edit;
    }

    private AutoCompleteTextView auto(List<String> values) {
        AutoCompleteTextView view = new AutoCompleteTextView(this);
        view.setSingleLine(true);
        view.setThreshold(1);
        view.setTextSize(15);
        view.setPadding(dp(10), 0, dp(10), 0);
        setAdapter(view, values);
        return view;
    }

    private void setAdapter(AutoCompleteTextView view, List<String> values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, values);
        view.setAdapter(adapter);
    }

    private Button navButton(String text, View.OnClickListener listener) {
        Button b = button(text, listener);
        b.setAllCaps(false);
        return b;
    }

    private Button button(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(44));
        lp.setMargins(dp(4), 0, dp(4), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private TextWatcher simpleWatcher(TextCallback callback) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                callback.onText(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
    }

    private List<String> list(String... values) {
        return java.util.Arrays.asList(values);
    }

    private String text(TextView view) {
        String value = view.getText() == null ? "" : view.getText().toString().trim();
        return value.isEmpty() ? null : value;
    }

    private boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean validDate(String value) {
        if (empty(value)) {
            return true;
        }
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private String nullText(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private interface TextCallback {
        void onText(String text);
    }
}
