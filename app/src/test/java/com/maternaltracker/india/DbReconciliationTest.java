package com.maternaltracker.india;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DbReconciliationTest {
    @Test
    public void unchangedSnapshotHasNoChangedColumns() {
        Map<String, Object> row = row("patient_name", "A", "record_locked", 0, "deleted_at", null);
        assertTrue(DbReconciliation.changedColumns(row, new LinkedHashMap<>(row)).isEmpty());
    }

    @Test
    public void comparesValuesRatherThanHashes() {
        assertEquals("Aa".hashCode(), "BB".hashCode());
        assertEquals(Arrays.asList("patient_name"), DbReconciliation.changedColumns(
                row("patient_name", "Aa"), row("patient_name", "BB")));
    }

    @Test
    public void distinguishesNullEmptyAndSoftDeletionChanges() {
        assertEquals(Arrays.asList("deleted_at", "remarks"), DbReconciliation.changedColumns(
                row("deleted_at", null, "remarks", ""),
                row("deleted_at", "2026-10-11", "remarks", null)));
    }

    @Test
    public void nullToEmptyChangesOnceThenRepeatedSnapshotIsStable() {
        Map<String, Object> current = row("deleted_at", null, "remarks", null);
        Map<String, Object> incoming = row("deleted_at", "", "remarks", "");
        assertEquals(Arrays.asList("deleted_at", "remarks"),
                DbReconciliation.changedColumns(current, incoming));
        current.putAll(incoming);
        assertTrue(DbReconciliation.changedColumns(current, incoming).isEmpty());
    }

    @Test
    public void emptyToNullChangesOnceThenRepeatedSnapshotIsStable() {
        Map<String, Object> current = row("deleted_at", "", "remarks", "");
        Map<String, Object> incoming = row("deleted_at", null, "remarks", null);
        assertEquals(Arrays.asList("deleted_at", "remarks"),
                DbReconciliation.changedColumns(current, incoming));
        current.putAll(incoming);
        assertTrue(DbReconciliation.changedColumns(current, incoming).isEmpty());
    }

    @Test
    public void returnsOnlyChangedIncomingColumnsWithoutMutatingInputs() {
        Map<String, Object> current = row("id", 12L, "created_at", "original",
                "serial_number", 1, "record_locked", 0, "patient_name", "A");
        Map<String, Object> incoming = row("serial_number", 2, "record_locked", 1, "patient_name", "A");
        assertEquals(Arrays.asList("serial_number", "record_locked"),
                DbReconciliation.changedColumns(current, incoming));
        assertEquals(5, current.size());
        assertEquals(3, incoming.size());
        assertEquals("original", current.get("created_at"));
    }

    @Test
    public void detectsMissingColumnEvenWhenIncomingValueIsNull() {
        assertEquals(Arrays.asList("remarks"), DbReconciliation.changedColumns(
                row("patient_name", "A"), row("remarks", null)));
    }

    private static Map<String, Object> row(Object... pairs) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            row.put((String) pairs[i], pairs[i + 1]);
        }
        return row;
    }
}
