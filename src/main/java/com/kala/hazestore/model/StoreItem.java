// made with ❤️ by haze
// made with ❤️ by haze
// made with ❤️ by haze
package com.kala.hazestore.model;

public record StoreItem(
        String id,
        String type,
        String mmoItemType,
        String mmoItemId,
        String material,
        String itemsadderId,
        int amount,
        double price,
        int weight,
        boolean vip,
        boolean hidden,
        int maxPurchases,
        int slot,
        String displayName
) {}
