package org.spoorn.spoornweaponattributes.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerMenu.class)
public interface ScreenHandlerAccessor {
    
    @Invoker("broadcastChanges")
    void trySendContentUpdates();
}
