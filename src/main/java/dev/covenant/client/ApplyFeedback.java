package dev.covenant.client;

/** Per-window submission state. Only a matching authoritative change can confirm Apply. */
final class ApplyFeedback<T> {
    enum State { IDLE, WAITING, APPLIED, CHANGED, UNCONFIRMED }
    private T expected, submitted;
    private long deadline;
    private State state = State.IDLE;

    void submit(T baseline, T desired, long tick) {
        expected = baseline; submitted = desired; deadline = tick + 100; state = State.WAITING;
    }
    void observe(T actual, long tick) {
        if (state != State.WAITING) { return; }
        if (!actual.equals(expected)) {
            state = actual.equals(submitted) ? State.APPLIED : State.CHANGED;
        } else if (tick >= deadline) { state = State.UNCONFIRMED; }
    }
    void edit() { if (state != State.WAITING) { state = State.IDLE; } }
    State state() { return state; }
    boolean waiting() { return state == State.WAITING; }
    String key() { return "ui.covenant.apply." + state.name().toLowerCase(java.util.Locale.ROOT); }
}
