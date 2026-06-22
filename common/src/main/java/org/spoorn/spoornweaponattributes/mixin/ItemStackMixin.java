package org.spoorn.spoornweaponattributes.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spoorn.spoornweaponattributes.client.SpoornWeaponAttributesClient;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    /**
     * Copy of Fabric API's ItemStackMixin for ItemTooltipCallback
     */
    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void getTooltip(Item.TooltipContext tooltipContext, Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> info) {
        SpoornWeaponAttributesClient.registerTooltipCallback().getTooltip((ItemStack) (Object) this, tooltipContext, info.getReturnValue());
    }
}
