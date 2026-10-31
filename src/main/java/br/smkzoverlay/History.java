package br.smkzoverlay;

import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.*;

/** Guarda com que frequ\u00eancia cada nick apareceu nas suas partidas (sinal forte de snipe). */
public class History {
    private final File f;
    private Map<String, List<Long>> m = new HashMap<>();

    public History(File f) {
        this.f = f;
        try {
            if (f.exists()) {
                Reader r = new InputStreamReader(new FileInputStream(f), "UTF-8");
                Map<String, List<Long>> x = Config.G.fromJson(r, new TypeToken<Map<String, List<Long>>>() {}.getType());
                r.close();
                if (x != null) m = x;
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void commit(Collection<String> names) {
        if (names.size() < 2) return;
        long now = System.currentTimeMillis();
        for (String n : names) {
            List<Long> l = m.get(n);
            if (l == null) { l = new ArrayList<>(); m.put(n, l); }
            l.add(now);
            while (l.size() > 10) l.remove(0);
        }
        Iterator<Map.Entry<String, List<Long>>> it = m.entrySet().iterator();
        while (it.hasNext()) {
            List<Long> l = it.next().getValue();
            Iterator<Long> li = l.iterator();
            while (li.hasNext()) if (now - li.next() > 86400000L) li.remove();
            if (l.isEmpty()) it.remove();
        }
        save();
    }

    public int recent(String lname, long windowMs) {
        List<Long> l = m.get(lname);
        if (l == null) return 0;
        long now = System.currentTimeMillis();
        int c = 0;
        for (Long t : l) if (now - t <= windowMs) c++;
        return c;
    }

    private void save() {
        try {
            Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
            Config.G.toJson(m, w);
            w.close();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
