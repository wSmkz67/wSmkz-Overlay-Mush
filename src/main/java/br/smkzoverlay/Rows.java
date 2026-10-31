package br.smkzoverlay;

import java.util.*;
import net.minecraft.client.network.NetworkPlayerInfo;

/** Monta as linhas de jogadores (compartilhado entre HUD e menu). */
public class Rows {
    public static class Row {
        public String lname, name, color = "\u00a7f";
        public PlayerData d;
        public Snipe.Res res;
        public NetworkPlayerInfo info;
        public double sort;
    }

    public static double value(PlayerData d, Config.Mode m, Config.Col c) {
        double a = d.stat(m, c.key);
        return c.ratio() ? a / Math.max(1, d.stat(m, c.per)) : a;
    }

    public static String color(double v, Config.Col c) {
        if (c.danger > 0 && v >= c.danger) return "\u00a7c";
        if (c.warn > 0 && v >= c.warn) return "\u00a7e";
        return "\u00a7a";
    }

    public static String colText(PlayerData d, Config.Mode m, Config.Col c) {
        if (d == null) return "\u00a77...";
        if (d.failed) return "\u00a7cerro";
        if (d.notFound) return "\u00a78-";
        double v = value(d, m, c);
        return color(v, c) + Utils.fmt(v, c.ratio());
    }

    public static List<Row> build(final Config.Mode m) {
        SmkzOverlay o = SmkzOverlay.I;
        List<Row> out = new ArrayList<>();
        for (Map.Entry<String, String> en : o.present.entrySet()) {
            Row r = new Row();
            r.lname = en.getKey();
            r.name = en.getValue();
            r.d = o.cache.get(r.lname);
            r.info = o.mc.getNetHandler() == null ? null : o.mc.getNetHandler().getPlayerInfo(r.name);
            if (r.info != null && r.info.getPlayerTeam() != null) r.color = Utils.lastColor(r.info.getPlayerTeam().getColorPrefix());
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
        return out;
    }

    public static String hudLine(Row r, Config.Mode m) {
        String head = (r.res.tag().isEmpty() ? "" : r.res.tag() + " ") + r.color + r.name;
        if (r.d == null) return head + " \u00a77carregando...";
        if (r.d.failed) return head + " \u00a7cerro na API";
        if (r.d.notFound) return head + " \u00a78sem dados (nick?)";
        StringBuilder sb = new StringBuilder(head);
        for (Config.Col c : m.cols) sb.append(" \u00a78| \u00a77").append(c.label).append(' ').append(colText(r.d, m, c));
        return sb.toString();
    }
}
