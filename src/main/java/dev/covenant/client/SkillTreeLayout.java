package dev.covenant.client;

/** Fits all tiers and occupied lanes into one page, independently of Minecraft rendering. */
record SkillTreeLayout(int left, int top, int width, int height, int columns, int rows) {
    private static final int LEVEL_GUTTER = 24;
    private static final int PADDING = 4;

    int nodeSize() { return Math.max(1, Math.min(32, Math.min(columnStep(), rowStep()) - 4)); }
    int x(int column) { return left + LEVEL_GUTTER + column * columnStep() + (columnStep() - nodeSize()) / 2; }
    int y(int row) { return top + PADDING + row * rowStep() + (rowStep() - nodeSize()) / 2; }
    private int columnStep() { return (width - LEVEL_GUTTER - PADDING) / columns; }
    private int rowStep() { return (height - PADDING * 2) / rows; }
}
