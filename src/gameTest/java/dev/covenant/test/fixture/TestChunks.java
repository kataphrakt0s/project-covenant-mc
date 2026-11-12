package dev.covenant.test.fixture;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

/** Bounded, fixture-owned simulation tickets. Never referenced by production gameplay. */
public final class TestChunks implements AutoCloseable {
    private static final int RADIUS = 2;
    private final GameTestHelper helper;
    private final TicketType ticket = new TicketType(TicketType.NO_TIMEOUT,
            TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private final Set<ChunkPos> chunks = new LinkedHashSet<>();
    private boolean closed;

    public TestChunks(GameTestHelper helper) {
        this.helper = helper;
        TestCleanup.onFinish(helper, this::close);
    }

    /** Absolute inclusive block bounds; coordinates may cross chunk edges or be negative. */
    public void load(double minX, double minZ, double maxX, double maxZ) {
        if (minX > maxX || minZ > maxZ) { throw new IllegalArgumentException("Reversed fixture bounds"); }
        for (int x = Mth.floor(minX) >> 4; x <= Mth.floor(maxX) >> 4; x++) {
            for (int z = Mth.floor(minZ) >> 4; z <= Mth.floor(maxZ) >> 4; z++) {
                load(new ChunkPos(x, z));
            }
        }
    }

    public void load(ChunkPos chunk) {
        if (closed) { throw new IllegalStateException("Fixture chunks already closed"); }
        if (chunks.add(chunk)) {
            var loaded = helper.getLevel().getChunkSource().addTicketAndLoadWithRadius(ticket, chunk, RADIUS);
            // Headless GameTests tick faster than background chunk loading; wait for actual loading work.
            helper.getLevel().getServer().managedBlock(loaded::isDone);
            loaded.join();
        }
    }

    /** Use in thenWaitUntil before starting gameplay deadlines; loading alone does not imply entity ticking. */
    public void assertReady() {
        helper.assertTrue(!chunks.isEmpty(), "Fixture must request its chunk area before waiting for readiness");
        for (var chunk : chunks) {
            helper.assertTrue(helper.getLevel().areEntitiesActuallyLoadedAndTicking(chunk),
                    "Fixture chunk " + chunk + " must be entity-ticking");
        }
    }

    @Override public void close() {
        if (closed) { return; }
        closed = true;
        chunks.forEach(chunk -> helper.getLevel().getChunkSource().removeTicketWithRadius(ticket, chunk, RADIUS));
        chunks.clear();
    }
}
