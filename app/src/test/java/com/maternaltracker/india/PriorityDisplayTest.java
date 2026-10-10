package com.maternaltracker.india;

import org.junit.Test;
import java.time.LocalDate;
import static org.junit.Assert.assertEquals;

public class PriorityDisplayTest {
    private final LocalDate today = LocalDate.of(2026, 12, 29);

    @Test public void handlesDateBoundariesAndOverdue() {
        assertEquals("Today", PriorityDisplay.countdown("2026-12-29", today));
        assertEquals("Tomorrow", PriorityDisplay.countdown("2026-12-30", today));
        assertEquals("In 7 days", PriorityDisplay.countdown("2027-01-05", today));
        assertEquals("Overdue 1 day", PriorityDisplay.countdown("2026-12-28", today));
    }

    @Test public void preservesYearAndHandlesMissingDates() {
        assertEquals("05 Jan 2027", PriorityDisplay.date("2027-01-05", today));
        assertEquals("29 Dec", PriorityDisplay.date("2026-12-29", today));
        assertEquals("Not recorded", PriorityDisplay.date("invalid", today));
        assertEquals("", PriorityDisplay.countdown(null, today));
    }
}
