package io.github.apace100.origins.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sun.tools.javac.jvm.Code;
import io.github.apace100.origins.power.PowerType;
import io.github.apace100.origins.power.PowerTypeReference;
import io.github.apace100.origins.power.PowerTypes;
import io.github.apace100.origins.power.action.entity.PlaySoundAction;
import io.github.apace100.origins.power.factory.GenericFactory;
import io.github.apace100.origins.power.factory.action.ActionFactory;
import io.github.apace100.origins.power.factory.condition.ConditionFactory;
import io.github.apace100.origins.registry.ModRegistriesArchitectury;
import io.github.apace100.origins.util.codec.IngredientCodec;
import io.github.apace100.origins.util.codec.InlineCodec;
import io.github.apace100.origins.util.codec.OptionalField;
import net.minecraft.block.Block;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.tag.ServerTagManagerHolder;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.Pair;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OriginsCodecs {
	//Large Codecs
	public static final Codec<Identifier> FUZZY_IDENTIFIER = Codec.STRING.flatXmap(input -> {
		String namespace = "minecraft";
		String path;
		if (input.contains(":")) {
			String[] split = input.split(":");
			if (split.length != 2)
				return DataResult.error("Incorrect number of `:` in identifier: \"" + input + "\".");
			namespace = split[0];
			path = split[1];
		} else
			path = input;
		if (namespace.contains("*")) {
			if (PowerTypes.CURRENT_NAMESPACE != null)
				namespace = namespace.replace("*", PowerTypes.CURRENT_NAMESPACE);
			else
				return DataResult.error("Identifier may only contain a `*` in the namespace inside of powers.");
		}
		if (path.contains("*")) {
			if (PowerTypes.CURRENT_PATH != null)
				path = path.replace("*", PowerTypes.CURRENT_NAMESPACE);
			else
				return DataResult.error("Identifier may only contain a `*` in the path inside of powers.");
		}
		try {
			return DataResult.success(new Identifier(namespace + ":" + path));
		} catch (InvalidIdentifierException var2) {
			return DataResult.error("Not a valid resource location: " + namespace + ":" + path + " " + var2.getMessage());
		}
	}, x -> DataResult.success(x.toString()));

	public static final Codec<OptionalField> REGISTRY_ENTRY = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("type").forGetter(x -> x.value),
			Codec.BOOL.optionalFieldOf("optional", false).forGetter(x -> x.optional)
	).apply(instance, OptionalField::new));

	public static final Codec<DamageSource> DAMAGE_SOURCE = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("name").forGetter(DamageSource::getName),
			Codec.BOOL.optionalFieldOf("bypasses_armor", false).forGetter(DamageSource::bypassesArmor),
			Codec.BOOL.optionalFieldOf("fire", false).forGetter(DamageSource::isFire),
			Codec.BOOL.optionalFieldOf("unblockable", false).forGetter(DamageSource::isUnblockable),
			Codec.BOOL.optionalFieldOf("magic", false).forGetter(DamageSource::getMagic),
			Codec.BOOL.optionalFieldOf("out_of_world", false).forGetter(DamageSource::isOutOfWorld)
	).apply(instance, (name, bypassesArmor, fire, unblockable, magic, outOfWorld) -> {
		DamageSource ds = new DamageSource(name);
		if (bypassesArmor) ds.setBypassesArmor();
		if (fire) ds.setFire();
		if (unblockable) ds.setUnblockable();
		if (magic) ds.setUsesMagic();
		if (outOfWorld) ds.setOutOfWorld();
		return ds;
	}));

	public static final Codec<StatusEffectInstance> STATUS_EFFECT_INSTANCE = RecordCodecBuilder.create(instance -> instance.group(
			Registry.STATUS_EFFECT.fieldOf("effect").forGetter(StatusEffectInstance::getEffectType),
			Codec.INT.optionalFieldOf("duration", 100).forGetter(StatusEffectInstance::getDuration),
			Codec.INT.optionalFieldOf("amplifier", 0).forGetter(StatusEffectInstance::getAmplifier),
			Codec.BOOL.optionalFieldOf("is_ambient", false).forGetter(StatusEffectInstance::isAmbient),
			Codec.BOOL.optionalFieldOf("show_particles", false).forGetter(StatusEffectInstance::shouldShowParticles),
			Codec.BOOL.optionalFieldOf("show_icon", false).forGetter(StatusEffectInstance::shouldShowIcon)
	).apply(instance, StatusEffectInstance::new));
	//Enum Codec
	public static final Codec<Comparison> COMPARISON = enumCodec(Comparison.values(), Arrays.stream(Comparison.values()).collect(ImmutableMap.toImmutableMap(Comparison::getComparisonString, Function.identity())));
	public static final Codec<LightType> LIGHT_TYPE = enumCodec(LightType.values(), ImmutableMap.of());
	public static final Codec<GameMode> GAME_MODE = enumCodec(GameMode.values(), Arrays.stream(GameMode.values()).collect(Collectors.toMap(GameMode::getName, Function.identity())));
	public static final Codec<EquipmentSlot> EQUIPMENT_SLOT = enumCodec(EquipmentSlot.values(), ImmutableMap.of());
	public static final Codec<Shape> SHAPE = enumCodec(Shape.values(), ImmutableMap.of());
	public static final Codec<Space> SPACE = enumCodec(Space.values(), ImmutableMap.of());
	//Tag Codecs
	public static final Codec<Tag<Block>> BLOCK_TAG = Tag.codec(() -> ServerTagManagerHolder.getTagManager().getBlocks());
	public static final Codec<Tag<Item>> ITEM_TAG = Tag.codec(() -> ServerTagManagerHolder.getTagManager().getItems());
	public static final Codec<Tag<Fluid>> FLUID_TAG = Tag.codec(() -> ServerTagManagerHolder.getTagManager().getFluids());
	public static final Codec<Tag<EntityType<?>>> ENTITY_TAG = Tag.codec(() -> ServerTagManagerHolder.getTagManager().getEntityTypes());
	//Simple Codecs
	public static final Codec<PowerType<?>> POWER_TYPE = FUZZY_IDENTIFIER.xmap(PowerTypeReference::new, PowerType::getIdentifier);
	public static final Codec<RegistryKey<World>> DIMENSION = Identifier.CODEC.xmap(x -> RegistryKey.of(Registry.DIMENSION, x), RegistryKey::getValue);
	public static final Codec<EntityGroup> ENTITY_GROUP = mappedCodec(Codec.STRING, ImmutableBiMap.of("default", EntityGroup.DEFAULT, "undead", EntityGroup.UNDEAD, "arthropod", EntityGroup.ARTHROPOD, "illager", EntityGroup.ILLAGER, "aquatic", EntityGroup.AQUATIC));
	public static final Codec<Ingredient> INGREDIENT = new IngredientCodec();
	//Condition Codecs
	public static final Codec<ConditionFactory<LivingEntity>> ENTITY_CONDITION_FACTORY = GenericFactory.factoryCodec(ModRegistriesArchitectury.ENTITY_CONDITION);
	public static final Codec<ConditionFactory<ItemStack>> ITEM_CONDITION_FACTORY = GenericFactory.factoryCodec(ModRegistriesArchitectury.ITEM_CONDITION);
	public static final Codec<ConditionFactory<CachedBlockPosition>> BLOCK_CONDITION_FACTORY = GenericFactory.factoryCodec(ModRegistriesArchitectury.BLOCK_CONDITION);
	public static final Codec<ConditionFactory<Pair<DamageSource, Float>>> DAMAGE_CONDITION_FACTORY = GenericFactory.factoryCodec(ModRegistriesArchitectury.DAMAGE_CONDITION);
	public static final Codec<ConditionFactory<FluidState>> FLUID_CONDITION_FACTORY = GenericFactory.factoryCodec(ModRegistriesArchitectury.FLUID_CONDITION);
	public static final Codec<ConditionFactory<Biome>> BIOME_CONDITION_FACTORY = GenericFactory.factoryCodec(ModRegistriesArchitectury.BIOME_CONDITION);
	public static final Codec<ConditionFactory.Instance<LivingEntity>> ENTITY_CONDITION = GenericFactory.instanceCodec(ENTITY_CONDITION_FACTORY);
	public static final Codec<ConditionFactory.Instance<ItemStack>> ITEM_CONDITION = GenericFactory.instanceCodec(ITEM_CONDITION_FACTORY);
	public static final Codec<ConditionFactory.Instance<CachedBlockPosition>> BLOCK_CONDITION = GenericFactory.instanceCodec(BLOCK_CONDITION_FACTORY);
	public static final Codec<ConditionFactory.Instance<Pair<DamageSource, Float>>> DAMAGE_CONDITION = GenericFactory.instanceCodec(DAMAGE_CONDITION_FACTORY);
	public static final Codec<ConditionFactory.Instance<FluidState>> FLUID_CONDITION = GenericFactory.instanceCodec(FLUID_CONDITION_FACTORY);
	public static final Codec<ConditionFactory.Instance<Biome>> BIOME_CONDITION = GenericFactory.instanceCodec(BIOME_CONDITION_FACTORY);
	//Action Codecs
	public static final Codec<ActionFactory<Triple<World, BlockPos, Direction>>> BLOCK_ACTION_FACTORY = GenericFactory.factoryCodec(ModRegistriesArchitectury.BLOCK_ACTION);
	public static final Codec<ActionFactory<Entity>> ENTITY_ACTION_FACTORY = GenericFactory.factoryCodec(ModRegistriesArchitectury.ENTITY_ACTION);
	public static final Codec<ActionFactory<ItemStack>> ITEM_ACTION_FACTORY = GenericFactory.factoryCodec(ModRegistriesArchitectury.ITEM_ACTION);
	public static final Codec<ActionFactory.Instance<Triple<World, BlockPos, Direction>>> BLOCK_ACTION = GenericFactory.instanceCodec(BLOCK_ACTION_FACTORY);
	public static final Codec<ActionFactory.Instance<Entity>> ENTITY_ACTION = GenericFactory.instanceCodec(ENTITY_ACTION_FACTORY);
	public static final Codec<ActionFactory.Instance<ItemStack>> ITEM_ACTION = GenericFactory.instanceCodec(ITEM_ACTION_FACTORY);
	//Registry Codecs
	public static final Codec<Optional<EntityAttribute>> OPTIONAL_ATTRIBUTE = optionalRegistry(Registry.ATTRIBUTE::getOrEmpty, Registry.ATTRIBUTE::getId);
	public static final Codec<Optional<Biome>> OPTIONAL_BIOME = optionalRegistry(BuiltinRegistries.BIOME::getOrEmpty, BuiltinRegistries.BIOME::getId);
	public static final Codec<Optional<Block>> OPTIONAL_BLOCK = optionalRegistry(Registry.BLOCK::getOrEmpty, Registry.BLOCK::getId);
	public static final Codec<Optional<EntityType<?>>> OPTIONAL_ENTITY_TYPE = optionalRegistry(Registry.ENTITY_TYPE::getOrEmpty, Registry.ENTITY_TYPE::getId);
	public static final Codec<Optional<Enchantment>> OPTIONAL_ENCHANTMENT = optionalRegistry(Registry.ENCHANTMENT::getOrEmpty, Registry.ENCHANTMENT::getId);
	public static final Codec<Optional<StatusEffect>> OPTIONAL_STATUS_EFFECT = optionalRegistry(Registry.STATUS_EFFECT::getOrEmpty, Registry.STATUS_EFFECT::getId);
	public static final Codec<Optional<SoundEvent>> OPTIONAL_SOUND_EVENT = optionalRegistry(Registry.SOUND_EVENT::getOrEmpty, Registry.SOUND_EVENT::getId);

	public static <T extends Enum<T>> Codec<T> enumCodec(T[] values, Map<String, T> additional) {
		ImmutableMap.Builder<String, T> builder = ImmutableMap.builder();
		builder.putAll(additional);
		for (T value : values) {
			String key = value.name().toLowerCase(Locale.ROOT);
			builder.put(key, value);
			if (value instanceof StringIdentifiable) {
				String name = ((StringIdentifiable) value).asString().toLowerCase(Locale.ROOT);
				if (!Objects.equals(name, key))
					builder.put(name, value);
			}
		}
		ImmutableMap<String, T> map = builder.build();
		return Codec.either(
				Codec.intRange(0, values.length - 1).xmap(i -> values[i], Enum::ordinal),
				Codec.STRING.flatXmap(
						x -> map.containsKey(x.toLowerCase(Locale.ROOT)) ? DataResult.success(map.get(x.toLowerCase(Locale.ROOT))) : DataResult.<T>error("Couldn't find field " + x + " for enum " + values.getClass().getComponentType()),
						x -> DataResult.success(x.name().toLowerCase(Locale.ROOT)))
		).xmap(x -> x.map(Function.identity(), Function.identity()), Either::left);
	}

	/**
	 * This codec is equivalent to {@link Codec#list(Codec)} with the exception that
	 * if the object is a single value instead of a {@link List}, it will be correctly handled and
	 * converted to a list.
	 *
	 * @param source The codec to make a list out of.
	 *
	 * @return The list codec.
	 */
	public static <T> Codec<List<T>> listOf(Codec<T> source) {
		return Codec.either(source, source.listOf()).xmap(x -> x.map(Lists::newArrayList, Function.identity()), Either::right);
	}

	/**
	 * Represents a registry entry, stored and read via {@link Identifier}, with platform and optional checking.
	 *
	 * @param accessor   The function used to access the object from it's {@link Identifier}
	 * @param nameGetter The function used to access the {@link Identifier} from the object.
	 *
	 * @return A new codec that supports inline and object type with fields "type", "optional" and "platform".
	 */
	public static <T> Codec<Optional<T>> optionalRegistry(Function<Identifier, Optional<T>> accessor, Function<T, Identifier> nameGetter) {
		return Codec.either(Identifier.CODEC.flatXmap(x -> DataResult.success(accessor.apply(x)), x -> x.map(nameGetter).map(DataResult::success).orElseGet(() -> DataResult.error("Object " + x + " wan't registered"))), REGISTRY_ENTRY)
				.flatXmap(x -> x.map(DataResult::success, t -> t.get(accessor)), t -> DataResult.success(Either.left(t)));
	}

	/**
	 * A dispatch codec whose second part will be inlined if no maps aren't being compressed.
	 * @param typeKey The name of the master type.
	 * @param sourceCodec The codec for the factory type.
	 * @param accessor A way to access the factory from the instance.
	 * @param codecAccessor A way to access the codec of the instance from the master.
	 * @return The new codec.
	 */
	public static <E, A> Codec<E> inlineDispatch(String typeKey, Codec<A> sourceCodec, final Function<? super E, ? extends A> accessor, final Function<? super A, ? extends Codec<? extends E>> codecAccessor) {
		return new InlineCodec<>(typeKey, sourceCodec, accessor, codecAccessor);
	}

	public static <E, A> Codec<E> inlineDispatch(Codec<A> sourceCodec, final Function<? super E, ? extends A> accessor, final Function<? super A, ? extends Codec<? extends E>> codecAccessor) {
		return inlineDispatch("type", sourceCodec, accessor, codecAccessor);
	}

	public static <E, A> Codec<E> mappedCodec(Codec<A> keyCodec, BiMap<A, E> map) {
		return keyCodec.flatXmap(x -> !map.containsKey(x) ? DataResult.error("Unknown key: " + x) : DataResult.success(map.get(x)), x -> !map.inverse().containsKey(x) ? DataResult.error("Unknown value: " + x) : DataResult.success(map.inverse().get(x)));
	}

	public static <E> Codec<FilterableWeightedList<E>> weightedListOf(Codec<E> source) {
		return RecordCodecBuilder.<Pair<E, Integer>>create(instance -> instance.group(
				source.fieldOf("element").forGetter(Pair::getLeft),
				Codec.INT.fieldOf("weight").forGetter(Pair::getRight)
		).apply(instance, Pair::new)).listOf().xmap(x -> {
			FilterableWeightedList<E> ls = new FilterableWeightedList<>();
			x.forEach(y -> ls.add(y.getLeft(), y.getRight()));
			return ls;
		}, x -> x.entryStream().map(y -> new Pair<>(y.getElement(), y.weight)).collect(Collectors.toList()));
	}
}
