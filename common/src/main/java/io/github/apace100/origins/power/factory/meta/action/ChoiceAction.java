package io.github.apace100.origins.power.factory.meta.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.origins.power.factory.action.ActionFactory;
import io.github.apace100.origins.util.FilterableWeightedList;

import java.util.Random;
import java.util.function.Consumer;

public class ChoiceAction<T> implements Consumer<T> {

	public static <T> Codec<ChoiceAction<T>> codec(Codec<FilterableWeightedList<ActionFactory.Instance<T>>> codec) {
		return RecordCodecBuilder.create(instance -> instance.group(
				codec.fieldOf("actions").forGetter(x -> x.list)
		).apply(instance, ChoiceAction::new));
	}

	private final FilterableWeightedList<ActionFactory.Instance<T>> list;

	public ChoiceAction(FilterableWeightedList<ActionFactory.Instance<T>> list) {this.list = list;}

	@Override
	public void accept(T t) {
		this.list.pickRandom(new Random()).accept(t);
	}
}
