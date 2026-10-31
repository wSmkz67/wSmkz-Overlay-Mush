package br.smkzoverlay;

import java.util.*;

/** Score de suspeita de snipe (0-100) somando sinais. \u00c9 heur\u00edstica, n\u00e3o prova. */
public class Snipe {
    public static class Res {
        public int score;
        public boolean black;
        public final List<String> why = new ArrayList<>();
        void add(int pts, String reason) { score = Math.min(100, score + pts); why.add(reason); }
        public String reasons() {
            if (why.isEmpty()) return "nenhum sinal";
            StringBuilder sb = new StringBuilder();
            for (String s : why) { if (sb.length() > 0) sb.append("; "); sb.append(s); }
            return sb.toString();
        }
        public String pill() {
            if (black) return "BL";
            if (score >= 60) return "SNIPE?";
            if (score >= 30) return "ALERTA";
            return "";
        }
        public int color() {
            if (black) return 0xFFB91C1C;
            if (score >= 60) return 0xFFEF4444;
            if (score >= 30) return 0xFFF59E0B;
            return 0xFF22C55E;
        }
        public String tag() {
            if (black) return "\u00a74BL";
            if (score >= 60) return "\u00a7c!!";
            if (score >= 30) return "\u00a7e!";
            return "";
        }
    }

    public static Res analyze(String lname, PlayerData d, Config.Mode m) {
        Res r = new Res();
        SmkzOverlay o = SmkzOverlay.I;
        if (o.lists.has(true, lname)) { r.black = true; r.add(100, "na sua blacklist"); }
        int seen = o.history.recent(lname, 3L * 3600000L);
        if (seen >= 3) r.add(50, "visto em " + seen + " partidas suas nas \u00faltimas 3h");
        else if (seen == 2) r.add(25, "visto em 2 partidas suas nas \u00faltimas 3h");
        if (d != null && d.loaded) {
            if (d.notFound) {
                r.add(10, "nick: sem registro na API");
            } else {
                if (d.firstLogin > 0) {
                    long days = (System.currentTimeMillis() - d.firstLogin) / 86400000L;
                    if (days < 7) r.add(25, "conta criada h\u00e1 " + days + "d");
                    else if (days < 30) r.add(10, "conta criada h\u00e1 " + days + "d");
                }
                double w = d.stat(m, m.winsKey), l = d.stat(m, m.lossesKey), g = w + l;
                if (g > 0 && g < 30) r.add(15, "poucas partidas (" + (int) g + ")");
                if (g >= 10 && w / g >= 0.85) r.add(15, "winrate de " + (int) (100 * w / g) + "%");
            }
        }
        return r;
    }
}
