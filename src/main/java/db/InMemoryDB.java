package db;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryDB implements MiniRedis {

    // Outer key = record key, inner key = field name, value = field value
    private final Map<String, Map<String, Integer>> store = new HashMap<>();

    @Override
    public void set(int timestamp, String key, String field, int value) {
        store.computeIfAbsent(key, k -> new HashMap<>()).put(field, value);
    }

    @Override
    public Optional<Integer> get(int timestamp, String key, String field) {
        Map<String, Integer> record = store.get(key);
        if (record == null) return Optional.empty();
        Integer value = record.get(field);
        return Optional.ofNullable(value);
    }

    // If the key is not present, do nothing return false.
    // If key is present but field is not present, do nothing return false
    // if key is present, field is present but value != expectedValue return false
    // else return true.
    // Mental note: These are basis of lock free algorithms, as we first compare then replace we do't need to have locks.
    // In redis - GETSET and LuaScripts serve a similar atomic compare swap purpose..
    @Override
    public boolean compareAndSet(int timestamp, String key, String field, int expectedValue, int newValue) {
        Map<String,Integer> record = store.get(key);
        if(record == null){
            System.out.println("Key does not exist in store");
            return false;
        }
        int value = record.get(field);
        if(value == null){
            System.out.println("Field does not exist in record");
            return false;
        }
        if(value != expectedValue){
            System.out.println("Value is not equal to expectedValue");
            return false;
        }
        record.put(field,newValue);
        store.put(key,record);
        return true;
    }

    @Override
    public boolean compareAndDelete(int timestamp, String key, String field, int expectedValue) {
        Map<String,Integer> record = store.get(key);
        if(record == null){
            System.out.println("Key does not exist in store");
            return false;
        }
        int value = record.get(field);
        if(value == null){
            System.out.println("Field does not exist in record");
            return false;
        }
        if(value != expectedValue){
            System.out.println("Value is not equal to expectedValue");
            return false;
        }
        record.remove(field);
        store.put(key,record);
        return true;
    }
}
