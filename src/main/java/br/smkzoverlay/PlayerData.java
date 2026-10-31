package br.smkzoverlay;

import com.google.gson.*;
import java.util.*;

public class PlayerData {
    public final String name;
    public boolean loaded, failed, notFound;
    public String error;
    public JsonObject root;
    public long firstLogin, lastLogin, fetched;
    private final Map<String, Double> memo = new HashMap<>();
    private final Map<String, JsonObject> modeObjs = new HashMap<>();

    public PlayerData(String name) { this.name = name; }

    public void parse(JsonElement e) {
        if (e == null || !e.isJsonObject()) { notFound = true; return; }
        root = e.getAsJsonObject();
        JsonElement s = root.get("success");
        try {
            if (s != null && s.isJsonPrimitive() && s.getAsJsonPrimitive().isBoolean() && !s.getAsBoolean()) notFound = true;
        } catch (Exception ignored) {}
        firstLogin = Json.ts(Json.find(root, "first_login", false));
        lastLogin = Json.ts(Json.find(root, "last_login", false));
    }

    private JsonObject modeObj(Config.Mode m) {
        if (!modeObjs.containsKey(m.id)) {
            JsonElement e = Json.find(root, m.apiKey, true);
            JsonObject o = e == null ? null : e.getAsJsonObject();
            if (o != null && m.sub != null && !m.sub.isEmpty()) {
                JsonElement s = Json.find(o, m.sub, true);
                o = s == null ? null : s.getAsJsonObject();
            }
            modeObjs.put(m.id, o);
        }
        return modeObjs.get(m.id);
    }

    public boolean hasMode(Config.Mode m) { return root != null && modeObj(m) != null; }

    public double stat(Config.Mode m, String key) {
        if (root == null || key == null || key.isEmpty()) return 0;
        String k = m.id + "." + key;
        Double c = memo.get(k);
        if (c != null) return c;
        JsonObject mo = modeObj(m);
        double v = mo == null ? 0 : Json.num(Json.find(mo, key, false));
        memo.put(k, v);
        return v;
    }
}
