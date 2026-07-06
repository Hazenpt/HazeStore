package com.kala.hazestore.model;

// made with ❤️ by haze

public record StoreItem(
        String id,
        String type,
        String mmoItemType,
        String mmoItemId,
        String material,
        int amount,
        double price,
        int weight
) {}
