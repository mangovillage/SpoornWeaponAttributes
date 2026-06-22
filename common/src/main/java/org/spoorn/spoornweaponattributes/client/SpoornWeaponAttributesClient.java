package org.spoorn.spoornweaponattributes.client;

import static org.spoorn.spoornweaponattributes.util.SpoornWeaponAttributesUtil.*;
import lombok.extern.log4j.Log4j2;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spoorn.spoornweaponattributes.att.Attribute;
import org.spoorn.spoornweaponattributes.util.SpoornWeaponAttributesUtil;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Log4j2
public class SpoornWeaponAttributesClient {

    private static final Style FIRE_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(16732754));
    private static final Style COLD_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(4890623));
    private static final Style CRIT_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(9851135));
    private static final Style LIGHTNING_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(15990666));
    private static final Style POISON_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(32537));
    private static final Style LIFESTESAL_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(7864320));
    private static final Style EXPLOSIVE_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(16711680));
    private static final MutableComponent FIRE_TOOLTIP = Component.translatable("swa.tooltip.firedamage");
    private static final MutableComponent COLD_TOOLTIP = Component.translatable("swa.tooltip.colddamage");
    private static final MutableComponent CRIT_TOOLTIP = Component.translatable("swa.tooltip.critchance");
    private static final MutableComponent LIGHTNING_TOOLTIP = Component.translatable("swa.tooltip.lightningdamage");
    private static final MutableComponent POISON_TOOLTIP = Component.translatable("swa.tooltip.poisondamage");
    private static final MutableComponent LIFESTEAL_TOOLTIP = Component.translatable("swa.tooltip.lifesteal");
    private static final MutableComponent EXPLOSIVE_TOOLTIP = Component.translatable("swa.tooltip.explosive");
    private static final MutableComponent EXPLOSIVE_PREPEND_TOOLTIP = Component.translatable("swa.tooltip.explosiveprepend").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY);
    private static final DecimalFormatSymbols SYMBOLS = new DecimalFormatSymbols(Locale.US);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.#", SYMBOLS);
    private static final DecimalFormat INTEGER_FORMAT = new DecimalFormat("#", SYMBOLS);

    private static final String LIFESTEAL_NO_TOOLTIP = "0";

    public static void init() {
        log.info("Hello client from SpoornWeaponAttributes!");
    }
    
    // Mimic Fabric API's ItemTooltipCallback with our own Mixin to support both Fabric and NeoForge without introducing
    // Fabric API as a dependency, yet still be easily converted back.
    @FunctionalInterface
    public interface ItemTooltipCallback {
        void getTooltip(ItemStack stack, Item.TooltipContext context, List<Component> lines);
    }

    public static ItemTooltipCallback registerTooltipCallback() {
//        ItemTooltipCallback.EVENT.register(
        return (ItemStack stack, Item.TooltipContext context, List<Component> lines) -> {
            Optional<CompoundTag> optNbt = SpoornWeaponAttributesUtil.getSWANbtIfPresent(stack);

            List<Component> adds = null;

            // Rerolling
            Optional<CompoundTag> rootNbt = SpoornWeaponAttributesUtil.getRootNbtIfPresent(stack);
            if (rootNbt.isPresent() && optNbt.isEmpty()) {
                CompoundTag root = rootNbt.get();
                if (root.getBooleanOr(REROLL_NBT_KEY, false)) {
                    adds = new ArrayList<>();
                    adds.add(Component.literal(""));
                    adds.add(Component.literal("???").withStyle(ChatFormatting.AQUA));
                }
            } else if (optNbt.isPresent()) {
                CompoundTag nbt = optNbt.get();
                adds = new ArrayList<>();
                adds.add(Component.literal(""));

                for (String name : Attribute.TOOLTIPS) {
                    if (nbt.contains(name)) {
                        CompoundTag subNbt = nbt.getCompoundOrEmpty(name);
                        switch (name) {
                            case Attribute.CRIT_NAME:
                                handleCrit(adds, subNbt);
                                break;
                            case Attribute.FIRE_NAME:
                                handleFire(adds, subNbt);
                                break;
                            case Attribute.COLD_NAME:
                                handleCold(adds, subNbt);
                                break;
                            case Attribute.LIGHTNING_NAME:
                                handleLightning(adds, subNbt);
                                break;
                            case Attribute.POISON_NAME:
                                handlePoison(adds, subNbt);
                                break;
                            case Attribute.LIFESTEAL_NAME:
                                handleLifesteal(adds, subNbt);
                                break;
                            case Attribute.EXPLOSIVE_NAME:
                                handleExplosive(adds, subNbt);
                                break;
                            default:
                                // do nothing
                        }
                    }
                }
            }

            // Upgrades
            if (rootNbt.isPresent() && rootNbt.get().getBooleanOr(UPGRADE_NBT_KEY, false)) {
                if (adds == null) {
                    adds = new ArrayList<>();
                }
                adds.add(Component.literal(""));
                adds.add(Component.literal("+++").withStyle(ChatFormatting.RED));
            }

            if (adds != null && adds.size() > 1) {
                // Add after the item name
                lines.addAll(1, adds);
            }
        };
    }

    private static void handleCrit(List<Component> tooltips, CompoundTag nbt) {
        if (nbt.contains(CRIT_CHANCE)) {
            float critChance = nbt.getFloatOr(CRIT_CHANCE, 0.0F);
            MutableComponent text = Component.literal(Integer.toString(Math.round(critChance * 100))).append(CRIT_TOOLTIP).setStyle(CRIT_STYLE);
            tooltips.add(text);
        }
    }

    private static void handleFire(List<Component> tooltips, CompoundTag nbt) {
        if (nbt.contains(BONUS_DAMAGE)) {
            float bonusDamage = nbt.getFloatOr(BONUS_DAMAGE, 0.0F);
            MutableComponent text = Component.literal("+" + DECIMAL_FORMAT.format(bonusDamage)).append(FIRE_TOOLTIP).setStyle(FIRE_STYLE);
            tooltips.add(text);
        }
    }

    private static void handleCold(List<Component> tooltips, CompoundTag nbt) {
        if (nbt.contains(BONUS_DAMAGE)) {
            float bonusDamage = nbt.getFloatOr(BONUS_DAMAGE, 0.0F);
            MutableComponent text = Component.literal("+" + DECIMAL_FORMAT.format(bonusDamage)).append(COLD_TOOLTIP).setStyle(COLD_STYLE);
            tooltips.add(text);
        }
    }

    private static void handleLightning(List<Component> tooltips, CompoundTag nbt) {
        if (nbt.contains(BONUS_DAMAGE)) {
            float bonusDamage = nbt.getFloatOr(BONUS_DAMAGE, 0.0F);
            MutableComponent text = Component.literal("+" + DECIMAL_FORMAT.format(bonusDamage)).append(LIGHTNING_TOOLTIP).setStyle(LIGHTNING_STYLE);
            tooltips.add(text);
        }
    }

    private static void handlePoison(List<Component> tooltips, CompoundTag nbt) {
        if (nbt.contains(BONUS_DAMAGE)) {
            float bonusDamage = nbt.getFloatOr(BONUS_DAMAGE, 0.0F);
            MutableComponent text = Component.literal("+" + DECIMAL_FORMAT.format(bonusDamage)).append(POISON_TOOLTIP).setStyle(POISON_STYLE);
            tooltips.add(text);
        }
    }

    private static void handleLifesteal(List<Component> tooltips, CompoundTag nbt) {
        if (nbt.contains(LIFESTEAL)) {
            float lifesteal = nbt.getFloatOr(LIFESTEAL, 0.0F);
            String lifestealStr = INTEGER_FORMAT.format(lifesteal);
            if (!LIFESTEAL_NO_TOOLTIP.equals(lifestealStr)) {
                MutableComponent text = Component.literal(lifestealStr).append(LIFESTEAL_TOOLTIP).setStyle(LIFESTESAL_STYLE);
                tooltips.add(text);
            }
        }
    }

    private static void handleExplosive(List<Component> tooltips, CompoundTag nbt) {
        MutableComponent text = EXPLOSIVE_TOOLTIP.setStyle(EXPLOSIVE_STYLE);
        tooltips.add(text);
        tooltips.add(0, EXPLOSIVE_PREPEND_TOOLTIP);
    }
}
