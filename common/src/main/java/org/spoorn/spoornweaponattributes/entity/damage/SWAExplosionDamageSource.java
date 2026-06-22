package org.spoorn.spoornweaponattributes.entity.damage;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/**
 * Used to identify if an explosion was from this mod.
 */
public class SWAExplosionDamageSource extends DamageSource {

    public SWAExplosionDamageSource(Holder<DamageType> type) {
        super(type);
    }
}
