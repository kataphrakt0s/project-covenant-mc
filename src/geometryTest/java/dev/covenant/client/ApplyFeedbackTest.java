package dev.covenant.client;

import java.util.Map;

/** Checks asynchronous snapshot confirmation, conflict, timeout and per-draft independence. */
public final class ApplyFeedbackTest {
    public static void main(String[] args) {
        var baseline = Map.of("skill", 1);
        var desired = Map.of("skill", 2);
        var feedback = new ApplyFeedback<Map<String, Integer>>();
        feedback.observe(desired, 1);
        check(feedback.state() == ApplyFeedback.State.IDLE, "Initial sync cannot replay Apply success");
        feedback.submit(baseline, desired, 10);
        feedback.observe(baseline, 11);
        check(feedback.waiting(), "Unchanged attachment is not acceptance");
        feedback.edit();
        check(feedback.waiting(), "Editing cannot remove the in-flight guard");
        feedback.observe(Map.copyOf(desired), 12);
        check(feedback.state() == ApplyFeedback.State.APPLIED, "Matching authoritative change confirms");
        feedback.edit();
        check(feedback.state() == ApplyFeedback.State.IDLE, "New draft clears old notice");
        feedback.submit(baseline, desired, 20);
        feedback.observe(Map.of("other", 1), 21);
        check(feedback.state() == ApplyFeedback.State.CHANGED, "Conflicting update cannot show success");
        feedback.submit(baseline, desired, 30);
        feedback.observe(baseline, 129);
        check(feedback.waiting(), "Wait up to the deadline");
        feedback.observe(baseline, 130);
        check(feedback.state() == ApplyFeedback.State.UNCONFIRMED, "Timeout permits retry without success");
        feedback.observe(desired, 131);
        check(feedback.state() == ApplyFeedback.State.UNCONFIRMED, "Late unrelated observation cannot replay success");
        var otherTab = new ApplyFeedback<Map<String, Integer>>();
        otherTab.submit(baseline, desired, 140);
        check(feedback.state() == ApplyFeedback.State.UNCONFIRMED && otherTab.waiting(), "Tab submissions stay independent");
        System.out.println("Authoritative Apply feedback checks passed.");
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
