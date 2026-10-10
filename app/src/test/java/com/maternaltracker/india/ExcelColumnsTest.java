package com.maternaltracker.india;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExcelColumnsTest {
    @Test public void allPatientExportColumnsHaveValidNames() {
        for (int i = 0; i < 26; i++) {
            assertEquals(String.valueOf((char) ('A' + i)), ExcelColumns.name(i));
        }
        assertEquals("AA", ExcelColumns.name(26));
        assertEquals("AB", ExcelColumns.name(27));
    }

    @Test public void handlesMultiLetterBoundaries() {
        assertEquals("AZ", ExcelColumns.name(51));
        assertEquals("BA", ExcelColumns.name(52));
        assertEquals("ZZ", ExcelColumns.name(701));
        assertEquals("AAA", ExcelColumns.name(702));
        assertEquals("XFD", ExcelColumns.name(16383));
    }

    @Test public void maximumIntegerIndexDoesNotOverflow() {
        assertEquals("FXSHRXX", ExcelColumns.name(Integer.MAX_VALUE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeIndex() {
        ExcelColumns.name(-1);
    }
}
