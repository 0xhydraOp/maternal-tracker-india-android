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
import java.util.Map;

final class MaternalDbHelper extends SQLiteOpenHelper {
    static final String DB_NAME = "maternal_tracker_india.db";
    static final int DB_VERSION = 5;
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
            String candidate = PatientRules.patientId(next, month, year);
            while (patientIdExists(db, candidate)) {
                next++;
                candidate = PatientRules.patientId(next, month, year);
            }
            return candidate;
        }
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
        insertLookup(db, "custom_motivators", patient.motivatorName);
        insertLookup(db, "custom_doctors", patient.doctorName);
        return id;
    }

    void replacePatientsFromCloud(List<Patient> patients) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("patients", null, null);
            for (Patient patient : patients) {
                ContentValues values = patientValues(patient, patient.serialNumber);
                long id = db.insertWithOnConflict("patients", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                patient.id = id;
                insertLookup(db, "custom_motivators", patient.motivatorName);
                insertLookup(db, "custom_doctors", patient.doctorName);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private ContentValues patientValues(Patient patient, int serialNumber) {
        ContentValues values = new ContentValues();
        values.put("serial_number", serialNumber > 0 ? serialNumber : 1);
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
                "SELECT id, serial_number, patient_id, patient_name, mobile_number, state_name, district_name, subdistrict_name, local_body_type, local_body_name, ward_name, village_name, lmp_date, edd_date, scheduled_delivery_date, scheduled_delivery_called_at, scheduled_delivery_called_by, motivator_name, doctor_name, visit1, visit2, visit3, final_visit, entry_date, created_by, updated_by, remarks, record_locked FROM patients WHERE id = ?",
                new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? patientFromCursor(c) : null;
        }
    }

    Patient getPatientByPatientId(String patientId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, serial_number, patient_id, patient_name, mobile_number, state_name, district_name, subdistrict_name, local_body_type, local_body_name, ward_name, village_name, lmp_date, edd_date, scheduled_delivery_date, scheduled_delivery_called_at, scheduled_delivery_called_by, motivator_name, doctor_name, visit1, visit2, visit3, final_visit, entry_date, created_by, updated_by, remarks, record_locked FROM patients WHERE patient_id = ?",
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
        String where = "(patient_name LIKE ? OR patient_id LIKE ? OR mobile_number LIKE ? OR village_name LIKE ? OR motivator_name LIKE ? OR doctor_name LIKE ? OR district_name LIKE ? OR local_body_name LIKE ?)";
        List<String> args = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            args.add(like);
        }
        if (extraWhere != null && !extraWhere.trim().isEmpty()) {
            where += " AND (" + extraWhere + ")";
            if (extraArgs != null) {
                for (String arg : extraArgs) {
                    args.add(arg);
                }
            }
        }
        try (Cursor c = db.rawQuery(
                "SELECT id, serial_number, patient_id, patient_name, mobile_number, state_name, district_name, subdistrict_name, local_body_type, local_body_name, ward_name, village_name, lmp_date, edd_date, scheduled_delivery_date, scheduled_delivery_called_at, scheduled_delivery_called_by, motivator_name, doctor_name, visit1, visit2, visit3, final_visit, entry_date, created_by, updated_by, remarks, record_locked " +
                        "FROM patients WHERE " + where + " ORDER BY entry_date DESC, serial_number DESC",
                args.toArray(new String[0]))) {
            while (c.moveToNext()) {
                out.add(patientFromCursor(c));
            }
        }
        return out;
    }

    void deletePatient(long id) {
        getWritableDatabase().delete("patients", "id = ?", new String[]{String.valueOf(id)});
    }

    int countPatients(String where, String[] args) {
        String sql = "SELECT COUNT(*) FROM patients" + (where == null || where.isEmpty() ? "" : " WHERE " + where);
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    Map<String, Integer> countBy(String column) {
        return countBy(column, null, null);
    }

    Map<String, Integer> countBy(String column, String where, String[] args) {
        Map<String, Integer> out = new LinkedHashMap<>();
        String sqlWhere = where == null || where.trim().isEmpty() ? "" : " WHERE " + where;
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
        rows.add(new String[]{"1st Visit", String.valueOf(countPatients(appendWhere(where, "visit1 IS NOT NULL AND visit1 != '' AND visit1 <= ?"), appendArgs(args, today))), String.valueOf(total)});
        rows.add(new String[]{"2nd Visit", String.valueOf(countPatients(appendWhere(where, "visit2 IS NOT NULL AND visit2 != '' AND visit2 <= ?"), appendArgs(args, today))), String.valueOf(total)});
        rows.add(new String[]{"3rd Visit", String.valueOf(countPatients(appendWhere(where, "visit3 IS NOT NULL AND visit3 != '' AND visit3 <= ?"), appendArgs(args, today))), String.valueOf(total)});
        rows.add(new String[]{"Final Visit", String.valueOf(countPatients(appendWhere(where, "final_visit IS NOT NULL AND final_visit != '' AND final_visit <= ?"), appendArgs(args, today))), String.valueOf(total)});
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
        String finalWhere = appendWhere(where, "edd_date IS NOT NULL AND edd_date != '' AND edd_date >= ?");
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
        String finalWhere = appendWhere(where, "scheduled_delivery_date IS NOT NULL AND scheduled_delivery_date != ''");
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
        p.scheduledDeliveryDate = c.getString(14);
        p.scheduledDeliveryCalledAt = c.getString(15);
        p.scheduledDeliveryCalledBy = c.getString(16);
        p.motivatorName = c.getString(17);
        p.doctorName = c.getString(18);
        p.visit1 = c.getString(19);
        p.visit2 = c.getString(20);
        p.visit3 = c.getString(21);
        p.finalVisit = c.getString(22);
        p.entryDate = c.getString(23);
        p.createdBy = c.getString(24);
        p.updatedBy = c.getString(25);
        p.remarks = c.getString(26);
        p.recordLocked = c.getInt(27) == 1;
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
        values.put("name", name.trim());
        db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

}
