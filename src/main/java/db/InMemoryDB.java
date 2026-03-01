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

    @Override
    public boolean compareAndSet(int timestamp, String key, String field, int expectedValue, int newValue) {
        // TODO(human): implement compareAndSet
        return false;
    }

    @Override
    public boolean compareAndDelete(int timestamp, String key, String field, int expectedValue) {
        // TODO(human): implement compareAndDelete
        return false;
    }
}
