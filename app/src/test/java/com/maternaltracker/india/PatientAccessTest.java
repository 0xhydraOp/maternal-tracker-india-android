package com.maternaltracker.india;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PatientAccessTest {
    private Patient patient(String owner, boolean locked, String deletedAt) {
        Patient p = new Patient();
        p.createdBy = owner;
        p.recordLocked = locked;
        p.deletedAt = deletedAt;
        return p;
    }

    @Test public void staffCanManageOwnOpenAndCompletedRecords() {
        assertTrue(PatientAccess.canManage("STAFF", "staff@example.com", patient("staff@example.com", false, "")));
        assertTrue(PatientAccess.canManage("staff", "STAFF@example.com", patient("staff@example.com", true, null)));
    }

    @Test public void staffCannotManageAnotherOwnerOrUnknownOwner() {
        assertFalse(PatientAccess.canManage("STAFF", "staff@example.com", patient("other@example.com", false, "")));
        assertFalse(PatientAccess.canManage("STAFF", "staff@example.com", patient(null, false, "")));
    }

    @Test public void adminCanManageAllOwnersButNotDeletedRecords() {
        assertTrue(PatientAccess.canManage("ADMIN", "admin@example.com", patient("staff@example.com", true, "")));
        assertFalse(PatientAccess.canManage("ADMIN", "admin@example.com", patient("staff@example.com", true, "2026-10-11")));
        assertFalse(PatientAccess.canManage("STAFF", "staff@example.com", patient("staff@example.com", false, "2026-10-11")));
    }

    @Test public void invalidSessionAndMissingRecordAreRejected() {
        Patient p = patient("staff@example.com", false, "");
        assertFalse(PatientAccess.canManage("STAFF", "", p));
        assertFalse(PatientAccess.canManage("STAFF", null, p));
        assertFalse(PatientAccess.canManage("STAFF", "  ", p));
        assertFalse(PatientAccess.canManage("STAFF", "staff@example.com", patient(" staff@example.com ", false, "")));
        assertFalse(PatientAccess.canManage("STAFF", "staff@example.com", patient("staff@example.com", false, " ")));
        assertFalse(PatientAccess.canManage(null, "staff@example.com", p));
        assertFalse(PatientAccess.canManage("OTHER", "staff@example.com", p));
        assertFalse(PatientAccess.canManage("ADMIN", "admin@example.com", null));
    }
}
