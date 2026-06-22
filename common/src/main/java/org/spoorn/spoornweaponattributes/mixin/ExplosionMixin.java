package org.spoorn.spoornweaponattributes.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spoorn.spoornweaponattributes.entity.damage.SWAExplosionDamageSource;

/**
 * TODO: configurations for these
 */
@Mixin(ServerExplosion.class)
public class ExplosionMixin {

    @Shadow @Final private DamageSource damageSource;
    
    @Redirect(method = {
            "hurtEntities()V",
            "hurtEntities(Ljava/util/List;)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;ignoreExplosion(Lnet/minecraft/world/level/Explosion;)Z"))
    private boolean disableExplosionOnNonLivingEntitiesAndPlayers(Entity instance, Explosion explosion) {
        if ((this.damageSource instanceof SWAExplosionDamageSource) && (!(instance instanceof LivingEntity) || instance instanceof Player)) {
            return true;
        }
        return instance.ignoreExplosion(explosion);
    }
}
