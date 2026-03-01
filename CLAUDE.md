# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Java implementation of a simplified in-memory database (mini Redis), built as a backend interview exercise. The project is structured as an IntelliJ IDEA project.

## Problem Domain

The database is built incrementally across 4 levels:

- **Level 1**: Basic CRUD — `set`, `get`, `compareAndSet`, `compareAndDelete`
- **Level 2**: Field scanning — `scan` (all fields), `scanByPrefix` (filtered by prefix), both returning `"<field>(<value>)"` strings sorted lexicographically
- **Level 3**: TTL support — `setWithTTL`, `compareAndSetWithTTL`. TTL interval is `[timestamp, timestamp + ttl)` (end exclusive)
- **Level 4**: Historical look-back — `getWhen(timestamp, key, field, atTimestamp)` returns the value at a past timestamp with TTL rules applied at that time

## Key Design Constraints

- All operations carry a strictly increasing `timestamp` (int), guaranteed unique and monotonically increasing
- Records: `Map<String key, Map<String field, value>>`
- Values are `int`; fields and keys are `String`
- `compareAndSet`/`compareAndDelete` are no-ops (return false) if key or field doesn't exist
- `scan`/`scanByPrefix` return empty list for missing keys
- TTL expiry: a value set at `t` with TTL `n` is valid for `t <= query_time < t + n`
- `getWhen` with `atTimestamp == 0` falls back to normal `get`

## Architecture Notes

When implementing, the natural data structure evolution is:
1. `HashMap<String, HashMap<String, Integer>>` for Levels 1–2
2. Wrap the value in a record/class (e.g., `Entry { int value; int expiresAt; }`) for Level 3 TTL support
3. Store a `List<VersionedEntry>` per field (keyed by timestamp) for Level 4 historical queries — each entry needs both its set-timestamp and its TTL to answer `getWhen` correctly