package com.aitu.sdp.assignment3.bridge;

import com.aitu.sdp.assignment2.Computer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JsonReportExporter implements ReportExporter {
    @Override
    public String exportHeader(String title) {
        return "{\"title\":\"" + escapeJson(title) + "\",\"specifications\":{";
    }

    @Override
    public String exportBody(Computer computer) {
        return exportBody(computer, ReportDetail.FULL_DIAGNOSTIC);
    }

    @Override
    public String exportBody(Computer computer, ReportDetail detail) {
        Map<String, String> specifications = ReportSpecifications.forComputer(computer, detail);
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> specification : specifications.entrySet()) {
            if (body.length() > 0) {
                body.append(',');
            }
            body.append('"').append(escapeJson(specification.getKey())).append("\":\"")
                    .append(escapeJson(specification.getValue())).append('"');
        }
        return body.toString();
    }

    @Override
    public String exportFooter() {
        return "}}";
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
