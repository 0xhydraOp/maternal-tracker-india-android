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
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

@SuppressLint("SetTextI18n")
public class MainActivity extends Activity {
    private static final int BG_TOP = Color.rgb(202, 235, 255);
    private static final int BG_BOTTOM = Color.rgb(239, 252, 255);
    private static final int SURFACE = Color.argb(204, 255, 255, 255);
    private static final int SURFACE_ALT = Color.argb(160, 255, 255, 255);
    private static final int SURFACE_WARM = Color.rgb(255, 252, 247);
    private static final int PRIMARY = Color.rgb(7, 84, 117);
    private static final int PRIMARY_DARK = Color.rgb(7, 59, 92);
    private static final int PRIMARY_SOFT = Color.rgb(231, 242, 245);
    private static final int ACCENT = Color.rgb(0, 137, 123);
    private static final int WARNING = Color.rgb(183, 110, 0);
    private static final int URGENT = Color.rgb(180, 35, 24);
    private static final int SLATE = Color.rgb(71, 85, 105);
    private static final int TEXT = Color.rgb(23, 43, 58);
    private static final int MUTED = Color.rgb(100, 116, 139);
    private static final int BORDER = Color.argb(210, 255, 255, 255);
    private static final int ALERT_INFO_BG = Color.rgb(237, 250, 247);
    private static final int ALERT_WARN_BG = Color.rgb(255, 248, 237);
    private static final int SPACE_XS = 4;
    private static final int SPACE_SM = 8;
    private static final int SPACE_MD = 10;
    private static final int SPACE_LG = 14;
    private static final int CARD_RADIUS = 12;
    private static final int CARD_GAP = 8;
    private static final int BUTTON_RADIUS = 10;
    private static final int BUTTON_HEIGHT = 42;
    private static final int CHIP_RADIUS = 14;
    private static final int CHIP_PAD_X = 10;
    private static final int CHIP_PAD_Y = 5;
    private static final int SECTION_ACCENT_WIDTH = 4;
    private static final int SECTION_ACCENT_HEIGHT = 22;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DASHBOARD_CLOCK_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy | hh:mm a");
    private static final String HOSPITAL_NAME = "BLUE BIRD A GENERAL HOSPITAL";
    private static final String APP_NAME = "Maternal Care Registry";
    private static final String DEFAULT_STATE = "West Bengal";
    private static final String DEFAULT_DISTRICT = "MURSHIDABAD";
    private static final int REQ_EXPORT_EXCEL = 501;
    private static final int REQ_EXPORT_PDF = 502;
    private static final int REQ_EXPORT_BACKUP = 503;
    private static final String SCHEDULED_WHERE = "scheduled_delivery_date IS NOT NULL AND scheduled_delivery_date != ''";
    private static final String SCHEDULED_PENDING_WHERE = SCHEDULED_WHERE + " AND record_locked = 0 AND (scheduled_delivery_called_at IS NULL OR scheduled_delivery_called_at = '')";
    private static final String SCHEDULED_WEEK_WHERE = SCHEDULED_PENDING_WHERE + " AND scheduled_delivery_date BETWEEN ? AND ?";
    private static final String SCHEDULED_CALL_PENDING_WHERE = SCHEDULED_PENDING_WHERE + " AND scheduled_delivery_date >= ?";
    private static final String SCHEDULED_COMPLETION_DUE_WHERE = SCHEDULED_WHERE + " AND scheduled_delivery_date < ? AND record_locked = 0";
    private static final String FOLLOWUP_WEEK_WHERE = "record_locked = 0 AND (((visit2 IS NOT NULL AND visit2 != '') AND (visit3 IS NULL OR visit3 = '') AND (final_visit IS NULL OR final_visit = '') AND visit2 <= ?) OR ((visit3 IS NOT NULL AND visit3 != '') AND (final_visit IS NULL OR final_visit = '') AND visit3 <= ?) OR ((final_visit IS NOT NULL AND final_visit != '') AND final_visit <= ?))";
    private static final String UPDATE_API_URL = "https://api.github.com/repos/0xhydraOp/maternal-tracker-india-android/releases/latest";
    private static final String UPDATE_RELEASES_URL = "https://github.com/0xhydraOp/maternal-tracker-india-android/releases/latest";

    private MaternalDbHelper db;
    private FirebaseGateway firebase;
    private LinearLayout root;
    private LinearLayout content;
    private LinearLayout bottomNav;
    private LinearLayout profileMenu;
    private TextView status;
    private TextView headerTitle;
    private TextView syncBadge;
    private TextView profileUserLabel;
    private TextView profileRoleBadge;
    private TextView profileSyncBadge;
    private TextView backButton;
    private TextView dashboardClock;
    private Runnable dashboardClockTicker;
    private ScrollView patientFormScroll;
    private String currentUser = "";
    private String currentRole = "";
    private String currentPage = "Dashboard";
    private String lastSyncText = "Not synced yet";

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
    private EditText scheduledDeliveryDate;
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
        LocationImporter.importBundledLgd(this, db);
        restoreOrShowLogin();
    }

    @Override
    protected void onDestroy() {
        stopDashboardClock();
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
        body.addView(profileMenu, new LinearLayout.LayoutParams(dp(238), -1));
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
        bottomNav.setBackground(glassPanel(dp(CARD_RADIUS)));
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
                    lastSyncText = "Last synced " + LocalDateTime.now().format(TIME_FMT);
                    if (syncBadge != null) {
                        syncBadge.setText("ONLINE");
                        syncBadge.setBackground(rounded(ACCENT, dp(CHIP_RADIUS), 0, ACCENT));
                    }
                    if ("Dashboard".equals(currentPage)) {
                        showDashboard();
                    }
                    if (status != null) {
                        status.setText("Online sync active | " + patients.size() + " patient(s) cached | " + lastSyncText);
                    }
                });
            }

            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    lastSyncText = "Last sync failed " + LocalDateTime.now().format(TIME_FMT);
                    if (syncBadge != null) {
                        syncBadge.setText("SYNC ERROR");
                        syncBadge.setBackground(rounded(WARNING, dp(CHIP_RADIUS), 0, WARNING));
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
        box.setBackground(gradient(PRIMARY_DARK, ACCENT, dp(CARD_RADIUS)));
        box.setPadding(dp(SPACE_MD), dp(SPACE_SM), dp(SPACE_MD), dp(SPACE_SM));

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
        bottomNav.addView(bottomNavItem("Dashboard", "H", "Home", v -> showDashboard()), new LinearLayout.LayoutParams(0, -1, 1));
        bottomNav.addView(bottomNavItem("Patient Entry", "+", "Entry", v -> showPatientForm(null)), new LinearLayout.LayoutParams(0, -1, 1));
        bottomNav.addView(bottomNavItem("Patient Search", "F", "Search", v -> showPatientList(false)), new LinearLayout.LayoutParams(0, -1, 1));
        bottomNav.addView(bottomNavItem("Reports", "R", "Reports", v -> showReports()), new LinearLayout.LayoutParams(0, -1, 1));
        if (isAdmin()) {
            bottomNav.addView(bottomNavItem("Administration", "A", "Admin", v -> showAdmin()), new LinearLayout.LayoutParams(0, -1, 1));
        }
    }

    private LinearLayout profileMenu() {
        LinearLayout menu = card();
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD));
        menu.addView(profileHeader());
        menu.addView(menuGroupTitle("Patient Work"));
        menu.addView(menuItem("+", "New Patient", v -> showPatientForm(null)));
        menu.addView(menuItem("Find", "Search Patients", v -> showPatientList(false)));
        menu.addView(menuGroupTitle("Reports & Export"));
        menu.addView(menuItem("Rpt", "Reports", v -> showReports()));
        menu.addView(menuItem("XLS", "Export Center", v -> showExportCenter()));
        menu.addView(menuGroupTitle("System"));
        menu.addView(menuItem("Upd", "Check for Updates", v -> checkForAppUpdate()));
        if (isAdmin()) {
            menu.addView(menuItem("Admin", "Administration", v -> showAdmin()));
            menu.addView(menuItem("Backup", "Backup Manager", v -> showBackup()));
        }
        menu.addView(menuGroupTitle("Session"));
        menu.addView(menuItem("Exit", "Sign Out", v -> {
            firebase.signOut();
            currentUser = "";
            currentRole = "";
            showLogin();
        }));
        return menu;
    }

    private View profileHeader() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD));
        box.setBackground(gradient(PRIMARY_DARK, ACCENT, dp(CARD_RADIUS)));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = label("BBH", 13, true);
        avatar.setTextColor(PRIMARY_DARK);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(rounded(Color.WHITE, dp(18), 0, Color.WHITE));
        top.addView(avatar, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout identity = new LinearLayout(this);
        identity.setOrientation(LinearLayout.VERTICAL);
        identity.setPadding(dp(SPACE_SM), 0, 0, 0);
        TextView title = label("Blue Bird Profile", 14, true);
        title.setTextColor(Color.WHITE);
        profileUserLabel = label(value(currentUser), 11, false);
        profileUserLabel.setTextColor(Color.argb(220, 255, 255, 255));
        profileUserLabel.setSingleLine(true);
        identity.addView(title);
        identity.addView(profileUserLabel);
        top.addView(identity, new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(top);

        LinearLayout badges = new LinearLayout(this);
        badges.setOrientation(LinearLayout.HORIZONTAL);
        badges.setPadding(0, dp(SPACE_SM), 0, 0);
        profileRoleBadge = chip(value(currentRole), Color.argb(55, 255, 255, 255), Color.WHITE);
        profileSyncBadge = chip(currentSyncText(), ACCENT, Color.WHITE);
        badges.addView(profileRoleBadge);
        badges.addView(profileSyncBadge);
        box.addView(badges);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(SPACE_SM));
        box.setLayoutParams(lp);
        return box;
    }

    private View menuGroupTitle(String title) {
        TextView label = label(title, 10, true);
        label.setTextColor(SLATE);
        label.setPadding(dp(4), dp(SPACE_SM), 0, dp(3));
        return label;
    }

    private View menuItem(String symbol, String text, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(8), dp(7), dp(8), dp(7));
        item.setBackground(rounded(Color.argb(92, 255, 255, 255), dp(CARD_RADIUS), dp(1), Color.argb(170, 255, 255, 255)));
        TextView icon = label(symbol, 11, true);
        icon.setTextColor(PRIMARY);
        icon.setGravity(Gravity.CENTER);
        icon.setSingleLine(true);
        icon.setBackground(rounded(PRIMARY_SOFT, dp(CHIP_RADIUS), dp(1), Color.rgb(184, 207, 225)));
        TextView caption = label(text, 13, true);
        caption.setTextColor(PRIMARY_DARK);
        caption.setPadding(dp(8), 0, 0, 0);
        TextView arrow = label(">", 14, true);
        arrow.setTextColor(MUTED);
        arrow.setGravity(Gravity.CENTER);
        item.addView(icon, new LinearLayout.LayoutParams(dp(54), dp(34)));
        item.addView(caption, new LinearLayout.LayoutParams(0, -2, 1));
        item.addView(arrow, new LinearLayout.LayoutParams(dp(18), dp(34)));
        item.setOnClickListener(v -> {
            closeProfileMenu();
            listener.onClick(v);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(6));
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
            refreshProfileMenuStatus();
            profileMenu.setTranslationX(-dp(36));
            profileMenu.setAlpha(0f);
            profileMenu.animate().translationX(0f).alpha(1f).setDuration(180).start();
        }
    }

    private void refreshProfileMenuStatus() {
        if (profileUserLabel != null) {
            profileUserLabel.setText(value(currentUser));
        }
        if (profileRoleBadge != null) {
            profileRoleBadge.setText(value(currentRole));
        }
        if (profileSyncBadge != null) {
            String text = currentSyncText();
            int color = "SYNC ERROR".equals(text) ? WARNING : ("ONLINE".equals(text) ? ACCENT : PRIMARY);
            profileSyncBadge.setText(text);
            profileSyncBadge.setBackground(rounded(color, dp(CHIP_RADIUS), 0, color));
        }
    }

    private String currentSyncText() {
        return syncBadge == null ? "SYNCING" : value(syncBadge.getText().toString());
    }

    private void closeProfileMenu() {
        if (profileMenu != null && profileMenu.getVisibility() == View.VISIBLE) {
            profileMenu.setVisibility(View.GONE);
        }
    }

    private void setPage(String title) {
        if (!"Dashboard".equals(title)) {
            stopDashboardClock();
        }
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
        int scheduledPending = db.countPatients(appendWhere(scopeWhere, SCHEDULED_CALL_PENDING_WHERE), appendArgs(scopeArgs, LocalDate.now().toString()));
        int scheduledWeek = db.countPatients(appendWhere(scopeWhere, SCHEDULED_WEEK_WHERE), appendArgs(scopeArgs, LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()));
        int scheduledCompletionDue = db.countPatients(appendWhere(scopeWhere, SCHEDULED_COMPLETION_DUE_WHERE), appendArgs(scopeArgs, LocalDate.now().toString()));
        int edd30 = db.countPatients(appendWhere(scopeWhere, "edd_date BETWEEN ? AND ?"), appendArgs(scopeArgs, LocalDate.now().toString(), LocalDate.now().plusDays(30).toString()));
        int locked = db.countPatients(appendWhere(scopeWhere, "record_locked = 1"), scopeArgs);
        int followupWeek = db.countPatients(appendWhere(scopeWhere, FOLLOWUP_WEEK_WHERE), appendArgs(scopeArgs, followupWeekArgs()));
        int todayFocus = scheduledCompletionDue + scheduledWeek + dueWeek + followupWeek;
        box.setPadding(0, 0, 0, dp(8));
        box.addView(dashboardStatusStrip(total));
        box.addView(todayFocusBanner(total, todayFocus));
        if (total == 0) {
            box.addView(zeroDashboardWorkspace());
            box.addView(operationalChecklist());
            box.addView(systemReadinessStrip());
            box.addView(dashboardPreviewState());
            return;
        }
        View priorityStrip = todayPriorityStrip(scheduledCompletionDue, scheduledWeek, dueWeek, scheduledPending, followupWeek, edd30);
        if (priorityStrip != null) {
            box.addView(priorityStrip);
        }
        box.addView(section("Today's Work", todayWorkView(scopeWhere, scopeArgs)));
        box.addView(compactKpiRow(
                compactKpi("Total", total, "Patients", v -> showScopedPatientList(null, null)),
                compactKpi("Due Week", dueWeek, "EDD within 7 days", v -> showScopedPatientList("edd_date BETWEEN ? AND ?", new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()})),
                compactKpi("Urgent", scheduledWeek, "Scheduled 7 days", v -> showScopedPatientList(SCHEDULED_WEEK_WHERE, new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()})),
                compactKpi("Complete", scheduledCompletionDue, "Delivery done", v -> showScopedPatientList(SCHEDULED_COMPLETION_DUE_WHERE, new String[]{LocalDate.now().toString()})),
                compactKpi("Calls", scheduledPending, "Scheduled", v -> showScopedPatientList(SCHEDULED_CALL_PENDING_WHERE, new String[]{LocalDate.now().toString()})),
                compactKpi("Visits", followupWeek, "Due now", v -> showScopedPatientList(FOLLOWUP_WEEK_WHERE, followupWeekArgs())),
                compactKpi("Today", today, "New entries", v -> showScopedPatientList("entry_date = ?", new String[]{LocalDate.now().toString()})),
                compactKpi("Done", locked, "Completed", v -> showScopedPatientList("record_locked = 1", null))
        ));
        box.addView(section("Upcoming EDD", upcomingEddView(scopeWhere, scopeArgs)));
        box.addView(section("Data Quality Alerts", needsAttentionView(scopeWhere, scopeArgs)));
    }

    private View todayPriorityStrip(int scheduledCompletionDue, int scheduledWeek, int dueWeek, int scheduledPending, int followupWeek, int edd30) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(4));
        int shown = 0;
        shown += addFocusCardIf(row, "Complete", scheduledCompletionDue, "Scheduled delivery date passed", URGENT, v -> showScopedPatientList(SCHEDULED_COMPLETION_DUE_WHERE, new String[]{LocalDate.now().toString()}));
        shown += addFocusCardIf(row, "Highest", scheduledWeek, "Scheduled delivery in 7 days", URGENT, v -> showScopedPatientList(SCHEDULED_WEEK_WHERE, new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()}));
        shown += addFocusCardIf(row, "Visits", followupWeek, "Due or next 7 days", WARNING, v -> showScopedPatientList(FOLLOWUP_WEEK_WHERE, followupWeekArgs()));
        shown += addFocusCardIf(row, "EDD Week", dueWeek, "Delivery dates in 7 days", WARNING, v -> showScopedPatientList("edd_date BETWEEN ? AND ?", new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()}));
        shown += addFocusCardIf(row, "Call Pending", scheduledPending, "Scheduled delivery calls", ACCENT, v -> showScopedPatientList(SCHEDULED_CALL_PENDING_WHERE, new String[]{LocalDate.now().toString()}));
        shown += addFocusCardIf(row, "EDD 30", edd30, "Next 30 days", PRIMARY, v -> showScopedPatientList("edd_date BETWEEN ? AND ?", new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(30).toString()}));
        if (shown == 0) {
            return null;
        }
        scroll.addView(row);
        return section("Today at a Glance", scroll);
    }

    private int addFocusCardIf(LinearLayout row, String title, int value, String caption, int color, View.OnClickListener click) {
        if (value <= 0) {
            return 0;
        }
        row.addView(focusCard(title, value, caption, color, click));
        return 1;
    }

    private View focusCard(String title, int value, String caption, int color, View.OnClickListener click) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setOnClickListener(click);
        box.setPadding(dp(10), dp(8), dp(10), dp(8));
        box.setBackground(rounded(Color.argb(76, Color.red(color), Color.green(color), Color.blue(color)), dp(12), dp(1), Color.argb(145, Color.red(color), Color.green(color), Color.blue(color))));
        TextView count = label(String.valueOf(value), 24, true);
        count.setTextColor(color);
        TextView label = label(title, 12, true);
        label.setTextColor(PRIMARY_DARK);
        TextView note = label(caption, 9, false);
        note.setTextColor(MUTED);
        box.addView(count);
        box.addView(label);
        box.addView(note);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(142), -2);
        lp.setMargins(0, 0, dp(7), dp(4));
        box.setLayoutParams(lp);
        animateIn(box);
        return box;
    }

    private View dashboardStatusStrip(int total) {
        stopDashboardClock();
        LinearLayout box = card();
        box.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label(isAdmin() ? "Hospital Overview" : "Your Follow-up Work", 14, true);
        title.setTextColor(PRIMARY_DARK);
        dashboardClock = label("", 11, true);
        dashboardClock.setTextColor(PRIMARY_DARK);
        dashboardClock.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        dashboardClock.setPadding(dp(8), 0, 0, 0);
        TextView context = label(isAdmin() ? "Admin view: all Blue Bird records" : "Staff view: records created by you", 11, false);
        context.setTextColor(MUTED);
        context.setPadding(0, dp(1), 0, dp(3));
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.addView(chip(DEFAULT_DISTRICT, Color.WHITE, PRIMARY_DARK));
        chips.addView(chip(DEFAULT_STATE, Color.WHITE, PRIMARY_DARK));
        chips.addView(chip(total + " records", ACCENT, Color.WHITE));
        chips.addView(chip(lastSyncText, PRIMARY_SOFT, PRIMARY_DARK));
        chipScroll.addView(chips);
        head.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(dashboardClock, new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(head);
        box.addView(context);
        box.addView(chipScroll);
        startDashboardClock();
        return box;
    }

    private void startDashboardClock() {
        if (dashboardClock == null) {
            return;
        }
        dashboardClockTicker = new Runnable() {
            @Override
            public void run() {
                if (dashboardClock == null || !"Dashboard".equals(currentPage)) {
                    return;
                }
                dashboardClock.setText(LocalDateTime.now().format(DASHBOARD_CLOCK_FMT));
                dashboardClock.postDelayed(this, 1000);
            }
        };
        dashboardClockTicker.run();
    }

    private void stopDashboardClock() {
        if (dashboardClock != null && dashboardClockTicker != null) {
            dashboardClock.removeCallbacks(dashboardClockTicker);
        }
        dashboardClockTicker = null;
        dashboardClock = null;
    }

    private View todayFocusBanner(int total, int actionCount) {
        LinearLayout box = card(actionCount > 0 ? ALERT_WARN_BG : ALERT_INFO_BG, 1, actionCount > 0 ? WARNING : ACCENT);
        box.setPadding(dp(SPACE_LG), dp(SPACE_MD), dp(SPACE_LG), dp(SPACE_MD));
        TextView title = label(total == 0 ? "Ready for first patient entry" : (actionCount > 0 ? actionCount + " patient action(s) need attention" : "No priority patient action due"), 15, true);
        title.setTextColor(actionCount > 0 ? WARNING : ACCENT);
        TextView sub = smallText(total == 0 ? "Start the Blue Bird registry with a synced patient record." : (actionCount > 0 ? "Work from the patient cards below: completion due first, then scheduled delivery, visit follow-ups, and EDD reminders." : "No scheduled delivery, overdue follow-up, or EDD is due within 7 days."));
        sub.setTextColor(MUTED);
        box.addView(title);
        box.addView(sub);
        if (total == 0) {
            box.addView(navButton("Add Patient", v -> showPatientForm(null)));
        }
        return box;
    }

    private View zeroDashboardWorkspace() {
        return section("Start Registry",
                compactTwoColumn(
                        zeroActionPanel("Add First Patient", "Create the first synced maternal record for Blue Bird.", "Add Patient", v -> showPatientForm(null), ACCENT),
                        zeroActionPanel("Patient Records", "Search and exports will activate after records are saved.", "Open Search", v -> showPatientList(false), PRIMARY)
                )
        );
    }

    private View zeroActionPanel(String title, String message, String action, View.OnClickListener listener, int color) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD));
        panel.setBackground(rounded(Color.argb(86, Color.red(color), Color.green(color), Color.blue(color)), dp(CARD_RADIUS), dp(1), Color.argb(140, Color.red(color), Color.green(color), Color.blue(color))));
        TextView heading = label(title, 14, true);
        heading.setTextColor(color);
        TextView body = smallText(message);
        body.setTextColor(MUTED);
        panel.addView(heading);
        panel.addView(body);
        panel.addView(navButton(action, listener));
        return panel;
    }

    private View operationalChecklist() {
        return section("Operational Checklist",
                checklistItem("Firebase sync", "Online listener active for hospital records", ACCENT),
                checklistItem("Admin account", isAdmin() ? "Current login has administration access" : "Current login has staff access", isAdmin() ? ACCENT : PRIMARY),
                checklistItem("Murshidabad blocks", "District and block selection is ready", ACCENT),
                checklistItem("Export system", "Excel, PDF, CSV, and backup flows are available", ACCENT)
        );
    }

    private View checklistItem(String title, String detail, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(5), 0, dp(5));
        TextView mark = label("OK", 11, true);
        mark.setTextColor(Color.WHITE);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(rounded(color, dp(14), 0, color));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(9), 0, 0, 0);
        TextView name = label(title, 13, true);
        name.setTextColor(PRIMARY_DARK);
        TextView desc = smallText(detail);
        desc.setTextColor(MUTED);
        copy.addView(name);
        copy.addView(desc);
        row.addView(mark, new LinearLayout.LayoutParams(dp(28), dp(28)));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private View systemReadinessStrip() {
        return section("System Readiness",
                readinessRow(
                        readinessPill("Sync", value(syncBadge == null ? "SYNCING" : syncBadge.getText().toString()), ACCENT),
                        readinessPill("Role", value(currentRole), PRIMARY),
                        readinessPill("Location", DEFAULT_DISTRICT, SLATE),
                        readinessPill("Export", "Ready", ACCENT)
                )
        );
    }

    private View readinessRow(View... pills) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (View pill : pills) {
            row.addView(pill);
        }
        scroll.addView(row);
        return scroll;
    }

    private View readinessPill(String title, String value, int color) {
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.VERTICAL);
        pill.setPadding(dp(SPACE_MD), dp(SPACE_SM), dp(SPACE_MD), dp(SPACE_SM));
        pill.setBackground(rounded(Color.argb(72, Color.red(color), Color.green(color), Color.blue(color)), dp(CHIP_RADIUS), dp(1), Color.argb(130, Color.red(color), Color.green(color), Color.blue(color))));
        TextView t = label(title, 10, true);
        t.setTextColor(MUTED);
        TextView v = label(value, 12, true);
        v.setTextColor(color);
        pill.addView(t);
        pill.addView(v);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(118), -2);
        lp.setMargins(0, 0, dp(7), 0);
        pill.setLayoutParams(lp);
        return pill;
    }

    private View dashboardPreviewState() {
        return section("Dashboard Preview",
                compactKpiRow(
                        previewMetric("EDD Week", "0", "Waiting for records"),
                        previewMetric("Pending", "0", "No follow-up yet"),
                        previewMetric("Attention", "0", "No flags yet"),
                        previewMetric("Recent", "0", "No patients yet")
                )
        );
    }

    private View previewMetric(String title, String value, String caption) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(9), dp(7), dp(9), dp(7));
        box.setBackground(rounded(Color.argb(118, 255, 255, 255), dp(10), dp(1), BORDER));
        TextView number = label(value, 22, true);
        number.setTextColor(MUTED);
        TextView label = label(title, 11, true);
        label.setTextColor(PRIMARY_DARK);
        TextView note = label(caption, 9, false);
        note.setTextColor(MUTED);
        box.addView(number);
        box.addView(label);
        box.addView(note);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(132), -2);
        lp.setMargins(0, 0, dp(7), 0);
        box.setLayoutParams(lp);
        return box;
    }

    private View todayWorkView(String scopeWhere, String[] scopeArgs) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        java.util.Set<String> seenPatientIds = new java.util.HashSet<>();
        int shown = 0;
        shown = addTodayWorkPatients(
                list,
                db.listPatients("", appendWhere(scopeWhere, SCHEDULED_COMPLETION_DUE_WHERE), appendArgs(scopeArgs, LocalDate.now().toString())),
                shown,
                seenPatientIds,
                "Completion due",
                "Scheduled delivery date has passed",
                URGENT,
                "Mark Completed"
        );
        shown = addTodayWorkPatients(
                list,
                db.listPatients("", appendWhere(scopeWhere, SCHEDULED_WEEK_WHERE), appendArgs(scopeArgs, LocalDate.now().toString(), LocalDate.now().plusDays(7).toString())),
                shown,
                seenPatientIds,
                "Scheduled delivery",
                "Doctor-given date within 7 days",
                URGENT,
                "Call"
        );
        shown = addTodayWorkPatients(
                list,
                db.listPatients("", appendWhere(scopeWhere, FOLLOWUP_WEEK_WHERE), appendArgs(scopeArgs, followupWeekArgs())),
                shown,
                seenPatientIds,
                "Visit follow-up",
                "Planned visit due or within 7 days",
                WARNING,
                "Update Visits"
        );
        shown = addTodayWorkPatients(
                list,
                db.listPatients("", appendWhere(scopeWhere, "edd_date BETWEEN ? AND ?"), appendArgs(scopeArgs, LocalDate.now().toString(), LocalDate.now().plusDays(7).toString())),
                shown,
                seenPatientIds,
                "EDD this week",
                "Expected delivery date within 7 days",
                WARNING,
                "Open Record"
        );
        if (shown == 0) {
            list.addView(emptyState("No priority patient action", "Scheduled delivery, visit follow-up, and EDD alerts within 7 days will appear here."));
        } else {
            list.addView(scrollingActions(
                    navButton("Scheduled", v -> showScopedPatientList(SCHEDULED_WEEK_WHERE, new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()})),
                    navButton("Follow-ups", v -> showScopedPatientList(FOLLOWUP_WEEK_WHERE, followupWeekArgs())),
                    navButton("EDD Week", v -> showScopedPatientList("edd_date BETWEEN ? AND ?", new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()}))
            ));
        }
        return list;
    }

    private int addTodayWorkPatients(LinearLayout list, List<Patient> patients, int shown, java.util.Set<String> seenPatientIds, String badge, String reason, int color, String primaryAction) {
        if (patients == null || patients.isEmpty() || shown >= 5) {
            return shown;
        }
        for (Patient p : patients) {
            if (shown >= 5) {
                break;
            }
            String patientKey = value(p.patientId);
            if (!seenPatientIds.add(patientKey)) {
                continue;
            }
            list.addView(todayWorkPatientCard(p, badge, reason, color, primaryAction));
            shown++;
        }
        return shown;
    }

    private View todayWorkPatientCard(Patient p, String badge, String reason, int color, String primaryAction) {
        LinearLayout item = card(Color.argb(66, Color.red(color), Color.green(color), Color.blue(color)), 1, color);
        item.setPadding(dp(SPACE_MD), dp(SPACE_SM), dp(SPACE_MD), dp(SPACE_SM));
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView name = label(value(p.patientName), 14, true);
        name.setTextColor(PRIMARY_DARK);
        TextView meta = smallText(value(p.villageName) + " | " + value(p.mobileNumber));
        meta.setTextColor(MUTED);
        copy.addView(name);
        copy.addView(meta);
        head.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(chip(badge, color, Color.WHITE));
        item.addView(head);
        item.addView(smallText(reason + " | EDD " + value(p.eddDate)));
        List<View> actions = new java.util.ArrayList<>();
        if ("Mark Completed".equals(primaryAction)) {
            actions.add(button("Mark Completed", v -> confirmScheduledDeliveryCompleted(db.getPatient(p.id))));
            actions.add(button("Call", v -> callPatient(db.getPatient(p.id))));
        } else if ("Update Visits".equals(primaryAction)) {
            actions.add(button("Update Visits", v -> showPatientForm(db.getPatient(p.id), true)));
            actions.add(button("Call", v -> callPatient(db.getPatient(p.id))));
        } else if ("Call".equals(primaryAction)) {
            actions.add(button("Call", v -> callPatient(db.getPatient(p.id))));
            actions.add(button("Open Record", v -> showPatientDetail(db.getPatient(p.id), false)));
        } else {
            actions.add(button("Open Record", v -> showPatientDetail(db.getPatient(p.id), false)));
            actions.add(button("Call", v -> callPatient(db.getPatient(p.id))));
        }
        actions.add(navButton("Details", v -> showPatientDetail(db.getPatient(p.id), false)));
        item.addView(scrollingActions(actions.toArray(new View[0])));
        return item;
    }

    private View needsAttentionView(String scopeWhere, String[] scopeArgs) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int missingDoctor = db.countPatients(appendWhere(scopeWhere, "doctor_name IS NULL OR doctor_name = ''"), scopeArgs);
        int invalidMobile = db.countPatients(appendWhere(scopeWhere, "mobile_number IS NULL OR length(trim(mobile_number)) != 10"), scopeArgs);
        int totalAttention = missingDoctor + invalidMobile;
        if (totalAttention == 0) {
            list.addView(emptyState("No data quality alerts", "Doctor names and mobile numbers look complete."));
            return list;
        }
        list.addView(attentionItem("Missing doctor", missingDoctor, "Review", SLATE, "doctor_name IS NULL OR doctor_name = ''", null));
        list.addView(attentionItem("Mobile needs review", invalidMobile, "Review", SLATE, "mobile_number IS NULL OR length(trim(mobile_number)) != 10", null));
        return list;
    }

    private View attentionItem(String title, int count, String severity, int color, String where, String[] args) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(9), dp(7), dp(9), dp(7));
        row.setBackground(rounded(Color.argb(count > 0 ? 72 : 34, Color.red(color), Color.green(color), Color.blue(color)), dp(CARD_RADIUS), dp(1), Color.argb(120, Color.red(color), Color.green(color), Color.blue(color))));
        row.setOnClickListener(v -> showScopedPatientList(where, args));
        TextView badge = chip(severity, count > 0 ? color : PRIMARY_SOFT, count > 0 ? Color.WHITE : MUTED);
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(9), 0, 0, 0);
        TextView name = label(title, 13, true);
        name.setTextColor(count > 0 ? color : MUTED);
        TextView detail = smallText(count + " record(s)");
        detail.setTextColor(MUTED);
        copy.addView(name);
        copy.addView(detail);
        row.addView(badge);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(lp);
        return row;
    }

    private View upcomingEddView(String where, String[] args) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        List<String[]> rows = db.upcomingEddRows(where, args, 3);
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
            item.addView(button("Call", v -> callPatient(db.getPatientByPatientId(row[0]))));
            list.addView(item);
        }
        list.addView(navButton("View EDD 30 Days", v -> showScopedPatientList("edd_date BETWEEN ? AND ?", new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(30).toString()})));
        return list;
    }

    private View scheduledDeliveryView(String where, String[] args) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        List<String[]> rows = db.scheduledDeliveryRows(appendWhere(where, SCHEDULED_CALL_PENDING_WHERE), appendArgs(args, LocalDate.now().toString()), 6);
        if (rows.isEmpty()) {
            list.addView(emptyState("No scheduled delivery dates", "Doctor-given delivery dates will appear here when entered on patient records."));
            return list;
        }
        for (String[] row : rows) {
            LinearLayout item = card();
            item.setPadding(dp(SPACE_MD), dp(SPACE_SM), dp(SPACE_MD), dp(SPACE_SM));
            item.addView(label(value(row[1]), 14, true));
            item.addView(smallText("Scheduled: " + value(row[2]) + " | Village: " + value(row[4])));
            item.addView(smallText("Mobile: " + value(row[3])));
            item.addView(chip("Call pending", WARNING, Color.WHITE));
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.addView(button("Call Mobile", v -> callPatient(db.getPatientByPatientId(row[0]))));
            actions.addView(button("Open Record", v -> showPatientDetail(db.getPatientByPatientId(row[0]), false)));
            item.addView(actions);
            list.addView(item);
        }
        list.addView(navButton("View All Scheduled", v -> showScopedPatientList(SCHEDULED_WHERE, null)));
        return list;
    }

    private View scheduledCompletionDueView(String where, String[] args) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        List<Patient> patients = db.listPatients("", appendWhere(where, SCHEDULED_COMPLETION_DUE_WHERE), appendArgs(args, LocalDate.now().toString()));
        if (patients.isEmpty()) {
            list.addView(emptyState("No scheduled delivery completion pending", "Patients move here only after the scheduled delivery date passes."));
            return list;
        }
        int limit = Math.min(6, patients.size());
        for (int i = 0; i < limit; i++) {
            Patient p = patients.get(i);
            LinearLayout item = card(ALERT_WARN_BG, 1, WARNING);
            item.setPadding(dp(SPACE_MD), dp(SPACE_SM), dp(SPACE_MD), dp(SPACE_SM));
            item.addView(label(value(p.patientName), 14, true));
            item.addView(smallText("Scheduled delivery date passed: " + value(p.scheduledDeliveryDate)));
            item.addView(smallText("Mobile: " + value(p.mobileNumber) + " | Village: " + value(p.villageName)));
            item.addView(chip("Complete this patient record", WARNING, Color.WHITE));
            item.addView(scrollingActions(
                    button("Mark Completed", v -> confirmScheduledDeliveryCompleted(db.getPatient(p.id))),
                    button("Call", v -> callPatient(db.getPatient(p.id))),
                    button("Open Record", v -> showPatientDetail(db.getPatient(p.id), false))
            ));
            list.addView(item);
        }
        list.addView(navButton("View Completion Due", v -> showScopedPatientList(SCHEDULED_COMPLETION_DUE_WHERE, new String[]{LocalDate.now().toString()})));
        return list;
    }

    private View followupWeekView(String where, String[] args) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        List<Patient> patients = db.listPatients("", appendWhere(where, FOLLOWUP_WEEK_WHERE), appendArgs(args, followupWeekArgs()));
        if (patients.isEmpty()) {
            list.addView(emptyState("No visit follow-ups due", "Planned 2nd, 3rd, or final visit dates appear here when they are overdue or due within 7 days."));
            return list;
        }
        int limit = Math.min(6, patients.size());
        for (int i = 0; i < limit; i++) {
            Patient p = patients.get(i);
            String[] next = nextFollowupVisit(p);
            LinearLayout item = card(ALERT_WARN_BG, 1, WARNING);
            item.setPadding(dp(SPACE_MD), dp(SPACE_SM), dp(SPACE_MD), dp(SPACE_SM));
            item.addView(label(value(p.patientName), 14, true));
            item.addView(smallText(value(next[0]) + ": " + value(next[1]) + " | Village: " + value(p.villageName)));
            item.addView(smallText("Mobile: " + value(p.mobileNumber)));
            item.addView(chip("Visit follow-up due", WARNING, Color.WHITE));
            item.addView(scrollingActions(
                    button("Call", v -> callPatient(db.getPatient(p.id))),
                    button("Update Visits", v -> showPatientForm(db.getPatient(p.id), true)),
                    button("Open Record", v -> showPatientDetail(db.getPatient(p.id), false))
            ));
            list.addView(item);
        }
        list.addView(navButton("View Visit Follow-ups", v -> showScopedPatientList(FOLLOWUP_WEEK_WHERE, followupWeekArgs())));
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
        box.setBackground(glassPanel(dp(CARD_RADIUS)));
        box.setPadding(dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD));
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
        showPatientForm(patient, false);
    }

    private void showPatientForm(Patient patient, boolean jumpToVisits) {
        setPage(patient == null ? "Patient Entry" : "Edit Patient");
        editingPatient = patient;
        selectedStateCode = null;
        selectedDistrictCode = null;
        selectedSubdistrictCode = null;
        selectedLocalBodyCode = null;
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        content.addView(screen, new LinearLayout.LayoutParams(-1, -1));
        ScrollView scroll = new ScrollView(this);
        patientFormScroll = scroll;
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(0, 0, 0, dp(8));
        scroll.addView(form);
        screen.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

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
        localBody.setInputType(InputType.TYPE_NULL);
        localBody.setKeyListener(null);
        localBody.setFocusable(false);
        localBody.setOnClickListener(v -> localBody.showDropDown());
        ward = auto(db.listWards(selectedLocalBodyCode));
        village = auto(list());
        village.setHint("Type village name");
        village.setThreshold(Integer.MAX_VALUE);
        lmpDate = input(patient == null ? LocalDate.now().toString() : value(patient.lmpDate));
        eddDate = input(patient == null ? LocalDate.now().plusDays(280).toString() : value(patient.eddDate));
        scheduledDeliveryDate = input(value(patient == null ? null : patient.scheduledDeliveryDate));
        motivator = auto(db.listNames("custom_motivators"));
        doctor = auto(db.listNames("custom_doctors"));
        lmpDate.setHint("YYYY-MM-DD");
        eddDate.setHint("YYYY-MM-DD");
        scheduledDeliveryDate.setHint("Optional YYYY-MM-DD");
        motivator.setHint("Optional");
        doctor.setHint("Doctor name");
        doctor.setThreshold(Integer.MAX_VALUE);
        visit1 = readOnlyInput(patient == null ? LocalDate.now().toString() : value(patient.visit1));
        visit2 = input(value(patient == null ? null : patient.visit2));
        visit3 = input(value(patient == null ? null : patient.visit3));
        finalVisit = input(value(patient == null ? null : patient.finalVisit));
        visit2.setHint("YYYY-MM-DD");
        visit3.setHint("YYYY-MM-DD");
        finalVisit.setHint("YYYY-MM-DD");
        attachDatePicker(lmpDate);
        attachDatePicker(scheduledDeliveryDate);
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
        addPatientValidationWatchers();
        subdistrict.setOnItemClickListener((parent, view, position, id) -> localBody.setText(text(subdistrict), false));
        localBody.setOnItemClickListener((parent, view, position, id) -> {
            subdistrict.setText(text(localBody), false);
            localBody.clearFocus();
        });

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
                row("Scheduled Delivery Date", scheduledDeliveryDate),
                smallText("Optional doctor-given delivery date. Changing it resets the call reminder."),
                row("Entry / 1st Visit *", visit1)
        ));
        form.addView(formStep("4", "Visit Tracking", true,
                row("2nd Visit", visit2),
                row("3rd Visit", visit3),
                row("Final Visit", finalVisit)
        ));

        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(dp(SPACE_MD), dp(SPACE_SM), dp(SPACE_MD), dp(SPACE_SM));
        actions.setGravity(Gravity.END);
        actions.setBackground(glassPanel(dp(12)));
        actions.addView(button("Clear", v -> showPatientForm(null)));
        savePatientButton = button(patient == null ? "Save Patient" : "Update Patient", v -> savePatient());
        savePatientButton.setEnabled(!lockedForStaff);
        actions.addView(savePatientButton);
        screen.addView(actions, new LinearLayout.LayoutParams(-1, -2));
        if (lockedForStaff) {
            status.setText("This record is locked after final visit. Admin can unlock it.");
        } else if (jumpToVisits) {
            status.setText("Update visit dates for the existing patient record.");
            TextView nextVisitField = empty(value(patient.visit2)) ? visit2 :
                    (empty(value(patient.visit3)) ? visit3 : finalVisit);
            scrollPatientFieldIntoView(nextVisitField);
        }
    }

    private void addPatientValidationWatchers() {
        TextView[] fields = {patientName, mobile, localBody, village, lmpDate, eddDate, scheduledDeliveryDate, doctor, visit2, visit3, finalVisit};
        for (TextView field : fields) {
            field.addTextChangedListener(simpleWatcher(s -> {
                field.setError(null);
                if (status != null) {
                    status.setText("");
                }
            }));
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
        p.scheduledDeliveryDate = text(scheduledDeliveryDate);
        if (empty(p.scheduledDeliveryDate)) {
            p.scheduledDeliveryCalledAt = "";
            p.scheduledDeliveryCalledBy = "";
        } else if (old != null && !value(old.scheduledDeliveryDate).equals(value(p.scheduledDeliveryDate))) {
            p.scheduledDeliveryCalledAt = "";
            p.scheduledDeliveryCalledBy = "";
        }
        p.motivatorName = text(motivator);
        p.doctorName = text(doctor);
        p.visit1 = text(visit1);
        p.visit2 = text(visit2);
        p.visit3 = text(visit3);
        p.finalVisit = text(finalVisit);
        p.entryDate = p.entryDate == null ? LocalDate.now().toString() : p.entryDate;
        p.createdBy = old == null || empty(old.createdBy) ? currentUser : old.createdBy;
        p.updatedBy = currentUser;
        p.remarks = "";

        String validation = validatePatient(p);
        if (validation != null) {
            showPatientValidationError(p, validation);
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
        if (empty(p.finalVisit) || !PatientRules.validDate(p.finalVisit) || LocalDate.parse(p.finalVisit).isAfter(LocalDate.now())) {
            return false;
        }
        return old == null || !old.recordLocked;
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

    private void showPatientValidationError(Patient p, String fallback) {
        TextView target = null;
        String message = fallback;
        if (empty(p.patientName)) {
            target = patientName;
            message = "Patient name is required";
        } else if (empty(p.mobileNumber)) {
            target = mobile;
            message = "Mobile number is required";
        } else if (p.mobileNumber.replaceAll("\\D", "").length() != 10) {
            target = mobile;
            message = "Enter a 10 digit mobile number";
        } else if (empty(p.localBodyName)) {
            target = localBody;
            message = "Select block name";
        } else if (empty(p.villageName)) {
            target = village;
            message = "Village name is required";
        } else if (empty(p.lmpDate) || !PatientRules.validDate(p.lmpDate) || LocalDate.parse(p.lmpDate).isAfter(LocalDate.now())) {
            target = lmpDate;
        } else if (empty(p.eddDate) || !PatientRules.validDate(p.eddDate)) {
            target = eddDate;
        } else if (LocalDate.parse(p.eddDate).isBefore(LocalDate.parse(p.lmpDate))) {
            target = eddDate;
            message = "EDD date cannot be before LMP";
        } else if (!PatientRules.validDate(p.scheduledDeliveryDate)) {
            target = scheduledDeliveryDate;
        } else if (!empty(p.scheduledDeliveryDate) && LocalDate.parse(p.scheduledDeliveryDate).isBefore(LocalDate.parse(p.lmpDate))) {
            target = scheduledDeliveryDate;
            message = "Scheduled delivery date cannot be before LMP";
        } else if (empty(p.doctorName)) {
            target = doctor;
            message = "Doctor name is required";
        } else if (LocalDate.parse(p.visit1).isAfter(LocalDate.now())) {
            target = visit1;
            message = "1st visit cannot be in the future";
        } else if (!PatientRules.validDate(p.visit2)) {
            target = visit2;
        } else if (!PatientRules.validDate(p.visit3)) {
            target = visit3;
        } else if (!PatientRules.validDate(p.finalVisit)) {
            target = finalVisit;
        }
        toast(message);
        if (target != null) {
            target.setError(message);
            target.requestFocus();
            scrollPatientFieldIntoView(target);
        }
    }

    private void scrollPatientFieldIntoView(View target) {
        if (patientFormScroll == null || target == null) {
            return;
        }
        patientFormScroll.post(() -> {
            int y = target.getTop();
            View parent = (View) target.getParent();
            while (parent != null && parent != patientFormScroll) {
                y += parent.getTop();
                if (!(parent.getParent() instanceof View)) {
                    break;
                }
                parent = (View) parent.getParent();
            }
            patientFormScroll.smoothScrollTo(0, Math.max(0, y - dp(80)));
        });
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
        db.logChange(p.patientId, "scheduled_delivery_date", old.scheduledDeliveryDate, p.scheduledDeliveryDate, currentUser);
        db.logChange(p.patientId, "scheduled_delivery_called_at", old.scheduledDeliveryCalledAt, p.scheduledDeliveryCalledAt, currentUser);
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
        showPatientList(adminMode, extraWhere, extraArgs, adminMode && isAdmin());
    }

    private void showPatientList(boolean adminMode, String extraWhere, String[] extraArgs, boolean alreadyScoped) {
        setPage(adminMode ? "Patient Management" : "Patient Search");
        boolean fullAccess = adminMode && isAdmin();
        String visibleWhere = fullAccess || alreadyScoped ? extraWhere : scopedWhere(extraWhere);
        String[] visibleArgs = fullAccess || alreadyScoped ? extraArgs : scopedArgs(extraArgs);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        content.addView(page, new LinearLayout.LayoutParams(-1, -1));
        EditText search = input("");
        search.setHint("Search name, mobile, patient ID, village");
        page.addView(searchPanel(search));
        page.addView(scrollingActions(
                navButton("Scheduled", v -> showPatientList(adminMode, appendWhere(visibleWhere, SCHEDULED_WHERE), visibleArgs, true)),
                navButton("Call Pending", v -> showPatientList(adminMode, appendWhere(visibleWhere, SCHEDULED_CALL_PENDING_WHERE), appendArgs(visibleArgs, LocalDate.now().toString()), true)),
                navButton("Visit Follow-ups", v -> showPatientList(adminMode, appendWhere(visibleWhere, FOLLOWUP_WEEK_WHERE), appendArgs(visibleArgs, followupWeekArgs()), true)),
                navButton("EDD 30 Days", v -> showPatientList(adminMode, appendWhere(visibleWhere, "edd_date BETWEEN ? AND ?"), appendArgs(visibleArgs, LocalDate.now().toString(), LocalDate.now().plusDays(30).toString()), true)),
                navButton("Locked", v -> showPatientList(adminMode, appendWhere(visibleWhere, "record_locked = 1"), visibleArgs, true))
        ));
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
        list.addView(searchResultHeader(patients.size()));
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
            LinearLayout card = patientSearchCard(p);
            if (scheduledDeliveryNeedsCompletion(p)) {
                card.addView(chip("Delivery completion required", WARNING, Color.WHITE));
            }
            if (!empty(p.scheduledDeliveryDate)) {
                card.addView(statusLine("Scheduled delivery", value(p.scheduledDeliveryDate), scheduledDeliveryStatusText(p), scheduledDeliveryStatusColor(p)));
            }
            if (p.recordLocked) {
                TextView locked = chip("Locked after final visit", ACCENT, Color.WHITE);
                card.addView(locked);
            } else {
                card.addView(chip("Open", PRIMARY_SOFT, PRIMARY_DARK));
            }
            card.setOnClickListener(v -> showPatientDetail(db.getPatient(p.id), adminMode));
            List<View> rowActions = new java.util.ArrayList<>();
            rowActions.add(button("View", v -> showPatientDetail(db.getPatient(p.id), adminMode)));
            rowActions.add(button("Call", v -> callPatient(db.getPatient(p.id))));
            if (scheduledDeliveryNeedsCompletion(p)) {
                rowActions.add(button("Mark Completed", v -> confirmScheduledDeliveryCompleted(db.getPatient(p.id))));
            }
            if (!p.recordLocked || isAdmin()) {
                rowActions.add(button("Update Visits", v -> showPatientForm(db.getPatient(p.id), true)));
            }
            if (adminMode && isAdmin()) {
                rowActions.add(button("Unlock", v -> {
                    p.recordLocked = false;
                    p.updatedBy = currentUser;
                    firebase.savePatient(p, (unused, error) -> runOnUiThread(() -> {
                        if (error != null) {
                            p.recordLocked = true;
                            toast("Unlock sync failed: " + error.getMessage());
                            return;
                        }
                        db.savePatient(p);
                        db.logActivity("PATIENT_UNLOCK", p.patientId, currentUser);
                        showPatientList(true);
                    }));
                }));
                rowActions.add(button("Delete", v -> confirmDeletePatient(p)));
            }
            card.addView(scrollingActions(rowActions.toArray(new View[0])));
            list.addView(card);
        }
    }

    private View searchResultHeader(int count) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(10), dp(7), dp(10), dp(7));
        box.setBackground(rounded(Color.argb(88, 255, 255, 255), dp(CARD_RADIUS), dp(1), Color.argb(180, 255, 255, 255)));
        TextView title = label("Search Results", 14, true);
        title.setTextColor(PRIMARY_DARK);
        TextView countChip = chip(count + " found", count == 0 ? WARNING : ACCENT, Color.WHITE);
        box.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(countChip);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(7));
        box.setLayoutParams(lp);
        return box;
    }

    private LinearLayout patientSearchCard(Patient p) {
        LinearLayout card = card();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout identity = new LinearLayout(this);
        identity.setOrientation(LinearLayout.VERTICAL);
        TextView name = label(value(p.patientName), 16, true);
        name.setTextColor(PRIMARY_DARK);
        TextView id = label(value(p.patientId), 11, true);
        id.setTextColor(MUTED);
        identity.addView(name);
        identity.addView(id);
        top.addView(identity, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(chip(p.recordLocked ? "Completed" : (scheduledDeliveryNeedsCompletion(p) ? "Complete due" : (empty(p.scheduledDeliveryCalledAt) && !empty(p.scheduledDeliveryDate) ? "Call pending" : "Open")), p.recordLocked ? ACCENT : (scheduledDeliveryNeedsCompletion(p) || (!empty(p.scheduledDeliveryDate) && empty(p.scheduledDeliveryCalledAt)) ? WARNING : PRIMARY_SOFT), p.recordLocked || scheduledDeliveryNeedsCompletion(p) || (!empty(p.scheduledDeliveryDate) && empty(p.scheduledDeliveryCalledAt)) ? Color.WHITE : PRIMARY_DARK));
        card.addView(top);
        card.addView(statusLine("Mobile", value(p.mobileNumber), "Call", PRIMARY));
        card.addView(statusLine("Location", value(p.villageName), value(p.localBodyName), SLATE));
        card.addView(statusLine("Care", "EDD " + value(p.eddDate), "Doctor " + value(p.doctorName), ACCENT));
        return card;
    }

    private View statusLine(String labelText, String valueText, String metaText, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));
        TextView label = label(labelText, 11, true);
        label.setTextColor(color);
        TextView value = label(valueText, 12, false);
        value.setTextColor(TEXT);
        TextView meta = label(metaText, 11, true);
        meta.setTextColor(MUTED);
        meta.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(label, new LinearLayout.LayoutParams(dp(82), -2));
        row.addView(value, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(meta, new LinearLayout.LayoutParams(dp(92), -2));
        return row;
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
        if (!empty(p.scheduledDeliveryDate)) {
            page.addView(section("Scheduled Delivery",
                    smallText("Doctor-given date: " + value(p.scheduledDeliveryDate)),
                    smallText(p.recordLocked ? "Completion status: Completed" : (scheduledDeliveryNeedsCompletion(p) ? "Completion status: Delivery date passed; completion pending" : (empty(p.scheduledDeliveryCalledAt) ? "Notification status: Call pending" : "Notification status: Patient notified"))),
                    smallText(p.recordLocked ? "This patient record is locked in the completed list." : (scheduledDeliveryNeedsCompletion(p) ? "Mark this patient completed after operator confirmation." : (empty(p.scheduledDeliveryCalledAt) ? "Call this patient from the action button below." : "Called " + value(p.scheduledDeliveryCalledAt) + " by " + value(p.scheduledDeliveryCalledBy))))
            ));
        }
        if (scheduledDeliveryNeedsCompletion(p)) {
            page.addView(section("Completion Required",
                    emptyState("Scheduled delivery date has passed", "Operator must mark this patient completed before it moves to the completed list."),
                    button("Mark Completed", v -> confirmScheduledDeliveryCompleted(db.getPatient(p.id)))
            ));
        }
        page.addView(section("Record Trust",
                smallText("Entry date: " + value(p.entryDate)),
                smallText("Created by: " + value(p.createdBy)),
                smallText("Updated by: " + value(p.updatedBy)),
                smallText("Cloud status: " + value(lastSyncText)),
                chip(p.recordLocked ? "Locked after final visit" : "Open for follow-up", p.recordLocked ? ACCENT : WARNING, Color.WHITE)
        ));
        page.addView(section("Visit Timeline", visitTimeline(p)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.addView(navButton("Back to Search", v -> showPatientList(adminMode)));
        if (!p.recordLocked || isAdmin()) {
            actions.addView(button("Update Visit Dates", v -> showPatientForm(db.getPatient(p.id), true)));
            actions.addView(button("Edit Patient", v -> showPatientForm(db.getPatient(p.id))));
        }
        actions.addView(button("Call Patient", v -> callPatient(db.getPatient(p.id))));
        actions.addView(button("Export Single Excel", v -> startPatientExport(p.id, REQ_EXPORT_EXCEL, value(p.patientId) + ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
        actions.addView(button("Export Single PDF", v -> startPatientExport(p.id, REQ_EXPORT_PDF, value(p.patientId) + ".pdf", "application/pdf")));
        if (p.recordLocked) {
            actions.addView(chip("Locked after final visit", ACCENT, Color.WHITE));
        }
        if (adminMode && isAdmin()) {
            actions.addView(button("Unlock", v -> {
                p.recordLocked = false;
                p.updatedBy = currentUser;
                firebase.savePatient(p, (unused, error) -> runOnUiThread(() -> {
                    if (error != null) {
                        p.recordLocked = true;
                        toast("Unlock sync failed: " + error.getMessage());
                        return;
                    }
                    db.savePatient(p);
                    db.logActivity("PATIENT_UNLOCK", p.patientId, currentUser);
                    showPatientDetail(db.getPatient(p.id), true);
                }));
            }));
            actions.addView(button("Delete", v -> confirmDeletePatient(p)));
        }
        page.addView(section("Actions", actions));
    }

    private boolean scheduledDeliveryNeedsCompletion(Patient p) {
        if (p == null || p.recordLocked || empty(p.scheduledDeliveryDate) || !PatientRules.validDate(p.scheduledDeliveryDate)) {
            return false;
        }
        return LocalDate.parse(p.scheduledDeliveryDate).isBefore(LocalDate.now());
    }

    private String scheduledDeliveryStatusText(Patient p) {
        if (p == null) {
            return "";
        }
        if (p.recordLocked) {
            return "Completed";
        }
        if (scheduledDeliveryNeedsCompletion(p)) {
            return "Completion required";
        }
        return empty(p.scheduledDeliveryCalledAt) ? "Call pending" : "Patient notified";
    }

    private int scheduledDeliveryStatusColor(Patient p) {
        if (p != null && (p.recordLocked || (!empty(p.scheduledDeliveryCalledAt) && !scheduledDeliveryNeedsCompletion(p)))) {
            return ACCENT;
        }
        return WARNING;
    }

    private String[] followupWeekArgs() {
        String week = LocalDate.now().plusDays(7).toString();
        return new String[]{week, week, week};
    }

    private String[] nextFollowupVisit(Patient p) {
        LocalDate today = LocalDate.now();
        LocalDate week = today.plusDays(7);
        if (!empty(p.finalVisit) && followupDueBy(p.finalVisit, week)) {
            return new String[]{"Final Visit", p.finalVisit};
        }
        if (empty(p.finalVisit) && !empty(p.visit3) && followupDueBy(p.visit3, week)) {
            return new String[]{"3rd Visit", p.visit3};
        }
        if (empty(p.finalVisit) && empty(p.visit3) && !empty(p.visit2) && followupDueBy(p.visit2, week)) {
            return new String[]{"2nd Visit", p.visit2};
        }
        return new String[]{"Follow-up", ""};
    }

    private boolean followupDueBy(String value, LocalDate week) {
        return !empty(value) && PatientRules.validDate(value) && !LocalDate.parse(value).isAfter(week);
    }

    private void confirmScheduledDeliveryCompleted(Patient p) {
        if (p == null) {
            toast("Patient not found");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Mark Delivery Completed")
                .setMessage("Confirm delivery completed for " + value(p.patientName) + "? This will lock the record and move it to the completed list.")
                .setPositiveButton("Mark Completed", (dialog, which) -> markScheduledDeliveryCompleted(p))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void markScheduledDeliveryCompleted(Patient p) {
        Patient old = db.getPatient(p.id);
        if (old == null) {
            toast("Patient not found");
            return;
        }
        p.finalVisit = empty(p.finalVisit) ? value(p.scheduledDeliveryDate) : p.finalVisit;
        p.recordLocked = true;
        p.updatedBy = currentUser;
        try {
            db.savePatient(p);
            db.logChange(p.patientId, "final_visit", old.finalVisit, p.finalVisit, currentUser);
            db.logChange(p.patientId, "record_locked", old.recordLocked ? "1" : "0", "1", currentUser);
            db.logActivity("SCHEDULED_DELIVERY_COMPLETE", p.patientId, currentUser);
            firebase.savePatient(p, (unused, error) -> runOnUiThread(() -> {
                if (error != null) {
                    db.savePatient(old);
                    toast("Completion sync failed: " + error.getMessage());
                    return;
                }
                toast("Patient marked completed");
                showDashboard();
            }));
        } catch (Exception ex) {
            db.savePatient(old);
            toast("Completion failed: " + ex.getMessage());
        }
    }

    private void callPatient(Patient p) {
        if (p == null) {
            toast("Patient not found");
            return;
        }
        String digits = value(p.mobileNumber).replaceAll("\\D", "");
        if (digits.length() != 10) {
            toast("Valid 10 digit mobile number is required");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + digits));
        try {
            startActivity(intent);
            if (!p.recordLocked && !scheduledDeliveryNeedsCompletion(p) && !empty(p.scheduledDeliveryDate) && empty(p.scheduledDeliveryCalledAt)) {
                p.scheduledDeliveryCalledAt = LocalDateTime.now().toString();
                p.scheduledDeliveryCalledBy = currentUser;
                p.updatedBy = currentUser;
                db.savePatient(p);
                db.logActivity("SCHEDULED_DELIVERY_CALL", p.patientId + " " + digits, currentUser);
                toast("Patient marked notified");
                firebase.savePatient(p, (unused, error) -> runOnUiThread(() -> {
                    if (error != null) {
                        toast("Call marked locally, cloud sync failed: " + error.getMessage());
                    }
                }));
            }
        } catch (Exception ex) {
            toast("Phone dialer unavailable: " + ex.getMessage());
        }
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
        AutoCompleteTextView villageFilter = auto(list());
        AutoCompleteTextView statusFilter = auto(list("All", "Open", "Locked"));
        from.setHint("From YYYY-MM-DD");
        to.setHint("To YYYY-MM-DD");
        villageFilter.setHint("Village");
        statusFilter.setText("All", false);
        attachDatePicker(from);
        attachDatePicker(to);
        filters.addView(compactTwoColumn(row("From", from), row("To", to)));
        filters.addView(compactTwoColumn(row("Village", villageFilter), row("Status", statusFilter)));

        LinearLayout reportBody = new LinearLayout(this);
        reportBody.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(reportBody);

        Runnable[] render = new Runnable[1];
        render[0] = () -> renderReports(reportBody, text(from), text(to), text(villageFilter), text(statusFilter));
        page.addView(section("Report Actions",
                scrollingActions(
                    button("Apply", v -> render[0].run()),
                    button("Search", v -> openFilteredPatientSearch(text(from), text(to), text(villageFilter), text(statusFilter))),
                    button("Excel", v -> startFilteredExport(text(from), text(to), text(villageFilter), text(statusFilter), REQ_EXPORT_EXCEL, "patients.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
                    button("PDF", v -> startFilteredExport(text(from), text(to), text(villageFilter), text(statusFilter), REQ_EXPORT_PDF, "patients.pdf", "application/pdf"))
            )
        ));
        page.addView(section("Quick Report Filters",
                scrollingActions(
                        navButton("Scheduled only", v -> openQuickReportFilter(text(from), text(to), text(villageFilter), text(statusFilter), SCHEDULED_WHERE, null)),
                        navButton("Call pending only", v -> openQuickReportFilter(text(from), text(to), text(villageFilter), text(statusFilter), SCHEDULED_CALL_PENDING_WHERE, new String[]{LocalDate.now().toString()}))
                )
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

        String exportWhere = scopedWhere(null);
        String[] exportArgs = scopedArgs(null);
        int total = db.countPatients(exportWhere, exportArgs);
        int locked = db.countPatients(appendWhere(exportWhere, "record_locked = 1"), exportArgs);
        page.addView(section("Export Records",
                emptyState(total + " records ready", "Save Excel or PDF directly through Android's file picker, then share it from the system sheet."),
                scrollingActions(
                        button("Excel", v -> startExport("", exportWhere, exportArgs, REQ_EXPORT_EXCEL, "blue_bird_patients.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
                        button("PDF", v -> startExport("", exportWhere, exportArgs, REQ_EXPORT_PDF, "blue_bird_patients.pdf", "application/pdf")),
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
                smallText("Use Reports for date, village, or lock-status filtered Excel/PDF exports."),
                smallText("Use Patient Detail when one patient's PDF or Excel file is needed.")
        ));
    }

    private void renderReports(LinearLayout page, String from, String to, String village, String statusName) {
        page.removeAllViews();
        ReportFilter filter = reportWhere(from, to, village, statusName);
        String where = scopedWhere(filter.where);
        String[] args = scopedArgs(filter.args);
        int total = db.countPatients(where, args);
        int locked = db.countPatients(appendWhere(where, "record_locked = 1"), appendArgs(args));
        int edd30 = db.countPatients(appendWhere(where, "edd_date BETWEEN ? AND ?"), appendArgs(args, LocalDate.now().toString(), LocalDate.now().plusDays(30).toString()));
        int scheduled = db.countPatients(appendWhere(where, SCHEDULED_WHERE), args);
        int scheduledPending = db.countPatients(appendWhere(where, SCHEDULED_CALL_PENDING_WHERE), appendArgs(args, LocalDate.now().toString()));
        int scheduledCompletionDue = db.countPatients(appendWhere(where, SCHEDULED_COMPLETION_DUE_WHERE), appendArgs(args, LocalDate.now().toString()));
        int scheduledNotified = db.countPatients(appendWhere(where, SCHEDULED_WHERE + " AND scheduled_delivery_called_at IS NOT NULL AND scheduled_delivery_called_at != ''"), args);
        int followupWeek = db.countPatients(appendWhere(where, FOLLOWUP_WEEK_WHERE), appendArgs(args, followupWeekArgs()));
        int today = db.countPatients(appendWhere(where, "entry_date = ?"), appendArgs(args, LocalDate.now().toString()));
        page.addView(section("Report Overview",
                statGrid(
                        stat("Filtered", total, v -> showPatientList(false, where, args, true)),
                        stat("Today", today, v -> showPatientList(false, appendWhere(where, "entry_date = ?"), appendArgs(args, LocalDate.now().toString()), true)),
                        stat("EDD 30", edd30, v -> showPatientList(false, appendWhere(where, "edd_date BETWEEN ? AND ?"), appendArgs(args, LocalDate.now().toString(), LocalDate.now().plusDays(30).toString()), true)),
                        stat("Scheduled", scheduled, v -> showPatientList(false, appendWhere(where, SCHEDULED_WHERE), args, true)),
                        stat("Call Pending", scheduledPending, v -> showPatientList(false, appendWhere(where, SCHEDULED_CALL_PENDING_WHERE), appendArgs(args, LocalDate.now().toString()), true)),
                        stat("Visit Follow-ups", followupWeek, v -> showPatientList(false, appendWhere(where, FOLLOWUP_WEEK_WHERE), appendArgs(args, followupWeekArgs()), true)),
                        stat("Complete Due", scheduledCompletionDue, v -> showPatientList(false, appendWhere(where, SCHEDULED_COMPLETION_DUE_WHERE), appendArgs(args, LocalDate.now().toString()), true)),
                        stat("Completed", locked, v -> showPatientList(false, appendWhere(where, "record_locked = 1"), args, true))
                )
        ));
        page.addView(section("Active Filters", reportFilterSummary(from, to, village, statusName)));
        page.addView(section("Scheduled Delivery Report",
                progressRow("Notified", scheduledNotified, scheduled, scheduled == 0 ? 0 : Math.round(scheduledNotified * 100f / scheduled)),
                progressRow("Call pending", scheduledPending, scheduled, scheduled == 0 ? 0 : Math.round(scheduledPending * 100f / scheduled)),
                progressRow("Completion due", scheduledCompletionDue, scheduled, scheduled == 0 ? 0 : Math.round(scheduledCompletionDue * 100f / scheduled)),
                scrollingActions(
                        navButton("Scheduled Records", v -> showPatientList(false, appendWhere(where, SCHEDULED_WHERE), args, true)),
                        navButton("Pending Calls", v -> showPatientList(false, appendWhere(where, SCHEDULED_CALL_PENDING_WHERE), appendArgs(args, LocalDate.now().toString()), true)),
                        navButton("Completion Due", v -> showPatientList(false, appendWhere(where, SCHEDULED_COMPLETION_DUE_WHERE), appendArgs(args, LocalDate.now().toString()), true)),
                        button("Export Excel", v -> startExport("", appendWhere(where, SCHEDULED_WHERE), args, REQ_EXPORT_EXCEL, "scheduled_delivery.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
                        button("Export PDF", v -> startExport("", appendWhere(where, SCHEDULED_WHERE), args, REQ_EXPORT_PDF, "scheduled_delivery.pdf", "application/pdf"))
                )
        ));
        page.addView(section("Report Snapshot",
                progressRow("Open records", Math.max(0, total - locked), total, total == 0 ? 0 : Math.round((total - locked) * 100f / total)),
                progressRow("Locked records", locked, total, total == 0 ? 0 : Math.round(locked * 100f / total)),
                progressRow("EDD within 30 days", edd30, total, total == 0 ? 0 : Math.round(edd30 * 100f / total)),
                progressRow("Visit follow-ups due", followupWeek, total, total == 0 ? 0 : Math.round(followupWeek * 100f / total)),
                progressRow("Scheduled delivery", scheduled, total, total == 0 ? 0 : Math.round(scheduled * 100f / total)),
                progressRow("Scheduled calls pending", scheduledPending, scheduled, scheduled == 0 ? 0 : Math.round(scheduledPending * 100f / scheduled)),
                progressRow("Delivery completion due", scheduledCompletionDue, scheduled, scheduled == 0 ? 0 : Math.round(scheduledCompletionDue * 100f / scheduled))
        ));
        if (total == 0) {
            page.addView(section("Report Result", emptyActionState("No records match these filters", "Change the filters or create a patient record first.", "New Patient", v -> showPatientForm(null))));
            return;
        }
        page.addView(section("Scheduled Delivery Access", scheduledDeliveryView(where, args)));
        page.addView(section("Visit Follow-up Access", followupWeekView(where, args)));
        LinearLayout visits = new LinearLayout(this);
        visits.setOrientation(LinearLayout.VERTICAL);
        for (String[] row : db.visitCompletionRows(where, args)) {
            int done = Integer.parseInt(row[1]);
            int pct = total == 0 ? 0 : Math.round(done * 100f / total);
            visits.addView(progressRow(row[0], done, total, pct));
        }
        page.addView(section("Visit Date Tracking", visits));
        page.addView(reportMap("Patients by Village", db.countBy("village_name", where, args)));
        page.addView(reportMap("Monthly Summary", monthlySummary(where, args)));
    }

    private View reportFilterSummary(String from, String to, String village, String statusName) {
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.addView(chip(empty(from) ? "From: any" : "From: " + from, PRIMARY_SOFT, PRIMARY_DARK));
        chips.addView(chip(empty(to) ? "To: any" : "To: " + to, PRIMARY_SOFT, PRIMARY_DARK));
        chips.addView(chip(empty(village) ? "Village: all" : "Village: " + village, PRIMARY_SOFT, PRIMARY_DARK));
        chips.addView(chip(empty(statusName) ? "Status: All" : "Status: " + statusName, PRIMARY_SOFT, PRIMARY_DARK));
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(chips);
        return scroll;
    }

    private ReportFilter reportWhere(String from, String to, String village, String statusName) {
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
        if (!empty(village)) {
            clauses.add("village_name LIKE ?");
            args.add("%" + village + "%");
        }
        if ("Locked".equalsIgnoreCase(value(statusName))) {
            clauses.add("record_locked = 1");
        } else if ("Open".equalsIgnoreCase(value(statusName))) {
            clauses.add("record_locked = 0");
        }
        return new ReportFilter(clauses.isEmpty() ? null : String.join(" AND ", clauses), args);
    }

    private void openFilteredPatientSearch(String from, String to, String village, String statusName) {
        ReportFilter filter = reportWhere(from, to, village, statusName);
        showPatientList(false, filter.where, filter.args);
    }

    private void openQuickReportFilter(String from, String to, String village, String statusName, String extraWhere, String[] extraArgs) {
        ReportFilter filter = reportWhere(from, to, village, statusName);
        showPatientList(false, appendWhere(scopedWhere(filter.where), extraWhere), appendArgs(scopedArgs(filter.args), extraArgs), true);
    }

    private String appendWhere(String where, String extra) {
        return empty(where) ? extra : "(" + where + ") AND (" + extra + ")";
    }

    private String[] appendArgs(String[] base, String... extra) {
        int baseLength = base == null ? 0 : base.length;
        int extraLength = extra == null ? 0 : extra.length;
        String[] out = new String[baseLength + extraLength];
        if (base != null) {
            System.arraycopy(base, 0, out, 0, baseLength);
        }
        if (extra != null) {
            System.arraycopy(extra, 0, out, baseLength, extraLength);
        }
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
        startExport(filter, null, null, requestCode, fileName, mimeType);
    }

    private void startExport(String filter, String where, String[] args, int requestCode, String fileName, String mimeType) {
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

    private void startFilteredExport(String from, String to, String village, String statusName, int requestCode, String fileName, String mimeType) {
        ReportFilter filter = reportWhere(from, to, village, statusName);
        pendingExportFilter = "";
        pendingExportWhere = scopedWhere(filter.where);
        pendingExportArgs = scopedArgs(filter.args);
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
                if (status != null) {
                    status.setText("Excel export saved | " + exportStamp());
                }
                toast("Excel export saved");
            } else if (requestCode == REQ_EXPORT_PDF) {
                writePatientsPdf(out, pendingExportPatients());
                if (status != null) {
                    status.setText("PDF export saved | " + exportStamp());
                }
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
            status.setText("CSV export created | " + exportStamp() + " | " + target.getAbsolutePath());
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
            page.getCanvas().drawText("LMP: " + value(p.lmpDate) + " | EDD: " + value(p.eddDate) + " | Scheduled: " + value(p.scheduledDeliveryDate), margin, y, textPaint);
            y += 14;
            page.getCanvas().drawText("Scheduled call: " + (empty(p.scheduledDeliveryCalledAt) ? "Pending" : "Patient notified " + value(p.scheduledDeliveryCalledAt)), margin, y, textPaint);
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
        page.getCanvas().drawText(exportStamp(), margin, 68, textPaint);
        page.getCanvas().drawLine(margin, 78, 565, 78, textPaint);
        return 98;
    }

    private String[] patientExportHeaders() {
        return new String[]{"Serial", "Entry Date", "Patient Name", "Patient ID", "State", "District", "Block", "Village", "Mobile", "Motivator", "Doctor", "LMP", "EDD", "Scheduled Delivery", "Scheduled Call At", "Scheduled Call By", "1st Visit", "2nd Visit", "3rd Visit", "Final Visit", "Locked", "Exported At", "Exported By"};
    }

    private String[] patientExportValues(Patient p) {
        return new String[]{String.valueOf(p.serialNumber), value(p.entryDate), value(p.patientName), value(p.patientId), value(p.stateName), value(p.districtName), value(p.localBodyName), value(p.villageName), value(p.mobileNumber), value(p.motivatorName), value(p.doctorName), value(p.lmpDate), value(p.eddDate), value(p.scheduledDeliveryDate), value(p.scheduledDeliveryCalledAt), value(p.scheduledDeliveryCalledBy), value(p.visit1), value(p.visit2), value(p.visit3), value(p.finalVisit), p.recordLocked ? "1" : "0", LocalDateTime.now().format(TIME_FMT), value(currentUser)};
    }

    private String exportStamp() {
        return "Exported " + LocalDateTime.now().format(TIME_FMT) + " by " + value(currentUser);
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
        int scheduled = db.countPatients(SCHEDULED_WHERE, null);
        int callPending = db.countPatients(SCHEDULED_CALL_PENDING_WHERE, new String[]{LocalDate.now().toString()});
        int doctors = db.listNames("custom_doctors").size();
        int motivators = db.listNames("custom_motivators").size();
        page.addView(adminHero(total, locked, scheduled, callPending));
        page.addView(section("Patient Control",
                scrollingActions(
                        button("Patient Management", v -> showPatientList(true)),
                        button("Reports", v -> showReports()),
                        button("Scheduled Calls", v -> showPatientList(true, SCHEDULED_CALL_PENDING_WHERE, new String[]{LocalDate.now().toString()})),
                        button("Visit Follow-ups", v -> showPatientList(true, FOLLOWUP_WEEK_WHERE, followupWeekArgs())),
                        button("Export Center", v -> showExportCenter())
                )
        ));
        page.addView(section("Data Control",
                scrollingActions(
                        button("Backup", v -> showBackup()),
                        button("Excel", v -> startExport("", null, null, REQ_EXPORT_EXCEL, "all_patients.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
                        button("PDF", v -> startExport("", null, null, REQ_EXPORT_PDF, "all_patients.pdf", "application/pdf"))
                ),
                smallText("Use exports for hospital reporting and backups for database recovery.")
        ));
        page.addView(section("Access Control",
                usersView(),
                navButton("Add User", v -> addUserDialog())
        ));
        page.addView(section("Reference Lists",
                readinessRow(
                        readinessPill("Doctors", String.valueOf(doctors), PRIMARY),
                        readinessPill("Motivators", String.valueOf(motivators), SLATE)
                ),
                scrollingActions(
                        navButton("Doctor Names", v -> showReferenceDialog("custom_doctors", "Doctor Names")),
                        navButton("Motivator Names", v -> showReferenceDialog("custom_motivators", "Motivator Names"))
                )
        ));
        page.addView(section("App Support",
                readinessRow(
                        readinessPill("Version", BuildConfig.VERSION_NAME, ACCENT),
                        readinessPill("Sync", value(syncBadge == null ? "SYNCING" : syncBadge.getText().toString()), PRIMARY),
                        readinessPill("Role", value(currentRole), SLATE)
                ),
                smallText("Support note: " + lastSyncText),
                scrollingActions(
                        button("Check for Updates", v -> checkForAppUpdate()),
                        button("Open Releases", v -> openUrl(UPDATE_RELEASES_URL))
                )
        ));
        page.addView(collapsibleSection("Audit Trail", false, changeLogView()));
    }

    private View adminHero(int total, int locked, int scheduled, int callPending) {
        return section("Admin Overview",
                compactKpiRow(
                        focusCard("Records", total, "All hospital records", PRIMARY, v -> showPatientList(true)),
                        focusCard("Completed", locked, "Locked patient records", ACCENT, v -> showPatientList(true, "record_locked = 1", null)),
                        focusCard("Scheduled", scheduled, "Doctor-given delivery dates", WARNING, v -> showPatientList(true, SCHEDULED_WHERE, null)),
                        focusCard("Calls", callPending, "Pending scheduled calls", URGENT, v -> showPatientList(true, SCHEDULED_CALL_PENDING_WHERE, new String[]{LocalDate.now().toString()}))
                )
        );
    }

    private void checkForAppUpdate() {
        toast("Checking for update...");
        new Thread(() -> {
            try {
                UpdateInfo update = fetchLatestUpdate();
                runOnUiThread(() -> showUpdateResult(update));
            } catch (Exception ex) {
                runOnUiThread(() -> toast("Update check failed: " + ex.getMessage()));
            }
        }).start();
    }

    private UpdateInfo fetchLatestUpdate() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(UPDATE_API_URL).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "BlueBirdHospital/" + BuildConfig.VERSION_NAME);
        int statusCode = connection.getResponseCode();
        InputStream stream = statusCode >= 200 && statusCode < 300 ? connection.getInputStream() : connection.getErrorStream();
        String body = readStream(stream);
        connection.disconnect();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException("GitHub returned " + statusCode);
        }
        JSONObject json = new JSONObject(body);
        String tag = json.optString("tag_name", "");
        String pageUrl = json.optString("html_url", UPDATE_RELEASES_URL);
        String apkUrl = "";
        JSONArray assets = json.optJSONArray("assets");
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String name = asset.optString("name", "");
                if (name.toLowerCase(java.util.Locale.US).endsWith(".apk")) {
                    apkUrl = asset.optString("browser_download_url", "");
                    break;
                }
            }
        }
        return new UpdateInfo(tag, pageUrl, apkUrl);
    }

    private String readStream(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
        }
        return out.toString();
    }

    private void showUpdateResult(UpdateInfo update) {
        if (empty(update.tag)) {
            toast("No release version found");
            return;
        }
        String latest = update.tag.startsWith("v") ? update.tag.substring(1) : update.tag;
        if (compareVersions(latest, BuildConfig.VERSION_NAME) <= 0) {
            new AlertDialog.Builder(this)
                    .setTitle("App is up to date")
                    .setMessage("Current version: " + BuildConfig.VERSION_NAME + "\nLatest version: " + update.tag)
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Update Available")
                .setMessage("Current version: " + BuildConfig.VERSION_NAME + "\nLatest version: " + update.tag + "\n\nDownload and install the latest APK from the release page.")
                .setPositiveButton("Download APK", (dialog, which) -> openUrl(empty(update.apkUrl) ? update.pageUrl : update.apkUrl))
                .setNegativeButton("Later", null)
                .show();
    }

    private int compareVersions(String left, String right) {
        String[] a = value(left).split("\\.");
        String[] b = value(right).split("\\.");
        int count = Math.max(a.length, b.length);
        for (int i = 0; i < count; i++) {
            int av = i < a.length ? parseVersionPart(a[i]) : 0;
            int bv = i < b.length ? parseVersionPart(b[i]) : 0;
            if (av != bv) {
                return av - bv;
            }
        }
        return 0;
    }

    private int parseVersionPart(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void openUrl(String url) {
        if (empty(url)) {
            toast("Update link unavailable");
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ex) {
            toast("Cannot open link: " + ex.getMessage());
        }
    }

    private static final class UpdateInfo {
        final String tag;
        final String pageUrl;
        final String apkUrl;

        UpdateInfo(String tag, String pageUrl, String apkUrl) {
            this.tag = tag;
            this.pageUrl = pageUrl;
            this.apkUrl = apkUrl;
        }
    }

    private void showReferenceDialog(String table, String title) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(namesView(table));
        box.addView(navButton("Add " + title.replace(" Names", ""), v -> {
            addNameDialog(table, title.replace(" Names", " Name"));
        }));
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(box)
                .setNegativeButton("Close", null)
                .show();
    }

    private View usersView() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.addView(smallText("Loading Firebase roles..."));
        firebase.listRoles((rows, error) -> runOnUiThread(() -> {
            list.removeAllViews();
            list.addView(smallText("Create app users here with email, password, and role. Remove Access revokes the app role; Firebase Auth credentials remain in Firebase."));
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
                LinearLayout badges = new LinearLayout(this);
                badges.setOrientation(LinearLayout.HORIZONTAL);
                badges.addView(chip(user[1], "ADMIN".equalsIgnoreCase(user[1]) ? PRIMARY : SLATE, Color.WHITE));
                if (user[0].equalsIgnoreCase(currentUser)) {
                    badges.addView(chip("Current login", ACCENT, Color.WHITE));
                }
                details.addView(badges);
                row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
                if (!user[0].equalsIgnoreCase(currentUser)) {
                    row.addView(button("Remove Access", v -> confirmRemoveAccess(user[0])));
                }
                list.addView(row);
            }
        }));
        return list;
    }

    private void confirmRemoveAccess(String email) {
        new AlertDialog.Builder(this)
                .setTitle("Revoke App Access")
                .setMessage("Revoke Blue Bird app access for " + email + "? This removes the role record, but does not delete the Firebase Auth credential.")
                .setPositiveButton("Remove", (dialog, which) -> firebase.deleteRole(email, (unused, deleteError) -> runOnUiThread(() -> {
                    if (deleteError != null) {
                        toast("Could not remove access: " + deleteError.getMessage());
                        return;
                    }
                    toast("Access revoked");
                    showAdmin();
                })))
                .setNegativeButton("Cancel", null)
                .show();
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
        accent.setBackground(rounded(ACCENT, dp(SECTION_ACCENT_WIDTH), 0, ACCENT));
        LinearLayout.LayoutParams accentLp = new LinearLayout.LayoutParams(dp(SECTION_ACCENT_WIDTH), dp(SECTION_ACCENT_HEIGHT));
        accentLp.setMargins(0, 0, dp(SPACE_SM), 0);
        head.addView(accent, accentLp);
        TextView heading = label(title, 14, true);
        heading.setTextColor(PRIMARY_DARK);
        head.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        head.setPadding(0, 0, 0, dp(SPACE_SM));
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
        TextView heading = label(title, 14, true);
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
        badge.setBackground(gradient(PRIMARY, ACCENT, dp(CHIP_RADIUS)));
        TextView heading = label(title, 14, true);
        heading.setTextColor(PRIMARY_DARK);
        TextView state = chip("Visit Tracking".equals(title) ? "Optional dates" : ("Pregnancy Dates".equals(title) ? "Required + optional" : "Required"), "Visit Tracking".equals(title) ? PRIMARY_SOFT : ACCENT, "Visit Tracking".equals(title) ? PRIMARY_DARK : Color.WHITE);
        TextView indicator = label(expanded ? "-" : "+", 18, true);
        indicator.setTextColor(PRIMARY);
        indicator.setGravity(Gravity.CENTER);
        head.addView(badge, new LinearLayout.LayoutParams(dp(30), dp(30)));
        heading.setPadding(dp(SPACE_SM), 0, 0, 0);
        head.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(state);
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
        TextView dot = label(done ? "Done" : "Due", 11, true);
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
        row.setPadding(0, dp(SPACE_XS), 0, dp(SPACE_SM));
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
        item.setBackground(rounded(active ? PRIMARY : Color.TRANSPARENT, dp(BUTTON_RADIUS), 0, Color.TRANSPARENT));
        item.setOnClickListener(listener);
        return item;
    }

    private View emptyState(String title, String message) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(SPACE_MD), dp(SPACE_SM), dp(SPACE_MD), dp(SPACE_SM));
        box.setBackground(rounded(Color.argb(70, 255, 255, 255), dp(CARD_RADIUS), dp(1), Color.argb(140, 255, 255, 255)));
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
        box.setPadding(dp(SPACE_MD), dp(SPACE_SM), dp(SPACE_MD), dp(SPACE_SM));
        box.setBackground(rounded(Color.argb(76, 255, 255, 255), dp(CARD_RADIUS), dp(1), Color.argb(150, 255, 255, 255)));
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
        box.setPadding(0, dp(SPACE_XS), 0, dp(SPACE_XS));
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
        row.setPadding(dp(2), dp(SPACE_XS), dp(2), dp(SPACE_XS));
        TextView tv = label(label, 12, true);
        tv.setTextColor(MUTED);
        row.addView(tv);
        row.addView(input, new LinearLayout.LayoutParams(-1, dp(BUTTON_HEIGHT)));
        return row;
    }

    private LinearLayout card() {
        return card(SURFACE, 1, BORDER);
    }

    private LinearLayout card(int color, int strokeWidth, int strokeColor) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        if (color == SURFACE) {
            box.setBackground(glassPanel(dp(CARD_RADIUS)));
        } else {
            int border = strokeWidth == 0 ? 0 : Color.argb(170, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor));
            box.setBackground(rounded(color, dp(CARD_RADIUS), strokeWidth == 0 ? 0 : dp(strokeWidth), border));
        }
        box.setPadding(dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD));
        box.setElevation(dp(4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(CARD_GAP));
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

    private View searchPanel(EditText search) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(SPACE_LG), dp(SPACE_MD), dp(SPACE_LG), dp(SPACE_LG));
        panel.setBackground(prominentPanel());
        panel.setElevation(dp(4));
        LinearLayout.LayoutParams panelLp = new LinearLayout.LayoutParams(-1, -2);
        panelLp.setMargins(0, 0, 0, dp(CARD_GAP));
        panel.setLayoutParams(panelLp);

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("Find Existing Patient", 18, true);
        title.setTextColor(Color.WHITE);
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = label("SEARCH", 11, true);
        badge.setTextColor(Color.WHITE);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(CHIP_PAD_X), dp(SPACE_XS), dp(CHIP_PAD_X), dp(SPACE_XS));
        badge.setBackground(rounded(Color.argb(74, 255, 255, 255), dp(CHIP_RADIUS), dp(1), Color.argb(110, 255, 255, 255)));
        heading.addView(badge);
        panel.addView(heading);

        TextView helper = label("Type patient name or mobile number, then use Update Visits for follow-up dates.", 13, false);
        helper.setTextColor(Color.argb(230, 255, 255, 255));
        helper.setPadding(0, dp(SPACE_XS), 0, dp(SPACE_MD));
        panel.addView(helper);

        LinearLayout searchBox = new LinearLayout(this);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setPadding(dp(SPACE_MD), 0, dp(SPACE_SM), 0);
        searchBox.setBackground(rounded(Color.argb(245, 255, 255, 255), dp(CARD_RADIUS), dp(2), Color.argb(235, 255, 255, 255)));
        searchBox.setClickable(true);
        searchBox.setOnClickListener(v -> {
            search.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(search, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });
        TextView icon = label("Search", 13, true);
        icon.setTextColor(PRIMARY);
        icon.setGravity(Gravity.CENTER);
        icon.setPadding(0, 0, dp(SPACE_MD), 0);
        searchBox.addView(icon);
        search.setTextSize(16);
        search.setBackgroundColor(Color.TRANSPARENT);
        search.setPadding(dp(SPACE_XS), 0, dp(SPACE_SM), 0);
        searchBox.addView(search, new LinearLayout.LayoutParams(0, dp(56), 1));
        panel.addView(searchBox, new LinearLayout.LayoutParams(-1, dp(58)));
        return panel;
    }

    private EditText input(String value) {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setText(value == null ? "" : value);
        edit.setTextSize(15);
        edit.setTextColor(TEXT);
        edit.setHintTextColor(Color.rgb(135, 151, 166));
        edit.setBackground(glassInput(false));
        edit.setPadding(dp(SPACE_MD), 0, dp(SPACE_MD), 0);
        edit.setOnFocusChangeListener((v, hasFocus) ->
                edit.setBackground(glassInput(hasFocus)));
        return edit;
    }

    private EditText readOnlyInput(String value) {
        EditText edit = input(value);
        edit.setEnabled(false);
        edit.setTextColor(MUTED);
        edit.setBackground(rounded(Color.rgb(235, 241, 246), dp(BUTTON_RADIUS), dp(1), BORDER));
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
        view.setPadding(dp(SPACE_MD), 0, dp(SPACE_MD), 0);
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
        b.setBackground(rounded(PRIMARY_SOFT, dp(BUTTON_RADIUS), dp(1), Color.rgb(184, 207, 225)));
        b.setElevation(dp(1));
        return b;
    }

    @SuppressLint("ClickableViewAccessibility")
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
        b.setMinHeight(dp(BUTTON_HEIGHT));
        b.setMinimumHeight(dp(BUTTON_HEIGHT));
        b.setPadding(dp(SPACE_LG), 0, dp(SPACE_LG), 0);
        int start = buttonStartColor(text);
        int end = buttonEndColor(text);
        b.setBackground(gradient(start, end, dp(BUTTON_RADIUS)));
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
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(BUTTON_HEIGHT));
        lp.setMargins(dp(SPACE_XS), dp(SPACE_XS), dp(SPACE_XS), dp(SPACE_XS));
        b.setLayoutParams(lp);
        return b;
    }

    private int buttonStartColor(String text) {
        String label = value(text).toLowerCase(java.util.Locale.US);
        if (label.contains("delete") || label.contains("remove")) {
            return URGENT;
        }
        if (label.contains("call") || label.contains("save") || label.contains("apply") || label.contains("add user")) {
            return ACCENT;
        }
        if (label.contains("excel") || label.contains("pdf") || label.contains("csv") || label.contains("search") || label.contains("export")) {
            return SLATE;
        }
        return PRIMARY;
    }

    private int buttonEndColor(String text) {
        String label = value(text).toLowerCase(java.util.Locale.US);
        if (label.contains("delete") || label.contains("remove")) {
            return Color.rgb(127, 29, 29);
        }
        if (label.contains("call") || label.contains("save") || label.contains("apply") || label.contains("add user")) {
            return Color.rgb(0, 105, 92);
        }
        if (label.contains("excel") || label.contains("pdf") || label.contains("csv") || label.contains("search") || label.contains("export")) {
            return PRIMARY_DARK;
        }
        return PRIMARY_DARK;
    }

    private TextView chip(String text, int bg, int fg) {
        TextView chip = label(text, 12, true);
        chip.setTextColor(fg);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(CHIP_PAD_X), dp(CHIP_PAD_Y), dp(CHIP_PAD_X), dp(CHIP_PAD_Y));
        chip.setBackground(rounded(bg, dp(CHIP_RADIUS), 0, bg));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(0, dp(SPACE_XS), 0, dp(2));
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
                        Color.argb(222, 255, 255, 255),
                        Color.argb(150, 219, 242, 252)
                }
        );
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), Color.argb(238, 255, 255, 255));
        return drawable;
    }

    private GradientDrawable prominentPanel() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.rgb(4, 78, 113),
                        Color.rgb(0, 135, 129)
                }
        );
        drawable.setCornerRadius(dp(CARD_RADIUS));
        drawable.setStroke(dp(1), Color.argb(95, 255, 255, 255));
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
        drawable.setCornerRadius(dp(BUTTON_RADIUS));
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
