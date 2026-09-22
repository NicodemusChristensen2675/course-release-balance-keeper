package education.devtools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JsonDocument {
    private JsonDocument() {}

    static Object parse(String source) {
        Parser parser = new Parser(source);
        Object value = parser.value();
        parser.whitespace();
        if (!parser.finished()) throw new IllegalArgumentException("Trailing JSON content");
        return value;
    }

    static String write(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return '"' + escape(text) + '"';
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            boolean comma = false;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (comma) out.append(',');
                out.append(write(entry.getKey().toString())).append(':').append(write(entry.getValue()));
                comma = true;
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable<?> items) {
            StringBuilder out = new StringBuilder("[");
            boolean comma = false;
            for (Object item : items) {
                if (comma) out.append(',');
                out.append(write(item));
                comma = true;
            }
            return out.append(']').toString();
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static final class Parser {
        private final String source;
        private int cursor;

        Parser(String source) { this.source = source; }
        boolean finished() { return cursor == source.length(); }
        void whitespace() { while (!finished() && Character.isWhitespace(source.charAt(cursor))) cursor++; }

        Object value() {
            whitespace();
            if (finished()) throw new IllegalArgumentException("Empty JSON");
            char token = source.charAt(cursor);
            if (token == '{') return object();
            if (token == '[') return array();
            if (token == '"') return string();
            if (token == 't') return literal("true", true);
            if (token == 'f') return literal("false", false);
            if (token == 'n') return literal("null", null);
            return number();
        }

        private Map<String, Object> object() {
            Map<String, Object> result = new LinkedHashMap<>();
            cursor++;
            whitespace();
            if (take('}')) return result;
            do {
                whitespace();
                String key = string();
                whitespace();
                expect(':');
                result.put(key, value());
                whitespace();
            } while (take(','));
            expect('}');
            return result;
        }

        private List<Object> array() {
            List<Object> result = new ArrayList<>();
            cursor++;
            whitespace();
            if (take(']')) return result;
            do { result.add(value()); whitespace(); } while (take(','));
            expect(']');
            return result;
        }

        private String string() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (!finished()) {
                char ch = source.charAt(cursor++);
                if (ch == '"') return out.toString();
                if (ch != '\\') { out.append(ch); continue; }
                if (finished()) throw new IllegalArgumentException("Incomplete JSON escape");
                char escaped = source.charAt(cursor++);
                switch (escaped) {
                    case '"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (cursor + 4 > source.length()) throw new IllegalArgumentException("Incomplete Unicode escape");
                        out.append((char) Integer.parseInt(source.substring(cursor, cursor + 4), 16));
                        cursor += 4;
                    }
                    default -> throw new IllegalArgumentException("Invalid JSON escape");
                }
            }
            throw new IllegalArgumentException("Unclosed JSON string");
        }

        private Object number() {
            int start = cursor;
            while (!finished() && "-+0123456789.eE".indexOf(source.charAt(cursor)) >= 0) cursor++;
            String token = source.substring(start, cursor);
            try { return Double.valueOf(token); }
            catch (NumberFormatException error) { throw new IllegalArgumentException("Invalid JSON number", error); }
        }

        private Object literal(String expected, Object value) {
            if (!source.startsWith(expected, cursor)) throw new IllegalArgumentException("Invalid JSON literal");
            cursor += expected.length();
            return value;
        }

        private boolean take(char expected) {
            if (!finished() && source.charAt(cursor) == expected) { cursor++; return true; }
            return false;
        }

        private void expect(char expected) {
            if (!take(expected)) throw new IllegalArgumentException("Expected " + expected);
        }
    }
}
