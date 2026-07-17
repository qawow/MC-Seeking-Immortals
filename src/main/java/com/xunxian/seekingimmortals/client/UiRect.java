package com.xunxian.seekingimmortals.client;

/** Shared axis-aligned rectangle used by journal layouts and list panels. */
public record UiRect(int x, int y, int width, int height) {
    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
    }

    public boolean intersects(UiRect other) {
        return other != null
                && x < other.right()
                && right() > other.x()
                && y < other.bottom()
                && bottom() > other.y();
    }

    public boolean inside(int screenWidth, int screenHeight) {
        return x >= 0 && y >= 0 && right() <= screenWidth && bottom() <= screenHeight
                && width >= 0 && height >= 0;
    }
}
