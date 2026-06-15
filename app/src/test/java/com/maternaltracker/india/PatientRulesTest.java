package com.maternaltracker.india;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.LocalDate;

public class PatientRulesTest {
    @Test
    public void patientIdPadsSequence() {
        assertEquals("PT03-06-2026", PatientRules.patientId(3, "06", "2026"));
    }

    @Test
    public void validationAcceptsCompleteRecordWithoutMotivator() {
        Patient p = validPatient();
        p.motivatorName = null;
        assertNull(PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationRejectsBadMobile() {
        Patient p = validPatient();
        p.mobileNumber = "12345";
        assertEquals("Mobile number must contain 10 to 15 digits", PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationRejectsVisitOrderRegression() {
        Patient p = validPatient();
        p.visit2 = "2026-05-31";
        assertEquals("2nd visit cannot be before 1st visit", PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationRequiresPreviousVisitBeforeSecondVisit() {
        Patient p = validPatient();
        p.visit1 = null;
        p.visit2 = "2026-06-20";
        assertEquals("1st visit is required before 2nd visit", PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationRequiresPreviousVisitBeforeFinalVisit() {
        Patient p = validPatient();
        p.visit1 = null;
        p.finalVisit = "2026-07-01";
        assertEquals("Previous visit is required before final visit", PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    private Patient validPatient() {
        Patient p = new Patient();
        p.patientName = "Test Patient";
        p.mobileNumber = "9876543210";
        p.stateName = "West Bengal";
        p.districtName = "MURSHIDABAD";
        p.localBodyName = "KANDI";
        p.villageName = "Test Village";
        p.lmpDate = "2026-06-01";
        p.eddDate = "2027-03-08";
        p.visit1 = "2026-06-15";
        return p;
    }
}
