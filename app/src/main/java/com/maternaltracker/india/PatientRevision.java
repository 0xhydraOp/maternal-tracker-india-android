package com.maternaltracker.india;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Domain revision for optimistic edits; excludes device identity and last-writer attribution. */
final class PatientRevision {
    private PatientRevision() {}

    /** Detached baseline preserving every field, including nulls and local/audit identity. */
    static Patient copy(Patient patient) {
        if (patient == null) {
            throw new IllegalArgumentException("Cannot copy a missing patient");
        }
        Patient copy = new Patient();
        copy.id = patient.id;
        copy.serialNumber = patient.serialNumber;
        copy.patientId = patient.patientId;
        copy.patientName = patient.patientName;
        copy.age = patient.age;
        copy.bloodGroup = patient.bloodGroup;
        copy.mobileNumber = patient.mobileNumber;
        copy.stateName = patient.stateName;
        copy.districtName = patient.districtName;
        copy.subdistrictName = patient.subdistrictName;
        copy.localBodyType = patient.localBodyType;
        copy.localBodyName = patient.localBodyName;
        copy.wardName = patient.wardName;
        copy.villageName = patient.villageName;
        copy.gravida = patient.gravida;
        copy.lastDeliveryMethod = patient.lastDeliveryMethod;
        copy.lmpDate = patient.lmpDate;
        copy.eddDate = patient.eddDate;
        copy.motivatorName = patient.motivatorName;
        copy.doctorName = patient.doctorName;
        copy.visit1 = patient.visit1;
        copy.visit2 = patient.visit2;
        copy.visit3 = patient.visit3;
        copy.finalVisit = patient.finalVisit;
        copy.scheduledDeliveryDate = patient.scheduledDeliveryDate;
        copy.scheduledDeliveryCalledAt = patient.scheduledDeliveryCalledAt;
        copy.scheduledDeliveryCalledBy = patient.scheduledDeliveryCalledBy;
        copy.entryDate = patient.entryDate;
        copy.createdBy = patient.createdBy;
        copy.updatedBy = patient.updatedBy;
        copy.deletedAt = patient.deletedAt;
        copy.deletedBy = patient.deletedBy;
        copy.remarks = patient.remarks;
        copy.recordLocked = patient.recordLocked;
        return copy;
    }

    static Map<String, Object> capture(Patient patient) {
        if (patient == null) {
            throw new IllegalArgumentException("Cannot capture a missing patient");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("serialNumber", patient.serialNumber);
        put(values, "patientId", patient.patientId);
        put(values, "patientName", patient.patientName);
        put(values, "age", patient.age);
        put(values, "bloodGroup", patient.bloodGroup);
        put(values, "mobileNumber", patient.mobileNumber);
        put(values, "stateName", patient.stateName);
        put(values, "districtName", patient.districtName);
        put(values, "subdistrictName", patient.subdistrictName);
        put(values, "localBodyType", patient.localBodyType);
        put(values, "localBodyName", patient.localBodyName);
        put(values, "wardName", patient.wardName);
        put(values, "villageName", patient.villageName);
        put(values, "gravida", patient.gravida);
        put(values, "lastDeliveryMethod", patient.lastDeliveryMethod);
        put(values, "lmpDate", patient.lmpDate);
        put(values, "eddDate", patient.eddDate);
        put(values, "motivatorName", patient.motivatorName);
        put(values, "doctorName", patient.doctorName);
        put(values, "visit1", patient.visit1);
        put(values, "visit2", patient.visit2);
        put(values, "visit3", patient.visit3);
        put(values, "finalVisit", patient.finalVisit);
        put(values, "scheduledDeliveryDate", patient.scheduledDeliveryDate);
        put(values, "scheduledDeliveryCalledAt", patient.scheduledDeliveryCalledAt);
        put(values, "scheduledDeliveryCalledBy", patient.scheduledDeliveryCalledBy);
        put(values, "entryDate", patient.entryDate);
        put(values, "createdBy", patient.createdBy);
        put(values, "deletedAt", patient.deletedAt);
        put(values, "deletedBy", patient.deletedBy);
        put(values, "remarks", patient.remarks);
        values.put("recordLocked", patient.recordLocked);
        return Collections.unmodifiableMap(values);
    }

    static boolean matches(Patient expected, Patient current) {
        return expected != null && current != null && capture(expected).equals(capture(current));
    }

    static boolean matches(Map<String, ?> expected, Patient current) {
        return expected != null && current != null && expected.equals(capture(current));
    }

    private static void put(Map<String, Object> values, String field, String value) {
        values.put(field, value == null ? "" : value);
    }
}
