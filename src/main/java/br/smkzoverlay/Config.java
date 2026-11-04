package br.smkzoverlay;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class Config {
    public String apiUrl = "https://mush.com.br/api/player/%s";
    public int requestDelayMs = 350, maxPlayers = 40, maxRows = 12, cacheMinutes = 15, hudX = 4, hudY = 4;
    public boolean hud = true, sound = true;
    public float hudScale = 0.85f;
    public boolean teams = true, npcPing = true;
    public boolean qb = true;                 // alerta de quickbuy parecida com a de sniper
    public int qbMin = 90, qbGeneric = 3, qbDelayMs = 700, qbLearnDelayMs = 1500;
    public String qbUrl = "https://mush.com.br/api/player/name/%s/bedwars"; // separar times; esconder NPCs sem ping
    public int accent = 0xFF8B5CF6, accent2 = 0xFF22D3EE; // cores do tema (ARGB)
    public String defaultMode = "bedwars";
    public int version = 0;
    public List<Mode> modes = new ArrayList<>();
    private transient File file;
    public static final Gson G = new GsonBuilder().setPrettyPrinting().create();

    public static class Col {
        public String label, key, per = "";
        public double warn, danger;
        public boolean global; // true = usa a chave exatamente como esta (sem prefixo/sufixo do modo)
        public Col() {}
        public Col(String label, String key, String per, double warn, double danger) {
            this.label = label; this.key = key; this.per = per; this.warn = warn; this.danger = danger;
        }
        public Col global() { this.global = true; return this; }
        public boolean ratio() { return per != null && !per.isEmpty(); }
    }

    /**
     * apiKey = objeto dentro de "stats" (bedwars, duels, skywars_r1).
     * prefix: campo = prefix_chave (ex: solo_wins). suffix: campo = chave_suffix (ex: wins_solo).
     */
    public static class Mode {
        public String id, name, apiKey, sub = "", prefix = "", suffix = "", basePrefix = "", baseSuffix = "", wsKey = "winstreak", winsKey = "wins", lossesKey = "losses";
        public int wsAlert = 15, sortCol = 1;
        public List<String> keywords = new ArrayList<>();
        public List<String> variants = new ArrayList<>(); // se preenchido, precisa aparecer no scoreboard
        public List<Col> cols = new ArrayList<>();
    }

    private static Mode mode(String id, String name, String apiKey, String prefix, String suffix, int wsAlert, String[] kw, Col... cols) {
        Mode m = new Mode();
        m.id = id; m.name = name; m.apiKey = apiKey; m.prefix = prefix; m.suffix = suffix; m.basePrefix = prefix; m.baseSuffix = suffix; m.wsAlert = wsAlert;
        m.keywords = new ArrayList<>(Arrays.asList(kw));
        m.cols = new ArrayList<>(Arrays.asList(cols));
        return m;
    }

    private static Mode variant(Mode base, String id, String name, String prefix, String suffix, String... variants) {
        Mode m = new Mode();
        m.id = id; m.name = name; m.apiKey = base.apiKey; m.prefix = prefix; m.suffix = suffix; m.basePrefix = base.prefix; m.baseSuffix = base.suffix;
        m.wsAlert = base.wsAlert; m.sortCol = base.sortCol;
        m.keywords = new ArrayList<>(base.keywords);
        m.variants = new ArrayList<>(Arrays.asList(variants));
        m.cols = new ArrayList<>(base.cols);
        return m;
    }

    /** Chaves conferidas no JSON real da API (stats.bedwars, stats.duels, stats.skywars_r1). */
    private static List<Mode> defaults() {
        List<Mode> l = new ArrayList<>();

        // ---- Bed Wars: stats.bedwars (campos prefixados: solo_, doubles_, 3v3v3v3_, 4v4v4v4_, 1v1_...) ----
        Col[] bwc = {
            new Col("Nivel", "level", "", 0, 0).global(),
            new Col("FKDR", "final_kills", "final_deaths", 3, 6),
            new Col("WS", "winstreak", "", 10, 25),
            new Col("Wins", "wins", "", 0, 0),
            new Col("Derrotas", "losses", "", 0, 0),
            new Col("WLR", "wins", "losses", 1.5, 3),
            new Col("Camas", "beds_broken", "", 0, 0)};
        Mode bw = mode("bedwars", "Bed Wars", "bedwars", "", "", 15, new String[]{"bed wars", "bedwars"}, bwc);
        // variacoes primeiro: a deteccao usa o primeiro modo que combinar
        l.add(variant(bw, "bw_solo", "BW Solo", "solo", "", "solo"));
        l.add(variant(bw, "bw_dupla", "BW Duplas", "doubles", "", "dupla", "doubles"));
        l.add(variant(bw, "bw_trio", "BW Trios", "3v3v3v3", "", "trio", "3v3v3v3"));
        l.add(variant(bw, "bw_quarteto", "BW Quartetos", "4v4v4v4", "", "quarteto", "quartetos", "4v4v4v4"));
        l.add(variant(bw, "bw_1v1", "BW 1v1", "1v1", "", "1v1"));
        l.add(variant(bw, "bw_2v2", "BW 2v2", "2v2", "", "2v2"));
        l.add(variant(bw, "bw_4v4", "BW 4v4", "4v4", "", "4v4"));
        l.add(variant(bw, "bw_mega", "BW Mega", "mega", "", "mega"));
        l.add(variant(bw, "bw_champions", "BW Champions", "champions", "", "champions"));
        l.add(bw);

        // ---- Bed Fight: stats.duels, campos bed_fight_*, bed_fight_solo_*, bed_fight_doubles_* ----
        Col[] bfc = {
            new Col("Nivel", "bed_fight_level", "", 0, 0).global(),
            new Col("KDR", "kills", "deaths", 1.5, 3),
            new Col("WS", "winstreak", "", 5, 15),
            new Col("Wins", "wins", "", 0, 0),
            new Col("Derrotas", "losses", "", 0, 0),
            new Col("WLR", "wins", "losses", 1.5, 3),
            new Col("Camas", "beds_broken", "", 0, 0)};
        Mode bf = mode("bedfight", "Bed Fight", "duels", "bed_fight", "", 10, new String[]{"bed fight", "bedfight"}, bfc);
        l.add(variant(bf, "bedfight_solo", "Bed Fight Solo", "bed_fight_solo", "", "solo"));
        l.add(variant(bf, "bedfight_dupla", "Bed Fight Duplas", "bed_fight_doubles", "", "dupla", "doubles"));
        l.add(bf);

        // ---- The Bridge: stats.duels, campos bridge_*, bridge_solo_*, bridge_doubles_* ----
        Col[] brc = {
            new Col("Nivel", "bridge_level", "", 0, 0).global(),
            new Col("KDR", "kills", "deaths", 1.5, 3),
            new Col("WS", "winstreak", "", 4, 10),
            new Col("Wins", "wins", "", 0, 0),
            new Col("Derrotas", "losses", "", 0, 0),
            new Col("WLR", "wins", "losses", 1.5, 3),
            new Col("Pontos", "points", "", 0, 0)};
        Mode br = mode("bridge", "The Bridge", "duels", "bridge", "", 8, new String[]{"the bridge", "bridge"}, brc);
        l.add(variant(br, "bridge_solo", "Bridge Solo", "bridge_solo", "", "solo"));
        l.add(variant(br, "bridge_dupla", "Bridge Duplas", "bridge_doubles", "", "dupla", "doubles"));
        l.add(br);

        // ---- Sky Wars: stats.skywars_r1, campos com sufixo (wins_solo, kills_1v1...) ----
        Col[] swc = {
            new Col("Nivel", "level", "", 0, 0).global(),
            new Col("KDR", "kills", "deaths", 1.5, 3),
            new Col("WS", "winstreak", "", 4, 10),
            new Col("Wins", "wins", "", 0, 0),
            new Col("Derrotas", "losses", "", 0, 0),
            new Col("WLR", "wins", "losses", 1.5, 3),
            new Col("Jogos", "games_played", "", 0, 0)};
        Mode sw = mode("skywars", "Sky Wars", "skywars_r1", "", "", 6, new String[]{"sky wars", "skywars"}, swc);
        l.add(variant(sw, "sw_solo", "SW Solo", "", "solo", "solo"));
        l.add(variant(sw, "sw_1v1", "SW 1v1", "", "1v1", "1v1"));
        l.add(variant(sw, "sw_champions", "SW Champions", "", "champions", "champions"));
        l.add(sw);
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
        if (c.version < 5) { c.modes = new ArrayList<>(); c.version = 5; } // formato novo: redefine os modos
        if (c.modes == null) c.modes = new ArrayList<>();
        for (Mode d : defaults()) if (c.find(d.id) == null) c.modes.add(d);
        for (Mode m : c.modes) {
            if (m.variants == null) m.variants = new ArrayList<>();
            if (m.sub == null) m.sub = "";
            if (m.prefix == null) m.prefix = "";
            if (m.suffix == null) m.suffix = "";
            if (m.basePrefix == null) m.basePrefix = "";
            if (m.baseSuffix == null) m.baseSuffix = "";
        }
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
