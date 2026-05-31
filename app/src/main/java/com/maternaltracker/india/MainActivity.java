package com.maternaltracker.india;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int BG_TOP = Color.rgb(239, 247, 250);
    private static final int BG_BOTTOM = Color.rgb(246, 241, 235);
    private static final int SURFACE = Color.WHITE;
    private static final int SURFACE_ALT = Color.rgb(247, 250, 252);
    private static final int PRIMARY = Color.rgb(26, 115, 110);
    private static final int PRIMARY_DARK = Color.rgb(14, 87, 82);
    private static final int ACCENT = Color.rgb(226, 140, 72);
    private static final int TEXT = Color.rgb(31, 45, 61);
    private static final int MUTED = Color.rgb(93, 109, 126);
    private static final int BORDER = Color.rgb(219, 228, 235);

    private MaternalDbHelper db;
    private LinearLayout root;
    private LinearLayout content;
    private TextView status;
    private TextView headerTitle;
    private String currentUser = "admin";
    private String currentRole = "ADMIN";

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
    private Patient editingPatient;
    private String selectedStateCode;
    private String selectedDistrictCode;
    private String selectedSubdistrictCode;
    private String selectedLocalBodyCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new MaternalDbHelper(this);
        db.ensureCoreData();
        LocationImporter.importBundledLgd(this, db);
        showLogin();
    }

    private void showLogin() {
        content = null;
        headerTitle = null;
        status = null;
        LinearLayout login = new LinearLayout(this);
        login.setOrientation(LinearLayout.VERTICAL);
        login.setGravity(Gravity.CENTER);
        login.setPadding(dp(20), dp(20), dp(20), dp(20));
        login.setBackground(gradient(BG_TOP, BG_BOTTOM, dp(0)));
        setContentView(login);

        LinearLayout card = card();
        card.setPadding(dp(24), dp(24), dp(24), dp(24));
        TextView title = label("Maternal Tracker India", 26, true);
        TextView sub = label("Offline Android version", 13, false);
        sub.setTextColor(MUTED);
        sub.setPadding(0, dp(3), 0, dp(16));
        EditText username = input("admin");
        EditText password = input("admin123");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        card.addView(title);
        card.addView(sub);
        card.addView(row("Username", username));
        card.addView(row("Password", password));
        Button loginBtn = button("Login", v -> {
            String role = db.loginRole(text(username), text(password));
            if (role == null) {
                toast("Invalid username or password");
                return;
            }
            currentUser = text(username);
            currentRole = role;
            buildShell();
            showDashboard();
        });
        card.addView(loginBtn);
        login.addView(card, new LinearLayout.LayoutParams(-1, -2));
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(gradient(BG_TOP, BG_BOTTOM, dp(0)));
        root.setPadding(dp(12), dp(12), dp(12), dp(8));
        setContentView(root);

        root.addView(header());
        root.addView(navBar());
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        status = label("", 12, false);
        status.setTextColor(MUTED);
        status.setPadding(dp(8), dp(6), dp(8), 0);
        root.addView(status);
    }

    private View header() {
        LinearLayout box = card(PRIMARY, 0, PRIMARY);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        headerTitle = label("Dashboard", 21, true);
        headerTitle.setTextColor(Color.WHITE);
        TextView user = label(currentUser + " (" + currentRole + ")", 12, true);
        user.setTextColor(Color.rgb(222, 244, 239));
        user.setGravity(Gravity.END);
        box.addView(headerTitle, new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(user);
        return box;
    }

    private View navBar() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(8));
        row.addView(navButton("Dashboard", v -> showDashboard()));
        row.addView(navButton("Patient Entry", v -> showPatientForm(null)));
        row.addView(navButton("Search", v -> showPatientList(false)));
        row.addView(navButton("Reports", v -> showReports()));
        row.addView(navButton("Backup", v -> showBackup()));
        if (isAdmin()) {
            row.addView(navButton("Administration", v -> showAdmin()));
        }
        row.addView(navButton("Logout", v -> showLogin()));
        scroll.addView(row);
        return scroll;
    }

    private void setPage(String title) {
        content.animate().cancel();
        content.removeAllViews();
        content.setAlpha(0f);
        content.setTranslationY(dp(10));
        headerTitle.setText(title);
        status.setText("");
        content.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(240)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    private void showDashboard() {
        setPage("Dashboard");
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(box);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));

        int total = db.countPatients(null, null);
        int today = db.countPatients("entry_date = ?", new String[]{LocalDate.now().toString()});
        int edd30 = db.countPatients("edd_date BETWEEN ? AND ?", new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(30).toString()});
        int locked = db.countPatients("record_locked = 1", null);
        box.addView(statGrid(
                stat("Total Patients", total, v -> showPatientList(false)),
                stat("Today's Entries", today, v -> showPatientList(false, "entry_date = ?", new String[]{LocalDate.now().toString()})),
                stat("EDD Within 30 Days", edd30, v -> showPatientList(false, "edd_date BETWEEN ? AND ?", new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(30).toString()})),
                stat("Completed / Locked", locked, v -> showPatientList(false, "record_locked = 1", null))
        ));
        box.addView(section("Quick Actions",
                navButton("New Patient", v -> showPatientForm(null)),
                navButton("Search / Edit Patients", v -> showPatientList(false)),
                navButton("Reports", v -> showReports()),
                navButton("Create Backup", v -> createBackupNow())
        ));
        box.addView(section("India Location Data",
                smallText("State, UT, district, sub-district, local body, ward, and village fields are available through the bundled LGD-ready lookup tables."),
                navButton("Refresh Location Data", v -> {
                    LocationImporter.importBundledLgd(this, db);
                    toast("Location data refreshed");
                })
        ));
    }

    private View statGrid(View... stats) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row = null;
        for (int i = 0; i < stats.length; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                wrap.addView(row);
            }
            row.addView(stats[i], new LinearLayout.LayoutParams(0, -2, 1));
        }
        return wrap;
    }

    private View stat(String title, int value, View.OnClickListener click) {
        LinearLayout box = card();
        box.setOnClickListener(click);
        TextView number = label(String.valueOf(value), 26, true);
        number.setTextColor(PRIMARY);
        TextView caption = label(title, 13, true);
        caption.setTextColor(MUTED);
        box.addView(number);
        box.addView(caption);
        return box;
    }

    private void showPatientForm(Patient patient) {
        setPage(patient == null ? "Patient Entry" : "Edit Patient");
        editingPatient = patient;
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

        patientId = readOnlyInput(patient == null ? db.nextPatientId() : patient.patientId);
        patientName = input(value(patient == null ? null : patient.patientName));
        mobile = input(value(patient == null ? null : patient.mobileNumber));
        state = auto(db.listStates());
        district = auto(db.listDistricts(selectedStateCode));
        subdistrict = auto(db.listSubdistricts(selectedDistrictCode));
        localBodyType = auto(list("Panchayat", "Municipality", "Municipal Corporation", "Nagar Panchayat", "Traditional Local Body"));
        localBody = auto(db.listLocalBodies(selectedDistrictCode));
        ward = auto(db.listWards(selectedLocalBodyCode));
        village = auto(db.listVillages(selectedDistrictCode, selectedSubdistrictCode, selectedLocalBodyCode));
        lmpDate = input(patient == null ? LocalDate.now().toString() : value(patient.lmpDate));
        eddDate = input(patient == null ? LocalDate.now().plusDays(280).toString() : value(patient.eddDate));
        motivator = auto(db.listNames("custom_motivators"));
        doctor = auto(db.listNames("custom_doctors"));
        visit1 = readOnlyInput(patient == null ? LocalDate.now().toString() : value(patient.visit1));
        visit2 = input(value(patient == null ? null : patient.visit2));
        visit3 = input(value(patient == null ? null : patient.visit3));
        finalVisit = input(value(patient == null ? null : patient.finalVisit));
        remarks = input(value(patient == null ? null : patient.remarks));

        if (patient != null) {
            state.setText(value(patient.stateName), false);
            selectedStateCode = db.getCodeByName("states", text(state));
            setAdapter(district, db.listDistricts(selectedStateCode));
            district.setText(value(patient.districtName), false);
            selectedDistrictCode = db.getCodeByNameAndParent("districts", text(district), "state_code", selectedStateCode);
            subdistrict.setText(value(patient.subdistrictName), false);
            localBodyType.setText(value(patient.localBodyType), false);
            setAdapter(localBody, db.listLocalBodies(selectedDistrictCode));
            localBody.setText(value(patient.localBodyName), false);
            selectedLocalBodyCode = db.getCodeByNameAndParent("local_bodies", text(localBody), "district_code", selectedDistrictCode);
            setAdapter(ward, db.listWards(selectedLocalBodyCode));
            ward.setText(value(patient.wardName), false);
            setAdapter(village, db.listVillages(selectedDistrictCode, selectedSubdistrictCode, selectedLocalBodyCode));
            village.setText(value(patient.villageName), false);
            motivator.setText(value(patient.motivatorName), false);
            doctor.setText(value(patient.doctorName), false);
            refreshDistrictChildren(false);
        }

        lmpDate.addTextChangedListener(simpleWatcher(s -> updateEdd()));
        state.setOnItemClickListener((parent, view, position, id) -> refreshDistricts(true));
        district.setOnItemClickListener((parent, view, position, id) -> refreshDistrictChildren(true));
        subdistrict.setOnItemClickListener((parent, view, position, id) -> refreshVillageAdapter(true));
        localBody.setOnItemClickListener((parent, view, position, id) -> refreshWardAndVillageAdapters(true));

        boolean lockedForStaff = patient != null && patient.recordLocked && !isAdmin();
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
                row("Village *", village),
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
        actions.addView(button("Clear", v -> showPatientForm(null)));
        Button save = button(patient == null ? "Save Patient" : "Update Patient", v -> savePatient());
        save.setEnabled(!lockedForStaff);
        actions.addView(save);
        form.addView(actions);
        if (lockedForStaff) {
            status.setText("This record is locked after final visit. Admin can unlock it.");
        }
    }

    private void savePatient() {
        Patient p = editingPatient == null ? new Patient() : editingPatient;
        Patient old = editingPatient == null ? null : db.getPatient(editingPatient.id);
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
        p.entryDate = p.entryDate == null ? LocalDate.now().toString() : p.entryDate;
        p.remarks = text(remarks);

        String validation = validatePatient(p);
        if (validation != null) {
            toast(validation);
            return;
        }
        try {
            db.savePatient(p);
            if (old != null) {
                logPatientDiff(old, p);
            }
            db.logActivity(editingPatient == null ? "PATIENT_CREATE" : "PATIENT_UPDATE", p.patientId, currentUser);
            hideKeyboard();
            toast(editingPatient == null ? "Patient saved" : "Patient updated");
            showPatientList(false);
        } catch (Exception ex) {
            toast("Save failed: " + ex.getMessage());
        }
    }

    private String validatePatient(Patient p) {
        if (empty(p.patientName) || empty(p.mobileNumber) || empty(p.stateName) || empty(p.districtName) || empty(p.villageName) || empty(p.motivatorName)) {
            return "Fill patient name, mobile, state, district, village, and motivator";
        }
        String digits = p.mobileNumber.replaceAll("\\D", "");
        if (digits.length() < 10 || digits.length() > 15) {
            return "Mobile number must contain 10 to 15 digits";
        }
        if (!validDate(p.lmpDate) || !validDate(p.eddDate) || !validDate(p.visit1) || !validDate(p.visit2) || !validDate(p.visit3) || !validDate(p.finalVisit)) {
            return "Use date format YYYY-MM-DD";
        }
        try {
            if (!empty(p.lmpDate) && LocalDate.parse(p.lmpDate).isAfter(LocalDate.now())) {
                return "LMP date cannot be in the future";
            }
            if (!empty(p.visit2) && LocalDate.parse(p.visit2).isBefore(LocalDate.parse(p.visit1))) {
                return "2nd visit cannot be before 1st visit";
            }
            if (!empty(p.visit3) && LocalDate.parse(p.visit3).isBefore(LocalDate.parse(empty(p.visit2) ? p.visit1 : p.visit2))) {
                return "3rd visit cannot be before previous visit";
            }
            if (!empty(p.finalVisit)) {
                String previous = !empty(p.visit3) ? p.visit3 : (!empty(p.visit2) ? p.visit2 : p.visit1);
                if (LocalDate.parse(p.finalVisit).isBefore(LocalDate.parse(previous))) {
                    return "Final visit cannot be before previous visit";
                }
            }
        } catch (DateTimeParseException ex) {
            return "Use date format YYYY-MM-DD";
        }
        return null;
    }

    private void logPatientDiff(Patient old, Patient p) {
        db.logChange(p.patientId, "patient_name", old.patientName, p.patientName, currentUser);
        db.logChange(p.patientId, "mobile_number", old.mobileNumber, p.mobileNumber, currentUser);
        db.logChange(p.patientId, "village_name", old.villageName, p.villageName, currentUser);
        db.logChange(p.patientId, "lmp_date", old.lmpDate, p.lmpDate, currentUser);
        db.logChange(p.patientId, "edd_date", old.eddDate, p.eddDate, currentUser);
        db.logChange(p.patientId, "motivator_name", old.motivatorName, p.motivatorName, currentUser);
        db.logChange(p.patientId, "doctor_name", old.doctorName, p.doctorName, currentUser);
        db.logChange(p.patientId, "visit2", old.visit2, p.visit2, currentUser);
        db.logChange(p.patientId, "visit3", old.visit3, p.visit3, currentUser);
        db.logChange(p.patientId, "final_visit", old.finalVisit, p.finalVisit, currentUser);
        db.logChange(p.patientId, "remarks", old.remarks, p.remarks, currentUser);
    }

    private void showPatientList(boolean adminMode) {
        showPatientList(adminMode, null, null);
    }

    private void showPatientList(boolean adminMode, String extraWhere, String[] extraArgs) {
        setPage(adminMode ? "Patient Management" : "Patient Search");
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        content.addView(page, new LinearLayout.LayoutParams(-1, -1));
        EditText search = input("");
        search.setHint("Search name, patient ID, mobile, village, motivator, doctor, district");
        page.addView(search);
        LinearLayout tools = new LinearLayout(this);
        tools.addView(button("Export CSV", v -> exportPatientsCsv(text(search))));
        tools.addView(button("New Patient", v -> showPatientForm(null)));
        page.addView(tools);
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        Runnable reload = () -> renderPatientRows(list, db.listPatients(text(search), extraWhere, extraArgs), adminMode);
        search.addTextChangedListener(simpleWatcher(s -> reload.run()));
        reload.run();
    }

    private void renderPatientRows(LinearLayout list, List<Patient> patients, boolean adminMode) {
        list.removeAllViews();
        list.addView(label(patients.size() + " patient(s)", 13, true));
        for (Patient p : patients) {
            LinearLayout card = card();
            card.addView(label(p.patientId + "  |  " + value(p.patientName), 15, true));
            card.addView(smallText("Mobile: " + value(p.mobileNumber) + "  Village: " + value(p.villageName)));
            card.addView(smallText("District: " + value(p.districtName) + "  Local body: " + value(p.localBodyName)));
            card.addView(smallText("Motivator: " + value(p.motivatorName) + "  Doctor: " + value(p.doctorName)));
            card.addView(smallText("LMP: " + value(p.lmpDate) + "  EDD: " + value(p.eddDate) + "  Final: " + value(p.finalVisit)));
            if (p.recordLocked) {
                TextView locked = chip("Locked after final visit", ACCENT, Color.WHITE);
                card.addView(locked);
            }
            LinearLayout actions = new LinearLayout(this);
            actions.addView(button("Open", v -> showPatientForm(db.getPatient(p.id))));
            if (adminMode && isAdmin()) {
                actions.addView(button("Unlock", v -> {
                    db.unlockPatient(p.id);
                    db.logActivity("PATIENT_UNLOCK", p.patientId, currentUser);
                    showPatientList(true);
                }));
                actions.addView(button("Delete", v -> confirmDeletePatient(p)));
            }
            card.addView(actions);
            list.addView(card);
        }
    }

    private void confirmDeletePatient(Patient p) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Patient")
                .setMessage("Delete " + p.patientName + " (" + p.patientId + ")?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.deletePatient(p.id);
                    db.logActivity("PATIENT_DELETE", p.patientId, currentUser);
                    showPatientList(true);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showReports() {
        setPage("Reports");
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(page);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        int total = db.countPatients(null, null);
        page.addView(section("Patient List",
                smallText("Total records: " + total),
                navButton("Open Patient Search", v -> showPatientList(false)),
                navButton("Export Patient CSV", v -> exportPatientsCsv(""))
        ));
        LinearLayout visits = new LinearLayout(this);
        visits.setOrientation(LinearLayout.VERTICAL);
        for (String[] row : db.visitCompletionRows()) {
            int done = Integer.parseInt(row[1]);
            int pct = total == 0 ? 0 : Math.round(done * 100f / total);
            visits.addView(smallText(row[0] + ": " + done + " / " + row[2] + " (" + pct + "%)"));
        }
        page.addView(section("Visit Completion", visits));
        page.addView(reportMap("Motivator Performance", db.countBy("motivator_name")));
        page.addView(reportMap("Patients by Village", db.countBy("village_name")));
        page.addView(reportMap("Patients by District", db.countBy("district_name")));
        page.addView(reportMap("Monthly Summary", monthlySummary()));
    }

    private Map<String, Integer> monthlySummary() {
        java.util.LinkedHashMap<String, Integer> rows = new java.util.LinkedHashMap<>();
        YearMonth month = YearMonth.now().minusMonths(11);
        for (int i = 0; i < 12; i++) {
            String prefix = month.plusMonths(i).toString();
            rows.put(prefix, db.countPatients("entry_date LIKE ?", new String[]{prefix + "%"}));
        }
        return rows;
    }

    private View reportMap(String title, Map<String, Integer> rows) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        if (rows.isEmpty()) {
            body.addView(smallText("No data"));
        }
        for (Map.Entry<String, Integer> row : rows.entrySet()) {
            body.addView(smallText(row.getKey() + ": " + row.getValue()));
        }
        return section(title, body);
    }

    private void showBackup() {
        setPage("Backup Manager");
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        content.addView(page, new LinearLayout.LayoutParams(-1, -1));
        page.addView(section("Database Backup",
                smallText("Backups are stored in the app external files backup folder."),
                navButton("Create Backup Now", v -> createBackupNow()),
                navButton("Refresh", v -> showBackup())
        ));
        File dir = backupDir();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".db"));
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        if (files == null || files.length == 0) {
            list.addView(smallText("No backups yet."));
        } else {
            java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (File file : files) {
                LinearLayout row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.addView(smallText(file.getName() + "  (" + file.length() + " bytes)"), new LinearLayout.LayoutParams(0, -2, 1));
                row.addView(button("Restore", v -> confirmRestore(file)));
                list.addView(row);
            }
        }
        page.addView(section("Existing Backups", list));
    }

    private void exportPatientsCsv(String filter) {
        try {
            File base = getExternalFilesDir(null);
            if (base == null) {
                base = getFilesDir();
            }
            File dir = new File(base, "exports");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File target = new File(dir, "patients_" + System.currentTimeMillis() + ".csv");
            StringBuilder csv = new StringBuilder();
            csv.append("Serial,Entry Date,Patient Name,Patient ID,State,District,Sub-District,Local Body Type,Local Body,Ward,Village,Mobile,Motivator,Doctor,LMP,EDD,1st Visit,2nd Visit,3rd Visit,Final Visit,Locked,Remarks\n");
            for (Patient p : db.listPatients(filter)) {
                csv.append(p.serialNumber).append(',')
                        .append(csv(p.entryDate)).append(',')
                        .append(csv(p.patientName)).append(',')
                        .append(csv(p.patientId)).append(',')
                        .append(csv(p.stateName)).append(',')
                        .append(csv(p.districtName)).append(',')
                        .append(csv(p.subdistrictName)).append(',')
                        .append(csv(p.localBodyType)).append(',')
                        .append(csv(p.localBodyName)).append(',')
                        .append(csv(p.wardName)).append(',')
                        .append(csv(p.villageName)).append(',')
                        .append(csv(p.mobileNumber)).append(',')
                        .append(csv(p.motivatorName)).append(',')
                        .append(csv(p.doctorName)).append(',')
                        .append(csv(p.lmpDate)).append(',')
                        .append(csv(p.eddDate)).append(',')
                        .append(csv(p.visit1)).append(',')
                        .append(csv(p.visit2)).append(',')
                        .append(csv(p.visit3)).append(',')
                        .append(csv(p.finalVisit)).append(',')
                        .append(p.recordLocked ? "1" : "0").append(',')
                        .append(csv(p.remarks)).append('\n');
            }
            try (FileOutputStream out = new FileOutputStream(target)) {
                out.write(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            status.setText("Export created: " + target.getAbsolutePath());
            toast("CSV exported");
        } catch (Exception ex) {
            toast("Export failed: " + ex.getMessage());
        }
    }

    private void createBackupNow() {
        try {
            File target = new File(backupDir(), "backup_" + LocalDate.now().toString().replace("-", "_") + ".db");
            copyFile(getDatabasePath(MaternalDbHelper.DB_NAME), target);
            status.setText("Backup created: " + target.getAbsolutePath());
            toast("Backup created");
        } catch (Exception ex) {
            toast("Backup failed: " + ex.getMessage());
        }
    }

    private void confirmRestore(File backup) {
        new AlertDialog.Builder(this)
                .setTitle("Restore Backup")
                .setMessage("Restore database from " + backup.getName() + "? A pre-restore backup will be created first.")
                .setPositiveButton("Restore", (dialog, which) -> restoreBackup(backup))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void restoreBackup(File backup) {
        try {
            File pre = new File(backupDir(), "pre_restore_" + System.currentTimeMillis() + ".db");
            copyFile(getDatabasePath(MaternalDbHelper.DB_NAME), pre);
            db.close();
            copyFile(backup, getDatabasePath(MaternalDbHelper.DB_NAME));
            db = new MaternalDbHelper(this);
            db.ensureCoreData();
            toast("Backup restored");
            showDashboard();
        } catch (Exception ex) {
            db = new MaternalDbHelper(this);
            toast("Restore failed: " + ex.getMessage());
        }
    }

    private File backupDir() {
        File base = getExternalFilesDir(null);
        if (base == null) {
            base = getFilesDir();
        }
        File dir = new File(base, "backups");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private void copyFile(File source, File target) throws Exception {
        try (FileInputStream in = new FileInputStream(source); FileOutputStream out = new FileOutputStream(target)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
    }

    private void showAdmin() {
        setPage("Administration");
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(page);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        page.addView(section("Patient Management",
                navButton("Open Patient Management", v -> showPatientList(true))
        ));
        page.addView(section("Users",
                usersView(),
                navButton("Add User", v -> addUserDialog())
        ));
        page.addView(section("Motivator Names",
                namesView("custom_motivators"),
                navButton("Add Motivator", v -> addNameDialog("custom_motivators", "Motivator Name"))
        ));
        page.addView(section("Doctor Names",
                namesView("custom_doctors"),
                navButton("Add Doctor", v -> addNameDialog("custom_doctors", "Doctor Name"))
        ));
        page.addView(section("Change Logs", changeLogView()));
    }

    private View usersView() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        for (String[] user : db.listUsers()) {
            list.addView(smallText(user[1] + "  (" + user[2] + ")"));
        }
        return list;
    }

    private View namesView(String table) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        for (String name : db.listNames(table)) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(smallText(name), new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(button("Remove", v -> {
                db.deleteName(table, name);
                showAdmin();
            }));
            list.addView(row);
        }
        return list;
    }

    private View changeLogView() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        List<String[]> logs = db.changeLogs();
        if (logs.isEmpty()) {
            list.addView(smallText("No changes logged yet."));
        }
        for (String[] row : logs) {
            list.addView(smallText(row[0] + " | " + row[1] + " | " + row[2] + ": " + value(row[3]) + " -> " + value(row[4]) + " | " + row[5]));
        }
        return list;
    }

    private void addNameDialog(String table, String title) {
        EditText input = input("");
        new AlertDialog.Builder(this)
                .setTitle("Add " + title)
                .setView(row(title, input))
                .setPositiveButton("Save", (dialog, which) -> {
                    db.addName(table, text(input));
                    showAdmin();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addUserDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        EditText username = input("");
        EditText password = input("");
        AutoCompleteTextView role = auto(list("STAFF", "ADMIN"));
        role.setText("STAFF", false);
        box.addView(row("Username", username));
        box.addView(row("Password", password));
        box.addView(row("Role", role));
        new AlertDialog.Builder(this)
                .setTitle("Add User")
                .setView(box)
                .setPositiveButton("Save", (dialog, which) -> {
                    if (empty(text(username)) || empty(text(password))) {
                        toast("Username and password are required");
                        return;
                    }
                    try {
                        db.saveUser(text(username), text(password), text(role));
                        db.logActivity("USER_CREATE", text(username), currentUser);
                        showAdmin();
                    } catch (Exception ex) {
                        toast("Could not add user: " + ex.getMessage());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshDistricts(boolean clear) {
        selectedStateCode = db.getCodeByName("states", text(state));
        selectedDistrictCode = null;
        selectedSubdistrictCode = null;
        selectedLocalBodyCode = null;
        setAdapter(district, db.listDistricts(selectedStateCode));
        if (clear) {
            district.setText("", false);
        }
        refreshDistrictChildren(clear);
    }

    private void refreshDistrictChildren(boolean clear) {
        selectedDistrictCode = db.getCodeByNameAndParent("districts", text(district), "state_code", selectedStateCode);
        selectedSubdistrictCode = null;
        selectedLocalBodyCode = null;
        setAdapter(subdistrict, db.listSubdistricts(selectedDistrictCode));
        setAdapter(localBody, db.listLocalBodies(selectedDistrictCode));
        if (clear) {
            subdistrict.setText("", false);
            localBody.setText("", false);
        }
        refreshWardAndVillageAdapters(clear);
    }

    private void refreshWardAndVillageAdapters(boolean clear) {
        selectedLocalBodyCode = db.getCodeByNameAndParent("local_bodies", text(localBody), "district_code", selectedDistrictCode);
        setAdapter(ward, db.listWards(selectedLocalBodyCode));
        if (clear) {
            ward.setText("", false);
        }
        refreshVillageAdapter(clear);
    }

    private void refreshVillageAdapter(boolean clear) {
        selectedSubdistrictCode = db.getCodeByNameAndParent("subdistricts", text(subdistrict), "district_code", selectedDistrictCode);
        setAdapter(village, db.listVillages(selectedDistrictCode, selectedSubdistrictCode, selectedLocalBodyCode));
        if (clear) {
            village.setText("", false);
        }
    }

    private void updateEdd() {
        try {
            LocalDate lmp = LocalDate.parse(text(lmpDate));
            eddDate.setText(lmp.plusDays(280).toString());
        } catch (Exception ignored) {
        }
    }

    private LinearLayout section(String title, View... rows) {
        LinearLayout box = card();
        TextView heading = label(title, 17, true);
        heading.setTextColor(PRIMARY);
        heading.setPadding(0, 0, 0, dp(6));
        box.addView(heading);
        for (View row : rows) {
            box.addView(row);
        }
        return box;
    }

    private LinearLayout row(String label, View input) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(5), 0, dp(5));
        TextView tv = label(label, 12, true);
        tv.setTextColor(MUTED);
        row.addView(tv);
        row.addView(input, new LinearLayout.LayoutParams(-1, dp(46)));
        return row;
    }

    private LinearLayout card() {
        return card(SURFACE, 1, BORDER);
    }

    private LinearLayout card(int color, int strokeWidth, int strokeColor) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(rounded(color, dp(12), strokeWidth == 0 ? 0 : dp(strokeWidth), strokeColor));
        box.setPadding(dp(15), dp(15), dp(15), dp(15));
        box.setElevation(dp(2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(12));
        box.setLayoutParams(lp);
        return box;
    }

    private TextView label(String text, int sp, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(sp);
        tv.setTextColor(TEXT);
        if (bold) {
            tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        }
        return tv;
    }

    private TextView smallText(String text) {
        TextView tv = label(text, 13, false);
        tv.setPadding(0, dp(3), 0, dp(3));
        return tv;
    }

    private EditText input(String value) {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setText(value == null ? "" : value);
        edit.setTextSize(15);
        edit.setTextColor(TEXT);
        edit.setHintTextColor(Color.rgb(135, 151, 166));
        edit.setBackground(rounded(SURFACE_ALT, dp(10), dp(1), BORDER));
        edit.setPadding(dp(12), 0, dp(12), 0);
        return edit;
    }

    private EditText readOnlyInput(String value) {
        EditText edit = input(value);
        edit.setEnabled(false);
        edit.setTextColor(MUTED);
        edit.setBackground(rounded(Color.rgb(235, 241, 246), dp(10), dp(1), BORDER));
        return edit;
    }

    private AutoCompleteTextView auto(List<String> values) {
        AutoCompleteTextView view = new AutoCompleteTextView(this);
        view.setSingleLine(true);
        view.setThreshold(1);
        view.setTextSize(15);
        view.setTextColor(TEXT);
        view.setHintTextColor(Color.rgb(135, 151, 166));
        view.setBackground(rounded(SURFACE_ALT, dp(10), dp(1), BORDER));
        view.setPadding(dp(12), 0, dp(12), 0);
        setAdapter(view, values);
        return view;
    }

    private void setAdapter(AutoCompleteTextView view, List<String> values) {
        view.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, values));
    }

    private Button navButton(String text, View.OnClickListener listener) {
        Button b = button(text, listener);
        b.setAllCaps(false);
        b.setTextColor(PRIMARY_DARK);
        b.setBackground(rounded(Color.rgb(232, 246, 242), dp(22), dp(1), Color.rgb(190, 224, 215)));
        b.setElevation(dp(1));
        return b;
    }

    private Button button(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setTypeface(b.getTypeface(), Typeface.BOLD);
        b.setAllCaps(false);
        b.setBackground(gradient(PRIMARY, PRIMARY_DARK, dp(22)));
        b.setElevation(dp(2));
        b.setOnClickListener(listener);
        b.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                view.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            }
            return false;
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(44));
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        b.setLayoutParams(lp);
        return b;
    }

    private TextView chip(String text, int bg, int fg) {
        TextView chip = label(text, 12, true);
        chip.setTextColor(fg);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(5), dp(10), dp(5));
        chip.setBackground(rounded(bg, dp(16), 0, bg));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(0, dp(6), 0, dp(2));
        chip.setLayoutParams(lp);
        return chip;
    }

    private GradientDrawable rounded(int color, int radius, int strokeWidth, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private GradientDrawable gradient(int startColor, int endColor, int radius) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{startColor, endColor}
        );
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private TextWatcher simpleWatcher(TextCallback callback) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { callback.onText(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        };
    }

    private List<String> list(String... values) {
        return java.util.Arrays.asList(values);
    }

    private String text(TextView view) {
        String value = view.getText() == null ? "" : view.getText().toString().trim();
        return value.isEmpty() ? null : value;
    }

    private String value(String value) {
        return value == null || value.trim().isEmpty() ? "" : value;
    }

    private String csv(String value) {
        String safe = value(value).replace("\"", "\"\"");
        return "\"" + safe + "\"";
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

    private boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(currentRole);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        if (status != null) {
            status.setText(text);
        }
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
