package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.ColorSetting;
import com.crest.client.core.setting.Setting;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Shield Status — ports Walksy/ShieldStatus behaviour: tints rendered shields
 * (in-hand, on-back and in GUI) by their state using a colour overlay that does
 * not replace the shield texture, so custom resource-pack shields still show.
 *
 * States (matching the reference):
 *   - Disabled  : shield on cooldown            -> disabled colour (red)
 *   - Rising    : holding shield, using item, not yet blocking, not cooling -> rising colour (orange)
 *   - Using     : actively blocking             -> using colour (green)
 *   - Enabled   : otherwise                     -> enabled colour (green)
 * With "Interpolate" on, the colour lerps green->red by cooldown progress.
 */
public class ShieldStatusModule implements CrestModule {

    private static final Identifier SHIELD_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/shield/shield_base_nopattern.png");
    private static final int COLOR_WHITE = 0xFFFFFFFF;

    private final BooleanSetting enableInterpolation = new BooleanSetting("Interpolate Color", false);
    private final BooleanSetting selfStateOnly = new BooleanSetting("Self State Only", false);
    private final BooleanSetting grayscaleTexture = new BooleanSetting("Grayscale Texture", false);

    private final BooleanSetting customEnabled = new BooleanSetting("Custom Enabled Color", true);
    private final ColorSetting enabledColor = new ColorSetting("Enabled Color", 0xFF00FF00);
    private final BooleanSetting customUsing = new BooleanSetting("Custom Using Color", false);
    private final ColorSetting usingColor = new ColorSetting("Using Color", 0xFF00FF00);
    private final BooleanSetting customRising = new BooleanSetting("Custom Rising Color", false);
    private final ColorSetting risingColor = new ColorSetting("Rising Color", 0xFFFFFF00);
    private final BooleanSetting customDisabled = new BooleanSetting("Custom Disabled Color", true);
    private final ColorSetting disabledColor = new ColorSetting("Disabled Color", 0xFFFF0000);

    @Override public String getId() { return "shield_status"; }
    @Override public String getName() { return "Shield Status"; }
    @Override public String getDescription() {
        return "Tints shields by state (enabled/using/rising/disabled) with a colour overlay";
    }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(
                enableInterpolation, selfStateOnly, grayscaleTexture,
                customEnabled, enabledColor,
                customUsing, usingColor,
                customRising, risingColor,
                customDisabled, disabledColor
        );
    }

    public static boolean isActive() {
        CrestModule m = CrestModules.get("shield_status");
        return m instanceof ShieldStatusModule && CrestModules.isEnabled("shield_status");
    }

    public static boolean isGrayscale() { return get().grayscaleTexture.get(); }

    public static boolean isSelfStateOnly() { return get().selfStateOnly.get(); }

    /** Colour for the given player's shield (white = unchanged). */
    public static int getColor(Player player) {
        ShieldStatusModule m = get();
        if (player == null) return COLOR_WHITE;
        if (isSelfStateOnly() && player != Minecraft.getInstance().player) return COLOR_WHITE;

        boolean cd = ShieldStateUtil.isCoolingDown(player);
        boolean active = ShieldStateUtil.isUsingShield(player);
        boolean rising = ShieldStateUtil.isShieldRising(player);

        int curEnabled = m.customEnabled.get() ? m.enabledColor.get() : COLOR_WHITE;
        int curDisabled = m.customDisabled.get() ? m.disabledColor.get() : COLOR_WHITE;
        int curUsing = m.customUsing.get() ? m.usingColor.get() : COLOR_WHITE;
        int curRising = m.customRising.get() ? m.risingColor.get() : COLOR_WHITE;

        if (rising && m.customRising.get()) return curRising;
        if (active && m.customUsing.get()) return curUsing;

        if (!m.enableInterpolation.get()) {
            return cd ? curDisabled : curEnabled;
        }

        float progress = cd ? ShieldStateUtil.getCooldownProgress(player) : 0.0f;
        return lerpColor(curEnabled, curDisabled, progress);
    }

    /** Texture identifier used to render the shield for the given player. */
    public static Identifier getTexture(Player player) {
        if (isSelfStateOnly() && player != Minecraft.getInstance().player) {
            return SHIELD_TEXTURE;
        }
        return ShieldStateUtil.isCoolingDown(player) ? SHIELD_TEXTURE : SHIELD_TEXTURE;
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF, aa = (a >> 24) & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF, ba = (b >> 24) & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        int al = (int) (aa + (ba - aa) * t);
        return (al << 24) | (r << 16) | (g << 8) | bl;
    }

    private static ShieldStatusModule get() {
        CrestModule m = CrestModules.get("shield_status");
        return m instanceof ShieldStatusModule ssm ? ssm : new ShieldStatusModule();
    }
}
