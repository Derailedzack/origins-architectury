package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.power.InvisibilityPower;
import io.github.apace100.origins.power.ModelColorPower;
import io.github.apace100.origins.power.ShakingPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin extends EntityRenderer<LivingEntity> {

    protected LivingEntityRendererMixin(EntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Inject(method = "isShaking", at = @At("HEAD"), cancellable = true)
    private void letPlayersShakeTheirBodies(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if(OriginComponent.hasPower(entity, ShakingPower.class)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    private void preventPumpkinRendering(LivingEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info) {
        List<InvisibilityPower> invisibilityPowers = OriginComponent.getPowers(livingEntity, InvisibilityPower.class);
        if(invisibilityPowers.size() > 0 && invisibilityPowers.stream().noneMatch(InvisibilityPower::shouldRenderArmor)) {
            info.cancel();
        }
    }

    @ModifyVariable(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;", shift = At.Shift.BEFORE))
    private RenderLayer changeRenderLayerWhenTranslucent(RenderLayer original, LivingEntity entity) {
        if(entity instanceof PlayerEntity) {
            if(OriginComponent.getPowers(entity, ModelColorPower.class).stream().anyMatch(ModelColorPower::isTranslucent)) {
                return RenderLayer.getItemEntityTranslucentCull(getTexture(entity));
            }
        }
        return original;
    }

    @Environment(EnvType.CLIENT)
    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V", ordinal = 0))
    private <T extends LivingEntity> void renderColorChangedModel(Args args, LivingEntity player, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        if(player instanceof PlayerEntity) {
            List<ModelColorPower> modelColorPowers = OriginComponent.getPowers(player, ModelColorPower.class);
            if (!modelColorPowers.isEmpty()) {
                //Mixin is being weird.
                //Basically: if there is a redirect, args[0] is a Model
                // otherwise args[0] is the MatrixStack
                int red = args.size() - 4;
                int green = args.size() - 3;
                int blue = args.size() - 2;
                int alpha = args.size() - 1;
                args.set(red, args.<Float>get(red) * modelColorPowers.stream().map(ModelColorPower::getRed).reduce(1.0F, (a, b) -> a * b));
                args.set(green, args.<Float>get(green) * modelColorPowers.stream().map(ModelColorPower::getGreen).reduce(1.0F, (a, b) -> a * b));
                args.set(blue, args.<Float>get(blue) * modelColorPowers.stream().map(ModelColorPower::getBlue).reduce(1.0F, (a, c) -> a * c));
                args.set(alpha, args.<Float>get(alpha) * modelColorPowers.stream().map(ModelColorPower::getAlpha).min(Float::compare).orElseThrow(RuntimeException::new));
            }
        }
    }
}
