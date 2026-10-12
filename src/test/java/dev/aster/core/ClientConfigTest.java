package dev.aster.core;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientConfigTest {
    @TempDir Path directory;

    @Test void settingsAndBindingsSurviveRestart() {
        ModuleRegistry modules=new ModuleRegistry();
        ClientConfig config=new ClientConfig(directory.resolve("client.json"),modules);
        modules.get("sprint").enabled=true;
        modules.get("sprint").key=71;
        modules.get("zoom").setValue(7.5);
        assertTrue(config.save());
        ModuleRegistry restarted=new ModuleRegistry();
        new ClientConfig(directory.resolve("client.json"),restarted).load();
        assertTrue(restarted.get("sprint").enabled);
        assertEquals(71,restarted.get("sprint").key);
        assertEquals(7.5,restarted.get("zoom").value);
    }

    @Test void switchingProfilesPreservesIndependentSettings() {
        ModuleRegistry modules=new ModuleRegistry();
        ClientConfig config=new ClientConfig(directory.resolve("client.json"),modules);
        modules.get("fullbright").enabled=true;
        config.selectProfile("Minimal");
        assertFalse(modules.get("fullbright").enabled);
        assertFalse(modules.get("fps").enabled);
        config.selectProfile("Everyday");
        assertTrue(modules.get("fullbright").enabled);
        assertTrue(modules.get("fps").enabled);
        String name=config.createProfile();
        modules.get("zoom").setValue(8);
        config.selectProfile("Everyday");
        assertEquals(4,modules.get("zoom").value);
        config.selectProfile(name);
        assertEquals(8,modules.get("zoom").value);
    }

    @Test void corruptConfigurationIsPreservedAndDefaultsRemainUsable() throws Exception {
        Path file=directory.resolve("client.json");Files.writeString(file,"{broken JSON");
        ModuleRegistry modules=new ModuleRegistry();ClientConfig config=new ClientConfig(file,modules);
        assertDoesNotThrow(config::load);
        assertNotNull(config.error());
        assertEquals("{broken JSON",Files.readString(directory.resolve("client.json.backup")));
        assertTrue(modules.get("zoom").enabled);
        assertTrue(config.save());
        assertNull(config.error());
    }

    @Test void invalidValuesCannotEscapeModuleLimits() {
        Module module=new ModuleRegistry().get("zoom");
        module.setValue(1000);assertEquals(10,module.value);
        module.setValue(-1);assertEquals(2,module.value);
        module.setValue(Double.NaN);assertEquals(4,module.value);
    }

    @Test void searchCombinesCategoryAndEnabledFilters() {
        ModuleRegistry modules=new ModuleRegistry();
        assertEquals(1,modules.search("  ZOOM ",Category.VISUAL,true).size());
        assertTrue(modules.search("sprint",Category.VISUAL,false).isEmpty());
        assertEquals(3,modules.search("",null,true).size());
    }

    @Test void namedProfilesPersistAndRemainIndependent() {
        Path file=directory.resolve("client.json");
        ModuleRegistry modules=new ModuleRegistry();
        ClientConfig config=new ClientConfig(file,modules);
        config.createProfile();
        modules.get("zoom").setValue(8);
        assertTrue(config.renameProfile("Custom 1","  Night Build  "));
        assertEquals("Night Build",config.activeProfile());
        assertFalse(config.renameProfile("Night Build","minimal"));
        assertFalse(config.renameProfile("Night Build","\n"));
        assertFalse(config.renameProfile("Night Build","x".repeat(25)));
        config.selectProfile("Everyday");
        assertEquals(4,modules.get("zoom").value);
        config.selectProfile("Night Build");
        ModuleRegistry restarted=new ModuleRegistry();
        ClientConfig reloaded=new ClientConfig(file,restarted);
        reloaded.load();
        assertEquals("Night Build",reloaded.activeProfile());
        assertEquals(8,restarted.get("zoom").value);
        assertEquals(java.util.List.of("Everyday","Minimal","Movement","Night Build"),reloaded.profiles());
    }

    @Test void deletingActiveProfileRestoresFallbackAndProtectsLastProfile() {
        ModuleRegistry modules=new ModuleRegistry();
        ClientConfig config=new ClientConfig(directory.resolve("client.json"),modules);
        config.selectProfile("Minimal");
        modules.get("zoom").setValue(9);
        assertTrue(config.deleteProfile("Minimal"));
        assertEquals("Everyday",config.activeProfile());
        assertEquals(4,modules.get("zoom").value);
        assertTrue(modules.get("fps").enabled);
        assertTrue(config.deleteProfile("Movement"));
        assertFalse(config.deleteProfile("Everyday"));
        assertEquals(1,config.profiles().size());
    }

    @Test void profilesHaveABoundedCount() {
        ClientConfig config=new ClientConfig(directory.resolve("client.json"),new ModuleRegistry());
        while(config.profiles().size()<ClientConfig.MAX_PROFILES)assertNotNull(config.createProfile());
        assertNull(config.createProfile());
        assertEquals(ClientConfig.MAX_PROFILES,config.profiles().size());
    }

    @Test void newerFormatsAreNeverOverwritten() throws Exception {
        Path file=directory.resolve("client.json");
        String future="{\"version\":2,\"profiles\":\"a future layout\"}";
        Files.writeString(file,future);
        ClientConfig config=new ClientConfig(file,new ModuleRegistry());
        config.load();
        assertNotNull(config.error());
        assertFalse(config.save());
        config.createProfile();
        assertEquals(future,Files.readString(file));
    }

    @Test void recoveryNeverReplacesAnEarlierBackup() throws Exception {
        Path file=directory.resolve("client.json");
        Files.writeString(file,"{bad config");
        Files.writeString(directory.resolve("client.json.backup"),"the original backup");
        ClientConfig config=new ClientConfig(file,new ModuleRegistry());
        config.load();
        assertTrue(config.save());
        assertEquals("the original backup",Files.readString(directory.resolve("client.json.backup")));
        assertEquals("{bad config",Files.readString(directory.resolve("client.json.backup.1")));
    }

    @Test void importedBindingsAreValidatedAndValuesAreClamped() throws Exception {
        Path file=directory.resolve("client.json");
        Files.writeString(file,"""
            {"version":1,"activeProfile":"Imported","profiles":{"Imported":{
              "sprint":{"enabled":true,"key":71,"value":6},
              "fullbright":{"enabled":true,"key":71,"value":100},
              "zoom":{"enabled":true,"key":512,"value":0},
              "fps":{"enabled":true,"key":0,"value":2}
            }}}
            """);
        ModuleRegistry modules=new ModuleRegistry();
        new ClientConfig(file,modules).load();
        assertEquals(71,modules.get("sprint").key);
        assertEquals(-1,modules.get("fullbright").key);
        assertEquals(-1,modules.get("zoom").key);
        assertEquals(-1,modules.get("fps").key);
        assertEquals(7,modules.get("sprint").value);
        assertEquals(1,modules.get("fullbright").value);
        assertEquals(2,modules.get("zoom").value);
        assertEquals(1.5,modules.get("fps").value);
    }

    @Test void failedWritesRemainVisibleWithoutThrowing() throws Exception {
        Path parent=directory.resolve("blocked");
        Files.writeString(parent,"a file, not a directory");
        ClientConfig config=new ClientConfig(parent.resolve("client.json"),new ModuleRegistry());
        assertFalse(config.save());
        assertNotNull(config.error());
    }

    @Test void sprintThresholdUsesWholeFoodPoints() {
        Module sprint=new ModuleRegistry().get("sprint");
        sprint.setValue(12.7);
        assertEquals(13,sprint.value);
        sprint.setValue(6);
        assertEquals(7,sprint.value);
    }
}
