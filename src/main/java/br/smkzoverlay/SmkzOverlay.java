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
    public ApiClient api;

    // Tudo abaixo s\u00f3 \u00e9 tocado na thread principal do jogo.
    public final Map<String, PlayerData> cache = new HashMap<>();
    public final Set<String> pending = new HashSet<>();
    public final Map<String, String> present = new LinkedHashMap<>();   // nick min\u00fasculo -> nick
    public final Set<String> seen = new HashSet<>();
    public final Set<String> alerted = new HashSet<>();
    public String modeOverride;
    public boolean openMenu;
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
                        msg("\u00a77Chaves do topo: \u00a7f" + Json.keys(d.root));
                        for (Config.Mode m : cfg.modes) {
                            JsonElement e = Json.find(d.root, m.apiKey, true);
                            msg("\u00a77" + m.id + ": " + (e == null ? "\u00a7cobjeto '" + m.apiKey + "' n\u00e3o encontrado" : "\u00a7a" + Json.keys(e)));
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
        if (mc.theWorld == null || mc.thePlayer == null || mc.getNetHandler() == null) return;
        tick++;
        if (tick % 20 == 0) {
            Config.Mode old = detected;
            detected = detect();
            if (detected != old) reevaluateAll();
        }
        if (tick % 10 == 0) scan();
    }

    private void scan() {
        present.clear();
        String self = mc.thePlayer.getName();
        for (NetworkPlayerInfo i : new ArrayList<>(mc.getNetHandler().getPlayerInfoMap())) {
            String n = i.getGameProfile().getName();
            if (Utils.isBot(n, i.getGameProfile().getId()) || n.equalsIgnoreCase(self)) continue;
            present.put(n.toLowerCase(), n);
        }
        if (present.size() > cfg.maxPlayers) return;   // lobby grande: n\u00e3o consulta ningu\u00e9m
        long now = System.currentTimeMillis();
        for (Map.Entry<String, String> en : present.entrySet()) {
            String l = en.getKey(), n = en.getValue();
            if (seen.add(l)) immediate(l, n);
            PlayerData d = cache.get(l);
            boolean refetch = d == null
                    || now - d.fetched > cfg.cacheMinutes * 60000L
                    || (d.failed && now - d.fetched > 15000L);
            if (refetch && pending.add(l)) api.enqueue(n);
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
        seen.clear(); alerted.clear(); present.clear(); pending.clear();
        if (api != null) api.clear();
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Text e) {
        if (!cfg.hud || mc.theWorld == null || mc.currentScreen instanceof StatsGui) return;
        if (present.isEmpty() || present.size() > cfg.maxPlayers) return;
        Config.Mode m = curMode();
        List<Rows.Row> rows = Rows.build(m);
        FontRenderer fr = mc.fontRendererObj;
        int shown = Math.min(rows.size(), cfg.maxRows);
        String title = "\u00a7b\u00a7lSmkz\u00a7d\u00a7lOverlay \u00a78| \u00a77" + m.name;
        int w = fr.getStringWidth(title);
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < shown; i++) {
            String l = Rows.hudLine(rows.get(i), m);
            lines.add(l);
            w = Math.max(w, fr.getStringWidth(l) + 4);
        }
        int x = cfg.hudX, y = cfg.hudY, h = 15 + shown * 11 + 2;
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.85f, 0.85f, 1f);
        Gui.drawRect(x - 3, y - 3, x + w + 5, y + h, 0xA0101020);
        Gui.drawRect(x - 3, y - 3, x + w + 5, y - 1, 0xFF8B5CF6);
        Gui.drawRect(x - 3, y - 1, x + w + 5, y + 11, 0xB0241B4D);
        Gui.drawRect(x - 3, y + 11, x + w + 5, y + 12, 0xFF22D3EE);
        fr.drawStringWithShadow(title, x + 2, y + 1, 0xFFFFFF);
        int ly = y + 15;
        for (int i = 0; i < shown; i++) {
            Gui.drawRect(x - 3, ly - 1, x - 1, ly + 9, rows.get(i).res.color());
            fr.drawStringWithShadow(lines.get(i), x + 2, ly, 0xFFFFFF);
            ly += 11;
        }
        GlStateManager.popMatrix();
    }
}
