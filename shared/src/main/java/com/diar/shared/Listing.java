package com.diar.shared;

import java.math.BigDecimal;
import java.util.*;

/** Canonical Supabase listing contract, used by Android and Spring Boot. No platform dependencies. */
public final class Listing {
    private final String id, ownerId, title, location, description, purpose, type, locationSource, photoPath;
    private final BigDecimal area;
    private final long price;
    private final boolean negotiable;
    private final int floors;
    private final double latitude, longitude;

    public Listing(String id, String ownerId, String title, String location, BigDecimal area, long price,
                   String description, boolean negotiable, String purpose, String type, int floors,
                   double latitude, double longitude, String locationSource, String photoPath) {
        this.id = required(id, "id", 1, 100); this.ownerId = required(ownerId, "owner_id", 1, 100);
        this.title = required(title, "title", 2, 160); this.location = required(location, "location", 1, 300);
        if (area == null || area.signum() <= 0 || area.compareTo(new BigDecimal("1000000000")) > 0 || area.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException("area");
        this.area = area;
        if (price < 1 || price > 1000000000L) throw new IllegalArgumentException("price");
        this.price = price; this.description = description == null ? "" : description;
        if (this.description.length() > 5000) throw new IllegalArgumentException("description");
        if (!Arrays.asList("sale", "rent").contains(purpose)) throw new IllegalArgumentException("purpose");
        if (!Arrays.asList("apartment", "house", "villa").contains(type)) throw new IllegalArgumentException("type");
        if (floors < 1 || floors > 3) throw new IllegalArgumentException("floors");
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude) || Math.abs(latitude) > 90 || Math.abs(longitude) > 180)
            throw new IllegalArgumentException("coordinates");
        if (!Arrays.asList("map", "current").contains(locationSource)) throw new IllegalArgumentException("location_source");
        this.purpose = purpose; this.type = type; this.floors = floors; this.negotiable = negotiable;
        this.latitude = latitude; this.longitude = longitude; this.locationSource = locationSource;
        this.photoPath = photoPath == null ? "" : photoPath;
        if (!this.photoPath.isEmpty() && (!this.photoPath.startsWith(ownerId + "/" + id + "/")
            || !this.photoPath.matches("[A-Za-z0-9._/-]+") || Arrays.asList(this.photoPath.split("/", -1)).stream().anyMatch(part -> part.equals("..") || part.equals(".") || part.isEmpty())))
            throw new IllegalArgumentException("photo_path");
    }
    private static String required(String value, String field, int min, int max) {
        if (value == null || value.trim().length() < min || value.length() > max) throw new IllegalArgumentException(field);
        return value.trim();
    }
    private static String string(Map<String, ?> row, String key) { Object value = row.get(key); return value == null ? "" : value.toString(); }
    public static Listing fromRow(Map<String, ?> row) {
        return new Listing(string(row,"id"), string(row,"owner_id"), string(row,"title"), string(row,"location"),
            new BigDecimal(string(row,"area")), new BigDecimal(string(row,"price")).longValueExact(), string(row,"description"),
            Boolean.TRUE.equals(row.get("negotiable")), string(row,"purpose"), string(row,"type"),
            new BigDecimal(string(row,"floors")).intValueExact(), Double.parseDouble(string(row,"latitude")),
            Double.parseDouble(string(row,"longitude")), string(row,"location_source"), string(row,"photo_path"));
    }
    public Map<String, Object> toRow() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id",id); row.put("owner_id",ownerId); row.put("title",title); row.put("location",location);
        row.put("area",area); row.put("price",price); row.put("description",description); row.put("negotiable",negotiable);
        row.put("purpose",purpose); row.put("type",type); row.put("floors",floors); row.put("latitude",latitude);
        row.put("longitude",longitude); row.put("location_source",locationSource); row.put("photo_path",photoPath);
        return Collections.unmodifiableMap(row);
    }
    public String getId() { return id; } public String getOwnerId() { return ownerId; }
    public String getTitle() { return title; } public String getLocation() { return location; }
    public BigDecimal getArea() { return area; } public long getPrice() { return price; }
    public String getDescription() { return description; } public boolean isNegotiable() { return negotiable; }
    public String getPurpose() { return purpose; } public String getType() { return type; }
    public int getFloors() { return floors; } public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; } public String getLocationSource() { return locationSource; }
    public String getPhotoPath() { return photoPath; }
}
