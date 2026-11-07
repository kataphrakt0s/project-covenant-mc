package dev.covenant.client;

/** Fit-to-page card geometry; higher catalogue counts trade label width for separate click targets. */
final class ProfessionTreeLayout {
    private final int columns, cellWidth, cellHeight, gap;
    ProfessionTreeLayout(int width, int height, int count) {
        if (width < 32 || height < 12 || count < 0 || count > 32) { throw new IllegalArgumentException("Invalid profession viewport"); }
        int maximumRows = Math.max(1, height / 28);
        columns = Math.min(Math.max(1, count), Math.max(2, (count + maximumRows - 1) / maximumRows));
        int rows = Math.max(1, (count + columns - 1) / columns);
        cellWidth = width / columns; cellHeight = height / rows;
        gap = Math.min(5, Math.max(1, cellWidth / 5));
    }
    int x(int index) { return index % columns * cellWidth; }
    int y(int index) { return index / columns * cellHeight; }
    int width() { return Math.max(1, cellWidth - gap); }
    int height() { return Math.max(1, Math.min(28, cellHeight - 3)); }
}
