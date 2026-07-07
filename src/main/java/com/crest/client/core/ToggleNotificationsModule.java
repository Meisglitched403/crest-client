package com.crest.client.core;

import com.crest.client.core.event.ModuleToggleEvent;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ToggleNotificationsModule implements CrestModule, RenderableModule {
    private static final long NOTIFICATION_TIME = 3000;
    private final LinkedList<Notification> notifications = new LinkedList<>();
    private final IntegerSetting duration = new IntegerSetting("Duration (s)", 1, 10, 3);

    @Override
    public String getId() { return "toggle_notifications"; }
    @Override
    public String getName() { return "Toggle Notifications"; }
    @Override
    public String getDescription() { return "Shows a toast when modules are toggled"; }
    @Override
    public String getCategory() { return "HUD"; }
    @Override
    public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(duration);
    }

    @Override
    public void onInitialize() {
        CrestModules.getEventBus().subscribe(ModuleToggleEvent.class, this::onModuleToggle);
    }

    private void onModuleToggle(ModuleToggleEvent event) {
        if (!CrestModules.isEnabled(getId())) return;
        String name = event.getModule().getName();
        String state = event.isEnabled() ? "§aON" : "§cOFF";
        notifications.addLast(new Notification(name + " " + state, System.currentTimeMillis()));
        if (notifications.size() > 5) notifications.removeFirst();
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        long now = System.currentTimeMillis();
        long timeoutMs = duration.get() * 1000L;

        Iterator<Notification> it = notifications.iterator();
        int y = 10;
        while (it.hasNext()) {
            Notification n = it.next();
            long age = now - n.timestamp;
            if (age > timeoutMs) {
                it.remove();
                continue;
            }
            float alpha = Math.min(1f, (timeoutMs - age) / 500f);
            int color = ((int)(alpha * 200) << 24) | 0xFFFFFF;

            int w = mc.font.width(n.text);
            int x = (mc.getWindow().getGuiScaledWidth() - w) / 2 - 4;

            g.fill(x - 2, y - 2, x + w + 2, y + mc.font.lineHeight + 2, 0x66000000);
            g.text(mc.font, Component.literal(n.text), x, y, color);
            y += mc.font.lineHeight + 4;
        }
    }

    private record Notification(String text, long timestamp) {}
}
