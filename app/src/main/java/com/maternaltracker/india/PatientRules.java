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
        if (empty(p.patientId) || empty(p.patientName) || empty(p.age) || empty(p.bloodGroup) || empty(p.mobileNumber) ||
                empty(p.stateName) || empty(p.districtName) || empty(p.localBodyType) ||
                empty(p.localBodyName) || empty(p.villageName) || empty(p.gravida) || empty(p.lmpDate) ||
                empty(p.eddDate) || empty(p.doctorName) || empty(p.visit1)) {
            return "Fill all required fields. Motivator, last delivery method, scheduled delivery, and later visits are optional";
        }
        String digits = p.mobileNumber.replaceAll("\\D", "");
        if (digits.length() != 10) {
            return "Mobile number must contain exactly 10 digits";
        }
        if (!validDate(p.lmpDate) || !validDate(p.eddDate) || !validDate(p.visit1) || !validDate(p.visit2) || !validDate(p.visit3) || !validDate(p.finalVisit) || !validDate(p.scheduledDeliveryDate)) {
            return "Use date format YYYY-MM-DD";
        }
        try {
            if (!empty(p.lmpDate) && LocalDate.parse(p.lmpDate).isAfter(today)) {
                return "LMP date cannot be in the future";
            }
            if (!empty(p.lmpDate) && !empty(p.eddDate) && LocalDate.parse(p.eddDate).isBefore(LocalDate.parse(p.lmpDate))) {
                return "EDD date cannot be before LMP";
            }
            if (!empty(p.scheduledDeliveryDate)) {
                LocalDate scheduled = LocalDate.parse(p.scheduledDeliveryDate);
                if (!empty(p.lmpDate) && scheduled.isBefore(LocalDate.parse(p.lmpDate))) {
                    return "Scheduled delivery date cannot be before LMP";
                }
            }
            if (LocalDate.parse(p.visit1).isAfter(today)) {
                return "1st visit cannot be in the future";
            }
            if (!empty(p.visit2) && empty(p.visit1)) {
                return "1st visit is required before 2nd visit";
            }
            if (!empty(p.visit2) && LocalDate.parse(p.visit2).isBefore(LocalDate.parse(p.visit1))) {
                return "2nd visit cannot be before 1st visit";
            }
            if (!empty(p.visit3) && empty(p.visit2)) {
                return "2nd visit is required before 3rd visit";
            }
            if (!empty(p.visit3) && LocalDate.parse(p.visit3).isBefore(LocalDate.parse(empty(p.visit2) ? p.visit1 : p.visit2))) {
                return "3rd visit cannot be before previous visit";
            }
            if (!empty(p.finalVisit)) {
                if (empty(p.visit2)) {
                    return "2nd visit is required before final visit";
                }
                String previous = !empty(p.visit3) ? p.visit3 : p.visit2;
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

    static String normalizedMobile(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }

    static boolean sameMobile(String left, String right) {
        String normalizedLeft = normalizedMobile(left);
        return normalizedLeft.length() == 10 && normalizedLeft.equals(normalizedMobile(right));
    }

    /** Upcoming dates take precedence; otherwise retain the latest populated overdue visit. */
    static String[] nextFollowupVisit(Patient p, LocalDate today) {
        if (!activeRecord(p)) {
            return new String[]{"Follow-up", ""};
        }
        LocalDate checkedToday = safeToday(today);
        LocalDate week = checkedToday.plusDays(7);
        String[] labels = {"2nd Visit", "3rd Visit", "Final Visit"};
        String[] dates = {p.visit2, p.visit3, p.finalVisit};
        int next = -1;
        LocalDate earliest = null;
        for (int i = 0; i < dates.length; i++) {
            if (empty(dates[i]) || !validDate(dates[i])) {
                continue;
            }
            LocalDate date = LocalDate.parse(dates[i]);
            if (!date.isBefore(checkedToday) && !date.isAfter(week)
                    && (earliest == null || date.isBefore(earliest))) {
                next = i;
                earliest = date;
            }
        }
        if (next >= 0) {
            return new String[]{labels[next], dates[next]};
        }
        for (int i = dates.length - 1; i >= 0; i--) {
            if (!empty(dates[i])) {
                if (validDate(dates[i]) && LocalDate.parse(dates[i]).isBefore(checkedToday)) {
                    return new String[]{labels[i], dates[i]};
                }
                break;
            }
        }
        return new String[]{"Follow-up", ""};
    }

    static boolean scheduledDeliveryNeedsCompletion(Patient p, LocalDate today) {
        return activeRecord(p) && dateBefore(p.scheduledDeliveryDate, safeToday(today));
    }

    static boolean eddNeedsCompletion(Patient p, LocalDate today) {
        return activeRecord(p) && dateBefore(p.eddDate, safeToday(today));
    }

    static boolean deliveryCompletionDue(Patient p, LocalDate today) {
        LocalDate checkedToday = safeToday(today);
        return scheduledDeliveryNeedsCompletion(p, checkedToday) || eddNeedsCompletion(p, checkedToday);
    }

    static boolean deliveryCompletionEligible(Patient p, LocalDate today) {
        if (!activeRecord(p)) {
            return false;
        }
        LocalDate checkedToday = safeToday(today);
        return completionWindowActive(p.scheduledDeliveryDate, checkedToday) || completionWindowActive(p.eddDate, checkedToday);
    }

    static String deliveryCompletionSource(Patient p, LocalDate today) {
        if (!activeRecord(p)) {
            return "";
        }
        LocalDate checkedToday = safeToday(today);
        if (dateBefore(p.scheduledDeliveryDate, checkedToday)) {
            return "scheduled";
        }
        if (dateBefore(p.eddDate, checkedToday)) {
            return "edd";
        }
        if (completionWindowActive(p.scheduledDeliveryDate, checkedToday)) {
            return "scheduled";
        }
        if (completionWindowActive(p.eddDate, checkedToday)) {
            return "edd";
        }
        return "";
    }

    static String deliveryCompletionReferenceDate(Patient p, LocalDate today) {
        String source = deliveryCompletionSource(p, today);
        if ("scheduled".equals(source)) {
            return value(p.scheduledDeliveryDate);
        }
        if ("edd".equals(source)) {
            return value(p.eddDate);
        }
        return "";
    }

    static LocalDate deliveryCompletionVisitDate(Patient p, LocalDate today) {
        LocalDate checkedToday = safeToday(today);
        String reference = deliveryCompletionReferenceDate(p, checkedToday);
        if (empty(reference) || !validDate(reference)) {
            return checkedToday;
        }
        LocalDate target = LocalDate.parse(reference);
        return target.isAfter(checkedToday) ? checkedToday : target;
    }

    /** Preserve an actual final visit; otherwise record the operator's completion day. */
    static LocalDate deliveryCompletionFinalVisitDate(Patient p, LocalDate today) {
        LocalDate checkedToday = safeToday(today);
        if (p != null && !empty(p.finalVisit) && validDate(p.finalVisit)) {
            LocalDate existing = LocalDate.parse(p.finalVisit);
            if (!existing.isAfter(checkedToday)) {
                return existing;
            }
        }
        return checkedToday;
    }

    /** Null means safe to complete; never changes patient data or discards planned visits. */
    static String validateDeliveryCompletion(Patient p, LocalDate today) {
        LocalDate checkedToday = safeToday(today);
        if (!deliveryCompletionEligible(p, checkedToday)) {
            return "Delivery completion is available only within the 7 day delivery window";
        }
        LocalDate finalDate = deliveryCompletionFinalVisitDate(p, checkedToday);
        String[] dates = {p.visit1, p.visit2, p.visit3};
        String[] labels = {"1st visit", "2nd visit", "3rd visit"};
        for (int i = 0; i < dates.length; i++) {
            if (empty(dates[i])) {
                continue;
            }
            if (!validDate(dates[i])) {
                return "Review " + labels[i] + " date before completing. Use date format YYYY-MM-DD";
            }
            if (LocalDate.parse(dates[i]).isAfter(finalDate)) {
                return "Completion final visit would be before " + labels[i]
                        + ". Review visit dates before completing";
            }
        }
        return null;
    }

    static String deliveryCompletionReason(Patient p, LocalDate today) {
        LocalDate checkedToday = safeToday(today);
        String source = deliveryCompletionSource(p, checkedToday);
        String reference = deliveryCompletionReferenceDate(p, checkedToday);
        boolean passed = dateBefore(reference, checkedToday);
        if ("scheduled".equals(source)) {
            return passed ? "Scheduled delivery date passed" : "Scheduled delivery within 7 days";
        }
        if ("edd".equals(source)) {
            return passed ? "EDD date passed" : "EDD within 7 days";
        }
        return "Delivery date eligible";
    }

    static boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean activeRecord(Patient p) {
        return p != null && !p.recordLocked;
    }

    private static boolean completionWindowActive(String value, LocalDate today) {
        if (empty(value) || !validDate(value)) {
            return false;
        }
        return !LocalDate.parse(value).isAfter(today.plusDays(7));
    }

    private static boolean dateBefore(String value, LocalDate today) {
        return !empty(value) && validDate(value) && LocalDate.parse(value).isBefore(today);
    }

    private static LocalDate safeToday(LocalDate today) {
        return today == null ? LocalDate.now() : today;
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
