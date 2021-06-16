package io.github.apace100.origins.mixin;

import io.github.apace100.origins.access.EndRespawningEntity;
import io.github.apace100.origins.api.component.OriginComponent;
import io.github.apace100.origins.power.ModifyPlayerSpawnPower;
import io.github.apace100.origins.power.Power;
import io.github.apace100.origins.registry.ModComponentsArchitectury;
import net.minecraft.entity.Dismounting;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(PlayerManager.class)
public abstract class LoginMixin {

	@Redirect(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;setSpawnPoint(Lnet/minecraft/util/registry/RegistryKey;Lnet/minecraft/util/math/BlockPos;FZZ)V"))
	private void preventEndExitSpawnPointSetting(ServerPlayerEntity serverPlayerEntity, RegistryKey<World> dimension, BlockPos pos, float angle, boolean spawnPointSet, boolean bl, ServerPlayerEntity playerEntity, boolean alive) {
		EndRespawningEntity ere = (EndRespawningEntity)playerEntity;
		// Prevent setting the spawn point if the player has a "fake" respawn point
		if(ere.hasRealRespawnPoint()) {
			serverPlayerEntity.setSpawnPoint(dimension, pos, angle, spawnPointSet, bl);
		}
	}

	@Inject(method = "remove", at = @At("HEAD"))
	private void invokeOnRemovedCallback(ServerPlayerEntity player, CallbackInfo ci) {
		OriginComponent component = ModComponentsArchitectury.getOriginComponent(player);
		component.getPowers().forEach(Power::onRemoved);
	}

	@Redirect(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;findRespawnPosition(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;FZZ)Ljava/util/Optional;"))
	private Optional<Vec3d> retryObstructedSpawnpointIfFailed(ServerWorld world, BlockPos pos, float f, boolean bl, boolean bl2, ServerPlayerEntity player, boolean alive) {
		Optional<Vec3d> original = PlayerEntity.findRespawnPosition(world, pos, f, bl, bl2);
		if(!original.isPresent()) {
			if(OriginComponent.hasPower(player, ModifyPlayerSpawnPower.class)) {
				return Optional.ofNullable(Dismounting.method_30769(EntityType.PLAYER, world, pos, bl));
			}
		}
		return original;
	}

	@Inject(method = "respawnPlayer", at = @At("HEAD"))
	private void invokePowerRemovedCallback(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> cir) {
		List<Power> powers = ModComponentsArchitectury.getOriginComponent(player).getPowers();
		powers.forEach(Power::onRemoved);
	}
}
