package io.github.apace100.origins.mixin.forge;

import io.github.apace100.origins.power.factories.ElytraFlightPower;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forge uses a different version of ElytraFeatureRenderer.
 * This is the same thing as the fabric version.
 */
@Mixin(ElytraFeatureRenderer.class)
public class ElytraFeatureRendererMixin {

    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true, remap = false)
    private <T extends LivingEntity> void modifyEquippedStackToElytra(ItemStack stack, T entity, CallbackInfoReturnable<Boolean> cir) {
        if(entity instanceof PlayerEntity player && ElytraFlightPower.shouldRenderElytra(player) && !entity.isInvisible()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
