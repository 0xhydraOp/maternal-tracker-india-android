# Maternal Tracker India Android

Native Android maternal follow-up app for `BLUE BIRD A GENERAL HOSPITAL`, backed by Firebase Authentication, Firestore sync, and a local SQLite cache for reports/exports.

Current version: `v1.1.31`

## Current Scope

- Patient registration with patient ID generation, mandatory Age/Blood Group/GRAVIDA, and pregnancy follow-up dates.
- Blue Bird scoped State/District defaults for West Bengal / Murshidabad with Murshidabad block dropdowns.
- Firebase login, admin/staff roles, and online patient sync.
- Scheduled delivery, EDD, planned visit, call-pending, and completed-record workflows.
- Excel/PDF export support through Android document picker/share flow.
- Visual Print Desk for exact hospital prescription, OT, and baby identification documents, with untouched full-color image handoff to Epson Smart Panel.
- Admin-managed doctor and motivator autocomplete lists.

The lower-level India location database is designed to be imported from official LGD/data.gov.in CSV exports because districts, panchayats, municipalities, wards, and villages change frequently. The official LGD catalog is published by the Ministry of Panchayati Raj and was updated in May 2026.

Place CSV files in `app/src/main/assets/lgd/` before building if you want them bundled:

- `states.csv`
- `districts.csv`
- `subdistricts.csv`
- `local_bodies.csv`
- `wards.csv`
- `villages.csv`

The importer accepts common LGD-style column names such as `state_code`, `state_name`, `district_code`, `district_name`, `subdistrict_code`, `subdistrict_name`, `local_body_code`, `local_body_name`, `ward_code`, `ward_name`, `village_code`, and `village_name`.

## Build

```powershell
.\build_apk.ps1
```

Output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Build signed release APK:

```powershell
.\build_release_apk.ps1
```

Output:

```text
release\MaternalTrackerIndia-v1.1.31-release.apk
```

Install to an attached phone/emulator:

```powershell
.\install_debug.ps1
```

## Data Sources

- LGD catalog: https://www.data.gov.in/catalog/local-government-directory-lgd
- LGD States: https://www.data.gov.in/resource/local-government-directory-lgd-states
- LGD Sub-Districts: https://www.data.gov.in/resource/local-government-directory-lgd-sub-districts
