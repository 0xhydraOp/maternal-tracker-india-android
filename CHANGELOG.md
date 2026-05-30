# Changelog

## v1.0.3 - 2026-05-31

- Fixed dashboard statistic cards so they open the correct filtered patient lists.
- Fixed backup restore to close and reopen the database around file replacement.
- Fixed edit-mode location autocomplete adapters so district, local body, ward, and village suggestions remain available.
- Added safe fallbacks for Android external app storage paths during CSV export and backup.
- Added add-user validation and hashed password storage for new/default users while keeping legacy plain-text login compatibility.
- Bumped Android app version to `1.0.3` / versionCode `4`.

## v1.0.2 - 2026-05-31

- Replaced the thin Android flow with multi-screen desktop parity: login, dashboard, patient entry/edit, patient search, reports, backup, and administration.
- Added saved motivator and doctor autocomplete lists, patient locking/unlocking, admin patient delete, change logs, user creation, and CSV export.
- Added backup creation and restore-from-backup support inside Android app storage.
- Bumped Android app version to `1.0.2` / versionCode `3`.

## v1.0.1 - 2026-05-28

- Added Play Store/proper release signing setup.
- Added local release keystore generation script.
- Added signed release APK build script.
- Bumped Android app version to `1.0.1` / versionCode `2`.

## v1.0.0 - 2026-05-28

- Initial Android project for Maternal Tracker India.
- Added local SQLite patient registration.
- Added complete India State/UT seed list.
- Added LGD CSV import support for districts, sub-districts, local bodies, wards, and villages.
- Added doctor and motivator autocomplete persistence.
- Added patient list/search screen.
