package com.crest.client.core.mixin;

import com.google.common.net.InetAddresses;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddressResolver;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.function.Function;

/**
 * Built-in "fast ping": for servers whose address is a literal IP, pin the
 * InetSocketAddress hostName to the IP so later getHostName() calls skip the
 * slow reverse-DNS lookup (getHostFromNameService). Cuts 1-5s off server-list
 * ping and connecting for IP-only servers. Client-side only, not a module.
 */
@Mixin(ServerNameResolver.class)
public abstract class FastPingMixin {

    private static final Function<ResolvedServerAddress, ResolvedServerAddress> WRAP = FastPingMixin::pinHostName;

    @Redirect(
            method = "resolveAddress",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/resolver/ServerAddressResolver;resolve(Lnet/minecraft/client/multiplayer/resolver/ServerAddress;)Ljava/util/Optional;"
            )
    )
    private Optional<ResolvedServerAddress> crest$resolve(ServerAddressResolver instance, ServerAddress address) {
        return instance.resolve(address).map(WRAP);
    }

    private static ResolvedServerAddress pinHostName(ResolvedServerAddress resolved) {
        InetSocketAddress sock = resolved.asInetSocketAddress();
        if (sock.isUnresolved()) {
            return resolved;
        }
        InetAddress inet = sock.getAddress();
        if (inet == null) {
            return resolved;
        }
        // getHostAddress does NOT trigger reverse DNS; only literal IPs get pinned.
        String ip = inet.getHostAddress();
        if (!InetAddresses.isInetAddress(ip)) {
            return resolved;
        }
        try {
            InetSocketAddress pinned = new InetSocketAddress(
                    InetAddress.getByAddress(ip, inet.getAddress()), sock.getPort());
            return ResolvedServerAddress.from(pinned);
        } catch (UnknownHostException e) {
            return resolved;
        }
    }
}
