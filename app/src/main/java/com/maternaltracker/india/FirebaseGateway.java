package com.maternaltracker.india;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class FirebaseGateway {
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_STAFF = "STAFF";
    private static final String SECONDARY_AUTH_APP = "blue_bird_user_creator";
    private static final int MAX_REFERENCE_NAMES = 2000;
    private static final int MAX_REFERENCE_NAME_LENGTH = 200;
    private static final String[] REFERENCE_TABLES = {"custom_doctors", "custom_motivators"};

    private final Context context;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private ListenerRegistration patientListener;
    private ListenerRegistration roleListener;
    private final List<ListenerRegistration> referenceListeners = new ArrayList<>();
    private FirebaseAuth.AuthStateListener roleAuthListener;
    private Session authorizedSession;
    private volatile long authGeneration;
    private long patientGeneration;
    private volatile long roleGeneration;
    private long referenceGeneration;

    FirebaseGateway(Context context) {
        this.context = context.getApplicationContext();
    }

    interface Result<T> {
        void onComplete(T value, Exception error);
    }

    interface PatientsListener {
        void onPatients(List<Patient> patients);

        void onError(Exception error);
    }

    interface ReferenceListener {
        void onNames(String table, List<String> names);

        void onError(Exception error);
    }

    static final class Session {
        final String email;
        final String role;

        Session(String email, String role) {
            this.email = email;
            this.role = role;
        }
    }

    void signIn(String email, String password, Result<Session> result) {
        invalidateSession();
        final long generation = authGeneration;
        String normalized = normalizeEmail(email);
        auth.signInWithEmailAndPassword(normalized, password)
                .addOnSuccessListener(authResult -> {
                    if (generation == authGeneration) loadRole(normalized, generation, result);
                })
                .addOnFailureListener(error -> {
                    if (generation == authGeneration) result.onComplete(null, error);
                });
    }

    void restoreSession(Result<Session> result) {
        invalidateSession();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            result.onComplete(null, null);
            return;
        }
        loadRole(normalizeEmail(user.getEmail()), authGeneration, result);
    }

    void signOut() {
        invalidateSession();
        auth.signOut();
    }

    void startPatientListener(PatientsListener listener) {
        Session session = authorizedSession;
        startPatientListener(session == null ? null : session.role,
                session == null ? null : session.email, listener);
    }

    void startPatientListener(String role, String email, PatientsListener listener) {
        stopPatientListener();
        String owner = normalizeEmail(email);
        Session session = authorizedSession;
        if (session == null || !session.email.equals(owner) || !session.role.equals(role)
                || !owner.equals(currentEmail())) {
            listener.onError(new IllegalStateException("A validated current session is required"));
            return;
        }
        final String uid = auth.getCurrentUser().getUid();
        final long generation = patientGeneration;
        Query query = firestore.collection("patients");
        if (ROLE_ADMIN.equals(role)) {
            query = query.orderBy("entryDate", Query.Direction.DESCENDING);
        } else {
            // SQLite provides ordering; the equality-only query needs no composite index.
            query = query.whereEqualTo("createdBy", owner);
        }
        patientListener = query.addSnapshotListener((snapshot, error) -> {
                    if (generation != patientGeneration || !sameIdentity(uid, owner)) return;
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<Patient> patients = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            patients.add(patientFromDocument(doc));
                        }
                    }
                    listener.onPatients(patients);
                });
    }

    void stopPatientListener() {
        patientGeneration++;
        if (patientListener != null) {
            patientListener.remove();
            patientListener = null;
        }
    }

    void startReferenceListener(ReferenceListener listener) {
        stopReferenceListener();
        Session session = authorizedSession;
        FirebaseUser user = auth.getCurrentUser();
        if (session == null || user == null || !session.email.equals(currentEmail())) {
            listener.onError(new IllegalStateException("A validated current session is required"));
            return;
        }
        final String owner = session.email;
        final String uid = user.getUid();
        final long generation = referenceGeneration;
        for (String table : REFERENCE_TABLES) {
            ListenerRegistration registration = firestore.collection("hospital_references").document(table)
                    .addSnapshotListener(MetadataChanges.INCLUDE, (doc, error) -> {
                        if (generation != referenceGeneration || !sameIdentity(uid, owner)) return;
                        if (error != null) {
                            listener.onError(error);
                            return;
                        }
                        if (doc != null && doc.getMetadata().hasPendingWrites()) return;
                        final List<String> names;
                        try {
                            names = referenceNames(doc, table);
                        } catch (Exception invalidDocument) {
                            listener.onError(invalidDocument);
                            return;
                        }
                        listener.onNames(table, names);
                    });
            referenceListeners.add(registration);
        }
    }

    void stopReferenceListener() {
        referenceGeneration++;
        for (ListenerRegistration registration : referenceListeners) registration.remove();
        referenceListeners.clear();
    }

    void saveReferenceNames(String table, List<String> names, Result<Void> result) {
        if (!hasAdminSession()) {
            result.onComplete(null, new IllegalStateException("A validated administrator session is required"));
            return;
        }
        final List<String> validated;
        try {
            requireReferenceTable(table);
            validated = validatedReferenceNames(names);
        } catch (Exception error) {
            result.onComplete(null, error);
            return;
        }
        Result<Void> delivery = sessionResult(result);
        Map<String, Object> values = new HashMap<>();
        values.put("names", validated);
        firestore.collection("hospital_references").document(table).set(values)
                .addOnSuccessListener(unused -> delivery.onComplete(null, null))
                .addOnFailureListener(error -> delivery.onComplete(null, error));
    }

    /** Apply one normalized edit against the latest server list, retrying on contention. */
    void changeReferenceName(String table, String name, boolean remove, Result<List<String>> result) {
        if (!hasAdminSession()) {
            result.onComplete(null, new IllegalStateException("A validated administrator session is required"));
            return;
        }
        final String normalized;
        try {
            requireReferenceTable(table);
            normalized = validatedReferenceNames(java.util.Collections.singletonList(name)).get(0);
        } catch (Exception error) {
            result.onComplete(null, error);
            return;
        }
        final long generation = authGeneration;
        final long roleEpoch = roleGeneration;
        final String owner = currentEmail();
        final String uid = auth.getCurrentUser().getUid();
        DocumentReference ref = firestore.collection("hospital_references").document(table);
        firestore.<List<String>>runTransaction(transaction -> {
            if (generation != authGeneration || roleEpoch != roleGeneration || !sameIdentity(uid, owner)) {
                throw new FirebaseFirestoreException("Administrator session changed",
                        FirebaseFirestoreException.Code.CANCELLED);
            }
            List<String> names = referenceNames(transaction.get(ref), table);
            if (remove) names.remove(normalized);
            else if (!names.contains(normalized)) names.add(normalized);
            List<String> validated = validatedReferenceNames(names);
            if (generation != authGeneration || roleEpoch != roleGeneration || !sameIdentity(uid, owner)) {
                throw new FirebaseFirestoreException("Administrator session changed",
                        FirebaseFirestoreException.Code.CANCELLED);
            }
            Map<String, Object> values = new HashMap<>();
            values.put("names", validated);
            transaction.set(ref, values);
            return validated;
        }).addOnSuccessListener(names -> {
            if (generation == authGeneration && roleEpoch == roleGeneration && sameIdentity(uid, owner)) {
                result.onComplete(new ArrayList<>(names), null);
            }
        }).addOnFailureListener(error -> {
            if (generation == authGeneration && roleEpoch == roleGeneration && sameIdentity(uid, owner)) {
                result.onComplete(null, error);
            }
        });
    }

    private static void requireReferenceTable(String table) {
        if (!"custom_doctors".equals(table) && !"custom_motivators".equals(table)) {
            throw new IllegalArgumentException("Unsupported hospital reference list");
        }
    }

    private static List<String> referenceNames(DocumentSnapshot doc, String table) {
        if (doc == null || !doc.exists()) {
            throw new IllegalStateException("Hospital reference list is not provisioned: " + table);
        }
        Object raw = doc.get("names");
        if (!(raw instanceof List)) throw new IllegalArgumentException("Invalid hospital reference list: " + table);
        return validatedReferenceNames((List<?>) raw);
    }

    private static List<String> validatedReferenceNames(List<?> values) {
        if (values == null || values.size() > MAX_REFERENCE_NAMES) {
            throw new IllegalArgumentException("Reference lists must contain at most 2000 names");
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (Object value : values) {
            if (!(value instanceof String)) throw new IllegalArgumentException("Reference names must be strings");
            String name = ((String) value).trim().toUpperCase(Locale.US);
            if (name.isEmpty() || name.length() > MAX_REFERENCE_NAME_LENGTH) {
                throw new IllegalArgumentException("Reference names must contain 1 to 200 characters");
            }
            names.add(name);
        }
        return new ArrayList<>(names);
    }

    /** Online gate: transient failures deny the operation without discarding the session. */
    void refreshCurrentRole(Result<String> result) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || normalizeEmail(user.getEmail()).isEmpty()) {
            invalidateSession();
            result.onComplete(null, new IllegalStateException("No signed-in account"));
            return;
        }
        final String owner = normalizeEmail(user.getEmail());
        final String uid = user.getUid();
        final long generation = authGeneration;
        final String previousRole = authorizedSession == null ? null : authorizedSession.role;
        final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        final long[] delays = {1000, 2000, 4000};
        class RefreshAttempt {
            private int retries;

            void run() {
                if (generation != authGeneration || !sameIdentity(uid, owner)) return;
                loadRole(owner, generation, (session, error) -> {
                    if (generation != authGeneration || !sameIdentity(uid, owner)) return;
                    if (error != null) {
                        boolean retryable = false;
                        if (error instanceof FirebaseFirestoreException) {
                            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) error).getCode();
                            retryable = code == FirebaseFirestoreException.Code.UNAVAILABLE
                                    || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
                                    || code == FirebaseFirestoreException.Code.CANCELLED;
                        }
                        if (retryable && retries < delays.length) {
                            handler.postDelayed(this::run, delays[retries++]);
                            return;
                        }
                        if (!isTransientRoleError(error)) {
                            authGeneration++;
                            authorizedSession = null;
                            stopPatientListener();
                            stopReferenceListener();
                        }
                        result.onComplete(null, error);
                        return;
                    }
                    if (!session.role.equals(previousRole)) {
                        authGeneration++;
                        stopPatientListener();
                        stopReferenceListener();
                    }
                    result.onComplete(session.role, null);
                });
            }
        }
        new RefreshAttempt().run();
    }

    static boolean isTransientRoleError(Exception error) {
        if (!(error instanceof FirebaseFirestoreException)) return false;
        switch (((FirebaseFirestoreException) error).getCode()) {
            case UNAVAILABLE:
            case DEADLINE_EXCEEDED:
            case CANCELLED:
            case ABORTED:
                return true;
            default:
                return false;
        }
    }

    /** Emits the current role, or null/error when access cannot be established. */
    void listenCurrentRole(Result<String> result) {
        stopRoleListener();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || normalizeEmail(user.getEmail()).isEmpty()) {
            authorizedSession = null;
            stopPatientListener();
            stopReferenceListener();
            result.onComplete(null, new IllegalStateException("No signed-in account"));
            return;
        }
        final String owner = normalizeEmail(user.getEmail());
        final String uid = user.getUid();
        final long generation = roleGeneration;
        roleAuthListener = ignored -> {
            if (generation != roleGeneration || sameIdentity(uid, owner)) return;
            invalidateSession();
            result.onComplete(null, new IllegalStateException("The signed-in account changed"));
        };
        auth.addAuthStateListener(roleAuthListener);
        roleListener = firestore.collection("user_roles").document(owner)
                .addSnapshotListener(MetadataChanges.INCLUDE, (doc, error) -> {
                    if (generation != roleGeneration || !sameIdentity(uid, owner)) return;
                    // Cached role data must not re-grant an obsolete role after revocation.
                    if (error == null && doc != null && doc.getMetadata().isFromCache()) return;
                    final Session session;
                    try {
                        if (error != null) throw error;
                        session = sessionFromRole(owner, doc);
                    } catch (Exception invalidRole) {
                        authGeneration++;
                        authorizedSession = null;
                        stopPatientListener();
                        stopReferenceListener();
                        result.onComplete(null, invalidRole);
                        return;
                    }
                    if (authorizedSession == null || !session.role.equals(authorizedSession.role)
                            || !session.email.equals(authorizedSession.email)) {
                        authGeneration++;
                        stopPatientListener();
                        stopReferenceListener();
                    }
                    authorizedSession = session;
                    result.onComplete(session.role, null);
                });
    }

    void stopRoleListener() {
        roleGeneration++;
        if (roleListener != null) {
            roleListener.remove();
            roleListener = null;
        }
        if (roleAuthListener != null) {
            auth.removeAuthStateListener(roleAuthListener);
            roleAuthListener = null;
        }
    }

    /** Activity teardown: cancels callbacks/listeners without signing out Firebase Auth. */
    void close() {
        invalidateSession();
    }

    private void invalidateSession() {
        authGeneration++;
        authorizedSession = null;
        stopPatientListener();
        stopRoleListener();
        stopReferenceListener();
    }

    private boolean sameIdentity(String uid, String email) {
        FirebaseUser user = auth.getCurrentUser();
        return user != null && uid.equals(user.getUid()) && email.equals(normalizeEmail(user.getEmail()));
    }

    /** Allocate both counters and create atomically; never overwrite a collided patient. */
    void createPatient(Patient patient, Result<Void> result) {
        FirebaseUser user = auth.getCurrentUser();
        Session session = authorizedSession;
        if (patient == null || user == null || session == null || !session.email.equals(currentEmail())) {
            result.onComplete(null, new IllegalStateException("A validated current session is required"));
            return;
        }
        final String owner = session.email;
        final String uid = user.getUid();
        final long generation = authGeneration;
        final String monthKey;
        final Map<String, Object> values;
        try {
            monthKey = PatientSequence.creationMonth(patient);
            if (ROLE_STAFF.equals(session.role) && !owner.equals(patient.createdBy)) {
                throw new IllegalArgumentException("Staff patient creator must match the signed-in account");
            }
            values = patientMap(patient);
        } catch (Exception error) {
            result.onComplete(null, error);
            return;
        }
        final DocumentReference monthCounter = firestore.collection("patient_sequences").document(monthKey);
        final DocumentReference globalCounter = firestore.collection("patient_sequences").document("all");
        firestore.<PatientSequence.Allocation>runTransaction(transaction -> {
            if (generation != authGeneration || !sameIdentity(uid, owner)) {
                throw new FirebaseFirestoreException("The signed-in account changed",
                        FirebaseFirestoreException.Code.CANCELLED);
            }
            long previousMonth = sequenceValue(transaction.get(monthCounter));
            long previousGlobal = sequenceValue(transaction.get(globalCounter));
            PatientSequence.Allocation allocation = PatientSequence.allocate(monthKey, previousMonth, previousGlobal);
            DocumentReference ref = firestore.collection("patients").document(allocation.patientId);
            if (transaction.get(ref).exists()) {
                throw new FirebaseFirestoreException("Allocated patient ID already exists; sequence counters need repair",
                        FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }
            if (generation != authGeneration || !sameIdentity(uid, owner)) {
                throw new FirebaseFirestoreException("The signed-in account changed",
                        FirebaseFirestoreException.Code.CANCELLED);
            }
            Map<String, Object> allocatedValues = new HashMap<>(values);
            allocatedValues.put("patientId", allocation.patientId);
            allocatedValues.put("serialNumber", allocation.serialNumber);
            Map<String, Object> monthValues = new HashMap<>();
            monthValues.put("lastSequence", allocation.monthSequence);
            Map<String, Object> globalValues = new HashMap<>();
            globalValues.put("lastSequence", allocation.serialNumber);
            transaction.set(monthCounter, monthValues);
            transaction.set(globalCounter, globalValues);
            transaction.set(ref, allocatedValues);
            return allocation;
        }).addOnSuccessListener(allocation -> {
            if (generation != authGeneration || !sameIdentity(uid, owner)) return;
            patient.patientId = allocation.patientId;
            patient.serialNumber = allocation.serialNumber;
            result.onComplete(null, null);
        }).addOnFailureListener(error -> {
            if (generation == authGeneration && sameIdentity(uid, owner)) result.onComplete(null, error);
        });
    }

    private static long sequenceValue(DocumentSnapshot counter) throws FirebaseFirestoreException {
        if (!counter.exists()) return 0;
        Object value = counter.get("lastSequence");
        if (!(value instanceof Long) || (Long) value < 1 || (Long) value >= Integer.MAX_VALUE) {
            throw new FirebaseFirestoreException("Invalid or exhausted patient sequence counter",
                    FirebaseFirestoreException.Code.FAILED_PRECONDITION);
        }
        return (Long) value;
    }

    /** The caller supplies a detached form baseline; a changed server record is never overwritten. */
    void updatePatient(Patient desired, Patient expected, Result<Void> result) {
        FirebaseUser user = auth.getCurrentUser();
        Session session = authorizedSession;
        if (desired == null || expected == null || user == null || session == null
                || !session.email.equals(currentEmail())) {
            result.onComplete(null, new IllegalStateException("A validated current session and patient baseline are required"));
            return;
        }
        final String owner = session.email;
        final String uid = user.getUid();
        final long generation = authGeneration;
        final long roleEpoch = roleGeneration;
        final DocumentReference ref;
        final Map<String, Object> values;
        try {
            String id = safeDocumentId(desired.patientId);
            if (!id.equals(desired.patientId) || !id.equals(expected.patientId)) {
                throw new IllegalArgumentException("Patient identity cannot be changed while editing");
            }
            if (!java.util.Objects.equals(desired.createdBy, expected.createdBy)) {
                throw new IllegalArgumentException("Patient creator cannot be changed while editing");
            }
            boolean canEdit = PatientAccess.canManage(session.role, owner, expected);
            if (!canEdit && ROLE_ADMIN.equals(session.role)
                    && expected.deletedAt != null && !expected.deletedAt.isEmpty()) {
                Map<String, Object> restoredRevision = new HashMap<>(PatientRevision.capture(expected));
                restoredRevision.put("deletedAt", "");
                restoredRevision.put("deletedBy", "");
                canEdit = restoredRevision.equals(PatientRevision.capture(desired));
            }
            if (!canEdit || (ROLE_STAFF.equals(session.role) && expected.recordLocked)) {
                throw new SecurityException("You do not have permission to edit this patient");
            }
            ref = firestore.collection("patients").document(id);
            values = patientMap(desired);
        } catch (Exception error) {
            result.onComplete(null, error);
            return;
        }
        Result<Void> delivery = sessionResult(result);
        firestore.<Void>runTransaction(transaction -> {
            if (generation != authGeneration || roleEpoch != roleGeneration || !sameIdentity(uid, owner)) {
                throw new FirebaseFirestoreException("The signed-in account changed",
                        FirebaseFirestoreException.Code.CANCELLED);
            }
            DocumentSnapshot doc = transaction.get(ref);
            if (!doc.exists() || !PatientRevision.matches(expected, patientFromDocument(doc))) {
                throw new FirebaseFirestoreException("Patient changed while editing. Reopen latest record",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            if (generation != authGeneration || roleEpoch != roleGeneration || !sameIdentity(uid, owner)) {
                throw new FirebaseFirestoreException("The signed-in account changed",
                        FirebaseFirestoreException.Code.CANCELLED);
            }
            Map<String, Object> update = new HashMap<>(values);
            update.put("createdBy", doc.get("createdBy"));
            transaction.update(ref, update);
            return null;
        }).addOnSuccessListener(unused -> {
            if (roleEpoch == roleGeneration) delivery.onComplete(null, null);
        }).addOnFailureListener(error -> {
            if (roleEpoch == roleGeneration) delivery.onComplete(null, error);
        });
    }

    void savePatient(Patient patient, Result<Void> result) {
        Result<Void> delivery = sessionResult(result);
        firestore.collection("patients")
                .document(safeDocumentId(patient.patientId))
                .update(patientMap(patient))
                .addOnSuccessListener(unused -> delivery.onComplete(null, null))
                .addOnFailureListener(error -> delivery.onComplete(null, error));
    }

    void softDeletePatient(Patient patient, Result<Void> result) {
        Result<Void> delivery = sessionResult(result);
        Map<String, Object> changes = new HashMap<>();
        changes.put("deletedAt", patient.deletedAt);
        changes.put("deletedBy", currentEmail());
        changes.put("updatedBy", currentEmail());
        changes.put("updatedAt", Timestamp.now());
        firestore.collection("patients")
                .document(safeDocumentId(patient.patientId))
                .update(changes)
                .addOnSuccessListener(unused -> delivery.onComplete(null, null))
                .addOnFailureListener(error -> delivery.onComplete(null, error));
    }

    void saveRole(String email, String role, Result<Void> result) {
        if (!hasAdminSession()) {
            result.onComplete(null, new IllegalStateException("A validated administrator session is required"));
            return;
        }
        Result<Void> delivery = sessionResult(result);
        Map<String, Object> values = new HashMap<>();
        values.put("email", normalizeEmail(email));
        values.put("role", normalizeRole(role));
        values.put("active", true);
        values.put("updatedAt", Timestamp.now());
        values.put("updatedBy", currentEmail());
        firestore.collection("user_roles")
                .document(normalizeEmail(email))
                .set(values)
                .addOnSuccessListener(unused -> delivery.onComplete(null, null))
                .addOnFailureListener(error -> delivery.onComplete(null, error));
    }

    void createAuthUserAndRole(String email, String password, String role, Result<Void> result) {
        if (!hasAdminSession()) {
            result.onComplete(null, new IllegalStateException("A validated administrator session is required"));
            return;
        }
        final long generation = authGeneration;
        final String owner = currentEmail();
        final String uid = auth.getCurrentUser().getUid();
        String normalized = normalizeEmail(email);
        if (normalized.isEmpty()) {
            result.onComplete(null, new IllegalArgumentException("Email is required"));
            return;
        }
        if (password == null || password.length() < 6) {
            result.onComplete(null, new IllegalArgumentException("Password must be at least 6 characters"));
            return;
        }
        FirebaseAuth creatorAuth = FirebaseAuth.getInstance(secondaryApp());
        creatorAuth.createUserWithEmailAndPassword(normalized, password)
                .addOnSuccessListener(authResult -> {
                    creatorAuth.signOut();
                    if (generation != authGeneration || !sameIdentity(uid, owner)) return;
                    saveRole(normalized, role, result);
                })
                .addOnFailureListener(error -> {
                    creatorAuth.signOut();
                    if (generation != authGeneration || !sameIdentity(uid, owner)) return;
                    if (error instanceof FirebaseAuthUserCollisionException) {
                        result.onComplete(null, new IllegalStateException(
                                "Email already registered. Existing login/password/access were not changed."));
                        return;
                    }
                    result.onComplete(null, error);
                });
    }

    void deleteRole(String email, Result<Void> result) {
        if (!hasAdminSession()) {
            result.onComplete(null, new IllegalStateException("A validated administrator session is required"));
            return;
        }
        Result<Void> delivery = sessionResult(result);
        firestore.collection("user_roles")
                .document(normalizeEmail(email))
                .delete()
                .addOnSuccessListener(unused -> delivery.onComplete(null, null))
                .addOnFailureListener(error -> delivery.onComplete(null, error));
    }

    void listRoles(Result<List<String[]>> result) {
        if (!hasAdminSession()) {
            result.onComplete(null, new IllegalStateException("A validated administrator session is required"));
            return;
        }
        Result<List<String[]>> delivery = sessionResult(result);
        firestore.collection("user_roles")
                .orderBy("email")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String[]> rows = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        rows.add(new String[]{
                                string(doc, "email", doc.getId()),
                                normalizeRole(doc.getString("role")),
                                String.valueOf(!Boolean.FALSE.equals(doc.getBoolean("active")))
                        });
                    }
                    delivery.onComplete(rows, null);
                })
                .addOnFailureListener(error -> delivery.onComplete(null, error));
    }

    private boolean hasAdminSession() {
        return authorizedSession != null && ROLE_ADMIN.equals(authorizedSession.role)
                && authorizedSession.email.equals(currentEmail());
    }

    private <T> Result<T> sessionResult(Result<T> result) {
        final long generation = authGeneration;
        final String owner = currentEmail();
        FirebaseUser user = auth.getCurrentUser();
        final String uid = user == null ? "" : user.getUid();
        return (value, error) -> {
            if (generation == authGeneration && sameIdentity(uid, owner)) result.onComplete(value, error);
        };
    }

    private void loadRole(String email, long generation, Result<Session> result) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || !email.equals(currentEmail())) {
            result.onComplete(null, new IllegalStateException("The signed-in account changed"));
            return;
        }
        final String uid = user.getUid();
        firestore.collection("user_roles").document(email).get(Source.SERVER)
                .addOnSuccessListener(doc -> {
                    if (generation != authGeneration || !sameIdentity(uid, email)) return;
                    final Session session;
                    try {
                        session = sessionFromRole(email, doc);
                    } catch (Exception error) {
                        authorizedSession = null;
                        result.onComplete(null, error);
                        return;
                    }
                    authorizedSession = session;
                    result.onComplete(session, null);
                })
                .addOnFailureListener(error -> {
                    if (generation == authGeneration && sameIdentity(uid, email)) result.onComplete(null, error);
                });
    }

    private static Session sessionFromRole(String email, DocumentSnapshot doc) {
        if (doc == null || !doc.exists() || !Boolean.TRUE.equals(doc.getBoolean("active"))) {
            throw new IllegalStateException("This login is not authorised for Blue Bird Hospital.");
        }
        String role = doc.getString("role");
        if ((!ROLE_ADMIN.equals(role) && !ROLE_STAFF.equals(role))
                || !email.equals(normalizeEmail(doc.getString("email")))) {
            throw new IllegalStateException("This login role is misconfigured.");
        }
        return new Session(email, role);
    }

    private FirebaseApp secondaryApp() {
        try {
            return FirebaseApp.getInstance(SECONDARY_AUTH_APP);
        } catch (IllegalStateException ignored) {
            return FirebaseApp.initializeApp(context, FirebaseOptions.fromResource(context), SECONDARY_AUTH_APP);
        }
    }

    private Patient patientFromDocument(DocumentSnapshot doc) {
        Patient p = new Patient();
        p.patientId = string(doc, "patientId", doc.getId());
        p.serialNumber = intValue(doc, "serialNumber");
        p.patientName = string(doc, "patientName", "");
        p.age = string(doc, "age", "");
        p.bloodGroup = string(doc, "bloodGroup", "");
        p.mobileNumber = string(doc, "mobileNumber", "");
        p.stateName = string(doc, "stateName", "");
        p.districtName = string(doc, "districtName", "");
        p.subdistrictName = string(doc, "subdistrictName", "");
        p.localBodyType = string(doc, "localBodyType", "");
        p.localBodyName = string(doc, "localBodyName", "");
        p.wardName = string(doc, "wardName", "");
        p.villageName = string(doc, "villageName", "");
        p.gravida = string(doc, "gravida", "");
        p.lastDeliveryMethod = string(doc, "lastDeliveryMethod", "");
        p.lmpDate = string(doc, "lmpDate", "");
        p.eddDate = string(doc, "eddDate", "");
        p.scheduledDeliveryDate = string(doc, "scheduledDeliveryDate", "");
        p.scheduledDeliveryCalledAt = string(doc, "scheduledDeliveryCalledAt", "");
        p.scheduledDeliveryCalledBy = string(doc, "scheduledDeliveryCalledBy", "");
        p.motivatorName = string(doc, "motivatorName", "");
        p.doctorName = string(doc, "doctorName", "");
        p.visit1 = string(doc, "visit1", "");
        p.visit2 = string(doc, "visit2", "");
        p.visit3 = string(doc, "visit3", "");
        p.finalVisit = string(doc, "finalVisit", "");
        p.entryDate = string(doc, "entryDate", "");
        p.createdBy = string(doc, "createdBy", string(doc, "updatedBy", ""));
        p.updatedBy = string(doc, "updatedBy", "");
        p.deletedAt = string(doc, "deletedAt", "");
        p.deletedBy = string(doc, "deletedBy", "");
        p.remarks = string(doc, "remarks", "");
        p.recordLocked = Boolean.TRUE.equals(doc.getBoolean("recordLocked"));
        return p;
    }

    private Map<String, Object> patientMap(Patient p) {
        Map<String, Object> values = new HashMap<>();
        values.put("serialNumber", p.serialNumber);
        values.put("patientId", p.patientId);
        values.put("patientName", p.patientName);
        values.put("age", p.age);
        values.put("bloodGroup", p.bloodGroup);
        values.put("mobileNumber", p.mobileNumber);
        values.put("stateName", p.stateName);
        values.put("districtName", p.districtName);
        values.put("subdistrictName", p.subdistrictName);
        values.put("localBodyType", p.localBodyType);
        values.put("localBodyName", p.localBodyName);
        values.put("wardName", p.wardName);
        values.put("villageName", p.villageName);
        values.put("gravida", p.gravida);
        values.put("lastDeliveryMethod", p.lastDeliveryMethod);
        values.put("lmpDate", p.lmpDate);
        values.put("eddDate", p.eddDate);
        values.put("scheduledDeliveryDate", p.scheduledDeliveryDate);
        values.put("scheduledDeliveryCalledAt", p.scheduledDeliveryCalledAt);
        values.put("scheduledDeliveryCalledBy", p.scheduledDeliveryCalledBy);
        values.put("motivatorName", p.motivatorName);
        values.put("doctorName", p.doctorName);
        values.put("visit1", p.visit1);
        values.put("visit2", p.visit2);
        values.put("visit3", p.visit3);
        values.put("finalVisit", p.finalVisit);
        values.put("entryDate", p.entryDate);
        values.put("createdBy", p.createdBy);
        values.put("updatedBy", currentEmail());
        values.put("deletedAt", p.deletedAt);
        values.put("deletedBy", p.deletedBy);
        values.put("recordLocked", p.recordLocked);
        values.put("remarks", p.remarks);
        values.put("updatedAt", Timestamp.now());
        return values;
    }

    private String currentEmail() {
        FirebaseUser user = auth.getCurrentUser();
        return user == null ? "" : normalizeEmail(user.getEmail());
    }

    private static String string(DocumentSnapshot doc, String key, String fallback) {
        String value = doc.getString(key);
        return value == null ? fallback : value;
    }

    private static int intValue(DocumentSnapshot doc, String key) {
        Number number = doc.getLong(key);
        if (number == null) {
            number = doc.getDouble(key);
        }
        return number == null ? 0 : number.intValue();
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.US);
    }

    private static String normalizeRole(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role) ? ROLE_ADMIN : ROLE_STAFF;
    }

    private static String safeDocumentId(String value) {
        String id = value == null ? "" : value.trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Patient ID is required");
        }
        return id.replace("/", "-");
    }
}
