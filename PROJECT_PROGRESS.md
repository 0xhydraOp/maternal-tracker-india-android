# Blue Bird Maternal Tracker Progress

Last updated: 2026-06-18

## Current App Scope

- App is dedicated to `BLUE BIRD A GENERAL HOSPITAL`.
- Package name is `com.maternaltracker.india`.
- Firebase-backed online sync is the confirmed direction.
- Admin controls user creation and deletion.
- Existing offline data migration is not required.

## Completed Work

- Added Blue Bird branding across the app header and dashboard.
- Converted the app toward online Firebase Authentication and Firestore sync.
- Kept admin/user roles with admin-only user management.
- Added persistent login behavior so users stay logged in until manual logout.
- Added export support for patient data in Excel and PDF formats.
- Reworked the main navigation with Home, Entry, Search, Reports, and Admin sections.
- Added Murshidabad/West Bengal defaults and Murshidabad block dropdown behavior.
- Made village entry manual.
- Made motivator optional.
- Made 2nd visit, 3rd visit, and final visit optional.
- Removed remarks from patient entry.
- Removed generic dashboard command box.
- Removed unnecessary Home dashboard notifications for follow-up pending and records needing completion.
- Removed Report dashboard Block and Motivator filters.
- Kept Reports focused on date range, village, status, village summary, monthly summary, and export actions.
- Reorganized Admin and Report dashboards.
- Improved dashboard styling, compact default layout, sky-blue background, translucent/glass-style cards, and bottom navigation.
- Added date/live-time dashboard display.
- Fixed Firebase permission/save issues found during patient-entry testing.
- Rebuilt and published `v1.1.1`.

## Latest Published Version

- Version: `1.1.1`
- Version code: `9`
- Commit: `bca8543`
- Release APK: `release/MaternalTrackerIndia-v1.1.1-release.apk`
- GitHub release: `https://github.com/0xhydraOp/maternal-tracker-india-android/releases/tag/v1.1.1`

## Verified Recently

- Debug unit tests passed.
- Debug APK build passed.
- Release APK build passed.
- Android release lint/build completed.
- ADB smoke-tested Home, Entry, Search, Reports, and Admin navigation.
- Static scan confirmed removed dashboard/report labels were gone from the main code.

## Scheduled Delivery Feature

- Added optional `Scheduled Delivery Date` to patient records.
- User/admin can search an existing patient, edit details, and enter the scheduled delivery date.
- Home dashboard shows scheduled-delivery patients in a dedicated priority call list.
- Reports dashboard shows scheduled-delivery metrics, pending-call count, progress rows, and a direct scheduled-delivery access list.
- Scheduled-delivery list includes patient mobile call actions.
- Upcoming EDD rows, patient search rows, and patient detail also include call actions.
- When the operator taps a call action for a scheduled-delivery patient, the app marks that patient as notified.
- If the scheduled-delivery patient has not been called/notified, the record remains visible as `Call pending`.
- If the scheduled delivery date changes, the previous call-notification state is cleared so the operator is reminded to call again.

## Final UI Polish Pass

- Replaced fragile navigation/menu symbols with Android-safe text labels.
- Added clearer dashboard ordering so scheduled-delivery calls rise above general priority work when pending.
- Added scheduled-call KPI and direct View All actions on Home.
- Added Patient Search quick filters for scheduled records, pending calls, EDD 30 days, and locked records.
- Added clearer patient status chips such as `Open`, `Call pending`, and `Patient notified`.
- Added form-section required/optional badges and helper text for Scheduled Delivery Date.
- Added active report filter chips and a dedicated Scheduled Delivery Report section.
- Split Admin into patient operations and database/audit panels with role badges.
