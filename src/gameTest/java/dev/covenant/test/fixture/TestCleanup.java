package dev.covenant.test.fixture;

import java.lang.reflect.Field;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;

/** Registers an idempotent cleanup for success, assertion failure and timeout (not just the last tick). */
public final class TestCleanup {
    private static final Field TEST_INFO;

    static {
        try {
            // 26.1.2 exposes listeners on GameTestInfo but no accessor on GameTestHelper.
            // Keep this pinned API bridge here, rather than spreading reflection through fixtures.
            TEST_INFO = GameTestHelper.class.getDeclaredField("testInfo");
            TEST_INFO.setAccessible(true);
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private TestCleanup() {}

    public static void onFinish(GameTestHelper helper, Runnable cleanup) {
        try {
            ((GameTestInfo) TEST_INFO.get(helper)).addListener(new GameTestListener() {
                private boolean closed;
                private void close() {
                    if (!closed) {
                        closed = true;
                        cleanup.run();
                    }
                }
                @Override public void testStructureLoaded(GameTestInfo info) {}
                @Override public void testPassed(GameTestInfo info, GameTestRunner runner) { close(); }
                @Override public void testFailed(GameTestInfo info, GameTestRunner runner) { close(); }
                @Override public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) { close(); }
            });
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Cannot attach test cleanup to pinned GameTestHelper", error);
        }
    }
}
