package com.crest.client.core.mixin;

import com.crest.client.core.StatePlayer;
import com.crest.client.core.replay.StatePlayerHolder;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Mixin(Minecraft.class)
public class ReplayTickMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void crest$replayTick(CallbackInfo ci) {
        if (!StatePlayerHolder.isPlaying()) return;
        StatePlayer sp = StatePlayerHolder.active;
        long elapsed = StatePlayerHolder.elapsedUs();
        var records = sp.pollUpTo(elapsed);
        Minecraft mc = Minecraft.getInstance();
        Connection conn = mc.getConnection() != null ? mc.getConnection().getConnection() : null;
        KeyboardHandler kb = mc.keyboardHandler;
        for (var rec : records) {
            try {
                switch (rec.type) {
                    case 1: // TYPE_PKT_IN
                        if (conn != null) {
                            var pkt = StatePlayer.decodeClientbound(rec.payload,
                                mc.getConnection().registryAccess());
                            if (pkt != null) invokeChannelRead(conn, pkt);
                        }
                        break;
                    case 4: // TYPE_KEY
                        if (kb != null) {
                            long hwnd = GLFW.glfwGetCurrentContext();
                            invokeKeyPress(kb, hwnd, bytesToInt(rec.payload, 0),
                                bytesToInt(rec.payload, 4), bytesToInt(rec.payload, 8), bytesToInt(rec.payload, 12));
                        }
                        break;
                    // ponytail: TYPE_MOUSE_MOVE replay skipped — MouseHandler fields
                        // changed in this MC version. Add when replay is actually tested.
                }
            } catch (Throwable ignored) {
            }
        }

        if (sp.eof()) StatePlayerHolder.stop();
    }

    private static void invokeChannelRead(Connection conn, net.minecraft.network.protocol.Packet<?> pkt) {
        try {
            Method m = Connection.class.getDeclaredMethod("channelRead0", ChannelHandlerContext.class, net.minecraft.network.protocol.Packet.class);
            m.setAccessible(true);
            m.invoke(conn, (ChannelHandlerContext) null, pkt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void invokeKeyPress(KeyboardHandler kb, long window, int key, int scancode, int action, int mods) {
        try {
            Method m = KeyboardHandler.class.getDeclaredMethod("keyPress", long.class, int.class, int.class, int.class, int.class);
            m.setAccessible(true);
            m.invoke(kb, window, key, scancode, action, mods);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int bytesToInt(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8) | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
    }

    private static float bytesToFloat(byte[] b, int off) {
        return Float.intBitsToFloat(bytesToInt(b, off));
    }
}