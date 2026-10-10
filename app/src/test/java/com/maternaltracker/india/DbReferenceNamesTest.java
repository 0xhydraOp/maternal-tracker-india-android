package com.maternaltracker.india;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DbReferenceNamesTest {
    @Test
    public void trimsUppercasesAndDeduplicatesInSnapshotOrder() {
        assertEquals(Arrays.asList("DR A", "DR B"), DbReferenceNames.normalize(
                Arrays.asList(" Dr A ", "dr b", "DR A", "DR B ")));
    }

    @Test
    public void ignoresNullAndBlankEntriesAndAllowsEmptyDirectory() {
        assertTrue(DbReferenceNames.normalize(Arrays.asList(null, "", " \t ")).isEmpty());
        assertTrue(DbReferenceNames.normalize(Collections.emptyList()).isEmpty());
    }

    @Test
    public void retainsInternalSpacingAndDoesNotModifySnapshot() {
        List<String> input = Arrays.asList(" dr  istanbul ", "dr  istanbul");
        assertEquals(Collections.singletonList("DR  ISTANBUL"), DbReferenceNames.normalize(input));
        assertEquals(Arrays.asList(" dr  istanbul ", "dr  istanbul"), input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullSnapshotIsRejectedRatherThanClearingDirectory() {
        DbReferenceNames.normalize(null);
    }
}
