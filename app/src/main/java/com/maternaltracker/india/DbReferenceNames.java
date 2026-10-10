package com.maternaltracker.india;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class DbReferenceNames {
    private DbReferenceNames() {}

    static List<String> normalize(List<String> names) {
        if (names == null) {
            throw new IllegalArgumentException("Reference name snapshot must not be null");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String name : names) {
            if (name != null && !name.trim().isEmpty()) {
                normalized.add(name.trim().toUpperCase(Locale.US));
            }
        }
        return new ArrayList<>(normalized);
    }
}
