package org.spoorn.spoornweaponattributes.mixin;

import static org.spoorn.spoornweaponattributes.util.SpoornWeaponAttributesUtil.*;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spoorn.spoornweaponattributes.config.ModConfig;
import org.spoorn.spoornweaponattributes.util.SpoornWeaponAttributesUtil;

@Mixin(AnvilMenu.class)
public class AnvilScreenHandlerMixin {

    @Shadow @Final private DataSlot cost;

    @Shadow private int repairItemCountCost;

    /**
     * @param player Player taking the output.
     * @param output ItemStack on the cursor. Note: This will be "air" if user Shift+Clicks the output item.
     */
    @Inject(method = "onTake", at = @At(value = "HEAD"))
    private void rerollSWA(Player player, ItemStack output, CallbackInfo ci) {
        ForgingScreenHandlerAccessor accessor = (ForgingScreenHandlerAccessor) this;
        Container inputInventory = accessor.getInput();
        ItemStack input1 = inputInventory.getItem(0);
        ItemStack input2 = inputInventory.getItem(1);

        // Apply on output item
        if (SpoornWeaponAttributesUtil.hasRootNbt(output)) {
            SpoornWeaponAttributesUtil.updateRootNbt(output, SpoornWeaponAttributesUtil::rollOrUpgradeNbt);
        }

        // Put items in the correct order so vanilla code can subtract the stack count and remove item correctly
        // First slot should be the weapon, 2nd slot should be the upgrade item
        // This prevents the wrong order from deleting the entire stack of the upgrade item
        ItemStack weapon;
        if ((weapon = canUpgradeSWA(input1, input2)) != null || (weapon = canRerollSWA(input1, input2)) != null) {
            ItemStack temp = weapon == input1 ? input2 : input1;
            // Swap if in wrong order
            inputInventory.setItem(0, weapon);
            inputInventory.setItem(1, temp);
        }
    }

    @Inject(method = "createResult", at = @At(value = "RETURN"))
    private void addRerollsSWA(CallbackInfo ci) {
        ForgingScreenHandlerAccessor accessor = (ForgingScreenHandlerAccessor) this;
        Container inputInventory = accessor.getInput();
        ItemStack input1 = inputInventory.getItem(0);
        ItemStack input2 = inputInventory.getItem(1);

        ItemStack swaStack = canRerollSWA(input1, input2);
        if (swaStack != null) {
            ItemStack existingOutputStack = accessor.getOutput().getItem(0);
            boolean useExistingOutput = existingOutputStack != null && !existingOutputStack.isEmpty() 
                    && existingOutputStack.getItem() == swaStack.getItem();
            ItemStack output;
            
            // use existing output stack and modify NBT in case the reroll item is used for other purposes
            if (useExistingOutput) {
                output = existingOutputStack;
            } else {
                output = swaStack.copy();
            }

            // This will cause a reroll no matter what. We could do the same thing with Upgrading in the future if it's simpler than the mixin in ForgingScreenHandlerMixin.
            SpoornWeaponAttributesUtil.updateRootNbt(output, root -> {
                if (root.contains(NBT_KEY)) {
                    root.remove(NBT_KEY);
                }
                root.putBoolean(REROLL_NBT_KEY, true);
            });

            if (!useExistingOutput) {
                this.cost.set(ModConfig.get().rerollLevelCost);
                this.repairItemCountCost = 1;
                accessor.getOutput().setItem(0, output);
                ((ScreenHandlerAccessor) this).trySendContentUpdates();
            }
        } else {
            swaStack = canUpgradeSWA(input1, input2);
            if (swaStack != null) {
                ItemStack existingOutputStack = accessor.getOutput().getItem(0);
                boolean useExistingOutput = existingOutputStack != null && !existingOutputStack.isEmpty()
                        && existingOutputStack.getItem() == swaStack.getItem();
                ItemStack output;

                // use existing output stack and modify NBT in case the upgrade item is used for other purposes
                if (useExistingOutput) {
                    output = existingOutputStack;
                } else {
                    output = swaStack.copy();
                }

                SpoornWeaponAttributesUtil.updateRootNbt(output, root -> root.putBoolean(UPGRADE_NBT_KEY, true));

                if (!useExistingOutput) {
                    this.cost.set(ModConfig.get().upgradeLevelCost);
                    this.repairItemCountCost = 1;
                    accessor.getOutput().setItem(0, output);
                    ((ScreenHandlerAccessor) this).trySendContentUpdates();
                }
            }
        }
    }

    @Inject(method = "mayPickup", at = @At(value = "HEAD"), cancellable = true)
    private void allowNoLevelCostSWA(Player player, boolean present, CallbackInfoReturnable<Boolean> cir) {
        ForgingScreenHandlerAccessor accessor = (ForgingScreenHandlerAccessor) this;
        Container inputInventory = accessor.getInput();
        ItemStack input1 = inputInventory.getItem(0);
        ItemStack input2 = inputInventory.getItem(1);

        if ((canRerollSWA(input1, input2) != null && ModConfig.get().rerollLevelCost <= 0)
                || (canUpgradeSWA(input1, input2) != null && ModConfig.get().upgradeLevelCost <= 0)) {
            cir.setReturnValue(player.getAbilities().instabuild || player.experienceLevel >= this.cost.get());
            cir.cancel();
        }
    }

    // Below methods need to have different names than SpoornArmorAttributes
    private ItemStack canRerollSWA(ItemStack stack1, ItemStack stack2) {
        if (SpoornWeaponAttributesUtil.shouldTryGenAttr(stack1) && SpoornWeaponAttributesUtil.isRerollItem(stack2)) {
            return stack1;
        } else if (SpoornWeaponAttributesUtil.shouldTryGenAttr(stack2) && SpoornWeaponAttributesUtil.isRerollItem(stack1)) {
            return stack2;
        }
        return null;
    }

    private ItemStack canUpgradeSWA(ItemStack stack1, ItemStack stack2) {
        if (SpoornWeaponAttributesUtil.shouldTryGenAttr(stack1) && SpoornWeaponAttributesUtil.isUpgradeItem(stack2)) {
            return stack1;
        } else if (SpoornWeaponAttributesUtil.shouldTryGenAttr(stack2) && SpoornWeaponAttributesUtil.isUpgradeItem(stack1)) {
            return stack2;
        }
        return null;
    }
}
