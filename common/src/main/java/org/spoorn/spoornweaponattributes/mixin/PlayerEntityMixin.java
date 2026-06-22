package org.spoorn.spoornweaponattributes.mixin;

import static org.spoorn.spoornweaponattributes.util.SpoornWeaponAttributesUtil.*;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spoorn.spoornweaponattributes.att.Attribute;
import org.spoorn.spoornweaponattributes.config.ModConfig;
import org.spoorn.spoornweaponattributes.config.attribute.ExplosiveConfig;
import org.spoorn.spoornweaponattributes.entity.damage.SWAExplosionDamageSource;
import org.spoorn.spoornweaponattributes.util.SpoornWeaponAttributesUtil;

import java.util.Map.Entry;
import java.util.Optional;

@Mixin(Player.class)
public class PlayerEntityMixin {

    /**
     * Fetch data from NBT and apply pre damage modifications.
     * 
     * ordinal = 0 is required for NeoForge, else this ends up also redirecting some knockback velocity for some reason.
     */
    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D", ordinal = 0))
    public double modifyBaseDamage(Player instance, Holder<net.minecraft.world.entity.ai.attributes.Attribute> entityAttribute, Entity target) {
        float f = (float) instance.getAttributeValue(Attributes.ATTACK_DAMAGE);
        try {
            if ((instance instanceof ServerPlayer) && (target instanceof LivingEntity)) {
                ItemStack mainItemStack = instance.getMainHandItem();
                Optional<CompoundTag> optNbt = SpoornWeaponAttributesUtil.getSWANbtIfPresent(mainItemStack);
                if (optNbt.isPresent()) {
                    CompoundTag nbt = optNbt.get();
                    for (Entry<String, Attribute> entry : Attribute.VALUES.entrySet()) {
                        String name = entry.getKey();

                        if (nbt.contains(name)) {
                            CompoundTag subNbt = nbt.getCompoundOrEmpty(name);
                            switch (name) {
                                case Attribute.FIRE_NAME:
                                    f += handleFire(subNbt, instance, target);
                                    break;
                                case Attribute.COLD_NAME:
                                    f += handleCold(subNbt, instance, target);
                                    break;
                                case Attribute.LIGHTNING_NAME:
                                    f += handleLightning(subNbt, instance, target);
                                    break;
                                case Attribute.POISON_NAME:
                                    f += handlePoison(subNbt, instance, target);
                                    break;
                                default:
                                    // crit, lifesteal, and other attributes apply to final damage
                                    // do nothing
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SpoornWeaponAttributes] Applying base attribute effects failed: " + e);
        }

        return f;
    }

    /**
     * Fetch data from NBT and apply modifications to final damage.
     */
    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    public boolean modifyFinalDamage(Entity instance, DamageSource source, float amount) {
        try {
            Player player = (Player) (Object) this;
            if ((player instanceof ServerPlayer) && (instance instanceof LivingEntity)) {
                ItemStack mainItemStack = player.getMainHandItem();
                Optional<CompoundTag> optNbt = SpoornWeaponAttributesUtil.getSWANbtIfPresent(mainItemStack);
                if (optNbt.isPresent()) {
                    CompoundTag nbt = optNbt.get();
                    if (nbt.contains(Attribute.CRIT_NAME)) {
                        CompoundTag subNbt = nbt.getCompoundOrEmpty(Attribute.CRIT_NAME);
                        amount = handleCrit(amount, subNbt, player, instance);
                    }

                    if (nbt.contains(Attribute.LIFESTEAL_NAME)) {
                        CompoundTag subNbt = nbt.getCompoundOrEmpty(Attribute.LIFESTEAL_NAME);
                        amount = handleLifesteal(amount, subNbt, player, instance);
                    }

                    if (nbt.contains(Attribute.EXPLOSIVE_NAME)) {
                        CompoundTag subNbt = nbt.getCompoundOrEmpty(Attribute.EXPLOSIVE_NAME);
                        amount = handleExplosive(amount, subNbt, player, instance);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SpoornWeaponAttributes] Applying final attribute effects failed: " + e);
        }
        return instance.hurtOrSimulate(source, amount);
    }


    /**
     * We manually list all the handles here for optimal latency
     */

    private float handleFire(CompoundTag nbt, Player player, Entity target) {
        int fireDurationTicks = 0;
        if (nbt.contains(DURATION)) {
            fireDurationTicks = (int) (nbt.getFloatOr(DURATION, 0.0F) * 20);
        }
        if (fireDurationTicks > 0 && target.getRemainingFireTicks() < fireDurationTicks) {
            target.setRemainingFireTicks(fireDurationTicks);
        }
        if (nbt.contains(BONUS_DAMAGE)) {
            return nbt.getFloatOr(BONUS_DAMAGE, 0.0F);
        }
        return 0;
    }

    private float handleCold(CompoundTag nbt, Player player, Entity target) {
        LivingEntity livingEntity = (LivingEntity) target;

        int freezeDurationTicks = 0;
        if (nbt.contains(FREEZE_DURATION)) {
            // *40 because freeze duration decreases by 2 per tick
            freezeDurationTicks = (int) (nbt.getFloatOr(FREEZE_DURATION, 0.0F) * 40);
        }
        if (freezeDurationTicks > 0 && livingEntity.getTicksFrozen() < freezeDurationTicks) {
            livingEntity.setTicksFrozen(freezeDurationTicks);
        }
        
        int slowDurationTicks = 0;
        if (nbt.contains(SLOW_DURATION)) {
            slowDurationTicks = (int) (nbt.getFloatOr(SLOW_DURATION, 0.0F)) * 20;
        }
        if (slowDurationTicks > 0) {
            MobEffectInstance existingSlowEffect = livingEntity.getEffect(MobEffects.SLOWNESS);
            if (existingSlowEffect == null || existingSlowEffect.getDuration() < slowDurationTicks) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slowDurationTicks, 2));
            }
        }
        if (nbt.contains(BONUS_DAMAGE)) {
            return nbt.getFloatOr(BONUS_DAMAGE, 0.0F);
        }
        return 0;
    }

    private float handleLightning(CompoundTag nbt, Player player, Entity target) {
        double lightningStrikeChance = ModConfig.get().lightningConfig.lightningStrikeChance;
        if (SpoornWeaponAttributesUtil.shouldEnable(lightningStrikeChance) && target.level() instanceof ServerLevel serverLevel) {
            LightningBolt lightningEntity = EntityTypes.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
            if (lightningEntity != null) {
                lightningEntity.teleportTo(target.getX(), target.getY(), target.getZ());
                serverLevel.addFreshEntity(lightningEntity);
            }
        }
        if (nbt.contains(BONUS_DAMAGE)) {
            return nbt.getFloatOr(BONUS_DAMAGE, 0.0F);
        }
        return 0;
    }

    private float handlePoison(CompoundTag nbt, Player player, Entity target) {
        int poisonDurationTicks = 0;
        if (nbt.contains(DURATION)) {
            poisonDurationTicks = (int) (nbt.getFloatOr(DURATION, 0.0F) * 20);
        }
        
        if (poisonDurationTicks > 0) {
            LivingEntity livingEntity = (LivingEntity) target;
            MobEffectInstance existingEffect = livingEntity.getEffect(MobEffects.POISON);
            if (existingEffect == null || existingEffect.getDuration() < poisonDurationTicks) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDurationTicks, 2));
            }
        }
            
        if (nbt.contains(BONUS_DAMAGE)) {
            return nbt.getFloatOr(BONUS_DAMAGE, 0.0F);
        }
        return 0;
    }

    private float handleCrit(float damage, CompoundTag nbt, Player player, Entity target) {
        if (nbt.contains(CRIT_CHANCE)) {
            float critChance = nbt.getFloatOr(CRIT_CHANCE, 0.0F);
            if (SpoornWeaponAttributesUtil.shouldEnable(critChance)) {
                return (float) (damage * ModConfig.get().critConfig.critMultiplier);
            }
        }
        return damage;
    }

    private float handleLifesteal(float damage, CompoundTag nbt, Player player, Entity target) {
        if (nbt.contains(LIFESTEAL)) {
            float lifesteal = nbt.getFloatOr(LIFESTEAL, 0.0F);
            player.heal(lifesteal * damage / 100);
        }
        return damage;
    }

    private float handleExplosive(float damage, CompoundTag nbt, Player player, Entity target) {
        if (nbt.contains(EXPLOSION_CHANCE)) {
            float explosionChance = nbt.getFloatOr(EXPLOSION_CHANCE, 0.0F);
            ExplosiveConfig config = ModConfig.get().explosiveConfig;
            Level level = target.level();
            if (SpoornWeaponAttributesUtil.shouldEnable(explosionChance) && !level.isClientSide()) {
                level.explode(target, new SWAExplosionDamageSource(player.damageSources().explosion(player, target).typeHolder()),
                        null, target.getX(), target.getY(), target.getZ(),
                        (float) config.explosionPower, config.causeFires, config.breakBlocks ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
            }
        }
        return damage;
    }
}
