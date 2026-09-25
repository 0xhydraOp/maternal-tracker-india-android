package com.maternaltracker.india;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class PrintLibraryTest {
    private final PrintLibrary.Document doctor = new PrintLibrary.Document("DR. SUYETA NASRIN", "DGO 79831", new String[]{"doctor.jpg"}, 0, 0, false);
    private final PrintLibrary.Document blood = new PrintLibrary.Document("Blood Requisition", "Blood bank", new String[]{"blood1.jpg", "blood2.jpg"}, 3, 0, false);
    private final List<PrintLibrary.Document> all = Arrays.asList(doctor, blood);

    @Test public void emptyAndroidTextValueShowsAllDocuments() {
        assertEquals(all, PrintLibrary.filter(all, null, 0, Collections.emptySet(), Collections.emptyList()));
    }

    @Test public void searchMatchesNameAndCredentialsIgnoringCaseAndOuterSpaces() {
        assertEquals(Collections.singletonList(doctor), PrintLibrary.filter(all, " suyeta ", 0, Collections.emptySet(), Collections.emptyList()));
        assertEquals(Collections.singletonList(doctor), PrintLibrary.filter(all, "79831", 0, Collections.emptySet(), Collections.emptyList()));
        assertTrue(PrintLibrary.filter(all, "unknown", 0, Collections.emptySet(), Collections.emptyList()).isEmpty());
    }

    @Test public void favouritesCombineWithSearchWithoutIncludingOtherDocuments() {
        assertEquals(Collections.singletonList(blood), PrintLibrary.filter(all, "", 1, Collections.singleton("blood1.jpg"), Collections.emptyList()));
        assertTrue(PrintLibrary.filter(all, "suyeta", 1, Collections.singleton("blood1.jpg"), Collections.emptyList()).isEmpty());
    }

    @Test public void recentDocumentsAreOrderedAndIgnoreRemovedAssets() {
        assertEquals(Arrays.asList(blood, doctor), PrintLibrary.filter(all, "", 2, Collections.emptySet(), Arrays.asList("deleted.jpg", "blood1.jpg", "doctor.jpg")));
        assertEquals(2, blood.assets.length);
    }

    @Test public void recordingRecentPromotesDeduplicatesAndCapsWithoutMutatingInput() {
        List<String> previous = Arrays.asList("a", "b", "", "a", "c", "d", "e", "f", "g");
        assertEquals(Arrays.asList("c", "a", "b", "d", "e", "f"), PrintLibrary.recordRecent(previous, "c"));
        assertEquals(9, previous.size());
    }
}
