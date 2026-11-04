package br.smkzoverlay;

import com.google.gson.*;
import java.util.*;
import java.util.regex.Pattern;

public class PlayerData {
    public final String name;
    public boolean loaded, failed, notFound;
    public String error;
    public JsonObject root;
    public long firstLogin, lastLogin, fetched;
    private final Map<String, Double> memo = new HashMap<>();
    private final Map<String, JsonObject> modeObjs = new HashMap<>();
    private final Map<String, String> badges = new HashMap<>();

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

    /** Texto do badge de nivel como o MushMC mostra (ex: "&4[117&l\u2764&4]"), vindo do campo <nivel>_badge.format da API. */
    public String badgeFormat(Config.Mode m, String levelKey) {
        if (root == null || levelKey == null) return null;
        String k = m.id + "|" + levelKey;
        if (badges.containsKey(k)) return badges.get(k);
        String out = null;
        JsonObject mo = modeObj(m);
        if (mo != null) {
            JsonElement b = Json.find(mo, levelKey + "_badge", true);
            if (b != null) {
                JsonElement f = b.getAsJsonObject().get("format");
                if (f != null && f.isJsonPrimitive()) out = f.getAsString();
            }
        }
        badges.put(k, out);
        return out;
    }

    public double stat(Config.Mode m, String key) { return stat(m, key, false); }

    /** verbatim = usa a chave exatamente como esta, sem prefixo/sufixo do modo. */
    public double stat(Config.Mode m, String key, boolean verbatim) { return statOf(m, key, verbatim, m.prefix, m.suffix, ""); }

    /** Mesma stat, mas do modo geral (ex: winstreak geral em vez do de trios). */
    public double statGeneral(Config.Mode m, String key) { return statOf(m, key, false, m.basePrefix, m.baseSuffix, "#G"); }

    private double statOf(Config.Mode m, String key, boolean verbatim, String prefix, String suffix, String tag) {
        if (root == null || key == null || key.isEmpty()) return 0;
        String k = m.id + "." + key + (verbatim ? "!" : "") + tag;
        Double c = memo.get(k);
        if (c != null) return c;
        JsonObject mo = modeObj(m);
        double v = 0;
        if (mo != null) {
            String name = key;
            if (!verbatim) {
                if (prefix != null && !prefix.isEmpty()) name = prefix + "_" + name;
                if (suffix != null && !suffix.isEmpty()) name = name + "_" + suffix;
            }
            v = Json.num(Json.find(mo, name, false));
        }
        memo.put(k, v);
        return v;
    }
}
