package dev.covenant.client;

/** Independent bounds/overlap assertions for initial and expanded profession catalogues. */
public final class ProfessionTreeLayoutTest {
    public static void main(String[] args) {
        for (int[] viewport : new int[][] {{280, 96}, {736, 312}, {160, 80}, {400, 220}}) {
            for (int count = 0; count <= 32; count++) {
                var layout = new ProfessionTreeLayout(viewport[0], viewport[1], count);
                for (int i = 0; i < count; i++) {
                    check(layout.width() > 0 && layout.height() > 0, "Visible click target");
                    check(layout.x(i) >= 0 && layout.x(i) + layout.width() <= viewport[0], "Fits horizontally");
                    check(layout.y(i) >= 0 && layout.y(i) + layout.height() <= viewport[1], "Fits vertically");
                    for (int j = 0; j < i; j++) {
                        check(layout.x(i) >= layout.x(j) + layout.width() || layout.x(j) >= layout.x(i) + layout.width()
                                || layout.y(i) >= layout.y(j) + layout.height() || layout.y(j) >= layout.y(i) + layout.height(), "Separate click targets");
                    }
                }
            }
        }
        System.out.println("Profession tree layout checks passed.");
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
