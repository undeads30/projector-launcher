package com.gathin.tvhome;

/** Main-thread state. A result belongs to one tunnel/profile generation only. */
final class VpnHealthState {
    enum Status { OFF, UNREACHABLE, ONLINE }
    private String identity = "";
    private long generation;
    private boolean healthy;
    void select(String value) {
        if (!identity.equals(value)) {
            identity = value;
            generation++;
            healthy = false;
        }
    }
    long generation() { return generation; }
    void complete(long version, boolean success) {
        if (version == generation && !identity.isEmpty()) healthy = success;
    }
    Status status() { return identity.isEmpty() ? Status.OFF : healthy ? Status.ONLINE : Status.UNREACHABLE; }
}
