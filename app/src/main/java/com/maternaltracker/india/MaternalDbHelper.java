package com.maternaltracker.india;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

final class MaternalDbHelper extends SQLiteOpenHelper {
    static final String DB_NAME = "maternal_tracker_india.db";
    static final int DB_VERSION = 8;
    private volatile boolean referenceCatalogEnabled = false;
    private final Object referenceCatalogLock = new Object();
    private static final String ACTIVE_PATIENT_WHERE = "(deleted_at IS NULL OR deleted_at = '')";
    private static final String DELETED_PATIENT_WHERE = "(deleted_at IS NOT NULL AND deleted_at != '')";
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy");
    private static final String PATIENT_COLUMNS =
            "id, serial_number, patient_id, patient_name, age, blood_group, mobile_number, " +
                    "state_name, district_name, subdistrict_name, local_body_type, local_body_name, ward_name, village_name, " +
                    "gravida, last_delivery_method, lmp_date, edd_date, scheduled_delivery_date, scheduled_delivery_called_at, " +
                    "scheduled_delivery_called_by, motivator_name, doctor_name, visit1, visit2, visit3, final_visit, " +
                    "entry_date, created_by, updated_by, remarks, record_locked, deleted_at, deleted_by";

    MaternalDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /** Copy committed non-WAL data under an exclusive transaction into a staging file. */
    synchronized void snapshotDatabase(File target) throws IOException {
        SQLiteDatabase database = getWritableDatabase();
        if (database.isWriteAheadLoggingEnabled()) {
            throw new IOException("Database snapshots require WAL to be disabled");
        }
        if (database.inTransaction()) {
            throw new IOException("Cannot snapshot inside an existing database transaction");
        }
        File source = new File(database.getPath()).getCanonicalFile();
        File destination = target.getCanonicalFile();
        if (source.equals(destination) || (destination.exists()
                && Files.isSameFile(source.toPath(), destination.toPath()))) {
            throw new IOException("Snapshot target must differ from the live database");
        }
        database.beginTransaction();
        try {
            try (FileInputStream in = new FileInputStream(source);
                 FileOutputStream out = new FileOutputStream(destination)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = in.read(buffer)) != -1) {
                    out.write(buffer, 0, length);
                }
                out.flush();
                out.getFD().sync();
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE patients (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "serial_number INTEGER," +
                "patient_name TEXT NOT NULL," +
                "patient_id TEXT NOT NULL UNIQUE," +
                "age TEXT," +
                "blood_group TEXT," +
                "mobile_number TEXT NOT NULL," +
                "state_name TEXT," +
                "district_name TEXT," +
                "subdistrict_name TEXT," +
                "local_body_type TEXT," +
                "local_body_name TEXT," +
                "ward_name TEXT," +
                "village_name TEXT," +
                "gravida TEXT," +
                "last_delivery_method TEXT," +
                "lmp_date TEXT," +
                "edd_date TEXT," +
                "scheduled_delivery_date TEXT," +
                "scheduled_delivery_called_at TEXT," +
                "scheduled_delivery_called_by TEXT," +
                "motivator_name TEXT," +
                "doctor_name TEXT," +
                "visit1 TEXT," +
                "visit2 TEXT," +
                "visit3 TEXT," +
                "final_visit TEXT," +
                "entry_date TEXT," +
                "created_by TEXT," +
                "updated_by TEXT," +
                "record_locked INTEGER NOT NULL DEFAULT 0," +
                "deleted_at TEXT," +
                "deleted_by TEXT," +
                "remarks TEXT," +
                "created_at TEXT NOT NULL DEFAULT (datetime('now'))" +
                ")");
        db.execSQL("CREATE TABLE custom_motivators (name TEXT PRIMARY KEY, added_at TEXT NOT NULL DEFAULT (datetime('now')))");
        db.execSQL("CREATE TABLE custom_doctors (name TEXT PRIMARY KEY, added_at TEXT NOT NULL DEFAULT (datetime('now')))");
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL, role TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT (datetime('now')))");
        db.execSQL("CREATE TABLE activity_log (id INTEGER PRIMARY KEY AUTOINCREMENT, action TEXT NOT NULL, details TEXT, performed_by TEXT NOT NULL, performed_at TEXT NOT NULL DEFAULT (datetime('now')))");
        db.execSQL("CREATE TABLE change_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, patient_id TEXT NOT NULL, field_name TEXT NOT NULL, old_value TEXT, new_value TEXT, changed_by TEXT NOT NULL, changed_at TEXT NOT NULL DEFAULT (datetime('now')))");
        db.execSQL("CREATE TABLE states (code TEXT PRIMARY KEY, name TEXT NOT NULL UNIQUE, kind TEXT)");
        db.execSQL("CREATE TABLE districts (code TEXT PRIMARY KEY, state_code TEXT, name TEXT NOT NULL)");
        db.execSQL("CREATE TABLE subdistricts (code TEXT PRIMARY KEY, district_code TEXT, name TEXT NOT NULL)");
        db.execSQL("CREATE TABLE local_bodies (code TEXT PRIMARY KEY, state_code TEXT, district_code TEXT, subdistrict_code TEXT, name TEXT NOT NULL, kind TEXT)");
        db.execSQL("CREATE TABLE wards (code TEXT PRIMARY KEY, local_body_code TEXT, name TEXT NOT NULL)");
        db.execSQL("CREATE TABLE villages (code TEXT PRIMARY KEY, state_code TEXT, district_code TEXT, subdistrict_code TEXT, local_body_code TEXT, name TEXT NOT NULL)");
        seedStates(db);
        createPatientIndexes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            ensureUpgradeTables(db);
        }
        if (oldVersion < 3) {
            ensureColumn(db, "patients", "created_by", "TEXT");
        }
        if (oldVersion < 4) {
            ensureColumn(db, "patients", "updated_by", "TEXT");
        }
        if (oldVersion < 5) {
            ensureColumn(db, "patients", "scheduled_delivery_date", "TEXT");
            ensureColumn(db, "patients", "scheduled_delivery_called_at", "TEXT");
            ensureColumn(db, "patients", "scheduled_delivery_called_by", "TEXT");
        }
        if (oldVersion < 6) {
            ensureColumn(db, "patients", "age", "TEXT");
            ensureColumn(db, "patients", "blood_group", "TEXT");
            ensureColumn(db, "patients", "gravida", "TEXT");
            ensureColumn(db, "patients", "last_delivery_method", "TEXT");
        }
        if (oldVersion < 7) {
            ensureColumn(db, "patients", "deleted_at", "TEXT");
            ensureColumn(db, "patients", "deleted_by", "TEXT");
        }
        if (oldVersion < 8) {
            createPatientIndexes(db);
        }
    }

    private void createPatientIndexes(SQLiteDatabase db) {
        // Match the existing active predicate exactly so partial indexes remain eligible.
        db.execSQL("CREATE INDEX IF NOT EXISTS patients_active_entry_order ON patients " +
                "(entry_date DESC, serial_number DESC) WHERE " + ACTIVE_PATIENT_WHERE);
        db.execSQL("CREATE INDEX IF NOT EXISTS patients_active_creator_entry_order ON patients " +
                "(created_by, entry_date DESC, serial_number DESC) WHERE " + ACTIVE_PATIENT_WHERE);
        for (String column : new String[]{"edd_date", "scheduled_delivery_date", "final_visit"}) {
            db.execSQL("CREATE INDEX IF NOT EXISTS patients_active_" + column + " ON patients (" +
                    column + ") WHERE " + ACTIVE_PATIENT_WHERE);
            db.execSQL("CREATE INDEX IF NOT EXISTS patients_active_creator_" + column +
                    " ON patients (created_by, " + column + ") WHERE " + ACTIVE_PATIENT_WHERE);
        }
    }

    private void ensureUpgradeTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS custom_motivators (name TEXT PRIMARY KEY, added_at TEXT NOT NULL DEFAULT (datetime('now')))");
        db.execSQL("CREATE TABLE IF NOT EXISTS custom_doctors (name TEXT PRIMARY KEY, added_at TEXT NOT NULL DEFAULT (datetime('now')))");
        db.execSQL("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL, role TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT (datetime('now')))");
        db.execSQL("CREATE TABLE IF NOT EXISTS activity_log (id INTEGER PRIMARY KEY AUTOINCREMENT, action TEXT NOT NULL, details TEXT, performed_by TEXT NOT NULL, performed_at TEXT NOT NULL DEFAULT (datetime('now')))");
        db.execSQL("CREATE TABLE IF NOT EXISTS change_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, patient_id TEXT NOT NULL, field_name TEXT NOT NULL, old_value TEXT, new_value TEXT, changed_by TEXT NOT NULL, changed_at TEXT NOT NULL DEFAULT (datetime('now')))");
        db.execSQL("CREATE TABLE IF NOT EXISTS states (code TEXT PRIMARY KEY, name TEXT NOT NULL UNIQUE, kind TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS districts (code TEXT PRIMARY KEY, state_code TEXT, name TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS subdistricts (code TEXT PRIMARY KEY, district_code TEXT, name TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS local_bodies (code TEXT PRIMARY KEY, state_code TEXT, district_code TEXT, subdistrict_code TEXT, name TEXT NOT NULL, kind TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS wards (code TEXT PRIMARY KEY, local_body_code TEXT, name TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS villages (code TEXT PRIMARY KEY, state_code TEXT, district_code TEXT, subdistrict_code TEXT, local_body_code TEXT, name TEXT NOT NULL)");
        seedStates(db);
    }

    void ensureCoreData() {
        SQLiteDatabase db = getWritableDatabase();
        seedStates(db);
    }

    void seedStates(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            for (String[] row : LocationSeed.STATES_AND_UTS) {
                values.clear();
                values.put("code", row[0]);
                values.put("name", row[1]);
                values.put("kind", row[2]);
                db.insertWithOnConflict("states", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    String nextPatientId() {
        return nextPatientId(1);
    }

    /** Local suggestion for create-only cloud retries; does not reserve or mutate an ID. */
    String nextPatientId(int minimumSequence) {
        if (minimumSequence < 1) {
            throw new IllegalArgumentException("Minimum patient sequence must be positive");
        }
        LocalDate today = LocalDate.now();
        String month = today.format(MONTH_FMT);
        String year = today.format(YEAR_FMT);
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM patients WHERE substr(entry_date, 6, 2)=? AND substr(entry_date, 1, 4)=?",
                new String[]{month, year})) {
            long baseline = c.moveToFirst() ? c.getLong(0) + 1 : 1;
            long startingSequence = Math.max(baseline, (long) minimumSequence);
            if (startingSequence > Integer.MAX_VALUE) {
                throw new IllegalStateException("Patient sequence exhausted for this month");
            }
            int next = (int) startingSequence;
            String candidate = PatientRules.patientId(next, month, year);
            while (patientIdExists(db, candidate)) {
                if (next == Integer.MAX_VALUE) {
                    throw new IllegalStateException("Patient sequence exhausted for this month");
                }
                next++;
                candidate = PatientRules.patientId(next, month, year);
            }
            return candidate;
        }
    }

    /** Clear only patient rows; retain lookup, audit, user and location data. */
    boolean clearPatientCache() {
        return replacePatientsFromCloud(java.util.Collections.emptyList());
    }

    long savePatient(Patient patient) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = patientValues(patient, patient.serialNumber > 0 ? patient.serialNumber : nextSerial(db));
        long id;
        if (patient.id > 0) {
            id = db.update("patients", values, "id = ?", new String[]{String.valueOf(patient.id)});
            id = patient.id;
        } else {
            id = db.insertWithOnConflict("patients", null, values, SQLiteDatabase.CONFLICT_ABORT);
        }
        patient.id = id;
        patient.serialNumber = values.getAsInteger("serial_number");
        patient.recordLocked = values.getAsInteger("record_locked") == 1;
        if (!referenceCatalogEnabled) {
            db.beginTransaction();
            try {
                synchronized (referenceCatalogLock) {
                    if (!referenceCatalogEnabled) {
                        insertLookup(db, "custom_motivators", patient.motivatorName);
                        insertLookup(db, "custom_doctors", patient.doctorName);
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        return id;
    }

    /** Reconcile an authoritative snapshot; return whether patient or lookup data changed. */
    boolean replacePatientsFromCloud(List<Patient> patients) {
        return replacePatientsFromCloud(patients, () -> true);
    }

    /** A stale snapshot returns false without committing or publishing incoming local IDs. */
    boolean replacePatientsFromCloud(List<Patient> patients, BooleanSupplier isCurrent) {
        if (!isCurrent.getAsBoolean()) {
            return false;
        }
        Map<String, ContentValues> incoming = new LinkedHashMap<>();
        for (Patient patient : patients) {
            if (patient.patientId == null) {
                throw new IllegalArgumentException("Cloud patient_id must not be null");
            }
            // As with the former CONFLICT_REPLACE loop, the last duplicate ID wins.
            incoming.put(patient.patientId, patientValues(patient, patient.serialNumber));
        }
        SQLiteDatabase db = getWritableDatabase();
        Map<String, Long> localIds = new LinkedHashMap<>();
        Map<Long, ContentValues> updates = new LinkedHashMap<>();
        List<Long> removals = new ArrayList<>();
        boolean changed = false;
        if (!isCurrent.getAsBoolean()) {
            return false;
        }
        db.beginTransaction();
        try {
            try (Cursor c = db.rawQuery("SELECT " + PATIENT_COLUMNS + " FROM patients", null)) {
                while (c.moveToNext()) {
                    long id = c.getLong(0);
                    String patientId = c.getString(2);
                    ContentValues values = incoming.remove(patientId);
                    if (values == null) {
                        removals.add(id);
                        continue;
                    }
                    localIds.put(patientId, id);
                    Map<String, Object> current = new LinkedHashMap<>();
                    Map<String, Object> desired = new LinkedHashMap<>();
                    for (Map.Entry<String, Object> entry : values.valueSet()) {
                        String column = entry.getKey();
                        int index = c.getColumnIndexOrThrow(column);
                        Object stored = null;
                        if (!c.isNull(index)) {
                            if (entry.getValue() instanceof Integer) {
                                stored = c.getInt(index);
                            } else {
                                stored = c.getString(index);
                            }
                        }
                        current.put(column, stored);
                        desired.put(column, entry.getValue());
                    }
                    List<String> columns = DbReconciliation.changedColumns(current, desired);
                    if (!columns.isEmpty()) {
                        ContentValues delta = new ContentValues(values);
                        for (String column : desired.keySet()) {
                            if (!columns.contains(column)) {
                                delta.remove(column);
                            }
                        }
                        updates.put(id, delta);
                    }
                }
            }
            // Close the scan before writes: never mutate rows under an advancing cursor.
            for (long id : removals) {
                changed |= db.delete("patients", "id = ?", new String[]{String.valueOf(id)}) > 0;
            }
            for (Map.Entry<Long, ContentValues> update : updates.entrySet()) {
                changed |= db.update("patients", update.getValue(), "id = ?",
                        new String[]{String.valueOf(update.getKey())}) > 0;
            }
            for (Map.Entry<String, ContentValues> entry : incoming.entrySet()) {
                localIds.put(entry.getKey(), db.insertOrThrow("patients", null, entry.getValue()));
                changed = true;
            }
            if (!referenceCatalogEnabled) {
                synchronized (referenceCatalogLock) {
                    if (!referenceCatalogEnabled) {
                        for (Patient patient : patients) {
                            changed |= ensureCloudLookup(db, "custom_motivators", patient.motivatorName);
                            changed |= ensureCloudLookup(db, "custom_doctors", patient.doctorName);
                        }
                    }
                }
            }
            if (!isCurrent.getAsBoolean()) {
                return false;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        // Publish IDs only after the entire snapshot commits successfully.
        for (Patient patient : patients) {
            patient.id = localIds.get(patient.patientId);
        }
        return changed;
    }

    private boolean ensureCloudLookup(SQLiteDatabase db, String table, String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        String normalized = name.trim().toUpperCase(java.util.Locale.US);
        try (Cursor c = db.rawQuery("SELECT name FROM " + table + " WHERE UPPER(name) = ?",
                new String[]{normalized})) {
            if (c.moveToFirst() && normalized.equals(c.getString(0)) && !c.moveToNext()) {
                return false;
            }
        }
        db.delete(table, "UPPER(name) = ?", new String[]{normalized});
        ContentValues values = new ContentValues();
        values.put("name", normalized);
        db.insertOrThrow(table, null, values);
        return true;
    }

    private ContentValues patientValues(Patient patient, int serialNumber) {
        ContentValues values = new ContentValues();
        values.put("serial_number", serialNumber > 0 ? serialNumber : 1);
        values.put("patient_name", patient.patientName);
        values.put("patient_id", patient.patientId);
        values.put("age", patient.age);
        values.put("blood_group", patient.bloodGroup);
        values.put("mobile_number", patient.mobileNumber);
        values.put("state_name", patient.stateName);
        values.put("district_name", patient.districtName);
        values.put("subdistrict_name", patient.subdistrictName);
        values.put("local_body_type", patient.localBodyType);
        values.put("local_body_name", patient.localBodyName);
        values.put("ward_name", patient.wardName);
        values.put("village_name", patient.villageName);
        values.put("gravida", patient.gravida);
        values.put("last_delivery_method", patient.lastDeliveryMethod);
        values.put("lmp_date", patient.lmpDate);
        values.put("edd_date", patient.eddDate);
        values.put("scheduled_delivery_date", patient.scheduledDeliveryDate);
        values.put("scheduled_delivery_called_at", patient.scheduledDeliveryCalledAt);
        values.put("scheduled_delivery_called_by", patient.scheduledDeliveryCalledBy);
        values.put("motivator_name", patient.motivatorName);
        values.put("doctor_name", patient.doctorName);
        values.put("visit1", patient.visit1);
        values.put("visit2", patient.visit2);
        values.put("visit3", patient.visit3);
        values.put("final_visit", patient.finalVisit);
        values.put("entry_date", patient.entryDate);
        values.put("created_by", patient.createdBy);
        values.put("updated_by", patient.updatedBy);
        values.put("record_locked", patient.recordLocked ? 1 : 0);
        values.put("deleted_at", patient.deletedAt);
        values.put("deleted_by", patient.deletedBy);
        values.put("remarks", patient.remarks);
        return values;
    }

    void logChange(String patientId, String field, String oldValue, String newValue, String user) {
        if ((oldValue == null ? "" : oldValue).equals(newValue == null ? "" : newValue)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("patient_id", patientId);
        values.put("field_name", field);
        values.put("old_value", oldValue);
        values.put("new_value", newValue);
        values.put("changed_by", user == null ? "unknown" : user);
        getWritableDatabase().insert("change_logs", null, values);
    }

    void logActivity(String action, String details, String user) {
        ContentValues values = new ContentValues();
        values.put("action", action);
        values.put("details", details);
        values.put("performed_by", user == null ? "unknown" : user);
        getWritableDatabase().insert("activity_log", null, values);
    }

    List<String> listNames(String table) {
        List<String> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = customLookupTable(table)
                ? "SELECT DISTINCT UPPER(name) FROM " + table + " WHERE name IS NOT NULL AND TRIM(name) != '' ORDER BY UPPER(name)"
                : "SELECT name FROM " + table + " ORDER BY name";
        try (Cursor c = db.rawQuery(sql, null)) {
            while (c.moveToNext()) {
                out.add(c.getString(0));
            }
        }
        return out;
    }

    void addName(String table, String name) {
        insertLookup(getWritableDatabase(), table, name);
    }

    /** Authoritative cloud directory; the first committed table disables derivation for both. */
    void replaceReferenceNames(String table, List<String> names) {
        if (!customLookupTable(table)) {
            throw new IllegalArgumentException("Unsupported reference table: " + table);
        }
        List<String> normalized = DbReferenceNames.normalize(names);
        SQLiteDatabase db = getWritableDatabase();
        if (db.inTransaction()) {
            throw new IllegalStateException("Reference replacement requires its own transaction");
        }
        // All derivation paths acquire the SQLite writer before this short catalog lock.
        db.beginTransaction();
        synchronized (referenceCatalogLock) {
            try {
                db.delete(table, null, null);
                ContentValues values = new ContentValues();
                for (String name : normalized) {
                    values.clear();
                    values.put("name", name);
                    db.insertOrThrow(table, null, values);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            // Publish after commit, before a waiting legacy derivation can resume.
            referenceCatalogEnabled = true;
        }
    }

    void deleteName(String table, String name) {
        if (name == null) {
            return;
        }
        if (customLookupTable(table)) {
            getWritableDatabase().delete(table, "UPPER(name) = ?", new String[]{name.trim().toUpperCase(java.util.Locale.US)});
        } else {
            getWritableDatabase().delete(table, "name = ?", new String[]{name});
        }
    }

    List<String> listStates() {
        return listNames("states");
    }

    String getCodeByName(String table, String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT code FROM " + table + " WHERE name = ? ORDER BY code LIMIT 1", new String[]{name.trim()})) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    String getCodeByNameAndParent(String table, String name, String parentColumn, String parentCode) {
        if (name == null || name.trim().isEmpty() || parentCode == null || parentCode.trim().isEmpty()) {
            return null;
        }
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT code FROM " + table + " WHERE name = ? AND " + parentColumn + " = ? ORDER BY code LIMIT 1",
                new String[]{name.trim(), parentCode.trim()})) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    List<String> listDistricts(String stateCode) {
        return listNamesByCode("districts", "state_code", stateCode);
    }

    List<String> listSubdistricts(String districtCode) {
        return listNamesByCode("subdistricts", "district_code", districtCode);
    }

    List<String> listLocalBodies(String districtCode) {
        return listNamesByCode("local_bodies", "district_code", districtCode);
    }

    List<String> listWards(String localBodyCode) {
        return listNamesByCode("wards", "local_body_code", localBodyCode);
    }

    List<String> listVillages(String districtCode, String subdistrictCode, String localBodyCode) {
        if (localBodyCode != null && !localBodyCode.trim().isEmpty()) {
            List<String> byLocalBody = listNamesByCode("villages", "local_body_code", localBodyCode);
            if (!byLocalBody.isEmpty()) {
                return byLocalBody;
            }
        }
        if (subdistrictCode != null && !subdistrictCode.trim().isEmpty()) {
            List<String> bySubdistrict = listNamesByCode("villages", "subdistrict_code", subdistrictCode);
            if (!bySubdistrict.isEmpty()) {
                return bySubdistrict;
            }
        }
        return listNamesByCode("villages", "district_code", districtCode);
    }

    private List<String> listNamesByCode(String table, String parentColumn, String parentCode) {
        List<String> out = new ArrayList<>();
        if (parentCode == null || parentCode.trim().isEmpty()) {
            return out;
        }
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT name FROM " + table + " WHERE " + parentColumn + " = ? ORDER BY name",
                new String[]{parentCode.trim()})) {
            while (c.moveToNext()) {
                out.add(c.getString(0));
            }
        }
        return out;
    }

    Patient getPatient(long id) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT " + PATIENT_COLUMNS + " FROM patients WHERE id = ?",
                new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? patientFromCursor(c) : null;
        }
    }

    Patient getPatientByPatientId(String patientId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT " + PATIENT_COLUMNS + " FROM patients WHERE patient_id = ?",
                new String[]{patientId == null ? "" : patientId})) {
            return c.moveToFirst() ? patientFromCursor(c) : null;
        }
    }

    List<Patient> listPatients(String filter) {
        return listPatients(filter, null, null);
    }

    List<Patient> listPatients(String filter, String extraWhere, String[] extraArgs) {
        List<Patient> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String like = "%" + (filter == null ? "" : filter.trim()) + "%";
        String where = "(patient_name LIKE ? OR patient_id LIKE ? OR mobile_number LIKE ? OR age LIKE ? OR blood_group LIKE ? OR gravida LIKE ? OR village_name LIKE ? OR motivator_name LIKE ? OR doctor_name LIKE ? OR district_name LIKE ? OR local_body_name LIKE ?)";
        List<String> args = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            args.add(like);
        }
        if (extraWhere != null && !extraWhere.trim().isEmpty()) {
            where = appendWhere(where, extraWhere);
            if (extraArgs != null) {
                for (String arg : extraArgs) {
                    args.add(arg);
                }
            }
        }
        where = appendWhere(where, ACTIVE_PATIENT_WHERE);
        try (Cursor c = db.rawQuery(
                "SELECT " + PATIENT_COLUMNS + " " +
                        "FROM patients WHERE " + where + " ORDER BY entry_date DESC, serial_number DESC",
                args.toArray(new String[0]))) {
            while (c.moveToNext()) {
                out.add(patientFromCursor(c));
            }
        }
        return out;
    }

    List<Patient> listDeletedPatients() {
        List<Patient> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT " + PATIENT_COLUMNS + " FROM patients WHERE " + DELETED_PATIENT_WHERE + " ORDER BY deleted_at DESC, entry_date DESC",
                null)) {
            while (c.moveToNext()) {
                out.add(patientFromCursor(c));
            }
        }
        return out;
    }

    int countDeletedPatients() {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM patients WHERE " + DELETED_PATIENT_WHERE,
                null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    void discardLocalPatient(long id) {
        getWritableDatabase().delete("patients", "id = ?", new String[]{String.valueOf(id)});
    }

    int countPatients(String where, String[] args) {
        String activeWhere = appendWhere(where, ACTIVE_PATIENT_WHERE);
        String sql = "SELECT COUNT(*) FROM patients WHERE " + activeWhere;
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /** One active, scoped scan; condition binds precede scope binds in SQL order. */
    int[] countCases(String where, String[] args, String[] conditions, String[][] conditionArgs) {
        if (conditions == null || (conditionArgs != null && conditionArgs.length != conditions.length)) {
            throw new IllegalArgumentException("Conditions and argument groups must have matching lengths");
        }
        int[] counts = new int[conditions.length];
        if (conditions.length == 0) {
            return counts;
        }
        StringBuilder sql = new StringBuilder("SELECT ");
        List<String> bindings = new ArrayList<>();
        for (int i = 0; i < conditions.length; i++) {
            if (conditions[i] == null || conditions[i].trim().isEmpty()) {
                throw new IllegalArgumentException("Count condition must not be empty");
            }
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("COALESCE(SUM(CASE WHEN (").append(conditions[i])
                    .append(") THEN 1 ELSE 0 END), 0)");
            if (conditionArgs != null && conditionArgs[i] != null) {
                for (String arg : conditionArgs[i]) {
                    bindings.add(arg);
                }
            }
        }
        sql.append(" FROM patients WHERE ").append(appendWhere(where, ACTIVE_PATIENT_WHERE));
        if (args != null) {
            for (String arg : args) {
                bindings.add(arg);
            }
        }
        try (Cursor c = getReadableDatabase().rawQuery(sql.toString(), bindings.toArray(new String[0]))) {
            if (c.moveToFirst()) {
                for (int i = 0; i < counts.length; i++) {
                    counts[i] = c.getInt(i);
                }
            }
        }
        return counts;
    }

    /** Ascending yyyy-MM buckets, including zero months; bounds are inclusive ISO dates. */
    Map<String, Integer> monthlyCounts(String where, String[] args, String dateColumn,
                                       String from, String to) {
        if (!"entry_date".equals(dateColumn) && !"edd_date".equals(dateColumn)
                && !"scheduled_delivery_date".equals(dateColumn) && !"final_visit".equals(dateColumn)) {
            throw new IllegalArgumentException("Unsupported report date column: " + dateColumn);
        }
        LocalDate start = LocalDate.parse(from);
        LocalDate end = LocalDate.parse(to);
        Map<String, Integer> out = new LinkedHashMap<>();
        if (start.isAfter(end)) {
            return out;
        }
        for (YearMonth month = YearMonth.from(start), last = YearMonth.from(end);
             !month.isAfter(last); month = month.plusMonths(1)) {
            out.put(month.toString(), 0);
        }
        String finalWhere = appendWhere(appendWhere(where, ACTIVE_PATIENT_WHERE),
                dateColumn + " BETWEEN ? AND ?");
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT substr(" + dateColumn + ", 1, 7) AS month, COUNT(*) FROM patients WHERE " +
                        finalWhere + " GROUP BY month ORDER BY month", appendArgs(args, from, to))) {
            while (c.moveToNext()) {
                out.put(c.getString(0), c.getInt(1));
            }
        }
        return out;
    }

    Map<String, Integer> countBy(String column) {
        return countBy(column, null, null);
    }

    Map<String, Integer> countBy(String column, String where, String[] args) {
        Map<String, Integer> out = new LinkedHashMap<>();
        String sqlWhere = " WHERE " + appendWhere(where, ACTIVE_PATIENT_WHERE);
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(NULLIF(" + column + ", ''), '-') AS label, COUNT(*) FROM patients" + sqlWhere + " GROUP BY label ORDER BY COUNT(*) DESC, label LIMIT 50",
                args)) {
            while (c.moveToNext()) {
                out.put(c.getString(0), c.getInt(1));
            }
        }
        return out;
    }

    List<String[]> visitCompletionRows() {
        return visitCompletionRows(null, null);
    }

    List<String[]> visitCompletionRows(String where, String[] args) {
        List<String[]> rows = new ArrayList<>();
        int total = countPatients(where, args);
        String today = LocalDate.now().toString();
        rows.add(new String[]{"1st visit date recorded", String.valueOf(countPatients(appendWhere(where, "visit1 IS NOT NULL AND visit1 != '' AND visit1 <= ?"), appendArgs(args, today))), String.valueOf(total)});
        rows.add(new String[]{"2nd visit date reached", String.valueOf(countPatients(appendWhere(where, "visit2 IS NOT NULL AND visit2 != '' AND visit2 <= ?"), appendArgs(args, today))), String.valueOf(total)});
        rows.add(new String[]{"3rd visit date reached", String.valueOf(countPatients(appendWhere(where, "visit3 IS NOT NULL AND visit3 != '' AND visit3 <= ?"), appendArgs(args, today))), String.valueOf(total)});
        rows.add(new String[]{"Final visit date reached", String.valueOf(countPatients(appendWhere(where, "final_visit IS NOT NULL AND final_visit != '' AND final_visit <= ?"), appendArgs(args, today))), String.valueOf(total)});
        return rows;
    }

    private String appendWhere(String where, String extra) {
        return where == null || where.trim().isEmpty() ? extra : "(" + where + ") AND (" + extra + ")";
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

    List<String[]> upcomingEddRows(int limit) {
        return upcomingEddRows(null, null, limit);
    }

    List<String[]> upcomingEddRows(String where, String[] args, int limit) {
        List<String[]> rows = new ArrayList<>();
        String finalWhere = appendWhere(appendWhere(where, ACTIVE_PATIENT_WHERE), "record_locked = 0 AND edd_date IS NOT NULL AND edd_date != '' AND edd_date >= ?");
        List<String> finalArgs = new ArrayList<>();
        if (args != null) {
            for (String arg : args) {
                finalArgs.add(arg);
            }
        }
        finalArgs.add(LocalDate.now().toString());
        finalArgs.add(String.valueOf(limit));
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT patient_id, patient_name, edd_date, mobile_number, village_name FROM patients " +
                        "WHERE " + finalWhere + " " +
                        "ORDER BY edd_date ASC LIMIT ?",
                finalArgs.toArray(new String[0]))) {
            while (c.moveToNext()) {
                rows.add(new String[]{c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4)});
            }
        }
        return rows;
    }

    List<String[]> scheduledDeliveryRows(String where, String[] args, int limit) {
        List<String[]> rows = new ArrayList<>();
        String finalWhere = appendWhere(appendWhere(where, ACTIVE_PATIENT_WHERE), "scheduled_delivery_date IS NOT NULL AND scheduled_delivery_date != ''");
        List<String> finalArgs = new ArrayList<>();
        if (args != null) {
            for (String arg : args) {
                finalArgs.add(arg);
            }
        }
        finalArgs.add(String.valueOf(limit));
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT patient_id, patient_name, scheduled_delivery_date, mobile_number, village_name, scheduled_delivery_called_at, scheduled_delivery_called_by FROM patients " +
                        "WHERE " + finalWhere + " " +
                        "ORDER BY scheduled_delivery_date ASC LIMIT ?",
                finalArgs.toArray(new String[0]))) {
            while (c.moveToNext()) {
                rows.add(new String[]{c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6)});
            }
        }
        return rows;
    }

    List<String[]> changeLogs() {
        List<String[]> rows = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT changed_at, patient_id, field_name, old_value, new_value, changed_by FROM change_logs ORDER BY changed_at DESC LIMIT 200",
                null)) {
            while (c.moveToNext()) {
                rows.add(new String[]{c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5)});
            }
        }
        return rows;
    }

    private Patient patientFromCursor(Cursor c) {
        Patient p = new Patient();
        p.id = c.getLong(0);
        p.serialNumber = c.getInt(1);
        p.patientId = c.getString(2);
        p.patientName = c.getString(3);
        p.age = c.getString(4);
        p.bloodGroup = c.getString(5);
        p.mobileNumber = c.getString(6);
        p.stateName = c.getString(7);
        p.districtName = c.getString(8);
        p.subdistrictName = c.getString(9);
        p.localBodyType = c.getString(10);
        p.localBodyName = c.getString(11);
        p.wardName = c.getString(12);
        p.villageName = c.getString(13);
        p.gravida = c.getString(14);
        p.lastDeliveryMethod = c.getString(15);
        p.lmpDate = c.getString(16);
        p.eddDate = c.getString(17);
        p.scheduledDeliveryDate = c.getString(18);
        p.scheduledDeliveryCalledAt = c.getString(19);
        p.scheduledDeliveryCalledBy = c.getString(20);
        p.motivatorName = c.getString(21);
        p.doctorName = c.getString(22);
        p.visit1 = c.getString(23);
        p.visit2 = c.getString(24);
        p.visit3 = c.getString(25);
        p.finalVisit = c.getString(26);
        p.entryDate = c.getString(27);
        p.createdBy = c.getString(28);
        p.updatedBy = c.getString(29);
        p.remarks = c.getString(30);
        p.recordLocked = c.getInt(31) == 1;
        p.deletedAt = c.getString(32);
        p.deletedBy = c.getString(33);
        return p;
    }

    private void ensureColumn(SQLiteDatabase db, String table, String column, String type) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (c.moveToNext()) {
                if (column.equalsIgnoreCase(c.getString(1))) {
                    return;
                }
            }
        }
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
    }

    private int nextSerial(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT COALESCE(MAX(serial_number), 0) + 1 FROM patients", null)) {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
            return 1;
        }
    }

    private boolean patientIdExists(SQLiteDatabase db, String patientId) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM patients WHERE patient_id = ? LIMIT 1", new String[]{patientId})) {
            return c.moveToFirst();
        }
    }

    private void insertLookup(SQLiteDatabase db, String table, String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        ContentValues values = new ContentValues();
        if (customLookupTable(table)) {
            String normalized = name.trim().toUpperCase(java.util.Locale.US);
            db.delete(table, "UPPER(name) = ?", new String[]{normalized});
            values.put("name", normalized);
        } else {
            values.put("name", name.trim());
        }
        db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private boolean customLookupTable(String table) {
        return "custom_motivators".equals(table) || "custom_doctors".equals(table);
    }

}
