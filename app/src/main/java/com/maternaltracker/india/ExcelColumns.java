package com.maternaltracker.india;

/** Excel column names from zero-based indexes. */
final class ExcelColumns {
    private ExcelColumns() {
    }

    static String name(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Column index must be nonnegative");
        }
        long column = (long) index + 1;
        StringBuilder result = new StringBuilder();
        while (column > 0) {
            column--;
            result.append((char) ('A' + column % 26));
            column /= 26;
        }
        return result.reverse().toString();
    }
}
