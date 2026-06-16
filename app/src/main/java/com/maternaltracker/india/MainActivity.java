package com.maternaltracker.india;

import android.app.Activity;
import android.app.AlertDialog;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
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
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressLint("SetTextI18n")
public class MainActivity extends Activity {
    private static final int BG_TOP = Color.rgb(198, 225, 241);
    private static final int BG_BOTTOM = Color.rgb(232, 246, 251);
    private static final int SURFACE = Color.argb(178, 255, 255, 255);
    private static final int SURFACE_ALT = Color.argb(132, 255, 255, 255);
    private static final int SURFACE_WARM = Color.rgb(255, 252, 247);
    private static final int PRIMARY = Color.rgb(22, 91, 145);
    private static final int PRIMARY_DARK = Color.rgb(9, 50, 91);
    private static final int PRIMARY_SOFT = Color.rgb(226, 240, 250);
    private static final int ACCENT = Color.rgb(0, 137, 123);
    private static final int WARNING = Color.rgb(180, 103, 22);
    private static final int TEXT = Color.rgb(24, 37, 54);
    private static final int MUTED = Color.rgb(92, 110, 128);
    private static final int BORDER = Color.argb(210, 255, 255, 255);
    private static final String HOSPITAL_NAME = "BLUE BIRD A GENERAL HOSPITAL";
    private static final String APP_NAME = "Maternal Care Registry";
    private static final String DEFAULT_STATE = "West Bengal";
    private static final String DEFAULT_DISTRICT = "MURSHIDABAD";
    private static final int REQ_EXPORT_EXCEL = 501;
    private static final int REQ_EXPORT_PDF = 502;
    private static final int REQ_EXPORT_BACKUP = 503;

    private MaternalDbHelper db;
    private FirebaseGateway firebase;
    private LinearLayout root;
    private LinearLayout content;
    private LinearLayout bottomNav;
    private LinearLayout profileMenu;
    private TextView status;
    private TextView headerTitle;
    private TextView syncBadge;
    private TextView backButton;
    private String currentUser = "";
    private String currentRole = "";
    private String currentPage = "Dashboard";
    private boolean dashboardDetailed = false;

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
    private Button savePatientButton;
    private Patient editingPatient;
    private String selectedStateCode;
    private String selectedDistrictCode;
    private String selectedSubdistrictCode;
    private String selectedLocalBodyCode;
    private String pendingExportFilter = "";
    private String pendingExportWhere = null;
    private String[] pendingExportArgs = null;
    private long pendingExportPatientId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(PRIMARY_DARK);
        getWindow().setNavigationBarColor(BG_BOTTOM);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );
        db = new MaternalDbHelper(this);
        firebase = new FirebaseGateway(this);
        db.ensureCoreData();
        dashboardDetailed = getPreferences(MODE_PRIVATE).getBoolean("dashboard_detailed", false);
        LocationImporter.importBundledLgd(this, db);
        restoreOrShowLogin();
    }

    @Override
    protected void onDestroy() {
        if (firebase != null) {
            firebase.stopPatientListener();
        }
        super.onDestroy();
    }

    private void showLogin() {
        content = null;
        headerTitle = null;
        status = null;
        LinearLayout login = new LinearLayout(this);
        login.setOrientation(LinearLayout.VERTICAL);
        login.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        login.setPadding(dp(18), topSafeInset() + dp(18), dp(18), bottomSafeInset() + dp(18));
        login.setBackground(gradient(BG_TOP, BG_BOTTOM, dp(0)));
        setContentView(login);

        LinearLayout card = card();
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(rounded(Color.WHITE, dp(8), dp(1), Color.rgb(194, 213, 228)));
        TextView mark = brandMark();
        TextView title = label(HOSPITAL_NAME, 18, true);
        title.setGravity(Gravity.CENTER);
        TextView sub = label("Secure online maternal registry", 13, true);
        sub.setTextColor(MUTED);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(2), 0, dp(4));
        TextView note = label("Firebase account required", 12, false);
        note.setTextColor(MUTED);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, 0, 0, dp(10));
        EditText username = input("");
        EditText password = input("");
        username.setHint("Email");
        password.setHint("Password");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        card.addView(mark);
        card.addView(title);
        card.addView(sub);
        card.addView(note);
        card.addView(row("Email", username));
        card.addView(row("Password", password));
        final Button[] loginHolder = new Button[1];
        Button loginBtn = button("Sign in securely", v -> {
            String email = text(username);
            String pass = text(password);
            if (empty(email) || empty(pass)) {
                toast("Email and password are required");
                return;
            }
            loginHolder[0].setEnabled(false);
            firebase.signIn(email, pass, (session, error) -> runOnUiThread(() -> {
                loginHolder[0].setEnabled(true);
                if (error != null) {
                    toast("Login failed: " + error.getMessage());
                    return;
                }
                currentUser = session.email;
                currentRole = session.role;
                buildShell();
                startOnlineSync();
                showDashboard();
            }));
        });
        loginHolder[0] = loginBtn;
        card.addView(loginBtn);
        TextView footer = label("Internet connection required for live sync", 11, false);
        footer.setTextColor(MUTED);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(12), 0, 0);
        card.addView(footer);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.setMargins(0, dp(12), 0, 0);
        login.addView(card, cardLp);
    }

    private void restoreOrShowLogin() {
        showStartup();
        firebase.restoreSession((session, error) -> runOnUiThread(() -> {
            if (session != null && error == null) {
                currentUser = session.email;
                currentRole = session.role;
                buildShell();
                startOnlineSync();
                showDashboard();
                return;
            }
            firebase.signOut();
            showLogin();
        }));
    }

    private void showStartup() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER);
        screen.setPadding(dp(18), topSafeInset() + dp(18), dp(18), bottomSafeInset() + dp(18));
        screen.setBackground(gradient(BG_TOP, BG_BOTTOM, dp(0)));
        TextView mark = brandMark();
        TextView title = label(HOSPITAL_NAME, 18, true);
        title.setGravity(Gravity.CENTER);
        TextView text = label("Checking secure session...", 13, false);
        text.setTextColor(MUTED);
        text.setGravity(Gravity.CENTER);
        screen.addView(mark);
        screen.addView(title);
        screen.addView(text);
        setContentView(screen);
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(gradient(BG_TOP, BG_BOTTOM, dp(0)));
        root.setPadding(dp(10), topSafeInset() + dp(8), dp(10), bottomSafeInset() + dp(8));
        setContentView(root);

        root.addView(header());
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        profileMenu = profileMenu();
        body.addView(profileMenu, new LinearLayout.LayoutParams(dp(214), -1));
        profileMenu.setVisibility(View.GONE);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, new LinearLayout.LayoutParams(0, -1, 1));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        status = label("", 12, false);
        status.setTextColor(MUTED);
        status.setPadding(dp(8), dp(4), dp(8), dp(4));
        root.addView(status);
        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setBackground(glassPanel(dp(14)));
        bottomNav.setElevation(dp(4));
        bottomNav.setPadding(dp(3), dp(3), dp(3), dp(3));
        root.addView(bottomNav, new LinearLayout.LayoutParams(-1, dp(50)));
        rebuildBottomNav();
    }

    private void startOnlineSync() {
        firebase.startPatientListener(new FirebaseGateway.PatientsListener() {
            @Override
            public void onPatients(List<Patient> patients) {
                runOnUiThread(() -> {
                    db.replacePatientsFromCloud(patients);
                    if (syncBadge != null) {
                        syncBadge.setText("ONLINE");
                        syncBadge.setBackground(rounded(ACCENT, dp(12), 0, ACCENT));
                    }
                    if ("Dashboard".equals(currentPage)) {
                        showDashboard();
                    }
                    if (status != null) {
                        status.setText("Online sync active | " + patients.size() + " patient(s) cached");
                    }
                });
            }

            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    if (syncBadge != null) {
                        syncBadge.setText("SYNC ERROR");
                        syncBadge.setBackground(rounded(WARNING, dp(12), 0, WARNING));
                    }
                    if ("Dashboard".equals(currentPage)) {
                        showDashboard();
                    }
                    if (status != null) {
                        status.setText("Sync error: " + error.getMessage());
                    }
                    toast("Sync error: " + error.getMessage());
                });
            }
        });
    }

    private View header() {
        LinearLayout box = card(PRIMARY_DARK, 0, PRIMARY_DARK);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(gradient(PRIMARY_DARK, PRIMARY, dp(8)));
        box.setPadding(dp(10), dp(8), dp(10), dp(8));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView mark = label("BBH", 13, true);
        mark.setTextColor(PRIMARY_DARK);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(rounded(Color.WHITE, dp(18), 0, Color.WHITE));
        mark.setOnClickListener(v -> toggleProfileMenu());
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(dp(34), dp(34));
        markLp.setMargins(0, 0, dp(9), 0);
        top.addView(mark, markLp);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView hospital = label(HOSPITAL_NAME, 14, true);
        hospital.setTextColor(Color.WHITE);
        hospital.setSingleLine(true);
        headerTitle = label("Dashboard", 12, true);
        headerTitle.setTextColor(Color.WHITE);
        headerTitle.setAlpha(0.82f);
        titles.addView(hospital);
        titles.addView(headerTitle);

        top.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        backButton = chip("Back", Color.argb(45, 255, 255, 255), Color.WHITE);
        backButton.setVisibility(View.GONE);
        backButton.setOnClickListener(v -> goBackInApp());
        top.addView(backButton);

        TextView logout = chip("Exit", Color.argb(45, 255, 255, 255), Color.WHITE);
        logout.setOnClickListener(v -> {
            firebase.signOut();
            currentUser = "";
            currentRole = "";
            showLogin();
        });
        top.addView(logout);
        box.addView(top);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        meta.setPadding(0, dp(4), 0, 0);
        meta.addView(new TextView(this), new LinearLayout.LayoutParams(0, 1, 1));
        meta.addView(chip(currentRole, Color.argb(45, 255, 255, 255), Color.WHITE));
        syncBadge = chip("SYNCING", WARNING, Color.WHITE);
        meta.addView(syncBadge);
        box.addView(meta);
        return box;
    }

    private void rebuildBottomNav() {
        if (bottomNav == null) {
            return;
        }
        bottomNav.removeAllViews();
        bottomNav.addView(bottomNavItem("Dashboard", "⌂", "Home", v -> showDashboard()), new LinearLayout.LayoutParams(0, -1, 1));
        bottomNav.addView(bottomNavItem("Patient Entry", "+", "Entry", v -> showPatientForm(null)), new LinearLayout.LayoutParams(0, -1, 1));
        bottomNav.addView(bottomNavItem("Patient Search", "⌕", "Search", v -> showPatientList(false)), new LinearLayout.LayoutParams(0, -1, 1));
        bottomNav.addView(bottomNavItem("Reports", "▤", "Reports", v -> showReports()), new LinearLayout.LayoutParams(0, -1, 1));
        if (isAdmin()) {
            bottomNav.addView(bottomNavItem("Administration", "⚙", "Admin", v -> showAdmin()), new LinearLayout.LayoutParams(0, -1, 1));
        }
    }

    private LinearLayout profileMenu() {
        LinearLayout menu = card();
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(10), dp(10), dp(10), dp(10));
        TextView avatar = brandMark();
        TextView title = label("Blue Bird Profile", 15, true);
        title.setTextColor(PRIMARY_DARK);
        TextView user = smallText(value(currentUser));
        user.setTextColor(MUTED);
        TextView role = chip(value(currentRole), PRIMARY, Color.WHITE);
        TextView sync = chip(syncBadge == null ? "SYNCING" : value(syncBadge.getText().toString()), ACCENT, Color.WHITE);
        menu.addView(avatar);
        menu.addView(title);
        menu.addView(user);
        LinearLayout badges = new LinearLayout(this);
        badges.setOrientation(LinearLayout.HORIZONTAL);
        badges.addView(role);
        badges.addView(sync);
        menu.addView(badges);
        menu.addView(menuItem("+", "New Patient", v -> showPatientForm(null)));
        menu.addView(menuItem("⌕", "Search Patients", v -> showPatientList(false)));
        menu.addView(menuItem("▤", "Reports", v -> showReports()));
        menu.addView(menuItem("⇩", "Export Center", v -> showExportCenter()));
        if (isAdmin()) {
            menu.addView(menuItem("⚙", "Administration", v -> showAdmin()));
            menu.addView(menuItem("☁", "Backup Manager", v -> showBackup()));
        }
        menu.addView(menuItem("⎋", "Exit", v -> {
            firebase.signOut();
            currentUser = "";
            currentRole = "";
            showLogin();
        }));
        return menu;
    }

    private View menuItem(String symbol, String text, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(8), dp(6), dp(8), dp(6));
        item.setBackground(glassInput(false));
        TextView icon = label(symbol, 13, true);
        icon.setTextColor(PRIMARY);
        icon.setGravity(Gravity.CENTER);
        TextView caption = label(text, 12, true);
        caption.setTextColor(PRIMARY_DARK);
        caption.setPadding(dp(8), 0, 0, 0);
        item.addView(icon, new LinearLayout.LayoutParams(dp(28), dp(30)));
        item.addView(caption, new LinearLayout.LayoutParams(0, -2, 1));
        item.setOnClickListener(v -> {
            closeProfileMenu();
            listener.onClick(v);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(5), 0, 0);
        item.setLayoutParams(lp);
        return item;
    }

    private void toggleProfileMenu() {
        if (profileMenu == null) {
            return;
        }
        boolean open = profileMenu.getVisibility() != View.VISIBLE;
        profileMenu.setVisibility(open ? View.VISIBLE : View.GONE);
        if (open) {
            profileMenu.setTranslationX(-dp(36));
            profileMenu.setAlpha(0f);
            profileMenu.animate().translationX(0f).alpha(1f).setDuration(180).start();
        }
    }

    private void closeProfileMenu() {
        if (profileMenu != null && profileMenu.getVisibility() == View.VISIBLE) {
            profileMenu.setVisibility(View.GONE);
        }
    }

    private void setPage(String title) {
        currentPage = title;
        closeProfileMenu();
        content.animate().cancel();
        content.removeAllViews();
        content.setAlpha(0f);
        content.setTranslationY(dp(16));
        content.setScaleX(0.98f);
        content.setScaleY(0.98f);
        headerTitle.setText(title);
        status.setText("");
        if (backButton != null) {
            backButton.setVisibility("Dashboard".equals(title) ? View.GONE : View.VISIBLE);
        }
        rebuildBottomNav();
        content.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(240)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    @Override
    public void onBackPressed() {
        if (profileMenu != null && profileMenu.getVisibility() == View.VISIBLE) {
            closeProfileMenu();
            return;
        }
        if (content != null && !"Dashboard".equals(currentPage)) {
            goBackInApp();
            return;
        }
        super.onBackPressed();
    }

    private void goBackInApp() {
        if (profileMenu != null && profileMenu.getVisibility() == View.VISIBLE) {
            closeProfileMenu();
            return;
        }
        if (content == null || "Dashboard".equals(currentPage)) {
            return;
        }
        showDashboard();
    }

    private void showDashboard() {
        setPage("Dashboard");
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(box);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));

        String scopeWhere = dashboardScopeWhere();
        String[] scopeArgs = dashboardScopeArgs();
        int total = db.countPatients(scopeWhere, scopeArgs);
        int today = db.countPatients(appendWhere(scopeWhere, "entry_date = ?"), appendArgs(scopeArgs, LocalDate.now().toString()));
        int dueWeek = db.countPatients(appendWhere(scopeWhere, "edd_date BETWEEN ? AND ?"), appendArgs(scopeArgs, LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()));
        int edd30 = db.countPatients(appendWhere(scopeWhere, "edd_date BETWEEN ? AND ?"), appendArgs(scopeArgs, LocalDate.now().toString(), LocalDate.now().plusDays(30).toString()));
        int locked = db.countPatients(appendWhere(scopeWhere, "record_locked = 1"), scopeArgs);
        int pending = Math.max(0, total - locked);
        int needsCompletion = db.countPatients(appendWhere(scopeWhere, "record_locked = 0 AND (visit2 IS NULL OR visit2 = '' OR visit3 IS NULL OR visit3 = '' OR final_visit IS NULL OR final_visit = '')"), scopeArgs);
        box.addView(dashboardStatusStrip(total));
        box.addView(dashboardDensityToggle());
        box.addView(section("Today's Priority Queue", priorityQueue(today, dueWeek, pending, needsCompletion)));
        box.addView(compactKpiRow(
                compactKpi("Total", total, "Patients", v -> showScopedPatientList(null, null)),
                compactKpi("Due Week", dueWeek, "EDD within 7 days", v -> showScopedPatientList("edd_date BETWEEN ? AND ?", new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()})),
                compactKpi("Pending", pending, "Follow-up", v -> showScopedPatientList("record_locked = 0", null)),
                compactKpi("Done", locked, "Completed", v -> showScopedPatientList("record_locked = 1", null))
        ));
        box.addView(section("Needs Attention", needsAttentionView(scopeWhere, scopeArgs)));
        box.addView(section("Upcoming EDD", upcomingEddView(scopeWhere, scopeArgs)));
        if (dashboardDetailed) {
            box.addView(section("30 Day Overview",
                    progressRow("EDD within 30 days", edd30, Math.max(total, 1), total == 0 ? 0 : Math.round(edd30 * 100f / total)),
                    progressRow("Follow-up pending", pending, Math.max(total, 1), total == 0 ? 0 : Math.round(pending * 100f / total)),
                    progressRow("Completed", locked, Math.max(total, 1), total == 0 ? 0 : Math.round(locked * 100f / total))
            ));
            box.addView(syncOverview(total));
            box.addView(section("Recent Patients", recentPatientsView(scopeWhere, scopeArgs)));
        }
    }

    private View dashboardStatusStrip(int total) {
        LinearLayout box = card();
        box.setPadding(dp(10), dp(8), dp(10), dp(8));
        TextView title = label("Blue Bird Maternal Follow-up", 14, true);
        title.setTextColor(PRIMARY_DARK);
        TextView context = label(isAdmin() ? "Admin view: all Blue Bird records" : "Staff view: records created by you", 11, false);
        context.setTextColor(MUTED);
        context.setPadding(0, dp(1), 0, dp(3));
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.addView(chip(DEFAULT_DISTRICT, Color.WHITE, PRIMARY_DARK));
        chips.addView(chip(DEFAULT_STATE, Color.WHITE, PRIMARY_DARK));
        chips.addView(chip(total + " records", ACCENT, Color.WHITE));
        box.addView(title);
        box.addView(context);
        box.addView(chips);
        return box;
    }

    private View dashboardDensityToggle() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, dp(5));
        Button compact = dashboardToggleButton("Compact", !dashboardDetailed);
        compact.setOnClickListener(v -> setDashboardDetailed(false));
        Button detailed = dashboardToggleButton("Detailed", dashboardDetailed);
        detailed.setOnClickListener(v -> setDashboardDetailed(true));
        row.addView(compact, new LinearLayout.LayoutParams(0, dp(38), 1));
        row.addView(detailed, new LinearLayout.LayoutParams(0, dp(38), 1));
        return row;
    }

    private Button dashboardToggleButton(String text, boolean active) {
        Button b = button(text, v -> {
        });
        b.setTextColor(active ? Color.WHITE : PRIMARY_DARK);
        b.setBackground(active ? gradient(PRIMARY, ACCENT, dp(16)) : rounded(Color.argb(120, 255, 255, 255), dp(16), dp(1), BORDER));
        b.setMinHeight(dp(38));
        b.setMinimumHeight(dp(38));
        return b;
    }

    private void setDashboardDetailed(boolean detailed) {
        if (dashboardDetailed == detailed) {
            return;
        }
        dashboardDetailed = detailed;
        getPreferences(MODE_PRIVATE).edit().putBoolean("dashboard_detailed", dashboardDetailed).apply();
        showDashboard();
    }

    private View priorityQueue(int today, int dueWeek, int pending, int needsCompletion) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.addView(priorityItem("EDD due this week", dueWeek, "Review patients with delivery dates in the next 7 days", "edd_date BETWEEN ? AND ?", new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()}));
        list.addView(priorityItem("Follow-up pending", pending, "Open records that still need visit tracking", "record_locked = 0", null));
        list.addView(priorityItem("Records needing completion", needsCompletion, "Open records missing later visit dates", "record_locked = 0 AND (visit2 IS NULL OR visit2 = '' OR visit3 IS NULL OR visit3 = '' OR final_visit IS NULL OR final_visit = '')", null));
        list.addView(priorityItem("New entries today", today, "Patients registered today", "entry_date = ?", new String[]{LocalDate.now().toString()}));
        return list;
    }

    private View priorityItem(String title, int count, String detail, String where, String[] args) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), dp(5), dp(2), dp(5));
        row.setOnClickListener(v -> showScopedPatientList(where, args));
        TextView countView = label(String.valueOf(count), 18, true);
        countView.setGravity(Gravity.CENTER);
        countView.setTextColor(Color.WHITE);
        countView.setBackground(gradient(count > 0 ? WARNING : ACCENT, PRIMARY, dp(18)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(10), 0, 0, 0);
        TextView titleView = label(title, 13, true);
        titleView.setTextColor(PRIMARY_DARK);
        TextView detailView = smallText(detail);
        detailView.setTextColor(MUTED);
        copy.addView(titleView);
        copy.addView(detailView);
        row.addView(countView, new LinearLayout.LayoutParams(dp(36), dp(36)));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private View needsAttentionView(String scopeWhere, String[] scopeArgs) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int noSecondVisit = db.countPatients(appendWhere(scopeWhere, "record_locked = 0 AND (visit2 IS NULL OR visit2 = '')"), scopeArgs);
        int edd7 = db.countPatients(appendWhere(scopeWhere, "edd_date BETWEEN ? AND ?"), appendArgs(scopeArgs, LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()));
        int missingDoctor = db.countPatients(appendWhere(scopeWhere, "doctor_name IS NULL OR doctor_name = ''"), scopeArgs);
        int invalidMobile = db.countPatients(appendWhere(scopeWhere, "mobile_number IS NULL OR length(trim(mobile_number)) != 10"), scopeArgs);
        int totalAttention = noSecondVisit + edd7 + missingDoctor + invalidMobile;
        if (totalAttention == 0) {
            list.addView(emptyState("No attention flags", "Current dashboard records are complete for the tracked checks."));
            return list;
        }
        list.addView(attentionItem("No 2nd visit recorded", noSecondVisit, "record_locked = 0 AND (visit2 IS NULL OR visit2 = '')", null));
        list.addView(attentionItem("EDD within 7 days", edd7, "edd_date BETWEEN ? AND ?", new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()}));
        list.addView(attentionItem("Missing doctor", missingDoctor, "doctor_name IS NULL OR doctor_name = ''", null));
        list.addView(attentionItem("Mobile needs review", invalidMobile, "mobile_number IS NULL OR length(trim(mobile_number)) != 10", null));
        return list;
    }

    private View attentionItem(String title, int count, String where, String[] args) {
        TextView item = smallText(title + ": " + count);
        item.setTextColor(count > 0 ? WARNING : MUTED);
        item.setTypeface(item.getTypeface(), count > 0 ? Typeface.BOLD : Typeface.NORMAL);
        item.setOnClickListener(v -> showScopedPatientList(where, args));
        return item;
    }

    private View upcomingEddView(String where, String[] args) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        List<String[]> rows = db.upcomingEddRows(where, args, dashboardDetailed ? 6 : 3);
        if (rows.isEmpty()) {
            list.addView(emptyState("No upcoming EDD", "Upcoming delivery dates will appear here after patient records are saved."));
            return list;
        }
        for (String[] row : rows) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(0, dp(4), 0, dp(4));
            TextView date = chip(value(row[2]), PRIMARY_SOFT, PRIMARY_DARK);
            TextView detail = smallText(value(row[1]) + " | " + value(row[4]) + " | " + value(row[3]));
            detail.setTextColor(TEXT);
            item.addView(date);
            item.addView(detail, new LinearLayout.LayoutParams(0, -2, 1));
            list.addView(item);
        }
        return list;
    }

    private View recentPatientsView(String where, String[] args) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        List<Patient> patients = db.listPatients("", where, args);
        if (patients.isEmpty()) {
            list.addView(emptyActionState("No patient records yet", "Create the first synced patient record for Blue Bird Hospital.", "Add Patient", v -> showPatientForm(null)));
            return list;
        }
        int limit = Math.min(4, patients.size());
        for (int i = 0; i < limit; i++) {
            Patient p = patients.get(i);
            TextView item = smallText(value(p.patientId) + " | " + value(p.patientName) + " | " + value(p.villageName) + " | EDD " + value(p.eddDate));
            item.setTextColor(TEXT);
            item.setOnClickListener(v -> showPatientDetail(db.getPatient(p.id), false));
            list.addView(item);
        }
        list.addView(navButton("Open Search", v -> showPatientList(false)));
        return list;
    }

    private View compactKpiRow(View... stats) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(4));
        for (View stat : stats) {
            row.addView(stat);
        }
        scroll.addView(row);
        return scroll;
    }

    private View compactKpi(String title, int value, String caption, View.OnClickListener click) {
        LinearLayout box = card();
        box.setOnClickListener(click);
        box.setPadding(dp(9), dp(7), dp(9), dp(7));
        TextView count = label(String.valueOf(value), 22, true);
        count.setTextColor(PRIMARY);
        TextView titleView = label(title, 11, true);
        titleView.setTextColor(PRIMARY_DARK);
        TextView captionView = label(caption, 9, false);
        captionView.setTextColor(MUTED);
        box.addView(count);
        box.addView(titleView);
        box.addView(captionView);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(132), -2);
        lp.setMargins(0, 0, dp(7), dp(7));
        box.setLayoutParams(lp);
        return box;
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
        box.setBackground(glassPanel(dp(14)));
        box.setPadding(dp(10), dp(9), dp(10), dp(9));
        TextView number = label(String.valueOf(value), 23, true);
        number.setTextColor(PRIMARY);
        TextView caption = label(title, 11, true);
        caption.setTextColor(MUTED);
        box.addView(number);
        box.addView(caption);
        return box;
    }

    private View workTile(String title, int value, String captionText, View.OnClickListener click) {
        LinearLayout box = card();
        box.setOnClickListener(click);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        TextView count = label(String.valueOf(value), 28, true);
        count.setTextColor(ACCENT);
        TextView titleView = label(title, 13, true);
        titleView.setTextColor(PRIMARY_DARK);
        TextView caption = smallText(captionText);
        caption.setTextColor(MUTED);
        box.addView(count);
        box.addView(titleView);
        box.addView(caption);
        return box;
    }

    private View syncOverview(int total) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        String syncText = syncBadge == null ? "SYNCING" : value(syncBadge.getText().toString());
        if (!"ONLINE".equals(syncText)) {
            body.addView(skeletonLine(80));
            body.addView(skeletonLine(55));
        }
        body.addView(progressRow("Cloud sync", "ONLINE".equals(syncText) ? 1 : 0, 1, "ONLINE".equals(syncText) ? 100 : 35));
        body.addView(smallText("Cached records available on this device: " + total));
        return section("Sync Status", body);
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
        patientName.setHint("Patient full name");
        mobile.setHint("10 digit mobile number");
        state = auto(db.listStates());
        state.setText(DEFAULT_STATE, false);
        state.setEnabled(false);
        selectedStateCode = db.getCodeByName("states", DEFAULT_STATE);
        district = auto(list(DEFAULT_DISTRICT));
        district.setText(DEFAULT_DISTRICT, false);
        district.setEnabled(false);
        selectedDistrictCode = db.getCodeByNameAndParent("districts", DEFAULT_DISTRICT, "state_code", selectedStateCode);
        subdistrict = auto(murshidabadBlocks());
        localBodyType = auto(list("Block"));
        localBodyType.setText("Block", false);
        localBody = auto(murshidabadBlocks());
        localBody.setHint("Select block");
        localBody.setThreshold(0);
        localBody.setOnClickListener(v -> localBody.showDropDown());
        localBody.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                localBody.showDropDown();
            }
        });
        ward = auto(db.listWards(selectedLocalBodyCode));
        village = auto(list());
        village.setHint("Type village name");
        village.setThreshold(Integer.MAX_VALUE);
        lmpDate = input(patient == null ? LocalDate.now().toString() : value(patient.lmpDate));
        eddDate = input(patient == null ? LocalDate.now().plusDays(280).toString() : value(patient.eddDate));
        motivator = auto(db.listNames("custom_motivators"));
        doctor = auto(db.listNames("custom_doctors"));
        lmpDate.setHint("YYYY-MM-DD");
        eddDate.setHint("YYYY-MM-DD");
        motivator.setHint("Optional");
        doctor.setHint("Doctor name");
        visit1 = readOnlyInput(patient == null ? LocalDate.now().toString() : value(patient.visit1));
        visit2 = input(value(patient == null ? null : patient.visit2));
        visit3 = input(value(patient == null ? null : patient.visit3));
        finalVisit = input(value(patient == null ? null : patient.finalVisit));
        visit2.setHint("YYYY-MM-DD");
        visit3.setHint("YYYY-MM-DD");
        finalVisit.setHint("YYYY-MM-DD");
        attachDatePicker(lmpDate);
        attachDatePicker(visit2);
        attachDatePicker(visit3);
        attachDatePicker(finalVisit);

        if (patient != null) {
            state.setText(value(patient.stateName), false);
            selectedStateCode = db.getCodeByName("states", text(state));
            setAdapter(district, db.listDistricts(selectedStateCode));
            district.setText(value(patient.districtName), false);
            selectedDistrictCode = db.getCodeByNameAndParent("districts", text(district), "state_code", selectedStateCode);
            subdistrict.setText(value(patient.subdistrictName), false);
            localBodyType.setText("Block", false);
            setAdapter(localBody, murshidabadBlocks());
            localBody.setText(value(patient.localBodyName), false);
            selectedLocalBodyCode = db.getCodeByNameAndParent("local_bodies", text(localBody), "district_code", selectedDistrictCode);
            setAdapter(ward, db.listWards(selectedLocalBodyCode));
            ward.setText(value(patient.wardName), false);
            village.setText(value(patient.villageName), false);
            motivator.setText(value(patient.motivatorName), false);
            doctor.setText(value(patient.doctorName), false);
        }

        lmpDate.addTextChangedListener(simpleWatcher(s -> updateEdd()));
        subdistrict.setOnItemClickListener((parent, view, position, id) -> localBody.setText(text(subdistrict), false));
        localBody.setOnItemClickListener((parent, view, position, id) -> subdistrict.setText(text(localBody), false));

        boolean lockedForStaff = patient != null && patient.recordLocked && !isAdmin();
        form.addView(formStep("1", "Basic Info", true,
                row("Patient ID *", patientId),
                row("Patient Name *", patientName),
                row("Mobile Number *", mobile),
                row("Motivator Name", motivator),
                row("Doctor Name *", doctor)
        ));
        form.addView(formStep("2", "Address", true,
                row("State / UT *", state),
                row("District *", district),
                row("Block Name *", localBody),
                row("Village *", village)
        ));
        form.addView(formStep("3", "Pregnancy Dates", true,
                row("LMP Date *", lmpDate),
                row("EDD Date *", eddDate),
                row("Entry / 1st Visit *", visit1)
        ));
        form.addView(formStep("4", "Visit Tracking", true,
                row("2nd Visit", visit2),
                row("3rd Visit", visit3),
                row("Final Visit", finalVisit)
        ));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        actions.addView(button("Clear", v -> showPatientForm(null)));
        savePatientButton = button(patient == null ? "Save Patient" : "Update Patient", v -> savePatient());
        savePatientButton.setEnabled(!lockedForStaff);
        actions.addView(savePatientButton);
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
        p.createdBy = old == null || empty(old.createdBy) ? currentUser : old.createdBy;
        p.remarks = "";

        String validation = validatePatient(p);
        if (validation != null) {
            toast(validation);
            return;
        }
        boolean shouldLockAfterSave = shouldConfirmFinalLock(old, p);
        p.recordLocked = old != null && old.recordLocked;
        if (shouldLockAfterSave) {
            new AlertDialog.Builder(this)
                    .setTitle("Lock Patient Record")
                    .setMessage("Saving a final visit locks this record for staff users. Continue?")
                    .setPositiveButton("Save and Lock", (dialog, which) -> {
                        p.recordLocked = true;
                        persistPatient(p, old);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }
        persistPatient(p, old);
    }

    private boolean shouldConfirmFinalLock(Patient old, Patient p) {
        return !empty(p.finalVisit) && (old == null || empty(old.finalVisit));
    }

    private void persistPatient(Patient p, Patient old) {
        try {
            boolean creating = editingPatient == null;
            setPatientSaveBusy(true);
            db.savePatient(p);
            if (old != null) {
                logPatientDiff(old, p);
            }
            db.logActivity(creating ? "PATIENT_CREATE" : "PATIENT_UPDATE", p.patientId, currentUser);
            if (status != null) {
                status.setText("Saving online...");
            }
            firebase.savePatient(p, (unused, error) -> runOnUiThread(() -> {
                hideKeyboard();
                if (error != null) {
                    rollbackPatientCache(p, old, creating);
                    setPatientSaveBusy(false);
                    toast("Cloud save failed: " + error.getMessage());
                    return;
                }
                toast(creating ? "Patient saved online" : "Patient updated online");
                showPatientList(false);
            }));
        } catch (Exception ex) {
            setPatientSaveBusy(false);
            toast("Save failed: " + ex.getMessage());
        }
    }

    private void setPatientSaveBusy(boolean busy) {
        if (savePatientButton == null) {
            return;
        }
        savePatientButton.setEnabled(!busy);
        savePatientButton.setText(busy ? "Saving..." : (editingPatient == null ? "Save Patient" : "Update Patient"));
    }

    private void rollbackPatientCache(Patient p, Patient old, boolean creating) {
        try {
            if (creating) {
                db.deletePatient(p.id);
                p.id = 0;
            } else if (old != null) {
                db.savePatient(old);
                editingPatient = old;
            }
        } catch (Exception ignored) {
        }
    }

    private String validatePatient(Patient p) {
        return PatientRules.validate(p, LocalDate.now());
    }

    private void logPatientDiff(Patient old, Patient p) {
        db.logChange(p.patientId, "patient_name", old.patientName, p.patientName, currentUser);
        db.logChange(p.patientId, "mobile_number", old.mobileNumber, p.mobileNumber, currentUser);
        db.logChange(p.patientId, "state_name", old.stateName, p.stateName, currentUser);
        db.logChange(p.patientId, "district_name", old.districtName, p.districtName, currentUser);
        db.logChange(p.patientId, "block_name", old.localBodyName, p.localBodyName, currentUser);
        db.logChange(p.patientId, "village_name", old.villageName, p.villageName, currentUser);
        db.logChange(p.patientId, "lmp_date", old.lmpDate, p.lmpDate, currentUser);
        db.logChange(p.patientId, "edd_date", old.eddDate, p.eddDate, currentUser);
        db.logChange(p.patientId, "motivator_name", old.motivatorName, p.motivatorName, currentUser);
        db.logChange(p.patientId, "doctor_name", old.doctorName, p.doctorName, currentUser);
        db.logChange(p.patientId, "visit2", old.visit2, p.visit2, currentUser);
        db.logChange(p.patientId, "visit3", old.visit3, p.visit3, currentUser);
        db.logChange(p.patientId, "final_visit", old.finalVisit, p.finalVisit, currentUser);
    }

    private void showPatientList(boolean adminMode) {
        showPatientList(adminMode, null, null);
    }

    private void showPatientList(boolean adminMode, String extraWhere, String[] extraArgs) {
        setPage(adminMode ? "Patient Management" : "Patient Search");
        boolean fullAccess = adminMode && isAdmin();
        String visibleWhere = fullAccess ? extraWhere : scopedWhere(extraWhere);
        String[] visibleArgs = fullAccess ? extraArgs : scopedArgs(extraArgs);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        content.addView(page, new LinearLayout.LayoutParams(-1, -1));
        EditText search = input("");
        search.setHint("Search name, ID, mobile, village, block, doctor");
        page.addView(search);
        page.addView(scrollingActions(
                button("Export CSV", v -> exportPatientsCsv(text(search), visibleWhere, visibleArgs)),
                button("Excel", v -> startListExport(text(search), visibleWhere, visibleArgs, REQ_EXPORT_EXCEL, "patients.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
                button("PDF", v -> startListExport(text(search), visibleWhere, visibleArgs, REQ_EXPORT_PDF, "patients.pdf", "application/pdf")),
                button("New Patient", v -> showPatientForm(null))
        ));
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        Runnable reload = () -> renderPatientRows(list, db.listPatients(text(search), visibleWhere, visibleArgs), adminMode);
        search.addTextChangedListener(simpleWatcher(s -> reload.run()));
        reload.run();
    }

    private void renderPatientRows(LinearLayout list, List<Patient> patients, boolean adminMode) {
        list.removeAllViews();
        TextView count = chip(patients.size() + " patient(s)", PRIMARY_SOFT, PRIMARY_DARK);
        list.addView(count);
        if (patients.isEmpty()) {
            LinearLayout empty = card(SURFACE_WARM, 1, Color.rgb(230, 214, 190));
            TextView title = label("No matching patient records", 15, true);
            title.setTextColor(WARNING);
            empty.addView(title);
            empty.addView(smallText("Create a new patient record or adjust the search text."));
            empty.addView(navButton("New Patient", v -> showPatientForm(null)));
            list.addView(empty);
            return;
        }
        for (Patient p : patients) {
            LinearLayout card = card();
            card.addView(label(p.patientId + "  |  " + value(p.patientName), 15, true));
            card.addView(smallText("Mobile: " + value(p.mobileNumber) + "  Village: " + value(p.villageName)));
            card.addView(smallText("District: " + value(p.districtName) + "  Block: " + value(p.localBodyName)));
            card.addView(smallText("Motivator: " + value(p.motivatorName) + "  Doctor: " + value(p.doctorName)));
            card.addView(smallText("LMP: " + value(p.lmpDate) + "  EDD: " + value(p.eddDate) + "  Final: " + value(p.finalVisit)));
            if (p.recordLocked) {
                TextView locked = chip("Locked after final visit", ACCENT, Color.WHITE);
                card.addView(locked);
            }
            LinearLayout actions = new LinearLayout(this);
            actions.addView(button("View", v -> showPatientDetail(db.getPatient(p.id), adminMode)));
            if (adminMode && isAdmin()) {
                actions.addView(button("Unlock", v -> {
                    p.recordLocked = false;
                    firebase.savePatient(p, (unused, error) -> runOnUiThread(() -> {
                        if (error != null) {
                            p.recordLocked = true;
                            toast("Unlock sync failed: " + error.getMessage());
                            return;
                        }
                        db.unlockPatient(p.id);
                        db.logActivity("PATIENT_UNLOCK", p.patientId, currentUser);
                        showPatientList(true);
                    }));
                }));
                actions.addView(button("Delete", v -> confirmDeletePatient(p)));
            }
            card.addView(actions);
            list.addView(card);
        }
    }

    private void showPatientDetail(Patient p, boolean adminMode) {
        if (p == null) {
            toast("Patient not found");
            return;
        }
        setPage("Patient Detail");
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(page);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));

        page.addView(section(value(p.patientName),
                smallText("Patient ID: " + value(p.patientId)),
                smallText("Mobile: " + value(p.mobileNumber)),
                smallText("State: " + value(p.stateName) + " | District: " + value(p.districtName)),
                smallText("Block: " + value(p.localBodyName) + " | Village: " + value(p.villageName)),
                smallText("Motivator: " + value(p.motivatorName) + " | Doctor: " + value(p.doctorName))
        ));
        page.addView(section("Visit Timeline", visitTimeline(p)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.addView(navButton("Back to Search", v -> showPatientList(adminMode)));
        if (!p.recordLocked || isAdmin()) {
            actions.addView(button("Edit Patient", v -> showPatientForm(db.getPatient(p.id))));
        }
        actions.addView(button("Export Single Excel", v -> startPatientExport(p.id, REQ_EXPORT_EXCEL, value(p.patientId) + ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
        actions.addView(button("Export Single PDF", v -> startPatientExport(p.id, REQ_EXPORT_PDF, value(p.patientId) + ".pdf", "application/pdf")));
        if (p.recordLocked) {
            actions.addView(chip("Locked after final visit", ACCENT, Color.WHITE));
        }
        if (adminMode && isAdmin()) {
            actions.addView(button("Unlock", v -> {
                p.recordLocked = false;
                firebase.savePatient(p, (unused, error) -> runOnUiThread(() -> {
                    if (error != null) {
                        p.recordLocked = true;
                        toast("Unlock sync failed: " + error.getMessage());
                        return;
                    }
                    db.unlockPatient(p.id);
                    db.logActivity("PATIENT_UNLOCK", p.patientId, currentUser);
                    showPatientDetail(db.getPatient(p.id), true);
                }));
            }));
            actions.addView(button("Delete", v -> confirmDeletePatient(p)));
        }
        page.addView(section("Actions", actions));
    }

    private void confirmDeletePatient(Patient p) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Patient")
                .setMessage("Delete " + p.patientName + " (" + p.patientId + ")?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    firebase.deletePatient(p, (unused, error) -> runOnUiThread(() -> {
                        if (error != null) {
                            toast("Delete sync failed: " + error.getMessage());
                            return;
                        }
                        db.deletePatient(p.id);
                        db.logActivity("PATIENT_DELETE", p.patientId, currentUser);
                        showPatientList(true);
                    }));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showReports() {
        setPage("Reports");
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        content.addView(page, new LinearLayout.LayoutParams(-1, -1));

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.VERTICAL);
        EditText from = input("");
        EditText to = input("");
        AutoCompleteTextView block = auto(murshidabadBlocks());
        AutoCompleteTextView villageFilter = auto(list());
        AutoCompleteTextView motivatorFilter = auto(db.listNames("custom_motivators"));
        AutoCompleteTextView statusFilter = auto(list("All", "Open", "Locked"));
        from.setHint("From YYYY-MM-DD");
        to.setHint("To YYYY-MM-DD");
        block.setHint("All blocks");
        villageFilter.setHint("Village");
        motivatorFilter.setHint("Motivator");
        statusFilter.setText("All", false);
        attachDatePicker(from);
        attachDatePicker(to);
        filters.addView(compactTwoColumn(row("From", from), row("To", to)));
        filters.addView(compactTwoColumn(row("Block", block), row("Village", villageFilter)));
        filters.addView(compactTwoColumn(row("Motivator", motivatorFilter), row("Status", statusFilter)));

        LinearLayout reportBody = new LinearLayout(this);
        reportBody.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(reportBody);

        Runnable[] render = new Runnable[1];
        render[0] = () -> renderReports(reportBody, text(from), text(to), text(block), text(villageFilter), text(motivatorFilter), text(statusFilter));
        page.addView(scrollingActions(
                button("Apply", v -> render[0].run()),
                button("Search", v -> openFilteredPatientSearch(text(from), text(to), text(block), text(villageFilter), text(motivatorFilter), text(statusFilter))),
                button("Excel", v -> startFilteredExport(text(from), text(to), text(block), text(villageFilter), text(motivatorFilter), text(statusFilter), REQ_EXPORT_EXCEL, "patients.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
                button("PDF", v -> startFilteredExport(text(from), text(to), text(block), text(villageFilter), text(motivatorFilter), text(statusFilter), REQ_EXPORT_PDF, "patients.pdf", "application/pdf"))
        ));
        page.addView(collapsibleSection("Filters", false, filters));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        render[0].run();
    }

    private void showExportCenter() {
        setPage("Export Center");
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(page);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));

        int total = db.countPatients(null, null);
        int locked = db.countPatients("record_locked = 1", null);
        page.addView(section("Export Records",
                emptyState(total + " records ready", "Save Excel or PDF directly through Android's file picker, then share it from the system sheet."),
                scrollingActions(
                        button("Excel", v -> startExport("", REQ_EXPORT_EXCEL, "blue_bird_patients.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
                        button("PDF", v -> startExport("", REQ_EXPORT_PDF, "blue_bird_patients.pdf", "application/pdf")),
                        button("Search Filtered Export", v -> showPatientList(false))
                )
        ));
        page.addView(section("Backup",
                smallText("Locked records: " + locked + " | Database backup is admin-only restore data."),
                scrollingActions(
                        button("Create Backup", v -> createBackupNow()),
                        button("Save Backup File", v -> startBackupExport())
                )
        ));
        page.addView(section("Export Tips",
                smallText("Use Reports for date, block, village, motivator, or lock-status filtered Excel/PDF exports."),
                smallText("Use Patient Detail when one patient's PDF or Excel file is needed.")
        ));
    }

    private void renderReports(LinearLayout page, String from, String to, String block, String village, String motivatorName, String statusName) {
        page.removeAllViews();
        ReportFilter filter = reportWhere(from, to, block, village, motivatorName, statusName);
        String where = filter.where;
        String[] args = filter.args;
        int total = db.countPatients(where, args);
        int locked = db.countPatients(appendWhere(where, "record_locked = 1"), appendArgs(args));
        int edd30 = db.countPatients(appendWhere(where, "edd_date BETWEEN ? AND ?"), appendArgs(args, LocalDate.now().toString(), LocalDate.now().plusDays(30).toString()));
        int today = db.countPatients(appendWhere(where, "entry_date = ?"), appendArgs(args, LocalDate.now().toString()));
        page.addView(statGrid(
                stat("Filtered Records", total, v -> showPatientList(false, where, args)),
                stat("Today's Entries", today, v -> showPatientList(false, appendWhere(where, "entry_date = ?"), appendArgs(args, LocalDate.now().toString()))),
                stat("EDD 30 Days", edd30, v -> showPatientList(false, appendWhere(where, "edd_date BETWEEN ? AND ?"), appendArgs(args, LocalDate.now().toString(), LocalDate.now().plusDays(30).toString()))),
                stat("Locked", locked, v -> showPatientList(false, appendWhere(where, "record_locked = 1"), args))
        ));
        page.addView(section("Report Snapshot",
                progressRow("Open records", Math.max(0, total - locked), Math.max(total, 1), total == 0 ? 0 : Math.round((total - locked) * 100f / total)),
                progressRow("Locked records", locked, Math.max(total, 1), total == 0 ? 0 : Math.round(locked * 100f / total)),
                progressRow("EDD within 30 days", edd30, Math.max(total, 1), total == 0 ? 0 : Math.round(edd30 * 100f / total))
        ));
        if (total == 0) {
            page.addView(section("Report Result", emptyActionState("No records match these filters", "Change the filters or create a patient record first.", "New Patient", v -> showPatientForm(null))));
            return;
        }
        LinearLayout visits = new LinearLayout(this);
        visits.setOrientation(LinearLayout.VERTICAL);
        for (String[] row : db.visitCompletionRows(where, args)) {
            int done = Integer.parseInt(row[1]);
            int pct = total == 0 ? 0 : Math.round(done * 100f / total);
            visits.addView(progressRow(row[0], done, total, pct));
        }
        page.addView(section("Visit Completion", visits));
        page.addView(reportMap("Motivator Performance", db.countBy("motivator_name", where, args)));
        page.addView(reportMap("Patients by Village", db.countBy("village_name", where, args)));
        page.addView(reportMap("Patients by Block", db.countBy("local_body_name", where, args)));
        page.addView(reportMap("Monthly Summary", monthlySummary(where, args)));
    }

    private ReportFilter reportWhere(String from, String to, String block, String village, String motivatorName, String statusName) {
        List<String> clauses = new java.util.ArrayList<>();
        List<String> args = new java.util.ArrayList<>();
        if (!empty(from)) {
            clauses.add("entry_date >= ?");
            args.add(from);
        }
        if (!empty(to)) {
            clauses.add("entry_date <= ?");
            args.add(to);
        }
        if (!empty(block)) {
            clauses.add("local_body_name = ?");
            args.add(block);
        }
        if (!empty(village)) {
            clauses.add("village_name LIKE ?");
            args.add("%" + village + "%");
        }
        if (!empty(motivatorName)) {
            clauses.add("motivator_name = ?");
            args.add(motivatorName);
        }
        if ("Locked".equalsIgnoreCase(value(statusName))) {
            clauses.add("record_locked = 1");
        } else if ("Open".equalsIgnoreCase(value(statusName))) {
            clauses.add("record_locked = 0");
        }
        return new ReportFilter(clauses.isEmpty() ? null : String.join(" AND ", clauses), args);
    }

    private void openFilteredPatientSearch(String from, String to, String block, String village, String motivatorName, String statusName) {
        ReportFilter filter = reportWhere(from, to, block, village, motivatorName, statusName);
        showPatientList(false, filter.where, filter.args);
    }

    private String appendWhere(String where, String extra) {
        return empty(where) ? extra : "(" + where + ") AND (" + extra + ")";
    }

    private String[] appendArgs(String[] base, String... extra) {
        String[] out = new String[(base == null ? 0 : base.length) + extra.length];
        if (base != null) {
            System.arraycopy(base, 0, out, 0, base.length);
        }
        System.arraycopy(extra, 0, out, base == null ? 0 : base.length, extra.length);
        return out;
    }

    private String dashboardScopeWhere() {
        return isAdmin() ? null : "created_by = ?";
    }

    private String[] dashboardScopeArgs() {
        return isAdmin() ? null : new String[]{currentUser};
    }

    private String scopedWhere(String where) {
        return isAdmin() ? where : appendWhere(where, "created_by = ?");
    }

    private String[] scopedArgs(String[] args) {
        return isAdmin() ? args : appendArgs(args, currentUser);
    }

    private void showScopedPatientList(String where, String[] args) {
        showPatientList(false, where, args);
    }

    private Map<String, Integer> monthlySummary(String where, String[] args) {
        java.util.LinkedHashMap<String, Integer> rows = new java.util.LinkedHashMap<>();
        YearMonth month = YearMonth.now().minusMonths(11);
        for (int i = 0; i < 12; i++) {
            String prefix = month.plusMonths(i).toString();
            rows.put(prefix, db.countPatients(appendWhere(where, "entry_date LIKE ?"), appendArgs(args, prefix + "%")));
        }
        return rows;
    }

    private View reportMap(String title, Map<String, Integer> rows) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        if (rows.isEmpty()) {
            body.addView(emptyState("No data", "Records will appear here after matching patients are saved."));
        }
        int max = 1;
        for (Integer value : rows.values()) {
            max = Math.max(max, value);
        }
        for (Map.Entry<String, Integer> row : rows.entrySet()) {
            body.addView(progressRow(row.getKey(), row.getValue(), max, Math.round(row.getValue() * 100f / max)));
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
                navButton("Export Backup File", v -> startBackupExport()),
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
                if (isAdmin()) {
                    row.addView(button("Restore", v -> confirmRestore(file)));
                }
                list.addView(row);
            }
        }
        page.addView(section("Existing Backups", list));
    }

    private void startExport(String filter, int requestCode, String fileName, String mimeType) {
        pendingExportFilter = value(filter);
        pendingExportWhere = null;
        pendingExportArgs = null;
        pendingExportPatientId = -1;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        try {
            startActivityForResult(intent, requestCode);
        } catch (Exception ex) {
            toast("No document app available: " + ex.getMessage());
        }
    }

    private void startFilteredExport(String from, String to, String block, String village, String motivatorName, String statusName, int requestCode, String fileName, String mimeType) {
        ReportFilter filter = reportWhere(from, to, block, village, motivatorName, statusName);
        pendingExportFilter = "";
        pendingExportWhere = filter.where;
        pendingExportArgs = filter.args;
        pendingExportPatientId = -1;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        try {
            startActivityForResult(intent, requestCode);
        } catch (Exception ex) {
            toast("No document app available: " + ex.getMessage());
        }
    }

    private void startListExport(String filter, String where, String[] args, int requestCode, String fileName, String mimeType) {
        pendingExportFilter = value(filter);
        pendingExportWhere = where;
        pendingExportArgs = args;
        pendingExportPatientId = -1;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        try {
            startActivityForResult(intent, requestCode);
        } catch (Exception ex) {
            toast("No document app available: " + ex.getMessage());
        }
    }

    private void startPatientExport(long patientId, int requestCode, String fileName, String mimeType) {
        pendingExportFilter = "";
        pendingExportWhere = null;
        pendingExportArgs = null;
        pendingExportPatientId = patientId;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        try {
            startActivityForResult(intent, requestCode);
        } catch (Exception ex) {
            toast("No document app available: " + ex.getMessage());
        }
    }

    private void startBackupExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, "blue_bird_backup_" + LocalDate.now() + ".db");
        try {
            startActivityForResult(intent, REQ_EXPORT_BACKUP);
        } catch (Exception ex) {
            toast("No document app available: " + ex.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) {
                toast("Could not open export location");
                return;
            }
            if (requestCode == REQ_EXPORT_BACKUP) {
                copyFile(getDatabasePath(MaternalDbHelper.DB_NAME), out);
                toast("Backup exported");
            } else if (requestCode == REQ_EXPORT_EXCEL) {
                writePatientsXlsx(out, pendingExportPatients());
                toast("Excel export saved");
            } else if (requestCode == REQ_EXPORT_PDF) {
                writePatientsPdf(out, pendingExportPatients());
                toast("PDF export saved");
            }
            shareExport(uri, requestCode);
        } catch (Exception ex) {
            toast("Export failed: " + ex.getMessage());
        }
    }

    private List<Patient> pendingExportPatients() {
        if (pendingExportPatientId > 0) {
            Patient p = db.getPatient(pendingExportPatientId);
            return p == null ? java.util.Collections.emptyList() : java.util.Collections.singletonList(p);
        }
        if (!empty(pendingExportWhere)) {
            return db.listPatients(pendingExportFilter, pendingExportWhere, pendingExportArgs);
        }
        return db.listPatients(pendingExportFilter);
    }

    private void shareExport(Uri uri, int requestCode) {
        if (requestCode == REQ_EXPORT_BACKUP) {
            return;
        }
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(requestCode == REQ_EXPORT_PDF ? "application/pdf" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(share, "Share export"));
        } catch (Exception ignored) {
        }
    }

    private void exportPatientsCsv(String filter) {
        exportPatientsCsv(filter, null, null);
    }

    private void exportPatientsCsv(String filter, String where, String[] args) {
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
            String[] headers = patientExportHeaders();
            for (int i = 0; i < headers.length; i++) {
                if (i > 0) {
                    csv.append(',');
                }
                csv.append(csv(headers[i]));
            }
            csv.append('\n');
            List<Patient> patients = empty(where) ? db.listPatients(filter) : db.listPatients(filter, where, args);
            for (Patient p : patients) {
                String[] values = patientExportValues(p);
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) {
                        csv.append(',');
                    }
                    csv.append(csv(values[i]));
                }
                csv.append('\n');
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

    private void writePatientsXlsx(OutputStream out, List<Patient> patients) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            addZipEntry(zip, "[Content_Types].xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                            "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                            "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                            "</Types>");
            addZipEntry(zip, "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                            "</Relationships>");
            addZipEntry(zip, "xl/_rels/workbook.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                            "</Relationships>");
            addZipEntry(zip, "xl/workbook.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                            "<sheets><sheet name=\"Patients\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            addZipEntry(zip, "xl/worksheets/sheet1.xml", patientsSheetXml(patients));
        }
    }

    private String patientsSheetXml(List<Patient> patients) {
        String[] headers = patientExportHeaders();
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        appendXlsxRow(xml, 1, headers);
        int row = 2;
        for (Patient p : patients) {
            appendXlsxRow(xml, row++, patientExportValues(p));
        }
        xml.append("</sheetData></worksheet>");
        return xml.toString();
    }

    private void appendXlsxRow(StringBuilder xml, int rowNumber, String[] values) {
        xml.append("<row r=\"").append(rowNumber).append("\">");
        for (int i = 0; i < values.length; i++) {
            xml.append("<c r=\"").append((char) ('A' + i)).append(rowNumber).append("\" t=\"inlineStr\"><is><t>")
                    .append(xml(value(values[i])))
                    .append("</t></is></c>");
        }
        xml.append("</row>");
    }

    private void addZipEntry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void writePatientsPdf(OutputStream out, List<Patient> patients) throws Exception {
        PdfDocument document = new PdfDocument();
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTextSize(16);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setColor(PRIMARY_DARK);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(9);
        textPaint.setColor(TEXT);
        int pageWidth = 595;
        int pageHeight = 842;
        int margin = 28;
        int pageNo = 1;
        PdfDocument.Page page = newPdfPage(document, pageWidth, pageHeight, pageNo);
        int y = drawPdfHeader(page, titlePaint, textPaint, margin, pageNo);
        for (Patient p : patients) {
            if (y > pageHeight - 70) {
                document.finishPage(page);
                page = newPdfPage(document, pageWidth, pageHeight, ++pageNo);
                y = drawPdfHeader(page, titlePaint, textPaint, margin, pageNo);
            }
            page.getCanvas().drawText(value(p.patientId) + " | " + value(p.patientName), margin, y, textPaint);
            y += 14;
            page.getCanvas().drawText("Mobile: " + value(p.mobileNumber) + " | Block: " + value(p.localBodyName) + " | Village: " + value(p.villageName), margin, y, textPaint);
            y += 14;
            page.getCanvas().drawText("LMP: " + value(p.lmpDate) + " | EDD: " + value(p.eddDate) + " | Final: " + value(p.finalVisit), margin, y, textPaint);
            y += 20;
        }
        document.finishPage(page);
        document.writeTo(out);
        document.close();
    }

    private PdfDocument.Page newPdfPage(PdfDocument document, int width, int height, int pageNo) {
        return document.startPage(new PdfDocument.PageInfo.Builder(width, height, pageNo).create());
    }

    private int drawPdfHeader(PdfDocument.Page page, Paint titlePaint, Paint textPaint, int margin, int pageNo) {
        page.getCanvas().drawText(HOSPITAL_NAME, margin, 36, titlePaint);
        page.getCanvas().drawText(APP_NAME + " | Patient Export | Page " + pageNo, margin, 54, textPaint);
        page.getCanvas().drawLine(margin, 66, 565, 66, textPaint);
        return 86;
    }

    private String[] patientExportHeaders() {
        return new String[]{"Serial", "Entry Date", "Patient Name", "Patient ID", "State", "District", "Block", "Village", "Mobile", "Motivator", "Doctor", "LMP", "EDD", "1st Visit", "2nd Visit", "3rd Visit", "Final Visit", "Locked"};
    }

    private String[] patientExportValues(Patient p) {
        return new String[]{String.valueOf(p.serialNumber), value(p.entryDate), value(p.patientName), value(p.patientId), value(p.stateName), value(p.districtName), value(p.localBodyName), value(p.villageName), value(p.mobileNumber), value(p.motivatorName), value(p.doctorName), value(p.lmpDate), value(p.eddDate), value(p.visit1), value(p.visit2), value(p.visit3), value(p.finalVisit), p.recordLocked ? "1" : "0"};
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
        if (!isAdmin()) {
            toast("Only admin can restore backups");
            return;
        }
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

    private void copyFile(File source, OutputStream out) throws Exception {
        try (FileInputStream in = new FileInputStream(source)) {
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
        int total = db.countPatients(null, null);
        int locked = db.countPatients("record_locked = 1", null);
        page.addView(section("Admin Control Panel",
                compactTwoColumn(stat("Records", total, v -> showPatientList(true)), stat("Locked", locked, v -> showPatientList(true, "record_locked = 1", null)))
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
        list.addView(smallText("Loading Firebase roles..."));
        firebase.listRoles((rows, error) -> runOnUiThread(() -> {
            list.removeAllViews();
            list.addView(smallText("Create app users here with email, password, and role. Admin controls access."));
            if (error != null) {
                list.addView(smallText("Could not load roles: " + error.getMessage()));
                return;
            }
            if (rows == null || rows.isEmpty()) {
                list.addView(emptyState("No users added yet", "Create the first staff login from the Add User button."));
                return;
            }
            for (String[] user : rows) {
                LinearLayout row = card();
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout details = new LinearLayout(this);
                details.setOrientation(LinearLayout.VERTICAL);
                details.addView(label(user[0], 13, true));
                details.addView(smallText("Role: " + user[1] + (user[0].equalsIgnoreCase(currentUser) ? " | Current login" : "")));
                row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
                if (!user[0].equalsIgnoreCase(currentUser)) {
                    row.addView(button("Remove Access", v -> firebase.deleteRole(user[0], (unused, deleteError) -> runOnUiThread(() -> {
                        if (deleteError != null) {
                            toast("Could not remove access: " + deleteError.getMessage());
                        }
                        showAdmin();
                    }))));
                }
                list.addView(row);
            }
        }));
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
        EditText email = input("");
        EditText password = input("");
        AutoCompleteTextView role = auto(list("STAFF", "ADMIN"));
        role.setText("STAFF", false);
        password.setHint("Minimum 6 characters");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(smallText("This creates the Firebase login and grants app access. Share the password only with that user."));
        box.addView(row("Email", email));
        box.addView(row("Temporary Password", password));
        box.addView(row("Role", role));
        new AlertDialog.Builder(this)
                .setTitle("Create App User")
                .setView(box)
                .setPositiveButton("Create", (dialog, which) -> {
                    if (empty(text(email))) {
                        toast("Email is required");
                        return;
                    }
                    if (empty(text(password)) || text(password).length() < 6) {
                        toast("Password must be at least 6 characters");
                        return;
                    }
                    firebase.createAuthUserAndRole(text(email), text(password), text(role), (unused, error) -> runOnUiThread(() -> {
                        if (error != null) {
                            toast("Could not create user: " + error.getMessage());
                            return;
                        }
                        db.logActivity("USER_CREATE", text(email), currentUser);
                        toast("User created");
                        showAdmin();
                    }));
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

    private void attachDatePicker(EditText field) {
        field.setFocusable(false);
        field.setOnClickListener(v -> showDatePicker(field));
    }

    private void showDatePicker(EditText field) {
        LocalDate date;
        try {
            date = empty(text(field)) ? LocalDate.now() : LocalDate.parse(text(field));
        } catch (Exception ex) {
            date = LocalDate.now();
        }
        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> field.setText(LocalDate.of(year, month + 1, dayOfMonth).toString()),
                date.getYear(),
                date.getMonthValue() - 1,
                date.getDayOfMonth()
        ).show();
    }

    private LinearLayout section(String title, View... rows) {
        LinearLayout box = card();
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView accent = new TextView(this);
        accent.setBackground(rounded(ACCENT, dp(2), 0, ACCENT));
        LinearLayout.LayoutParams accentLp = new LinearLayout.LayoutParams(dp(4), dp(20));
        accentLp.setMargins(0, 0, dp(8), 0);
        head.addView(accent, accentLp);
        TextView heading = label(title, 14, true);
        heading.setTextColor(PRIMARY_DARK);
        head.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        head.setPadding(0, 0, 0, dp(7));
        box.addView(head);
        for (View row : rows) {
            box.addView(row);
        }
        return box;
    }

    private LinearLayout collapsibleSection(String title, boolean expanded, View... rows) {
        LinearLayout box = card();
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = label(title, 15, true);
        heading.setTextColor(PRIMARY_DARK);
        TextView indicator = label(expanded ? "-" : "+", 18, true);
        indicator.setTextColor(PRIMARY);
        indicator.setGravity(Gravity.CENTER);
        head.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(indicator, new LinearLayout.LayoutParams(dp(34), dp(34)));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setVisibility(expanded ? View.VISIBLE : View.GONE);
        for (View row : rows) {
            body.addView(row);
        }
        head.setOnClickListener(v -> {
            boolean open = body.getVisibility() != View.VISIBLE;
            indicator.setText(open ? "-" : "+");
            body.setVisibility(open ? View.VISIBLE : View.GONE);
            body.setAlpha(open ? 0f : 1f);
            if (open) {
                body.animate().alpha(1f).setDuration(160).start();
            }
        });
        box.addView(head);
        box.addView(body);
        return box;
    }

    private LinearLayout formStep(String number, String title, boolean expanded, View... rows) {
        LinearLayout box = card();
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = label(number, 13, true);
        badge.setTextColor(Color.WHITE);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(gradient(PRIMARY, ACCENT, dp(15)));
        TextView heading = label(title, 15, true);
        heading.setTextColor(PRIMARY_DARK);
        TextView indicator = label(expanded ? "-" : "+", 18, true);
        indicator.setTextColor(PRIMARY);
        indicator.setGravity(Gravity.CENTER);
        head.addView(badge, new LinearLayout.LayoutParams(dp(30), dp(30)));
        heading.setPadding(dp(9), 0, 0, 0);
        head.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(indicator, new LinearLayout.LayoutParams(dp(34), dp(34)));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setVisibility(expanded ? View.VISIBLE : View.GONE);
        for (View row : rows) {
            body.addView(row);
        }
        head.setOnClickListener(v -> {
            boolean open = body.getVisibility() != View.VISIBLE;
            indicator.setText(open ? "-" : "+");
            body.setVisibility(open ? View.VISIBLE : View.GONE);
            body.setAlpha(open ? 0f : 1f);
            if (open) {
                body.animate().alpha(1f).translationY(0f).setDuration(160).start();
            }
        });
        box.addView(head);
        box.addView(body);
        return box;
    }

    private View visitTimeline(Patient p) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.addView(timelineStep("LMP", p.lmpDate, true));
        list.addView(timelineStep("EDD", p.eddDate, true));
        list.addView(timelineStep("1st Visit", p.visit1, true));
        list.addView(timelineStep("2nd Visit", p.visit2, false));
        list.addView(timelineStep("3rd Visit", p.visit3, false));
        list.addView(timelineStep("Final Visit", p.finalVisit, false));
        return list;
    }

    private View timelineStep(String title, String date, boolean required) {
        boolean done = !empty(date);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(5), 0, dp(5));
        TextView dot = label(done ? "✓" : "○", 18, true);
        dot.setTextColor(done ? ACCENT : MUTED);
        dot.setGravity(Gravity.CENTER);
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView name = label(title + (required ? " *" : ""), 13, true);
        name.setTextColor(PRIMARY_DARK);
        TextView value = smallText(done ? date : "Pending");
        value.setTextColor(done ? TEXT : MUTED);
        copy.addView(name);
        copy.addView(value);
        row.addView(dot, new LinearLayout.LayoutParams(dp(34), dp(42)));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private LinearLayout compactTwoColumn(View left, View right) {
        LinearLayout row = new LinearLayout(this);
        boolean stack = getResources().getConfiguration().screenWidthDp < 600;
        row.setOrientation(stack ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        if (stack) {
            row.addView(left, new LinearLayout.LayoutParams(-1, -2));
            row.addView(right, new LinearLayout.LayoutParams(-1, -2));
        } else {
            row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(right, new LinearLayout.LayoutParams(0, -2, 1));
        }
        return row;
    }

    private View scrollingActions(View... actions) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(6));
        for (View action : actions) {
            row.addView(action);
        }
        scroll.addView(row);
        return scroll;
    }

    private TextView bottomNavItem(String pageName, String icon, String text, View.OnClickListener listener) {
        boolean active = currentPage.equals(pageName) || (currentPage.equals("Edit Patient") && pageName.equals("Patient Entry")) || (currentPage.equals("Patient Detail") && pageName.equals("Patient Search")) || (currentPage.equals("Patient Management") && pageName.equals("Administration"));
        TextView item = label(icon + "\n" + text, 10, true);
        item.setGravity(Gravity.CENTER);
        item.setSingleLine(false);
        item.setTextColor(active ? Color.WHITE : PRIMARY_DARK);
        item.setBackground(rounded(active ? PRIMARY : Color.TRANSPARENT, dp(8), 0, Color.TRANSPARENT));
        item.setOnClickListener(listener);
        return item;
    }

    private View emptyState(String title, String message) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(8), dp(10), dp(8));
        box.setBackground(rounded(Color.argb(70, 255, 255, 255), dp(10), dp(1), Color.argb(140, 255, 255, 255)));
        TextView t = label(title, 14, true);
        t.setTextColor(PRIMARY_DARK);
        TextView m = smallText(message);
        m.setTextColor(MUTED);
        box.addView(t);
        box.addView(m);
        return box;
    }

    private View emptyActionState(String title, String message, String action, View.OnClickListener listener) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(8), dp(10), dp(8));
        box.setBackground(rounded(Color.argb(76, 255, 255, 255), dp(10), dp(1), Color.argb(150, 255, 255, 255)));
        TextView t = label(title, 14, true);
        t.setTextColor(PRIMARY_DARK);
        TextView m = smallText(message);
        m.setTextColor(MUTED);
        box.addView(t);
        box.addView(m);
        box.addView(navButton(action, listener));
        return box;
    }

    private View progressRow(String title, int done, int total, int pct) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(5), 0, dp(5));
        TextView label = smallText(title + "  " + done + " / " + total + "  (" + pct + "%)");
        label.setTextColor(TEXT);
        TextView bar = new TextView(this);
        bar.setBackground(rounded(PRIMARY_SOFT, dp(4), 0, PRIMARY_SOFT));
        LinearLayout fill = new LinearLayout(this);
        fill.setBackground(rounded(ACCENT, dp(4), 0, ACCENT));
        LinearLayout.LayoutParams fillLp = new LinearLayout.LayoutParams(0, dp(6), Math.max(1, pct));
        LinearLayout.LayoutParams restLp = new LinearLayout.LayoutParams(0, dp(6), Math.max(1, 100 - pct));
        LinearLayout meter = new LinearLayout(this);
        meter.setOrientation(LinearLayout.HORIZONTAL);
        meter.setBackground(rounded(PRIMARY_SOFT, dp(4), 0, PRIMARY_SOFT));
        meter.addView(fill, fillLp);
        meter.addView(new TextView(this), restLp);
        box.addView(label);
        box.addView(meter);
        return box;
    }

    private View skeletonLine(int widthPercent) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.HORIZONTAL);
        TextView line = new TextView(this);
        line.setBackground(rounded(Color.argb(120, 255, 255, 255), dp(5), dp(1), Color.argb(120, 255, 255, 255)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(10), Math.max(1, widthPercent));
        lp.setMargins(0, dp(4), 0, dp(4));
        wrap.addView(line, lp);
        wrap.addView(new TextView(this), new LinearLayout.LayoutParams(0, dp(10), Math.max(1, 100 - widthPercent)));
        return wrap;
    }

    private LinearLayout row(String label, View input) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(2), dp(4), dp(2), dp(5));
        TextView tv = label(label, 12, true);
        tv.setTextColor(MUTED);
        row.addView(tv);
        row.addView(input, new LinearLayout.LayoutParams(-1, dp(42)));
        return row;
    }

    private LinearLayout card() {
        return card(SURFACE, 1, BORDER);
    }

    private LinearLayout card(int color, int strokeWidth, int strokeColor) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        if (color == SURFACE) {
            box.setBackground(glassPanel(dp(10)));
        } else {
            int border = strokeWidth == 0 ? 0 : Color.argb(170, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor));
            box.setBackground(rounded(color, dp(10), strokeWidth == 0 ? 0 : dp(strokeWidth), border));
        }
        box.setPadding(dp(10), dp(10), dp(10), dp(10));
        box.setElevation(dp(3));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(7));
        box.setLayoutParams(lp);
        animateIn(box);
        return box;
    }

    private TextView brandMark() {
        TextView mark = label("BBH", 14, true);
        mark.setTextColor(Color.WHITE);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(gradient(PRIMARY, ACCENT, dp(28)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.setMargins(0, 0, 0, dp(8));
        mark.setLayoutParams(lp);
        return mark;
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
        edit.setBackground(glassInput(false));
        edit.setPadding(dp(12), 0, dp(12), 0);
        edit.setOnFocusChangeListener((v, hasFocus) ->
                edit.setBackground(glassInput(hasFocus)));
        return edit;
    }

    private EditText readOnlyInput(String value) {
        EditText edit = input(value);
        edit.setEnabled(false);
        edit.setTextColor(MUTED);
        edit.setBackground(rounded(Color.rgb(235, 241, 246), dp(8), dp(1), BORDER));
        return edit;
    }

    private AutoCompleteTextView auto(List<String> values) {
        AutoCompleteTextView view = new AutoCompleteTextView(this);
        view.setSingleLine(true);
        view.setThreshold(1);
        view.setTextSize(15);
        view.setTextColor(TEXT);
        view.setHintTextColor(Color.rgb(135, 151, 166));
        view.setBackground(glassInput(false));
        view.setPadding(dp(12), 0, dp(12), 0);
        view.setOnFocusChangeListener((v, hasFocus) ->
                view.setBackground(glassInput(hasFocus)));
        setAdapter(view, values);
        return view;
    }

    private void setAdapter(AutoCompleteTextView view, List<String> values) {
        view.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, values));
    }

    private Button navButton(String text, View.OnClickListener listener) {
        Button b = button(text, listener);
        b.setAllCaps(false);
        b.setTextColor(PRIMARY);
        b.setBackground(rounded(PRIMARY_SOFT, dp(18), dp(1), Color.rgb(184, 207, 225)));
        b.setElevation(dp(1));
        return b;
    }

    private Button button(String text, View.OnClickListener listener) {
        Button b = new Button(this) {
            @Override
            public boolean performClick() {
                super.performClick();
                return true;
            }
        };
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setTypeface(b.getTypeface(), Typeface.BOLD);
        b.setAllCaps(false);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(dp(42));
        b.setMinimumHeight(dp(42));
        b.setPadding(dp(12), 0, dp(12), 0);
        b.setBackground(gradient(PRIMARY, PRIMARY_DARK, dp(8)));
        b.setElevation(dp(2));
        b.setOnClickListener(listener);
        b.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                view.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    view.performClick();
                }
            }
            return true;
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(42));
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        b.setLayoutParams(lp);
        return b;
    }

    private TextView chip(String text, int bg, int fg) {
        TextView chip = label(text, 12, true);
        chip.setTextColor(fg);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(5), dp(10), dp(5));
        chip.setBackground(rounded(bg, dp(14), 0, bg));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(0, dp(6), 0, dp(2));
        chip.setLayoutParams(lp);
        return chip;
    }

    private void animateIn(View view) {
        view.setAlpha(0f);
        view.setTranslationY(dp(8));
        view.post(() -> view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start());
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

    private GradientDrawable glassPanel(int radius) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.argb(210, 255, 255, 255),
                        Color.argb(132, 228, 244, 252)
                }
        );
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), Color.argb(230, 255, 255, 255));
        return drawable;
    }

    private GradientDrawable glassInput(boolean focused) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        focused ? Color.argb(236, 255, 255, 255) : Color.argb(150, 255, 255, 255),
                        focused ? Color.argb(210, 238, 249, 255) : Color.argb(108, 226, 242, 250)
                }
        );
        drawable.setCornerRadius(dp(10));
        drawable.setStroke(dp(1), focused ? PRIMARY : Color.argb(210, 255, 255, 255));
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

    private List<String> murshidabadBlocks() {
        return list(
                "BELDANGA I",
                "BELDANGA II",
                "BERHAMPUR",
                "BHAGAWANGOLA I",
                "BHAGAWANGOLA II",
                "BHARATPUR I",
                "BHARATPUR II",
                "BURWAN",
                "DOMKAL",
                "FARAKKA",
                "HARIHARPARA",
                "JALANGI",
                "KANDI",
                "KHARGRAM",
                "LALGOLA",
                "MURSHIDABAD-JIAGANJ",
                "NABAGRAM",
                "NOWDA",
                "RAGHUNATHGANJ I",
                "RAGHUNATHGANJ II",
                "RANINAGAR I",
                "RANINAGAR II",
                "SAGARDIGHI",
                "SAMSHERGANJ",
                "SUTI I",
                "SUTI II"
        );
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

    private String xml(String value) {
        return value(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(currentRole);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int topSafeInset() {
        return dp(32);
    }

    private int bottomSafeInset() {
        return dp(42);
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

    private static final class ReportFilter {
        final String where;
        final String[] args;

        ReportFilter(String where, List<String> args) {
            this.where = where;
            this.args = args.toArray(new String[0]);
        }
    }
}
