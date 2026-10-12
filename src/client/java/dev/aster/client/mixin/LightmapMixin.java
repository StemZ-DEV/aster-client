package dev.aster.client.mixin;

import dev.aster.client.AsterClient;
import dev.aster.core.Module;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapMixin {
    @Inject(method="extract",at=@At("TAIL"))
    private void aster$fullbright(LightmapRenderState state,float partialTicks,CallbackInfo callback) {
        Module module=AsterClient.modules().get("fullbright");
        if(module.enabled&&state.needsUpdate) {
            state.nightVisionEffectIntensity=Math.max(state.nightVisionEffectIntensity,(float)module.value);
            state.darknessEffectScale*=(float)(1-module.value);
        }
    }
}
