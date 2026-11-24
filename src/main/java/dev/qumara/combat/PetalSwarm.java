package dev.qumara.combat;

import dev.qumara.entity.QumaraEntity;
import dev.qumara.vfx.Vfx;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Petalos cortantes en vuelo (tormenta de petalos). Se esquivan saltando: vuelan a ~1 bloque del suelo. */
public class PetalSwarm implements Lingering {
    private static final class Petal { Vec3d pos, vel; int life; }
    private final List<Petal> petals = new ArrayList<>();
    private boolean closed;
    public static final float DAMAGE = 8f; public static final double HIT_R = 1.2;

    public void add(Vec3d pos, Vec3d vel, int life) { Petal p = new Petal(); p.pos = pos; p.vel = vel; p.life = life; petals.add(p); }
    public void close() { closed = true; }

    @Override
    public boolean tick(QumaraEntity b) {
        for (int i = petals.size() - 1; i >= 0; i--) {
            Petal p = petals.get(i);
            p.pos = p.pos.add(p.vel); p.life--;
            boolean dead = p.life <= 0;
            if (p.life % 2 == 0) b.sw().spawnParticles(ParticleTypes.CHERRY_LEAVES, p.pos.x, p.pos.y, p.pos.z, 1, 0.1, 0.1, 0.1, 0.0);
            if (p.life % 3 == 0) b.sw().spawnParticles(Vfx.RED_DUST, p.pos.x, p.pos.y, p.pos.z, 1, 0.05, 0.05, 0.05, 0.0);
            if (!dead) {
                for (ServerPlayerEntity pl : b.players()) {
                    if (Math.hypot(pl.getX() - p.pos.x, pl.getZ() - p.pos.z) > HIT_R) continue;
                    if (pl.getY() > p.pos.y + 0.4 || pl.getY() + 1.8 < p.pos.y - 0.2) continue;       // saltar por encima / agacharse no
                    b.hurtPlayer(pl, DAMAGE);
                    Vec3d push = p.vel.normalize();
                    pl.setVelocity(push.x * 0.7, 0.35, push.z * 0.7); pl.velocityModified = true;
                    Vfx.burst(b.sw(), Vfx.RED_DUST, p.pos, 8, 0.6, 0.1);
                    dead = true; break;
                }
            }
            if (dead) petals.remove(i);
        }
        return closed && petals.isEmpty();
    }
}
