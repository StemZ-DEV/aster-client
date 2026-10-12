package dev.aster.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Versioned snapshots contain data only; game effects are applied by the client on its next tick. */
public final class ClientConfig {
    public static final int MAX_PROFILES = 32;
    public static final int MAX_NAME_LENGTH = 24;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LoggerFactory.getLogger("Aster");
    private final Path path;
    private final ModuleRegistry registry;
    private Data data = new Data();
    private String error;
    private boolean writable = true;

    public ClientConfig(Path path, ModuleRegistry registry) {
        this.path = path;
        this.registry = registry;
        installDefaults();
    }

    public void load() {
        if (!Files.exists(path)) return;
        try {
            String contents = Files.readString(path);
            var document = JsonParser.parseString(contents);
            if (document.isJsonObject() && document.getAsJsonObject().has("version")
                && document.getAsJsonObject().get("version").getAsInt() > 1) {
                writable = false;
                error = "Newer settings format. Original file is protected.";
                return;
            }
            Data loaded = GSON.fromJson(document, Data.class);
            if (loaded == null || loaded.version != 1 || loaded.profiles == null || loaded.profiles.isEmpty()
                || loaded.profiles.size() > MAX_PROFILES
                || loaded.activeProfile == null || !loaded.profiles.containsKey(loaded.activeProfile)
                || loaded.profiles.keySet().stream().anyMatch(name -> !validName(name))
                || loaded.profiles.keySet().stream().map(name -> name.toLowerCase(java.util.Locale.ROOT)).distinct().count() != loaded.profiles.size()
                || loaded.profiles.values().stream().anyMatch(profile -> profile == null
                    || profile.values().stream().anyMatch(state -> state == null))) {
                throw new IOException("Invalid or unsupported Aster configuration");
            }
            apply(loaded.profiles.get(loaded.activeProfile));
            data = loaded;
        } catch (Exception exception) {
            error = "Could not load settings. Defaults are active.";
            LOGGER.error("Could not load Aster configuration", exception);
            // Preserve the original so the next change cannot erase a recoverable configuration.
            try {
                Path backup = path.resolveSibling(path.getFileName() + ".backup");
                int suffix = 1;
                while (Files.exists(backup)) backup = path.resolveSibling(path.getFileName() + ".backup." + suffix++);
                Files.copy(path, backup);
            } catch (IOException backupFailure) {
                writable = false;
                error = "Backup failed. Original settings file is protected.";
                LOGGER.error("Could not back up Aster configuration", backupFailure);
            }
        }
    }

    public boolean save() {
        if (!writable) return false;
        data.profiles.put(data.activeProfile, snapshot());
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(data));
            try { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            error = null;
            return true;
        } catch (IOException exception) {
            error = "Settings could not be saved. Check the log.";
            LOGGER.error("Could not save Aster configuration", exception);
            return false;
        }
    }

    public String activeProfile() { return data.activeProfile; }
    public java.util.List<String> profiles() { return java.util.List.copyOf(data.profiles.keySet()); }
    public String error() { return error; }
    public boolean animations() { return data.animations; }
    public void animations(boolean enabled) { data.animations = enabled; save(); }

    public void selectProfile(String name) {
        if (!data.profiles.containsKey(name)) return;
        data.profiles.put(data.activeProfile, snapshot());
        data.activeProfile = name;
        apply(data.profiles.get(name));
        save();
    }

    public String createProfile() {
        if (data.profiles.size() >= MAX_PROFILES) {
            error = "Profile limit reached. Delete a profile first.";
            return null;
        }
        int number = 1;
        while (data.profiles.containsKey("Custom " + number)) number++;
        String name = "Custom " + number;
        data.profiles.put(data.activeProfile, snapshot());
        data.profiles.put(name, snapshot());
        data.activeProfile = name;
        save();
        return name;
    }

    public boolean renameProfile(String currentName, String requestedName) {
        String name = requestedName == null ? "" : requestedName.strip();
        if (!data.profiles.containsKey(currentName) || !validName(name)) {
            error = "Use a name between 1 and 24 characters.";
            return false;
        }
        if (data.profiles.keySet().stream().anyMatch(existing -> !existing.equals(currentName) && existing.equalsIgnoreCase(name))) {
            error = "That profile name is already in use.";
            return false;
        }
        data.profiles.put(data.activeProfile, snapshot());
        Map<String, Map<String, State>> renamed = new LinkedHashMap<>();
        data.profiles.forEach((key, value) -> renamed.put(key.equals(currentName) ? name : key, value));
        data.profiles = renamed;
        if (data.activeProfile.equals(currentName)) data.activeProfile = name;
        save();
        return true;
    }

    public boolean deleteProfile(String name) {
        if (!data.profiles.containsKey(name)) return false;
        if (data.profiles.size() == 1) {
            error = "Keep at least one profile.";
            return false;
        }
        data.profiles.put(data.activeProfile, snapshot());
        data.profiles.remove(name);
        if (data.activeProfile.equals(name)) {
            data.activeProfile = data.profiles.keySet().iterator().next();
            apply(data.profiles.get(data.activeProfile));
        }
        save();
        return true;
    }

    private static boolean validName(String name) {
        return name != null && !name.isBlank() && name.equals(name.strip()) && name.length() <= MAX_NAME_LENGTH
            && name.chars().noneMatch(Character::isISOControl);
    }

    public void resetActiveProfile() {
        for (Module module : registry.all()) {
            module.enabled = module.defaultEnabled;
            module.key = -1;
            module.value = module.defaultValue;
        }
        save();
    }

    private void installDefaults() {
        data.profiles.put("Everyday", snapshot());
        Map<String, State> minimal = snapshot();
        minimal.replaceAll((id, state) -> new State(id.equals("zoom"), state.key, state.value));
        data.profiles.put("Minimal", minimal);
        Map<String, State> movement = snapshot();
        movement.put("sprint", new State(true, -1, 7));
        movement.put("keystrokes", new State(true, -1, 1));
        data.profiles.put("Movement", movement);
    }

    private Map<String, State> snapshot() {
        Map<String, State> snapshot = new LinkedHashMap<>();
        for (Module module : registry.all()) snapshot.put(module.id, new State(module.enabled, module.key, module.value));
        return snapshot;
    }

    private void apply(Map<String, State> states) {
        var assignedKeys = new HashSet<Integer>();
        for (Module module : registry.all()) {
            State state = states.get(module.id);
            module.enabled = state == null ? module.defaultEnabled : state.enabled;
            module.key = state == null || state.key <= 0 || state.key >= 512 || !assignedKeys.add(state.key) ? -1 : state.key;
            module.setValue(state == null ? module.defaultValue : state.value);
        }
    }

    private static final class Data {
        int version = 1;
        String activeProfile = "Everyday";
        boolean animations = true;
        Map<String, Map<String, State>> profiles = new LinkedHashMap<>();
    }
    private record State(boolean enabled, int key, double value) {}
}
