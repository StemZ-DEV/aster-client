package dev.aster.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.aster.client.AsterClient;
import dev.aster.core.Category;
import dev.aster.core.ClientConfig;
import dev.aster.core.Module;
import dev.aster.core.ModuleRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** A frameless command rail, composited over the world or the screen that opened it. */
public final class AsterScreen extends Screen {
    private static final int WIDTH = 560;
    private static final int ROW_HEIGHT = 44;
    private static final int LIST_TOP = 216;
    private static final int DETAIL_HEIGHT = 104;
    private final Screen parent;
    private final ModuleRegistry registry = AsterClient.modules();
    private final ClientConfig config = AsterClient.config();
    private final Map<String, Float> animations = new HashMap<>();
    private Module selected = registry.get("zoom");
    private Module expanded;
    private Category category;
    private String tab = "modules";
    private String query = "";
    private boolean activeOnly;
    private boolean capturing;
    private Module bindingTarget;
    private String bindingHint = "Press a key...";
    private String pendingDelete;
    private boolean confirmingReset;
    private EditBox search;
    private EditBox profileName;
    private float scale;
    private float offsetX;
    private float offsetY;
    private int railHeight;
    private int rowScroll;
    private int profileScroll;
    private long lastFrame = System.nanoTime();

    public AsterScreen(Screen parent) {
        super(Component.literal("Aster command rail"));
        this.parent = parent;
        for (Module module : registry.all()) animations.put(module.id, module.enabled ? 1f : 0f);
    }

    @Override
    protected void init() {
        scale = Math.min(width / 1120f, (height - 16f) / 560f);
        railHeight = Math.min(650, (int) ((height - 16f) / scale));
        offsetX = width * .045f;
        offsetY = (height - railHeight * scale) / 2;
        search = new EditBox(font, 31, 85, 480, 24, Ui.component("Search modules"));
        search.setBordered(false);
        search.setTextShadow(false);
        search.setTextColor(Ui.TEXT);
        search.setMaxLength(64);
        search.setHint(Ui.component("Find a module..."));
        search.addFormatter((text, offset) -> FormattedCharSequence.forward(text, Ui.FONT_STYLE));
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            rowScroll = 0;
            selectFirstResult();
            rebuildControls();
        });
        rebuildControls();
        focusSearch();
    }

    private void rebuildControls() {
        boolean searchFocused = search != null && search.isFocused();
        clearFocus();
        clearWidgets();
        search.visible = tab.equals("modules");
        addRenderableWidget(search);
        tabButton(0, 102, "Modules", "modules");
        tabButton(126, 98, "Profiles", "profiles");
        tabButton(248, 102, "Settings", "settings");
        addRenderableWidget(new AsterButton(528, 5, 28, 28, "Close Aster", this::onClose,
            (g, button) -> Ui.icon(g, "close", 534, 11, 16, button.isHoveredOrFocused() ? Ui.TEXT : Ui.DIM)));
        switch (tab) {
            case "modules" -> moduleControls();
            case "profiles" -> profileControls();
            default -> settingsControls();
        }
        if (searchFocused && search.visible) focusSearch();
    }

    private void tabButton(int x, int width, String label, String destination) {
        addRenderableWidget(new AsterButton(x, 132, width, 30, label, () -> {
            tab = destination;
            capturing = false;
            pendingDelete = null;
            confirmingReset = false;
            rowScroll = 0;
            rebuildControls();
        }, (g, button) -> {
            boolean current = tab.equals(destination);
            Ui.text(g, label, x, 139, current ? Ui.TEXT : button.isHoveredOrFocused() ? Ui.MUTED : Ui.DIM, 1.05f);
            if (current) g.fill(x, 161, x + Ui.width(label) + 10, 163, Ui.ACCENT);
        }));
    }

    private void categoryButton(int x, int width, String label, Category destination) {
        addRenderableWidget(new AsterButton(x, 179, width, 24, label, () -> {
            category = destination;
            rowScroll = 0;
            selectFirstResult();
            rebuildControls();
        }, (g, button) -> Ui.text(g, label, x, 186,
            category == destination ? Ui.ACCENT_TEXT : button.isHoveredOrFocused() ? Ui.TEXT : Ui.DIM, .9f)));
    }

    private List<Module> results() { return registry.search(query, category, activeOnly); }

    private void selectFirstResult() {
        List<Module> visible = results();
        if (!visible.contains(selected) && !visible.isEmpty()) selected = visible.getFirst();
        if (expanded != null && !visible.contains(expanded)) expanded = null;
    }

    private int visibleRowCount() {
        int detail = expanded != null && results().contains(expanded) ? DETAIL_HEIGHT : 0;
        return Math.max(1, (railHeight - LIST_TOP - 58 - detail) / ROW_HEIGHT);
    }

    private void moduleControls() {
        categoryButton(0, 52, "All", null);
        categoryButton(67, 96, "Movement", Category.MOVEMENT);
        categoryButton(180, 72, "Visual", Category.VISUAL);
        categoryButton(271, 98, "Interface", Category.INTERFACE);
        addRenderableWidget(new AsterButton(447, 179, 111, 24, "Show enabled modules", () -> {
            activeOnly = !activeOnly;
            rowScroll = 0;
            selectFirstResult();
            rebuildControls();
        }, (g, button) -> {
            Ui.circle(g, 449, 188, 5, activeOnly ? Ui.ACCENT : Ui.DIM);
            Ui.text(g, "Enabled only", 462, 186, activeOnly ? Ui.ACCENT_TEXT : Ui.DIM, .85f);
        }));
        List<Module> visible = results();
        int count = visibleRowCount();
        rowScroll = Math.clamp(rowScroll, 0, Math.max(0, visible.size() - count));
        int y = LIST_TOP;
        for (int index = rowScroll; index < Math.min(visible.size(), rowScroll + count); index++) {
            Module module = visible.get(index);
            int rowY = y;
            int number = index + 1;
            addRenderableWidget(new AsterButton(0, rowY, 510, ROW_HEIGHT, "Toggle " + module.name, () -> {
                selected = module;
                toggle(module);
            }, (g, button) -> drawRow(g, module, number, rowY, button.isHoveredOrFocused())));
            addRenderableWidget(new AsterButton(526, rowY + 7, 30, 30, "Tune " + module.name,
                () -> expand(module), (g, button) -> Ui.icon(g, "sliders", 534, rowY + 14, 16,
                    expanded == module ? Ui.ACCENT_TEXT : button.isHoveredOrFocused() ? Ui.TEXT : Ui.DIM)));
            y += ROW_HEIGHT;
            if (expanded == module) {
                detailControls(module, y);
                y += DETAIL_HEIGHT;
            }
        }
    }

    private void drawRow(GuiGraphicsExtractor g, Module module, int number, int y, boolean hovered) {
        if (module == selected) g.fill(0, y + 9, 2, y + 31, Ui.ACCENT);
        Ui.text(g, String.format(Locale.ROOT, "%02d", number), 9, y + 16, module == selected ? Ui.ACCENT_TEXT : Ui.DIM, .8f);
        Ui.text(g, module.name, 39, y + 13, hovered || module == selected ? Ui.TEXT : Ui.MUTED, 1.15f);
        Ui.text(g, module.category.label.toUpperCase(Locale.ROOT), 275, y + 16, Ui.DIM, .72f);
        if (module.key >= 0) Ui.text(g, keyName(module.key), 389, y + 16, Ui.MUTED, .8f);
        Ui.toggle(g, 472, y + 12, animations.get(module.id));
        g.fill(39, y + ROW_HEIGHT - 1, WIDTH, y + ROW_HEIGHT, 0x553A3A3A);
    }

    private void detailControls(Module module, int y) {
        addRenderableOnly((g, mouseX, mouseY, delta) -> {
            String description = module.description.substring(module.description.indexOf('.') + 1).strip();
            if (module.id.equals("zoom")) description = "Hold " + AsterClient.zoomKey().getTranslatedKeyMessage().getString() + " to smoothly narrow your view.";
            Ui.text(g, description, 39, y + 7, Ui.MUTED, .88f);
            Ui.text(g, module.settingLabel, 39, y + 36, Ui.DIM, .85f);
            String value = module.id.equals("sprint") ? String.format(Locale.ROOT, "%.0f", module.value)
                : module.id.equals("fullbright") ? String.format(Locale.ROOT, "%.0f%%", module.value * 100)
                : String.format(Locale.ROOT, "%.1fx", module.value);
            Ui.text(g, value, 320 - Ui.width(value), y + 36, Ui.ACCENT_TEXT, .9f);
            Ui.text(g, "Drag to adjust", 39, y + 80, Ui.DIM, .75f);
        });
        addRenderableWidget(new AsterSlider(39, y + 51, 281, module, config::save));
        addRenderableWidget(new AsterButton(365, y + 48, 155, 28, "Bind " + module.name, () -> {
            selected = module;
            capturing = !capturing;
            bindingTarget = capturing ? module : null;
            bindingHint = "Press a key...";
        }, (g, button) -> {
            Ui.icon(g, "keyboard", 368, y + 55, 15, capturing ? Ui.ACCENT_TEXT : Ui.DIM);
            Ui.text(g, capturing ? bindingHint : module.key < 0 ? "Bind a key" : keyName(module.key), 390, y + 58,
                capturing || button.isHoveredOrFocused() ? Ui.ACCENT_TEXT : Ui.MUTED, .9f);
            if (capturing) g.fill(390, y + 74, 520, y + 75, Ui.ACCENT);
        }));
        addRenderableWidget(new AsterButton(529, y + 49, 26, 26, "Reset " + module.name + " settings", () -> {
            module.setValue(module.defaultValue);
            module.key = -1;
            config.save();
        }, (g, button) -> Ui.text(g, "R", 537, y + 58, button.isHoveredOrFocused() ? Ui.TEXT : Ui.DIM, .85f)));
    }

    private void expand(Module module) {
        selected = module;
        expanded = expanded == module ? null : module;
        capturing = false;
        ensureSelectedVisible();
        rebuildControls();
    }

    private void ensureSelectedVisible() {
        int index = results().indexOf(selected);
        int count = visibleRowCount();
        if (index >= 0) {
            if (index < rowScroll) rowScroll = index;
            if (index >= rowScroll + count) rowScroll = index - count + 1;
        }
    }

    private void profileControls() {
        List<String> profiles = config.profiles();
        int count = Math.max(1, (railHeight - 400) / 57);
        profileScroll = Math.clamp(profileScroll, 0, Math.max(0, profiles.size() - count));
        for (int index = profileScroll; index < Math.min(profiles.size(), profileScroll + count); index++) {
            String name = profiles.get(index);
            int y = LIST_TOP + (index - profileScroll) * 57;
            int number = index + 1;
            addRenderableWidget(new AsterButton(0, y, WIDTH, 55, "Activate " + name, () -> {
                pendingDelete = null;
                config.selectProfile(name);
                rebuildControls();
            }, (g, button) -> {
                boolean current = name.equals(config.activeProfile());
                Ui.text(g, String.format(Locale.ROOT, "%02d", number), 9, y + 17, current ? Ui.ACCENT_TEXT : Ui.DIM, .8f);
                Ui.text(g, name, 39, y + 10, current || button.isHoveredOrFocused() ? Ui.TEXT : Ui.MUTED, 1.15f);
                Ui.text(g, profileDescription(name), 39, y + 32, Ui.DIM, .8f);
                if (current) Ui.text(g, "ACTIVE", 492, y + 19, Ui.ACCENT_TEXT, .8f);
                else Ui.icon(g, "arrow", 533, y + 18, 16, Ui.DIM);
                g.fill(39, y + 54, WIDTH, y + 55, 0x553A3A3A);
            }));
        }
        int editorY = railHeight - 154;
        addRenderableOnly((g, mouseX, mouseY, delta) -> {
            Ui.text(g, "CURRENT PROFILE", 0, editorY - 20, Ui.ACCENT_TEXT, .75f);
            g.fill(0, editorY + 25, 320, editorY + 26, profileName.isFocused() ? Ui.ACCENT : Ui.BORDER);
        });
        profileName = new EditBox(font, 0, editorY + 5, 318, 22, Ui.component("Profile name"));
        profileName.setBordered(false);
        profileName.setTextShadow(false);
        profileName.setTextColor(Ui.TEXT);
        profileName.setMaxLength(ClientConfig.MAX_NAME_LENGTH);
        profileName.addFormatter((text, offset) -> FormattedCharSequence.forward(text, Ui.FONT_STYLE));
        profileName.setValue(config.activeProfile());
        addRenderableWidget(profileName);
        addRenderableWidget(new AsterButton(365, editorY, 78, 30, "Rename current profile", this::renameCurrentProfile,
            (g, button) -> Ui.text(g, "Rename", 365, editorY + 9, button.isHoveredOrFocused() ? Ui.TEXT : Ui.MUTED, .9f)));
        addRenderableWidget(new AsterButton(465, editorY, 93, 30, "Delete current profile", () -> {
            if (config.activeProfile().equals(pendingDelete)) {
                config.deleteProfile(pendingDelete);
                pendingDelete = null;
                rebuildControls();
            } else pendingDelete = config.activeProfile();
        }, (g, button) -> Ui.text(g, config.activeProfile().equals(pendingDelete) ? "Confirm delete" : "Delete...", 465, editorY + 9,
            pendingDelete != null || button.isHoveredOrFocused() ? Ui.ACCENT_TEXT : Ui.DIM, .9f)));
        addRenderableWidget(new AsterButton(0, railHeight - 91, 235, 32, "Save new profile", () -> {
            pendingDelete = null;
            config.createProfile();
            profileScroll = config.profiles().size();
            rebuildControls();
        }, (g, button) -> {
            Ui.icon(g, "plus", 0, railHeight - 83, 16, Ui.ACCENT_TEXT);
            Ui.text(g, "Save this setup as a new profile", 25, railHeight - 81, button.isHoveredOrFocused() ? Ui.TEXT : Ui.ACCENT_TEXT, .9f);
        }));
    }

    private void renameCurrentProfile() {
        pendingDelete = null;
        if (config.renameProfile(config.activeProfile(), profileName.getValue())) rebuildControls();
    }

    private String profileDescription(String name) {
        return switch (name) {
            case "Everyday" -> "Zoom, coordinates, and performance.";
            case "Minimal" -> "A quieter screen. Only zoom.";
            case "Movement" -> "Sprint and input feedback.";
            default -> "Your saved modules, settings, and shortcuts.";
        };
    }

    private void settingsControls() {
        addRenderableWidget(new AsterButton(0, LIST_TOP, WIDTH, 48, "Toggle interface animations", () -> {
            config.animations(!config.animations());
        }, (g, button) -> {
            Ui.text(g, "Motion", 0, LIST_TOP + 10, Ui.TEXT, 1.15f);
            Ui.text(g, "Smooth toggles and zoom transitions", 0, LIST_TOP + 32, Ui.DIM, .8f);
            Ui.toggle(g, 520, LIST_TOP + 15, config.animations() ? 1 : 0);
        }));
        addRenderableWidget(new AsterButton(0, railHeight - 91, 235, 32, "Reset current profile", () -> {
            if (confirmingReset) {
                config.resetActiveProfile();
                confirmingReset = false;
                rebuildControls();
            } else confirmingReset = true;
        }, (g, button) -> Ui.text(g, confirmingReset ? "Confirm reset  /  Esc cancels" : "Reset current profile", 0, railHeight - 81,
            confirmingReset || button.isHoveredOrFocused() ? Ui.ACCENT_TEXT : Ui.MUTED, .9f)));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (minecraft.level == null) {
            extractPanorama(g, delta);
        } else if (parent != null) {
            if (parent.width != width || parent.height != height) parent.resize(width, height);
            parent.extractBackground(g, -10000, -10000, delta);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float elapsed = Math.min(.1f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        for (Module module : registry.all()) {
            float target = module.enabled ? 1 : 0;
            float previous = animations.get(module.id);
            animations.put(module.id, config.animations() ? previous + (target - previous) * (1 - (float) Math.exp(-elapsed * 18)) : target);
        }
        // A continuous fade keeps text readable while leaving the other side of the scene visible.
        int fadeWidth = Math.min(width, (int) (offsetX + (WIDTH + 220) * scale));
        for (int x = 0; x < fadeWidth; x += 2) {
            float position = Math.clamp((x - offsetX - WIDTH * scale * .8f) / (fadeWidth - offsetX - WIDTH * scale * .8f), 0, 1);
            int alpha = (int) (236 * (1 - position * position * (3 - 2 * position)));
            g.fill(x, 0, Math.min(x + 2, fadeWidth), height, (alpha << 24) | 0x0B0B0B);
        }
        g.pose().pushMatrix();
        g.pose().translate(offsetX, offsetY);
        g.pose().scale(scale);
        Ui.icon(g, "aster", 0, 8, 20, Ui.ACCENT);
        Ui.text(g, "aster", 31, 1, Ui.TEXT, 2.4f);
        Ui.text(g, "FLOW  /  1.0", 121, 18, Ui.ACCENT_TEXT, .85f);
        Ui.circle(g, 0, 48, 5, Ui.ACCENT);
        String context = minecraft.player == null ? "MENU" : parent != null && parent.isPauseScreen() ? "PAUSED" : "IN GAME";
        Ui.text(g, context + "  /  " + registry.activeCount() + " ACTIVE", 13, 46, Ui.DIM, .8f);
        if (tab.equals("modules")) {
            Ui.icon(g, "search", 0, 87, 17, Ui.MUTED);
            if (query.isEmpty() && search.isFocused()) Ui.text(g, "Find a module...", 40, 85, Ui.DIM, 1);
            g.fill(0, 115, WIDTH, 116, search.isFocused() ? Ui.ACCENT : Ui.BORDER);
            drawModules(g);
        } else {
            Ui.text(g, tab.equals("profiles") ? "Switch your flow." : "The finer details.", 0, 83, Ui.TEXT, 1.65f);
            Ui.text(g, tab.equals("profiles") ? "Every setup stays independent." : "Make the interface feel like you.", 0, 184, Ui.DIM, .9f);
        }
        if (tab.equals("settings")) drawSettings(g);
        g.fill(0, railHeight - 42, WIDTH, railHeight - 41, 0x553A3A3A);
        Ui.text(g, config.error() == null ? config.activeProfile() + "  /  saved locally" : config.error(), 0, railHeight - 27,
            config.error() == null ? Ui.DIM : 0xFFDCA987, .8f);
        Ui.text(g, "ESC  CLOSE", 470, railHeight - 27, Ui.MUTED, .75f);
        super.extractRenderState(g, (int) logicalX(mouseX), (int) logicalY(mouseY), delta);
        g.pose().popMatrix();
    }

    private void drawModules(GuiGraphicsExtractor g) {
        List<Module> visible = results();
        if (visible.isEmpty()) {
            Ui.text(g, "Nothing here yet.", 39, LIST_TOP + 38, Ui.TEXT, 1.2f);
            Ui.text(g, "Try another search or category.", 39, LIST_TOP + 66, Ui.DIM, .9f);
        }
        Ui.text(g, capturing ? "ESC CANCEL  /  BACKSPACE CLEAR" : "↑ ↓ SELECT   ENTER TOGGLE   → TUNE", 0, railHeight - 60, Ui.DIM, .75f);
        if (visible.size() > visibleRowCount()) Ui.text(g, "SCROLL FOR MORE", 424, railHeight - 60, Ui.DIM, .7f);
    }

    private void drawSettings(GuiGraphicsExtractor g) {
        int y = LIST_TOP + 104;
        Ui.text(g, "SHORTCUTS", 0, y, Ui.ACCENT_TEXT, .8f);
        shortcut(g, "Open / close Aster", AsterClient.dashboardKey().getTranslatedKeyMessage().getString(), y + 29);
        shortcut(g, "Hold to zoom", AsterClient.zoomKey().getTranslatedKeyMessage().getString(), y + 59);
        shortcut(g, "Focus search", "Ctrl / Cmd + K", y + 89);
        shortcut(g, "Navigate controls", "Tab / Shift + Tab", y + 119);
    }

    private void shortcut(GuiGraphicsExtractor g, String label, String key, int y) {
        Ui.text(g, label, 0, y, Ui.MUTED, .9f);
        Ui.text(g, key, 370, y, Ui.TEXT, .9f);
    }

    private void focusSearch() {
        setFocused(search);
        search.setFocused(true);
    }

    private void toggle(Module module) {
        capturing = false;
        module.enabled = !module.enabled;
        config.save();
        selectFirstResult();
        rebuildControls();
    }

    public static String keyName(int key) {
        return key < 0 ? "Unbound" : InputConstants.Type.KEYBOARD.getOrCreate(key).getDisplayName().getString();
    }

    private double logicalX(double x) { return (x - offsetX) / scale; }
    private double logicalY(double y) { return (y - offsetY) / scale; }
    private MouseButtonEvent logicalEvent(MouseButtonEvent event) {
        return new MouseButtonEvent(logicalX(event.x()), logicalY(event.y()), event.buttonInfo());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(logicalEvent(event), doubleClick);
        if (getFocused() != null && !children().contains(getFocused())) clearFocus();
        return handled;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) { return super.mouseReleased(logicalEvent(event)); }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) { return super.mouseDragged(logicalEvent(event), dx / scale, dy / scale); }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (tab.equals("modules")) {
            rowScroll -= (int) Math.signum(vertical);
            capturing = false;
            rebuildControls();
            return true;
        }
        if (tab.equals("profiles")) {
            profileScroll -= (int) Math.signum(vertical);
            rebuildControls();
            return true;
        }
        return super.mouseScrolled(logicalX(x), logicalY(y), horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (capturing) {
            if (event.isEscape()) { capturing = false; return true; }
            if (event.key() == InputConstants.KEY_BACKSPACE || event.key() == InputConstants.KEY_DELETE) bindingTarget.key = -1;
            else if (event.key() > 0 && event.key() < 512 && !AsterClient.dashboardKey().matches(event) && !AsterClient.zoomKey().matches(event)) {
                for (Module module : registry.all()) if (module.key == event.key()) module.key = -1;
                bindingTarget.key = event.key();
            } else { bindingHint = "Key is reserved"; return true; }
            capturing = false;
            config.save();
            return true;
        }
        if (event.isEscape() && (pendingDelete != null || confirmingReset)) {
            pendingDelete = null;
            confirmingReset = false;
            return true;
        }
        if (tab.equals("profiles") && getFocused() == profileName && event.key() == InputConstants.KEY_RETURN) {
            renameCurrentProfile();
            return true;
        }
        if (AsterClient.dashboardKey().matches(event)) { onClose(); return true; }
        if ((event.modifiers() & (InputConstants.MOD_CONTROL | InputConstants.MOD_SUPER)) != 0 && event.key() == InputConstants.KEY_K) {
            tab = "modules";
            rebuildControls();
            focusSearch();
            return true;
        }
        if (event.isEscape() && tab.equals("modules") && !query.isEmpty()) { search.setValue(""); return true; }
        if (getFocused() instanceof AsterSlider) return super.keyPressed(event);
        if (tab.equals("modules")) {
            List<Module> visible = results();
            if (!visible.isEmpty() && (event.isUp() || event.isDown())) {
                int index = Math.max(0, visible.indexOf(selected));
                selected = visible.get(Math.floorMod(index + (event.isDown() ? 1 : -1), visible.size()));
                ensureSelectedVisible();
                rebuildControls();
                return true;
            }
            if (!visible.isEmpty() && event.key() == InputConstants.KEY_RETURN && (getFocused() == search || getFocused() == null)) {
                toggle(selected);
                return true;
            }
            if (!visible.isEmpty() && event.isRight() && (!search.isFocused() || search.getValue().isEmpty())) {
                expand(selected);
                clearFocus();
                return true;
            }
            if (event.isLeft() && !search.isFocused() && expanded != null) {
                expanded = null;
                rebuildControls();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        config.save();
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() { config.save(); }

    @Override
    public boolean isPauseScreen() { return parent != null && parent.isPauseScreen(); }
}
