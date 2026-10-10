package com.maternaltracker.india;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PatientSequenceTest {
    @Test public void creationAcceptsAutoOnSaveWithoutTrustingDisplayId() {
        Patient patient = new Patient();
        patient.patientId = "AUTO ON SAVE";
        patient.entryDate = "2026-10-11";
        PatientSequence.Allocation allocation = PatientSequence.allocate(PatientSequence.creationMonth(patient), 17, 193);
        assertEquals("PT18-10-2026", allocation.patientId);
        assertEquals(194, allocation.serialNumber);
        assertEquals("AUTO ON SAVE", patient.patientId);
    }

    @Test public void creationAcceptsUnassignedIdAndIgnoresClientChosenId() {
        Patient patient = new Patient();
        patient.entryDate = "2027-01-01";
        assertEquals("2027-01", PatientSequence.creationMonth(patient));
        patient.patientId = "PT99-12-2026";
        PatientSequence.Allocation allocation = PatientSequence.allocate(PatientSequence.creationMonth(patient), 0, 194);
        assertEquals("PT01-01-2027", allocation.patientId);
        assertEquals(195, allocation.serialNumber);
    }

    @Test(expected = IllegalArgumentException.class)
    public void creationRejectsMissingEntryDate() {
        PatientSequence.creationMonth(new Patient());
    }

    @Test public void derivesCounterMonthFromProvisionalAndHistoricalIds() {
        assertEquals("2026-06", PatientSequence.monthKey("PT03-06-2026"));
        assertEquals("2025-12", PatientSequence.monthKey("PT123-12-2025"));
    }

    @Test public void allocatesFirstAndFollowingIdsWithExistingPadding() {
        assertEquals(1, PatientSequence.nextSequence(0));
        assertEquals("PT01-06-2026", PatientSequence.patientId(1, "2026-06"));
        assertEquals("PT100-12-2026", PatientSequence.patientId(PatientSequence.nextSequence(99), "2026-12"));
    }

    @Test public void decemberAndJanuaryUseSeparateCounters() {
        assertEquals("2026-12", PatientSequence.monthKey("PT10-12-2026"));
        assertEquals("2027-01", PatientSequence.monthKey("PT01-01-2027"));
        assertEquals("PT01-01-2027", PatientSequence.patientId(1, "2027-01"));
    }

    @Test public void acceptsLastSupportedSequenceWithoutOverflow() {
        assertEquals(Integer.MAX_VALUE, PatientSequence.nextSequence(Integer.MAX_VALUE - 1L));
    }

    @Test public void allocatesMonthlyIdentityAndIndependentHospitalSerial() {
        PatientSequence.Allocation allocation = PatientSequence.allocate("2026-10", 12, 345);
        assertEquals("PT13-10-2026", allocation.patientId);
        assertEquals(13, allocation.monthSequence);
        assertEquals(346, allocation.serialNumber);
    }

    @Test public void newMonthRestartsIdentityButNotHospitalSerial() {
        PatientSequence.Allocation allocation = PatientSequence.allocate("2027-01", 0, 345);
        assertEquals("PT01-01-2027", allocation.patientId);
        assertEquals(346, allocation.serialNumber);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exhaustedGlobalCounterRejectsWholeAllocation() {
        PatientSequence.allocate("2026-10", 1, Integer.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullPreview() {
        PatientSequence.monthKey(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPaddedPreview() {
        PatientSequence.monthKey(" PT01-06-2026 ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidMonth() {
        PatientSequence.monthKey("PT01-13-2026");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroYear() {
        PatientSequence.monthKey("PT01-06-0000");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeCounter() {
        PatientSequence.nextSequence(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsCounterOverflow() {
        PatientSequence.nextSequence(Integer.MAX_VALUE);
    }
}
