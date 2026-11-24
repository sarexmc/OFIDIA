package dev.qumara.combat.attack;

import dev.qumara.combat.DangerShape;
import dev.qumara.combat.DangerZone;
import dev.qumara.combat.PetalSwarm;
import dev.qumara.combat.Scale;
import dev.qumara.entity.QumaraEntity;
import dev.qumara.registry.ModSounds;
import dev.qumara.vfx.Vfx;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

/**
 * TORMENTA DE PETALOS (furia+): abre los petales y gira sobre si misma lanzando petales cortantes en espiral (3, 4 o 5 brazos segun la fase).
 * Vuelan a ~1 bloque del suelo: se esquivan SALTANDO y buscando los huecos de la espiral. El anillo rojo marca su alcance.
 */
public class PetalStormAttack extends BossAttack {
    public static final float START = 24f, END = 80f, TOTAL = 96f;
    public static final double RANGE = 12.0 * Scale.K, SPEED = 0.7;
    private PetalSwarm swarm; private int arms;

    @Override public String animation() { return "petal_storm"; }
    @Override public String tongueAnimation() { return "lengua_rugir"; }
    @Override public float total() { return TOTAL; }

    /** Giro del cuerpo en grados en el tick 'now' (misma curva que 'petal_storm' en tools/anims.py). */
    private static double spin(float now) {
        double u = Math.max(0, Math.min(1, (now - START) / (END - START)));
        return 720.0 * (u * u * (3 - 2 * u) * 0.5 + u * 0.5);
    }

    @Override
    public void start(QumaraEntity b, LivingEntity target) {
        arms = 3 + b.phase();
        swarm = new PetalSwarm(); b.addLingering(swarm);
        Vec3d c = b.getPos();
        show(b, new DangerShape.Circle(c.x, c.z, (float) RANGE), b.groundY(c.x, c.z), START, (END - START) + 30f, DangerZone.RING);
        b.sound(ModSounds.ARM_WHOOSH, 3.5f, 0.5f); b.sound(ModSounds.WARNING, 3.0f, 0.8f);
    }

    @Override
    public void tick(QumaraEntity b, float prev, float now) {
        if (now >= START && now <= END && (int) now % 2 == 0 && (int) prev != (int) now) {
            double th = Math.toRadians(spin(now));
            Vec3d left = b.right().multiply(-1);
            for (int j = 0; j < arms; j++) {
                double a = th + Math.PI * 2 * j / arms;
                Vec3d dir = b.forward().multiply(Math.cos(a)).add(left.multiply(Math.sin(a))).normalize();
                Vec3d from = b.getPos().add(dir.multiply(3.0 * Scale.K));
                double y = b.groundY(from.x, from.z) + 0.9;
                swarm.add(new Vec3d(from.x, y, from.z), dir.multiply(SPEED), (int) (RANGE / SPEED) + 4);
            }
            if ((int) now % 6 == 0) { b.sound(ModSounds.ARM_WHOOSH, 2.5f, 1.0f + b.rng().nextFloat() * 0.4f); b.shake(5, 0.15f); }
            Vfx.burst(b.sw(), Vfx.AZALEA, b.getPos().add(0, Scale.MOUTH_Y, 0), 6, 3.0, 0.2);
        }
        if (crossed(prev, now, START)) { b.sound(ModSounds.FURY, 4.0f, 1.2f); b.sound(ModSounds.GARDEN_BLOOM, 4.0f, 0.6f); }
    }

    @Override
    public void end(QumaraEntity b) { if (swarm != null) swarm.close(); super.end(b); }
}
