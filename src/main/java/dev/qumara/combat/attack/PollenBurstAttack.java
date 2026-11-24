package dev.qumara.combat.attack;

import dev.qumara.combat.DangerShape;
import dev.qumara.combat.DangerZone;
import dev.qumara.combat.PollenCloud;
import dev.qumara.combat.Scale;
import dev.qumara.entity.QumaraEntity;
import dev.qumara.registry.ModSounds;
import dev.qumara.vfx.Vfx;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

/**
 * POLEN CEGADOR: se encoge y estalla en una nube de polen que ciega y ralentiza (poco daño). En furia/ira encadena un MARTILLEO: hay que
 * salir de la nube o pelear a ciegas. El anillo rojo es el alcance maximo de la nube.
 */
public class PollenBurstAttack extends BossAttack {
    public static final float BURST = 24f, TOTAL = 68f;
    public static final double R = 6.0 * Scale.K;

    @Override public String animation() { return "pollen"; }
    @Override public float total() { return TOTAL; }

    @Override
    public void start(QumaraEntity b, LivingEntity target) {
        Vec3d c = b.getPos();
        show(b, new DangerShape.Circle(c.x, c.z, (float) R), b.groundY(c.x, c.z), BURST, 36f, DangerZone.RING);
        b.sound(ModSounds.WARNING, 3.0f, 0.7f); b.sound(ModSounds.SPAWN_RUMBLE, 3.5f, 1.1f);
    }

    @Override
    public void tick(QumaraEntity b, float prev, float now) {
        if (now > 4f && now < BURST && (int) now % 3 == 0) Vfx.burst(b.sw(), Vfx.POLLEN_DUST, b.getPos().add(0, Scale.MOUTH_Y * 0.6, 0), 8, 3.0, 0.02);
        if (crossed(prev, now, BURST)) {
            Vec3d c = b.getPos();
            b.addLingering(new PollenCloud(new Vec3d(c.x, b.groundY(c.x, c.z), c.z), 3.0 * Scale.K, R, b.ticks(36f), b.ticks(120f)));
            Vfx.burst(b.sw(), Vfx.POLLEN_DUST, c.add(0, 4, 0), 60, 6.0, 0.2);
            b.sound(ModSounds.FURY, 4.5f, 1.4f); b.sound(ModSounds.GARDEN_BLOOM, 4.0f, 0.5f); b.sound(ModSounds.PLANT_GROW, 4.0f, 0.5f);
            b.shake(16, 0.4f);
        }
    }

    @Override
    public BossAttack next(QumaraEntity b) { return (b.fury() && b.rng().nextFloat() < 0.7f) ? new ArmBarrageAttack() : null; }
}
