package com.maternaltracker.india;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExportRequestTest {
    @Test
    public void snapshotsScalarFieldsWithoutChangingQueryText() {
        ExportRequest request = new ExportRequest(" Staff@Example.com ", 501,
                " patient ", "created_by = ?", new String[]{"staff@example.com"}, -1);

        assertEquals("staff@example.com", request.owner);
        assertEquals(501, request.requestCode);
        assertEquals(" patient ", request.filter);
        assertEquals("created_by = ?", request.where);
        assertEquals(-1L, request.patientId);
    }

    @Test
    public void copiesArgumentsOnConstruction() {
        String[] args = {"staff@example.com", "2026-10-11"};
        ExportRequest request = new ExportRequest("staff@example.com", 502, "",
                "created_by = ? AND entry_date = ?", args, -1);
        args[0] = "other@example.com";

        assertArrayEquals(new String[]{"staff@example.com", "2026-10-11"}, request.getArgs());
    }

    @Test
    public void copiesArgumentsOnEveryAccess() {
        ExportRequest request = new ExportRequest("staff@example.com", 501, "",
                "created_by = ?", new String[]{"staff@example.com"}, -1);
        String[] first = request.getArgs();
        String[] second = request.getArgs();
        assertNotSame(first, second);
        first[0] = "other@example.com";

        assertArrayEquals(new String[]{"staff@example.com"}, request.getArgs());
        assertArrayEquals(new String[]{"staff@example.com"}, second);
    }

    @Test
    public void preservesNullAndEmptyArgumentsDistinctly() {
        ExportRequest nullArgs = new ExportRequest("admin@example.com", 503, null, null, null, -1);
        ExportRequest emptyArgs = new ExportRequest("admin@example.com", 501, "", "", new String[0], -1);

        assertNull(nullArgs.getArgs());
        assertNull(nullArgs.where);
        assertEquals("", nullArgs.filter);
        assertArrayEquals(new String[0], emptyArgs.getArgs());
        assertEquals("", emptyArgs.where);
    }

    @Test
    public void validatesSameAccountAndExactResultCodeForEveryFormat() {
        for (int code : new int[]{501, 502, 503}) {
            ExportRequest request = new ExportRequest("staff@example.com", code, "", null, null, -1);
            assertTrue(request.isValidFor(" STAFF@EXAMPLE.COM ", code));
            assertFalse(request.isValidFor("other@example.com", code));
            assertFalse(request.isValidFor("staff@example.com", 500));
        }
    }

    @Test
    public void rejectsDifferentSupportedResultCode() {
        ExportRequest request = new ExportRequest("staff@example.com", 501, "", null, null, -1);

        assertFalse(request.isValidFor("staff@example.com", 502));
        assertFalse(request.isValidFor("staff@example.com", 503));
    }

    @Test
    public void rejectsSignedOutAccount() {
        ExportRequest request = new ExportRequest("staff@example.com", 502, "", null, null, 42);

        assertFalse(request.isValidFor(null, 502));
        assertFalse(request.isValidFor("", 502));
        assertFalse(request.isValidFor("   ", 502));
        assertEquals(42L, request.patientId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullOwner() {
        new ExportRequest(null, 501, "", null, null, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankOwner() {
        new ExportRequest("   ", 501, "", null, null, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsupportedRequestCode() {
        new ExportRequest("staff@example.com", 504, "", null, null, -1);
    }
}
