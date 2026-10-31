package br.smkzoverlay;

import com.google.gson.JsonElement;
import java.io.File;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = "smkzoverlay", name = "SmkzOverlay", version = "1.0.0", clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class SmkzOverlay {
    public static SmkzOverlay I;
    public final Minecraft mc = Minecraft.getMinecraft();
    public Config cfg;
    public ListStore lists;
    public History history;
    public QuickBuy qb;
    public ApiClient api;

    // Tudo abaixo s\u00f3 \u00e9 tocado na thread principal do jogo.
    public final Map<String, PlayerData> cache = new HashMap<>();
    public final Set<String> pending = new HashSet<>();
    public final Map<String, String> present = new LinkedHashMap<>();   // nick min\u00fasculo -> nick
    public final Set<String> seen = new HashSet<>();
    public final Set<String> alerted = new HashSet<>();
    public boolean inMatchTeams;   // times de partida detectados neste mundo
    public final Set<String> npcs = new HashSet<>();   // NPCs detectados nesta partida
    public String modeOverride;
    public boolean openMenu, openTheme;
    private Config.Mode detected;
    private File dir;
    private KeyBinding keyMenu, keyHud;
    private int tick;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) { dir = e.getModConfigurationDirectory(); }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        I = this;
        File d = new File(dir, "smkzoverlay");
        d.mkdirs();
        cfg = Config.load(new File(d, "config.json"));
        lists = new ListStore(new File(d, "lists.json"));
        history = new History(new File(d, "history.json"));
        qb = new QuickBuy(new File(d, "quickbuy.json"));
        api = new ApiClient();
        keyMenu = new KeyBinding("Abrir menu", Keyboard.KEY_APOSTROPHE, "SmkzOverlay");
        keyHud = new KeyBinding("Ligar/desligar HUD", Keyboard.KEY_NONE, "SmkzOverlay");
        ClientRegistry.registerKeyBinding(keyMenu);
        ClientRegistry.registerKeyBinding(keyHud);
        ClientCommandHandler.instance.registerCommand(new Cmd());
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
    }

    // ---------- util ----------
    public void msg(String s) {
        if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText("\u00a78[\u00a7bSmkz\u00a78] \u00a7r" + s));
    }

    public String modeIds() {
        StringBuilder sb = new StringBuilder();
        for (Config.Mode m : cfg.modes) { if (sb.length() > 0) sb.append(", "); sb.append(m.id); }
        return sb.toString();
    }

    public Config.Mode curMode() {
        if (modeOverride != null && cfg.find(modeOverride) != null) return cfg.find(modeOverride);
        if (detected != null) return detected;
        Config.Mode m = cfg.find(cfg.defaultMode);
        return m != null ? m : cfg.modes.get(0);
    }

    public void cycleMode() {
        Config.Mode cur = curMode();
        int i = cfg.modes.indexOf(cur);
        modeOverride = cfg.modes.get((i + 1) % cfg.modes.size()).id;
        reevaluateAll();
    }

    private static String strip(String t) { return t == null ? "" : EnumChatFormatting.getTextWithoutFormattingCodes(t).toLowerCase(); }

    private static boolean any(String text, List<String> kws) {
        for (String k : kws) if (text.contains(k.toLowerCase())) return true;
        return false;
    }

    /** L\u00ea t\u00edtulo + linhas do scoreboard e escolhe o primeiro modo que combina (varia\u00e7\u00f5es v\u00eam antes). */
    private Config.Mode detect() {
        if (mc.theWorld == null) return null;
        Scoreboard sb = mc.theWorld.getScoreboard();
        ScoreObjective o = sb.getObjectiveInDisplaySlot(1);
        if (o == null) return null;
        StringBuilder all = new StringBuilder(strip(o.getDisplayName()));
        for (Score sc : sb.getSortedScores(o)) {
            String n = sc.getPlayerName();
            if (n == null || n.startsWith("#")) continue;
            all.append(' ').append(strip(ScorePlayerTeam.formatPlayerName(sb.getPlayersTeam(n), n)));
        }
        String text = all.toString();
        for (Config.Mode m : cfg.modes) {
            if (!any(text, m.keywords)) continue;
            if (m.variants.isEmpty() || any(text, m.variants)) return m;
        }
        return null;
    }

    // ---------- alertas ----------
    private void alert(String l, String type, String text, String sound, float pitch) {
        if (!alerted.add(type + ":" + l)) return;
        msg(text);
        if (cfg.sound && mc.thePlayer != null) mc.thePlayer.playSound(sound, 1.0f, pitch);
    }

    private void immediate(String l, String n) {
        ListStore.Entry b = lists.black.get(l);
        if (b != null) alert(l, "bl", "\u00a7c\u00a7lBLACKLIST \u00a7f" + n + (b.reason.isEmpty() ? "" : " \u00a77(" + b.reason + ")"), "random.anvil_land", 0.8f);
        ListStore.Entry w = lists.watch.get(l);
        if (w != null) alert(l, "watch", "\u00a7bLista de interesse: \u00a7f" + n + (w.reason.isEmpty() ? "" : " \u00a77(" + w.reason + ")"), "random.levelup", 1.0f);
    }

    private void evaluate(String l) {
        PlayerData d = cache.get(l);
        if (d == null || !d.loaded || !present.containsKey(l)) return;
        String n = present.get(l);
        Config.Mode m = curMode();
        if (!d.notFound && m.wsAlert > 0) {
            double ws = d.stat(m, m.wsKey);
            if (ws >= m.wsAlert) alert(l, "ws:" + m.id, "\u00a76WINSTREAK \u00a7f" + n + " \u00a77(" + m.name + "): \u00a7e" + (int) ws, "note.pling", 2.0f);
        }
        if (!lists.has(true, l)) {
            Snipe.Res r = Snipe.analyze(l, d, m);
            if (r.score >= 60) alert(l, "snipe", "\u00a7cSuspeita de snipe: \u00a7f" + n + " \u00a77(" + r.reasons() + ")", "random.orb", 0.6f);
        }
    }

    private void reevaluateAll() { for (String l : new ArrayList<>(present.keySet())) evaluate(l); }

    public void onData(PlayerData d) {
        String l = d.name.toLowerCase();
        pending.remove(l);
        cache.put(l, d);
        evaluate(l);
    }

    public void debug(final String nick) {
        new Thread(new Runnable() {
            public void run() {
                final PlayerData d = api.fetch(nick);
                mc.addScheduledTask(new Runnable() {
                    public void run() {
                        if (d.root == null) { msg("\u00a7cSem JSON utiliz\u00e1vel (" + (d.error == null ? "nick n\u00e3o encontrado?" : d.error) + "). Confira apiUrl no config.json."); return; }
                        msg("\u00a77Chaves do topo: \u00a7f" + Json.keys(d.root, 60));
                        Set<String> done = new HashSet<>();
                        for (Config.Mode m : cfg.modes) {
                            if (!done.add(m.apiKey)) continue;
                            JsonElement e = Json.find(d.root, m.apiKey, true);
                            msg("\u00a77" + m.id + ": " + (e == null ? "\u00a7cobjeto '" + m.apiKey + "' n\u00e3o encontrado" : "\u00a7a" + Json.groups(e)));
                        }
                    }
                });
            }
        }, "SmkzOverlay-debug").start();
    }

    // ---------- eventos ----------
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (openMenu && mc.currentScreen == null) { openMenu = false; mc.displayGuiScreen(new StatsGui()); }
        if (openTheme && mc.currentScreen == null) { openTheme = false; mc.displayGuiScreen(new ThemeGui(null)); }
        if (mc.theWorld == null || mc.thePlayer == null || mc.getNetHandler() == null) return;
        tick++;
        if (tick % 20 == 0) {
            Config.Mode old = detected;
            detected = detect();
            if (detected != old) reevaluateAll();
        }
        if (tick % 10 == 0) scan();
    }

    /** Alerta no chat: quickbuy igual (ou quase) a de um sniper conhecido. */
    public void qbAlert(String l, String n, QuickBuy.Match m) {
        if (!present.containsKey(l)) return;
        alert(l, "qb", "\u00a7c\u00a7lQUICKBUY \u00a7f" + n + " \u00a77tem \u00a7e" + m.pct + "% \u00a77igual \u00e0 de \u00a7c" + m.central, "note.bass", 0.7f);
    }

    /** Mostra time e prefixo de cada jogador na tab (para conferir/ajustar a separacao por times). */
    public void debugTeams() {
        int n = 0;
        for (Map.Entry<String, String> en : present.entrySet()) {
            if (n++ >= 20) { msg("\u00a77..."); break; }
            NetworkPlayerInfo i = mc.getNetHandler() == null ? null : mc.getNetHandler().getPlayerInfo(en.getValue());
            ScorePlayerTeam t = i == null ? null : i.getPlayerTeam();
            msg("\u00a7f" + en.getValue() + " \u00a77time=\u00a7f" + (t == null ? "-" : t.getRegisteredName())
                    + " \u00a77prefixo=\u00a7f" + (t == null ? "-" : t.getColorPrefix().replace('\u00a7', '&')));
        }
        if (n == 0) msg("\u00a77Nenhum jogador na lista.");
    }

    /** A API nao conhece o nome e o ping na tab e 0: tipico de NPC (um nicked de verdade tem ping). */
    private boolean looksLikeNpc(String l, String n) {
        PlayerData d = cache.get(l);
        if (d == null || !d.loaded || !d.notFound) return false;
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(n);
        return info != null && info.getResponseTime() <= 0;
    }

    private void scan() {
        present.clear();
        String self = mc.thePlayer.getName();
        for (NetworkPlayerInfo i : new ArrayList<>(mc.getNetHandler().getPlayerInfoMap())) {
            String n = i.getGameProfile().getName();
            if (Utils.isBot(n, i.getGameProfile().getId()) || n.equalsIgnoreCase(self) || lists.isIgnored(n) || npcs.contains(n.toLowerCase())) continue;
            present.put(n.toLowerCase(), n);
        }
        if (present.size() > cfg.maxPlayers) return;   // lobby grande: n\u00e3o consulta ningu\u00e9m
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<String, String>> it = present.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, String> en = it.next();
            String l = en.getKey(), n = en.getValue();
            if (cfg.npcPing && looksLikeNpc(l, n)) { npcs.add(l); seen.remove(l); it.remove(); continue; }
            if (seen.add(l)) immediate(l, n);
            PlayerData d = cache.get(l);
            boolean refetch = d == null
                    || now - d.fetched > cfg.cacheMinutes * 60000L
                    || (d.failed && now - d.fetched > 15000L);
            if (refetch && pending.add(l)) api.enqueue(n);
            if (cfg.qb && qb.hasCentrals() && curMode().apiKey.equals("bedwars") && qb.want(l)) qb.request(n);
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (keyMenu.isPressed()) openMenu = true;
        if (keyHud.isPressed()) { cfg.hud = !cfg.hud; cfg.save(); msg("HUD " + (cfg.hud ? "\u00a7aligado" : "\u00a7cdesligado")); }
    }

    @SubscribeEvent
    public void onWorld(WorldEvent.Load e) {
        if (!e.world.isRemote) return;
        history.commit(seen);
        seen.clear(); alerted.clear(); present.clear(); pending.clear(); npcs.clear(); inMatchTeams = false;
        if (qb != null) qb.resetSession();
        if (api != null) api.clear();
    }

    private static int pillW(FontRenderer fr, String p) { return fr.getStringWidth("\u00a7l" + p) + 6; }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Text e) {
        if (!cfg.hud || mc.theWorld == null || mc.currentScreen instanceof StatsGui || mc.currentScreen instanceof ThemeGui) return;
        if (present.isEmpty() || present.size() > cfg.maxPlayers) return;
        Config.Mode m = curMode();
        List<Rows.Row> all = Rows.build(m);
        boolean teamMode = false;
        for (Rows.Row r : all) if (r.header) { teamMode = true; break; }
        int shown = Math.min(all.size(), teamMode ? 60 : cfg.maxRows);   // em modo times mostra todos
        if (shown == 0) return;
        FontRenderer fr = mc.fontRendererObj;
        int nc = m.cols.size();
        int a1 = Theme.a1(), a2 = Theme.a2();

        int nameW = fr.getStringWidth("JOGADOR"), pw = 0, hdrW = 0;
        int[] cw = new int[nc];
        for (int c = 0; c < nc; c++) cw[c] = fr.getStringWidth(m.cols.get(c).label.toUpperCase());
        String[][] cells = new String[shown][nc];
        int[][] bgs = new int[shown][nc];
        for (int i = 0; i < shown; i++) {
            Rows.Row r = all.get(i);
            if (r.header) { hdrW = Math.max(hdrW, fr.getStringWidth(r.headerText)); continue; }
            nameW = Math.max(nameW, fr.getStringWidth(r.nameStr()));
            String p = r.pill();
            if (!p.isEmpty()) pw = Math.max(pw, pillW(fr, p));
            for (int c = 0; c < nc; c++) {
                Config.Col col = m.cols.get(c);
                cells[i][c] = Rows.colText(r.d, m, col);
                bgs[i][c] = Rows.cellBg(r.d, m, col);
                cw[c] = Math.max(cw[c], fr.getStringWidth(cells[i][c]));
            }
        }
        int x0 = cfg.hudX, y0 = cfg.hudY;
        int nameX = x0 + 16, tagX = nameX + nameW + 4;
        int statX = tagX + (pw > 0 ? pw + 4 : 0) + 4;
        int total = 0;
        for (int c = 0; c < nc; c++) total += cw[c] + (c > 0 ? 9 : 0);
        int W = Math.max(Math.max(statX - x0 + total + 6, fr.getStringWidth("Smkz Overlay") + fr.getStringWidth(m.name) + 20), hdrW + 14);
        int H = 14 + 11 + shown * 11 + 3;

        GlStateManager.pushMatrix();
        GlStateManager.scale(cfg.hudScale, cfg.hudScale, 1f);
        Gui.drawRect(x0, y0, x0 + W, y0 + H, 0xB8101018);
        Gui.drawRect(x0, y0, x0 + W / 2, y0 + 2, a1);
        Gui.drawRect(x0 + W / 2, y0, x0 + W, y0 + 2, a2);
        Gui.drawRect(x0, y0 + 2, x0 + W, y0 + 14, Theme.alpha(Theme.dark(a1, 0.7f), 0xC0));
        Theme.title(fr, x0 + 5, y0 + 4);
        fr.drawStringWithShadow(m.name, x0 + W - 5 - fr.getStringWidth(m.name), y0 + 4, a2);

        int hy = y0 + 14;
        Gui.drawRect(x0, hy, x0 + W, hy + 11, 0x50000000);
        fr.drawString("\u00a78JOGADOR", nameX, hy + 2, 0xFFFFFF);
        int cx = statX;
        for (int c = 0; c < nc; c++) {
            String lb = m.cols.get(c).label.toUpperCase();
            fr.drawString("\u00a77" + lb, cx + cw[c] - fr.getStringWidth(lb), hy + 2, 0xFFFFFF);
            cx += cw[c] + 9;
        }

        for (int i = 0; i < shown; i++) {
            Rows.Row r = all.get(i);
            int y = hy + 11 + i * 11;
            if (r.header) {   // cabecalho do time
                int rgb = Utils.rgbOf(r.teamColor);
                Gui.drawRect(x0, y, x0 + W, y + 11, Theme.alpha(rgb, 0x38));
                Gui.drawRect(x0, y, x0 + 2, y + 11, 0xFF000000 | rgb);
                fr.drawStringWithShadow(r.headerText, x0 + 6, y + 2, 0xFFFFFF);
                continue;
            }
            if (i % 2 == 1) Gui.drawRect(x0, y, x0 + W, y + 11, 0x16FFFFFF);
            Gui.drawRect(x0, y, x0 + 2, y + 11, r.pillColor());
            if (r.info != null) {
                mc.getTextureManager().bindTexture(r.info.getLocationSkin());
                GlStateManager.color(1f, 1f, 1f, 1f);
                Gui.drawScaledCustomSizeModalRect(x0 + 5, y + 1, 8, 8, 8, 8, 9, 9, 64, 64);
            }
            fr.drawStringWithShadow(r.nameStr(), nameX, y + 2, 0xFFFFFF);
            String p = r.pill();
            if (!p.isEmpty()) {
                Gui.drawRect(tagX, y + 1, tagX + pillW(fr, p), y + 10, r.pillColor());
                fr.drawString("\u00a7f\u00a7l" + p, tagX + 3, y + 2, 0xFFFFFF);
            }
            cx = statX;
            for (int c = 0; c < nc; c++) {
                int right = cx + cw[c], tw = fr.getStringWidth(cells[i][c]);
                if (bgs[i][c] != 0) Gui.drawRect(right - tw - 2, y, right + 2, y + 11, bgs[i][c]);
                fr.drawStringWithShadow(cells[i][c], right - tw, y + 2, 0xFFFFFF);
                cx += cw[c] + 9;
            }
        }
        Gui.drawRect(x0, y0 + H - 1, x0 + W, y0 + H, a2);
        GlStateManager.popMatrix();
    }
}
