# Blue Bird Maternal Tracker Specification

Last updated: 2026-10-12
Current version: `1.1.46` / versionCode `55`
Latest published version: `1.1.46` / versionCode `55`
Release: https://github.com/0xhydraOp/maternal-tracker-india-android/releases/tag/v1.1.46
Package: `com.maternaltracker.india`  
Hospital scope: `BLUE BIRD A GENERAL HOSPITAL`

## 1. Product Scope

### Rajibul Biswas Prescription Update v1.1.46

- Replaced the existing Rajibul prescription asset with the user's exact supplied `r biswas.jpg`, preserving original bytes and artwork.
- Print Desk credentials now reflect Malda Medical College and Hospital, M.B.B.S., MD (Pediatrics) and WBMC-84100 from the replacement pad.
- Asset identity, A4 portrait orientation, role access and printing callbacks remain unchanged.

### Print Desk Visual Refinements v1.1.45

- Category rail groups Prescriptions, OT Papers and Other Forms; all five original category choices remain.
- Fixed-form tiles share the selector thumbnail/title/metadata treatment, retaining blue borders and selected checkmarks.
- All workspaces place the primary Epson action first, followed by secondary preview and Android Print/Save as PDF actions. Buttons wrap rather than clip enlarged text.
- Full-page previews use a neutral grey stage, white paper with subtle shadow and Page X of Y labels outside the image.
- No printing dispatch, original document assets, permissions or patient logic changed.

### Print Desk Popup Polish v1.1.44

- App-owned doctor/OT selectors, document library (including favourites/recent), full-page previews and print confirmations share white selection sheets with navy headings, blue actions and subtle motion.
- Doctor rows include original-document thumbnails, bold names, readable qualifications and emphasized registration details. Selected rows use a pale-blue surface, blue outline and checkmark.
- Paper orientation and page counts remain visible; short OT lists size to their content while long lists scroll. Footer controls remain reachable with the keyboard visible.
- Preview/confirmation bodies scroll independently of their controls, retaining full original artwork and both pages of multi-page forms.
- Epson preparation uses a matching compact panel. Android/Epson system screens are not reskinned.
- Document assets, favourites/recent persistence, selection callbacks, A4/BHT orientation rules, printing dispatch and Admin/Staff access are unchanged.

### Navigation Simplification v1.1.43

- Profile contains one Print Desk entry instead of five category shortcuts; Entry and Search remain in bottom navigation. Every original print document remains accessible in Print Desk.
- Staff retains ownership-scoped Excel/PDF exports from Search and patient details. The separate Export Center destination is removed from the profile.
- Admin > Advanced Maintenance contains hospital-wide Excel/PDF export, CSV export (all patients or the last search), and local backup/restore. Patient Recovery remains a separate Admin control.
- CSV is absent from everyday Search for both roles and guarded as Admin-only at its execution boundary.
- Reports remain Admin-only: one Priority Overview, expandable Delivery & Follow-up, Patients by Village, and Monthly Registrations containing the trend and clickable month rows. Date filtering and export queries are unchanged.
- No changes to patient validation, completion eligibility, calling, online synchronization, user management, recovery, update handling, or original print artwork.

### Release v1.1.42

- Search uses a native lazy ListView, background queries, a 180ms typing delay, and screen/session/request guards. Returning restores the first visible row and offset; new queries reset to the top.
- Authoritative cloud snapshots reconcile by stable patient ID in a transaction, preserve local row IDs, and update only changed data on a serial worker. SQLite version 8 adds active-patient indexes. Unchanged sync snapshots avoid rebuilding Home.
- Excel/PDF/CSV exports run in the background. Picker export scope and owner survive Activity recreation; output closes before saved/share feedback; account changes and destruction cancel work.
- Reports combine overview counts and group monthly counts in single queries. Monthly and village sections construct their hidden content when expanded.
- Preview images load asynchronously with an 8 MiB maximum cache and stale-request guards. Original print image assets and full-resolution printing are unchanged.
- Priority and Upcoming EDD use compact sky-blue bodies, navy patient names, applicable dates, countdowns, separate phone taps and village. Expanded lists restore on Back. No watermark. Both roles can complete accessible eligible records; Staff can soft-delete owned records, including locked records. Admin alone can unlock, recover and restore.
- Patient Detail defaults to Delivery Status, Patient and Pregnancy, with compact sky-blue bodies, navy titles and bold 16sp values. Full Details expands secondary groups without nested cards or changing workflow.
- Cloud transactions atomically allocate monthly patient IDs and global serial numbers from the entered registration month. Stable-ID reconciliation, stale-form checks and mutation-conflict guards prevent obsolete state from replacing newer records.
- Follow-up selects the earliest upcoming second, third or final visit within seven days, retaining overdue tracking when appropriate. Overdue EDD takes completion precedence over a later upcoming scheduled delivery; completion preserves actual final visits and validates chronology against planned visits.
- Doctor/motivator references come from a shared cloud catalog with validated transactional changes and synchronized dropdowns.
- Fresh server role checks retry transient failures after 1, 2 and 4 seconds with identity/lifecycle guards; failed authorization never falls back to an unchecked cached role.
- Home retains compact summary links, seven-day eligibility, overdue reminders, the 30-day EDD range and Admin/Staff scopes. See section 15 for targeted verification and remaining limits.

### Search Layout Update (Included in v1.1.42)

- Compact, accessible search field replaces the large explanatory banner.
- Existing shortcut filters, exports and New Patient action are grouped in a collapsed-by-default "Filters & actions" section.
- Result count and selected shortcut remain visible in one compact results heading.
- Queries, staff/admin scope, export filtering and patient actions are unchanged. This is a Search-only change, not the cancelled app-wide polish.

### Prescription Update v1.1.41

- Replaced Dr. Arnab Saha's prescription with the supplied 906 x 1280 original JPEG, without recompression or artwork changes. A4 portrait printing remains unchanged.
- The cancelled UI refresh is not included. Version code 49 was used locally for the cancelled refresh and rollback; this release uses 50 to update those installations safely.

### Usability Update v1.1.40

- Shared white surfaces, blue primary actions, clearer section typography, and native pressed feedback.
- Search tab and Back navigation restore the query, shortcut and list position during the session; fresh registration still opens an unfiltered list.
- Report filters, expanded sections and results position survive navigation during the session. Session UI state clears at logout.
- Patient details emphasise name, tappable mobile number and delivery target, with larger values and a single-column layout at large font sizes or narrow widths.
- Print Desk includes a searchable document library, per-login on-device favourites and six recent print handoffs, thumbnails, page counts and A4 orientation. Recent indicates a handed-off job, not confirmed physical printing.
- Original document assets and all patient clinical rules remain unchanged.

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
- Can complete eligible records, soft-delete hospital records, unlock records and restore deleted records from Recovery.

### Staff

- Can log in with Firebase credentials.
- Can create and update patient records.
- Can search patients, update visit dates, call patients, export allowed data, and use the in-app updater.
- Can complete eligible owned records and soft-delete owned records, including completed/locked records; cannot unlock or restore records or change ownership.
- Cannot access admin-only user and reference-management controls.

## 2A. Hospital Print Desk

The profile panel exposes one Print Desk link to both Admin and Staff. The desk retains all document categories:

- Print Prescriptions
- Print OT Papers
- Print Baby Birth Form
- Print Blood Requisition
- Print Baby Refer Form

Print Desk is also a primary bottom navigation destination. Reports are Admin-only, accessible from Profile and Administration rather than bottom navigation. Staff retains Entry, Search and Print in bottom navigation; Admin also has Administration.

Printing requirements:

- Printing uses a unified visual Print Desk with category navigation, document selection, exact preview, print summary, and confirmation.
- Print Desk styling uses a no-gold royal-blue, cyan, white, and cool-graphite visual system.
- Print Desk shows a printer readiness banner before document selection.
- Doctor and OT choices use custom bold selection sheets with supporting credentials or orientation details; the Baby Identification form uses the same selected-document visual language.
- Print category navigation uses document cards with document-type metadata.
- Every Print Desk document supports a full-page preview before printing.
- Document previews use a soft A4 paper-stage treatment and high-quality document language.
- Epson Smart Panel is the primary action when installed; Android printers / Save as PDF and print-service management remain secondary actions.
- Epson Smart Panel receives an untouched copy of original single-page bundled JPEG documents through its supported image-print activity. Multi-page documents are prepared as a print-ready PDF containing the supplied page images in order.
- Android printers / Save as PDF opens Android's destination chooser for other enabled printers and PDF output.
- A4 is the default paper size; the operator may change supported media in the Android print dialog.
- Supplied hospital documents are printed from their original bundled image files.
- Document artwork, text, logos, borders, spacing, and layout must not be recreated, transcribed, or recompressed.
- Documents scale proportionally and remain centered inside the selected paper's printable area.
- Portrait documents default to portrait A4; the BHT / Input & Output Chart defaults to landscape A4.
- Android print writes run on a managed worker, check cancellation between pages/output writes, report only written page ranges and clean up on replacement writes or adapter finish. Preview sampling never affects original print output.

Prescription selection:

- DR. SUYETA NASRIN - MBBS, DNB, DGO (NEW DELHI)
- DR. ARNAB SAHA - M.B.B.S., M.S. (Obstetrics & Gynaecology)
- DR. SUDIPTA BISWAS - MBBS, PGPN, DCH
- DR. PIARUL SK - MBBS, MS (General Surgeon)
- DR. RAJU MAHAJAN - M.B.B.S. (WBUHS), M.S. Obstetrics & Gynaecology
- DR. RAJIBUL BISWAS - M.B.B.S. MD. (Pediatrics), Regd. No.: WBMC-84100

OT Paper selection is a single-choice document sheet. The operator selects and prints one of:

- BHT / Input & Output Chart - Bed Sheet
- Anaesthetic Note
- OT Note

Baby Identification Form is a dedicated single-document print action.

Blood / Component Requisition Form is a dedicated two-page A4 portrait print action. When the operator prints this form, both supplied blood requisition pages print together in order.

Baby Refer Form is a dedicated two-page A4 portrait print action. When the operator prints this form, both supplied referral pages print together in order.

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
- Follow-up selects the earliest upcoming second, third or final visit within that window rather than the last populated field; overdue follow-up remains visible when no upcoming date applies.
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
- Completion is available within the scheduled-delivery or EDD seven-day window, including overdue dates. An overdue EDD takes precedence over a later upcoming scheduled delivery.
- Completion preserves an existing actual Final Visit; otherwise it records the operator's completion date. It rejects chronology conflicts with earlier visit fields rather than discarding planned dates, then locks and syncs the record.

## 7. Home Dashboard

The Home dashboard should remain compact, professional, and action-focused.

Required Home areas:

- Hospital overview with live date/time, sync status, role, Murshidabad/West Bengal context, and record count.
- Priority work cards only when matching patients exist.
- Today's Work section for patient-specific actions.
- Compact KPI row for total, due week, urgent scheduled, completion due, calls, visits, today, and completed.
- Upcoming EDD list for open records.

Priority order:

1. Delivery completion due from scheduled delivery or EDD.
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
- Mark Completed for accessible records within the delivery/EDD eligibility window
- Delete for Admin or the owning Staff account, including locked records
- Unlock and Recovery/Restore for Admin only

## 9. Reports

Reports should stay organized and operational, not generic.

Required report controls:

- Month selector with live month names
- From/to registration dates
- View Records, Excel and PDF actions using the selected range

Required report sections:

- New Patient This Month (live current-month registrations)
- Report Controls
- Priority Overview with selected-range counts and drill-down actions
- Delivery & Follow-up (expandable delivery counts and visit progress)
- Patients by Village
- Monthly Registrations (expandable trend and clickable monthly totals)

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
- Advanced Maintenance
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

Data and Recovery:

- Patient Recovery

Advanced Maintenance:

- Hospital Export (all-patient Excel/PDF and access to filtered Search)
- CSV export: all hospital patients or the last Search results
- Local database backup creation, export and restore
- Admin-only access; Staff contextual Excel/PDF exports remain in Search and patient details

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
- XLSX streams 28 columns into the ZIP without retaining the entire worksheet; AA/AB references and caller-owned stream lifetime are preserved.
- PDF includes the 28 export fields plus Created By, Updated By and record status. Text wraps to the A4 printable width and paginates with repeated headers, cancellation guards and page/document cleanup. Deprecated Remarks, municipality/ward/subdistrict and deletion metadata are not reintroduced into clinical exports.
- Single patient Excel/PDF export is available from Patient Detail.
- Patient Detail defaults to Delivery Status, Patient, and Pregnancy (including Doctor), using navy titles and patient names over compact sky-blue bodies with bold 16sp values. View Full Details expands Care & Address, Visits & Record, and record/export tools in place; Hide Full Details collapses them again. Everyday actions remain below the compact summary.
- Admin may delete any active patient record; Staff may delete their own active records, including completed records, into Recovery. Recovery and restore remain Admin-only. Both roles may mark their accessible records completed under the existing delivery-window eligibility rules. Staff cannot change ownership or unlock/restore records. Firestore deletion updates change only deletion/audit fields.
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
- Compact sky-blue content bodies with navy titles and patient names.
- Compact professional layout.
- Hospital-branded header.
- Native icon bottom navigation for Home, Entry, Search, Print, and Admin; Reports remain available through Administration.
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
- Cloud patient creation atomically allocates the entered registration month's sequence and global serial number with the patient document. Device-local counters are not authoritative.
- Patient IDs reconcile to stable local row IDs. Obsolete snapshots, stale forms and mutation conflicts cannot silently overwrite newer state.
- Doctor and Motivator names use the shared cloud reference catalog; Admin changes are validated and transactional, while Staff dropdowns receive synchronized names.

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

v1.1.44 published verification (2026-10-12): 118 automated tests passed; release lint has zero errors and five existing warnings. Focused review and targeted ADB checks covered Print Desk selectors, selected state, search/empty results, favourites/recent, previews, confirmation cancellation, keyboard-visible controls and 130% fonts (restored). Save as PDF confirmed one landscape A4 BHT page and two portrait A4 blood-form pages. All 15 original assets are byte-identical. Physical printer output and the Epson preparation handoff were not submitted during this test. The signed APK was installed with Admin login retained; GitHub uploaded asset SHA-256 matches `236759dc4050bc10bca990dfeed08493a2496ed942c5b8a8ea35738bbda291b5`.

v1.1.43 final verification (2026-10-12): automated tests passed (118); release lint has zero errors and five existing warnings. Focused independent review found no blockers. Admin device checks covered the shortened Profile, combined report expansion, monthly drill-down, filtered Excel and last-search CSV, maintenance/backup/export routes and Back behavior. Staff device checks covered the shortened Profile, absence of Admin utilities and CSV, retained Print Desk, required-field validation, and ownership-scoped empty-result PDF/Excel export. Temporary Staff access and test files were removed; no patient data was changed. Physical printing and destructive database restore were not performed for this navigation-only release.

The signed v1.1.43 APK was installed via ADB with the Admin session retained, then published to GitHub. Uploaded asset SHA-256 matches the verified local file: `1df11c89f69733c7f3c1293d165256687727ce9fad56aef861a67f88dbf9440a`.

Previous published release verification:

Published release: `1.1.42` / versionCode `51`. Signed APK uploaded and GitHub asset SHA-256 verified against the local release file: `607891cac3e2a3d7751449e67c5653e83e0d3297e9ae4f0554c43ab2da572030`.

Final targeted verification (2026-10-11):

- Automated tests: 118 tests, zero failures. Latest lint: zero errors, five warnings.
- Admin ADB acceptance passed for registration, future visit editing, September monthly counts and filtered Excel/PDF export, call-notification persistence in the delivery window, completion, soft-delete, Patient Recovery, backup creation and update checking.
- Staff ADB acceptance passed for registration, future visit editing, completion, deletion of locked owned records, session restart, ownership scope, Print Desk and update checking.
- Staff export outputs passed after authorization retry: XLSX 2,252 bytes and PDF 54,147 bytes. The earlier empty PDF followed a device DNS failure reaching Firestore during fresh authorization, not PDF serialization. Temporary diagnostic Info logs were removed; export catch error logging remains.
- Android Save as PDF outputs were inspected with pypdf: prescription one A4 portrait page, BHT one A4 landscape page, blood requisition two A4 portrait pages, baby referral two A4 portrait pages, and Staff baby identification one A4 portrait page. Original bundled images remain unchanged.
- Physical printer output was not tested because no printer was available. Runtime database backup restore and all real-call paths remain untested; backup creation and call-notification state checks do not imply those paths passed.
- Acceptance is targeted rather than exhaustive. No speed benchmark or measured performance gain is claimed, and no QA account credentials or patient identifiers are recorded here.

## 16. Recent Release Summary

### v1.1.43

- Shortened profile navigation and consolidated Admin-only maintenance tools.
- Kept contextual Staff/Admin Excel/PDF exports and moved CSV into Admin maintenance with last-search scope retained.
- Combined report breakdowns without changing their queries or drill-downs.

### v1.1.42

- Compact patient summaries and explicit Staff-owned completion/deletion with Admin-only recovery.
- Stable-ID reconciliation, atomic entry-date-based cloud sequences, background search/export/preview work and guarded stale mutations.
- Earliest upcoming visit selection, EDD-aware completion priority, shared reference catalogs and bounded fresh-authorization retries.
- Streaming 28-column XLSX, wrapped clinical PDF exports and asynchronous cancellable original-quality printing. Final targeted verification and limits are recorded in section 15.

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
