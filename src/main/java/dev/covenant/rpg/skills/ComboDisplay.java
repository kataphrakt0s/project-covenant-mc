package dev.covenant.rpg.skills;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Unsaved owner-only transition snapshot. Deadlines use the persistent skill clock. */
public record ComboDisplay(long clockOffset, List<Row> rows) {
    public static final ComboDisplay EMPTY = new ComboDisplay(0, List.of());
    public record Row(int combo, int stage, boolean moving, long deadline) {
        public Row {
            if (combo < 0 || combo > 1 || stage < 0 || stage > 2) { throw new IllegalArgumentException("Invalid combo row"); }
        }
    }
    public ComboDisplay {
        rows = List.copyOf(rows);
        if (rows.size() > 2) { throw new IllegalArgumentException("Too many combo rows"); }
    }
    public static final StreamCodec<RegistryFriendlyByteBuf, ComboDisplay> STREAM_CODEC = StreamCodec.of((buf, value) -> {
        buf.writeLong(value.clockOffset);
        buf.writeByte(value.rows.size());
        for (var row : value.rows) {
            buf.writeByte(row.combo); buf.writeByte(row.stage); buf.writeBoolean(row.moving); buf.writeLong(row.deadline);
        }
    }, buf -> {
        long offset = buf.readLong();
        int count = buf.readUnsignedByte();
        if (count > 2) { throw new IllegalArgumentException("Too many combo rows"); }
        var rows = new ArrayList<Row>(count);
        for (int i = 0; i < count; i++) { rows.add(new Row(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readBoolean(), buf.readLong())); }
        return new ComboDisplay(offset, rows);
    });
}
