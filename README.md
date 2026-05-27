# Maternal Tracker India Android

Native Android version of the desktop Maternal Tracking app. This project lives beside the Windows `.exe` project and keeps its own SQLite database on the Android device.

Current version: `v1.0.1`

## Current Scope

- Patient registration with patient ID generation.
- India-wide location fields: State/UT, District, Sub-District, Panchayat/Municipality, Ward, Village.
- Complete built-in State/UT seed list.
- LGD CSV import support for national district, sub-district, local body, ward, and village data.
- Motivator and doctor autocomplete persistence.
- Patient list/search view.

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
release\MaternalTrackerIndia-v1.0.1-release.apk
```

Install to an attached phone/emulator:

```powershell
.\install_debug.ps1
```

## Data Sources

- LGD catalog: https://www.data.gov.in/catalog/local-government-directory-lgd
- LGD States: https://www.data.gov.in/resource/local-government-directory-lgd-states
- LGD Sub-Districts: https://www.data.gov.in/resource/local-government-directory-lgd-sub-districts
