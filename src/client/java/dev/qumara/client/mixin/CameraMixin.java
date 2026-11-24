package dev.qumara.client.mixin;

import dev.qumara.client.cinematic.IntroCamera;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sustituye la posicion/rotacion de la camara por la de la cinematica de aparicion (solo mientras dura). */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"), require = 0)
    private void qumara$cinematic(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        IntroCamera.Pose p = IntroCamera.pose(MinecraftClient.getInstance(), tickDelta);
        if (p != null) { setRotation(p.yaw(), p.pitch()); setPos(p.x(), p.y(), p.z()); }
    }
}
