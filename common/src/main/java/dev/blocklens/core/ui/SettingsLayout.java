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
        if (width < 160 || height < 140 || count < 0 || count > 64) {
            throw new IllegalArgumentException("Settings layout dimensions or item count are invalid");
        }
        int contentWidth = Math.min(620, width - 24);
        int footerY = height - 28;
        int listTop = 64;
        int listBottom = footerY - 22;
        int rowHeight = Math.max(42, Math.min(requestedRowHeight, listBottom - listTop));
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
