# Design-a-mini-redis
Designing a mini redis

## Result 
This was a written round. I did not hear back from the company. 

## Question

You are required to implement a simplified in-memory database. All operations that should be supported by the database are described below. Solving this task consists of several levels. Subsequent levels are opened when the current level is correctly solved.Plan your design according to the level specifications below:
- **Level 1** : In-memory database should support basic operations to manipulate records, fields and values within fields.
- **Level 2** : In-memory database should support displaying a record's fields based on a filter.
- **Level 3** : In-memory database should support the TTL settings for records.
- **Level 4** : In-memory database should support look back operations to retrieve.


### Level 1

The database stores records identified by a unique key (String). Each record contains multiple field–value pairs, where:

- `field` is of type String
- `value` is of type int

All operations include a strictly increasing timestamp (`int`).  Timestamps are guaranteed to be unique and provided in increasing order.

Implement the following methods:

1. set : `void set(int timestamp, String key, String field, int value)`

- Inserts the field–value pair into the record identified by key. 
- If the record does not exist, create it. 
- If the field already exists in the record, replace its value with the new value. 

2. compareAndSet :
`boolean compareAndSet(
   int timestamp,
   String key,
   String field,
   int expectedValue,
   int newValue
)`

- Updates the value of field in record key to newValue 
- Only if the current value equals expectedValue 
- If the key or field does not exist, do nothing 
- Returns true if the update occurred, otherwise false

3. compareAndDelete
`boolean compareAndDelete(
   int timestamp,
   String key,
   String field,
   int expectedValue
   )`

- Removes field from record key 
- Only if the current value equals expectedValue 
- If the key or field does not exist, do nothing 
- Returns true if deletion occurred, otherwise false

4. get
`Optional<Integer> get(int timestamp, String key, String field)`

- Returns the value of field in record key 
- If the record or field does not exist, return Optional.empty()


### Level 2 - Field Scanning

Extend the database to support retrieving multiple fields.

5. scan : `List<String> scan(int timestamp, String key)`

- Returns a list of strings representing all fields in record key 
- Each entry must be formatted as: `"<field>(<value>)"`
- Fields must be sorted in lexicographical order 
- If the record does not exist, return an empty list

6. scanByPrefix : `List<String> scanByPrefix(
   int timestamp,
   String key,
   String prefix
   )`

- Same as scan 
- Only include fields whose names start with prefix 
- Fields must be sorted lexicographically

### Level 3 - Time-To-Live (TTL)

Extend the database to support expiration of field values. A value with TTL is valid only during the interval:

`[timestamp, timestamp + ttl)`

The end of the interval is **exclusive**. It is guaranteed that `ttl > 0.`

7. setWithTTL
`   void setWithTTL(
   int timestamp,
   String key,
   String field,
   int value,
   int ttl
   )`

- Inserts the field–value pair and assigns it a TTL 
- If the field already exists, update both its value and TTL

8. compareAndSetWithTTL
`   boolean compareAndSetWithTTL(
   int timestamp,
   String key,
   String field,
   int expectedValue,
   int newValue,
   int ttl
   )`

- Same as compareAndSet 
- If update occurs, assign the new TTL to the value 
- Expired values behave as if they do not exist.

### Level 4 - Historical Look-Back

Extend the database to support querying past values.

9. getWhen
   `Optional<Integer> getWhen(
   int timestamp,
   String key,
   String field,
   int atTimestamp
   )`

- Returns the value of field at time atTimestamp 
- If atTimestamp == 0, perform the normal get operation 
- It is guaranteed that atTimestamp <= timestamp 
- If the field did not exist at that time, return Optional.empty()

TTL rules must apply at atTimestamp.