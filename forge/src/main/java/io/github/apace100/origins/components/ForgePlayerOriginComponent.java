package io.github.apace100.origins.components;

import io.github.apace100.origins.api.component.OriginComponent;
import io.github.apace100.origins.component.PlayerOriginComponent;
import io.github.apace100.origins.registry.forge.ModComponentsArchitecturyImpl;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.Tag;
import net.minecraft.util.math.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//TODO Check if reimplementing IItemHandlerModifiable is necessary.
public class ForgePlayerOriginComponent extends PlayerOriginComponent implements ICapabilityProvider, ICapabilitySerializable<Tag> {

	private final transient LazyOptional<OriginComponent> thisOptional = LazyOptional.of(() -> this);

	public ForgePlayerOriginComponent(PlayerEntity player) {
		super(player);
	}

	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction arg) {
		return ModComponentsArchitecturyImpl.ORIGIN_COMPONENT_CAPABILITY.orEmpty(capability, this.thisOptional);
	}

	@Override
	public Tag serializeNBT() {
		return ModComponentsArchitecturyImpl.ORIGIN_COMPONENT_CAPABILITY.writeNBT(this, null);
	}

	@Override
	public void deserializeNBT(Tag arg) {
		ModComponentsArchitecturyImpl.ORIGIN_COMPONENT_CAPABILITY.readNBT(this, null, arg);
	}
}
