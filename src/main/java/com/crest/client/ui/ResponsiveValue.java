package com.crest.client.ui;

public class ResponsiveValue<T> {
    private final T xs, sm, md, lg, xl;
    private final T defaultValue;

    public ResponsiveValue(T defaultValue) {
        this(defaultValue, defaultValue, defaultValue, defaultValue, defaultValue, defaultValue);
    }

    public ResponsiveValue(T defaultValue, T xs, T sm, T md, T lg, T xl) {
        this.defaultValue = defaultValue;
        this.xs = xs;
        this.sm = sm;
        this.md = md;
        this.lg = lg;
        this.xl = xl;
    }

    public T get(int screenWidth) {
        if (screenWidth < Breakpoints.XS) return xs != null ? xs : defaultValue;
        if (screenWidth < Breakpoints.SM) return sm != null ? sm : defaultValue;
        if (screenWidth < Breakpoints.MD) return md != null ? md : defaultValue;
        if (screenWidth < Breakpoints.LG) return lg != null ? lg : defaultValue;
        if (screenWidth < Breakpoints.XL) return xl != null ? xl : defaultValue;
        return defaultValue;
    }

    public T get(Breakpoints.Size size) {
        return switch (size) {
            case XS -> xs != null ? xs : defaultValue;
            case SM -> sm != null ? sm : defaultValue;
            case MD -> md != null ? md : defaultValue;
            case LG -> lg != null ? lg : defaultValue;
            case XL, XXL -> xl != null ? xl : defaultValue;
        };
    }

    public static ResponsiveValue<Integer> of(int defaultValue) {
        return new ResponsiveValue<>(defaultValue);
    }

    public static ResponsiveValue<Integer> of(int defaultValue, int xs, int sm, int md, int lg, int xl) {
        return new ResponsiveValue<>(defaultValue, xs, sm, md, lg, xl);
    }

    public static ResponsiveValue<Float> of(float defaultValue) {
        return new ResponsiveValue<>(defaultValue);
    }

    public static ResponsiveValue<Float> of(float defaultValue, float xs, float sm, float md, float lg, float xl) {
        return new ResponsiveValue<>(defaultValue, xs, sm, md, lg, xl);
    }

    public static ResponsiveValue<Boolean> of(boolean defaultValue) {
        return new ResponsiveValue<>(defaultValue);
    }

    public static ResponsiveValue<Boolean> of(boolean defaultValue, boolean xs, boolean sm, boolean md, boolean lg, boolean xl) {
        return new ResponsiveValue<>(defaultValue, xs, sm, md, lg, xl);
    }
}
