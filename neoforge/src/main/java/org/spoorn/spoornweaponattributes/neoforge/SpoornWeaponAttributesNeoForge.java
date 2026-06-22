package org.spoorn.spoornweaponattributes.neoforge;

import net.neoforged.fml.common.Mod;
import org.spoorn.spoornweaponattributes.SpoornWeaponAttributes;
import org.spoorn.spoornweaponattributes.neoforge.client.SpoornWeaponAttributesClientNeoForge;

@Mod(SpoornWeaponAttributes.MODID)
public class SpoornWeaponAttributesNeoForge {

    public SpoornWeaponAttributesNeoForge() {
        SpoornWeaponAttributes.init();

        // Client
        SpoornWeaponAttributesClientNeoForge.init();
    }
}
