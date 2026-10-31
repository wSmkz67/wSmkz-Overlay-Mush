package br.smkzoverlay;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class Config {
    public String apiUrl = "https://mush.com.br/api/player/%s";
    public int requestDelayMs = 350, maxPlayers = 40, maxRows = 12, cacheMinutes = 15, hudX = 4, hudY = 4;
    public boolean hud = true, sound = true;
    public String defaultMode = "bedwars";
    public List<Mode> modes = new ArrayList<>();
    private transient File file;
    public static final Gson G = new GsonBuilder().setPrettyPrinting().create();

    public static class Col {
        public String label, key, per = "";
        public double warn, danger;
        public Col() {}
        public Col(String label, String key, String per, double warn, double danger) {
            this.label = label; this.key = key; this.per = per; this.warn = warn; this.danger = danger;
        }
        public boolean ratio() { return per != null && !per.isEmpty(); }
    }

    public static class Mode {
        public String id, name, apiKey, sub = "", wsKey = "winstreak", winsKey = "wins", lossesKey = "losses";
        public int wsAlert = 15, sortCol = 0;
        public List<String> keywords = new ArrayList<>();
        public List<String> variants = new ArrayList<>(); // se preenchido, precisa aparecer no scoreboard
        public List<Col> cols = new ArrayList<>();
    }

    private static Mode mode(String id, String name, String apiKey, int wsAlert, int sortCol, String[] kw, Col... cols) {
        Mode m = new Mode();
        m.id = id; m.name = name; m.apiKey = apiKey; m.wsAlert = wsAlert; m.sortCol = sortCol;
        m.keywords = new ArrayList<>(Arrays.asList(kw));
        m.cols = new ArrayList<>(Arrays.asList(cols));
        return m;
    }

    private static Mode variant(Mode base, String id, String name, String sub, String... variants) {
        Mode m = new Mode();
        m.id = id; m.name = name; m.apiKey = base.apiKey; m.sub = sub; m.wsAlert = base.wsAlert; m.sortCol = base.sortCol;
        m.keywords = new ArrayList<>(base.keywords);
        m.variants = new ArrayList<>(Arrays.asList(variants));
        m.cols = new ArrayList<>(base.cols);
        return m;
    }

    /** ATEN\u00c7\u00c3O: nomes de chaves (final_kills, winstreak, doubles...) s\u00e3o palpites. Confira com /ov debug <nick>. */
    private static List<Mode> defaults() {
        List<Mode> l = new ArrayList<>();
        Mode bw = mode("bedwars", "Bed Wars", "bedwars", 15, 1, new String[]{"bed wars", "bedwars"},
                new Col("Lvl", "level", "", 0, 0), new Col("FKDR", "final_kills", "final_deaths", 3, 6),
                new Col("WS", "winstreak", "", 10, 25), new Col("Wins", "wins", "", 0, 0), new Col("Camas", "beds_destroyed", "", 0, 0));
        // varia\u00e7\u00f5es primeiro: a detec\u00e7\u00e3o usa o primeiro modo que combinar
        l.add(variant(bw, "bw_solo", "BW Solo", "solo", "solo"));
        l.add(variant(bw, "bw_dupla", "BW Duplas", "doubles", "dupla", "doubles"));
        l.add(variant(bw, "bw_trio", "BW Trios", "trios", "trio"));
        l.add(variant(bw, "bw_quarteto", "BW Quartetos", "quads", "quarteto", "quartetos", "quad", "squad"));
        l.add(bw);
        l.add(mode("bedfight", "Bed Fight", "bedfight", 10, 0, new String[]{"bed fight", "bedfight"},
                new Col("KDR", "kills", "deaths", 2, 4), new Col("Wins", "wins", "", 0, 0),
                new Col("Camas", "beds_destroyed", "", 0, 0), new Col("WS", "winstreak", "", 5, 15)));
        l.add(mode("skywars", "Sky Wars", "skywars", 10, 0, new String[]{"sky wars", "skywars"},
                new Col("KDR", "kills", "deaths", 2, 4), new Col("Wins", "wins", "", 0, 0), new Col("WS", "winstreak", "", 5, 15)));
        l.add(mode("bridge", "The Bridge", "bridge", 10, 0, new String[]{"the bridge", "bridge"},
                new Col("Wins", "wins", "", 0, 0), new Col("Kills", "kills", "", 0, 0), new Col("WS", "winstreak", "", 5, 15)));
        return l;
    }

    public Mode find(String id) {
        for (Mode m : modes) if (m.id.equalsIgnoreCase(id)) return m;
        return null;
    }

    public static Config load(File f) {
        Config c = null;
        try {
            if (f.exists()) {
                Reader r = new InputStreamReader(new FileInputStream(f), "UTF-8");
                c = G.fromJson(r, Config.class);
                r.close();
            }
        } catch (Exception e) { e.printStackTrace(); }
        if (c == null) c = new Config();
        if (c.modes == null) c.modes = new ArrayList<>();
        for (Mode d : defaults()) if (c.find(d.id) == null) c.modes.add(d); // acrescenta modos novos de vers\u00f5es futuras
        for (Mode m : c.modes) { if (m.variants == null) m.variants = new ArrayList<>(); if (m.sub == null) m.sub = ""; }
        c.file = f;
        c.save();
        return c;
    }

    public void save() {
        try {
            Writer w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
            G.toJson(this, w);
            w.close();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
