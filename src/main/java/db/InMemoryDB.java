package db;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;


class Entry {
    int value;
    int expiresAt;
    int timestamp;

    Entry(int val, int expiry,int timestamp){
        this.value = val;
        this.expiresAt = expiry;
        this.timestamp = timestamp;
    }
}

public class InMemoryDB implements MiniRedis {

    // Outer key = record key, inner key = field name, value = field value
    private final Map<String, Map<String, List<Entry>>> store = new HashMap<>();

    boolean isExpired(Entry entry, int timestamp){
        return (entry.expiresAt != -1 && timestamp >= entry.expiresAt);
    }
    @Override
    public void set(int timestamp, String key, String field, int value) {
        setWithTTL(timestamp,key,field,value,-1);
//        store.computeIfAbsent(key, k -> new HashMap<>()).put(field, value);
    }

    @Override
    public Optional<Integer> get(int timestamp, String key, String field) {
        Map<String, List<Entry>> record = store.get(key);
        if (record == null) return Optional.empty();
        List<Entry> entries = record.get(field);
        if(entries == null || entries.size() == 0 || isExpired(entries.get(entries.size()-1),timestamp)) return Optional.empty();
        return Optional.of(entries.get(entries.size()-1).value);
    }

    // If the key is not present, do nothing return false.
    // If key is present but field is not present, do nothing return false
    // if key is present, field is present but value != expectedValue return false
    // else return true.
    // Mental note: These are basis of lock free algorithms, as we first compare then replace we do't need to have locks.
    // In redis - GETSET and LuaScripts serve a similar atomic compare swap purpose..
    @Override
    public boolean compareAndSet(int timestamp, String key, String field, int expectedValue, int newValue) {
//        if(!store.containsKey(key)){
//            return false;
//        }
//        Map<String,Integer> record = store.get(key);
//        if(!record.containsKey(field)){
//            return false;
//        }
//        Integer value = record.get(field);
//        if(!Objects.equals(value, expectedValue) ){
//            return false;
//        }
//        record.put(field,newValue);
//        return true;
        return compareAndSetWithTTL(timestamp,key,field,expectedValue,newValue,-1);
    }

    @Override
    public boolean compareAndDelete(int timestamp, String key, String field, int expectedValue) {
        if(!store.containsKey(key)){
            return false;
        }
        Map<String,List<Entry>> record = store.get(key);
        if(!record.containsKey(field)){
            return false;
        }
        List<Entry> entry = record.get(field);
        if(entry == null || entry.size() == 0 || isExpired(entry.get(entry.size()-1),timestamp)) return false;
        if(!Objects.equals(entry.get(entry.size()-1).value, expectedValue)){
            return false;
        }
        record.remove(field);
        return true;
    }

    // If we make the store TreeMap then this one takes O(n) time, but the set and get above becomes O(logn) instead of O(1)
    // with hashmap the scan takes O(nlogn) time, but the get/set take O(1)
    // I choose to keep HashMap because lookups like get/set happens more in redis system then the scans..
    // scanByPrefix usage, so bugs and all can be fixed at one place..
    @Override
    List<String> scan(int timestamp, String key){
        return scanByPrefix(timestamp, key, "");
//        List<String> output = new ArrayList<>();
//        if(!store.containsKey(key)){
//            return output;
//        }
//        Map<String,Integer> record = store.get(key);
//        for(Map.Entry<String,Integer> entry : record.entrySet()){
//            output.add(entry.getKey()+"("+entry.getValue()+")");
//        }
//        Collections.sort(output);
//        return output;
    }

    @Override
    List<String> scanByPrefix(
            int timestamp,
            String key,
            String prefix
    ){
        List<String> output = new ArrayList<>();
        if(!store.containsKey(key)){
            return output;
        }
        Map<String,List<Entry>> record = store.get(key);

        for(Map.Entry<String,List<Entry>> entry : record.entrySet()){
            String field = entry.getKey();
            List<Entry> value = entry.getValue();
            Entry latestEntry = value.get(value.size()-1);
            if(isExpired(latestEntry,timestamp)) continue;
            if(field.startsWith(prefix)){
                output.add(field+"("+latestEntry.value+")");
            }
        }
        Collections.sort(output);
        return output;
    }

    void setWithTTL(
            int timestamp,
            String key,
            String field,
            int value,
            int ttl
    ){
        int expiresAt = (ttl == -1) ? -1 : timestamp + ttl;
        store.computeIfAbsent(key, k -> new HashMap<>())
                .computeIfAbsent(field, f -> new ArrayList<>())
                .add(new Entry(value, expiresAt, timestamp));
    }

    boolean compareAndSetWithTTL(
            int timestamp,
            String key,
            String field,
            int expectedValue,
            int newValue,
            int ttl
    ){
        if(!store.containsKey(key)){
            return false;
        }
        Map<String,List<Entry>> record = store.get(key);
        if(!record.containsKey(field)){
            return false;
        }
        List<Entry> value = record.get(field);
        int expiresAt = (ttl == -1) ? -1 : timestamp + ttl;
        Entry latestEntry = value.get(value.size()-1);
        if(isExpired(latestEntry,timestamp) || !Objects.equals(latestEntry.value, expectedValue) ){
            return false;
        }
        value.add(new Entry(newValue,expiresAt,timestamp));
        return true;
    }

    Optional<Integer> getWhen(
            int timestamp,
            String key,
            String field,
            int atTimestamp
    ){
        if(atTimestamp == 0) return get(timestamp, key, field);
        Map<String, List<Entry>> record = store.get(key);
        if (record == null) return Optional.empty();
        List<Entry> entries = record.get(field);
        if(entries == null || entries.size() == 0) return Optional.empty();
        int size = entries.size();
        int l = 0;
        int r = size-1;
        Entry closestEntry = null;
        while(l<=r){
            int mid = (r+l)/2;
            Entry entry = entries.get(mid);
            if(entry.timestamp <= atTimestamp){
                closestEntry = entry;
                l = mid+1;
            }
            else{
                r = mid -1;
            }
        }
        if(closestEntry == null || isExpired(closestEntry,atTimestamp)){
                return Optional.empty();
            }
        return Optional.of(closestEntry.value);
    }
}
