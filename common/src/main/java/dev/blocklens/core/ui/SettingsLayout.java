package dev.blocklens.core.ui;

/** Shared GUI coordinate policy, independent of either Minecraft adapter. */
public record SettingsLayout(
        int left, int contentWidth, int listTop, int listBottom, int footerY,
        int rowHeight, int count, int scroll, int maxScroll) {

    public static SettingsLayout create(int width, int height, int count, int requestedScroll) {
        return create(width, height, count, requestedScroll, 48);
    }

    public static SettingsLayout create(
            int width, int height, int count, int requestedScroll, int requestedRowHeight) {
        return create(width, height, count, requestedScroll, requestedRowHeight, false);
    }

    /** Smallest row the list draws; the window is treated as at least tall enough for one. */
    public static final int MIN_ROW_HEIGHT = 42;
    public static final int MIN_WIDTH = 160;

    /** Smallest height at which one row fits between the list top and the footer. */
    public static int minHeight(boolean resourceActions) {
        return listTop(resourceActions) + MIN_ROW_HEIGHT + 22 + 28;
    }

    private static int listTop(boolean resourceActions) {
        return resourceActions ? 88 : 64;
    }

    /**
     * Lays out {@code count} rows in a window of {@code width} x {@code height}. A window smaller than
     * {@link #MIN_WIDTH} x {@link #minHeight} is laid out as if it were that size, so the screen never
     * fails to open; in such a window the controls past its edge are clipped, as in any screen too
     * small for its content. At or above the minimum the first row is visible and every row reachable.
     */
    public static SettingsLayout create(
            int width, int height, int count, int requestedScroll, int requestedRowHeight, boolean resourceActions) {
        if (count < 0 || count > 64) {
            throw new IllegalArgumentException("Settings layout item count is invalid");
        }
        width = Math.max(MIN_WIDTH, width);
        height = Math.max(minHeight(resourceActions), height);
        int contentWidth = Math.min(620, width - 24);
        int footerY = height - 28;
        int listTop = listTop(resourceActions);
        int listBottom = footerY - 22;
        int rowHeight = Math.max(MIN_ROW_HEIGHT, Math.min(requestedRowHeight, listBottom - listTop));
        int maxScroll = Math.max(0, count * rowHeight - (listBottom - listTop));
        return new SettingsLayout((width - contentWidth) / 2, contentWidth, listTop, listBottom,
                footerY, rowHeight, count, Math.clamp(requestedScroll, 0, maxScroll), maxScroll);
    }

    public int nameWidth() {
        return contentWidth - 90;
    }

    public int rowY(int index) {
        return listTop + index * rowHeight - scroll;
    }

    public boolean visible(int index) {
        int y = rowY(index);
        return index >= 0 && index < count && y >= listTop && y + rowHeight <= listBottom;
    }

    public int scrollToReveal(int index) {
        if (index < 0 || index >= count) {
            throw new IllegalArgumentException("Settings row is outside the category");
        }
        int y = rowY(index);
        int requested = scroll;
        if (y < listTop) {
            requested = index * rowHeight;
        } else if (y + rowHeight > listBottom) {
            requested = (index + 1) * rowHeight - (listBottom - listTop);
        }
        return Math.clamp(requested, 0, maxScroll);
    }
}
