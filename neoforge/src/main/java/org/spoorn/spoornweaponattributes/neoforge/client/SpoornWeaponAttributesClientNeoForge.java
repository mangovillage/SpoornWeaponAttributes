package org.spoorn.spoornweaponattributes.neoforge.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.spoorn.spoornweaponattributes.client.SpoornWeaponAttributesClient;

public class SpoornWeaponAttributesClientNeoForge {

    public static void init() {
        if (FMLEnvironment.getDist() != Dist.CLIENT) {
            return;
        }

        SpoornWeaponAttributesClient.init();
    }
}
