package com.maternaltracker.india;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class FirebaseGateway {
    private static final String ADMIN_BOOTSTRAP_EMAIL = "iamrobiul94@gmail.com";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_STAFF = "STAFF";
    private static final String SECONDARY_AUTH_APP = "blue_bird_user_creator";

    private final Context context;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private ListenerRegistration patientListener;

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

    static final class Session {
        final String email;
        final String role;

        Session(String email, String role) {
            this.email = email;
            this.role = role;
        }
    }

    void signIn(String email, String password, Result<Session> result) {
        String normalized = normalizeEmail(email);
        auth.signInWithEmailAndPassword(normalized, password)
                .addOnSuccessListener(authResult -> loadRole(normalized, result))
                .addOnFailureListener(error -> result.onComplete(null, error));
    }

    void restoreSession(Result<Session> result) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            result.onComplete(null, null);
            return;
        }
        loadRole(normalizeEmail(user.getEmail()), result);
    }

    void signOut() {
        stopPatientListener();
        auth.signOut();
    }

    void startPatientListener(PatientsListener listener) {
        stopPatientListener();
        patientListener = firestore.collection("patients")
                .orderBy("entryDate", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
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
        if (patientListener != null) {
            patientListener.remove();
            patientListener = null;
        }
    }

    void savePatient(Patient patient, Result<Void> result) {
        firestore.collection("patients")
                .document(safeDocumentId(patient.patientId))
                .set(patientMap(patient))
                .addOnSuccessListener(unused -> result.onComplete(null, null))
                .addOnFailureListener(error -> result.onComplete(null, error));
    }

    void deletePatient(Patient patient, Result<Void> result) {
        firestore.collection("patients")
                .document(safeDocumentId(patient.patientId))
                .delete()
                .addOnSuccessListener(unused -> result.onComplete(null, null))
                .addOnFailureListener(error -> result.onComplete(null, error));
    }

    void saveRole(String email, String role, Result<Void> result) {
        Map<String, Object> values = new HashMap<>();
        values.put("email", normalizeEmail(email));
        values.put("role", normalizeRole(role));
        values.put("active", true);
        values.put("updatedAt", Timestamp.now());
        values.put("updatedBy", currentEmail());
        firestore.collection("user_roles")
                .document(normalizeEmail(email))
                .set(values)
                .addOnSuccessListener(unused -> result.onComplete(null, null))
                .addOnFailureListener(error -> result.onComplete(null, error));
    }

    void createAuthUserAndRole(String email, String password, String role, Result<Void> result) {
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
                    saveRole(normalized, role, result);
                })
                .addOnFailureListener(error -> {
                    creatorAuth.signOut();
                    if (error instanceof FirebaseAuthUserCollisionException) {
                        saveRole(normalized, role, result);
                        return;
                    }
                    result.onComplete(null, error);
                });
    }

    void deleteRole(String email, Result<Void> result) {
        firestore.collection("user_roles")
                .document(normalizeEmail(email))
                .delete()
                .addOnSuccessListener(unused -> result.onComplete(null, null))
                .addOnFailureListener(error -> result.onComplete(null, error));
    }

    void listRoles(Result<List<String[]>> result) {
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
                    result.onComplete(rows, null);
                })
                .addOnFailureListener(error -> result.onComplete(null, error));
    }

    private void loadRole(String email, Result<Session> result) {
        firestore.collection("user_roles").document(email).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists() && ADMIN_BOOTSTRAP_EMAIL.equals(email)) {
                        saveRole(email, ROLE_ADMIN, (unused, error) -> {
                            if (error == null) {
                                result.onComplete(new Session(email, ROLE_ADMIN), null);
                            } else {
                                result.onComplete(null, error);
                            }
                        });
                        return;
                    }
                    if (!doc.exists() || Boolean.FALSE.equals(doc.getBoolean("active"))) {
                        result.onComplete(null, new IllegalStateException("This login is not authorised for Blue Bird Hospital."));
                        return;
                    }
                    String storedEmail = doc.getString("email");
                    if (storedEmail != null && !email.equals(normalizeEmail(storedEmail))) {
                        result.onComplete(null, new IllegalStateException("This login role is misconfigured."));
                        return;
                    }
                    result.onComplete(new Session(email, normalizeRole(doc.getString("role"))), null);
                })
                .addOnFailureListener(error -> result.onComplete(null, error));
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
