package dev.aster.core;

public enum Category {
    MOVEMENT("Movement"), VISUAL("Visual"), INTERFACE("Interface");
    public final String label;
    Category(String label) { this.label = label; }
}
