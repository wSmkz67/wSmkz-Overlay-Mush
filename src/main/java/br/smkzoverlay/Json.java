package br.smkzoverlay;

import com.google.gson.*;
import java.util.*;

/** Utilit\u00e1rios de JSON tolerantes: procuram uma chave em qualquer n\u00edvel. */
public class Json {
    public static JsonElement find(JsonElement e, String key, boolean objOnly) {
        if (e == null || key == null) return null;
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            for (Map.Entry<String, JsonElement> en : o.entrySet())
                if (en.getKey().equalsIgnoreCase(key) && (!objOnly || en.getValue().isJsonObject())) return en.getValue();
            for (Map.Entry<String, JsonElement> en : o.entrySet()) {
                JsonElement r = find(en.getValue(), key, objOnly);
                if (r != null) return r;
            }
        } else if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) {
                JsonElement r = find(x, key, objOnly);
                if (r != null) return r;
            }
        }
        return null;
    }

    public static double num(JsonElement e) {
        try { if (e != null && e.isJsonPrimitive()) return e.getAsDouble(); } catch (Exception ignored) {}
        return 0;
    }

    /** Aceita segundos ou milissegundos; devolve ms. */
    public static long ts(JsonElement e) {
        double v = num(e);
        if (v <= 0) return 0;
        return v < 1e11 ? (long) (v * 1000) : (long) v;
    }

    public static String groups(JsonElement e) {
        if (e == null || !e.isJsonObject()) return "-";
        Map<String, Integer> g = new LinkedHashMap<>();
        int total = 0;
        for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
            String k = en.getKey();
            int i = k.indexOf('_');
            String p = i > 0 ? k.substring(0, i) : k;
            Integer c = g.get(p);
            g.put(p, c == null ? 1 : c + 1);
            total++;
        }
        List<Map.Entry<String, Integer>> l = new ArrayList<>(g.entrySet());
        Collections.sort(l, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) { return b.getValue() - a.getValue(); }
        });
        StringBuilder sb = new StringBuilder(total + " chaves; grupos: ");
        int n = 0;
        for (Map.Entry<String, Integer> en : l) {
            if (n++ >= 24) break;
            sb.append(en.getKey()).append('(').append(en.getValue()).append(") ");
        }
        return sb.toString();
    }

    public static String keys(JsonElement e, int limit) {
        if (e == null || !e.isJsonObject()) return "-";
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
            if (n++ > 0) sb.append(", ");
            sb.append(en.getKey());
            if (n >= limit) { sb.append("..."); break; }
        }
        return sb.toString();
    }

    public static String keys(JsonElement e) {
        if (e == null || !e.isJsonObject()) return "-";
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) { if (n++ > 0) sb.append(", "); sb.append(en.getKey()); if (n >= 18) { sb.append("..."); break; } }
        return sb.toString();
    }
}
