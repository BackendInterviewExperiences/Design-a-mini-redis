package db;

import java.util.Optional;

public interface MiniRedis {

    // Level 1
    void set(int timestamp, String key, String field, int value);

    boolean compareAndSet(int timestamp, String key, String field, int expectedValue, int newValue);

    boolean compareAndDelete(int timestamp, String key, String field, int expectedValue);

    Optional<Integer> get(int timestamp, String key, String field);
}
