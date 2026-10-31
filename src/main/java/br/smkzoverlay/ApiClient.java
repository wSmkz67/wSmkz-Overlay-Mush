package br.smkzoverlay;

import com.google.gson.JsonParser;
import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;
import net.minecraft.client.Minecraft;

/** Fila com uma thread pr\u00f3pria: uma requisi\u00e7\u00e3o por vez, com pausa entre elas e retentativas. */
public class ApiClient {
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

    public ApiClient() {
        Thread t = new Thread(new Runnable() { public void run() { loop(); } }, "SmkzOverlay-API");
        t.setDaemon(true);
        t.start();
    }

    public void enqueue(String nick) { queue.offer(nick); }
    public void clear() { queue.clear(); }

    private void loop() {
        while (true) {
            try {
                String nick = queue.take();
                final PlayerData d = fetch(nick);
                Minecraft.getMinecraft().addScheduledTask(new Runnable() { public void run() { SmkzOverlay.I.onData(d); } });
                Thread.sleep(SmkzOverlay.I.cfg.requestDelayMs);
            } catch (InterruptedException e) {
                return;
            } catch (Throwable ignored) {}
        }
    }

    public PlayerData fetch(String nick) {
        PlayerData d = new PlayerData(nick);
        d.fetched = System.currentTimeMillis();
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpURLConnection c = null;
            try {
                String url = String.format(SmkzOverlay.I.cfg.apiUrl, URLEncoder.encode(nick, "UTF-8"));
                c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                c.setRequestProperty("User-Agent", "SmkzOverlay/1.0");
                c.setRequestProperty("Accept", "application/json");
                int code = c.getResponseCode();
                if (code == 429) { Thread.sleep(1500L * (attempt + 1)); continue; }
                if (code == 404) { d.notFound = true; d.loaded = true; return d; }
                if (code != 200) { d.error = "HTTP " + code; continue; }
                d.parse(new JsonParser().parse(read(c.getInputStream())));
                d.loaded = true;
                d.error = null;
                return d;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                d.error = e.getClass().getSimpleName();
            } finally {
                if (c != null) c.disconnect();
            }
        }
        d.failed = true;
        return d;
    }

    private static String read(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        in.close();
        return new String(b.toByteArray(), "UTF-8");
    }
}
