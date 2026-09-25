package com.maternaltracker.india;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Stable asset paths identify documents even when a supplied pad is replaced. */
final class PrintLibrary {
    static final class Document {
        final String title, details;
        final String[] assets;
        final int category, index;
        final boolean landscape;

        Document(String title, String details, String[] assets, int category, int index, boolean landscape) {
            this.title = title;
            this.details = details;
            this.assets = assets;
            this.category = category;
            this.index = index;
            this.landscape = landscape;
        }

        String id() { return assets[0]; }
    }

    static List<String> recordRecent(List<String> previous, String id) {
        List<String> result = new ArrayList<>();
        result.add(id);
        for (String item : previous) {
            if (!item.isEmpty() && !result.contains(item) && result.size() < 6) result.add(item);
        }
        return result;
    }

    static List<Document> filter(List<Document> all, String query, int mode, Set<String> favourites, List<String> recent) {
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Document> result = new ArrayList<>();
        for (Document document : all) {
            if (mode == 1 && !favourites.contains(document.id())) continue;
            if (mode == 2 && !recent.contains(document.id())) continue;
            if ((document.title + " " + document.details).toLowerCase(Locale.ROOT).contains(term)) result.add(document);
        }
        if (mode == 2) result.sort((a, b) -> Integer.compare(recent.indexOf(a.id()), recent.indexOf(b.id())));
        return result;
    }
}
