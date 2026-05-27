package com.maternaltracker.india;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MaternalDbHelper extends SQLiteOpenHelper {
    static final String DB_NAME = "maternal_tracker_india.db";
    static final int DB_VERSION = 1;
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
        if (oldVersion < 1) {
            onCreate(db);
        }
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
            return String.format(Locale.US, "PT%02d-%s-%s", next, month, year);
        }
    }

    long savePatient(Patient patient) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("serial_number", nextSerial(db));
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
        long id = db.insertWithOnConflict("patients", null, values, SQLiteDatabase.CONFLICT_ABORT);
        insertLookup(db, "custom_motivators", patient.motivatorName);
        insertLookup(db, "custom_doctors", patient.doctorName);
        return id;
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

    List<Patient> listPatients(String filter) {
        List<Patient> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String like = "%" + (filter == null ? "" : filter.trim()) + "%";
        try (Cursor c = db.rawQuery(
                "SELECT patient_id, patient_name, mobile_number, state_name, district_name, local_body_name, village_name, motivator_name, doctor_name, entry_date " +
                        "FROM patients WHERE patient_name LIKE ? OR patient_id LIKE ? OR mobile_number LIKE ? ORDER BY entry_date DESC, serial_number DESC",
                new String[]{like, like, like})) {
            while (c.moveToNext()) {
                Patient p = new Patient();
                p.patientId = c.getString(0);
                p.patientName = c.getString(1);
                p.mobileNumber = c.getString(2);
                p.stateName = c.getString(3);
                p.districtName = c.getString(4);
                p.localBodyName = c.getString(5);
                p.villageName = c.getString(6);
                p.motivatorName = c.getString(7);
                p.doctorName = c.getString(8);
                p.entryDate = c.getString(9);
                out.add(p);
            }
        }
        return out;
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
