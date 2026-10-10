package com.maternaltracker.india;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class PatientRevisionTest {
    @Test
    public void copyPreservesEveryCurrentFieldIncludingIdentityAndAudit() throws Exception {
        Patient original = new Patient();
        for (Field field : Patient.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (field.getType() == String.class) field.set(original, " value:" + field.getName() + " ");
            else if (field.getType() == int.class) field.setInt(original, 17);
            else if (field.getType() == long.class) field.setLong(original, 123L);
            else if (field.getType() == boolean.class) field.setBoolean(original, true);
            else throw new AssertionError("Extend copy coverage for " + field.getName());
        }
        Patient copy = PatientRevision.copy(original);
        assertNotSame(original, copy);
        for (Field field : Patient.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                assertEquals(field.getName(), field.get(original), field.get(copy));
            }
        }
    }

    @Test
    public void copiedBaselineIsDetachedFromDesiredMutationAndPreservesNulls() {
        Patient desired = patient();
        Patient expected = PatientRevision.copy(desired);
        desired.doctorName = "Doctor B";
        desired.scheduledDeliveryCalledAt = "2026-10-11T12:00:00";
        desired.finalVisit = "2026-10-11";
        desired.recordLocked = true;
        desired.updatedBy = "admin@example.com";
        assertEquals("Doctor A", expected.doctorName);
        assertEquals(null, expected.scheduledDeliveryCalledAt);
        assertEquals(null, expected.finalVisit);
        assertEquals(false, expected.recordLocked);
        assertEquals("staff@example.com", expected.updatedBy);
        assertTrue(PatientRevision.matches(expected, patient()));
        assertFalse(PatientRevision.matches(expected, desired));
        expected.remarks = "Only the copy changed";
        assertEquals(null, desired.remarks);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotCopyMissingPatient() {
        PatientRevision.copy(null);
    }

    @Test
    public void captureRemainsUnchangedWhenOriginalPatientIsMutated() {
        Patient original = patient();
        Map<String, Object> revision = PatientRevision.capture(original);
        original.doctorName = "Other Doctor";
        original.visit3 = "2026-10-12";
        original.recordLocked = true;
        assertEquals("Doctor A", revision.get("doctorName"));
        assertEquals("", revision.get("visit3"));
        assertEquals(false, revision.get("recordLocked"));
        assertFalse(PatientRevision.matches(revision, original));
        assertTrue(PatientRevision.matches(revision, patient()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void capturedMapCannotBeModified() {
        PatientRevision.capture(patient()).put("doctorName", "Other Doctor");
    }

    @Test
    public void ignoresDeviceIdAndUpdatedByButRetainsSerialNumber() {
        Patient expected = patient();
        Patient current = patient();
        current.id = 987L;
        current.updatedBy = "different@example.com";
        assertTrue(PatientRevision.matches(expected, current));
        assertTrue(PatientRevision.matches(PatientRevision.capture(expected), current));
        current.serialNumber++;
        assertFalse(PatientRevision.matches(expected, current));
    }

    @Test
    public void detectsDoctorDeliveryAndVisitChanges() {
        assertChanged(p -> p.doctorName = "Doctor B");
        assertChanged(p -> p.scheduledDeliveryDate = "2026-10-15");
        assertChanged(p -> p.eddDate = "2026-10-20");
        assertChanged(p -> p.visit1 = "2026-10-02");
        assertChanged(p -> p.visit2 = "2026-10-12");
        assertChanged(p -> p.visit3 = "2026-10-13");
        assertChanged(p -> p.finalVisit = "2026-10-14");
    }

    @Test
    public void detectsBothCallMetadataFields() {
        assertChanged(p -> p.scheduledDeliveryCalledAt = "2026-10-11T12:00:00");
        assertChanged(p -> p.scheduledDeliveryCalledBy = "caller@example.com");
    }

    @Test
    public void detectsOwnershipDeletionRemarksAndLockChanges() {
        assertChanged(p -> p.createdBy = "other@example.com");
        assertChanged(p -> p.deletedAt = "2026-10-11T12:00:00");
        assertChanged(p -> p.deletedBy = "admin@example.com");
        assertChanged(p -> p.remarks = "Preserved clinical history");
        assertChanged(p -> p.recordLocked = true);
    }

    @Test
    public void nullAndEmptyStringsAreEquivalentAcrossAllStringFields() throws Exception {
        Patient nulls = new Patient();
        Patient empty = new Patient();
        for (Field field : Patient.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                field.set(empty, "");
            }
        }
        assertTrue(PatientRevision.matches(nulls, empty));
        assertTrue(PatientRevision.matches(PatientRevision.capture(nulls), empty));
    }

    @Test
    public void preservesStringWhitespaceAndCaseExactly() {
        assertChanged(p -> p.doctorName = " Doctor A ");
        assertChanged(p -> p.doctorName = "doctor a");
        assertChanged(p -> p.remarks = " ");
    }

    @Test
    public void capturesEveryCurrentPatientFieldExceptExplicitExclusions() {
        Set<String> fields = new HashSet<>();
        for (Field field : Patient.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !"id".equals(field.getName())
                    && !"updatedBy".equals(field.getName())) {
                fields.add(field.getName());
            }
        }
        Map<String, Object> captured = PatientRevision.capture(patient());
        assertEquals(fields, captured.keySet());
        assertEquals(Integer.class, captured.get("serialNumber").getClass());
        assertEquals(Boolean.class, captured.get("recordLocked").getClass());
    }

    @Test
    public void missingPatientNeverMatchesAnEditBaseline() {
        assertFalse(PatientRevision.matches(patient(), null));
        assertFalse(PatientRevision.matches((Patient) null, patient()));
        assertFalse(PatientRevision.matches((Patient) null, null));
        assertFalse(PatientRevision.matches(PatientRevision.capture(patient()), null));
        assertFalse(PatientRevision.matches((Map<String, Object>) null, patient()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotCaptureMissingPatient() {
        PatientRevision.capture(null);
    }

    private static void assertChanged(Consumer<Patient> mutation) {
        Patient expected = patient();
        Map<String, Object> captured = PatientRevision.capture(expected);
        Patient current = patient();
        mutation.accept(current);
        assertFalse(PatientRevision.matches(expected, current));
        assertFalse(PatientRevision.matches(captured, current));
    }

    private static Patient patient() {
        Patient p = new Patient();
        p.id = 1L;
        p.serialNumber = 12;
        p.patientId = "PT12-10-2026";
        p.doctorName = "Doctor A";
        p.visit1 = "2026-10-01";
        p.scheduledDeliveryDate = "2026-10-14";
        p.createdBy = "staff@example.com";
        p.updatedBy = "staff@example.com";
        return p;
    }
}
