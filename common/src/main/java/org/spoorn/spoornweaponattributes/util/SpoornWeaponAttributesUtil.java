package org.spoorn.spoornweaponattributes.util;

import lombok.extern.log4j.Log4j2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spoorn.spoornweaponattributes.att.Attribute;
import org.spoorn.spoornweaponattributes.att.Roller;
import org.spoorn.spoornweaponattributes.config.Expressions;
import org.spoorn.spoornweaponattributes.config.ModConfig;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

/**
 * All generic utilities.
 */
@Log4j2
public final class SpoornWeaponAttributesUtil {

    public static final String NBT_KEY = "swa3";
    public static final String REROLL_NBT_KEY = "swa3_reroll";
    public static final String UPGRADE_NBT_KEY = "swa3_upgrade";
    public static final String BONUS_DAMAGE = "bonusDmg";
    public static final String DURATION = "dur";
    public static final String SLOW_DURATION = "slowDur";
    public static final String FREEZE_DURATION = "freezeDur";
    public static final String CRIT_CHANCE = "critChance";
    public static final String LIFESTEAL = "lifesteal";
    public static final String EXPLOSION_CHANCE = "explosionChance";
    public static final Random RANDOM = new Random();

    public static final String SPOORN_LOOT_NBT_KEY = "spoornConfig";

    public static boolean shouldTryGenAttr(ItemStack stack) {
        return stack.has(DataComponents.WEAPON) || stack.has(DataComponents.TOOL);
    }

    public static CompoundTag createAttributesSubNbt(CompoundTag root) {
        CompoundTag res = new CompoundTag();
        root.put(NBT_KEY, res);
        return res;
    }

    public static CompoundTag createAttributesSubNbtReturnRoot(CompoundTag root) {
        CompoundTag res = new CompoundTag();
        root.put(NBT_KEY, res);
        return root;
    }

    public static Optional<CompoundTag> getRootNbtIfPresent(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            return Optional.of(customData.copyTag());
        }
        return Optional.empty();
    }

    public static boolean hasRootNbt(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && !customData.isEmpty();
    }

    public static void updateRootNbt(ItemStack stack, Consumer<CompoundTag> updater) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, updater);
    }

    public static Optional<CompoundTag> getSWANbtIfPresent(ItemStack stack) {
        Optional<CompoundTag> root = getRootNbtIfPresent(stack);
        if (root.isPresent() && root.get().contains(SpoornWeaponAttributesUtil.NBT_KEY)) {
            return Optional.of(root.get().getCompoundOrEmpty(SpoornWeaponAttributesUtil.NBT_KEY));
        }
        return Optional.empty();
    }

    public static boolean hasSWANbt(ItemStack stack) {
        Optional<CompoundTag> root = getRootNbtIfPresent(stack);
        return root.isPresent() && root.get().contains(NBT_KEY);
    }

    public static boolean isRerollItem(ItemStack stack) {
        String rerollItem = ModConfig.get().rerollItem;
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(rerollItem));
        if (item.isEmpty()) {
            throw new RuntimeException("Reroll item " + rerollItem + " was not found in the registry!");
        }
        return stack.getItem().equals(item.get());
    }

    public static boolean isUpgradeItem(ItemStack stack) {
        String upgradeItem = ModConfig.get().upgradeItem;
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(upgradeItem));
        if (item.isEmpty()) {
            throw new RuntimeException("Upgrade item " + upgradeItem + " was not found in the registry!");
        }
        return stack.getItem().equals(item.get());
    }

    /**
     * Assumes chance is between 0.0 and 1.0.
     */
    public static boolean shouldEnable(float chance) {
        return (chance > 0) && (RANDOM.nextFloat() < chance);
    }

    public static boolean shouldEnable(double chance) {
        return (chance > 0) && (RANDOM.nextDouble() < chance);
    }

    public static float getRandomInRange(float min, float max) {
        return RANDOM.nextFloat() * (max - min) + min;
    }

    public static int getRandomInRange(int min, int max) {
        return Math.round(RANDOM.nextFloat() * (max - min) + min);
    }

    public static float drawRandom(boolean useGaussian, float mean, double sd, float min, float max) {
        if (useGaussian) {
            return (float) getNextGaussian(mean, sd, min, max);
        } else {
            return getRandomInRange(min, max);
        }
    }

    // Assumes parameters are correct
    public static double getNextGaussian(float mean, double sd, float min, float max) {
        double nextGaussian = RANDOM.nextGaussian() * sd + mean;
        if (nextGaussian < min) {
            nextGaussian = min;
        } else if (nextGaussian > max) {
            nextGaussian = max;
        }
        return nextGaussian;
    }

    public static void rollOrUpgradeNbt(CompoundTag root) {
        if (root.getBooleanOr(UPGRADE_NBT_KEY, false)) {
            SpoornWeaponAttributesUtil.upgradeAttributes(root);
            root.remove(UPGRADE_NBT_KEY);
        } else if (root.getBooleanOr(REROLL_NBT_KEY, false)) {
            // Technically this is redundant as AnvilScreenHandlerMixin removes the NBT_KEY causing a reroll as soon as the item ticks in an inventory
            SpoornWeaponAttributesUtil.rollAttributes(root);
            root.remove(REROLL_NBT_KEY);
        }
    }


    // Apply attributes
    public static void rollAttributes(CompoundTag root) {
        if (!root.contains(SpoornWeaponAttributesUtil.NBT_KEY) && !root.contains(SPOORN_LOOT_NBT_KEY)) {
            CompoundTag nbt = SpoornWeaponAttributesUtil.createAttributesSubNbt(root);
            //System.out.println("Initial Nbt: " + nbt);

            for (Map.Entry<String, Attribute> entry : Attribute.VALUES.entrySet()) {
                String name = entry.getKey();
                Attribute att = entry.getValue();

                if (SpoornWeaponAttributesUtil.shouldEnable(att.chance)) {
                    CompoundTag newNbt = new CompoundTag();
                    float bonusDamage;
                    switch (name) {
                        case Attribute.CRIT_NAME:
                            newNbt.putFloat(CRIT_CHANCE, Roller.rollCrit());
                            break;
                        case Attribute.FIRE_NAME:
                            bonusDamage = Roller.rollFire();
                            newNbt.putFloat(BONUS_DAMAGE, bonusDamage);
                            newNbt.putFloat(DURATION, Roller.rollDamageDuration(Expressions.fireDuration, bonusDamage));
                            break;
                        case Attribute.COLD_NAME:
                            bonusDamage = Roller.rollCold();
                            newNbt.putFloat(BONUS_DAMAGE, bonusDamage);
                            newNbt.putFloat(SLOW_DURATION, Roller.rollDamageDuration(Expressions.slowDuration, bonusDamage));
                            newNbt.putFloat(FREEZE_DURATION, Roller.rollDamageDuration(Expressions.freezeDuration, bonusDamage));
                            break;
                        case Attribute.LIGHTNING_NAME:
                            newNbt.putFloat(BONUS_DAMAGE, Roller.rollLightning());
                            break;
                        case Attribute.POISON_NAME:
                            bonusDamage = Roller.rollPoison();
                            newNbt.putFloat(BONUS_DAMAGE, bonusDamage);
                            newNbt.putFloat(DURATION, Roller.rollDamageDuration(Expressions.poisonDuration, bonusDamage));
                            break;
                        case Attribute.LIFESTEAL_NAME:
                            newNbt.putFloat(LIFESTEAL, Roller.rollLifesteal());
                            break;
                        case Attribute.EXPLOSIVE_NAME:
                            newNbt.putFloat(EXPLOSION_CHANCE, Roller.rollExplosive());
                            break;
                        default:
                            // do nothing
                            log.error("Unknown SpoornWeaponAttribute: {}", name);
                    }
                    nbt.put(name, newNbt);
                }
            }
            //System.out.println("Updated Nbt: " + nbt);
        }
    }

    // Upgrade stats if applicable
    public static void upgradeAttributes(CompoundTag root) {
        if (!root.contains(SPOORN_LOOT_NBT_KEY)) {
            if (!root.contains(SpoornWeaponAttributesUtil.NBT_KEY)) {
                SpoornWeaponAttributesUtil.createAttributesSubNbtReturnRoot(root);
            }
            CompoundTag nbt = root.getCompoundOrEmpty(SpoornWeaponAttributesUtil.NBT_KEY);

            for (Map.Entry<String, Attribute> entry : Attribute.VALUES.entrySet()) {
                String name = entry.getKey();
                Attribute att = entry.getValue();

                if (SpoornWeaponAttributesUtil.shouldEnable(att.chance)) {
                    CompoundTag newNbt = nbt.contains(name) ? nbt.getCompoundOrEmpty(name) : new CompoundTag();

                    float bonusDamage;
                    switch (name) {
                        case Attribute.CRIT_NAME:
                            checkFloatUpgradeThenAdd(newNbt, CRIT_CHANCE, Roller.rollCrit());
                            break;
                        case Attribute.FIRE_NAME:
                            bonusDamage = Roller.rollFire();
                            checkFloatUpgradeThenAdd(newNbt, BONUS_DAMAGE, bonusDamage);
                            checkFloatUpgradeThenAdd(newNbt, DURATION, Roller.rollDamageDuration(Expressions.fireDuration, bonusDamage));
                            break;
                        case Attribute.COLD_NAME:
                            bonusDamage = Roller.rollCold();
                            checkFloatUpgradeThenAdd(newNbt, BONUS_DAMAGE, bonusDamage);
                            checkFloatUpgradeThenAdd(newNbt, SLOW_DURATION, Roller.rollDamageDuration(Expressions.slowDuration, bonusDamage));
                            checkFloatUpgradeThenAdd(newNbt, FREEZE_DURATION, Roller.rollDamageDuration(Expressions.freezeDuration, bonusDamage));
                            break;
                        case Attribute.LIGHTNING_NAME:
                            checkFloatUpgradeThenAdd(newNbt, BONUS_DAMAGE, Roller.rollLightning());
                            break;
                        case Attribute.POISON_NAME:
                            bonusDamage = Roller.rollPoison();
                            checkFloatUpgradeThenAdd(newNbt, BONUS_DAMAGE, bonusDamage);
                            checkFloatUpgradeThenAdd(newNbt, DURATION, Roller.rollDamageDuration(Expressions.poisonDuration, bonusDamage));
                            break;
                        case Attribute.LIFESTEAL_NAME:
                            checkFloatUpgradeThenAdd(newNbt, LIFESTEAL, Roller.rollLifesteal());
                            break;
                        case Attribute.EXPLOSIVE_NAME:
                            checkFloatUpgradeThenAdd(newNbt, EXPLOSION_CHANCE, Roller.rollExplosive());
                            break;
                        default:
                            // do nothing
                            log.error("Unknown SpoornWeaponAttribute: {}", name);
                    }
                    nbt.put(name, newNbt);
                }
            }
            //System.out.println("Updated Nbt: " + nbt);
        }
    }

    private static void checkFloatUpgradeThenAdd(CompoundTag nbt, String attribute, float newValue) {
        if (!nbt.contains(attribute) || nbt.getFloatOr(attribute, 0.0F) < newValue) {
            nbt.putFloat(attribute, newValue);
        }
    }
}
