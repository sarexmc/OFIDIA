package dev.qumara.client.cinematic;

import dev.qumara.entity.QumaraEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * CINEMATICA DE APARICION con camara propia (solo visual, local: no mueve al jugador). Se activa sola mientras la entidad cercana
 * reproduce la animacion "spawn" y el tiempo sale de e.age (cuenta desde que Qumara aparece), asi todos los jugadores ven los mismos planos
 * a la vez sin enviar paquetes. 5 planos que siguen el guion de la animacion (tools/anims.py, 'spawn'):
 *   1  0.0-3.5 s  grietas en el suelo: plano bajo con travelling hacia delante
 *   2  3.5-6.0 s  la planta emerge: grua ascendente desde su derecha
 *   3  6.0-8.0 s  contrapicado sobre la cabeza mientras el capullo se abre
 *   4  8.0-8.7 s  el golpe de presentacion: plano lateral abierto
 *   5  8.7-10.2 s rugido: retroceso lento desde la cara
 * Distancias en bloques relativas a Qumara (f = frente, r = su derecha, u = arriba). Ajusta SHOTS para cambiar planos.
 */
public final class IntroCamera {
    public static final float DURATION = 10.2f;
    public record Pose(double x, double y, double z, float yaw, float pitch) {}

    // t0, t1, posA(f,r,u), posB, lookA(f,r,u), lookB
    private static final double[][] SHOTS = {
        {0.0, 3.5,   34, 6, 2.5,   26, 3, 1.5,     0, 0, 3,    0, 0, 2},
        {3.5, 6.0,   16, 22, 2.0,  20, 17, 14.0,   0, 0, 6,    0, 0, 10},
        {6.0, 8.0,   16, -5, 3.0,  11, -2, 6.0,    0, 0, 14,   0, 0, 17},
        {8.0, 8.7,   9, -30, 7.0,  10, -26, 5.0,   11, 1.8, 0.5, 11, 1.8, 0.5},
        {8.7, DURATION, 14, 2, 15.0, 26, 4, 12.0,  0, 0, 15,   0, 0, 14},
    };

    private static QumaraEntity find(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return null;
        QumaraEntity best = null; double bd = 96 * 96;
        for (var e : mc.world.getEntities()) {
            if (e instanceof QumaraEntity q && "spawn".equals(q.mainAnim())) {
                double d = q.squaredDistanceTo(mc.player);
                if (d < bd) { bd = d; best = q; }
            }
        }
        return best;
    }

    private static float time(QumaraEntity q, float tickDelta) { return (q.age + tickDelta) / 20f; }

    private static Vec3d at(Vec3d c, Vec3d f, Vec3d r, double[] v, int o) { return c.add(f.multiply(v[o])).add(r.multiply(v[o + 1])).add(0, v[o + 2], 0); }

    /** Pose de camara o null si no hay cinematica activa. */
    public static Pose pose(MinecraftClient mc, float tickDelta) {
        QumaraEntity q = find(mc);
        if (q == null) return null;
        float t = time(q, tickDelta);
        if (t > DURATION) return null;
        double[] s = SHOTS[SHOTS.length - 1];
        for (double[] sh : SHOTS) if (t < sh[1]) { s = sh; break; }
        double u = MathHelper.clamp((t - s[0]) / (s[1] - s[0]), 0.0, 1.0);
        double e = u * u * (3 - 2 * u);                                  // ease in-out dentro de cada plano
        Vec3d c = q.getLerpedPos(tickDelta);
        double yaw = Math.toRadians(MathHelper.lerpAngleDegrees(tickDelta, q.prevBodyYaw, q.bodyYaw));
        Vec3d f = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw)), r = new Vec3d(-f.z, 0, f.x);
        Vec3d pa = at(c, f, r, s, 2), pb = at(c, f, r, s, 5), la = at(c, f, r, s, 8), lb = at(c, f, r, s, 11);
        Vec3d p = pa.lerp(pb, e), look = la.lerp(lb, e);
        // pulso de camara en mano (muy leve) + temblor en el plano 4
        double n = (s[0] == 8.0 ? 0.25 : 0.04);
        p = p.add(Math.sin(t * 2.3) * n, Math.sin(t * 3.1 + 1) * n * 0.6, Math.cos(t * 2.7) * n);
        double dx = look.x - p.x, dy = look.y - p.y, dz = look.z - p.z;
        float cyaw = (float) (Math.atan2(-dx, dz) * 57.29577951308232), cpitch = (float) -(Math.atan2(dy, Math.hypot(dx, dz)) * 57.29577951308232);
        return new Pose(p.x, p.y, p.z, cyaw, cpitch);
    }

    /** Barras de cine que entran y salen suavemente. */
    public static void bars(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        QumaraEntity q = find(mc);
        if (q == null) return;
        float t = time(q, tickDelta);
        if (t > DURATION + 0.5f) return;
        float a = MathHelper.clamp(Math.min(t / 0.6f, (DURATION + 0.5f - t) / 0.6f), 0f, 1f);
        a = a * a * (3 - 2 * a);
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight(), bar = (int) (h * 0.12f * a);
        ctx.fill(0, 0, w, bar, 0xFF000000);
        ctx.fill(0, h - bar, w, h, 0xFF000000);
    }

    private IntroCamera() {}
}
