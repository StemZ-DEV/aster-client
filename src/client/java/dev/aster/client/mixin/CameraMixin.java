package dev.aster.client.mixin;

import dev.aster.client.AsterClient;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method="calculateFov",at=@At("RETURN"),cancellable=true)
    private void aster$zoom(float partialTicks,CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue(callback.getReturnValue()*AsterClient.zoomScale());
    }
}
