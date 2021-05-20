package io.github.apace100.origins.power;

import io.github.apace100.origins.Origins;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

public class PreventItemUsePower extends Power {

    private final Predicate<ItemStack> predicate;

    public PreventItemUsePower(PowerType<?> type, PlayerEntity player, Predicate<ItemStack> predicate) {
        super(type, player);
        this.predicate = predicate;
        /*UseItemCallback.EVENT.register(((playerEntity, world, hand) -> {
            if(getType().isActive(playerEntity)) {
                ItemStack stackInHand = playerEntity.getStackInHand(hand);
                if(doesPrevent(stackInHand)) {
                    return TypedActionResult.fail(stackInHand);
                }
            }
            return TypedActionResult.pass(ItemStack.EMPTY);
        }));*/
    }

    public boolean doesPrevent(ItemStack stack) {
        return (stack.isFood() && Origins.config.disableFoodRestrictions) || predicate.test(stack);
    }
}
