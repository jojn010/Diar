package com.diar.shared;

import java.util.Map;
import java.util.UUID;

/** One private account-to-listing association in public.favorites. */
public final class Favorite {
    private final String userId, listingId;
    public Favorite(String userId, String listingId) {
        UUID.fromString(userId); UUID.fromString(listingId);
        this.userId = userId; this.listingId = listingId;
    }
    public String getUserId() { return userId; }
    public String getListingId() { return listingId; }
    public Map<String, Object> toRow() { java.util.Map<String,Object> row = new java.util.LinkedHashMap<>(); row.put("user_id", userId); row.put("listing_id", listingId); return java.util.Collections.unmodifiableMap(row); }
}
