package com.maternaltracker.india;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PatientSequence {
    private static final Pattern PATIENT_ID = Pattern.compile("^PT[0-9]+-([0-9]{2})-([0-9]{4})$");

    private PatientSequence() {}

    static final class Allocation {
        final String patientId;
        final int monthSequence;
        final int serialNumber;

        private Allocation(String monthKey, long previousMonth, long previousGlobal) {
            monthSequence = nextSequence(previousMonth);
            serialNumber = nextSequence(previousGlobal);
            patientId = patientId(monthSequence, monthKey);
        }
    }

    static Allocation allocate(String monthKey, long previousMonth, long previousGlobal) {
        return new Allocation(monthKey, previousMonth, previousGlobal);
    }

    /** Creation never trusts a provisional ID; the registration date chooses its counter. */
    static String creationMonth(Patient patient) {
        if (patient == null || patient.entryDate == null
                || !patient.entryDate.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw new IllegalArgumentException("Registration entry date must use YYYY-MM-DD");
        }
        YearMonth month = YearMonth.from(LocalDate.parse(patient.entryDate));
        if (month.getYear() < 1) throw new IllegalArgumentException("Invalid registration year");
        return month.toString();
    }

    static String monthKey(String provisionalId) {
        Matcher match = PATIENT_ID.matcher(provisionalId == null ? "" : provisionalId);
        if (!match.matches()) throw new IllegalArgumentException("Patient ID must use PT<sequence>-MM-yyyy");
        int year = Integer.parseInt(match.group(2));
        int month = Integer.parseInt(match.group(1));
        if (year < 1 || month < 1 || month > 12) throw new IllegalArgumentException("Invalid patient ID month");
        return YearMonth.of(year, month).toString();
    }

    static int nextSequence(long previous) {
        if (previous < 0 || previous >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid or exhausted patient sequence counter");
        }
        return (int) previous + 1;
    }

    static String patientId(int sequence, String monthKey) {
        YearMonth month = YearMonth.parse(monthKey);
        if (sequence < 1 || month.getYear() < 1 || month.getYear() > 9999) {
            throw new IllegalArgumentException("Invalid patient sequence or month");
        }
        return String.format(Locale.US, "PT%02d-%02d-%04d", sequence, month.getMonthValue(), month.getYear());
    }
}
