package com.diar.shared;

import java.util.*;

/** Resolves cloud snapshots without losing taps made offline or during an in-flight request. */
public final class FavoriteSync {
    private FavoriteSync() { }
    public static final class Result {
        private final Set<String> ids;
        private final Map<String,Boolean> pending;
        Result(Set<String> ids,Map<String,Boolean> pending) {
            this.ids=Collections.unmodifiableSet(ids); this.pending=Collections.unmodifiableMap(pending);
        }
        public Set<String> getIds() { return ids; }
        public Map<String,Boolean> getPending() { return pending; }
    }
    public static Result merge(Set<String> current,Map<String,Boolean> pending,Set<String> remote,
                               Map<String,Boolean> acknowledged,Set<String> privateIds) {
        Map<String,Boolean> remaining=new LinkedHashMap<>(pending);
        acknowledged.forEach((id,saved)-> {if(Objects.equals(remaining.get(id),saved))remaining.remove(id);});
        Set<String> merged=new LinkedHashSet<>(remote);
        for(String id:current)if(privateIds.contains(id))merged.add(id);
        remaining.forEach((id,saved)->{if(saved)merged.add(id);else merged.remove(id);});
        return new Result(merged,remaining);
    }
}
