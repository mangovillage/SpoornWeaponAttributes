package org.spoorn.spoornweaponattributes.mixin;

import static org.spoorn.spoornweaponattributes.util.SpoornWeaponAttributesUtil.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spoorn.spoornweaponattributes.att.Attribute;
import org.spoorn.spoornweaponattributes.config.Expressions;
import org.spoorn.spoornweaponattributes.config.ModConfig;
import org.spoorn.spoornweaponattributes.util.SpoornWeaponAttributesUtil;

import java.util.Map.Entry;

@Mixin(Item.class)
public class ItemMixin {

    // For compatibility
    private static final String OLD_NBT_KEY = "spoornWeaponAttributes";

    /**
     * Generates the NBT data for our mod on an item.
     */
    @Inject(method = "inventoryTick", at = @At(value = "HEAD"))
    public void addCustomNbt(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot, CallbackInfo ci) {
        if (ModConfig.get().applyOnInventoryTick && SpoornWeaponAttributesUtil.shouldTryGenAttr(stack)) {
            SpoornWeaponAttributesUtil.updateRootNbt(stack, root -> {
                if (root.contains(OLD_NBT_KEY)) {
                    handleMigration(root);
                }

                SpoornWeaponAttributesUtil.rollAttributes(root);
            });
        }
    }
    
    // Handles migration from old system to new 3.x+ system with durations as expressions for backwards compatibility
    private void handleMigration(CompoundTag root) {
        CompoundTag oldNbt = root.getCompoundOrEmpty(OLD_NBT_KEY);
        CompoundTag newNbt = SpoornWeaponAttributesUtil.createAttributesSubNbt(root);

        for (Entry<String, Attribute> entry : Attribute.VALUES.entrySet()) {
            String name = entry.getKey();
            
            if (oldNbt.contains(name)) {
                CompoundTag oldInner = oldNbt.getCompoundOrEmpty(name).copy();

                float bonusDamage;
                float duration;
                switch (name) {
                    case Attribute.FIRE_NAME:
                        bonusDamage = oldInner.getFloatOr(BONUS_DAMAGE, 0.0F);
                        duration = (float) Expressions.fireDuration.setVariable(Expressions.DAMAGE_VAR, bonusDamage).evaluate();
                        oldInner.putFloat(DURATION, duration);
                        break;
                    case Attribute.COLD_NAME:
                        bonusDamage = oldInner.getFloatOr(BONUS_DAMAGE, 0.0F);
                        float slowDuration = (float) Expressions.slowDuration.setVariable(Expressions.DAMAGE_VAR, bonusDamage).evaluate();
                        oldInner.putFloat(SLOW_DURATION, slowDuration);
                        float freezeDuration = (float) Expressions.freezeDuration.setVariable(Expressions.DAMAGE_VAR, bonusDamage).evaluate();
                        oldInner.putFloat(FREEZE_DURATION, freezeDuration);
                        break;
                    case Attribute.POISON_NAME:
                        bonusDamage = oldInner.getFloatOr(BONUS_DAMAGE, 0.0F);
                        duration = (float) Expressions.poisonDuration.setVariable(Expressions.DAMAGE_VAR, bonusDamage).evaluate();
                        oldInner.putFloat(DURATION, duration);
                        break;
                    default:
                        // do nothing
                }

                newNbt.put(name, oldInner);
            }
        }
    }
}
