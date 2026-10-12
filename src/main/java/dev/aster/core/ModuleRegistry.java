package dev.aster.core;

import java.util.List;
import java.util.Locale;

public final class ModuleRegistry {
    private final List<Module> modules = List.of(
        new Module("sprint", "Auto Sprint", "Keep your momentum. Sprint automatically while moving forward.",
            Category.MOVEMENT, "sprint", false, -1, "Minimum food level", 7, 20, 7),
        new Module("fullbright", "Fullbright", "Bring the dark into focus. Illuminate caves and night scenes.",
            Category.VISUAL, "sun", false, -1, "Brightness", 0, 1, 1),
        new Module("zoom", "Zoom", "A closer look, on demand. Hold C to smoothly narrow your view.",
            Category.VISUAL, "zoom", true, -1, "Magnification", 2, 10, 4),
        new Module("coordinates", "Coordinates", "Find your place. A compact position readout, always in reach.",
            Category.INTERFACE, "compass", true, -1, "HUD scale", 0.75, 1.5, 1),
        new Module("fps", "Performance", "Stay in the know. A clean, live frame-rate indicator.",
            Category.INTERFACE, "activity", true, -1, "HUD scale", 0.75, 1.5, 1),
        new Module("keystrokes", "Keystrokes", "See every input. A minimal movement and mouse-key overlay.",
            Category.INTERFACE, "keyboard", false, -1, "HUD scale", 0.75, 1.5, 1)
    );

    public List<Module> all() { return modules; }
    public Module get(String id) {
        return modules.stream().filter(module -> module.id.equals(id)).findFirst().orElseThrow();
    }
    public long activeCount() { return modules.stream().filter(module -> module.enabled).count(); }
    public List<Module> search(String query, Category category, boolean activeOnly) {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        return modules.stream()
            .filter(module -> category == null || module.category == category)
            .filter(module -> !activeOnly || module.enabled)
            .filter(module -> (module.name + " " + module.description + " " + module.category.label)
                .toLowerCase(Locale.ROOT).contains(needle)).toList();
    }
}
