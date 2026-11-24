package dev.qumara.combat.attack;

import dev.qumara.combat.DangerShape;
import dev.qumara.combat.DangerZone;
import dev.qumara.combat.RootedManager;
import dev.qumara.combat.Scale;
import dev.qumara.entity.QumaraEntity;
import dev.qumara.registry.ModSounds;
import dev.qumara.vfx.Vfx;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * JAULA DE RAICES: marca un circulo bajo cada jugador (hasta 2 + fase, los mas cercanos); 0.9 s despues unas raices altas emergen formando
 * una jaula, inmovilizan a quien quedo dentro y, a los 1.7 s, se cierran sobre el: 14 de daño. Hay que salir del circulo mientras se marca.
 */
public class CageAttack extends BossAttack {
    public static final float MARK = 6f, CAGE = 26f, CLOSE = 60f, TOTAL = 80f;
    public static final float R = 3.8f;
    private record Cell(Vec3d at, DangerZone outer) {}
    private final List<Cell> cells = new ArrayList<>();
    private final List<DangerZone> inner = new ArrayList<>();

    @Override public String animation() { return "cage"; }
    @Override public String tongueAnimation() { return "lengua_rugir"; }
    @Override public float total() { return TOTAL; }

    @Override
    public void start(QumaraEntity b, LivingEntity target) {
        b.sound(ModSounds.ARM_WHOOSH, 3.0f, 0.6f);
        b.sound(ModSounds.PLANT_GROW, 3.0f, 0.6f);
    }

    @Override
    public void tick(QumaraEntity b, float prev, float now) {
        if (crossed(prev, now, MARK)) {
            List<ServerPlayerEntity> ps = new ArrayList<>(b.players());
            ps.sort(Comparator.comparingDouble(p -> p.squaredDistanceTo(b)));
            for (int i = 0; i < Math.min(ps.size(), 2 + b.phase()); i++) {
                Vec3d p = ps.get(i).getPos(); double y = b.groundY(p.x, p.z);
                cells.add(new Cell(new Vec3d(p.x, y, p.z), show(b, new DangerShape.Circle(p.x, p.z, R), y, CAGE - MARK, 6f, DangerZone.ROOTS)));
            }
            b.sound(ModSounds.WARNING, 3.0f, 0.8f);
        }
        if (crossed(prev, now, CAGE)) {
            for (Cell c : cells) {
                for (ServerPlayerEntity p : b.zones().playersIn(c.outer)) { b.hurtPlayer(p, 8f); RootedManager.root(p, b.ticks(CLOSE - CAGE) + 8); }
                for (int k = 0; k < 10; k++) {                                                    // la jaula: 10 raices altas en corona
                    double a = Math.PI * 2 * k / 10 + 0.3;
                    Vec3d at = c.at.add(Math.cos(a) * R * 0.9, 0, Math.sin(a) * R * 0.9);
                    b.plants().sprout(Blocks.MANGROVE_ROOTS.getDefaultState(), new Vec3d(at.x, b.groundY(at.x, at.z), at.z), 0.8f * Scale.KF, 2.6f * Scale.KF, 0, 0, 0f, k % 3, 6, b.ticks(CLOSE - CAGE), 8);
                }
                Vfx.rootEruption(b.sw(), c.at);
                inner.add(show(b, new DangerShape.Circle(c.at.x, c.at.z, R * 0.9f), c.at.y, CLOSE - CAGE, 8f, DangerZone.SLAM));
            }
            b.sound(ModSounds.ROOT_BURST, 4.0f, 0.7f); b.sound(ModSounds.SQUEEZE, 3.5f, 0.6f); b.shake(10, 0.4f);
        }
        if (crossed(prev, now, CLOSE)) {
            for (int i = 0; i < inner.size(); i++) {
                for (ServerPlayerEntity p : b.zones().playersIn(inner.get(i))) {
                    b.hurtPlayer(p, 14f); RootedManager.release(p);
                    p.setVelocity(0, 0.9, 0); p.velocityModified = true;
                }
                Vfx.slamImpact(b.sw(), cells.get(i).at);
            }
            b.sound(ModSounds.IMPACT, 4.0f, 0.8f); b.heavy(4.0f, 0.8f); b.sound(ModSounds.SQUEEZE, 4.0f, 0.5f); b.shake(16, 0.6f);
        }
    }
}
