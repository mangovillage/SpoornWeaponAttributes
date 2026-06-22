package org.spoorn.spoornweaponattributes.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spoorn.spoornweaponattributes.util.SpoornWeaponAttributesUtil;

/**
 * Note: If all this code starts running into problems, we can migrate to using inventoryTicks, like we did with rerolls
 * in AnvilScreenHandlerMixin - by removing the NBT_KEY and triggering rerolls/upgrades during the inventoryTick.
 */
@Mixin(ItemCombinerMenu.class)
public class ForgingScreenHandlerMixin {
    
    // This is to save the ItemStack during each call to transferSlot to be used by our 2 mixins. Use a map keyed by
    // the player Id in case this someday needs to be thread safe
    private static ItemStack originalTransferSlotItemStack;
    
    @Redirect(method = "quickMoveStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack saveSlotItemStack(ItemStack instance) {
        ItemStack res = instance.copy();
        if ((Object)this instanceof AnvilMenu) {
            originalTransferSlotItemStack = instance;
        }
        return res;
    }

    /**
     * This is a backup method on top of {@link AnvilScreenHandlerMixin}. When the user Shift+Clicks on the output item
     * instead of a simple Left Click, slot.onTakeItem() will be called with an empty ItemStack which causes the code in
     * {@link AnvilScreenHandlerMixin} to not properly apply the attribute logic. Instead, this transferSlot() is called here.
     */
    @Inject(method = "quickMoveStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ItemCombinerMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z", ordinal = 0))
    private void swaRollOrUpgradeShiftClick(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if ((Object)this instanceof AnvilMenu && originalTransferSlotItemStack != null) {
            ItemStack output = originalTransferSlotItemStack;
            // index == 2 when transferring from output to player inventory
            if (index == 2 && SpoornWeaponAttributesUtil.hasRootNbt(output)) {
                SpoornWeaponAttributesUtil.updateRootNbt(output, SpoornWeaponAttributesUtil::rollOrUpgradeNbt);
            }

            originalTransferSlotItemStack = null;
        }
    }
}
