package com.shopstack.util;

import com.opencsv.CSVWriter;
import com.shopstack.exception.ReportException;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * Renders report data as CSV. Input is a list of ordered row maps with a header list
 * describing column order and labels.
 */
public final class CsvExporter {

    private CsvExporter() {
    }

    public static String export(List<String> headers, List<Map<String, ?>> rows) {
        try (StringWriter stringWriter = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            csvWriter.writeNext(headers.toArray(new String[0]));
            for (Map<String, ?> row : rows) {
                String[] values = headers.stream()
                        .map(h -> {
                            Object v = row.get(h);
                            return v == null ? "" : v.toString();
                        })
                        .toArray(String[]::new);
                csvWriter.writeNext(values);
            }
            csvWriter.flush();
            return stringWriter.toString();
        } catch (Exception ex) {
            throw new ReportException("Failed to generate CSV export", ex);
        }
    }
}
