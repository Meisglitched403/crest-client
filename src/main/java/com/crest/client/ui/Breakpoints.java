package com.crest.client.ui;

public class Breakpoints {
    public static final int XS = 480;
    public static final int SM = 768;
    public static final int MD = 1024;
    public static final int LG = 1440;
    public static final int XL = 1920;

    public enum Size {
        XS, SM, MD, LG, XL, XXL
    }

    public static Size getCurrentSize(int screenWidth) {
        if (screenWidth < XS) return Size.XS;
        if (screenWidth < SM) return Size.SM;
        if (screenWidth < MD) return Size.MD;
        if (screenWidth < LG) return Size.LG;
        if (screenWidth < XL) return Size.XL;
        return Size.XXL;
    }

    public static boolean isXs(int screenWidth) { return screenWidth < XS; }
    public static boolean isSm(int screenWidth) { return screenWidth < SM; }
    public static boolean isMd(int screenWidth) { return screenWidth < MD; }
    public static boolean isLg(int screenWidth) { return screenWidth < LG; }
    public static boolean isXl(int screenWidth) { return screenWidth < XL; }

    public static boolean isXsOrSmaller(int screenWidth) { return screenWidth < SM; }
    public static boolean isSmOrSmaller(int screenWidth) { return screenWidth < MD; }
    public static boolean isMdOrSmaller(int screenWidth) { return screenWidth < LG; }
    public static boolean isLgOrSmaller(int screenWidth) { return screenWidth < XL; }

    public static boolean isSmOrLarger(int screenWidth) { return screenWidth >= SM; }
    public static boolean isMdOrLarger(int screenWidth) { return screenWidth >= MD; }
    public static boolean isLgOrLarger(int screenWidth) { return screenWidth >= LG; }
    public static boolean isXlOrLarger(int screenWidth) { return screenWidth >= XL; }

    public interface BreakpointObserver {
        void onBreakpointChanged(Size oldSize, Size newSize, int screenWidth);
    }
}
