package com.maternaltracker.india;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MaternalDbHelper extends SQLiteOpenHelper {
    static final String DB_NAME = "maternal_tracker_india.db";
    static final int DB_VERSION = 2;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy");

    MaternalDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE patients (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "serial_number INTEGER," +
                "patient_name TEXT NOT NULL," +
                "patient_id TEXT NOT NULL UNIQUE," +
                "mobile_number TEXT NOT NULL," +
                "state_name TEXT," +
                "district_name TEXT," +
                "subdistrict_name TEXT," +
                "local_body_type TEXT," +
                "local_body_name TEXT," +
                "ward_name TEXT," +
                "village_name TEXT," +
                "lmp_date TEXT," +
                "edd_date TEXT," +
                "motivator_name TEXT," +
                "doctor_name TEXT," +
                "visit1 TEXT," +
                "visit2 TEXT," +
                "visit3 TEXT," +
                "final_visit TEXT," +
                "entry_date TEXT," +
                "record_locked INTEGER NOT NULL DEFAULT 0," +
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
        seedDefaultAdmin(db);
        seedStates(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL, role TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT (datetime('now')))");
            db.execSQL("CREATE TABLE IF NOT EXISTS activity_log (id INTEGER PRIMARY KEY AUTOINCREMENT, action TEXT NOT NULL, details TEXT, performed_by TEXT NOT NULL, performed_at TEXT NOT NULL DEFAULT (datetime('now')))");
            db.execSQL("CREATE TABLE IF NOT EXISTS change_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, patient_id TEXT NOT NULL, field_name TEXT NOT NULL, old_value TEXT, new_value TEXT, changed_by TEXT NOT NULL, changed_at TEXT NOT NULL DEFAULT (datetime('now')))");
            seedDefaultAdmin(db);
        }
    }

    void ensureCoreData() {
        SQLiteDatabase db = getWritableDatabase();
        seedDefaultAdmin(db);
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

    private void seedDefaultAdmin(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM users", null)) {
            if (c.moveToFirst() && c.getInt(0) > 0) {
                return;
            }
        }
        ContentValues values = new ContentValues();
        values.put("username", "admin");
        values.put("password_hash", "admin123");
        values.put("role", "ADMIN");
        db.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    String loginRole(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT role FROM users WHERE username = ? AND password_hash = ? LIMIT 1",
                new String[]{username.trim(), password.trim()})) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    long saveUser(String username, String password, String role) {
        ContentValues values = new ContentValues();
        values.put("username", username.trim());
        values.put("password_hash", password.trim());
        values.put("role", role == null || role.trim().isEmpty() ? "STAFF" : role.trim());
        return getWritableDatabase().insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_ABORT);
    }

    List<String[]> listUsers() {
        List<String[]> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id, username, role FROM users ORDER BY username", null)) {
            while (c.moveToNext()) {
                out.add(new String[]{String.valueOf(c.getLong(0)), c.getString(1), c.getString(2)});
            }
        }
        return out;
    }

    String nextPatientId() {
        LocalDate today = LocalDate.now();
        String month = today.format(MONTH_FMT);
        String year = today.format(YEAR_FMT);
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM patients WHERE substr(entry_date, 6, 2)=? AND substr(entry_date, 1, 4)=?",
                new String[]{month, year})) {
            int next = 1;
            if (c.moveToFirst()) {
                next = c.getInt(0) + 1;
            }
            return String.format(Locale.US, "PT%02d-%s-%s", next, month, year);
        }
    }

    long savePatient(Patient patient) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("serial_number", patient.serialNumber > 0 ? patient.serialNumber : nextSerial(db));
        values.put("patient_name", patient.patientName);
        values.put("patient_id", patient.patientId);
        values.put("mobile_number", patient.mobileNumber);
        values.put("state_name", patient.stateName);
        values.put("district_name", patient.districtName);
        values.put("subdistrict_name", patient.subdistrictName);
        values.put("local_body_type", patient.localBodyType);
        values.put("local_body_name", patient.localBodyName);
        values.put("ward_name", patient.wardName);
        values.put("village_name", patient.villageName);
        values.put("lmp_date", patient.lmpDate);
        values.put("edd_date", patient.eddDate);
        values.put("motivator_name", patient.motivatorName);
        values.put("doctor_name", patient.doctorName);
        values.put("visit1", patient.visit1);
        values.put("visit2", patient.visit2);
        values.put("visit3", patient.visit3);
        values.put("final_visit", patient.finalVisit);
        values.put("entry_date", patient.entryDate);
        values.put("record_locked", patient.finalVisit == null || patient.finalVisit.isEmpty() ? 0 : 1);
        values.put("remarks", patient.remarks);
        long id;
        if (patient.id > 0) {
            id = db.update("patients", values, "id = ?", new String[]{String.valueOf(patient.id)});
        } else {
            id = db.insertWithOnConflict("patients", null, values, SQLiteDatabase.CONFLICT_ABORT);
        }
        insertLookup(db, "custom_motivators", patient.motivatorName);
        insertLookup(db, "custom_doctors", patient.doctorName);
        return id;
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
        try (Cursor c = db.rawQuery("SELECT name FROM " + table + " ORDER BY name", null)) {
            while (c.moveToNext()) {
                out.add(c.getString(0));
            }
        }
        return out;
    }

    void addName(String table, String name) {
        insertLookup(getWritableDatabase(), table, name);
    }

    void deleteName(String table, String name) {
        if (name == null) {
            return;
        }
        getWritableDatabase().delete(table, "name = ?", new String[]{name});
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
                "SELECT id, serial_number, patient_id, patient_name, mobile_number, state_name, district_name, subdistrict_name, local_body_type, local_body_name, ward_name, village_name, lmp_date, edd_date, motivator_name, doctor_name, visit1, visit2, visit3, final_visit, entry_date, remarks, record_locked FROM patients WHERE id = ?",
                new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? patientFromCursor(c) : null;
        }
    }

    List<Patient> listPatients(String filter) {
        List<Patient> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String like = "%" + (filter == null ? "" : filter.trim()) + "%";
        try (Cursor c = db.rawQuery(
                "SELECT id, serial_number, patient_id, patient_name, mobile_number, state_name, district_name, subdistrict_name, local_body_type, local_body_name, ward_name, village_name, lmp_date, edd_date, motivator_name, doctor_name, visit1, visit2, visit3, final_visit, entry_date, remarks, record_locked " +
                        "FROM patients WHERE patient_name LIKE ? OR patient_id LIKE ? OR mobile_number LIKE ? OR village_name LIKE ? OR motivator_name LIKE ? OR doctor_name LIKE ? OR district_name LIKE ? ORDER BY entry_date DESC, serial_number DESC",
                new String[]{like, like, like, like, like, like, like})) {
            while (c.moveToNext()) {
                out.add(patientFromCursor(c));
            }
        }
        return out;
    }

    void deletePatient(long id) {
        getWritableDatabase().delete("patients", "id = ?", new String[]{String.valueOf(id)});
    }

    void unlockPatient(long id) {
        ContentValues values = new ContentValues();
        values.put("record_locked", 0);
        getWritableDatabase().update("patients", values, "id = ?", new String[]{String.valueOf(id)});
    }

    int countPatients(String where, String[] args) {
        String sql = "SELECT COUNT(*) FROM patients" + (where == null || where.isEmpty() ? "" : " WHERE " + where);
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    Map<String, Integer> countBy(String column) {
        Map<String, Integer> out = new LinkedHashMap<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(NULLIF(" + column + ", ''), '-') AS label, COUNT(*) FROM patients GROUP BY label ORDER BY COUNT(*) DESC, label LIMIT 50",
                null)) {
            while (c.moveToNext()) {
                out.put(c.getString(0), c.getInt(1));
            }
        }
        return out;
    }

    List<String[]> visitCompletionRows() {
        List<String[]> rows = new ArrayList<>();
        int total = countPatients(null, null);
        rows.add(new String[]{"1st Visit", String.valueOf(countPatients("visit1 IS NOT NULL AND visit1 != ''", null)), String.valueOf(total)});
        rows.add(new String[]{"2nd Visit", String.valueOf(countPatients("visit2 IS NOT NULL AND visit2 != ''", null)), String.valueOf(total)});
        rows.add(new String[]{"3rd Visit", String.valueOf(countPatients("visit3 IS NOT NULL AND visit3 != ''", null)), String.valueOf(total)});
        rows.add(new String[]{"Final Visit", String.valueOf(countPatients("final_visit IS NOT NULL AND final_visit != ''", null)), String.valueOf(total)});
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
        p.mobileNumber = c.getString(4);
        p.stateName = c.getString(5);
        p.districtName = c.getString(6);
        p.subdistrictName = c.getString(7);
        p.localBodyType = c.getString(8);
        p.localBodyName = c.getString(9);
        p.wardName = c.getString(10);
        p.villageName = c.getString(11);
        p.lmpDate = c.getString(12);
        p.eddDate = c.getString(13);
        p.motivatorName = c.getString(14);
        p.doctorName = c.getString(15);
        p.visit1 = c.getString(16);
        p.visit2 = c.getString(17);
        p.visit3 = c.getString(18);
        p.finalVisit = c.getString(19);
        p.entryDate = c.getString(20);
        p.remarks = c.getString(21);
        p.recordLocked = c.getInt(22) == 1;
        return p;
    }

    private int nextSerial(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT COALESCE(MAX(serial_number), 0) + 1 FROM patients", null)) {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
            return 1;
        }
    }

    private void insertLookup(SQLiteDatabase db, String table, String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }
}
