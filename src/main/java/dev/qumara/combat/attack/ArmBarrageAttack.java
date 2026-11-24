package dev.qumara.combat.attack;

import dev.qumara.combat.DangerShape;
import dev.qumara.combat.DangerZone;
import dev.qumara.combat.Scale;
import dev.qumara.combat.Shockwave;
import dev.qumara.entity.QumaraEntity;
import dev.qumara.registry.ModSounds;
import dev.qumara.vfx.Vfx;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * MARTILLEO: cuatro puñetazos seguidos con el mismo brazo, cada uno sobre un circulo distinto que aparece 0.8 s antes. El ultimo es
 * mas grande y suelta una onda de choque. Puntos y tiempos = BARRAGE en tools/anims.py (la palma llega exacta a cada circulo).
 */
public class ArmBarrageAttack extends BossAttack {
    public static final float[] TICK = {30f, 46f, 62f, 78f};
    public static final double[][] PT = {{6.0, 2.0}, {8.5, -2.5}, {8.5, 4.0}, {11.5, 0.5}};      // bloques al frente, a la derecha
    public static final float WARN = 16f, TOTAL = 104f;
    private final DangerZone[] zones = new DangerZone[4];
    private final Vec3d[] centers = new Vec3d[4];

    @Override public String animation() { return "barrage"; }
    @Override public String tongueAnimation() { return "lengua_rugir"; }
    @Override public float total() { return TOTAL; }

    private static float radius(int i) { return (i == 3 ? 2.4f : 1.5f) * Scale.KF; }

    @Override
    public void start(QumaraEntity b, LivingEntity target) {
        b.sound(ModSounds.ARM_WHOOSH, 3.5f, 0.6f);
        b.sound(ModSounds.WARNING, 3.0f, 0.7f);
    }

    @Override
    public void tick(QumaraEntity b, float prev, float now) {
        for (int i = 0; i < 4; i++) {
            if (crossed(prev, now, TICK[i] - WARN)) {
                Vec3d c = b.getPos().add(b.forward().multiply(PT[i][0])).add(b.right().multiply(PT[i][1]));
                double y = b.groundY(c.x, c.z); centers[i] = new Vec3d(c.x, y, c.z);
                zones[i] = show(b, new DangerShape.Circle(c.x, c.z, radius(i)), y, WARN, 6f, DangerZone.SLAM);
                b.sound(ModSounds.WARNING, 2.0f, 0.9f + 0.1f * i);
            }
            if (crossed(prev, now, TICK[i] - 10f)) b.sound(ModSounds.ARM_WHOOSH, 3.0f, 0.7f + 0.1f * i);
            if (crossed(prev, now, TICK[i]) && zones[i] != null) impact(b, i);
        }
    }

    private void impact(QumaraEntity b, int i) {
        Vec3d c = centers[i]; boolean last = i == 3;
        for (ServerPlayerEntity p : b.zones().playersIn(zones[i])) {
            b.hurtPlayer(p, last ? 20f : 11f);
            Vec3d away = p.getPos().subtract(c).multiply(1, 0, 1);
            away = away.lengthSquared() < 1e-4 ? b.forward() : away.normalize();
            p.setVelocity(away.x * (last ? 1.0 : 0.7), last ? 0.85 : 0.6, away.z * (last ? 1.0 : 0.7)); p.velocityModified = true;
        }
        Vfx.slamImpact(b.sw(), c);
        b.sound(ModSounds.IMPACT, last ? 5.0f : 3.5f, last ? 0.7f : 1.0f - 0.05f * i); b.heavy(last ? 5.0f : 3.5f, last ? 0.7f : 1.0f);
        b.sound(ModSounds.ROOT_BURST, 2.5f, 0.8f + 0.1f * i);
        b.shake(last ? 22 : 10, last ? 0.8f : 0.4f);
        b.addLingering(new Shockwave(c, radius(i), (last ? 6.5 : 3.5) * Scale.K, b.ticks(last ? 14f : 8f), last ? 9f : 4f));
        if (last) Vfx.rootEruption(b.sw(), c);
    }
}
