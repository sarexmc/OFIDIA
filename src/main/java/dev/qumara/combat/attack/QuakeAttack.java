package dev.qumara.combat.attack;

import dev.qumara.combat.DangerShape;
import dev.qumara.combat.DangerZone;
import dev.qumara.combat.Scale;
import dev.qumara.combat.Shockwave;
import dev.qumara.entity.QumaraEntity;
import dev.qumara.registry.ModSounds;
import dev.qumara.vfx.Vfx;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

/**
 * PISOTON SISMICO: se yergue (preparacion larga y visible) y se estrella contra el suelo; del impacto salen 3 (fase 0), 4 (furia) o 5 (ira)
 * ondas de choque consecutivas que hay que SALTAR una a una (solo golpean a quien esta en el suelo). El anillo rojo marca el alcance.
 */
public class QuakeAttack extends BossAttack {
    public static final float SLAM = 31f, GAP = 10f, TOTAL = 96f;
    public static final double R0 = 2.5 * Scale.K, R1 = 9.0 * Scale.K;
    private int waves; private Vec3d c;

    @Override public String animation() { return "quake"; }
    @Override public String tongueAnimation() { return "lengua_rugir"; }
    @Override public float total() { return TOTAL; }

    @Override
    public void start(QumaraEntity b, LivingEntity target) {
        waves = 3 + b.phase();
        c = b.getPos(); c = new Vec3d(c.x, b.groundY(c.x, c.z), c.z);
        show(b, new DangerShape.Circle(c.x, c.z, (float) R1), c.y, SLAM, GAP * waves + 24f, DangerZone.RING);
        b.sound(ModSounds.ARM_WHOOSH, 3.5f, 0.5f);
        b.sound(ModSounds.PLANT_GROW, 3.5f, 0.5f);
    }

    @Override
    public void tick(QumaraEntity b, float prev, float now) {
        if (now > 6f && now < SLAM - 2f && (int) now % 4 == 0 && (int) prev != (int) now) { Vfx.burst(b.sw(), Vfx.DIRT, c.add(0, 0.3, 0), 6, 3.0 * Scale.K, 0.05); b.shake(4, 0.12f); }
        if (crossed(prev, now, SLAM - 8f)) b.sound(ModSounds.WARNING, 3.5f, 0.8f);
        for (int i = 0; i < waves; i++) {
            float at = SLAM + GAP * i;
            if (crossed(prev, now, at)) {
                if (i == 0) {
                    Vfx.slamImpact(b.sw(), c.add(b.forward().multiply(3.0 * Scale.K)));
                    b.sound(ModSounds.IMPACT, 6.0f, 0.6f); b.heavy(6.0f, 0.6f); b.sound(ModSounds.EARTH_CRACK, 5.0f, 0.7f);
                    b.shake(30, 0.9f);
                } else {
                    b.sound(ModSounds.IMPACT, 4.0f, 0.8f + 0.05f * i); b.heavy(4.0f, 0.8f); b.sound(ModSounds.ROOT_BURST, 3.0f, 0.7f);
                    b.shake(14, 0.5f);
                }
                b.addLingering(new Shockwave(c, R0, R1, b.ticks(24f), 9f + 2f * b.phase()));
            }
        }
    }
}
