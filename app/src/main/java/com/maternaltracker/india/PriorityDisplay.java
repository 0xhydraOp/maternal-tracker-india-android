package com.maternaltracker.india;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

final class PriorityDisplay {
    private PriorityDisplay() {}

    static String date(String value, LocalDate today) {
        if (PatientRules.empty(value) || !PatientRules.validDate(value)) return "Not recorded";
        LocalDate date = LocalDate.parse(value);
        return date.format(DateTimeFormatter.ofPattern(date.getYear() == today.getYear()
                ? "dd MMM" : "dd MMM yyyy", Locale.US));
    }

    static String countdown(String value, LocalDate today) {
        if (PatientRules.empty(value) || !PatientRules.validDate(value)) return "";
        long days = ChronoUnit.DAYS.between(today, LocalDate.parse(value));
        if (days == 0) return "Today";
        if (days == 1) return "Tomorrow";
        if (days < 0) return "Overdue " + (-days) + ((days == -1) ? " day" : " days");
        return "In " + days + " days";
    }
}
