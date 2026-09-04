# Blue Bird Maternal Tracker Specification

Last updated: 2026-09-04
Current app version: `1.1.33` / versionCode `41`
Package: `com.maternaltracker.india`  
Hospital scope: `BLUE BIRD A GENERAL HOSPITAL`

## 1. Product Scope

This Android app is a dedicated maternal care registry for Blue Bird A General Hospital. It is not a generic maternal tracker and should keep hospital-specific branding, location defaults, workflows, reports, and admin controls.

The confirmed direction is online Firebase-backed operation:

- Firebase Authentication is used for login.
- Firestore is used for online patient sync and role records.
- Users stay logged in until they manually sign out.
- Admin users can see all hospital data.
- Staff users see records scoped by their creator account where applicable.
- Existing offline data migration is not required.

## 2. Core Roles

### Admin

- Can view all patient records.
- Can create app users with email, password, and role.
- Can revoke app access by removing the role record.
- Can access Administration, Backup Manager, Reference Lists, exports, reports, and app update support.
- Can unlock or delete patient records where admin-only controls are exposed.

### Staff

- Can log in with Firebase credentials.
- Can create and update patient records.
- Can search patients, update visit dates, call patients, export allowed data, and use the in-app updater.
- Cannot access admin-only user and reference-management controls.

## 2A. Hospital Print Desk

The profile panel exposes the following actions to both admins and staff, above patient workflow actions:

- Print Prescriptions
- Print OT Papers
- Print Baby Birth Form

Printing requirements:

- Printing uses a unified visual Print Desk with category navigation, document selection, exact preview, print summary, and confirmation.
- Doctor and OT choices use custom bold selection sheets with supporting credentials or orientation details; the Baby Identification form uses the same selected-document visual language.
- Epson Smart Panel is the primary action when installed; Android printers / Save as PDF and print-service management remain secondary actions.
- Epson Smart Panel receives an untouched copy of the original bundled JPEG through its supported image-print activity. The app does not decode, redraw, recolor, resize, or recompress that image for Epson.
- Android printers / Save as PDF opens Android's destination chooser for other enabled printers and PDF output.
- A4 is the default paper size; the operator may change supported media in the Android print dialog.
- Supplied hospital documents are printed from their original bundled image files.
- Document artwork, text, logos, borders, spacing, and layout must not be recreated, transcribed, or recompressed.
- Documents scale proportionally and remain centered inside the selected paper's printable area.
- Portrait documents default to portrait A4; the BHT / Input & Output Chart defaults to landscape A4.

Prescription selection:

- DR. SUYETA NASRIN - MBBS, DNB, DGO (NEW DELHI)
- DR. ARNAB SAHA - M.B.B.S., M.S. (Obstetrics & Gynaecology)
- DR. SUDIPTA BISWAS - MBBS, PGPN, DCH
- DR. PIARUL SK - MBBS, MS (General Surgeon)

OT Paper selection is a single-choice document sheet. The operator selects and prints one of:

- BHT / Input & Output Chart - Bed Sheet
- Anaesthetic Note
- OT Note

Baby Identification Form is a dedicated single-document print action.

## 3. Location Scope

The app is locked to:

- State: `West Bengal`
- District: `MURSHIDABAD`

Location behavior:

- State and district are preselected and disabled in patient entry.
- Municipality option is removed.
- Block Name uses a dropdown of Murshidabad blocks.
- Village is manual typing only.
- Village is mandatory.

## 4. Patient Entry Form

Mandatory fields:

- Patient ID
- Patient Name
- Age
- Blood Group
- Mobile Number
- Doctor Name
- State
- District
- Block Name
- Village
- GRAVIDA
- LMP Date
- EDD Date
- Entry / 1st Visit

Optional fields:

- Motivator Name
- Method of Last Delivery
- Scheduled Delivery Date
- 2nd Visit
- 3rd Visit
- Final Visit

Removed fields:

- Remarks

Entry behavior:

- Patient-entry text inputs stay uppercase while typing.
- Patient-entry values are normalized to uppercase again before save.
- Dates and mobile numbers are unaffected by uppercase filtering.
- Doctor and Motivator fields use autocomplete dropdowns from admin-managed reference lists.
- Doctor and Motivator reference names are stored and displayed uppercase.
- Blood Group is a fixed dropdown with `A+`, `A-`, `B+`, `B-`, `AB+`, `AB-`, `O+`, and `O-`.
- Age and GRAVIDA are manual entry fields.
- Method of Last Delivery is a manual optional field for patients with previous delivery history.

Pregnancy age:

- A calculated Pregnancy Age row appears below LMP Date.
- Pregnancy age is shown as weeks and days, for example `20 weeks 4 days`.
- It continues updating until the patient record is completed/locked.
- For completed records, pregnancy age freezes using final visit date when available, otherwise scheduled delivery date.

## 5. Visit Logic

Visit fields:

- Entry / 1st Visit is mandatory and cannot be future dated.
- 2nd Visit, 3rd Visit, and Final Visit are optional planned or actual dates.
- Future 2nd, 3rd, and Final Visit dates are allowed because staff may enter planned follow-up dates.
- Planned visit dates due within the next 7 days are tracked in dashboard/report priority flows.
- Final Visit can trigger record locking when the date is valid and not in the future.

Completed behavior:

- Locked records are treated as completed.
- Completed/locked records are excluded from active EDD and priority queues.

## 6. Scheduled Delivery Logic

Scheduled Delivery Date is optional and represents a doctor-given delivery date before or different from EDD.

Scheduled delivery behavior:

- Patient records can be searched and edited to add or update Scheduled Delivery Date.
- Scheduled delivery within 7 days is highest-attention work.
- Scheduled delivery patients stay visible in the 7-day delivery window after call notification.
- Scheduled delivery call actions are available from Home, Search, Reports, and Patient Detail.
- Patient Detail should keep scheduled delivery status visible inside the compact detail panel.
- When staff taps the call action, the app opens the Android dialer and marks the scheduled delivery patient as notified.
- Confirmation toast: `Patient marked notified`.
- Changing Scheduled Delivery Date resets the notified/called state.
- If Scheduled Delivery Date has passed, the patient remains visible as completion-required until the operator marks completed.
- Marking scheduled delivery completed sets Final Visit when missing, locks the record, syncs online, and moves the record to completed.

## 7. Home Dashboard

The Home dashboard should remain compact, professional, and action-focused.

Required Home areas:

- Hospital overview with live date/time, sync status, role, Murshidabad/West Bengal context, and record count.
- Priority work cards only when matching patients exist.
- Today's Work section for patient-specific actions.
- Compact KPI row for total, due week, urgent scheduled, completion due, calls, visits, today, and completed.
- Upcoming EDD list for open records.

Priority order:

1. Scheduled delivery completion due.
2. Scheduled delivery within 7 days.
3. Visit follow-ups due or within 7 days.
4. EDD within 7 days.
5. Scheduled delivery call pending.
6. EDD within 30 days.

No-action state:

- If no patient falls into priority queues, priority sections should not show noisy alerts.
- Display a calm no-priority message instead.

## 8. Search Dashboard

Search supports:

- Patient name
- Mobile number
- Patient ID
- Village
- Motivator
- Doctor
- District
- Block

Search UI requirements:

- Search bar must be visually prominent.
- Results use patient cards with clear identity hierarchy.
- Cards show status rails and clinical priority chips.
- Cards include Mobile, Pregnancy, Care/EDD, scheduled delivery status when applicable, and action buttons.
- The care row must not prefix the doctor name with another `Doctor`; staff may already enter names as `DR. NAME`.

Quick filters:

- Scheduled
- Call Pending
- Visit Follow-ups
- EDD 30 Days
- Locked

Actions:

- View
- Call
- Update Visits
- Mark Completed when scheduled delivery completion is due
- Unlock/Delete for admin where allowed

## 9. Reports

Reports should stay organized and operational, not generic.

Required report controls:

- Date range filter
- Village filter
- Status filter: All, Open, Locked
- Scheduled only filter chip
- Call pending only filter chip

Required report sections:

- Report Actions
- Quick Report Filters
- Report Overview
- Active Filters
- Scheduled Delivery Report
- Report Snapshot
- Scheduled Delivery Access
- Visit Follow-up Access
- Visit Date Tracking
- Patients by Village
- Monthly Summary

Removed report areas:

- Motivator Performance
- Patients by Block
- Follow-up pending noise panels
- Records needing completion noise panels not tied to scheduled delivery logic

Exports:

- Excel and PDF exports are available from Reports.
- Patient exports include Age, Blood Group, GRAVIDA, Last Delivery Method, Pregnancy Age, and Scheduled Delivery fields.

## 10. Admin Panel

Administration is admin-only.

Admin panel should be organized into:

- Admin Overview
- Operations Control
- Data and Recovery
- Access Control
- Reference Lists
- App Support
- Audit Trail

Admin Overview:

- Records
- Completed
- Scheduled
- Calls

Operations Control:

- Patient Records
- Priority Follow-up
- Reports
- Export Center

Data and Recovery:

- Backup Manager
- Full Export
- Export PDF
- Save Backup File

Access Control:

- Create Staff Login
- User Review
- Firebase role list
- Add User
- Remove Access

Reference Lists:

- Doctor Names
- Motivator Names
- Doctor/Motivator managers must use polished managed rows, not plain generic rows.
- Reference list dialog must keep Add Doctor/Add Motivator visible even when the saved-name list is long.
- Remove actions require confirmation.
- Added names are saved uppercase and appear in patient-entry dropdowns.

App Support:

- Version
- Sync
- Role
- Check for Updates
- Open Releases

## 11. Export, Backup, and Update

Export formats:

- CSV
- Excel `.xlsx`
- PDF

Export behavior:

- Exports should work directly on Android via the system document picker/share flow.
- Patient list exports include all current clinical tracking fields.
- Single patient Excel/PDF export is available from Patient Detail.
- Patient Detail uses a compact grouped panel so identity, address, pregnancy, scheduled delivery, visit dates, and record status are visible without scanning a long plain-text stack.
- Patient Detail labels and values should use larger bold text for field readability on staff phones.

Backup:

- Admin can create backup.
- Admin can export/save backup file.
- Admin can restore local backup when required.

In-app update:

- Staff and Admin can check for updates.
- The updater checks the latest GitHub release:
  `https://github.com/0xhydraOp/maternal-tracker-india-android/releases/latest`
- APK is published as a GitHub release asset.

## 12. UI and Styling Requirements

The app should feel like a finished hospital system.

Current UI standards:

- Sky-blue app background.
- Glass/translucent card styling.
- Compact professional layout.
- Hospital-branded header.
- Native icon bottom navigation for Home, Entry, Search, Reports, and Admin.
- Profile drawer with grouped actions.
- Patient cards with status rails, chips, and clear hierarchy.
- Admin command panels instead of generic settings rows.
- Buttons, chips, alert colors, spacing, and section titles should remain consistent across screens.

Avoid:

- Generic plain rows where managed cards are more appropriate.
- Letter-only navigation icons.
- Redundant labels such as `Doctor DR. NAME`.
- Noisy dashboard alerts when there is no actionable patient.

## 13. Validation Rules

Patient validation:

- Patient name is required.
- Age is required.
- Blood Group is required.
- Mobile number is required and must contain 10 digits.
- Block is required.
- Village is required.
- GRAVIDA is required.
- LMP Date is required and cannot be future dated.
- EDD Date is required and cannot be before LMP.
- Scheduled Delivery Date is optional but cannot be before LMP.
- Doctor name is required.
- Entry / 1st Visit is required and cannot be future dated.
- 2nd/3rd/Final Visit must use `YYYY-MM-DD` when present.
- Final Visit cannot be before previous visit dates.

Report filter validation:

- From and To must use `YYYY-MM-DD` when present.
- From cannot be after To.
- Status must be All, Open, or Locked.

Admin validation:

- Add User requires valid email.
- Password must be at least 6 characters.
- Role must be STAFF or ADMIN.
- Doctor/Motivator reference names are required and saved uppercase.

## 14. Firebase and Data Model

Firebase:

- Firebase Authentication handles app login.
- Firestore stores patients and role records.
- A secondary Firebase app is used for admin-created user accounts so admin stays logged in.
- Patient listener keeps local SQLite cache updated from cloud.
- Local SQLite is used as device cache and for local export/report queries.

Important patient fields:

- serial number
- patient ID
- patient name
- age
- blood group
- mobile number
- state/district/block/village
- GRAVIDA
- last delivery method
- motivator name
- doctor name
- LMP Date
- Pregnancy Age, calculated at UI/export time
- EDD Date
- Scheduled Delivery Date
- Scheduled Delivery Called At
- Scheduled Delivery Called By
- Visit 1
- Visit 2
- Visit 3
- Final Visit
- Entry Date
- Created By
- Updated By
- Record Locked

## 15. Latest Release State

Latest published release:

- Version: `1.1.18`
- Version code: `26`
- Git tag: `v1.1.18`
- Release URL: `https://github.com/0xhydraOp/maternal-tracker-india-android/releases/tag/v1.1.18`
- APK asset: `MaternalTrackerIndia-v1.1.18-release.apk`

Latest verification:

- Unit tests passed.
- Java compile passed.
- Debug lint passed.
- Signed release build passed.
- ADB install passed.
- ADB smoke test passed for Home, Entry new fields/dropdown, Search, Reports, Admin reference controls, installed version, and logcat fatal check.

## 16. Recent Release Summary

### v1.1.18

- Kept scheduled delivery patients visible in the 7-day delivery window after call notification.
- Enlarged and bolded Patient Detail field text for better readability.
- Polished cards, buttons, empty states, form labels, report status colors, and press animations across the app.

### v1.1.17

- Added mandatory Age and Blood Group fields to Basic Info.
- Added mandatory GRAVIDA and optional Method of Last Delivery fields to Pregnancy Dates.
- Synced the new fields through SQLite, Firebase, Firestore rules, patient detail, search cards, and exports.
- Reworked Patient Detail into a compact grouped panel for faster review from the View button.
- Added validation tests for the new required fields.

### v1.1.16

- Forced patient-entry text inputs to stay uppercase while typing and before save.
- Fixed Doctor and Motivator dropdowns so saved names appear while staff types.
- Removed the extra `Doctor` prefix from Search result care rows.
- Reworked Admin Doctor/Motivator manager with polished managed rows, uppercase add dialogs, and safer remove confirmation.
- Normalized custom Doctor/Motivator lookup storage and removal to avoid lowercase duplicates.

### v1.1.15

- Replaced placeholder bottom-navigation letters with native icons.
- Upgraded patient search cards with hierarchy, status color rails, and clinical priority chips.
- Reorganized Admin panel into command panels.

### v1.1.14

- Added automatic pregnancy age display from LMP.
- Refined profile drawer.
- Hardened report filter validation, admin access guarding, and admin dialog validation.
- Excluded completed records from active EDD queues.

### v1.1.10 to v1.1.13

- Allowed future planned 2nd/3rd/final visit dates.
- Added visit follow-up tracking due within 7 days.
- Added in-app updater for staff and admin.
- Reworked Home dashboard around patient-specific Today's Work and priority cards.
- Removed duplicate/noisy dashboard panels.

### v1.1.2 to v1.1.9

- Added Scheduled Delivery Date workflow.
- Added scheduled-delivery calls, call-pending state, patient-notified marking, and completion-required state.
- Added scheduled-delivery reports and exports.
- Fixed Firebase patient ownership and scheduled-delivery permission/save behavior.
