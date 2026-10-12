package dev.aster.core;

public final class Module {
    public final String id;
    public final String name;
    public final String description;
    public final Category category;
    public final String icon;
    public final String settingLabel;
    public final double minimum;
    public final double maximum;
    public final double defaultValue;
    public final boolean defaultEnabled;
    public boolean enabled;
    public int key;
    public double value;

    public Module(String id, String name, String description, Category category, String icon,
                  boolean enabled, int key, String settingLabel, double minimum, double maximum, double value) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.icon = icon;
        this.enabled = this.defaultEnabled = enabled;
        this.key = key;
        this.settingLabel = settingLabel;
        this.minimum = minimum;
        this.maximum = maximum;
        this.value = this.defaultValue = value;
    }

    public void setValue(double value) {
        this.value = Double.isFinite(value) ? Math.clamp(value, minimum, maximum) : defaultValue;
        if (id.equals("sprint")) this.value = Math.round(this.value);
    }
}
