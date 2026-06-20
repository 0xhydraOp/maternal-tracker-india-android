# Changelog

## v1.1.15 - 2026-06-21

- Replaced placeholder bottom-navigation letters with native Home, Entry, Search, Reports, and Admin icons.
- Upgraded patient search cards with clearer identity hierarchy, status color rails, and clinical priority chips.
- Reorganized the Admin panel into operations, data/recovery, access control, reference lists, and app support command panels.
- Kept core patient, report, export, backup, user-management, and update flows unchanged.
- Bumped Android app version to `1.1.15` / versionCode `23`.

## v1.1.14 - 2026-06-20

- Added automatic pregnancy age display from LMP in patient entry, search results, patient detail, and exports.
- Refined the profile drawer with cleaner action rows and a scrollable account panel.
- Hardened report filter validation, admin access guarding, and admin dialog validation behavior.
- Excluded completed records from active EDD queues and upcoming EDD lists.
- Added unit coverage for first-visit and final-visit validation edge cases.
- Bumped Android app version to `1.1.14` / versionCode `22`.

## v1.1.13 - 2026-06-20

- Removed the duplicate Home dashboard Today's Priority Queue.
- Kept Home focused on status/date sync, conditional Today at a Glance cards, Today's Work, KPI metrics, Upcoming EDD, and Data Quality Alerts.
- Reworked the profile slide area into a grouped Blue Bird drawer with live role/sync status and cleaner action rows.
- Bumped Android app version to `1.1.13` / versionCode `21`.

## v1.1.12 - 2026-06-20

- Corrected the Home dashboard no-priority message so overdue visit follow-ups are described accurately.
- Bumped Android app version to `1.1.12` / versionCode `20`.

## v1.1.11 - 2026-06-20

- Reorganized the Home dashboard around patient-specific Today's Work cards with direct call, update, completion, and detail actions.
- Kept priority patients de-duplicated so the same record does not occupy multiple Home action slots.
- Fixed visit follow-up queries so overdue planned follow-ups remain visible until staff updates or completes the record.
- Updated scheduled-delivery report counting so Patient Notified is counted directly from call-marked records.
- Renamed visit reporting copy to date-tracking language so planned dates are not presented as completed clinical visits.
- Clarified Admin user removal wording as app-access revocation, matching Firebase client-side behavior.
- Bumped Android app version to `1.1.11` / versionCode `19`.

## v1.1.10 - 2026-06-19

- Allowed future 2nd, 3rd, and final visit dates as planned follow-up dates instead of blocking them as invalid.
- Added Home dashboard tracking for planned visit follow-ups due within the next 7 days.
- Added visit follow-up access from Search, Reports, and Admin patient controls.
- Updated visit completion reporting so future planned visit dates are not counted as completed visits.
- Added an in-app update checker linked to the latest GitHub release APK; staff and admins can access it from the profile menu, and admins also see it in App Support.
- Bumped Android app version to `1.1.10` / versionCode `18`.

## v1.1.9 - 2026-06-18

- Reviewed scheduled-delivery workflow, dashboard filters, report filters, and patient validation for behavior/query mismatches.
- Allowed past scheduled-delivery dates so overdue records can correctly enter the completion-required workflow instead of being blocked during save.
- Excluded locked/completed records from pending scheduled-call counts and lists.
- Limited the Home scheduled-call panel to actionable upcoming call-pending records.
- Added completion-due metrics/actions to Reports so scheduled records are separated into notified, call-pending, and completion-required states.
- Bumped Android app version to `1.1.9` / versionCode `17`.

## v1.1.8 - 2026-06-18

- Added scheduled-delivery completion workflow after doctor-given delivery dates pass.
- Overdue scheduled-delivery patients now remain visible as completion-required notifications until an operator marks them completed.
- Marking scheduled delivery completed sets the final visit date when missing, locks the record, syncs it online, and moves the patient to the completed list.
- Bumped Android app version to `1.1.8` / versionCode `16`.

## v1.1.7 - 2026-06-18

- Unified card spacing, button sizing, section title treatment, chip styling, and alert colors across the app.
- Standardized shared UI dimensions through common spacing/radius constants.
- Fixed the Admin support version display to use the current app build version.
- Bumped Android app version to `1.1.7` / versionCode `15`.

## v1.1.6 - 2026-06-18

- Fixed Home dashboard priority count so scheduled delivery within 7 days is not double-counted against pending scheduled calls.
- Added validation for scheduled delivery dates so pending call reminders cannot be saved in the past.
- Added validation to prevent scheduled delivery before LMP, EDD before LMP, and future actual visit dates.
- Added unit tests for scheduled-delivery and visit-date validation behavior.
- Bumped Android app version to `1.1.6` / versionCode `14`.

## v1.1.5 - 2026-06-18

- Final polish pass for Home dashboard, Search, Reports, Admin, and shared visual styling.
- Added a compact Home priority strip so urgent scheduled-delivery work is immediately visible.
- Redesigned patient search result cards with clearer call/update actions and visible status chips.
- Reorganized Reports into cleaner metric cards, quick filters, scheduled-delivery access, and filtered exports.
- Reorganized Administration into control sections with clearer account, data, support, and app-status areas.
- Bumped Android app version to `1.1.5` / versionCode `13`.

## v1.1.4 - 2026-06-18

- Made scheduled delivery dates within the next 7 days the highest-attention dashboard priority.
- Added Reports quick filter chips for `Scheduled only` and `Call pending only`.
- Added a `Patient marked notified` confirmation toast after scheduled-delivery call marking.
- Bumped Android app version to `1.1.4` / versionCode `12`.

## v1.1.3 - 2026-06-18

- Fixed Firebase patient writes to include `createdBy`, preserving staff-scoped patient ownership after cloud sync and admin edits.
- Bumped Android app version to `1.1.3` / versionCode `11`.

## v1.1.2 - 2026-06-18

- Added optional Scheduled Delivery Date to patient records.
- Added Home dashboard Scheduled Delivery Calls list with pending/notified status.
- Added scheduled-delivery metrics and direct access list to the Reports dashboard.
- Added call actions from scheduled-delivery rows, upcoming EDD rows, patient search rows, and patient detail.
- Mark scheduled-delivery patients as notified when the operator taps the app call action.
- Reset scheduled-delivery call status when the scheduled delivery date changes.
- Added scheduled-delivery fields to Firebase sync, Firestore rules, local database migration, Excel export, and PDF export.
- Added `PROJECT_PROGRESS.md` with the project progress and current handoff notes.
- Polished navigation, action buttons, dashboard ordering, search quick filters, report filter summaries, admin grouping, and status badges.
- Replaced fragile symbol icons with Android-safe text labels to avoid broken characters in the UI.

## v1.1.1 - 2026-06-18

- Removed Report dashboard Block and Motivator filters; Reports now filter by date, village, and status only.
- Removed Home dashboard follow-up pending and records-needing-completion notifications.
- Reorganized Home dashboard cards around Total, Due Week, Today, Done, EDD priorities, upcoming EDD, and data quality.
- Bumped Android app version to `1.1.1` / versionCode `9`.

## v1.0.5 - 2026-05-31

- Cleared stale screen references when returning to login so detached status labels cannot be updated after logout.
- Rechecked animation/drawable UI paths, Android lint, debug build, release build, and APK signature.
- Bumped Android app version to `1.0.5` / versionCode `6`.

## v1.0.4 - 2026-05-31

- Refined the Android UI with warmer background treatment, rounded elevated cards, styled form inputs, polished header, and softer navigation/action buttons.
- Added page transition animation and button press scale animation.
- Added visual chips for locked patient records and improved status/nav coloring.
- Bumped Android app version to `1.0.4` / versionCode `5`.

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
