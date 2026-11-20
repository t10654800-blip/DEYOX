package com.example.modkick;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

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
        final Vec3d pos;
        final float yaw, pitch;
        int ticksRestantes = TICKS_ANTES_DE_KICK;

        Congelado(ServerPlayerEntity p) {
            this.pos = p.getPos();
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
                } else if (p.getPos().squaredDistanceTo(c.pos) > 0.0001) {
                    p.networkHandler.requestTeleport(c.pos.x, c.pos.y, c.pos.z, c.yaw, c.pitch);
                }
            }

            // Kickeamos fuera del bucle para no modificar el mapa mientras se recorre
            for (ServerPlayNetworkHandler h : aKickear) {
                congelados.remove(h.getPlayer().getUuid());
                h.disconnect(Text.literal(
                    "§cNo puedes entrar.\n§7Te falta el mod obligatorio: §e" + MOD));
            }
        });
    }
}
