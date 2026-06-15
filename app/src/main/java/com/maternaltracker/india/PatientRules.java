package com.maternaltracker.india;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

final class PatientRules {
    private PatientRules() {}

    static String patientId(int sequence, String month, String year) {
        return String.format(Locale.US, "PT%02d-%s-%s", sequence, month, year);
    }

    static String validate(Patient p, LocalDate today) {
        if (empty(p.patientId) || empty(p.patientName) || empty(p.mobileNumber) ||
                empty(p.stateName) || empty(p.districtName) || empty(p.localBodyType) ||
                empty(p.localBodyName) || empty(p.villageName) || empty(p.lmpDate) ||
                empty(p.eddDate) || empty(p.doctorName) || empty(p.visit1)) {
            return "Fill all required fields. Motivator and later visits are optional";
        }
        String digits = p.mobileNumber.replaceAll("\\D", "");
        if (digits.length() < 10 || digits.length() > 15) {
            return "Mobile number must contain 10 to 15 digits";
        }
        if (!validDate(p.lmpDate) || !validDate(p.eddDate) || !validDate(p.visit1) || !validDate(p.visit2) || !validDate(p.visit3) || !validDate(p.finalVisit)) {
            return "Use date format YYYY-MM-DD";
        }
        try {
            if (!empty(p.lmpDate) && LocalDate.parse(p.lmpDate).isAfter(today)) {
                return "LMP date cannot be in the future";
            }
            if (!empty(p.visit2) && empty(p.visit1)) {
                return "1st visit is required before 2nd visit";
            }
            if (!empty(p.visit2) && LocalDate.parse(p.visit2).isBefore(LocalDate.parse(p.visit1))) {
                return "2nd visit cannot be before 1st visit";
            }
            if (!empty(p.visit3) && empty(p.visit1) && empty(p.visit2)) {
                return "Previous visit is required before 3rd visit";
            }
            if (!empty(p.visit3) && LocalDate.parse(p.visit3).isBefore(LocalDate.parse(empty(p.visit2) ? p.visit1 : p.visit2))) {
                return "3rd visit cannot be before previous visit";
            }
            if (!empty(p.finalVisit)) {
                String previous = !empty(p.visit3) ? p.visit3 : (!empty(p.visit2) ? p.visit2 : p.visit1);
                if (empty(previous)) {
                    return "Previous visit is required before final visit";
                }
                if (LocalDate.parse(p.finalVisit).isBefore(LocalDate.parse(previous))) {
                    return "Final visit cannot be before previous visit";
                }
            }
        } catch (DateTimeParseException ex) {
            return "Use date format YYYY-MM-DD";
        }
        return null;
    }

    static boolean validDate(String value) {
        if (empty(value)) {
            return true;
        }
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    static boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
