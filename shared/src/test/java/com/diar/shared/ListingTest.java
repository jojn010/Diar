package com.diar.shared;

import java.math.BigDecimal;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ListingTest {
    private Map<String,Object> row() {
        Map<String,Object> row = new LinkedHashMap<>();
        row.put("id","4f4bc2e2-3e24-4e44-9450-c8a1cd0d095a"); row.put("owner_id","f592f18e-0225-49f4-b012-9d9e419ae709");
        row.put("title","بيت الحديقة"); row.put("location","القدس"); row.put("area",new BigDecimal("125.50"));
        row.put("price",73500); row.put("negotiable",true); row.put("purpose","sale"); row.put("type","house");
        row.put("floors",2); row.put("latitude",31.78); row.put("longitude",35.22); row.put("location_source","map");
        row.put("photo_path",""); row.put("description",""); return row;
    }
    @Test public void arabicAndExactNumbersRoundTripToDatabaseContract() {
        var listing=Listing.fromRow(row());
        assertEquals("بيت الحديقة",listing.getTitle()); assertEquals(73500,listing.getPrice());
        assertEquals(new BigDecimal("125.50"),listing.getArea());
        assertEquals(row().get("location"),Listing.fromRow(listing.toRow()).getLocation());
        assertEquals(15,listing.toRow().size());
    }
    @Test public void rejectsFractionalPricesInvalidPinsAndAnotherOwnersPhoto() {
        for (var entry:Arrays.asList(new AbstractMap.SimpleEntry<String,Object>("price",73500.5),new AbstractMap.SimpleEntry<String,Object>("area","1.001"),new AbstractMap.SimpleEntry<String,Object>("latitude",Double.NaN),new AbstractMap.SimpleEntry<String,Object>("longitude",181),new AbstractMap.SimpleEntry<String,Object>("photo_path","someone/else/photo.jpg"),new AbstractMap.SimpleEntry<String,Object>("floors",4))) {
            var row=row();row.put(entry.getKey(),entry.getValue());
            assertThrows(RuntimeException.class,()->Listing.fromRow(row));
        }
    }
    @Test public void favoriteUsesAccountAndListingCompositeKey() {
        var row=row(); var favorite=new Favorite(row.get("owner_id").toString(),row.get("id").toString());
        assertEquals(row.get("owner_id"),favorite.toRow().get("user_id"));
        assertEquals(row.get("id"),favorite.toRow().get("listing_id"));
        assertThrows(IllegalArgumentException.class,()->new Favorite("not-a-user",row.get("id").toString()));
    }
}
