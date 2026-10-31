package br.smkzoverlay;

import java.util.*;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;

/** Monta as linhas de jogadores (compartilhado entre HUD e menu). */
public class Rows {
    // limites de destaque (ajuste aqui)
    public static final double FKDR_HIGH = 7, FKDR_VERY = 10, WS_HIGH = 10, WS_VERY = 20;
    public static final int NICK_COLOR = 0xFF3B82F6;
    private static final String HEX = "0123456789abcdef";

    public static class Row {
        public String lname = "", name = "", color = "\u00a7f", team = "", headerText = "";
        public char teamColor = 'f';
        public boolean header, nicked;
        public PlayerData d;
        public Snipe.Res res;
        public NetworkPlayerInfo info;
        public double sort;

        /** Texto do selo: BL > SNIPE? > NICK > ALERTA. */
        public String pill() {
            if (res.black) return "BL";
            if (res.score >= 60) return "SNIPE?";
            if (nicked) return "NICK";
            return res.pill();
        }

        public int pillColor() { return (res.black || res.score >= 60 || !nicked) ? res.color() : NICK_COLOR; }

        /** Nick colorido pelo time; nickeds em italico. */
        public String nameStr() { return color + (nicked ? "\u00a7o" : "") + name; }
    }

    // ---------------- valores e cores ----------------
    public static double value(PlayerData d, Config.Mode m, Config.Col c) {
        double a = d.stat(m, c.key, c.global);
        return c.ratio() ? a / Math.max(1, d.stat(m, c.per, c.global)) : a;
    }

    public static String color(double v, Config.Col c) {
        if (c.danger > 0 && v >= c.danger) return "\u00a7c";
        if (c.warn > 0 && v >= c.warn) return "\u00a7e";
        return "\u00a7a";
    }

    /** Tipo da coluna, inferido pela chave: level, fkdr, ws ou vazio. */
    public static String kind(Config.Col c) {
        if (c.global && c.key != null && c.key.endsWith("level")) return "level";
        if (c.ratio() && "final_kills".equals(c.key)) return "fkdr";
        if ("winstreak".equals(c.key)) return "ws";
        return "";
    }

    /** Primeira cor do badge de nivel do MushMC (campo level_badge.format da API). */
    private static char levelChar(PlayerData d, Config.Mode m) {
        for (Config.Col c : m.cols) {
            if (!"level".equals(kind(c))) continue;
            String f = d.badgeFormat(m, c.key);
            if (f == null) return 'f';
            for (int i = 0; i < f.length() - 1; i++)
                if (f.charAt(i) == '&' && HEX.indexOf(Character.toLowerCase(f.charAt(i + 1))) >= 0)
                    return Character.toLowerCase(f.charAt(i + 1));
            return 'f';
        }
        return 'f';
    }

    /** Escala propria do FKDR (branco > amarelo > laranja > vermelho > rosa negrito); evita a cor do nivel. */
    private static String fkdrPrefix(double v, char avoid) {
        char c = 'f';
        String extra = "";
        if (v >= FKDR_HIGH) { c = 'd'; extra = "\u00a7l"; }
        else if (v >= 4) c = 'c';
        else if (v >= 2) c = '6';
        else if (v >= 1) c = 'e';
        if (c == avoid) c = avoid == 'b' ? '3' : 'b';
        return "\u00a7" + c + extra;
    }

    private static String wsPrefix(double v) {
        if (v >= 40) return "\u00a74\u00a7l";
        if (v >= 20) return "\u00a7c\u00a7l";
        if (v >= WS_HIGH) return "\u00a76\u00a7l";
        if (v >= 5) return "\u00a7e";
        if (v >= 1) return "\u00a7a";
        return "\u00a78";
    }

    public static String colText(PlayerData d, Config.Mode m, Config.Col c) {
        if (d == null) return "\u00a77...";
        if (d.failed) return "\u00a7cerro";
        if (d.notFound) return "\u00a78--";
        String k = kind(c);
        double v = value(d, m, c);
        if (k.equals("level")) {
            String f = d.badgeFormat(m, c.key);
            return f != null ? f.replace('&', '\u00a7') : "\u00a7f" + Utils.fmt(v, false);
        }
        if (k.equals("fkdr")) return fkdrPrefix(v, levelChar(d, m)) + Utils.fmt(v, true);
        if (k.equals("ws")) return wsPrefix(v) + Utils.fmt(v, false);
        return color(v, c) + Utils.fmt(v, c.ratio());
    }

    /** Fundo translucido atras de valores muito altos (FKDR e WS); 0 = sem fundo. */
    public static int cellBg(PlayerData d, Config.Mode m, Config.Col c) {
        if (d == null || !d.loaded || d.failed || d.notFound) return 0;
        String k = kind(c);
        if (k.isEmpty() || k.equals("level")) return 0;
        double v = value(d, m, c);
        if (k.equals("fkdr")) return v >= FKDR_VERY ? 0x77D946EF : v >= FKDR_HIGH ? 0x44D946EF : 0;
        return v >= WS_VERY ? 0x66EF4444 : v >= WS_HIGH ? 0x33F59E0B : 0;
    }

    // ---------------- linhas ----------------
    public static List<Row> build(final Config.Mode m) {
        SmkzOverlay o = SmkzOverlay.I;
        List<Row> out = new ArrayList<>();
        for (Map.Entry<String, String> en : o.present.entrySet()) {
            Row r = new Row();
            r.lname = en.getKey();
            r.name = en.getValue();
            r.d = o.cache.get(r.lname);
            r.nicked = r.d != null && r.d.loaded && r.d.notFound;
            r.info = o.mc.getNetHandler() == null ? null : o.mc.getNetHandler().getPlayerInfo(r.name);
            if (r.info != null) {
                ScorePlayerTeam t = r.info.getPlayerTeam();
                if (t != null) {
                    r.color = Utils.lastColor(t.getColorPrefix());
                    r.team = t.getRegisteredName();
                    r.teamColor = Utils.colorChar(t.getColorPrefix());
                }
            }
            r.res = Snipe.analyze(r.lname, r.d, m);
            if (r.d != null && r.d.loaded && !r.d.notFound && !m.cols.isEmpty())
                r.sort = value(r.d, m, m.cols.get(Math.min(m.sortCol, m.cols.size() - 1)));
            out.add(r);
        }
        Collections.sort(out, new Comparator<Row>() {
            public int compare(Row a, Row b) {
                if (a.res.score != b.res.score) return b.res.score - a.res.score;
                return Double.compare(b.sort, a.sort);
            }
        });
        return o.cfg.teams ? groupByTeam(out, o) : out;
    }

    /** Em partida (2+ times com varios jogadores): agrupa por time, com cabecalho; seu time primeiro. */
    private static List<Row> groupByTeam(List<Row> rows, SmkzOverlay o) {
        Map<String, List<Row>> g = new TreeMap<>();
        List<Row> none = new ArrayList<>();
        int biggest = 0;
        for (Row r : rows) {
            if (r.team.isEmpty()) { none.add(r); continue; }
            List<Row> l = g.get(r.team);
            if (l == null) { l = new ArrayList<>(); g.put(r.team, l); }
            l.add(r);
            biggest = Math.max(biggest, l.size());
        }
        if (g.size() < 2 || biggest < 2) return rows;   // lobby ou solo: sem separacao
        String mine = "";
        try {
            if (o.mc.thePlayer != null && o.mc.thePlayer.getTeam() != null) mine = o.mc.thePlayer.getTeam().getRegisteredName();
        } catch (Exception ignored) {}
        List<String> order = new ArrayList<>(g.keySet());
        if (g.containsKey(mine)) { order.remove(mine); order.add(0, mine); }
        List<Row> out = new ArrayList<>();
        for (String t : order) {
            List<Row> l = g.get(t);
            Row h = new Row();
            h.header = true;
            h.lname = "#team:" + t;
            h.teamColor = l.get(0).teamColor;
            h.headerText = "\u00a7" + h.teamColor + "\u00a7l" + Utils.colorName(h.teamColor).toUpperCase()
                    + " \u00a78(" + l.size() + ")" + (t.equals(mine) ? " \u00a77- seu time" : "");
            out.add(h);
            out.addAll(l);
        }
        if (!none.isEmpty()) {
            Row h = new Row();
            h.header = true;
            h.lname = "#none";
            h.teamColor = '8';
            h.headerText = "\u00a77\u00a7lOUTROS \u00a78(" + none.size() + ")";
            out.add(h);
            out.addAll(none);
        }
        return out;
    }
}
