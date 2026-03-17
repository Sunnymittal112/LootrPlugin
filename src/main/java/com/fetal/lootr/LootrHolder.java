package com.fetal.lootr;

import java.util.UUID;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class LootrHolder implements InventoryHolder {

    private final String chestKey;
    private final UUID ownerUuid;
    private Inventory inventory;

    public LootrHolder(String chestKey, UUID ownerUuid) {
        this.chestKey = chestKey;
        this.ownerUuid = ownerUuid;
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

    public UUID getOwnerUuid() {
        return ownerUuid;
    }
}