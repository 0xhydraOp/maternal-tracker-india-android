package com.maternaltracker.india;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Value-level diff for the columns owned by an authoritative snapshot. */
final class DbReconciliation {
    private DbReconciliation() {}

    static List<String> changedColumns(Map<String, ?> current, Map<String, ?> incoming) {
        List<String> changed = new ArrayList<>();
        for (Map.Entry<String, ?> entry : incoming.entrySet()) {
            String column = entry.getKey();
            if (!current.containsKey(column) || !Objects.equals(current.get(column), entry.getValue())) {
                changed.add(column);
            }
        }
        return changed;
    }
}
