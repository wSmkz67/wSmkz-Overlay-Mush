package br.smkzoverlay;

import java.util.*;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;

/** Monta as linhas de jogadores (compartilhado entre HUD e menu). O resultado fica em cache (ver SmkzOverlay.rows). */
public class Rows {
    // limites de destaque (ajuste aqui)
    public static final double FKDR_HIGH = 7, FKDR_VERY = 10, WS_HIGH = 10, WS_VERY = 20;
    public static final int NICK_COLOR = 0xFF3B82F6;
    private static final String HEX = "0123456789abcdef";

    public static class Row {
        public String lname = "", name = "", color = "\u00a7f", team = "", headerText = "";
        public char teamColor = 'f';   // cor do cabecalho / do time do scoreboard
        public char armorTeam;         // cor do time pela armadura tingida (0 = desconhecida)
        public boolean header, nicked;
        public PlayerData d;
        public Snipe.Res res;
        public NetworkPlayerInfo info;
        public double sort;
        // pre-calculados uma vez por montagem (nao a cada frame)
        public String[] cells;
        public int[] bgs, cellW;
        public int nameW;

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

    // ---------------- time pela armadura ----------------
    /** Cor do time de um jogador, lida da armadura de couro tingida (Bed Wars); 0 se nao der para saber. */
    public static char armorChar(EntityPlayer p) {
        if (p == null) return 0;
        for (int slot = 3; slot >= 0; slot--) {
            ItemStack s = p.getCurrentArmor(slot);
            if (s == null || !(s.getItem() instanceof ItemArmor)) continue;
            ItemArmor a = (ItemArmor) s.getItem();
            if (a.getArmorMaterial() != ItemArmor.ArmorMaterial.LEATHER) continue;
            int rgb = a.getColor(s);
            if (rgb == 10511680) continue;   // couro sem tingir
            char c = Utils.teamCharFromRgb(rgb);
            if (c != 0) return c;
        }
        return 0;
    }

    // ---------------- linhas ----------------
    public static List<Row> build(final Config.Mode m) {
        SmkzOverlay o = SmkzOverlay.I;
        Map<String, EntityPlayer> ents = new HashMap<>();
        if (o.mc.theWorld != null)
            for (EntityPlayer p : o.mc.theWorld.playerEntities) ents.put(p.getName().toLowerCase(), p);
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
            char at = armorChar(ents.get(r.lname));
            if (at != 0) o.teamOf.put(r.lname, at);
            else { Character mem = o.teamOf.get(r.lname); if (mem != null) at = mem; }
            r.armorTeam = at;
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
        List<Row> res = o.cfg.teams ? groupByTeam(out, o, m) : out;
        finish(res, m, o);
        return res;
    }

    /** Textos e larguras das celulas, calculados uma vez por montagem. */
    private static void finish(List<Row> rows, Config.Mode m, SmkzOverlay o) {
        FontRenderer fr = o.mc.fontRendererObj;
        int nc = m.cols.size();
        for (Row r : rows) {
            if (r.header) continue;
            r.cells = new String[nc];
            r.bgs = new int[nc];
            r.cellW = new int[nc];
            for (int c = 0; c < nc; c++) {
                Config.Col col = m.cols.get(c);
                r.cells[c] = colText(r.d, m, col);
                r.bgs[c] = cellBg(r.d, m, col);
                r.cellW[c] = fr.getStringWidth(r.cells[c]);
            }
            r.nameW = fr.getStringWidth(r.nameStr());
        }
    }

    // ---------------- times ----------------
    /** Tamanho do time pelo modo (solo = 1, dupla = 2, trio = 3, quarteto = 4; desconhecido = 4). */
    private static int teamSize(Config.Mode m) {
        String id = m.id;
        if (id.contains("solo") || id.endsWith("1v1")) return 1;
        if (id.contains("dupla") || id.endsWith("2v2")) return 2;
        if (id.contains("trio")) return 3;
        if (id.contains("quarteto") || id.endsWith("4v4")) return 4;
        return 4;
    }

    private static char myTeamChar(SmkzOverlay o) {
        char a = armorChar(o.mc.thePlayer);
        if (a != 0) return a;
        try {
            Team t = o.mc.thePlayer == null ? null : o.mc.thePlayer.getTeam();
            if (t instanceof ScorePlayerTeam) return Utils.colorChar(((ScorePlayerTeam) t).getColorPrefix());
        } catch (Exception ignored) {}
        return 0;
    }

    /**
     * Agrupa por cor de time, na ordem do Bed Wars (vermelho, azul, verde, amarelo, ciano, branco, rosa, cinza),
     * com o seu time primeiro. A cor vem da armadura tingida; se nao houver, dos times do scoreboard
     * (so quando parecem times de partida e nao tags/ranks do lobby).
     */
    private static List<Row> groupByTeam(List<Row> rows, SmkzOverlay o, Config.Mode m) {
        int ts = teamSize(m);
        if (ts <= 1) return rows;
        Map<Character, List<Row>> g = new HashMap<>();
        List<Row> none = new ArrayList<>();
        int tagged = 0;
        for (Row r : rows) {   // 1) armadura
            if (r.armorTeam != 0) {
                List<Row> l = g.get(r.armorTeam);
                if (l == null) { l = new ArrayList<>(); g.put(r.armorTeam, l); }
                l.add(r);
                tagged++;
            } else none.add(r);
        }
        boolean ok = !g.isEmpty() && g.size() <= 8;
        for (List<Row> l : g.values()) if (l.size() > ts) ok = false;
        if (ok && !o.inMatchTeams) ok = tagged >= 4 && g.size() >= 2;
        if (!ok) {   // 2) times do scoreboard
            g.clear();
            none.clear();
            Map<String, List<Row>> byName = new HashMap<>();
            for (Row r : rows) {
                if (r.team.isEmpty()) { none.add(r); continue; }
                List<Row> l = byName.get(r.team);
                if (l == null) { l = new ArrayList<>(); byName.put(r.team, l); }
                l.add(r);
            }
            if (byName.isEmpty() || byName.size() > 8) return rows;
            Set<Character> cs = new HashSet<>();
            int full = 0;
            for (List<Row> l : byName.values()) {
                if (l.size() > ts) return rows;
                if (l.size() == ts) full++;
                cs.add(l.get(0).teamColor);
            }
            if (cs.size() != byName.size()) return rows;   // cores repetidas = ranks/tags de lobby
            if (!o.inMatchTeams && (byName.size() < 2 || full < 2)) return rows;
            for (List<Row> l : byName.values()) g.put(l.get(0).teamColor, l);
        }
        o.inMatchTeams = true;   // trava ate trocar de mundo, para nao piscar
        return assemble(g, none, myTeamChar(o));
    }

    private static List<Row> assemble(Map<Character, List<Row>> g, List<Row> none, char mine) {
        List<Character> order = new ArrayList<>();
        for (int i = 0; i < Utils.TEAM_ORDER.length(); i++) if (g.containsKey(Utils.TEAM_ORDER.charAt(i))) order.add(Utils.TEAM_ORDER.charAt(i));
        List<Character> rest = new ArrayList<>(g.keySet());
        rest.removeAll(order);
        Collections.sort(rest);
        order.addAll(rest);
        if (mine != 0 && order.remove(Character.valueOf(mine))) order.add(0, mine);
        List<Row> out = new ArrayList<>();
        for (char ch : order) {
            List<Row> l = g.get(ch);
            Row h = new Row();
            h.header = true;
            h.lname = "#team:" + ch;
            h.teamColor = ch;
            h.headerText = "\u00a7" + ch + "\u00a7l" + Utils.colorName(ch).toUpperCase()
                    + " \u00a78(" + l.size() + ")" + (ch == mine ? " \u00a77- seu time" : "");
            out.add(h);
            for (Row r : l) { r.color = "\u00a7" + ch; out.add(r); }   // nick na cor do time
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
