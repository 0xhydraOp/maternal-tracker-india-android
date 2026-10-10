package com.maternaltracker.india;

import java.util.Locale;

/** Immutable export intent; Android persistence and role/scope checks remain caller-owned. */
final class ExportRequest {
    static final int EXCEL = 501;
    static final int PDF = 502;
    static final int BACKUP = 503;

    final String owner;
    final int requestCode;
    final String filter;
    final String where;
    final long patientId;
    private final String[] args;

    ExportRequest(String owner, int requestCode, String filter, String where,
                  String[] args, long patientId) {
        String normalizedOwner = normalizeAccount(owner);
        if (normalizedOwner.isEmpty()) {
            throw new IllegalArgumentException("Export owner is required");
        }
        if (requestCode != EXCEL && requestCode != PDF && requestCode != BACKUP) {
            throw new IllegalArgumentException("Unsupported export request code");
        }
        this.owner = normalizedOwner;
        this.requestCode = requestCode;
        this.filter = filter == null ? "" : filter;
        this.where = where;
        this.args = args == null ? null : args.clone();
        this.patientId = patientId;
    }

    String[] getArgs() {
        return args == null ? null : args.clone();
    }

    boolean isValidFor(String account, int resultRequestCode) {
        return requestCode == resultRequestCode && owner.equals(normalizeAccount(account));
    }

    private static String normalizeAccount(String account) {
        return account == null ? "" : account.trim().toLowerCase(Locale.US);
    }
}
