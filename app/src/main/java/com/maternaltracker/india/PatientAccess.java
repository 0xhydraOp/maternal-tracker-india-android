package com.maternaltracker.india;

final class PatientAccess {
    private PatientAccess() {}

    static boolean canManage(String role, String account, Patient patient) {
        if (patient == null || account == null || account.trim().isEmpty()
                || (patient.deletedAt != null && !patient.deletedAt.isEmpty())) return false;
        if ("ADMIN".equalsIgnoreCase(role)) return true;
        return "STAFF".equalsIgnoreCase(role) && patient.createdBy != null
                && account.trim().equalsIgnoreCase(patient.createdBy);
    }
}
