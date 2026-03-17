package com.fetal.lootr;

import java.util.UUID;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class LootrHolder implements InventoryHolder {

    private final String chestKey;
    private final UUID viewerUuid;
    private final UUID storageUuid;
    private Inventory inventory;

    public LootrHolder(String chestKey, UUID viewerUuid, UUID storageUuid) {
        this.chestKey = chestKey;
        this.viewerUuid = viewerUuid;
        this.storageUuid = storageUuid;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public String getChestKey() {
        return chestKey;
    }

    public UUID getViewerUuid() {
        return viewerUuid;
    }

    public UUID getStorageUuid() {
        return storageUuid;
    }
}
