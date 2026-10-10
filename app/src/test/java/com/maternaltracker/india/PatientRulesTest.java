package com.maternaltracker.india;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
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
        assertEquals("Mobile number must contain exactly 10 digits", PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void mobileComparisonAcceptsIndianPrefixAndFormatting() {
        assertEquals(true, PatientRules.sameMobile("+91 98765-43210", "9876543210"));
    }

    @Test
    public void mobileComparisonRejectsShortOrDifferentNumbers() {
        assertEquals(false, PatientRules.sameMobile("12345", "12345"));
        assertEquals(false, PatientRules.sameMobile("9876543210", "9876543211"));
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
        assertEquals("Fill all required fields. Motivator, last delivery method, scheduled delivery, and later visits are optional", PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationRequiresAge() {
        Patient p = validPatient();
        p.age = null;
        assertEquals("Fill all required fields. Motivator, last delivery method, scheduled delivery, and later visits are optional", PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationRequiresBloodGroup() {
        Patient p = validPatient();
        p.bloodGroup = null;
        assertEquals("Fill all required fields. Motivator, last delivery method, scheduled delivery, and later visits are optional", PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationRequiresGravida() {
        Patient p = validPatient();
        p.gravida = null;
        assertEquals("Fill all required fields. Motivator, last delivery method, scheduled delivery, and later visits are optional", PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationAcceptsMissingLastDeliveryMethod() {
        Patient p = validPatient();
        p.lastDeliveryMethod = null;
        assertNull(PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationRequiresPreviousVisitBeforeFinalVisit() {
        Patient p = validPatient();
        p.visit2 = null;
        p.visit3 = null;
        p.finalVisit = "2026-07-01";
        assertEquals("2nd visit is required before final visit", PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationRequiresSecondVisitBeforeThirdVisit() {
        Patient p = validPatient();
        p.visit2 = null;
        p.visit3 = "2026-06-20";
        p.finalVisit = null;
        assertEquals("2nd visit is required before 3rd visit", PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationAcceptsMissingLaterVisits() {
        Patient p = validPatient();
        p.visit2 = null;
        p.visit3 = null;
        p.finalVisit = null;
        assertNull(PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationAcceptsPastScheduledDeliveryForCompletionWorkflow() {
        Patient p = validPatient();
        p.scheduledDeliveryDate = "2026-06-14";
        p.scheduledDeliveryCalledAt = null;
        assertNull(PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationAcceptsPastScheduledDeliveryWhenNotified() {
        Patient p = validPatient();
        p.scheduledDeliveryDate = "2026-06-14";
        p.scheduledDeliveryCalledAt = "2026-06-14T10:00:00";
        assertNull(PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationAcceptsPastScheduledDeliveryWhenCompleted() {
        Patient p = validPatient();
        p.scheduledDeliveryDate = "2026-06-14";
        p.scheduledDeliveryCalledAt = null;
        p.recordLocked = true;
        assertNull(PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationRejectsScheduledDeliveryBeforeLmp() {
        Patient p = validPatient();
        p.scheduledDeliveryDate = "2026-05-31";
        p.scheduledDeliveryCalledAt = "2026-06-01T10:00:00";
        assertEquals("Scheduled delivery date cannot be before LMP", PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationRejectsEddBeforeLmp() {
        Patient p = validPatient();
        p.eddDate = "2026-05-31";
        assertEquals("EDD date cannot be before LMP", PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationRejectsInvalidFirstVisitDate() {
        Patient p = validPatient();
        p.visit1 = "2026/06/01";
        assertEquals("Use date format YYYY-MM-DD", PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationRejectsFutureFirstVisitDate() {
        Patient p = validPatient();
        p.visit1 = "2026-06-16";
        assertEquals("1st visit cannot be in the future", PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationRejectsFinalVisitBeforePreviousVisit() {
        Patient p = validPatient();
        p.finalVisit = "2026-06-09";
        assertEquals("Final visit cannot be before previous visit", PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void validationAcceptsFutureTentativeVisitDate() {
        Patient p = validPatient();
        p.visit2 = "2026-06-16";
        p.visit3 = "2026-06-20";
        p.finalVisit = "2026-06-25";
        assertNull(PatientRules.validate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void deliveryCompletionDueAcceptsOverdueEddWithoutScheduledDelivery() {
        Patient p = validPatient();
        p.eddDate = "2026-06-14";
        p.scheduledDeliveryDate = "";
        p.finalVisit = "";

        assertEquals(true, PatientRules.deliveryCompletionDue(p, LocalDate.parse("2026-06-15")));
        assertEquals(true, PatientRules.deliveryCompletionEligible(p, LocalDate.parse("2026-06-15")));
        assertEquals("EDD date passed", PatientRules.deliveryCompletionReason(p, LocalDate.parse("2026-06-15")));
        assertEquals(LocalDate.parse("2026-06-14"), PatientRules.deliveryCompletionVisitDate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void deliveryCompletionEligibleAcceptsEddWithinSevenDays() {
        Patient p = validPatient();
        p.eddDate = "2026-06-20";
        p.scheduledDeliveryDate = "";
        p.finalVisit = "";

        assertEquals(false, PatientRules.deliveryCompletionDue(p, LocalDate.parse("2026-06-15")));
        assertEquals(true, PatientRules.deliveryCompletionEligible(p, LocalDate.parse("2026-06-15")));
        assertEquals("EDD within 7 days", PatientRules.deliveryCompletionReason(p, LocalDate.parse("2026-06-15")));
        assertEquals(LocalDate.parse("2026-06-15"), PatientRules.deliveryCompletionVisitDate(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void deliveryCompletionPrefersScheduledDateInsideWindow() {
        Patient p = validPatient();
        p.eddDate = "2026-06-20";
        p.scheduledDeliveryDate = "2026-06-18";
        p.finalVisit = "";

        assertEquals("scheduled", PatientRules.deliveryCompletionSource(p, LocalDate.parse("2026-06-15")));
        assertEquals("Scheduled delivery within 7 days", PatientRules.deliveryCompletionReason(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void deliveryCompletionIgnoresLockedRecords() {
        Patient p = validPatient();
        p.eddDate = "2026-06-14";
        p.scheduledDeliveryDate = "";
        p.recordLocked = true;

        assertEquals(false, PatientRules.deliveryCompletionDue(p, LocalDate.parse("2026-06-15")));
        assertEquals(false, PatientRules.deliveryCompletionEligible(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void followupSelectsEarliestUpcomingAcrossMultipleTentativeDates() {
        Patient p = validPatient();
        p.visit2 = "2026-06-16";
        p.visit3 = "2026-07-01";
        p.finalVisit = "2026-07-10";
        assertArrayEquals(new String[]{"2nd Visit", "2026-06-16"},
                PatientRules.nextFollowupVisit(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void followupDoesNotUsePastRecordedVisitInsteadOfUpcomingVisit() {
        Patient p = validPatient();
        p.visit2 = "2026-06-10";
        p.visit3 = "2026-06-18";
        p.finalVisit = "2026-07-10";
        assertArrayEquals(new String[]{"3rd Visit", "2026-06-18"},
                PatientRules.nextFollowupVisit(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void followupWindowIncludesTodayAndSeventhDayButNotEighthDay() {
        Patient p = validPatient();
        p.visit2 = "2026-06-15";
        p.visit3 = "2026-06-22";
        p.finalVisit = "2026-06-23";
        LocalDate today = LocalDate.parse("2026-06-15");
        assertArrayEquals(new String[]{"2nd Visit", "2026-06-15"}, PatientRules.nextFollowupVisit(p, today));
        p.visit2 = "2026-06-14";
        assertArrayEquals(new String[]{"3rd Visit", "2026-06-22"}, PatientRules.nextFollowupVisit(p, today));
        p.visit3 = "2026-06-14";
        assertArrayEquals(new String[]{"Follow-up", ""}, PatientRules.nextFollowupVisit(p, today));
    }

    @Test
    public void followupSameDayTieUsesEarlierVisitNumber() {
        Patient p = validPatient();
        p.visit2 = p.visit3 = p.finalVisit = "2026-06-18";
        assertArrayEquals(new String[]{"2nd Visit", "2026-06-18"},
                PatientRules.nextFollowupVisit(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void followupRetainsLatestPopulatedOverdueFallback() {
        Patient p = validPatient();
        p.finalVisit = "2026-06-14";
        LocalDate today = LocalDate.parse("2026-06-15");
        assertArrayEquals(new String[]{"Final Visit", "2026-06-14"}, PatientRules.nextFollowupVisit(p, today));
        p.finalVisit = "";
        assertArrayEquals(new String[]{"3rd Visit", "2026-06-10"}, PatientRules.nextFollowupVisit(p, today));
        p.visit3 = null;
        assertArrayEquals(new String[]{"2nd Visit", "2026-06-05"}, PatientRules.nextFollowupVisit(p, today));
    }

    @Test
    public void followupIgnoresMissingInvalidAndLockedRecords() {
        LocalDate today = LocalDate.parse("2026-06-15");
        assertArrayEquals(new String[]{"Follow-up", ""}, PatientRules.nextFollowupVisit(null, today));
        Patient p = validPatient();
        p.visit2 = "not-a-date";
        p.visit3 = "";
        p.finalVisit = null;
        assertArrayEquals(new String[]{"Follow-up", ""}, PatientRules.nextFollowupVisit(p, today));
        p.visit2 = "2026-06-16";
        p.recordLocked = true;
        assertArrayEquals(new String[]{"Follow-up", ""}, PatientRules.nextFollowupVisit(p, today));
    }

    @Test
    public void overdueEddWinsOverUpcomingScheduledDelivery() {
        Patient p = validPatient();
        p.eddDate = "2026-06-14";
        p.scheduledDeliveryDate = "2026-06-18";
        LocalDate today = LocalDate.parse("2026-06-15");
        assertEquals("edd", PatientRules.deliveryCompletionSource(p, today));
        assertEquals("2026-06-14", PatientRules.deliveryCompletionReferenceDate(p, today));
        assertEquals("EDD date passed", PatientRules.deliveryCompletionReason(p, today));
        assertEquals(LocalDate.parse("2026-06-14"), PatientRules.deliveryCompletionVisitDate(p, today));
    }

    @Test
    public void overdueScheduledDeliveryRetainsPrecedenceWhenBothAreOverdue() {
        Patient p = validPatient();
        p.eddDate = "2026-06-13";
        p.scheduledDeliveryDate = "2026-06-14";
        assertEquals("scheduled", PatientRules.deliveryCompletionSource(p, LocalDate.parse("2026-06-15")));
        assertEquals("Scheduled delivery date passed",
                PatientRules.deliveryCompletionReason(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void completionWindowIncludesSeventhDayNotEighthDay() {
        Patient p = validPatient();
        p.scheduledDeliveryDate = "";
        p.eddDate = "2026-06-22";
        LocalDate today = LocalDate.parse("2026-06-15");
        assertEquals(true, PatientRules.deliveryCompletionEligible(p, today));
        assertEquals("edd", PatientRules.deliveryCompletionSource(p, today));
        p.eddDate = "2026-06-23";
        assertEquals(false, PatientRules.deliveryCompletionEligible(p, today));
        assertEquals("", PatientRules.deliveryCompletionSource(p, today));
    }

    @Test
    public void completionOnTargetDayIsEligibleButNotOverdue() {
        Patient p = validPatient();
        p.scheduledDeliveryDate = "2026-06-15";
        p.eddDate = "2026-06-22";
        LocalDate today = LocalDate.parse("2026-06-15");
        assertEquals(false, PatientRules.deliveryCompletionDue(p, today));
        assertEquals("scheduled", PatientRules.deliveryCompletionSource(p, today));
    }

    @Test
    public void completionRejectsFuturePreviousVisitWithoutChangingTentativeDates() {
        Patient p = validPatient();
        p.scheduledDeliveryDate = "2026-06-18";
        p.visit3 = "2026-06-17";
        p.finalVisit = "2026-06-20";
        LocalDate today = LocalDate.parse("2026-06-15");
        assertNull(PatientRules.validate(p, today));
        assertEquals("Completion final visit would be before 3rd visit. Review visit dates before completing",
                PatientRules.validateDeliveryCompletion(p, today));
        assertEquals("2026-06-17", p.visit3);
        assertEquals("2026-06-20", p.finalVisit);
        assertEquals(false, p.recordLocked);
    }

    @Test
    public void completionChecksSecondVisitEvenWhenThirdIsMissing() {
        Patient p = validPatient();
        p.scheduledDeliveryDate = "2026-06-18";
        p.visit2 = "2026-06-16";
        p.visit3 = "";
        p.finalVisit = "";
        assertEquals("Completion final visit would be before 2nd visit. Review visit dates before completing",
                PatientRules.validateDeliveryCompletion(p, LocalDate.parse("2026-06-15")));
    }

    @Test
    public void overdueScheduledReferenceDoesNotBackdateFinalBeforeActualPreviousVisit() {
        Patient p = validPatient();
        p.scheduledDeliveryDate = "2026-06-12";
        p.visit3 = "2026-06-14";
        p.finalVisit = "";
        LocalDate today = LocalDate.parse("2026-06-15");
        assertNull(PatientRules.validateDeliveryCompletion(p, today));
        assertEquals(today, PatientRules.deliveryCompletionFinalVisitDate(p, today));
        assertEquals(LocalDate.parse("2026-06-12"), PatientRules.deliveryCompletionVisitDate(p, today));
    }

    @Test
    public void overdueEddWithFirstVisitTodayCanCompleteToday() {
        Patient p = validPatient();
        p.eddDate = "2026-06-14";
        p.scheduledDeliveryDate = "";
        p.visit1 = "2026-06-15";
        p.visit2 = p.visit3 = p.finalVisit = "";
        LocalDate today = LocalDate.parse("2026-06-15");
        assertEquals(true, PatientRules.deliveryCompletionEligible(p, today));
        assertNull(PatientRules.validateDeliveryCompletion(p, today));
        assertEquals(today, PatientRules.deliveryCompletionFinalVisitDate(p, today));
        assertEquals("2026-06-15", p.visit1);
        assertEquals("", p.finalVisit);
        assertEquals(false, p.recordLocked);
    }

    @Test
    public void overdueEddWithActualThirdVisitTodayCanCompleteToday() {
        Patient p = validPatient();
        p.eddDate = "2026-06-14";
        p.scheduledDeliveryDate = "";
        p.visit3 = "2026-06-15";
        p.finalVisit = "2026-06-20";
        LocalDate today = LocalDate.parse("2026-06-15");
        assertEquals(true, PatientRules.deliveryCompletionEligible(p, today));
        assertNull(PatientRules.validateDeliveryCompletion(p, today));
        assertEquals(today, PatientRules.deliveryCompletionFinalVisitDate(p, today));
        assertEquals("2026-06-15", p.visit3);
        assertEquals("2026-06-20", p.finalVisit);
        assertEquals(false, p.recordLocked);
    }

    @Test
    public void completionPreservesExistingPastActualFinalVisit() {
        Patient p = validPatient();
        p.eddDate = "2026-06-14";
        p.scheduledDeliveryDate = "";
        p.finalVisit = "2026-06-13";
        LocalDate today = LocalDate.parse("2026-06-15");
        assertNull(PatientRules.validateDeliveryCompletion(p, today));
        assertEquals(LocalDate.parse("2026-06-13"), PatientRules.deliveryCompletionFinalVisitDate(p, today));
        assertEquals("2026-06-13", p.finalVisit);
    }

    @Test
    public void completionPreservesActualFinalVisitRatherThanBackdatingToTarget() {
        Patient p = validPatient();
        p.scheduledDeliveryDate = "2026-06-09";
        LocalDate today = LocalDate.parse("2026-06-15");
        assertEquals(today, PatientRules.deliveryCompletionFinalVisitDate(p, today));
        assertNull(PatientRules.validateDeliveryCompletion(p, today));
        assertEquals("2026-06-15", p.finalVisit);
        assertEquals(false, p.recordLocked);
    }

    @Test
    public void completionRejectsRetainedFinalVisitBeforeAnotherRecordedVisit() {
        Patient p = validPatient();
        p.scheduledDeliveryDate = "2026-06-18";
        p.visit3 = "2026-06-14";
        p.finalVisit = "2026-06-13";
        assertEquals("Completion final visit would be before 3rd visit. Review visit dates before completing",
                PatientRules.validateDeliveryCompletion(p, LocalDate.parse("2026-06-15")));
        assertEquals("2026-06-13", p.finalVisit);
    }

    @Test
    public void completionSourceIgnoresInvalidScheduledDateAndUsesEligibleEdd() {
        Patient p = validPatient();
        p.scheduledDeliveryDate = "bad-date";
        p.eddDate = "2026-06-20";
        LocalDate today = LocalDate.parse("2026-06-15");
        assertEquals("edd", PatientRules.deliveryCompletionSource(p, today));
        assertEquals(true, PatientRules.deliveryCompletionEligible(p, today));
    }

    @Test
    public void completionAllowsSameDayPreviousVisitAndMissingOptionalVisits() {
        Patient p = validPatient();
        p.scheduledDeliveryDate = "2026-06-18";
        p.visit1 = "2026-06-15";
        p.visit2 = p.visit3 = p.finalVisit = "";
        LocalDate today = LocalDate.parse("2026-06-15");
        assertNull(PatientRules.validateDeliveryCompletion(p, today));
        assertEquals(today, PatientRules.deliveryCompletionFinalVisitDate(p, today));
        p.visit2 = "2026-06-15";
        assertNull(PatientRules.validateDeliveryCompletion(p, today));
    }

    @Test
    public void completionRejectsInvalidPreviousDateAndIneligibleRecords() {
        Patient p = validPatient();
        p.scheduledDeliveryDate = "2026-06-18";
        p.visit2 = "bad-date";
        LocalDate today = LocalDate.parse("2026-06-15");
        assertEquals("Review 2nd visit date before completing. Use date format YYYY-MM-DD",
                PatientRules.validateDeliveryCompletion(p, today));
        p.recordLocked = true;
        assertEquals("Delivery completion is available only within the 7 day delivery window",
                PatientRules.validateDeliveryCompletion(p, today));
        assertEquals("Delivery completion is available only within the 7 day delivery window",
                PatientRules.validateDeliveryCompletion(null, today));
    }

    private Patient validPatient() {
        Patient p = new Patient();
        p.patientId = "PT01-06-2026";
        p.patientName = "Test Patient";
        p.age = "24";
        p.bloodGroup = "O+";
        p.mobileNumber = "9876543210";
        p.stateName = "West Bengal";
        p.districtName = "MURSHIDABAD";
        p.localBodyType = "Block";
        p.localBodyName = "KANDI";
        p.villageName = "Test Village";
        p.gravida = "G2P1";
        p.lastDeliveryMethod = "NORMAL";
        p.lmpDate = "2026-06-01";
        p.eddDate = "2027-03-08";
        p.scheduledDeliveryDate = "2027-02-28";
        p.doctorName = "Test Doctor";
        p.visit1 = "2026-06-01";
        p.visit2 = "2026-06-05";
        p.visit3 = "2026-06-10";
        p.finalVisit = "2026-06-15";
        return p;
    }
}
