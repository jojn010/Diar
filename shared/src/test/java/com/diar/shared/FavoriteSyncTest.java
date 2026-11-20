package com.diar.shared;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class FavoriteSyncTest {
    @Test public void preservesNewTapWhileAcknowledgingOnlyTheSentState() {
        var result=FavoriteSync.merge(Set.of(),Map.of("home",false),Set.of("home","from-web"),Map.of("home",true),Set.of());
        assertEquals(Set.of("from-web"),result.getIds());assertEquals(Map.of("home",false),result.getPending());
    }
    @Test public void acknowledgedRemovalAndRemoteChangesConverge() {
        var result=FavoriteSync.merge(Set.of("old-web-home"),Map.of("home",false),Set.of("new-web-home"),Map.of("home",false),Set.of());
        assertEquals(Set.of("new-web-home"),result.getIds());assertTrue(result.getPending().isEmpty());
    }
    @Test public void privateHomesStayLocalAndOfflineAddsSurviveCloudReads() {
        var result=FavoriteSync.merge(Set.of("private","offline-add"),Map.of("offline-add",true),Set.of("web"),Map.of(),Set.of("private"));
        assertEquals(Set.of("private","offline-add","web"),result.getIds());assertEquals(Map.of("offline-add",true),result.getPending());
    }
}
