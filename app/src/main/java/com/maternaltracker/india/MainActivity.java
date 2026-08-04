package com.maternaltracker.india;

import android.app.Activity;
import android.app.AlertDialog;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

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
import java.util.Locale;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

@SuppressLint("SetTextI18n")
public class MainActivity extends Activity {
    private static final int BG_TOP = Color.rgb(225, 243, 251);
    private static final int BG_BOTTOM = Color.rgb(244, 251, 254);
    private static final int SURFACE = Color.rgb(255, 255, 255);
    private static final int SURFACE_ALT = Color.rgb(244, 250, 253);
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
    private static final int BORDER = Color.rgb(205, 224, 234);
    private static final int ALERT_INFO_BG = Color.rgb(237, 250, 247);
    private static final int ALERT_WARN_BG = Color.rgb(255, 248, 237);
    private static final int SPACE_XS = 4;
    private static final int SPACE_SM = 8;
    private static final int SPACE_MD = 12;
    private static final int SPACE_LG = 16;
    private static final int SPACE_XL = 24;
    private static final int CARD_RADIUS = 8;
    private static final int CARD_GAP = 10;
    private static final int BUTTON_RADIUS = 8;
    private static final int BUTTON_HEIGHT = 48;
    private static final int CHIP_RADIUS = 14;
    private static final int CHIP_PAD_X = 10;
    private static final int CHIP_PAD_Y = 5;
    private static final int SECTION_ACCENT_WIDTH = 4;
    private static final int SECTION_ACCENT_HEIGHT = 22;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DASHBOARD_CLOCK_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy | hh:mm a");
    private static final DateTimeFormatter REPORT_MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.US);
    private static final String HOSPITAL_NAME = "BLUE BIRD A GENERAL HOSPITAL";
    private static final String APP_NAME = "Maternal Care Registry";
    private static final String DEFAULT_STATE = "West Bengal";
    private static final String DEFAULT_DISTRICT = "MURSHIDABAD";
    private static final int REQ_EXPORT_EXCEL = 501;
    private static final int REQ_EXPORT_PDF = 502;
    private static final int REQ_EXPORT_BACKUP = 503;
    private static final String SCHEDULED_WHERE = "scheduled_delivery_date IS NOT NULL AND scheduled_delivery_date != ''";
    private static final String SCHEDULED_ACTIVE_WHERE = SCHEDULED_WHERE + " AND record_locked = 0";
    private static final String SCHEDULED_PENDING_WHERE = SCHEDULED_ACTIVE_WHERE + " AND (scheduled_delivery_called_at IS NULL OR scheduled_delivery_called_at = '')";
    private static final String SCHEDULED_WEEK_WHERE = SCHEDULED_ACTIVE_WHERE + " AND scheduled_delivery_date BETWEEN ? AND ?";
    private static final String SCHEDULED_CALL_PENDING_WHERE = SCHEDULED_PENDING_WHERE + " AND scheduled_delivery_date >= ?";
    private static final String SCHEDULED_COMPLETION_DUE_WHERE = SCHEDULED_WHERE + " AND scheduled_delivery_date < ? AND record_locked = 0";
    private static final String EDD_COMPLETION_DUE_WHERE = "record_locked = 0 AND edd_date IS NOT NULL AND edd_date != '' AND edd_date < ?";
    private static final String DELIVERY_COMPLETION_DUE_WHERE = "(" + SCHEDULED_COMPLETION_DUE_WHERE + ") OR (" + EDD_COMPLETION_DUE_WHERE + ")";
    private static final String FOLLOWUP_WEEK_WHERE = "record_locked = 0 AND (((visit2 IS NOT NULL AND visit2 != '') AND (visit3 IS NULL OR visit3 = '') AND (final_visit IS NULL OR final_visit = '') AND visit2 <= ?) OR ((visit3 IS NOT NULL AND visit3 != '') AND (final_visit IS NULL OR final_visit = '') AND visit3 <= ?) OR ((final_visit IS NOT NULL AND final_visit != '') AND final_visit <= ?))";
    private static final String OPEN_EDD_RANGE_WHERE = "record_locked = 0 AND edd_date BETWEEN ? AND ?";
    private static final String UPDATE_API_URL = "https://api.github.com/repos/0xhydraOp/maternal-tracker-india-android/releases/latest";
    private static final String REPORT_DATE_ENTRY = "Entry Date";
    private static final String REPORT_DATE_EDD = "EDD Date";
    private static final String REPORT_DATE_SCHEDULED = "Scheduled Delivery";
    private static final String REPORT_DATE_COMPLETED = "Completed / Locked";

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
    private TextView reportMonthLiveLabel;
    private Runnable reportMonthTicker;
    private Runnable reportsRefresh;
    private ScrollView patientFormScroll;
    private String currentUser = "";
    private String currentRole = "";
    private String currentPage = "Dashboard";
    private String lastSyncText = "Not synced yet";

    private EditText patientId;
    private EditText patientName;
    private EditText age;
    private Spinner bloodGroup;
    private EditText mobile;
    private AutoCompleteTextView state;
    private AutoCompleteTextView district;
    private AutoCompleteTextView subdistrict;
    private AutoCompleteTextView localBodyType;
    private Spinner localBody;
    private AutoCompleteTextView ward;
    private AutoCompleteTextView village;
    private EditText lmpDate;
    private TextView pregnancyAge;
    private EditText gravida;
    private EditText lastDeliveryMethod;
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
        root.setPadding(dp(8), topSafeInset() + dp(6), dp(8), bottomSafeInset() + dp(6));
        setContentView(root);

        root.addView(header());
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        profileMenu = profileMenu();
        body.addView(profileMenu, new LinearLayout.LayoutParams(dp(260), -1));
        profileMenu.setVisibility(View.GONE);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, new LinearLayout.LayoutParams(0, -1, 1));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        status = label("", 12, false);
        status.setTextColor(MUTED);
        status.setTextSize(10);
        status.setSingleLine(true);
        status.setEllipsize(android.text.TextUtils.TruncateAt.END);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setPadding(dp(8), dp(1), dp(8), dp(1));
        root.addView(status);
        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setBackground(rounded(Color.WHITE, dp(CARD_RADIUS), dp(1), BORDER));
        bottomNav.setElevation(dp(3));
        bottomNav.setPadding(dp(4), dp(4), dp(4), dp(4));
        root.addView(bottomNav, new LinearLayout.LayoutParams(-1, dp(58)));
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
                    } else if ("Reports".equals(currentPage) && reportsRefresh != null) {
                        reportsRefresh.run();
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
        box.setBackground(gradient(PRIMARY_DARK, PRIMARY, dp(CARD_RADIUS)));
        box.setPadding(dp(SPACE_MD), dp(8), dp(SPACE_MD), dp(8));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView mark = label("BBH", 11, true);
        mark.setTextColor(PRIMARY_DARK);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(rounded(Color.WHITE, dp(18), 0, Color.WHITE));
        mark.setContentDescription("Open profile menu");
        mark.setOnClickListener(v -> toggleProfileMenu());
        attachPressAnimation(mark, 0.94f);
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        markLp.setMargins(dp(8), 0, 0, 0);

        backButton = label("<", 20, true);
        backButton.setTextColor(Color.WHITE);
        backButton.setGravity(Gravity.CENTER);
        backButton.setContentDescription("Back");
        backButton.setBackground(rounded(Color.argb(38, 255, 255, 255), dp(BUTTON_RADIUS), 0, Color.TRANSPARENT));
        backButton.setVisibility(View.GONE);
        backButton.setOnClickListener(v -> goBackInApp());
        attachPressAnimation(backButton, 0.94f);
        top.addView(backButton, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView hospital = label(HOSPITAL_NAME, 13, true);
        hospital.setTextColor(Color.WHITE);
        hospital.setSingleLine(true);
        headerTitle = label("Dashboard", 11, true);
        headerTitle.setTextColor(Color.WHITE);
        headerTitle.setAlpha(0.9f);
        titles.addView(hospital);
        titles.addView(headerTitle);

        top.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        syncBadge = chip("SYNCING", Color.argb(58, 255, 255, 255), Color.WHITE);
        top.addView(syncBadge);
        top.addView(mark, markLp);
        box.addView(top);
        return box;
    }

    private void rebuildBottomNav() {
        if (bottomNav == null) {
            return;
        }
        bottomNav.removeAllViews();
        bottomNav.addView(bottomNavItem("Dashboard", R.drawable.ic_nav_home, "Home", v -> showDashboard()), new LinearLayout.LayoutParams(0, -1, 1));
        bottomNav.addView(bottomNavItem("Patient Entry", R.drawable.ic_nav_entry, "Entry", v -> showPatientForm(null)), new LinearLayout.LayoutParams(0, -1, 1));
        bottomNav.addView(bottomNavItem("Patient Search", R.drawable.ic_nav_search, "Search", v -> showPatientList(false)), new LinearLayout.LayoutParams(0, -1, 1));
        bottomNav.addView(bottomNavItem("Reports", R.drawable.ic_nav_reports, "Reports", v -> showReports()), new LinearLayout.LayoutParams(0, -1, 1));
        if (isAdmin()) {
            bottomNav.addView(bottomNavItem("Administration", R.drawable.ic_nav_admin, "Admin", v -> showAdmin()), new LinearLayout.LayoutParams(0, -1, 1));
        }
    }

    private LinearLayout profileMenu() {
        LinearLayout menu = card(Color.argb(218, 255, 255, 255), 1, Color.WHITE);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_LG));
        menu.setBackground(profilePanelBackground());
        menu.addView(profileHeader());

        ScrollView actionScroll = new ScrollView(this);
        actionScroll.setFillViewport(false);
        actionScroll.setVerticalScrollBarEnabled(false);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actionScroll.addView(actions);

        actions.addView(menuGroupTitle("Patient Work"));
        actions.addView(menuItem("New Patient", "Register a new maternal record", ACCENT, v -> showPatientForm(null)));
        actions.addView(menuItem("Search Patients", "Find records and update visits", PRIMARY, v -> showPatientList(false)));
        actions.addView(menuGroupTitle("Reports & Export"));
        actions.addView(menuItem("Reports", "Review scheduled, EDD, and village data", SLATE, v -> showReports()));
        actions.addView(menuItem("Export Center", "Create Excel and PDF outputs", PRIMARY_DARK, v -> showExportCenter()));
        actions.addView(menuGroupTitle("System"));
        actions.addView(menuItem("Update Center", "Download and install updates inside the app", WARNING, v -> showUpdateCenter()));
        if (isAdmin()) {
            actions.addView(menuItem("Administration", "Manage users and reference data", PRIMARY, v -> showAdmin()));
            actions.addView(menuItem("Patient Recovery", "Restore accidentally deleted records", WARNING, v -> showPatientRecovery()));
            actions.addView(menuItem("Backup Manager", "Create or restore database backup", SLATE, v -> showBackup()));
        }
        actions.addView(menuGroupTitle("Session"));
        actions.addView(menuItem("Sign Out", "Close this secure session", URGENT, v -> {
            firebase.signOut();
            currentUser = "";
            currentRole = "";
            showLogin();
        }));
        menu.addView(actionScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        return menu;
    }

    private View profileHeader() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(SPACE_LG), dp(SPACE_LG), dp(SPACE_LG), dp(SPACE_MD));
        box.setBackground(prominentPanel());

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
        TextView eyebrow = label("Account panel", 10, true);
        eyebrow.setTextColor(Color.argb(210, 255, 255, 255));
        TextView title = label("Blue Bird Hospital", 15, true);
        title.setTextColor(Color.WHITE);
        profileUserLabel = label(value(currentUser), 11, false);
        profileUserLabel.setTextColor(Color.argb(220, 255, 255, 255));
        profileUserLabel.setSingleLine(true);
        profileUserLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        identity.addView(eyebrow);
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
        TextView label = label(title.toUpperCase(java.util.Locale.US), 10, true);
        label.setTextColor(MUTED);
        label.setPadding(dp(2), dp(SPACE_MD), 0, dp(4));
        return label;
    }

    private View menuItem(String text, String detail, int accentColor, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(10), dp(8), dp(8), dp(8));
        item.setBackground(rounded(Color.argb(184, 255, 255, 255), dp(CARD_RADIUS), dp(1), Color.argb(210, 226, 242, 250)));
        TextView accent = new TextView(this);
        accent.setBackground(rounded(accentColor, dp(4), 0, accentColor));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(10), 0, dp(6), 0);
        TextView caption = label(text, 13, true);
        caption.setTextColor(PRIMARY_DARK);
        caption.setSingleLine(true);
        caption.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView helper = label(detail, 10, false);
        helper.setTextColor(MUTED);
        helper.setSingleLine(true);
        helper.setEllipsize(android.text.TextUtils.TruncateAt.END);
        copy.addView(caption);
        copy.addView(helper);

        TextView arrow = label(">", 13, true);
        arrow.setTextColor(MUTED);
        arrow.setGravity(Gravity.CENTER);
        item.addView(accent, new LinearLayout.LayoutParams(dp(4), dp(34)));
        item.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        item.addView(arrow, new LinearLayout.LayoutParams(dp(18), dp(34)));
        item.setOnClickListener(v -> {
            closeProfileMenu();
            listener.onClick(v);
        });
        attachPressAnimation(item, 0.98f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(7));
        item.setLayoutParams(lp);
        item.setElevation(dp(1));
        return item;
    }

    private void toggleProfileMenu() {
        if (profileMenu == null) {
            return;
        }
        boolean open = profileMenu.getVisibility() != View.VISIBLE;
        applyProfileMenuLayout(open);
        profileMenu.setVisibility(open ? View.VISIBLE : View.GONE);
        if (open) {
            refreshProfileMenuStatus();
            profileMenu.setTranslationX(-dp(36));
            profileMenu.setAlpha(0f);
            profileMenu.animate().translationX(0f).alpha(1f).setDuration(180).start();
        }
    }

    private void applyProfileMenuLayout(boolean open) {
        if (profileMenu == null || content == null) {
            return;
        }
        boolean compact = getResources().getConfiguration().screenWidthDp < 700;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) profileMenu.getLayoutParams();
        lp.width = compact && open ? -1 : dp(260);
        lp.height = -1;
        profileMenu.setLayoutParams(lp);
        content.setVisibility(compact && open ? View.GONE : View.VISIBLE);
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
            applyProfileMenuLayout(false);
            profileMenu.setVisibility(View.GONE);
        } else if (content != null) {
            content.setVisibility(View.VISIBLE);
        }
    }

    private void setPage(String title) {
        if (!"Dashboard".equals(title)) {
            stopDashboardClock();
        }
        if (!"Reports".equals(title)) {
            stopReportMonthTicker();
            reportsRefresh = null;
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
        scroll.post(() -> scroll.scrollTo(0, 0));

        String scopeWhere = dashboardScopeWhere();
        String[] scopeArgs = dashboardScopeArgs();
        int total = db.countPatients(scopeWhere, scopeArgs);
        int today = db.countPatients(appendWhere(scopeWhere, "entry_date = ?"), appendArgs(scopeArgs, LocalDate.now().toString()));
        int dueWeek = db.countPatients(appendWhere(scopeWhere, OPEN_EDD_RANGE_WHERE), appendArgs(scopeArgs, LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()));
        int scheduledPending = db.countPatients(appendWhere(scopeWhere, SCHEDULED_CALL_PENDING_WHERE), appendArgs(scopeArgs, LocalDate.now().toString()));
        int scheduledWeek = db.countPatients(appendWhere(scopeWhere, SCHEDULED_WEEK_WHERE), appendArgs(scopeArgs, LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()));
        int locked = db.countPatients(appendWhere(scopeWhere, "record_locked = 1"), scopeArgs);
        box.setPadding(0, 0, 0, dp(8));
        box.addView(dashboardStatusStrip(total));
        if (total == 0) {
            box.addView(zeroDashboardWorkspace());
            box.addView(operationalChecklist());
            box.addView(systemReadinessStrip());
            box.addView(dashboardPreviewState());
            return;
        }
        box.addView(todaySummaryStrip(today, dueWeek, scheduledWeek, scheduledPending, locked));
        box.addView(section("Priority Work", todayWorkView(scopeWhere, scopeArgs)));
        box.addView(verticalGap(4));
        box.addView(section("Upcoming EDD", upcomingEddView(scopeWhere, scopeArgs)));
        box.addView(section("Data Quality Alerts", needsAttentionView(scopeWhere, scopeArgs)));
    }

    private View todaySummaryStrip(int today, int dueWeek, int scheduledWeek, int scheduledPending, int locked) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(3));
        row.addView(summaryCard("New Today", today, "Registered", ACCENT, v -> showScopedPatientList("entry_date = ?", new String[]{LocalDate.now().toString()})));
        row.addView(summaryCard("EDD 7 Days", dueWeek, "Delivery window", WARNING, v -> showScopedPatientList(OPEN_EDD_RANGE_WHERE, new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()})));
        row.addView(summaryCard("Scheduled", scheduledWeek, "Doctor date", URGENT, v -> showScopedPatientList(SCHEDULED_WEEK_WHERE, new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()})));
        row.addView(summaryCard("Calls Pending", scheduledPending, "Need call", WARNING, v -> showScopedPatientList(SCHEDULED_CALL_PENDING_WHERE, new String[]{LocalDate.now().toString()})));
        row.addView(summaryCard("Completed", locked, "Locked records", PRIMARY, v -> showScopedPatientList("record_locked = 1", null)));
        scroll.addView(row);
        return section("Today Summary", scroll);
    }

    private View summaryCard(String title, int value, String caption, int color, View.OnClickListener click) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackground(rounded(Color.WHITE, dp(CARD_RADIUS), dp(1), Color.argb(125, Color.red(color), Color.green(color), Color.blue(color))));
        box.setOnClickListener(click);
        attachPressAnimation(box, 0.98f);
        TextView count = label(String.valueOf(value), 24, true);
        count.setTextColor(color);
        TextView name = label(title, 12, true);
        name.setTextColor(PRIMARY_DARK);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView detail = label(caption, 9, true);
        detail.setTextColor(MUTED);
        box.addView(count);
        box.addView(name);
        box.addView(detail);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(132), -2);
        lp.setMargins(0, 0, dp(8), dp(3));
        box.setLayoutParams(lp);
        animateIn(box);
        return box;
    }

    private View focusCard(String title, int value, String caption, int color, View.OnClickListener click) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setOnClickListener(click);
        attachPressAnimation(box, 0.98f);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackground(rounded(Color.argb(76, Color.red(color), Color.green(color), Color.blue(color)), dp(12), dp(1), Color.argb(145, Color.red(color), Color.green(color), Color.blue(color))));
        TextView count = label(String.valueOf(value), 24, true);
        count.setTextColor(color);
        TextView label = label(title, 12, true);
        label.setTextColor(PRIMARY_DARK);
        TextView note = label(caption, 9, true);
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
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label(isAdmin() ? "Hospital Overview" : "My Work Overview", 15, true);
        title.setTextColor(PRIMARY_DARK);
        dashboardClock = label("", 11, true);
        dashboardClock.setTextColor(PRIMARY_DARK);
        dashboardClock.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        dashboardClock.setPadding(dp(8), 0, 0, 0);
        TextView context = label(isAdmin() ? "All Blue Bird maternal records" : "Records assigned to this account", 11, false);
        context.setTextColor(MUTED);
        context.setPadding(0, dp(1), 0, dp(3));
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.addView(chip(DEFAULT_DISTRICT, PRIMARY_SOFT, PRIMARY_DARK));
        chips.addView(chip(DEFAULT_STATE, PRIMARY_SOFT, PRIMARY_DARK));
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

    private void startReportMonthTicker(TextView monthLabel, Runnable render) {
        stopReportMonthTicker();
        reportMonthLiveLabel = monthLabel;
        final String[] lastMonth = {""};
        reportMonthTicker = new Runnable() {
            @Override
            public void run() {
                if (reportMonthLiveLabel == null || !"Reports".equals(currentPage)) {
                    return;
                }
                String currentMonth = YearMonth.now().format(REPORT_MONTH_FMT);
                reportMonthLiveLabel.setText("Current report month: " + currentMonth + " | Updated " + LocalDateTime.now().format(TIME_FMT));
                if (!empty(lastMonth[0]) && !lastMonth[0].equals(currentMonth) && render != null) {
                    render.run();
                }
                lastMonth[0] = currentMonth;
                reportMonthLiveLabel.postDelayed(this, 60000);
            }
        };
        reportMonthTicker.run();
    }

    private void stopReportMonthTicker() {
        if (reportMonthLiveLabel != null && reportMonthTicker != null) {
            reportMonthLiveLabel.removeCallbacks(reportMonthTicker);
        }
        reportMonthTicker = null;
        reportMonthLiveLabel = null;
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
                db.listPatients("", appendWhere(scopeWhere, DELIVERY_COMPLETION_DUE_WHERE), appendArgs(scopeArgs, LocalDate.now().toString(), LocalDate.now().toString())),
                shown,
                seenPatientIds,
                "Completion due",
                "Delivery date has passed",
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
                db.listPatients("", appendWhere(scopeWhere, OPEN_EDD_RANGE_WHERE), appendArgs(scopeArgs, LocalDate.now().toString(), LocalDate.now().plusDays(7).toString())),
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
            list.addView(dashboardActions(
                    navButton("Completion Due", v -> showScopedPatientList(DELIVERY_COMPLETION_DUE_WHERE, new String[]{LocalDate.now().toString(), LocalDate.now().toString()})),
                    navButton("Scheduled", v -> showScopedPatientList(SCHEDULED_WEEK_WHERE, new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()})),
                    navButton("Follow-ups", v -> showScopedPatientList(FOLLOWUP_WEEK_WHERE, followupWeekArgs())),
                    navButton("EDD Week", v -> showScopedPatientList(OPEN_EDD_RANGE_WHERE, new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(7).toString()}))
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
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView meta = smallText(value(p.villageName) + " | " + value(p.mobileNumber));
        meta.setTextColor(MUTED);
        meta.setSingleLine(true);
        meta.setEllipsize(android.text.TextUtils.TruncateAt.END);
        copy.addView(name);
        copy.addView(meta);
        head.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(chip(badge, color, Color.WHITE));
        item.addView(head);
        TextView reasonLine = smallText(reason + " | EDD " + value(p.eddDate));
        reasonLine.setSingleLine(true);
        reasonLine.setEllipsize(android.text.TextUtils.TruncateAt.END);
        item.addView(reasonLine);
        List<View> actions = new java.util.ArrayList<>();
        boolean hasOpenRecord = false;
        if ("Mark Completed".equals(primaryAction)) {
            actions.add(button("Call Patient", v -> callPatient(db.getPatient(p.id))));
        } else if ("Update Visits".equals(primaryAction)) {
            actions.add(button("Update Visits", v -> showPatientForm(db.getPatient(p.id), true)));
            actions.add(button("Call Patient", v -> callPatient(db.getPatient(p.id))));
        } else if ("Call".equals(primaryAction)) {
            actions.add(button("Call Patient", v -> callPatient(db.getPatient(p.id))));
            actions.add(button("Open Record", v -> showPatientDetail(db.getPatient(p.id), false)));
            hasOpenRecord = true;
        } else {
            actions.add(button("Call Patient", v -> callPatient(db.getPatient(p.id))));
            actions.add(button("Open Record", v -> showPatientDetail(db.getPatient(p.id), false)));
            hasOpenRecord = true;
        }
        if (!hasOpenRecord) {
            actions.add(navButton("Open Record", v -> showPatientDetail(db.getPatient(p.id), false)));
        }
        if (deliveryCompletionEligible(p)) {
            actions.add(button("Mark Completed", v -> confirmDeliveryCompleted(db.getPatient(p.id))));
        }
        item.addView(dashboardActions(actions.toArray(new View[0])));
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
            Patient patient = db.getPatientByPatientId(row[0]);
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(SPACE_SM), dp(SPACE_SM), dp(SPACE_SM), dp(SPACE_SM));
            item.setBackground(rounded(Color.argb(76, 255, 255, 255), dp(10), dp(1), Color.argb(135, 255, 255, 255)));
            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(-1, -2);
            itemLp.setMargins(0, 0, 0, dp(7));
            item.setLayoutParams(itemLp);
            item.setOnClickListener(v -> showPatientDetail(db.getPatientByPatientId(row[0]), false));

            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout copy = new LinearLayout(this);
            copy.setOrientation(LinearLayout.VERTICAL);
            TextView name = label(value(row[1]), 14, true);
            name.setTextColor(PRIMARY_DARK);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            TextView detail = label(value(row[4]) + " | " + value(row[3]), 12, true);
            detail.setTextColor(TEXT);
            detail.setSingleLine(true);
            detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
            copy.addView(name);
            copy.addView(detail);
            top.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
            top.addView(chip("EDD " + value(row[2]), PRIMARY_SOFT, PRIMARY_DARK));
            item.addView(top);

            if (deliveryCompletionEligible(patient)) {
                item.addView(horizontalWrap(chip(deliveryWindowChipText(patient), URGENT, Color.WHITE)));
            }
            List<View> actions = new java.util.ArrayList<>();
            actions.add(button("Call Patient", v -> callPatient(db.getPatientByPatientId(row[0]))));
            actions.add(navButton("Open Record", v -> showPatientDetail(db.getPatientByPatientId(row[0]), false)));
            if (deliveryCompletionEligible(patient)) {
                actions.add(button("Mark Completed", v -> confirmDeliveryCompleted(db.getPatientByPatientId(row[0]))));
            }
            item.addView(dashboardActions(actions.toArray(new View[0])));
            list.addView(item);
        }
        list.addView(navButton("View EDD 30 Days", v -> showScopedPatientList(OPEN_EDD_RANGE_WHERE, new String[]{LocalDate.now().toString(), LocalDate.now().plusDays(30).toString()})));
        return list;
    }

    private View scheduledDeliveryView(String where, String[] args) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        List<String[]> rows = db.scheduledDeliveryRows(appendWhere(where, SCHEDULED_ACTIVE_WHERE + " AND scheduled_delivery_date >= ?"), appendArgs(args, LocalDate.now().toString()), 6);
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
            boolean notified = !empty(row[5]);
            item.addView(chip(notified ? "Patient notified" : "Call pending", notified ? ACCENT : WARNING, Color.WHITE));
            Patient patient = db.getPatientByPatientId(row[0]);
            List<View> actions = new java.util.ArrayList<>();
            actions.add(button("Call Patient", v -> callPatient(db.getPatientByPatientId(row[0]))));
            actions.add(button("Open Record", v -> showPatientDetail(db.getPatientByPatientId(row[0]), false)));
            if (deliveryCompletionEligible(patient)) {
                actions.add(button("Mark Completed", v -> confirmDeliveryCompleted(db.getPatientByPatientId(row[0]))));
            }
            item.addView(dashboardActions(actions.toArray(new View[0])));
            list.addView(item);
        }
        list.addView(navButton("View All Scheduled", v -> showScopedPatientList(SCHEDULED_WHERE, null)));
        return list;
    }

    private View scheduledCompletionDueView(String where, String[] args) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        List<Patient> patients = db.listPatients("", appendWhere(where, DELIVERY_COMPLETION_DUE_WHERE), appendArgs(args, LocalDate.now().toString(), LocalDate.now().toString()));
        if (patients.isEmpty()) {
            list.addView(emptyState("No delivery completion pending", "Patients move here after EDD or doctor-given delivery date passes."));
            return list;
        }
        int limit = Math.min(6, patients.size());
        for (int i = 0; i < limit; i++) {
            Patient p = patients.get(i);
            LinearLayout item = card(ALERT_WARN_BG, 1, WARNING);
            item.setPadding(dp(SPACE_MD), dp(SPACE_SM), dp(SPACE_MD), dp(SPACE_SM));
            item.addView(label(value(p.patientName), 14, true));
            item.addView(smallText(deliveryCompletionReason(p) + ": " + value(PatientRules.deliveryCompletionReferenceDate(p, LocalDate.now()))));
            item.addView(smallText("Mobile: " + value(p.mobileNumber) + " | Village: " + value(p.villageName)));
            item.addView(chip("Complete this patient record", WARNING, Color.WHITE));
            item.addView(dashboardActions(
                    button("Call Patient", v -> callPatient(db.getPatient(p.id))),
                    button("Open Record", v -> showPatientDetail(db.getPatient(p.id), false)),
                    button("Mark Completed", v -> confirmDeliveryCompleted(db.getPatient(p.id)))
            ));
            list.addView(item);
        }
        list.addView(navButton("View Completion Due", v -> showScopedPatientList(DELIVERY_COMPLETION_DUE_WHERE, new String[]{LocalDate.now().toString(), LocalDate.now().toString()})));
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
                    button("Call Patient", v -> callPatient(db.getPatient(p.id))),
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
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        int columns = getResources().getConfiguration().screenWidthDp < 600 ? 2 : 4;
        LinearLayout row = null;
        for (int i = 0; i < stats.length; i++) {
            if (i % columns == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                wrap.addView(row, new LinearLayout.LayoutParams(-1, -2));
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            lp.setMargins(dp(3), dp(3), dp(3), dp(5));
            row.addView(stats[i], lp);
        }
        return wrap;
    }

    private View compactKpi(String title, int value, String caption, View.OnClickListener click) {
        LinearLayout box = card();
        box.setOnClickListener(click);
        attachPressAnimation(box, 0.98f);
        box.setPadding(dp(10), dp(8), dp(10), dp(8));
        box.setMinimumHeight(dp(88));
        TextView count = label(String.valueOf(value), 22, true);
        count.setTextColor(PRIMARY);
        TextView titleView = label(title, 11, true);
        titleView.setTextColor(PRIMARY_DARK);
        TextView captionView = label(caption, 9, true);
        captionView.setTextColor(MUTED);
        titleView.setSingleLine(true);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        captionView.setSingleLine(true);
        captionView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        box.addView(count);
        box.addView(titleView);
        box.addView(captionView);
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
        attachPressAnimation(box, 0.98f);
        box.setBackground(glassPanel(dp(CARD_RADIUS)));
        box.setPadding(dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD));
        TextView number = label(String.valueOf(value), 23, true);
        int tone = reportTone(title);
        number.setTextColor(tone);
        TextView caption = label(title, 12, true);
        caption.setTextColor(PRIMARY_DARK);
        TextView underline = new TextView(this);
        underline.setBackground(rounded(tone, dp(4), 0, tone));
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(dp(42), dp(4));
        lineLp.setMargins(0, dp(4), 0, 0);
        box.addView(number);
        box.addView(caption);
        box.addView(underline, lineLp);
        return box;
    }

    private View reportOverviewBlock(String title, View... stats) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(dp(8), dp(7), dp(8), dp(8));
        block.setBackground(rounded(Color.argb(82, 255, 255, 255), dp(10), dp(1), Color.argb(145, 255, 255, 255)));
        TextView heading = label(title, 13, true);
        heading.setTextColor(PRIMARY_DARK);
        heading.setPadding(0, 0, 0, dp(5));
        block.addView(heading);
        block.addView(statGrid(stats));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(7));
        block.setLayoutParams(lp);
        return block;
    }

    private View reportContextLine(String from, String to, String monthLabel) {
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        if (!empty(monthLabel) && !"All Months".equalsIgnoreCase(monthLabel)) {
            chips.addView(chip("Month: " + monthLabel, ACCENT, Color.WHITE));
        } else {
            chips.addView(chip("Month: all", ACCENT, Color.WHITE));
        }
        String range = empty(from) && empty(to) ? "Date range: all" : "Date range: " + (empty(from) ? "..." : from) + " to " + (empty(to) ? "..." : to);
        chips.addView(chip(range, PRIMARY_SOFT, PRIMARY_DARK));
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(chips);
        return scroll;
    }

    private View reportHeroMetric(String title, String value, String period, String hint) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackground(rounded(Color.argb(132, Color.red(PRIMARY), Color.green(PRIMARY), Color.blue(PRIMARY)), dp(14), dp(1), Color.argb(170, 255, 255, 255)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView t = label(title, 16, true);
        t.setTextColor(Color.WHITE);
        TextView h = label(period + " | " + hint, 12, true);
        h.setTextColor(Color.argb(220, 255, 255, 255));
        copy.addView(t);
        copy.addView(h);
        TextView number = label(value, 35, true);
        number.setTextColor(Color.WHITE);
        number.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        box.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(number, new LinearLayout.LayoutParams(dp(96), -2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(8));
        box.setLayoutParams(lp);
        return box;
    }

    private int reportTone(String title) {
        String normalized = value(title).toLowerCase(java.util.Locale.US);
        if (normalized.contains("complete due") || normalized.contains("call pending")) {
            return URGENT;
        }
        if (normalized.contains("scheduled") || normalized.contains("edd") || normalized.contains("follow")) {
            return WARNING;
        }
        if (normalized.contains("completed") || normalized.contains("today")) {
            return ACCENT;
        }
        return PRIMARY;
    }

    private View workTile(String title, int value, String captionText, View.OnClickListener click) {
        LinearLayout box = card();
        box.setOnClickListener(click);
        attachPressAnimation(box, 0.98f);
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
        age = input(value(patient == null ? null : patient.age));
        bloodGroup = spinner(bloodGroups(), "Select blood group", "Blood Group");
        mobile = input(value(patient == null ? null : patient.mobileNumber));
        patientName.setHint("Patient full name");
        age.setHint("Patient age");
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
        localBody = spinner(murshidabadBlocks(), "Select block", "Block Name");
        ward = auto(db.listWards(selectedLocalBodyCode));
        village = auto(list());
        village.setHint("Type village name");
        village.setThreshold(Integer.MAX_VALUE);
        lmpDate = input(patient == null ? LocalDate.now().toString() : value(patient.lmpDate));
        pregnancyAge = readOnlyInput(pregnancyAgeText(patient == null ? null : patient));
        gravida = input(value(patient == null ? null : patient.gravida));
        lastDeliveryMethod = input(value(patient == null ? null : patient.lastDeliveryMethod));
        eddDate = input(patient == null ? LocalDate.now().plusDays(280).toString() : value(patient.eddDate));
        scheduledDeliveryDate = input(value(patient == null ? null : patient.scheduledDeliveryDate));
        motivator = auto(uppercaseList(db.listNames("custom_motivators")));
        doctor = auto(uppercaseList(db.listNames("custom_doctors")));
        lmpDate.setHint("YYYY-MM-DD");
        gravida.setHint("Example: G2P1");
        lastDeliveryMethod.setHint("Optional");
        eddDate.setHint("YYYY-MM-DD");
        scheduledDeliveryDate.setHint("Optional YYYY-MM-DD");
        motivator.setHint("Optional");
        doctor.setHint("Doctor name");
        configureLookupDropdown(motivator);
        configureLookupDropdown(doctor);
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
            selectSpinnerValue(localBody, patient.localBodyName);
            selectedLocalBodyCode = db.getCodeByNameAndParent("local_bodies", selectedSpinnerValue(localBody), "district_code", selectedDistrictCode);
            setAdapter(ward, db.listWards(selectedLocalBodyCode));
            ward.setText(value(patient.wardName), false);
            village.setText(value(patient.villageName), false);
            age.setText(value(patient.age));
            selectSpinnerValue(bloodGroup, patient.bloodGroup);
            gravida.setText(value(patient.gravida));
            lastDeliveryMethod.setText(value(patient.lastDeliveryMethod));
            motivator.setText(value(patient.motivatorName), false);
            doctor.setText(value(patient.doctorName), false);
        }

        configurePatientEntryCapitalization();
        refreshPregnancyAgePreview(patient);
        lmpDate.addTextChangedListener(simpleWatcher(s -> {
            updateEdd();
            refreshPregnancyAgePreview(patient);
        }));
        scheduledDeliveryDate.addTextChangedListener(simpleWatcher(s -> refreshPregnancyAgePreview(patient)));
        finalVisit.addTextChangedListener(simpleWatcher(s -> refreshPregnancyAgePreview(patient)));
        addPatientValidationWatchers();
        subdistrict.setOnItemClickListener((parent, view, position, id) -> selectSpinnerValue(localBody, text(subdistrict)));
        localBody.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String block = selectedSpinnerValue(localBody);
                if (!empty(block)) {
                    subdistrict.setText(block, false);
                    selectedLocalBodyCode = db.getCodeByNameAndParent("local_bodies", block, "district_code", selectedDistrictCode);
                    setAdapter(ward, db.listWards(selectedLocalBodyCode));
                }
                if (status != null) {
                    status.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        boolean lockedForStaff = patient != null && patient.recordLocked && !isAdmin();
        form.addView(patientFormOverview(patient != null));
        form.addView(formStep("1", "Basic Info", true,
                reportTwoColumn(row("Patient ID *", patientId), row("Age *", age)),
                row("Patient Name *", patientName),
                reportTwoColumn(row("Blood Group *", bloodGroup), row("Mobile Number *", mobile)),
                row("Motivator Name", motivator),
                row("Doctor Name *", doctor)
        ));
        form.addView(formStep("2", "Address", true,
                reportTwoColumn(row("State / UT *", state), row("District *", district)),
                reportTwoColumn(row("Block Name *", localBody), row("Village *", village))
        ));
        form.addView(formStep("3", "Pregnancy Dates", true,
                reportTwoColumn(row("LMP Date *", lmpDate), row("Pregnancy Age", pregnancyAge)),
                smallText("Auto-calculated from LMP; freezes after the patient is completed."),
                reportTwoColumn(row("GRAVIDA *", gravida), row("Method of Last Delivery", lastDeliveryMethod)),
                reportTwoColumn(row("EDD Date *", eddDate), row("Scheduled Delivery Date", scheduledDeliveryDate)),
                smallText("Optional doctor-given delivery date. Changing it resets the call reminder."),
                row("Entry / 1st Visit *", visit1)
        ));
        form.addView(formStep("4", "Visit Tracking", true,
                reportTwoColumn(row("2nd Visit", visit2), row("3rd Visit", visit3)),
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

    private View patientFormOverview(boolean editing) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(SPACE_LG), dp(SPACE_MD), dp(SPACE_LG), dp(SPACE_MD));
        panel.setBackground(prominentPanel());
        panel.setElevation(dp(2));
        TextView title = label(editing ? "Update Patient Record" : "Register New Patient", 17, true);
        title.setTextColor(Color.WHITE);
        TextView detail = label(editing ? "Review details and save only the information that changed." : "Complete the four clinical sections. Required fields are marked with *.", 12, false);
        detail.setTextColor(Color.argb(225, 255, 255, 255));
        detail.setPadding(0, dp(3), 0, dp(8));
        HorizontalScrollView progress = new HorizontalScrollView(this);
        progress.setHorizontalScrollBarEnabled(false);
        LinearLayout steps = new LinearLayout(this);
        steps.setOrientation(LinearLayout.HORIZONTAL);
        steps.addView(formProgressChip("1", "Patient"));
        steps.addView(formProgressChip("2", "Address"));
        steps.addView(formProgressChip("3", "Pregnancy"));
        steps.addView(formProgressChip("4", "Visits"));
        progress.addView(steps);
        panel.addView(title);
        panel.addView(detail);
        panel.addView(progress);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(CARD_GAP));
        panel.setLayoutParams(lp);
        return panel;
    }

    private View formProgressChip(String number, String title) {
        TextView chip = label(number + "  " + title, 11, true);
        chip.setTextColor(Color.WHITE);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        chip.setBackground(rounded(Color.argb(42, 255, 255, 255), dp(CHIP_RADIUS), dp(1), Color.argb(70, 255, 255, 255)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(0, 0, dp(6), 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    private void addPatientValidationWatchers() {
        TextView[] fields = {patientName, age, mobile, village, gravida, lmpDate, eddDate, scheduledDeliveryDate, doctor, visit2, visit3, finalVisit};
        for (TextView field : fields) {
            field.addTextChangedListener(simpleWatcher(s -> {
                field.setError(null);
                if (status != null) {
                    status.setText("");
                }
            }));
        }
        bloodGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (status != null) {
                    status.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void configurePatientEntryCapitalization() {
        applyUppercaseFilter(
                patientId,
                patientName,
                age,
                mobile,
                state,
                district,
                subdistrict,
                localBodyType,
                ward,
                village,
                lmpDate,
                gravida,
                lastDeliveryMethod,
                eddDate,
                scheduledDeliveryDate,
                motivator,
                doctor,
                visit1,
                visit2,
                visit3,
                finalVisit
        );
        normalizeUppercaseFields(
                patientId,
                patientName,
                age,
                state,
                district,
                subdistrict,
                localBodyType,
                ward,
                village,
                gravida,
                lastDeliveryMethod,
                motivator,
                doctor
        );
    }

    private void applyUppercaseFilter(EditText... fields) {
        for (EditText field : fields) {
            if (field == null) {
                continue;
            }
            InputFilter[] current = field.getFilters();
            boolean hasAllCaps = false;
            for (InputFilter filter : current) {
                if (filter instanceof InputFilter.AllCaps) {
                    hasAllCaps = true;
                    break;
                }
            }
            if (hasAllCaps) {
                continue;
            }
            InputFilter[] next = new InputFilter[current.length + 1];
            System.arraycopy(current, 0, next, 0, current.length);
            next[current.length] = new InputFilter.AllCaps();
            field.setFilters(next);
        }
    }

    private void normalizeUppercaseFields(EditText... fields) {
        for (EditText field : fields) {
            if (field == null) {
                continue;
            }
            String normalized = uppercaseEntryValue(text(field));
            if (!value(normalized).equals(value(text(field)))) {
                field.setText(value(normalized));
                if (field.isEnabled()) {
                    field.setSelection(field.getText().length());
                }
            }
        }
    }

    private void configureLookupDropdown(AutoCompleteTextView view) {
        view.setThreshold(1);
        view.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (view.hasFocus() && s != null && s.length() >= view.getThreshold()) {
                    view.post(view::showDropDown);
                }
            }
        });
        view.setOnClickListener(v -> {
            if (view.getText() != null && view.getText().length() >= view.getThreshold()) {
                view.showDropDown();
            }
        });
        view.setOnFocusChangeListener((v, hasFocus) -> {
            view.setBackground(glassInput(hasFocus));
            if (hasFocus && view.getText() != null && view.getText().length() >= view.getThreshold()) {
                view.post(view::showDropDown);
            }
        });
    }

    private void savePatient() {
        Patient p = editingPatient == null ? new Patient() : editingPatient;
        Patient old = editingPatient == null ? null : db.getPatient(editingPatient.id);
        p.patientId = text(patientId);
        p.patientName = entryText(patientName);
        p.age = entryText(age);
        p.bloodGroup = selectedSpinnerValue(bloodGroup);
        p.mobileNumber = text(mobile);
        p.stateName = entryText(state);
        p.districtName = entryText(district);
        p.subdistrictName = entryText(subdistrict);
        p.localBodyType = entryText(localBodyType);
        p.localBodyName = selectedSpinnerValue(localBody);
        p.wardName = entryText(ward);
        p.villageName = entryText(village);
        p.gravida = entryText(gravida);
        p.lastDeliveryMethod = entryText(lastDeliveryMethod);
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
        p.motivatorName = entryText(motivator);
        p.doctorName = entryText(doctor);
        p.visit1 = text(visit1);
        p.visit2 = text(visit2);
        p.visit3 = text(visit3);
        p.finalVisit = text(finalVisit);
        p.entryDate = p.entryDate == null ? LocalDate.now().toString() : p.entryDate;
        p.createdBy = old == null || empty(old.createdBy) ? currentUser : old.createdBy;
        p.updatedBy = currentUser;
        p.deletedAt = old == null ? value(p.deletedAt) : value(old.deletedAt);
        p.deletedBy = old == null ? value(p.deletedBy) : value(old.deletedBy);
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
                db.discardLocalPatient(p.id);
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
        View target = null;
        String message = fallback;
        if (empty(p.patientName)) {
            target = patientName;
            message = "Patient name is required";
        } else if (empty(p.age)) {
            target = age;
            message = "Age is required";
        } else if (empty(p.bloodGroup)) {
            target = bloodGroup;
            message = "Blood group is required";
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
        } else if (empty(p.gravida)) {
            target = gravida;
            message = "GRAVIDA is required";
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
        } else if (empty(p.visit1) || !PatientRules.validDate(p.visit1)) {
            target = visit1;
            message = "Entry / 1st visit must use YYYY-MM-DD";
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
            if (target instanceof TextView) {
                ((TextView) target).setError(message);
            }
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
        db.logChange(p.patientId, "age", old.age, p.age, currentUser);
        db.logChange(p.patientId, "blood_group", old.bloodGroup, p.bloodGroup, currentUser);
        db.logChange(p.patientId, "mobile_number", old.mobileNumber, p.mobileNumber, currentUser);
        db.logChange(p.patientId, "state_name", old.stateName, p.stateName, currentUser);
        db.logChange(p.patientId, "district_name", old.districtName, p.districtName, currentUser);
        db.logChange(p.patientId, "block_name", old.localBodyName, p.localBodyName, currentUser);
        db.logChange(p.patientId, "village_name", old.villageName, p.villageName, currentUser);
        db.logChange(p.patientId, "gravida", old.gravida, p.gravida, currentUser);
        db.logChange(p.patientId, "last_delivery_method", old.lastDeliveryMethod, p.lastDeliveryMethod, currentUser);
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
        showPatientList(adminMode, extraWhere, extraArgs, alreadyScoped, null);
    }

    private void showPatientList(boolean adminMode, String extraWhere, String[] extraArgs, boolean alreadyScoped, String activeShortcut) {
        setPage(adminMode ? "Patient Management" : "Patient Search");
        boolean fullAccess = adminMode && isAdmin();
        String visibleWhere = fullAccess || alreadyScoped ? extraWhere : scopedWhere(extraWhere);
        String[] visibleArgs = fullAccess || alreadyScoped ? extraArgs : scopedArgs(extraArgs);
        String shortcutBaseWhere = fullAccess ? null : scopedWhere(null);
        String[] shortcutBaseArgs = fullAccess ? null : scopedArgs(null);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        content.addView(page, new LinearLayout.LayoutParams(-1, -1));
        EditText search = input("");
        search.setHint("Search name, mobile, patient ID, village");
        page.addView(searchPanel(search));
        page.addView(scrollingActions(
                shortcutButton("Completion Due", activeShortcut, v -> showPatientList(adminMode, appendWhere(shortcutBaseWhere, DELIVERY_COMPLETION_DUE_WHERE), appendArgs(shortcutBaseArgs, LocalDate.now().toString(), LocalDate.now().toString()), true, "Completion Due")),
                shortcutButton("Scheduled", activeShortcut, v -> showPatientList(adminMode, appendWhere(shortcutBaseWhere, SCHEDULED_WHERE), shortcutBaseArgs, true, "Scheduled")),
                shortcutButton("Call Pending", activeShortcut, v -> showPatientList(adminMode, appendWhere(shortcutBaseWhere, SCHEDULED_CALL_PENDING_WHERE), appendArgs(shortcutBaseArgs, LocalDate.now().toString()), true, "Call Pending")),
                shortcutButton("Visit Follow-ups", activeShortcut, v -> showPatientList(adminMode, appendWhere(shortcutBaseWhere, FOLLOWUP_WEEK_WHERE), appendArgs(shortcutBaseArgs, followupWeekArgs()), true, "Visit Follow-ups")),
                shortcutButton("EDD 30 Days", activeShortcut, v -> showPatientList(adminMode, appendWhere(shortcutBaseWhere, OPEN_EDD_RANGE_WHERE), appendArgs(shortcutBaseArgs, LocalDate.now().toString(), LocalDate.now().plusDays(30).toString()), true, "EDD 30 Days")),
                shortcutButton("Locked", activeShortcut, v -> showPatientList(adminMode, appendWhere(shortcutBaseWhere, "record_locked = 1"), shortcutBaseArgs, true, "Locked"))
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
        Runnable reload = () -> renderPatientRows(list, db.listPatients(text(search), visibleWhere, visibleArgs), adminMode, visibleWhere, activeShortcut);
        search.addTextChangedListener(simpleWatcher(s -> reload.run()));
        reload.run();
    }

    private void renderPatientRows(LinearLayout list, List<Patient> patients, boolean adminMode, String visibleWhere, String activeShortcut) {
        list.removeAllViews();
        list.addView(searchResultHeader(patients.size(), activeShortcut));
        if (patients.isEmpty()) {
            list.addView(patientListEmptyState(visibleWhere));
            return;
        }
        for (Patient p : patients) {
            LinearLayout card = patientSearchCard(p);
            if (!empty(p.scheduledDeliveryDate)) {
                card.addView(statusLine("Scheduled delivery", value(p.scheduledDeliveryDate), scheduledDeliveryStatusText(p), scheduledDeliveryStatusColor(p)));
            }
            card.setOnClickListener(v -> showPatientDetail(db.getPatient(p.id), adminMode));
            List<View> rowActions = new java.util.ArrayList<>();
            rowActions.add(button("Call Patient", v -> callPatient(db.getPatient(p.id))));
            rowActions.add(button("Open Record", v -> showPatientDetail(db.getPatient(p.id), adminMode)));
            if (!p.recordLocked || isAdmin()) {
                rowActions.add(button("Update Visits", v -> showPatientForm(db.getPatient(p.id), true)));
            }
            if (deliveryCompletionEligible(p)) {
                rowActions.add(button("Mark Completed", v -> confirmDeliveryCompleted(db.getPatient(p.id))));
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
            }
            card.addView(scrollingActions(rowActions.toArray(new View[0])));
            if (adminMode && isAdmin()) {
                card.addView(adminPatientDeleteControl(p));
            }
            list.addView(card);
        }
    }

    private View adminPatientDeleteControl(Patient p) {
        LinearLayout control = new LinearLayout(this);
        control.setOrientation(LinearLayout.HORIZONTAL);
        control.setGravity(Gravity.CENTER_VERTICAL);
        control.setPadding(dp(9), dp(7), dp(9), dp(7));
        control.setBackground(rounded(Color.argb(54, Color.red(URGENT), Color.green(URGENT), Color.blue(URGENT)), dp(10), dp(1), Color.argb(120, Color.red(URGENT), Color.green(URGENT), Color.blue(URGENT))));
        TextView label = label("Admin Patient Control", 12, true);
        label.setTextColor(URGENT);
        Button delete = button("Delete Patient", v -> confirmDeletePatient(db.getPatient(p.id)));
        control.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        control.addView(delete);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(4), 0, 0);
        control.setLayoutParams(lp);
        return control;
    }

    private View searchResultHeader(int count, String activeShortcut) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(7), dp(10), dp(7));
        box.setBackground(rounded(Color.argb(88, 255, 255, 255), dp(CARD_RADIUS), dp(1), Color.argb(180, 255, 255, 255)));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label(empty(activeShortcut) ? "Search Results" : "Selected Shortcut", 14, true);
        title.setTextColor(PRIMARY_DARK);
        TextView countChip = chip(count + (empty(activeShortcut) ? " found" : " available"), count == 0 ? WARNING : ACCENT, Color.WHITE);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(countChip);
        box.addView(top);
        if (!empty(activeShortcut)) {
            TextView selected = label(activeShortcut, 16, true);
            selected.setTextColor(PRIMARY);
            selected.setPadding(0, dp(2), 0, 0);
            box.addView(selected);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(7));
        box.setLayoutParams(lp);
        return box;
    }

    private View patientListEmptyState(String visibleWhere) {
        String filter = value(visibleWhere).toLowerCase(java.util.Locale.US);
        if (filter.contains("scheduled_delivery_date <") || filter.contains("edd_date <")) {
            return emptyState("No completion-due patients", "No EDD or doctor-given delivery records currently need completion.");
        }
        if (filter.contains("scheduled_delivery_date between")) {
            return emptyState("No scheduled delivery window", "Doctor-given delivery dates due within this window will appear here.");
        }
        if (filter.contains("edd_date between")) {
            return emptyState("No EDD window patients", "Patients with EDD inside this date window will appear here.");
        }
        if (filter.contains("scheduled_delivery_called_at")) {
            return emptyState("No call-pending patients", "Scheduled delivery patients appear here until staff call and mark them notified.");
        }
        if (filter.contains("visit2") || filter.contains("visit3") || filter.contains("final_visit")) {
            return emptyState("No visit follow-ups due", "Planned 2nd, 3rd, or final visit dates due in this window will appear here.");
        }
        if (filter.contains("record_locked = 1")) {
            return emptyState("No completed records", "Completed and locked patient records will appear here.");
        }
        return emptyActionState("No matching patient records", "Create a new patient record or adjust the search text.", "New Patient", v -> showPatientForm(null));
    }

    private LinearLayout patientSearchCard(Patient p) {
        LinearLayout card = card();
        card.setPadding(dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_SM));
        int tone = patientStatusColor(p);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView rail = new TextView(this);
        rail.setBackground(rounded(tone, dp(5), 0, tone));
        top.addView(rail, new LinearLayout.LayoutParams(dp(5), dp(66)));

        LinearLayout identity = new LinearLayout(this);
        identity.setOrientation(LinearLayout.VERTICAL);
        identity.setPadding(dp(SPACE_SM), 0, dp(SPACE_SM), 0);
        TextView name = label(value(p.patientName), 17, true);
        name.setTextColor(PRIMARY_DARK);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView id = label(value(p.patientId), 12, true);
        id.setTextColor(MUTED);
        TextView contact = label(value(p.mobileNumber) + " | " + value(p.villageName) + ", " + value(p.localBodyName), 12, true);
        contact.setTextColor(SLATE);
        contact.setSingleLine(true);
        contact.setEllipsize(android.text.TextUtils.TruncateAt.END);
        identity.addView(name);
        identity.addView(id);
        identity.addView(contact);
        top.addView(identity, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(chip(patientStatusLabel(p), tone, Color.WHITE));
        card.addView(top);
        card.addView(patientBadgeRow(p));
        card.addView(statusLine("Profile", "Age " + value(p.age), "Blood " + value(p.bloodGroup), PRIMARY));
        card.addView(statusLine("Mobile", value(p.mobileNumber), "Call Patient", PRIMARY));
        card.addView(statusLine("Pregnancy", pregnancyAgeText(p), p.recordLocked ? "Completed" : "Today", WARNING));
        card.addView(statusLine("Care", "EDD " + value(p.eddDate), value(p.doctorName), ACCENT));
        if (deliveryCompletionEligible(p)) {
            card.addView(statusLine("Delivery", deliveryDisplayDate(p), deliveryCompletionReason(p), URGENT));
        }
        return card;
    }

    private String patientStatusLabel(Patient p) {
        if (p.recordLocked) {
            return "Completed";
        }
        if (deliveryCompletionDue(p)) {
            return "Complete due";
        }
        if (deliveryCompletionEligible(p)) {
            return "Delivery week";
        }
        if (dateWithinDays(p.scheduledDeliveryDate, 7)) {
            return "Scheduled week";
        }
        if (!empty(p.scheduledDeliveryDate) && empty(p.scheduledDeliveryCalledAt)) {
            return "Call pending";
        }
        if (dateWithinDays(p.eddDate, 7)) {
            return "EDD week";
        }
        return "Open";
    }

    private int patientStatusColor(Patient p) {
        if (p.recordLocked) {
            return ACCENT;
        }
        if (deliveryCompletionDue(p) || dateWithinDays(p.scheduledDeliveryDate, 7)) {
            return URGENT;
        }
        if (!empty(p.scheduledDeliveryDate) && empty(p.scheduledDeliveryCalledAt)) {
            return WARNING;
        }
        if (dateWithinDays(p.eddDate, 7)) {
            return WARNING;
        }
        return PRIMARY;
    }

    private View patientBadgeRow(Patient p) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(7), 0, dp(4));
        row.addView(chip(p.recordLocked ? "Locked" : "Active", p.recordLocked ? ACCENT : PRIMARY_SOFT, p.recordLocked ? Color.WHITE : PRIMARY_DARK));
        if (deliveryCompletionDue(p)) {
            row.addView(chip(deliveryWindowChipText(p), URGENT, Color.WHITE));
        } else if (deliveryCompletionEligible(p)) {
            row.addView(chip(deliveryWindowChipText(p), URGENT, Color.WHITE));
        } else if (dateWithinDays(p.scheduledDeliveryDate, 7)) {
            row.addView(chip("Scheduled delivery within 7 days", URGENT, Color.WHITE));
        } else if (!empty(p.scheduledDeliveryDate)) {
            row.addView(chip("Scheduled delivery", ALERT_WARN_BG, WARNING));
        }
        if (!empty(p.scheduledDeliveryDate) && empty(p.scheduledDeliveryCalledAt) && !p.recordLocked) {
            row.addView(chip("Call pending", WARNING, Color.WHITE));
        }
        if (!p.recordLocked && dateWithinDays(p.eddDate, 7)) {
            row.addView(chip("EDD within 7 days", WARNING, Color.WHITE));
        } else if (!p.recordLocked && dateWithinDays(p.eddDate, 30)) {
            row.addView(chip("EDD within 30 days", PRIMARY_SOFT, PRIMARY_DARK));
        }
        scroll.addView(row);
        return scroll;
    }

    private boolean dateWithinDays(String dateValue, int days) {
        if (empty(dateValue) || !PatientRules.validDate(dateValue)) {
            return false;
        }
        LocalDate date = LocalDate.parse(dateValue);
        LocalDate today = LocalDate.now();
        return !date.isBefore(today) && !date.isAfter(today.plusDays(days));
    }

    private View statusLine(String labelText, String valueText, String metaText, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));
        TextView label = label(labelText, 12, true);
        label.setTextColor(color);
        TextView value = label(valueText, 13, true);
        value.setTextColor(TEXT);
        TextView meta = label(metaText, 12, true);
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

        page.addView(compactPatientDetailCard(p, adminMode));
    }

    private View compactPatientDetailCard(Patient p, boolean adminMode) {
        LinearLayout panel = card();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.VERTICAL);
        TextView name = label(value(p.patientName), 19, true);
        name.setTextColor(PRIMARY_DARK);
        TextView meta = label(value(p.patientId) + " | " + value(p.villageName) + " | " + value(p.mobileNumber), 12, true);
        meta.setTextColor(SLATE);
        title.addView(name);
        title.addView(meta);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(chip(patientStatusLabel(p), patientStatusColor(p), Color.WHITE));
        panel.addView(top);

        if (deliveryCompletionEligible(p)) {
            TextView alert = chip("Completion action: " + deliveryCompletionReason(p), URGENT, Color.WHITE);
            panel.addView(horizontalWrap(alert));
        }

        java.util.List<View> actions = new java.util.ArrayList<>();
        actions.add(navButton("Back to Search", v -> showPatientList(adminMode)));
        if (!p.recordLocked || isAdmin()) {
            actions.add(button("Update Visits", v -> showPatientForm(db.getPatient(p.id), true)));
            actions.add(button("Edit Patient", v -> showPatientForm(db.getPatient(p.id))));
        }
        actions.add(button("Call Patient", v -> callPatient(db.getPatient(p.id))));
        if (deliveryCompletionEligible(p)) {
            actions.add(button("Mark Completed", v -> confirmDeliveryCompleted(db.getPatient(p.id))));
        }
        actions.add(button("Excel", v -> startPatientExport(p.id, REQ_EXPORT_EXCEL, value(p.patientId) + ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
        actions.add(button("PDF", v -> startPatientExport(p.id, REQ_EXPORT_PDF, value(p.patientId) + ".pdf", "application/pdf")));
        if (p.recordLocked) {
            actions.add(chip("Locked after final visit", ACCENT, Color.WHITE));
        }
        if (adminMode && isAdmin()) {
            actions.add(button("Unlock", v -> {
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
            actions.add(button("Delete", v -> confirmDeletePatient(p)));
        }
        panel.addView(scrollingActions(actions.toArray(new View[0])));
        panel.addView(deliveryStatusBlock(p));

        panel.addView(detailGroup("Patient",
                detailGrid(
                        detailCell("Patient ID", p.patientId, PRIMARY),
                        detailCell("Mobile", p.mobileNumber, PRIMARY),
                        detailCell("Age", p.age, SLATE),
                        detailCell("Blood Group", p.bloodGroup, SLATE)
                )
        ));
        panel.addView(detailGroup("Care & Address",
                detailGrid(
                        detailCell("Doctor", p.doctorName, PRIMARY),
                        detailCell("Motivator", optionalValue(p.motivatorName), SLATE),
                        detailCell("State", p.stateName, PRIMARY),
                        detailCell("District", p.districtName, PRIMARY),
                        detailCell("Block", p.localBodyName, ACCENT),
                        detailCell("Village", p.villageName, ACCENT)
                )
        ));
        panel.addView(detailGroup("Pregnancy",
                detailGrid(
                        detailCell("LMP", p.lmpDate, PRIMARY),
                        detailCell("Pregnancy Age", pregnancyAgeText(p), ACCENT),
                        detailCell("GRAVIDA", p.gravida, WARNING),
                        detailCell("Last Delivery", optionalValue(p.lastDeliveryMethod), SLATE),
                        detailCell("EDD", p.eddDate, WARNING),
                        detailCell("Scheduled Delivery", optionalValue(p.scheduledDeliveryDate), scheduledDeliveryStatusColor(p))
                )
        ));
        panel.addView(detailGroup("Visits & Record",
                detailGrid(
                        detailCell("1st Visit", p.visit1, PRIMARY),
                        detailCell("2nd Visit", optionalValue(p.visit2), SLATE),
                        detailCell("3rd Visit", optionalValue(p.visit3), SLATE),
                        detailCell("Final Visit", optionalValue(p.finalVisit), p.recordLocked ? ACCENT : SLATE),
                        detailCell("Call Status", scheduledDeliveryStatusText(p), scheduledDeliveryStatusColor(p)),
                        detailCell("Called At", optionalValue(p.scheduledDeliveryCalledAt), ACCENT),
                        detailCell("Called By", optionalValue(p.scheduledDeliveryCalledBy), SLATE),
                        detailCell("Record State", p.recordLocked ? "Completed / Locked" : "Open", p.recordLocked ? ACCENT : WARNING),
                        detailCell("Entry Date", p.entryDate, PRIMARY),
                        detailCell("Cloud", value(lastSyncText), ACCENT)
                ),
                detailFull("Created By", optionalValue(p.createdBy), SLATE),
                detailFull("Updated By", optionalValue(p.updatedBy), SLATE)
        ));
        return panel;
    }

    private View deliveryStatusBlock(Patient p) {
        return detailGroup("Delivery Status",
                detailGrid(
                        detailCell("Status", deliveryStatusSummary(p), deliveryStatusColor(p)),
                        detailCell("Target Date", deliveryDisplayDate(p), deliveryStatusColor(p)),
                        detailCell("EDD", p.eddDate, WARNING),
                        detailCell("Scheduled", optionalValue(p.scheduledDeliveryDate), scheduledDeliveryStatusColor(p)),
                        detailCell("Call", scheduledDeliveryStatusText(p), scheduledDeliveryStatusColor(p)),
                        detailCell("Record", p.recordLocked ? "Completed / Locked" : "Open", p.recordLocked ? ACCENT : WARNING)
                )
        );
    }

    private View detailGroup(String title, View... rows) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0, dp(5), 0, dp(7));
        LinearLayout headingRow = new LinearLayout(this);
        headingRow.setOrientation(LinearLayout.HORIZONTAL);
        headingRow.setGravity(Gravity.CENTER_VERTICAL);
        headingRow.setPadding(dp(8), dp(6), dp(8), dp(6));
        headingRow.setBackground(rounded(Color.argb(86, Color.red(PRIMARY), Color.green(PRIMARY), Color.blue(PRIMARY)), dp(10), dp(1), Color.argb(130, Color.red(PRIMARY), Color.green(PRIMARY), Color.blue(PRIMARY))));
        TextView accent = new TextView(this);
        accent.setBackground(rounded(ACCENT, dp(3), 0, ACCENT));
        LinearLayout.LayoutParams accentLp = new LinearLayout.LayoutParams(dp(5), dp(24));
        accentLp.setMargins(0, 0, dp(8), 0);
        headingRow.addView(accent, accentLp);
        TextView heading = label(title.toUpperCase(java.util.Locale.US), 14, true);
        heading.setTextColor(PRIMARY_DARK);
        heading.setSingleLine(true);
        heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        headingRow.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams headingLp = new LinearLayout.LayoutParams(-1, -2);
        headingLp.setMargins(0, dp(2), 0, dp(7));
        group.addView(headingRow, headingLp);
        for (View row : rows) {
            group.addView(row);
        }
        return group;
    }

    private View detailGrid(View... cells) {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < cells.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(cells[i], new LinearLayout.LayoutParams(0, -2, 1));
            if (i + 1 < cells.length) {
                row.addView(cells[i + 1], new LinearLayout.LayoutParams(0, -2, 1));
            } else {
                TextView spacer = new TextView(this);
                row.addView(spacer, new LinearLayout.LayoutParams(0, -2, 1));
            }
            grid.addView(row);
        }
        return grid;
    }

    private View detailCell(String title, String value, int color) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setPadding(dp(7), dp(4), dp(7), dp(4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        cell.setLayoutParams(lp);
        cell.setBackground(rounded(Color.argb(62, 255, 255, 255), dp(8), dp(1), Color.argb(120, Color.red(color), Color.green(color), Color.blue(color))));
        TextView label = label(title, 10, true);
        label.setTextColor(color);
        TextView body = label(displayValue(value), 13, true);
        body.setTextColor(TEXT);
        cell.addView(label);
        cell.addView(body);
        return cell;
    }

    private View detailFull(String title, String value, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(7), dp(4), dp(7), dp(4));
        TextView label = label(title, 10, true);
        label.setTextColor(color);
        TextView body = label(displayValue(value), 13, true);
        body.setTextColor(TEXT);
        row.addView(label);
        row.addView(body);
        return row;
    }

    private View horizontalWrap(View row) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(row);
        return scroll;
    }

    private String displayValue(String value) {
        return empty(value) ? "-" : value(value);
    }

    private String optionalValue(String value) {
        return empty(value) ? "Not added" : value(value);
    }

    private boolean scheduledDeliveryNeedsCompletion(Patient p) {
        return PatientRules.scheduledDeliveryNeedsCompletion(p, LocalDate.now());
    }

    private boolean eddNeedsCompletion(Patient p) {
        return PatientRules.eddNeedsCompletion(p, LocalDate.now());
    }

    private boolean deliveryCompletionDue(Patient p) {
        return PatientRules.deliveryCompletionDue(p, LocalDate.now());
    }

    private boolean deliveryCompletionEligible(Patient p) {
        return PatientRules.deliveryCompletionEligible(p, LocalDate.now());
    }

    private String deliveryCompletionReason(Patient p) {
        return PatientRules.deliveryCompletionReason(p, LocalDate.now());
    }

    private String deliveryStatusSummary(Patient p) {
        if (p == null) {
            return "-";
        }
        if (p.recordLocked) {
            return "Completed";
        }
        if (deliveryCompletionDue(p)) {
            return "Completion due";
        }
        if (deliveryCompletionEligible(p)) {
            return "Delivery window active";
        }
        if (!empty(p.scheduledDeliveryDate) && empty(p.scheduledDeliveryCalledAt)) {
            return "Scheduled call pending";
        }
        if (dateWithinDays(p.eddDate, 30)) {
            return "EDD tracking";
        }
        return "Open tracking";
    }

    private int deliveryStatusColor(Patient p) {
        if (p == null) {
            return PRIMARY;
        }
        if (p.recordLocked) {
            return ACCENT;
        }
        if (deliveryCompletionDue(p) || deliveryCompletionEligible(p)) {
            return URGENT;
        }
        if (!empty(p.scheduledDeliveryDate) && empty(p.scheduledDeliveryCalledAt)) {
            return WARNING;
        }
        return PRIMARY;
    }

    private String deliveryDisplayDate(Patient p) {
        if (p == null) {
            return "-";
        }
        String completionDate = PatientRules.deliveryCompletionReferenceDate(p, LocalDate.now());
        if (!empty(completionDate)) {
            return value(completionDate);
        }
        if (!empty(p.scheduledDeliveryDate)) {
            return value(p.scheduledDeliveryDate);
        }
        return value(p.eddDate);
    }

    private String deliveryWindowChipText(Patient p) {
        String source = PatientRules.deliveryCompletionSource(p, LocalDate.now());
        if ("scheduled".equals(source)) {
            return deliveryCompletionDue(p) ? "Complete scheduled delivery" : "Scheduled delivery window";
        }
        if ("edd".equals(source)) {
            return deliveryCompletionDue(p) ? "Complete EDD delivery" : "EDD delivery window";
        }
        return "Delivery window";
    }

    private String scheduledDeliveryStatusText(Patient p) {
        if (p == null) {
            return "";
        }
        if (p.recordLocked) {
            return "Completed";
        }
        if (empty(p.scheduledDeliveryDate)) {
            return "Not scheduled";
        }
        if (scheduledDeliveryNeedsCompletion(p)) {
            return "Completion required";
        }
        return empty(p.scheduledDeliveryCalledAt) ? "Call pending" : "Patient notified";
    }

    private int scheduledDeliveryStatusColor(Patient p) {
        if (p == null) {
            return PRIMARY;
        }
        if (p.recordLocked) {
            return ACCENT;
        }
        if (empty(p.scheduledDeliveryDate)) {
            return SLATE;
        }
        if (scheduledDeliveryNeedsCompletion(p) || dateWithinDays(p.scheduledDeliveryDate, 7)) {
            return URGENT;
        }
        if (empty(p.scheduledDeliveryCalledAt)) {
            return WARNING;
        }
        if (!empty(p.scheduledDeliveryCalledAt)) {
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

    private void confirmDeliveryCompleted(Patient p) {
        if (p == null) {
            toast("Patient not found");
            return;
        }
        if (!deliveryCompletionEligible(p)) {
            toast("Delivery completion is available only within the 7 day delivery window");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Mark Delivery Completed")
                .setMessage("Confirm delivery completed for " + value(p.patientName) + "?\n\n" + deliveryCompletionReason(p) + "\n\nThis will lock the record and move it to the completed list.")
                .setPositiveButton("Mark Completed", (dialog, which) -> markDeliveryCompleted(p))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void markDeliveryCompleted(Patient p) {
        Patient old = db.getPatient(p.id);
        if (old == null) {
            toast("Patient not found");
            return;
        }
        if (!deliveryCompletionEligible(old)) {
            toast("Delivery completion is available only within the 7 day delivery window");
            return;
        }
        if (empty(p.finalVisit) || !PatientRules.validDate(p.finalVisit) || LocalDate.parse(p.finalVisit).isAfter(LocalDate.now())) {
            p.finalVisit = PatientRules.deliveryCompletionVisitDate(old, LocalDate.now()).toString();
        }
        p.recordLocked = true;
        p.updatedBy = currentUser;
        try {
            db.savePatient(p);
            db.logChange(p.patientId, "final_visit", old.finalVisit, p.finalVisit, currentUser);
            db.logChange(p.patientId, "record_locked", old.recordLocked ? "1" : "0", "1", currentUser);
            db.logActivity(deliveryCompletionActivity(old), p.patientId, currentUser);
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

    private String deliveryCompletionActivity(Patient p) {
        return "edd".equals(PatientRules.deliveryCompletionSource(p, LocalDate.now())) ? "EDD_DELIVERY_COMPLETE" : "SCHEDULED_DELIVERY_COMPLETE";
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
        if (!isAdmin()) {
            toast("Only admin can delete patient records");
            return;
        }
        if (p == null) {
            toast("Patient not found");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Move Patient to Recovery")
                .setMessage("Hide " + value(p.patientName) + " (" + value(p.patientId) + ")?\n\nThe record will be removed from normal search, dashboard, reports, and exports. Admin can restore it later from Patient Recovery.")
                .setPositiveButton("Move to Recovery", (dialog, which) -> softDeletePatient(p))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void softDeletePatient(Patient source) {
        Patient p = db.getPatient(source.id);
        if (p == null) {
            toast("Patient not found");
            return;
        }
        String oldDeletedAt = value(p.deletedAt);
        String oldDeletedBy = value(p.deletedBy);
        String oldUpdatedBy = value(p.updatedBy);
        p.deletedAt = LocalDateTime.now().toString();
        p.deletedBy = currentUser;
        p.updatedBy = currentUser;
        db.savePatient(p);
        firebase.savePatient(p, (unused, error) -> runOnUiThread(() -> {
            if (error != null) {
                p.deletedAt = oldDeletedAt;
                p.deletedBy = oldDeletedBy;
                p.updatedBy = oldUpdatedBy;
                db.savePatient(p);
                toast("Delete sync failed: " + error.getMessage());
                return;
            }
            db.logActivity("PATIENT_DELETE", p.patientId + " moved to recovery", currentUser);
            toast("Patient moved to recovery");
            showPatientList(true);
        }));
    }

    private void confirmRestorePatient(Patient p) {
        if (!isAdmin()) {
            toast("Only admin can restore patient records");
            return;
        }
        if (p == null) {
            toast("Patient not found");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Restore Patient")
                .setMessage("Restore " + value(p.patientName) + " (" + value(p.patientId) + ") to active patient records?")
                .setPositiveButton("Restore", (dialog, which) -> restoreDeletedPatient(p))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void restoreDeletedPatient(Patient source) {
        Patient p = db.getPatient(source.id);
        if (p == null) {
            toast("Patient not found");
            return;
        }
        String oldDeletedAt = value(p.deletedAt);
        String oldDeletedBy = value(p.deletedBy);
        String oldUpdatedBy = value(p.updatedBy);
        p.deletedAt = "";
        p.deletedBy = "";
        p.updatedBy = currentUser;
        db.savePatient(p);
        firebase.savePatient(p, (unused, error) -> runOnUiThread(() -> {
            if (error != null) {
                p.deletedAt = oldDeletedAt;
                p.deletedBy = oldDeletedBy;
                p.updatedBy = oldUpdatedBy;
                db.savePatient(p);
                toast("Restore sync failed: " + error.getMessage());
                return;
            }
            db.logActivity("PATIENT_RESTORE", p.patientId + " restored from recovery", currentUser);
            toast("Patient restored");
            showPatientRecovery();
        }));
    }

    private void showReports() {
        setPage("Reports");
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        content.addView(page, new LinearLayout.LayoutParams(-1, -1));
        page.addView(reportPageHeader());

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.VERTICAL);
        EditText from = input("");
        EditText to = input("");
        TextView monthFilter = selectorField("All Months");
        TextView monthLive = smallText("");
        monthLive.setTextColor(PRIMARY_DARK);
        Runnable[] render = new Runnable[1];
        from.setHint("From YYYY-MM-DD");
        to.setHint("To YYYY-MM-DD");
        attachDatePicker(from);
        attachDatePicker(to);
        monthFilter.setOnClickListener(v -> showReportMonthPicker(monthFilter, from, to, render[0]));
        filters.addView(row("Month", monthFilter));
        filters.addView(reportTwoColumn(row("From", from), row("To", to)));

        LinearLayout reportBody = new LinearLayout(this);
        reportBody.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(reportBody);

        LinearLayout monthlyNewSlot = new LinearLayout(this);
        monthlyNewSlot.setOrientation(LinearLayout.VERTICAL);
        render[0] = () -> {
            if (validateReportFilters(text(from), text(to), text(monthFilter), from, to, monthFilter)) {
                renderNewPatientThisMonth(monthlyNewSlot);
                renderReports(reportBody, text(from), text(to), "", "All", REPORT_DATE_ENTRY, text(monthFilter));
            }
        };
        reportsRefresh = render[0];
        startReportMonthTicker(monthLive, render[0]);
        page.addView(monthlyNewSlot);
        page.addView(collapsibleSection("Report Controls", false,
                filters,
                monthLive,
                smallText("Search and export follow the selected month or date range."),
                dashboardActions(
                        button("Apply", v -> render[0].run()),
                        button("View Records", v -> {
                            if (validateReportFilters(text(from), text(to), text(monthFilter), from, to, monthFilter)) {
                                openFilteredPatientSearch(text(from), text(to), "", "All", REPORT_DATE_ENTRY);
                            }
                        }),
                        button("Excel", v -> {
                            if (validateReportFilters(text(from), text(to), text(monthFilter), from, to, monthFilter)) {
                                startFilteredExport(text(from), text(to), "", "All", REPORT_DATE_ENTRY, REQ_EXPORT_EXCEL, "patients.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                            }
                        }),
                        button("PDF", v -> {
                            if (validateReportFilters(text(from), text(to), text(monthFilter), from, to, monthFilter)) {
                                startFilteredExport(text(from), text(to), "", "All", REPORT_DATE_ENTRY, REQ_EXPORT_PDF, "patients.pdf", "application/pdf");
                            }
                        })
                )
        ));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        render[0].run();
    }

    private View reportPageHeader() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(SPACE_LG), dp(SPACE_MD), dp(SPACE_LG), dp(SPACE_MD));
        panel.setBackground(prominentPanel());
        panel.setElevation(dp(2));
        TextView eyebrow = label("LIVE HOSPITAL REPORTING", 10, true);
        eyebrow.setTextColor(Color.argb(205, 255, 255, 255));
        TextView title = label("Maternal Care Overview", 18, true);
        title.setTextColor(Color.WHITE);
        TextView detail = label("Registration, delivery, follow-up, and completion status in one view.", 12, false);
        detail.setTextColor(Color.argb(225, 255, 255, 255));
        detail.setPadding(0, dp(3), 0, 0);
        panel.addView(eyebrow);
        panel.addView(title);
        panel.addView(detail);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(CARD_GAP));
        panel.setLayoutParams(lp);
        return panel;
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

    private boolean validateReportFilters(String from, String to, String monthName, TextView fromField, TextView toField, View monthField) {
        clearFilterError(fromField);
        clearFilterError(toField);
        clearFilterError(monthField);
        if (!validReportMonth(monthName)) {
            return showFilterError(monthField, "Select a valid report month");
        }
        if (!empty(from) && !PatientRules.validDate(from)) {
            return showFilterError(fromField, "From date must use YYYY-MM-DD");
        }
        if (!empty(to) && !PatientRules.validDate(to)) {
            return showFilterError(toField, "To date must use YYYY-MM-DD");
        }
        if (!empty(from) && !empty(to) && LocalDate.parse(from).isAfter(LocalDate.parse(to))) {
            return showFilterError(fromField, "From date cannot be after To date");
        }
        return true;
    }

    private void clearFilterError(View field) {
        if (field instanceof TextView) {
            ((TextView) field).setError(null);
        }
    }

    private boolean showFilterError(View field, String message) {
        toast(message);
        if (field != null) {
            if (field instanceof TextView) {
                ((TextView) field).setError(message);
            }
            field.requestFocus();
        }
        return false;
    }

    private String reportDateColumn(String dateType) {
        String value = value(dateType);
        if (REPORT_DATE_EDD.equalsIgnoreCase(value)) {
            return "edd_date";
        }
        if (REPORT_DATE_SCHEDULED.equalsIgnoreCase(value)) {
            return "scheduled_delivery_date";
        }
        if (REPORT_DATE_COMPLETED.equalsIgnoreCase(value)) {
            return "final_visit";
        }
        return "entry_date";
    }

    private String reportDateLabel(String dateType) {
        String value = value(dateType);
        if (REPORT_DATE_EDD.equalsIgnoreCase(value)) {
            return REPORT_DATE_EDD;
        }
        if (REPORT_DATE_SCHEDULED.equalsIgnoreCase(value)) {
            return REPORT_DATE_SCHEDULED;
        }
        if (REPORT_DATE_COMPLETED.equalsIgnoreCase(value)) {
            return "Completed / Final Visit";
        }
        return REPORT_DATE_ENTRY;
    }

    private List<String> reportMonthOptions() {
        List<String> months = new java.util.ArrayList<>();
        months.add("All Months");
        YearMonth start = reportStartMonth();
        YearMonth month = YearMonth.now();
        while (!month.isBefore(start)) {
            months.add(month.format(REPORT_MONTH_FMT));
            month = month.minusMonths(1);
        }
        return months;
    }

    private YearMonth parseReportMonth(String monthName) {
        String selected = value(monthName);
        if (empty(selected) || "All Months".equalsIgnoreCase(selected)) {
            return null;
        }
        try {
            return YearMonth.parse(selected, REPORT_MONTH_FMT);
        } catch (Exception ignored) {
            return YearMonth.parse(selected);
        }
    }

    private boolean validReportMonth(String monthName) {
        if (empty(monthName) || "All Months".equalsIgnoreCase(value(monthName))) {
            return true;
        }
        try {
            parseReportMonth(monthName);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void applyReportMonthSelection(String monthValue, EditText from, EditText to) {
        String selected = value(monthValue);
        if (empty(selected) || "All Months".equalsIgnoreCase(selected)) {
            from.setText("");
            to.setText("");
            return;
        }
        try {
            YearMonth month = parseReportMonth(selected);
            from.setText(month.atDay(1).toString());
            to.setText(month.atEndOfMonth().toString());
        } catch (Exception ignored) {
            toast("Select a valid report month");
        }
    }

    private void showReportMonthPicker(TextView monthField, EditText from, EditText to, Runnable render) {
        List<String> months = reportMonthOptions();
        new AlertDialog.Builder(this)
                .setTitle("Select Report Month")
                .setItems(months.toArray(new String[0]), (dialog, which) -> {
                    String selected = months.get(which);
                    monthField.setText(selected);
                    applyReportMonthSelection(selected, from, to);
                    if (render != null) {
                        render.run();
                    }
                })
                .show();
    }

    private void renderNewPatientThisMonth(LinearLayout slot) {
        slot.removeAllViews();
        YearMonth current = YearMonth.now();
        String where = scopedWhere("entry_date BETWEEN ? AND ?");
        String[] args = scopedArgs(new String[]{current.atDay(1).toString(), current.atEndOfMonth().toString()});
        int count = db.countPatients(where, args);
        slot.addView(reportHeroMetric(
                "New Patient This Month",
                String.valueOf(count),
                current.format(DateTimeFormatter.ofPattern("MMM yyyy", java.util.Locale.US)),
                "Updates automatically from live sync"
        ));
    }

    private void renderReports(LinearLayout page, String from, String to, String village, String statusName, String dateType, String monthLabel) {
        page.removeAllViews();
        ReportFilter filter = reportWhere(from, to, village, statusName, dateType);
        ReportFilter baseFilter = reportWhere("", "", village, statusName, dateType);
        String where = scopedWhere(filter.where);
        String[] args = scopedArgs(filter.args);
        String baseWhere = scopedWhere(baseFilter.where);
        String[] baseArgs = scopedArgs(baseFilter.args);
        int total = db.countPatients(where, args);
        int locked = db.countPatients(appendWhere(where, "record_locked = 1"), appendArgs(args));
        int open = Math.max(0, total - locked);
        int edd30 = db.countPatients(appendWhere(where, OPEN_EDD_RANGE_WHERE), appendArgs(args, LocalDate.now().toString(), LocalDate.now().plusDays(30).toString()));
        int scheduled = db.countPatients(appendWhere(where, SCHEDULED_WHERE), args);
        int scheduledPending = db.countPatients(appendWhere(where, SCHEDULED_CALL_PENDING_WHERE), appendArgs(args, LocalDate.now().toString()));
        int deliveryCompletionDue = db.countPatients(appendWhere(where, DELIVERY_COMPLETION_DUE_WHERE), appendArgs(args, LocalDate.now().toString(), LocalDate.now().toString()));
        int scheduledNotified = db.countPatients(appendWhere(where, SCHEDULED_WHERE + " AND scheduled_delivery_called_at IS NOT NULL AND scheduled_delivery_called_at != ''"), args);
        int followupWeek = db.countPatients(appendWhere(where, FOLLOWUP_WEEK_WHERE), appendArgs(args, followupWeekArgs()));
        int today = db.countPatients(appendWhere(where, "entry_date = ?"), appendArgs(args, LocalDate.now().toString()));
        page.addView(collapsibleSection("Priority Overview", true,
                reportContextLine(from, to, monthLabel),
                reportOverviewBlock("Needs Attention",
                        stat("Complete Due", deliveryCompletionDue, v -> showPatientList(false, appendWhere(where, DELIVERY_COMPLETION_DUE_WHERE), appendArgs(args, LocalDate.now().toString(), LocalDate.now().toString()), true)),
                        stat("Call Pending", scheduledPending, v -> showPatientList(false, appendWhere(where, SCHEDULED_CALL_PENDING_WHERE), appendArgs(args, LocalDate.now().toString()), true)),
                        stat("EDD 30", edd30, v -> showPatientList(false, appendWhere(where, OPEN_EDD_RANGE_WHERE), appendArgs(args, LocalDate.now().toString(), LocalDate.now().plusDays(30).toString()), true)),
                        stat("Visit Follow-ups", followupWeek, v -> showPatientList(false, appendWhere(where, FOLLOWUP_WEEK_WHERE), appendArgs(args, followupWeekArgs()), true))
                ),
                reportOverviewBlock("Patient Records",
                        stat("Total Registered", total, v -> showPatientList(false, where, args, true)),
                        stat("Open", open, v -> showPatientList(false, appendWhere(where, "record_locked = 0"), args, true)),
                        stat("Completed", locked, v -> showPatientList(false, appendWhere(where, "record_locked = 1"), args, true)),
                        stat("Today", today, v -> showPatientList(false, appendWhere(where, "entry_date = ?"), appendArgs(args, LocalDate.now().toString()), true))
                )
        ));
        page.addView(registrationTrendChart(baseWhere, baseArgs));
        if (total == 0) {
            page.addView(collapsibleSection("Report Result", true, emptyActionState("No records match these filters", "Change the filters or create a patient record first.", "New Patient", v -> showPatientForm(null))));
            return;
        }
        page.addView(collapsibleSection("Scheduled Delivery", false,
                progressRow("Patient notified", scheduledNotified, scheduled, scheduled == 0 ? 0 : Math.round(scheduledNotified * 100f / scheduled)),
                progressRow("Call pending", scheduledPending, scheduled, scheduled == 0 ? 0 : Math.round(scheduledPending * 100f / scheduled)),
                progressRow("Completion due", deliveryCompletionDue, total, total == 0 ? 0 : Math.round(deliveryCompletionDue * 100f / total))
        ));
        LinearLayout visits = new LinearLayout(this);
        visits.setOrientation(LinearLayout.VERTICAL);
        for (String[] row : db.visitCompletionRows(where, args)) {
            int done = Integer.parseInt(row[1]);
            int pct = total == 0 ? 0 : Math.round(done * 100f / total);
            visits.addView(progressRow(row[0], done, total, pct));
        }
        page.addView(collapsibleSection("Visit Progress", false, visits));
        page.addView(villageSummaryView(where, args));
        page.addView(monthlySummaryView(baseWhere, baseArgs, dateType));
    }

    private ReportFilter reportWhere(String from, String to, String village, String statusName, String dateType) {
        List<String> clauses = new java.util.ArrayList<>();
        List<String> args = new java.util.ArrayList<>();
        String dateColumn = reportDateColumn(dateType);
        if (!empty(from)) {
            clauses.add(dateColumn + " >= ?");
            args.add(from);
        }
        if (!empty(to)) {
            clauses.add(dateColumn + " <= ?");
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

    private void openFilteredPatientSearch(String from, String to, String village, String statusName, String dateType) {
        ReportFilter filter = reportWhere(from, to, village, statusName, dateType);
        showPatientList(false, scopedWhere(filter.where), scopedArgs(filter.args), true);
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

    private Map<String, Integer> monthlySummary(String where, String[] args, String dateType) {
        java.util.LinkedHashMap<String, Integer> rows = new java.util.LinkedHashMap<>();
        YearMonth month = reportSummaryStartMonth();
        YearMonth end = YearMonth.now();
        String dateColumn = reportDateColumn(dateType);
        while (!month.isAfter(end)) {
            YearMonth current = month;
            rows.put(current.toString(), db.countPatients(
                    appendWhere(where, dateColumn + " BETWEEN ? AND ?"),
                    appendArgs(args, current.atDay(1).toString(), current.atEndOfMonth().toString())
            ));
            month = month.plusMonths(1);
        }
        return rows;
    }

    private YearMonth reportStartMonth() {
        return YearMonth.of(2026, 1);
    }

    private YearMonth reportSummaryStartMonth() {
        YearMonth rollingStart = YearMonth.now().minusMonths(11);
        YearMonth fixedStart = reportStartMonth();
        return rollingStart.isBefore(fixedStart) ? fixedStart : rollingStart;
    }

    private View monthlySummaryView(String baseWhere, String[] baseArgs, String dateType) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.addView(smallText("Monthly rows use " + reportDateLabel(dateType) + ". Tap a month to open the matching records."));
        Map<String, Integer> rows = monthlySummary(baseWhere, baseArgs, dateType);
        int max = 1;
        for (Integer value : rows.values()) {
            max = Math.max(max, value);
        }
        String dateColumn = reportDateColumn(dateType);
        for (Map.Entry<String, Integer> row : rows.entrySet()) {
            String monthKey = row.getKey();
            int value = row.getValue();
            LinearLayout monthRow = clickableProgressRow(reportMonthLabel(monthKey), value, max, Math.round(value * 100f / max));
            monthRow.setOnClickListener(v -> {
                YearMonth month = YearMonth.parse(monthKey);
                String monthWhere = appendWhere(baseWhere, dateColumn + " BETWEEN ? AND ?");
                String[] monthArgs = appendArgs(baseArgs, month.atDay(1).toString(), month.atEndOfMonth().toString());
                showPatientList(false, monthWhere, monthArgs, true);
            });
            body.addView(monthRow);
        }
        return collapsibleSection("Monthly Summary", false, body);
    }

    private String reportMonthLabel(String monthKey) {
        try {
            return YearMonth.parse(monthKey).format(REPORT_MONTH_FMT);
        } catch (Exception ignored) {
            return value(monthKey);
        }
    }

    private View registrationTrendChart(String baseWhere, String[] baseArgs) {
        Map<String, Integer> rows = monthlySummary(baseWhere, baseArgs, REPORT_DATE_ENTRY);
        int max = 1;
        for (Integer value : rows.values()) {
            max = Math.max(max, value);
        }
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chart = new LinearLayout(this);
        chart.setOrientation(LinearLayout.HORIZONTAL);
        chart.setGravity(Gravity.BOTTOM);
        chart.setPadding(0, dp(4), 0, dp(2));
        for (Map.Entry<String, Integer> row : rows.entrySet()) {
            chart.addView(trendBar(row.getKey(), row.getValue(), max));
        }
        scroll.addView(chart);
        return collapsibleSection("Registration Trend", false, smallText("New patient registrations by month."), scroll);
    }

    private View trendBar(String monthKey, int count, int max) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        column.setPadding(dp(4), 0, dp(4), 0);
        TextView number = label(String.valueOf(count), 11, true);
        number.setTextColor(PRIMARY_DARK);
        number.setGravity(Gravity.CENTER);
        int height = dp(18 + Math.round((count * 54f) / Math.max(1, max)));
        TextView bar = new TextView(this);
        bar.setBackground(rounded(count == 0 ? PRIMARY_SOFT : ACCENT, dp(6), 0, count == 0 ? PRIMARY_SOFT : ACCENT));
        TextView month = label(shortMonthLabel(monthKey), 10, true);
        month.setTextColor(MUTED);
        month.setGravity(Gravity.CENTER);
        column.addView(number, new LinearLayout.LayoutParams(-1, dp(18)));
        column.addView(bar, new LinearLayout.LayoutParams(dp(26), height));
        column.addView(month, new LinearLayout.LayoutParams(-1, dp(20)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(54), dp(104));
        lp.setMargins(0, 0, dp(4), 0);
        column.setLayoutParams(lp);
        return column;
    }

    private String shortMonthLabel(String monthKey) {
        try {
            return YearMonth.parse(monthKey).format(DateTimeFormatter.ofPattern("MMM", java.util.Locale.US));
        } catch (Exception ignored) {
            return value(monthKey);
        }
    }

    private View villageSummaryView(String where, String[] args) {
        Map<String, Integer> rows = db.countBy("village_name", where, args);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        if (rows.isEmpty()) {
            body.addView(emptyState("No data", "Village totals will appear after matching patients are saved."));
            return collapsibleSection("Patients by Village", false, body);
        }
        int max = 1;
        for (Integer value : rows.values()) {
            max = Math.max(max, value);
        }
        for (Map.Entry<String, Integer> row : rows.entrySet()) {
            String village = value(row.getKey());
            int count = row.getValue();
            LinearLayout item = clickableProgressRow("-".equals(village) || empty(village) ? "Unknown Village" : village, count, max, Math.round(count * 100f / max));
            item.setOnClickListener(v -> {
                if ("-".equals(village) || empty(village)) {
                    showPatientList(false, appendWhere(where, "village_name IS NULL OR village_name = ''"), args, true);
                } else {
                    showPatientList(false, appendWhere(where, "village_name = ?"), appendArgs(args, village), true);
                }
            });
            body.addView(item);
        }
        return collapsibleSection("Patients by Village", false, body);
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

    private void startFilteredExport(String from, String to, String village, String statusName, String dateType, int requestCode, String fileName, String mimeType) {
        ReportFilter filter = reportWhere(from, to, village, statusName, dateType);
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
            if (y > pageHeight - 112) {
                document.finishPage(page);
                page = newPdfPage(document, pageWidth, pageHeight, ++pageNo);
                y = drawPdfHeader(page, titlePaint, textPaint, margin, pageNo);
            }
            page.getCanvas().drawText(value(p.patientId) + " | " + value(p.patientName), margin, y, textPaint);
            y += 14;
            page.getCanvas().drawText("Mobile: " + value(p.mobileNumber) + " | Block: " + value(p.localBodyName) + " | Village: " + value(p.villageName), margin, y, textPaint);
            y += 14;
            page.getCanvas().drawText("Age: " + value(p.age) + " | Blood group: " + value(p.bloodGroup) + " | GRAVIDA: " + value(p.gravida), margin, y, textPaint);
            y += 14;
            page.getCanvas().drawText("Last delivery method: " + value(p.lastDeliveryMethod), margin, y, textPaint);
            y += 14;
            page.getCanvas().drawText("LMP: " + value(p.lmpDate) + " | Pregnancy age: " + pregnancyAgeText(p) + " | EDD: " + value(p.eddDate), margin, y, textPaint);
            y += 14;
            page.getCanvas().drawText("Scheduled: " + value(p.scheduledDeliveryDate), margin, y, textPaint);
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
        return new String[]{"Serial", "Entry Date", "Patient Name", "Patient ID", "Age", "Blood Group", "State", "District", "Block", "Village", "Mobile", "Motivator", "Doctor", "GRAVIDA", "Last Delivery Method", "LMP", "Pregnancy Age", "EDD", "Scheduled Delivery", "Scheduled Call At", "Scheduled Call By", "1st Visit", "2nd Visit", "3rd Visit", "Final Visit", "Locked", "Exported At", "Exported By"};
    }

    private String[] patientExportValues(Patient p) {
        return new String[]{String.valueOf(p.serialNumber), value(p.entryDate), value(p.patientName), value(p.patientId), value(p.age), value(p.bloodGroup), value(p.stateName), value(p.districtName), value(p.localBodyName), value(p.villageName), value(p.mobileNumber), value(p.motivatorName), value(p.doctorName), value(p.gravida), value(p.lastDeliveryMethod), value(p.lmpDate), pregnancyAgeText(p), value(p.eddDate), value(p.scheduledDeliveryDate), value(p.scheduledDeliveryCalledAt), value(p.scheduledDeliveryCalledBy), value(p.visit1), value(p.visit2), value(p.visit3), value(p.finalVisit), p.recordLocked ? "1" : "0", LocalDateTime.now().format(TIME_FMT), value(currentUser)};
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

    private void showPatientRecovery() {
        if (!isAdmin()) {
            toast("Only admin can open patient recovery");
            showDashboard();
            return;
        }
        setPage("Patient Recovery");
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(page);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));

        List<Patient> deleted = db.listDeletedPatients();
        page.addView(section("Patient Recovery",
                compactKpiRow(
                        focusCard("Recoverable", deleted.size(), "Hidden patient records", WARNING, v -> toast("Recoverable records: " + deleted.size())),
                        focusCard("Active", db.countPatients(null, null), "Visible hospital records", PRIMARY, v -> showPatientList(true))
                ),
                smallText("Deleted patients are hidden from dashboard, search, reports, exports, and priority queues until an admin restores them.")
        ));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        if (deleted.isEmpty()) {
            list.addView(emptyState("No recoverable patients", "Accidentally deleted records will appear here for admin restore."));
        } else {
            for (Patient p : deleted) {
                list.addView(patientRecoveryCard(p));
            }
        }
        page.addView(section("Recover Deleted Patients", list));
    }

    private View patientRecoveryCard(Patient p) {
        LinearLayout card = card();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.VERTICAL);
        TextView name = label(value(p.patientName), 17, true);
        name.setTextColor(PRIMARY_DARK);
        TextView meta = label(value(p.patientId) + " | " + value(p.mobileNumber), 12, true);
        meta.setTextColor(SLATE);
        title.addView(name);
        title.addView(meta);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(chip("RECOVERY", WARNING, Color.WHITE));
        card.addView(top);
        card.addView(statusLine("Address", value(p.villageName), value(p.localBodyName), PRIMARY));
        card.addView(statusLine("Pregnancy", "EDD " + value(p.eddDate), "Scheduled " + optionalValue(p.scheduledDeliveryDate), WARNING));
        card.addView(statusLine("Deleted", deletedMeta(p.deletedAt), "By " + optionalValue(p.deletedBy), URGENT));
        card.addView(scrollingActions(
                navButton("Restore Patient", v -> confirmRestorePatient(db.getPatient(p.id))),
                button("Back to Admin", v -> showAdmin())
        ));
        return card;
    }

    private String deletedMeta(String deletedAt) {
        return empty(deletedAt) ? "Unknown time" : value(deletedAt).replace('T', ' ');
    }

    private void showAdmin() {
        if (!isAdmin()) {
            toast("Only admin can open administration");
            showDashboard();
            return;
        }
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
        int deleted = db.countDeletedPatients();
        int doctors = db.listNames("custom_doctors").size();
        int motivators = db.listNames("custom_motivators").size();
        page.addView(adminHero(total, locked, scheduled, callPending, deleted));
        page.addView(section("Operations Control",
                compactTwoColumn(
                        adminCommandPanel("Patient Records", "Search, edit, complete, unlock, and remove hospital records.", "Open Patients", PRIMARY, v -> showPatientList(true)),
                        adminCommandPanel("Priority Follow-up", "Scheduled calls, delivery completion, and planned visit tracking.", "Open Priority", URGENT, v -> showPatientList(true, adminPriorityWhere(), adminPriorityArgs()))
                ),
                compactTwoColumn(
                        adminCommandPanel("Reports", "Open scheduled delivery, EDD, village, and monthly summaries.", "Open Reports", ACCENT, v -> showReports()),
                        adminCommandPanel("Export Center", "Create Excel and PDF files for staff and hospital records.", "Open Export", SLATE, v -> showExportCenter())
                )
        ));
        page.addView(section("Data and Recovery",
                compactTwoColumn(
                        adminCommandPanel("Patient Recovery", deleted + " recoverable deleted record(s). Restore accidental deletes here.", "Open Recovery", WARNING, v -> showPatientRecovery()),
                        adminCommandPanel("Backup Manager", "Create or restore a local database backup when required.", "Open Backup", PRIMARY, v -> showBackup())
                ),
                adminCommandPanel("Full Export", "Export all Blue Bird records directly as Excel or PDF.", "Export Excel", ACCENT, v -> startExport("", null, null, REQ_EXPORT_EXCEL, "all_patients.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
                scrollingActions(
                        button("Export PDF", v -> startExport("", null, null, REQ_EXPORT_PDF, "all_patients.pdf", "application/pdf")),
                        button("Save Backup File", v -> startBackupExport())
                ),
                smallText("Use exports for reporting and backups only for recovery or handover.")
        ));
        page.addView(section("Access Control",
                compactTwoColumn(
                        adminCommandPanel("Create Staff Login", "Only admins can create new app users and assign roles.", "Add User", PRIMARY, v -> addUserDialog()),
                        adminCommandPanel("User Review", "Remove app access for users who should no longer sync records.", "Review Users", WARNING, v -> toast("Review the user list below"))
                ),
                usersView(),
                navButton("Add User", v -> addUserDialog())
        ));
        page.addView(section("Reference Lists",
                readinessRow(
                        readinessPill("Doctors", String.valueOf(doctors), PRIMARY),
                        readinessPill("Motivators", String.valueOf(motivators), SLATE)
                ),
                compactTwoColumn(
                        adminCommandPanel("Doctor Names", "Maintain the doctor dropdown used in patient entry.", "Edit Doctors", PRIMARY, v -> showReferenceDialog("custom_doctors", "Doctor Names")),
                        adminCommandPanel("Motivator Names", "Optional field support for staff who still use motivators.", "Edit Motivators", SLATE, v -> showReferenceDialog("custom_motivators", "Motivator Names"))
                )
        ));
        page.addView(section("App Support",
                readinessRow(
                        readinessPill("Version", BuildConfig.VERSION_NAME, ACCENT),
                        readinessPill("Sync", value(syncBadge == null ? "SYNCING" : syncBadge.getText().toString()), PRIMARY),
                        readinessPill("Role", value(currentRole), SLATE)
                ),
                smallText("Support note: " + lastSyncText),
                compactTwoColumn(
                        adminCommandPanel("App Update", "Download, verify, and install the newest APK inside the app.", "Open Update", ACCENT, v -> showUpdateCenter()),
                        adminCommandPanel("Installed Version", "Current app release: " + BuildConfig.VERSION_NAME + ".", "Check Now", SLATE, v -> showUpdateCenter())
                )
        ));
        page.addView(collapsibleSection("Audit Trail", false, changeLogView()));
    }

    private View adminHero(int total, int locked, int scheduled, int callPending, int deleted) {
        return section("Admin Overview",
                compactKpiRow(
                        focusCard("Records", total, "All hospital records", PRIMARY, v -> showPatientList(true)),
                        focusCard("Completed", locked, "Locked patient records", ACCENT, v -> showPatientList(true, "record_locked = 1", null)),
                        focusCard("Scheduled", scheduled, "Doctor-given delivery dates", WARNING, v -> showPatientList(true, SCHEDULED_WHERE, null)),
                        focusCard("Calls", callPending, "Pending scheduled calls", URGENT, v -> showPatientList(true, SCHEDULED_CALL_PENDING_WHERE, new String[]{LocalDate.now().toString()})),
                        focusCard("Recovery", deleted, "Deleted records", WARNING, v -> showPatientRecovery())
                )
        );
    }

    private View adminCommandPanel(String title, String message, String action, int color, View.OnClickListener listener) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD));
        panel.setBackground(rounded(Color.argb(138, 255, 255, 255), dp(CARD_RADIUS), dp(1), Color.argb(210, 255, 255, 255)));
        TextView rail = new TextView(this);
        rail.setBackground(rounded(color, dp(4), 0, color));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(SPACE_SM), 0, dp(SPACE_SM), 0);
        TextView heading = label(title, 13, true);
        heading.setTextColor(PRIMARY_DARK);
        TextView body = smallText(message);
        body.setTextColor(MUTED);
        copy.addView(heading);
        copy.addView(body);
        Button cta = button(action, listener);
        panel.addView(rail, new LinearLayout.LayoutParams(dp(4), dp(56)));
        panel.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        panel.addView(cta);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, dp(7), dp(7));
        panel.setLayoutParams(lp);
        return panel;
    }

    private String adminPriorityWhere() {
        return "(" + DELIVERY_COMPLETION_DUE_WHERE + ") OR (" + SCHEDULED_WEEK_WHERE + ") OR (" + SCHEDULED_CALL_PENDING_WHERE + ") OR (" + FOLLOWUP_WEEK_WHERE + ")";
    }

    private String[] adminPriorityArgs() {
        String today = LocalDate.now().toString();
        String week = LocalDate.now().plusDays(7).toString();
        return appendArgs(new String[]{today, today, today, week, today}, followupWeekArgs());
    }

    private void checkForAppUpdate() {
        showUpdateCenter();
    }

    private void showUpdateCenter() {
        setPage("App Update");
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(page);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));

        LinearLayout updateSlot = new LinearLayout(this);
        updateSlot.setOrientation(LinearLayout.VERTICAL);
        page.addView(section("Update Center",
                updateStatusCard(
                        "Installed Version",
                        "Current app version: " + BuildConfig.VERSION_NAME + "\nUpdates download inside Blue Bird Hospital and then open Android's installer for final confirmation.",
                        PRIMARY
                ),
                updateSlot,
                scrollingActions(
                        button("Check Now", v -> loadUpdateCenter(updateSlot)),
                        button("Back to Admin", v -> {
                            if (isAdmin()) {
                                showAdmin();
                            } else {
                                showDashboard();
                            }
                        })
                )
        ));
        loadUpdateCenter(updateSlot);
    }

    private void loadUpdateCenter(LinearLayout slot) {
        slot.removeAllViews();
        slot.addView(updateStatusCard(
                "Checking Latest Update",
                "Contacting the secure release source. This should take only a moment.",
                ACCENT
        ));
        new Thread(() -> {
            try {
                UpdateInfo update = fetchLatestUpdate();
                runOnUiThread(() -> renderUpdateCenter(slot, update));
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    slot.removeAllViews();
                    slot.addView(updateStatusCard(
                            "Update Check Failed",
                            "Could not check for update: " + ex.getMessage(),
                            URGENT,
                            button("Try Again", v -> loadUpdateCenter(slot))
                    ));
                });
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
        String apkUrl = "";
        String apkName = "";
        String digest = "";
        long apkSize = 0;
        JSONArray assets = json.optJSONArray("assets");
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String name = asset.optString("name", "");
                if (name.toLowerCase(Locale.US).endsWith(".apk")) {
                    apkUrl = asset.optString("browser_download_url", "");
                    apkName = name;
                    apkSize = asset.optLong("size", 0);
                    digest = asset.optString("digest", "");
                    break;
                }
            }
        }
        return new UpdateInfo(tag, apkUrl, apkName, apkSize, digest);
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

    private void renderUpdateCenter(LinearLayout slot, UpdateInfo update) {
        slot.removeAllViews();
        if (empty(update.tag)) {
            slot.addView(updateStatusCard(
                    "No Release Found",
                    "The update source did not return a valid app version.",
                    WARNING,
                    button("Try Again", v -> loadUpdateCenter(slot))
            ));
            return;
        }
        String latest = update.tag.startsWith("v") ? update.tag.substring(1) : update.tag;
        if (compareVersions(latest, BuildConfig.VERSION_NAME) <= 0) {
            slot.addView(updateStatusCard(
                    "App Is Up To Date",
                    "Current version: " + BuildConfig.VERSION_NAME + "\nLatest version: " + update.tag + "\nNo staff action is required.",
                    ACCENT,
                    button("Check Again", v -> loadUpdateCenter(slot))
            ));
            return;
        }
        if (empty(update.apkUrl)) {
            slot.addView(updateStatusCard(
                    "Update Found, APK Missing",
                    "Latest version: " + update.tag + "\nThe release does not include an APK asset for in-app installation.",
                    URGENT,
                    button("Check Again", v -> loadUpdateCenter(slot))
            ));
            return;
        }
        slot.addView(updateStatusCard(
                "Update Available",
                "Current version: " + BuildConfig.VERSION_NAME
                        + "\nLatest version: " + update.tag
                        + "\nFile: " + value(update.apkName)
                        + "\nSize: " + formatBytes(update.apkSize)
                        + "\nVerification: " + (empty(update.digest) ? "package check only" : "SHA-256 + package check"),
                WARNING,
                button("Download Update", v -> downloadAndInstallUpdate(update)),
                button("Check Again", v -> loadUpdateCenter(slot))
        ));
    }

    private View updateStatusCard(String title, String message, int color, View... actions) {
        LinearLayout box = card(Color.argb(74, Color.red(color), Color.green(color), Color.blue(color)), 1, color);
        TextView heading = label(title, 16, true);
        heading.setTextColor(PRIMARY_DARK);
        TextView body = label(message, 13, true);
        body.setTextColor(TEXT);
        body.setPadding(0, dp(4), 0, dp(6));
        box.addView(heading);
        box.addView(body);
        if (actions != null && actions.length > 0) {
            box.addView(scrollingActions(actions));
        }
        return box;
    }

    private void downloadAndInstallUpdate(UpdateInfo update) {
        if (requiresInstallPermission()) {
            showInstallPermissionDialog();
            return;
        }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(SPACE_SM), dp(SPACE_SM), dp(SPACE_SM), 0);
        TextView statusText = label("Preparing download...", 13, true);
        statusText.setTextColor(TEXT);
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setIndeterminate(update.apkSize <= 0);
        box.addView(statusText);
        box.addView(progress, new LinearLayout.LayoutParams(-1, dp(28)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Downloading Update")
                .setView(box)
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();

        new Thread(() -> {
            try {
                File apk = downloadUpdateApk(update, progress, statusText);
                runOnUiThread(() -> statusText.setText("Verifying update..."));
                verifyDownloadedUpdate(apk, update);
                runOnUiThread(() -> {
                    dialog.dismiss();
                    showInstallReadyDialog(apk, update);
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    new AlertDialog.Builder(this)
                            .setTitle("Update Failed")
                            .setMessage(ex.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        }).start();
    }

    private File downloadUpdateApk(UpdateInfo update, ProgressBar progress, TextView statusText) throws Exception {
        File dir = updateApkDir();
        File apk = new File(dir, safeApkName(update));
        HttpURLConnection connection = (HttpURLConnection) new URL(update.apkUrl).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("Accept", "application/octet-stream");
        connection.setRequestProperty("User-Agent", "BlueBirdHospital/" + BuildConfig.VERSION_NAME);
        try {
            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException("Download failed with HTTP " + statusCode);
            }
            long total = connection.getContentLengthLong();
            if (total <= 0) {
                total = update.apkSize;
            }
            final long totalBytes = total;
            runOnUiThread(() -> {
                progress.setIndeterminate(totalBytes <= 0);
                statusText.setText("Downloading update...");
            });
            try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(apk, false)) {
                byte[] buffer = new byte[8192];
                long downloaded = 0;
                int lastPercent = -1;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    downloaded += read;
                    if (totalBytes > 0) {
                        int percent = (int) Math.min(100, Math.round(downloaded * 100f / totalBytes));
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            int visiblePercent = percent;
                            long visibleDownloaded = downloaded;
                            runOnUiThread(() -> {
                                progress.setProgress(visiblePercent);
                                statusText.setText("Downloading update " + visiblePercent + "% (" + formatBytes(visibleDownloaded) + " of " + formatBytes(totalBytes) + ")");
                            });
                        }
                    } else {
                        long visibleDownloaded = downloaded;
                        runOnUiThread(() -> statusText.setText("Downloading update... " + formatBytes(visibleDownloaded)));
                    }
                }
            }
        } finally {
            connection.disconnect();
        }
        return apk;
    }

    private void verifyDownloadedUpdate(File apk, UpdateInfo update) throws Exception {
        if (!apk.exists() || apk.length() == 0) {
            throw new IllegalStateException("Downloaded APK is empty");
        }
        String expected = normalizedSha256(update.digest);
        if (!empty(expected)) {
            String actual = sha256(apk);
            if (!expected.equals(actual)) {
                throw new IllegalStateException("Update verification failed. SHA-256 does not match.");
            }
        }
        PackageInfo packageInfo = getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(), 0);
        if (packageInfo == null) {
            throw new IllegalStateException("Downloaded file is not a valid Android APK");
        }
        if (!getPackageName().equals(packageInfo.packageName)) {
            throw new IllegalStateException("Downloaded APK package does not match Blue Bird Hospital");
        }
        if (!empty(packageInfo.versionName) && compareVersions(packageInfo.versionName, BuildConfig.VERSION_NAME) <= 0) {
            throw new IllegalStateException("Downloaded APK is not newer than the installed app");
        }
    }

    private void showInstallReadyDialog(File apk, UpdateInfo update) {
        new AlertDialog.Builder(this)
                .setTitle("Update Ready")
                .setMessage("Version " + update.tag + " downloaded and verified inside the app.\n\nAndroid will ask for final install confirmation.")
                .setPositiveButton("Install Update", (dialog, which) -> installUpdateApk(apk))
                .setNegativeButton("Later", null)
                .show();
    }

    private void installUpdateApk(File apk) {
        if (requiresInstallPermission()) {
            showInstallPermissionDialog();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apk);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception ex) {
            toast("Installer unavailable: " + ex.getMessage());
        }
    }

    private boolean requiresInstallPermission() {
        return !getPackageManager().canRequestPackageInstalls();
    }

    private void showInstallPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Allow In-App Updates")
                .setMessage("Android requires one-time permission before Blue Bird Hospital can open its downloaded update installer.\n\nEnable install permission, then return to the app and tap Download Update again.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } catch (Exception ex) {
                        toast("Cannot open install settings: " + ex.getMessage());
                    }
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private File updateApkDir() {
        File dir = new File(getCacheDir(), "updates");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private String safeApkName(UpdateInfo update) {
        String name = empty(update.apkName) ? "blue_bird_hospital_" + value(update.tag) + ".apk" : update.apkName;
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String normalizedSha256(String digest) {
        String value = value(digest).toLowerCase(Locale.US).trim();
        if (value.startsWith("sha256:")) {
            value = value.substring("sha256:".length());
        }
        return value.matches("[0-9a-f]{64}") ? value : "";
    }

    private String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        StringBuilder out = new StringBuilder();
        for (byte b : digest.digest()) {
            out.append(String.format(Locale.US, "%02x", b));
        }
        return out.toString();
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "Unknown";
        }
        double mb = bytes / (1024d * 1024d);
        if (mb >= 1d) {
            return String.format(Locale.US, "%.1f MB", mb);
        }
        double kb = bytes / 1024d;
        return String.format(Locale.US, "%.0f KB", kb);
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

    private static final class UpdateInfo {
        final String tag;
        final String apkUrl;
        final String apkName;
        final long apkSize;
        final String digest;

        UpdateInfo(String tag, String apkUrl, String apkName, long apkSize, String digest) {
            this.tag = tag;
            this.apkUrl = apkUrl;
            this.apkName = apkName;
            this.apkSize = apkSize;
            this.digest = digest;
        }
    }

    private void showReferenceDialog(String table, String title) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(SPACE_SM), dp(SPACE_SM), dp(SPACE_SM), 0);
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        ScrollView listScroll = new ScrollView(this);
        listScroll.setVerticalScrollBarEnabled(false);
        listScroll.addView(list);
        final Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            header.removeAllViews();
            header.addView(referenceHeader(title, table));
            list.removeAllViews();
            list.addView(namesView(table, title, refresh[0]));
        };
        box.addView(header);
        box.addView(listScroll, new LinearLayout.LayoutParams(-1, dp(310)));
        box.addView(adminCommandPanel(
                "Add " + referenceEntity(title),
                "New " + referenceEntity(title).toLowerCase(java.util.Locale.US) + " names are saved in uppercase and appear in the patient entry dropdown.",
                "Add " + referenceEntity(title),
                PRIMARY,
                v -> addNameDialog(table, referenceSingular(title), refresh[0])
        ));
        refresh[0].run();
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(box)
                .setNegativeButton("Close", null)
                .show();
    }

    private View referenceHeader(String title, String table) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD));
        box.setBackground(prominentPanel());
        TextView heading = label(referenceEntity(title) + " Manager", 15, true);
        heading.setTextColor(Color.WHITE);
        TextView sub = label(db.listNames(table).size() + " saved | Dropdown ready", 11, true);
        sub.setTextColor(Color.argb(225, 255, 255, 255));
        box.addView(heading);
        box.addView(sub);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(CARD_GAP));
        box.setLayoutParams(lp);
        return box;
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

    private View namesView(String table, String title, Runnable afterChange) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        List<String> rows = uppercaseList(db.listNames(table));
        if (rows.isEmpty()) {
            list.addView(emptyState("No " + referenceEntity(title).toLowerCase(java.util.Locale.US) + " names saved", "Add names here so staff can select them quickly in patient entry."));
            return list;
        }
        for (String name : rows) {
            LinearLayout row = card(Color.argb(185, 255, 255, 255), 1, Color.argb(225, 255, 255, 255));
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(SPACE_MD), dp(SPACE_SM), dp(SPACE_MD), dp(SPACE_SM));
            TextView rail = new TextView(this);
            rail.setBackground(rounded(PRIMARY, dp(4), 0, PRIMARY));
            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            details.setPadding(dp(SPACE_SM), 0, dp(SPACE_SM), 0);
            TextView nameView = label(name, 13, true);
            nameView.setTextColor(PRIMARY_DARK);
            TextView meta = label(referenceEntity(title) + " dropdown option", 10, false);
            meta.setTextColor(MUTED);
            details.addView(nameView);
            details.addView(meta);
            row.addView(rail, new LinearLayout.LayoutParams(dp(4), dp(40)));
            row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(button("Remove", v -> confirmRemoveReferenceName(table, title, name, afterChange)));
            list.addView(row);
        }
        return list;
    }

    private void confirmRemoveReferenceName(String table, String title, String name, Runnable afterChange) {
        new AlertDialog.Builder(this)
                .setTitle("Remove " + referenceEntity(title))
                .setMessage("Remove " + name + " from the " + referenceEntity(title).toLowerCase(java.util.Locale.US) + " dropdown?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    db.deleteName(table, name);
                    toast(referenceEntity(title) + " removed");
                    if (afterChange != null) {
                        afterChange.run();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        addNameDialog(table, title, null);
    }

    private void addNameDialog(String table, String title, Runnable afterSave) {
        EditText input = input("");
        input.setHint(title.toUpperCase(java.util.Locale.US));
        applyUppercaseFilter(input);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add " + title)
                .setView(row(title, input))
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (empty(text(input))) {
                input.setError(title + " is required");
                input.requestFocus();
                toast(title + " is required");
                return;
            }
            try {
                db.addName(table, entryText(input));
                dialog.dismiss();
                toast(title + " added");
                if (afterSave != null) {
                    afterSave.run();
                } else {
                    showAdmin();
                }
            } catch (Exception ex) {
                toast("Could not save " + title.toLowerCase(java.util.Locale.US) + ": " + ex.getMessage());
            }
        }));
        dialog.show();
    }

    private String referenceEntity(String title) {
        return title.replace(" Names", "");
    }

    private String referenceSingular(String title) {
        return title.replace(" Names", " Name");
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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Create App User")
                .setView(box)
                .setPositiveButton("Create", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String emailValue = text(email);
            String passwordValue = text(password);
            String roleValue = text(role);
            email.setError(null);
            password.setError(null);
            role.setError(null);
            if (empty(emailValue) || !emailValue.contains("@") || !emailValue.contains(".")) {
                email.setError("Enter a valid email address");
                email.requestFocus();
                toast("Enter a valid email address");
                return;
            }
            if (empty(passwordValue) || passwordValue.length() < 6) {
                password.setError("Password must be at least 6 characters");
                password.requestFocus();
                toast("Password must be at least 6 characters");
                return;
            }
            if (!"STAFF".equalsIgnoreCase(roleValue) && !"ADMIN".equalsIgnoreCase(roleValue)) {
                role.setError("Role must be STAFF or ADMIN");
                role.requestFocus();
                toast("Role must be STAFF or ADMIN");
                return;
            }
            Button create = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            create.setEnabled(false);
            firebase.createAuthUserAndRole(emailValue, passwordValue, roleValue, (unused, error) -> runOnUiThread(() -> {
                create.setEnabled(true);
                if (error != null) {
                    toast("Could not create user: " + error.getMessage());
                    return;
                }
                db.logActivity("USER_CREATE", emailValue, currentUser);
                toast("User created");
                dialog.dismiss();
                showAdmin();
            }));
        }));
        dialog.show();
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
        setSpinnerOptions(localBody, db.listLocalBodies(selectedDistrictCode), "Select block");
        if (clear) {
            subdistrict.setText("", false);
            localBody.setSelection(0);
        }
        refreshWardAndVillageAdapters(clear);
    }

    private void refreshWardAndVillageAdapters(boolean clear) {
        selectedLocalBodyCode = db.getCodeByNameAndParent("local_bodies", selectedSpinnerValue(localBody), "district_code", selectedDistrictCode);
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

    private void refreshPregnancyAgePreview(Patient patient) {
        if (pregnancyAge == null) {
            return;
        }
        boolean locked = patient != null && patient.recordLocked;
        pregnancyAge.setText(pregnancyAgeText(
                text(lmpDate),
                locked,
                finalVisit == null ? "" : text(finalVisit),
                scheduledDeliveryDate == null ? "" : text(scheduledDeliveryDate)
        ));
    }

    private String pregnancyAgeText(Patient patient) {
        if (patient == null) {
            return pregnancyAgeText(text(lmpDate), false, "", "");
        }
        return pregnancyAgeText(patient.lmpDate, patient.recordLocked, patient.finalVisit, patient.scheduledDeliveryDate);
    }

    private String pregnancyAgeText(String lmpValue, boolean recordLocked, String finalVisitValue, String scheduledDeliveryValue) {
        if (empty(lmpValue)) {
            return "Select LMP date";
        }
        if (!PatientRules.validDate(lmpValue)) {
            return "Use YYYY-MM-DD";
        }
        LocalDate lmp = LocalDate.parse(lmpValue);
        LocalDate end = pregnancyAgeEndDate(recordLocked, finalVisitValue, scheduledDeliveryValue);
        if (end.isBefore(lmp)) {
            return "Check LMP date";
        }
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(lmp, end);
        long weeks = totalDays / 7;
        long days = totalDays % 7;
        return weeks + " " + (weeks == 1 ? "week" : "weeks") + " " + days + " " + (days == 1 ? "day" : "days");
    }

    private LocalDate pregnancyAgeEndDate(boolean recordLocked, String finalVisitValue, String scheduledDeliveryValue) {
        LocalDate today = LocalDate.now();
        LocalDate end = today;
        if (recordLocked) {
            if (PatientRules.validDate(finalVisitValue) && !empty(finalVisitValue)) {
                end = LocalDate.parse(finalVisitValue);
            } else if (PatientRules.validDate(scheduledDeliveryValue) && !empty(scheduledDeliveryValue)) {
                end = LocalDate.parse(scheduledDeliveryValue);
            }
        }
        return end.isAfter(today) ? today : end;
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
        TextView heading = label(title, 15, true);
        heading.setTextColor(PRIMARY_DARK);
        head.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        head.setPadding(dp(SPACE_SM), dp(6), dp(SPACE_SM), dp(6));
        head.setBackground(rounded(PRIMARY_SOFT, dp(6), 0, PRIMARY_SOFT));
        box.addView(head);
        box.addView(verticalGap(SPACE_SM));
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
        head.setPadding(dp(SPACE_SM), dp(5), dp(2), dp(5));
        head.setBackground(rounded(PRIMARY_SOFT, dp(6), 0, PRIMARY_SOFT));
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
        body.setPadding(0, dp(SPACE_SM), 0, 0);
        box.addView(body);
        return box;
    }

    private LinearLayout formStep(String number, String title, boolean expanded, View... rows) {
        LinearLayout box = card();
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(SPACE_SM), dp(6), dp(2), dp(6));
        head.setBackground(rounded(PRIMARY_SOFT, dp(6), 0, PRIMARY_SOFT));
        TextView badge = label(number, 14, true);
        badge.setTextColor(Color.WHITE);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(gradient(PRIMARY, ACCENT, dp(CHIP_RADIUS)));
        TextView heading = label(title, 15, true);
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
        body.setPadding(0, dp(SPACE_SM), 0, 0);
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

    private LinearLayout reportTwoColumn(View left, View right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, -2, 1);
        leftLp.setMargins(0, 0, dp(4), 0);
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, -2, 1);
        rightLp.setMargins(dp(4), 0, 0, 0);
        row.addView(left, leftLp);
        row.addView(right, rightLp);
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

    private View dashboardActions(View... actions) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        int columns = getResources().getConfiguration().screenWidthDp < 600 ? 2 : 3;
        LinearLayout row = null;
        for (int i = 0; i < actions.length; i++) {
            if (i % columns == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                wrap.addView(row, new LinearLayout.LayoutParams(-1, -2));
            }
            View action = actions[i];
            if (action instanceof TextView) {
                TextView text = (TextView) action;
                text.setSingleLine(true);
                text.setEllipsize(android.text.TextUtils.TruncateAt.END);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(BUTTON_HEIGHT), 1);
            lp.setMargins(dp(SPACE_XS), dp(SPACE_XS), dp(SPACE_XS), dp(SPACE_XS));
            row.addView(action, lp);
        }
        return wrap;
    }

    private View bottomNavItem(String pageName, int iconRes, String text, View.OnClickListener listener) {
        boolean active = currentPage.equals(pageName) || (currentPage.equals("Edit Patient") && pageName.equals("Patient Entry")) || (currentPage.equals("Patient Detail") && pageName.equals("Patient Search")) || ((currentPage.equals("Patient Management") || currentPage.equals("Patient Recovery")) && pageName.equals("Administration"));
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(2), dp(4), dp(2), dp(3));
        item.setContentDescription(text);
        item.setBackground(rounded(active ? PRIMARY_SOFT : Color.TRANSPARENT, dp(BUTTON_RADIUS), 0, Color.TRANSPARENT));
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(active ? ACCENT : PRIMARY_DARK);
        TextView caption = label(text, 10, true);
        caption.setGravity(Gravity.CENTER);
        caption.setSingleLine(true);
        caption.setTextColor(active ? PRIMARY : PRIMARY_DARK);
        item.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));
        item.addView(caption, new LinearLayout.LayoutParams(-1, -2));
        TextView indicator = new TextView(this);
        indicator.setBackground(rounded(active ? ACCENT : Color.TRANSPARENT, dp(2), 0, Color.TRANSPARENT));
        LinearLayout.LayoutParams indicatorLp = new LinearLayout.LayoutParams(dp(24), dp(3));
        indicatorLp.setMargins(0, dp(2), 0, 0);
        item.addView(indicator, indicatorLp);
        item.setOnClickListener(listener);
        attachPressAnimation(item, 0.96f);
        return item;
    }

    private View emptyState(String title, String message) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD));
        box.setBackground(rounded(Color.argb(118, 255, 255, 255), dp(CARD_RADIUS), dp(1), Color.argb(185, 255, 255, 255)));
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = chip("Info", PRIMARY_SOFT, PRIMARY_DARK);
        TextView t = label(title, 15, true);
        t.setTextColor(PRIMARY_DARK);
        head.addView(mark);
        head.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        TextView m = label(message, 13, true);
        m.setTextColor(SLATE);
        m.setPadding(0, dp(4), 0, 0);
        box.addView(head);
        box.addView(m);
        return box;
    }

    private View emptyActionState(String title, String message, String action, View.OnClickListener listener) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD));
        box.setBackground(rounded(Color.argb(118, 255, 255, 255), dp(CARD_RADIUS), dp(1), Color.argb(185, 255, 255, 255)));
        TextView t = label(title, 15, true);
        t.setTextColor(PRIMARY_DARK);
        TextView m = label(message, 13, true);
        m.setTextColor(SLATE);
        m.setPadding(0, dp(4), 0, dp(6));
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

    private LinearLayout clickableProgressRow(String title, int done, int total, int pct) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(8), dp(10), dp(8));
        box.setBackground(rounded(Color.argb(92, 255, 255, 255), dp(10), dp(1), Color.argb(160, 255, 255, 255)));
        attachPressAnimation(box, 0.98f);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = label(title, 14, true);
        label.setTextColor(PRIMARY_DARK);
        TextView count = label(done + " record(s)", 13, true);
        count.setTextColor(ACCENT);
        count.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        TextView arrow = label(">", 17, true);
        arrow.setTextColor(PRIMARY);
        arrow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        top.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(count, new LinearLayout.LayoutParams(dp(104), -2));
        top.addView(arrow, new LinearLayout.LayoutParams(dp(22), -2));
        LinearLayout fill = new LinearLayout(this);
        fill.setBackground(rounded(ACCENT, dp(4), 0, ACCENT));
        LinearLayout.LayoutParams fillLp = new LinearLayout.LayoutParams(0, dp(6), Math.max(1, pct));
        LinearLayout.LayoutParams restLp = new LinearLayout.LayoutParams(0, dp(6), Math.max(1, 100 - pct));
        LinearLayout meter = new LinearLayout(this);
        meter.setOrientation(LinearLayout.HORIZONTAL);
        meter.setBackground(rounded(PRIMARY_SOFT, dp(4), 0, PRIMARY_SOFT));
        meter.addView(fill, fillLp);
        meter.addView(new TextView(this), restLp);
        TextView detail = smallText(pct + "% of highest row");
        detail.setTextColor(MUTED);
        box.addView(top);
        box.addView(detail);
        box.addView(meter);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(6));
        box.setLayoutParams(lp);
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

    private View verticalGap(int heightDp) {
        TextView gap = new TextView(this);
        gap.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(heightDp)));
        return gap;
    }

    private LinearLayout row(String label, View input) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(2), dp(SPACE_XS), dp(2), dp(SPACE_XS));
        TextView tv = label(label, 13, true);
        tv.setTextColor(SLATE);
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
        int border = strokeWidth == 0 ? 0 : (color == SURFACE ? BORDER : Color.argb(170, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor)));
        box.setBackground(rounded(color, dp(CARD_RADIUS), strokeWidth == 0 ? 0 : dp(strokeWidth), border));
        box.setPadding(dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD), dp(SPACE_MD));
        box.setElevation(dp(1));
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
        edit.setTextSize(16);
        edit.setTypeface(edit.getTypeface(), Typeface.BOLD);
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
        view.setTextSize(16);
        view.setTypeface(view.getTypeface(), Typeface.BOLD);
        view.setTextColor(TEXT);
        view.setHintTextColor(Color.rgb(135, 151, 166));
        view.setBackground(glassInput(false));
        view.setPadding(dp(SPACE_MD), 0, dp(SPACE_MD), 0);
        view.setDropDownHeight(dp(280));
        view.setDropDownVerticalOffset(dp(4));
        view.setDropDownBackgroundDrawable(rounded(Color.WHITE, dp(CARD_RADIUS), dp(1), BORDER));
        view.setOnFocusChangeListener((v, hasFocus) ->
                view.setBackground(glassInput(hasFocus)));
        setAdapter(view, values);
        return view;
    }

    private TextView selectorField(String value) {
        TextView view = label(value, 16, true);
        view.setTextColor(TEXT);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setBackground(glassInput(false));
        view.setPadding(dp(SPACE_MD), 0, dp(SPACE_MD), 0);
        view.setSingleLine(true);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        view.setClickable(true);
        attachPressAnimation(view, 0.98f);
        return view;
    }

    private Spinner spinner(List<String> values, String placeholder, String prompt) {
        Spinner view = new Spinner(this);
        List<String> options = new java.util.ArrayList<>();
        options.add(placeholder);
        if (values != null) {
            options.addAll(values);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        view.setAdapter(adapter);
        view.setBackground(glassInput(false));
        view.setPadding(dp(SPACE_MD), 0, dp(SPACE_MD), 0);
        view.setPrompt(prompt);
        return view;
    }

    private void setAdapter(AutoCompleteTextView view, List<String> values) {
        view.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, values));
    }

    private void setSpinnerOptions(Spinner view, List<String> values, String placeholder) {
        List<String> options = new java.util.ArrayList<>();
        options.add(placeholder);
        if (values != null) {
            options.addAll(values);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        view.setAdapter(adapter);
    }

    private Button navButton(String text, View.OnClickListener listener) {
        Button b = button(text, listener);
        b.setAllCaps(false);
        b.setTextColor(PRIMARY);
        b.setBackground(rounded(PRIMARY_SOFT, dp(BUTTON_RADIUS), dp(1), Color.rgb(184, 207, 225)));
        b.setElevation(dp(1));
        return b;
    }

    private Button shortcutButton(String text, String activeShortcut, View.OnClickListener listener) {
        Button b = navButton(text, listener);
        if (text.equals(activeShortcut)) {
            b.setTextColor(Color.WHITE);
            b.setBackground(gradient(PRIMARY, ACCENT, dp(BUTTON_RADIUS)));
            b.setElevation(dp(3));
        }
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
        b.setTextSize(14);
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
        if (label.contains("mark completed")) {
            return URGENT;
        }
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
        if (label.contains("mark completed")) {
            return Color.rgb(146, 64, 14);
        }
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
        lp.setMargins(dp(2), dp(SPACE_XS), dp(2), dp(2));
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

    @SuppressLint("ClickableViewAccessibility")
    private void attachPressAnimation(View view, float scale) {
        view.setOnTouchListener((target, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                target.animate().scaleX(scale).scaleY(scale).setDuration(70).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                target.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            }
            return false;
        });
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

    private GradientDrawable profilePanelBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        Color.argb(236, 255, 255, 255),
                        Color.argb(188, 226, 246, 255)
                }
        );
        drawable.setCornerRadius(dp(CARD_RADIUS));
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
                        Color.WHITE,
                        focused ? Color.rgb(239, 249, 252) : Color.rgb(250, 253, 254)
                }
        );
        drawable.setCornerRadius(dp(BUTTON_RADIUS));
        drawable.setStroke(dp(focused ? 2 : 1), focused ? PRIMARY : BORDER);
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

    private List<String> bloodGroups() {
        return list("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
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

    private String entryText(TextView view) {
        return uppercaseEntryValue(text(view));
    }

    private String selectedSpinnerValue(Spinner spinner) {
        if (spinner == null || spinner.getSelectedItemPosition() <= 0 || spinner.getSelectedItem() == null) {
            return null;
        }
        return uppercaseEntryValue(spinner.getSelectedItem().toString());
    }

    private void selectSpinnerValue(Spinner spinner, String value) {
        if (spinner == null || empty(value)) {
            return;
        }
        String normalized = uppercaseEntryValue(value);
        for (int i = 0; i < spinner.getCount(); i++) {
            Object item = spinner.getItemAtPosition(i);
            if (item != null && normalized.equals(uppercaseEntryValue(item.toString()))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private String uppercaseEntryValue(String raw) {
        if (empty(raw)) {
            return null;
        }
        return raw.trim().toUpperCase(java.util.Locale.US);
    }

    private List<String> uppercaseList(List<String> values) {
        List<String> out = new java.util.ArrayList<>();
        if (values == null) {
            return out;
        }
        for (String item : values) {
            String normalized = uppercaseEntryValue(item);
            if (!empty(normalized) && !out.contains(normalized)) {
                out.add(normalized);
            }
        }
        return out;
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
