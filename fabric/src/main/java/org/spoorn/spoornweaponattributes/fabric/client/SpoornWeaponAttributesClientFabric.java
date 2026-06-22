package org.spoorn.spoornweaponattributes.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spoorn.spoornweaponattributes.client.SpoornWeaponAttributesClient;

@Environment(EnvType.CLIENT)
public class SpoornWeaponAttributesClientFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SpoornWeaponAttributesClient.init();
    }
}
