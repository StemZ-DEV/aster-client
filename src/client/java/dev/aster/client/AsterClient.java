package dev.aster.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.aster.client.ui.AsterButton;
import dev.aster.client.ui.AsterHud;
import dev.aster.client.ui.AsterScreen;
import dev.aster.client.ui.Ui;
import dev.aster.core.ClientConfig;
import dev.aster.core.Module;
import dev.aster.core.ModuleRegistry;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;


public final class AsterClient implements ClientModInitializer {
    private static final ModuleRegistry MODULES=new ModuleRegistry();
    private static ClientConfig config;
    private static KeyMapping dashboard,zoom;
    private final Map<String,Boolean> held=new HashMap<>();
    private boolean appliedSprint,previewOpened;
    private int titleTicks;
    private static float zoomScale=1;
    private static long zoomTime=System.nanoTime();

    @Override public void onInitializeClient() {
        config=new ClientConfig(FabricLoader.getInstance().getConfigDir().resolve("aster/client.json"),MODULES);
        config.load();
        KeyMapping.Category category=KeyMapping.Category.register(Ui.id("client"));
        dashboard=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.aster.dashboard",InputConstants.Type.KEYBOARD,InputConstants.KEY_RSHIFT,category));
        zoom=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.aster.zoom",InputConstants.Type.KEYBOARD,InputConstants.KEY_C,category));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client->config.save());
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Ui.id("hud"),AsterHud::render);
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR,original->(g,delta)->{
            if(!(Minecraft.getInstance().gui.screen() instanceof AsterScreen))original.extractRenderState(g,delta);
        });
        ScreenEvents.AFTER_INIT.register((client,screen,width,height)->{
            if(screen instanceof TitleScreen) {
                Screens.getWidgets(screen).add(new AsterButton(12,height-38,110,26,"Open Aster",()->client.gui.setScreen(new AsterScreen(screen)),(g,b)->{
                    Ui.icon(g,"aster",b.getX()+9,b.getY()+6,14,Ui.ACCENT_TEXT);Ui.text(g,"Open Aster",b.getX()+30,b.getY()+9,Ui.TEXT,.9f);
                    if(b.isHoveredOrFocused())g.fill(b.getX()+30,b.getY()+23,b.getX()+100,b.getY()+24,Ui.ACCENT);
                }));
            }
            if(!(screen instanceof AsterScreen)) {
                ScreenKeyboardEvents.allowKeyPress(screen).register((current,event)->{
                    if(dashboard.matches(event)&&!current.isInputCaptured()
                        && !(current instanceof KeyBindsScreen bindings && bindings.selectedKey != null)) {
                        client.gui.setScreen(new AsterScreen(current));return false;
                    }
                    return true;
                });
            }
        });
    }

    private void tick(Minecraft client) {
        while(dashboard.consumeClick())if(client.gui.screen()==null)client.gui.setScreen(new AsterScreen(null));
        boolean playing=client.player!=null&&client.gui.screen()==null&&client.isWindowActive();
        for(Module module:MODULES.all()) {
            boolean pressed=module.key>0&&!isReservedToggleKey(module.key)&&InputConstants.isKeyDown(module.key);
            if(playing&&pressed&&!held.getOrDefault(module.id,false)){module.enabled=!module.enabled;config.save();}
            held.put(module.id,pressed);
        }
        if(client.player!=null) {
            Module sprint=MODULES.get("sprint");
            boolean eligible=playing&&sprint.enabled&&client.options.keyUp.isDown()
                &&!client.player.isCrouching()&&!client.player.horizontalCollision
                &&(client.player.getFoodData().getFoodLevel()>=sprint.value||client.player.getAbilities().mayfly);
            if(eligible){client.player.setSprinting(true);appliedSprint=true;}
            else if(appliedSprint){client.player.setSprinting(false);appliedSprint=false;}
        } else appliedSprint=false;
        // Development runs open the dashboard after resource loading; installed builds use the title button.
        if(Boolean.getBoolean("aster.dev")&&!previewOpened&&client.gui.screen() instanceof TitleScreen&&++titleTicks>20) {
            previewOpened=true;client.gui.setScreen(new AsterScreen(client.gui.screen()));
        }
    }

    public static float zoomScale() {
        Minecraft client=Minecraft.getInstance();
        Module module=MODULES.get("zoom");
        boolean active=module.enabled&&client.player!=null&&client.gui.screen()==null&&zoom!=null&&zoom.isDown();
        float target=active?(float)(1/module.value):1;
        long now=System.nanoTime();float elapsed=Math.min(.1f,(now-zoomTime)/1_000_000_000f);zoomTime=now;
        zoomScale=config.animations()?zoomScale+(target-zoomScale)*(1-(float)Math.exp(-elapsed*16)):target;
        return zoomScale;
    }
    public static ModuleRegistry modules(){return MODULES;}
    public static ClientConfig config(){return config;}
    public static KeyMapping dashboardKey(){return dashboard;}
    public static KeyMapping zoomKey(){return zoom;}
    public static boolean isReservedToggleKey(int key) {
        var openKey=KeyMappingHelper.getBoundKeyOf(dashboard);
        var zoomKey=KeyMappingHelper.getBoundKeyOf(zoom);
        return openKey.getType()==InputConstants.Type.KEYBOARD&&openKey.getValue()==key
            ||zoomKey.getType()==InputConstants.Type.KEYBOARD&&zoomKey.getValue()==key;
    }
}
