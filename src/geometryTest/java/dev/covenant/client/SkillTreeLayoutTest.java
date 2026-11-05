package dev.covenant.client;

/** Checks that every skill remains visible and separately clickable without scrolling after resizing. */
public final class SkillTreeLayoutTest {
    public static void main(String[] args) {
        // Tree viewports at minimum Minecraft GUI size, common scales, wide screens, and portrait windows.
        for (int[] viewport : new int[][] {{204, 151}, {311, 151}, {524, 271}, {780, 417}, {124, 231}}) {
            for (int columns : new int[] {1, 5, 6, 8}) {
                var layout = new SkillTreeLayout(17, 29, viewport[0], viewport[1], columns, 8);
                int size = layout.nodeSize();
                check(size > 0, "Positive node size");
                for (int row = 0; row < 8; row++) {
                    for (int column = 0; column < columns; column++) {
                        int x = layout.x(column);
                        int y = layout.y(row);
                        check(x >= 17 + 24 && x + size <= 17 + viewport[0], "All nodes fit horizontally, clear of tier labels");
                        check(y >= 29 && y + size <= 29 + viewport[1], "All eight tiers fit vertically");
                        if (column > 0) {
                            check(layout.x(column - 1) + size < x, "Adjacent lanes have separate click targets");
                        }
                        if (row > 0) {
                            check(layout.y(row - 1) + size < y, "Higher tiers stay above lower tiers with room for connections");
                        }
                    }
                }
            }
        }
        System.out.println("Full skill tree layout checks passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
