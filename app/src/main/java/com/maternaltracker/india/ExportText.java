package com.maternaltracker.india;

final class ExportText {
    private ExportText() {}

    static String csv(String input) {
        String value = input == null ? "" : input;
        String trimmed = value.trim();
        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    static String xml(String input) {
        StringBuilder clean = new StringBuilder();
        String value = input == null ? "" : input;
        for (int offset = 0; offset < value.length();) {
            int c = value.codePointAt(offset);
            offset += Character.charCount(c);
            if (c == 9 || c == 10 || c == 13 || (c >= 0x20 && c <= 0xd7ff)
                    || (c >= 0xe000 && c <= 0xfffd) || (c >= 0x10000 && c <= 0x10ffff)) {
                clean.appendCodePoint(c);
            }
        }
        return clean.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
