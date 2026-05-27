package com.maternaltracker.india;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class LocationImporter {
    private LocationImporter() {
    }

    static void importBundledLgd(Context context, MaternalDbHelper helper) {
        AssetManager assets = context.getAssets();
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            importCsvIfPresent(assets, db, "lgd/states.csv", "states");
            importCsvIfPresent(assets, db, "lgd/districts.csv", "districts");
            importCsvIfPresent(assets, db, "lgd/subdistricts.csv", "subdistricts");
            importCsvIfPresent(assets, db, "lgd/local_bodies.csv", "local_bodies");
            importCsvIfPresent(assets, db, "lgd/wards.csv", "wards");
            importCsvIfPresent(assets, db, "lgd/villages.csv", "villages");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static void importCsvIfPresent(AssetManager assets, SQLiteDatabase db, String assetPath, String table) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(assets.open(assetPath), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return;
            }
            List<String> headers = parseCsvLine(headerLine);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                Map<String, String> row = rowMap(headers, parseCsvLine(line));
                ContentValues values = valuesFor(table, row);
                if (values.size() > 0) {
                    db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                }
            }
        } catch (IOException ignored) {
            // Missing bundled LGD CSVs are allowed; import the files that are available.
        }
    }

    private static ContentValues valuesFor(String table, Map<String, String> row) {
        ContentValues values = new ContentValues();
        if ("states".equals(table)) {
            put(values, "code", first(row, "state_code", "statecode", "lgd_state_code"));
            put(values, "name", first(row, "state_name", "statename", "name"));
            put(values, "kind", first(row, "kind", "state_type"));
        } else if ("districts".equals(table)) {
            put(values, "code", first(row, "district_code", "districtcode", "lgd_district_code"));
            put(values, "state_code", first(row, "state_code", "statecode", "lgd_state_code"));
            put(values, "name", first(row, "district_name", "districtname", "name"));
        } else if ("subdistricts".equals(table)) {
            put(values, "code", first(row, "subdistrict_code", "sub_district_code", "subdistrictcode", "lgd_subdistrict_code"));
            put(values, "district_code", first(row, "district_code", "districtcode", "lgd_district_code"));
            put(values, "name", first(row, "subdistrict_name", "sub_district_name", "subdistrictname", "name"));
        } else if ("local_bodies".equals(table)) {
            put(values, "code", first(row, "local_body_code", "localbody_code", "localbodycode", "lgd_local_body_code"));
            put(values, "state_code", first(row, "state_code", "statecode", "lgd_state_code"));
            put(values, "district_code", first(row, "district_code", "districtcode", "lgd_district_code"));
            put(values, "subdistrict_code", first(row, "subdistrict_code", "sub_district_code", "subdistrictcode"));
            put(values, "name", first(row, "local_body_name", "localbody_name", "localbodyname", "name"));
            put(values, "kind", first(row, "local_body_type", "localbody_type", "type", "kind"));
        } else if ("wards".equals(table)) {
            put(values, "code", first(row, "ward_code", "wardcode", "lgd_ward_code"));
            put(values, "local_body_code", first(row, "local_body_code", "localbody_code", "localbodycode"));
            put(values, "name", first(row, "ward_name", "wardname", "name"));
        } else if ("villages".equals(table)) {
            put(values, "code", first(row, "village_code", "villagecode", "lgd_village_code"));
            put(values, "state_code", first(row, "state_code", "statecode", "lgd_state_code"));
            put(values, "district_code", first(row, "district_code", "districtcode", "lgd_district_code"));
            put(values, "subdistrict_code", first(row, "subdistrict_code", "sub_district_code", "subdistrictcode"));
            put(values, "local_body_code", first(row, "local_body_code", "localbody_code", "localbodycode"));
            put(values, "name", first(row, "village_name", "villagename", "name"));
        }
        if (!values.containsKey("code") || !values.containsKey("name")) {
            values.clear();
        }
        return values;
    }

    private static Map<String, String> rowMap(List<String> headers, List<String> values) {
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String key = normalize(headers.get(i));
            String value = i < values.size() ? values.get(i).trim() : "";
            row.put(key, value);
        }
        return row;
    }

    private static String first(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(normalize(key));
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static void put(ContentValues values, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            values.put(key, value.trim());
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
    }

    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                out.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        out.add(current.toString());
        return out;
    }
}
