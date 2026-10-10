package com.maternaltracker.india;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ExportTextTest {
    @Test public void quotesCsvSeparatorsAndQuotes() {
        assertEquals("\"A,\"\"B\"\"\nC\"", ExportText.csv("A,\"B\"\nC"));
        assertEquals("\"\"", ExportText.csv(null));
    }

    @Test public void preventsCsvFormulaEvaluation() {
        for (String value : new String[]{"=1+1", "+SUM(A1)", "-1+2", "@SUM(A1)", "  =1+1"}) {
            assertEquals("\"'" + value + "\"", ExportText.csv(value));
        }
        assertEquals("\"9999999901\"", ExportText.csv("9999999901"));
    }

    @Test public void escapesXmlAndRemovesInvalidCharacters() {
        assertEquals("&lt;&amp;&gt;&quot;&apos;", ExportText.xml("<&>\"'"));
        assertEquals("AB", ExportText.xml("A\u0001\ud800B"));
        assertEquals("", ExportText.xml(null));
    }

    @Test public void preservesValidUnicodeAndWhitespace() {
        assertEquals("\u09ae\ud83d\udc99\n\t", ExportText.xml("\u09ae\ud83d\udc99\n\t"));
    }
}
