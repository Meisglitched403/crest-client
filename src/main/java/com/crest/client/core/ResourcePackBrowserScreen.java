package com.crest.client.core;

import com.crest.client.ui.Anim;
import com.crest.client.ui.ColorUtil;
import com.crest.client.ui.Panel;
import com.crest.client.ui.Theme;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ResourcePackBrowserScreen extends Screen {

    private final Screen parent;

    private static final String MODRINTH_SEARCH_BASE =
            "https://api.modrinth.com/v2/search?facets=" +
                    URLEncoder.encode("[[\"project_type:resourcepack\"]]", java.nio.charset.StandardCharsets.UTF_8) +
                    "&index=downloads&limit=30&query=";
    private static final String MODRINTH_VERSIONS =
            "https://api.modrinth.com/v2/project/";

    private enum State { LOADING, LIST, ERROR, FALLBACK }
    private State state = State.LOADING;

    private String query = "";
    private boolean searchFocused = false;
    private int cursorBlink = 0;

    private List<PackEntry> packs = new ArrayList<>();
    private String status = "Loading packs...";
    private boolean loading = true;

    private float scrollOffset = 0;
    private float scrollTarget = 0;
    private int maxScroll = 0;
    private int hoveredIndex = -1;

    private int panelX, panelY, panelW, panelH;
    private int listX, listY, listW, listH;
    private int rowH = 64;
    private int rowGap = 8;

    private final HttpClient http = HttpClient.newHttpClient();
    private CompletableFuture<Void> fetchFuture;
    private final Object fetchLock = new Object();

    private static class PackEntry {
        String id;
        String slug;
        String title;
        String description;
        String author;
        long downloads;
        String iconUrl;
        boolean installed;

        PackEntry() {}
    }

    public ResourcePackBrowserScreen(Screen parent) {
        super(Component.literal("Resource Pack Browser"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelX = 30;
        panelY = 30;
        panelW = width - 60;
        panelH = height - 60;
        listX = panelX + 20;
        listY = panelY + 84;
        listW = panelW - 40;
        listH = panelH - 110;
        startFetch(query);
    }

    @Override
    public void onClose() {
        if (fetchFuture != null) fetchFuture.cancel(true);
        minecraft.setScreen(parent);
    }

    // ---- Fetch ----

    private void startFetch(String q) {
        synchronized (fetchLock) {
            if (fetchFuture != null && !fetchFuture.isDone()) fetchFuture.cancel(true);
            loading = true;
            state = State.LOADING;
            status = q.isEmpty() ? "Loading packs..." : "Searching...";
            final String ql = q.toLowerCase();
            fetchFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return fetchModrinth(q);
                } catch (Exception e) {
                    return null;
                }
            }).thenAccept(result -> {
                loading = false;
                List<PackEntry> base;
                if (result != null && !result.isEmpty()) {
                    base = result;
                    state = State.LIST;
                } else {
                    try {
                        base = loadFallback();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                        state = State.ERROR;
                        status = "Failed to load packs: " + e2.getMessage();
                        refreshInstalledFlags();
                        maxScroll = 0;
                        return;
                    }
                    state = State.FALLBACK;
                    status = result == null ? "Offline — showing bundled packs" : "No results — showing bundled packs";
                }
                // Client-side filter so offline search still narrows the list
                if (!ql.isEmpty()) {
                    base = base.stream()
                            .filter(p -> p.title.toLowerCase().contains(ql)
                                    || p.author.toLowerCase().contains(ql)
                                    || p.description.toLowerCase().contains(ql))
                            .collect(Collectors.toList());
                }
                packs = base;
                if (packs.isEmpty()) {
                    status = "No packs match \"" + q + "\"";
                } else if (state == State.LIST) {
                    status = "";
                }
                refreshInstalledFlags();
                maxScroll = 0;
                scrollTarget = 0;
                scrollOffset = 0;
            });
        }
    }

    private List<PackEntry> fetchModrinth(String q) throws Exception {
        String url = MODRINTH_SEARCH_BASE + URLEncoder.encode(q.isEmpty() ? "" : q, java.nio.charset.StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "crest-client")
                .timeout(java.time.Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new IOException("HTTP " + resp.statusCode());
        JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
        JsonArray hits = root.getAsJsonArray("hits");
        List<PackEntry> out = new ArrayList<>();
        for (JsonElement el : hits) {
            JsonObject h = el.getAsJsonObject();
            PackEntry p = new PackEntry();
            p.id = h.has("project_id") ? h.get("project_id").getAsString() : "";
            p.slug = h.has("slug") ? h.get("slug").getAsString() : p.id;
            p.title = h.has("title") ? h.get("title").getAsString() : p.slug;
            p.description = h.has("description") ? h.get("description").getAsString() : "";
            p.author = h.has("author") ? h.get("author").getAsString() : "";
            p.downloads = h.has("downloads") ? h.get("downloads").getAsLong() : 0;
            p.iconUrl = h.has("icon_url") ? h.get("icon_url").getAsString() : null;
            out.add(p);
        }
        return out;
    }

    private List<PackEntry> loadFallback() throws Exception {
        InputStream in = ResourcePackBrowserScreen.class
                .getResourceAsStream("/resourcepacks.json");
        if (in == null) throw new IOException("resourcepacks.json not found on classpath");
        try (in) {
            String txt = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            JsonArray arr = JsonParser.parseString(txt).getAsJsonArray();
            List<PackEntry> out = new ArrayList<>();
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                PackEntry p = new PackEntry();
                p.id = o.has("project_id") ? o.get("project_id").getAsString() : "";
                p.slug = o.has("slug") ? o.get("slug").getAsString() : p.id;
                p.title = o.has("title") ? o.get("title").getAsString() : p.slug;
                p.description = o.has("description") ? o.get("description").getAsString() : "";
                p.author = o.has("author") ? o.get("author").getAsString() : "";
                p.iconUrl = o.has("icon_url") ? o.get("icon_url").getAsString() : null;
                out.add(p);
            }
            return out;
        }
    }

    private void refreshInstalledFlags() {
        Path dir = Minecraft.getInstance().getResourcePackDirectory();
        for (PackEntry p : packs) {
            Path target = dir.resolve(p.slug + ".zip");
            p.installed = Files.exists(target);
        }
    }

    // ---- Install ----

    private void install(PackEntry p) {
        if (loading) return;
        p.installed = true; // optimistic
        status = "Downloading " + p.title + "...";
        CompletableFuture.runAsync(() -> {
            try {
                String fileUrl = fetchDownloadUrl(p.id);
                String sha1 = fetchSha1(p.id);
                Path dir = Minecraft.getInstance().getResourcePackDirectory();
                Path target = dir.resolve(p.slug + ".zip");
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(fileUrl))
                        .header("User-Agent", "crest-client")
                        .timeout(java.time.Duration.ofSeconds(60))
                        .GET()
                        .build();
                HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() != 200) throw new IOException("HTTP " + resp.statusCode());
                byte[] bytes;
                try (InputStream body = resp.body()) {
                    bytes = body.readAllBytes();
                }
                if (sha1 != null && !sha1.isEmpty()) {
                    String got = sha1(bytes);
                    if (!got.equalsIgnoreCase(sha1)) {
                        status = "Hash mismatch for " + p.title + " — aborted";
                        p.installed = Files.exists(target);
                        return;
                    }
                }
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(target.toFile())) {
                    fos.write(bytes);
                }
                enablePack(p.slug);
                status = "Installed & enabled: " + p.title;
                p.installed = true;
            } catch (Exception e) {
                status = "Failed: " + (e.getMessage() != null ? e.getMessage() : e.toString());
                p.installed = false;
            }
        });
    }

    private String fetchDownloadUrl(String projectId) throws Exception {
        return fetchVersionField(projectId, false);
    }

    private String fetchSha1(String projectId) throws Exception {
        return fetchVersionField(projectId, true);
    }

    private String fetchVersionField(String projectId, boolean sha) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(MODRINTH_VERSIONS + projectId + "/version"))
                .header("User-Agent", "crest-client")
                .timeout(java.time.Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new IOException("HTTP " + resp.statusCode());
        JsonArray arr = JsonParser.parseString(resp.body()).getAsJsonArray();
        if (arr.isEmpty()) throw new IOException("No versions");
        JsonObject v = arr.get(0).getAsJsonObject();
        JsonArray files = v.getAsJsonArray("files");
        if (files.isEmpty()) throw new IOException("No files");
        JsonObject f = files.get(0).getAsJsonObject();
        if (sha) {
            JsonObject hashes = f.getAsJsonObject("hashes");
            return hashes.has("sha1") ? hashes.get("sha1").getAsString() : null;
        }
        return f.get("url").getAsString();
    }

    private void enablePack(String slug) {
        Minecraft mc = Minecraft.getInstance();
        PackRepository repo = mc.getResourcePackRepository();
        repo.reload();
        List<String> ids = repo.getSelectedIds().stream().collect(Collectors.toList());
        if (!ids.contains(slug)) ids.add(slug);
        repo.setSelected(ids);
        mc.options.updateResourcePacks(repo);
        mc.reloadResourcePacks();
    }

    private static String sha1(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] dig = md.digest(data);
            Formatter fmt = new Formatter();
            for (byte b : dig) fmt.format("%02x", b);
            String s = fmt.toString();
            fmt.close();
            return s;
        } catch (Exception e) {
            return "";
        }
    }

    // ---- Render ----

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme.tick(delta);
        cursorBlink++;

        g.fill(0, 0, width, height, ColorUtil.withAlpha(Theme.OVERLAY, 230));
        Panel.drawGlass(g, panelX, panelY, panelW, panelH, ColorUtil.withAlpha(Theme.GLASS_BG, 240), Theme.getAnimatedAccent());

        int cx = panelX + panelW / 2;
        int left = panelX + 20;
        int right = panelX + panelW - 20;
        int cw = right - left;

        g.centeredText(font, Component.literal("Resource Pack Browser"), cx, panelY + 14, Theme.FOREGROUND);

        // Back
        String back = "< Back";
        int backW = font.width(back) + 12;
        boolean backHover = mx >= left && mx <= left + backW && my >= panelY + 6 && my <= panelY + 22;
        Panel.draw(g, left, panelY + 6, backW, 16, ColorUtil.withAlpha(backHover ? Theme.getAnimatedAccent() : 0, 120));
        g.text(font, Component.literal(back), left + 6, panelY + 8, Theme.FOREGROUND);

        // Search field
        int fieldY = panelY + 44;
        boolean fieldHover = mx >= left && mx <= right && my >= fieldY && my <= fieldY + 30;
        Panel.draw(g, left, fieldY, cw, 30, ColorUtil.withAlpha(searchFocused || fieldHover ? Theme.getAnimatedAccent() : Theme.BACKGROUND, searchFocused ? 40 : 120));
        Panel.drawHollowRect(g, left, fieldY, cw, 30, searchFocused ? Theme.getAnimatedAccent() : Theme.BORDER_LIGHT);
        String shown = query.isEmpty() ? "Search resource packs..." : query;
        g.text(font, Component.literal(shown), left + 10, fieldY + 10, query.isEmpty() ? Theme.MUTED_FOREGROUND : Theme.FOREGROUND);
        if (searchFocused && (cursorBlink / 10) % 2 == 0) {
            int tx = left + 10 + font.width(query);
            g.fill(tx, fieldY + 6, tx + 1, fieldY + 24, Theme.getAnimatedAccent());
        }

        // Search button (right of field)
        int btnW = 90;
        int sBtnX = right - btnW;
        boolean sBtnHover = mx >= sBtnX && mx <= sBtnX + btnW && my >= fieldY && my <= fieldY + 30;
        Panel.draw(g, sBtnX, fieldY, btnW, 30, sBtnHover ? ColorUtil.withAlpha(Theme.getAnimatedAccent(), 50) : ColorUtil.withAlpha(Theme.BACKGROUND, 130));
        Panel.drawHollowRect(g, sBtnX, fieldY, btnW, 30, sBtnHover ? Theme.getAnimatedAccent() : Theme.BORDER_LIGHT);
        g.centeredText(font, Component.literal("Search"), sBtnX + btnW / 2, fieldY + 10, sBtnHover ? Theme.getAnimatedAccent() : Theme.FOREGROUND);

        // Status line
        if (!status.isEmpty()) {
            g.text(font, Component.literal(status), left, panelY + 64, Theme.MUTED_FOREGROUND);
        }

        // List area
        hoveredIndex = -1;
        if (state == State.LIST || state == State.FALLBACK) {
            renderList(g, mx, my, delta);
        } else if (state == State.LOADING) {
            g.centeredText(font, Component.literal(status), cx, listY + listH / 2, Theme.MUTED_FOREGROUND);
        } else {
            g.centeredText(font, Component.literal(status), cx, listY + listH / 2, Theme.DESTRUCTIVE);
        }
    }

    private void renderList(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int total = packs.size();
        int rowsPerView = Math.max(1, listH / (rowH + rowGap));
        int frameMax = Math.max(0, total - rowsPerView);
        maxScroll = frameMax;
        scrollTarget = Anim.clamp(scrollTarget, 0, maxScroll);
        scrollOffset += (scrollTarget - scrollOffset) * 0.35f;
        if (Math.abs(scrollOffset - scrollTarget) < 0.01f) scrollOffset = scrollTarget;

        g.enableScissor(listX, listY, listX + listW, listY + listH);
        int baseY = listY - (int) (scrollOffset * (rowH + rowGap));
        for (int i = 0; i < total; i++) {
            int cy = baseY + i * (rowH + rowGap);
            if (cy > listY + listH + rowH) break;
            if (cy + rowH < listY) continue;
            renderRow(g, packs.get(i), i, listX, cy, listW, rowH, mx, my);
        }
        g.disableScissor();

        if (maxScroll > 0) {
            float thumbH = (float) rowsPerView / total * listH;
            float thumbY = (scrollOffset / maxScroll) * (listH - thumbH);
            int tx = listX + listW + 2;
            int ty = listY + (int) thumbY;
            int th = (int) thumbH;
            g.fill(tx - 1, ty - 1, tx + 3, ty + th + 1, ColorUtil.withAlpha(Theme.GLASS_BG, 180));
            g.fill(tx, ty, tx + 2, ty + th, Theme.getAnimatedAccent());
        }
    }

    private void renderRow(GuiGraphicsExtractor g, PackEntry p, int idx, int x, int y, int w, int h, int mx, int my) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        if (hover) hoveredIndex = idx;

        int top = ColorUtil.withAlpha(0xFFFFFFFF, hover ? 32 : 21);
        int bot = ColorUtil.withAlpha(0xFFFFFFFF, hover ? 5 : 2);
        g.fillGradient(x, y, x + w, y + h, top, bot);
        Panel.drawHollowRect(g, x, y, w, h, hover ? Theme.getAnimatedAccent() : Theme.BORDER_LIGHT);

        int textX = x + 14;
        // Title
        String title = p.title;
        if (font.width(title) > w - 220) title = font.plainSubstrByWidth(title, w - 224) + "…";
        g.text(font, Component.literal(title), textX, y + 10, Theme.FOREGROUND);

        // Author + downloads
        String meta = (p.author.isEmpty() ? "" : "by " + p.author + "  ·  ")
                + formatDownloads(p.downloads) + " downloads";
        g.text(font, Component.literal(meta), textX, y + 28, Theme.MUTED_FOREGROUND);

        // Description
        String desc = p.description;
        if (!desc.isEmpty()) {
            if (font.width(desc) > w - 220) desc = font.plainSubstrByWidth(desc, w - 224) + "…";
            g.text(font, Component.literal(desc), textX, y + 44, ColorUtil.withAlpha(Theme.MUTED_FOREGROUND, 200));
        }

        // Install / Installed button (right side)
        int btnW = 110;
        int btnH = 30;
        int btnX = x + w - btnW - 14;
        int btnY = y + (h - btnH) / 2;
        boolean btnHover = mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH;
        boolean installing = status.startsWith("Downloading");
        String label = p.installed ? "Installed" : (installing ? "..." : "Install");
        int btnCol = p.installed ? Theme.MUTED_FOREGROUND
                : (btnHover ? Theme.getAnimatedAccent() : Theme.FOREGROUND);
        Panel.draw(g, btnX, btnY, btnW, btnH, p.installed ? ColorUtil.withAlpha(Theme.BORDER_LIGHT, 60)
                : (btnHover ? ColorUtil.withAlpha(Theme.getAnimatedAccent(), 50) : ColorUtil.withAlpha(Theme.BACKGROUND, 130)));
        Panel.drawHollowRect(g, btnX, btnY, btnW, btnH, p.installed ? Theme.BORDER_LIGHT : (btnHover ? Theme.getAnimatedAccent() : Theme.BORDER_LIGHT));
        g.centeredText(font, Component.literal(label), btnX + btnW / 2, btnY + 10, btnCol);
    }

    private static String formatDownloads(long d) {
        if (d >= 1_000_000) return String.format("%.1fM", d / 1_000_000f);
        if (d >= 1_000) return String.format("%.1fk", d / 1_000f);
        return Long.toString(d);
    }

    // ---- Input ----

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int left = panelX + 20;
        int right = panelX + panelW - 20;
        int cw = right - left;
        int fieldY = panelY + 44;

        String back = "< Back";
        int backW = font.width(back) + 12;
        if (mx >= left && mx <= left + backW && my >= panelY + 6 && my <= panelY + 22) {
            onClose();
            return true;
        }

        // Search button
        int btnW = 90;
        int sBtnX = right - btnW;
        if (mx >= sBtnX && mx <= sBtnX + btnW && my >= fieldY && my <= fieldY + 30) {
            startFetch(query.trim());
            return true;
        }

        searchFocused = mx >= left && mx <= right && my >= fieldY && my <= fieldY + 30;

        // Rows
        if ((state == State.LIST || state == State.FALLBACK) && !loading) {
            int baseY = listY - (int) (scrollOffset * (rowH + rowGap));
            for (int i = 0; i < packs.size(); i++) {
                int cy = baseY + i * (rowH + rowGap);
                if (cy > listY + listH + rowH) break;
                if (cy + rowH < listY) continue;
                if (mx >= listX && mx <= listX + listW && my >= cy && my <= cy + rowH) {
                    int bW = 110, bH = 30;
                    int bX = listX + listW - bW - 14;
                    int bY = cy + (rowH - bH) / 2;
                    PackEntry p = packs.get(i);
                    if (mx >= bX && mx <= bX + bW && my >= bY && my <= bY + bH) {
                        if (!p.installed) install(p);
                        return true;
                    }
                    // Click elsewhere on row also installs
                    if (!p.installed) install(p);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            if (maxScroll > 0) {
                scrollTarget = Anim.clamp(scrollTarget - (float) deltaY, 0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int cp = event.codepoint();
        if (cp < 32 || cp > 126) return super.charTyped(event);
        if (searchFocused) {
            query += event.codepointAsString();
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == 256) { onClose(); return true; }
        if (searchFocused) {
            if (key == 259 && !query.isEmpty()) { query = query.substring(0, query.length() - 1); return true; }
            if (key == 257 || key == 335) { startFetch(query.trim()); return true; }
        }
        if (key == 258) { searchFocused = !searchFocused; return true; } // TAB
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
