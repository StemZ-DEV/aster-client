package dev.aster.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.aster.client.AsterClient;
import dev.aster.client.ui.AsterScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.input.KeyEvent;

/** End-to-end checks exercise real input through the scaled native interface. */
public final class DashboardGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        context.waitForScreen(TitleScreen.class);
        context.getInput().resizeWindow(1440,900);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(AsterScreen.class);
        context.waitTicks(3);
        context.takeScreenshot("aster-menu");
        click(context,200,230);
        context.runOnClient(client->{
            if(!AsterClient.modules().get("sprint").enabled)throw new AssertionError("Scaled toggle click did not enable Sprint");
        });
        click(context,540,326);
        click(context,200,410);
        context.runOnClient(client->{
            if(AsterClient.modules().get("zoom").value <= 6)throw new AssertionError("Inline slider did not change Zoom magnification");
        });
        click(context,420,410);
        context.getInput().pressKey(InputConstants.KEY_C);
        context.runOnClient(client->{
            if(AsterClient.modules().get("zoom").key!=-1)throw new AssertionError("Reserved hold-to-zoom key was accepted as a toggle");
        });
        context.getInput().pressKey(InputConstants.KEY_G);
        context.runOnClient(client->{
            if(AsterClient.modules().get("zoom").key != InputConstants.KEY_G)throw new AssertionError("Inline binding capture failed");
        });
        context.takeScreenshot("aster-inline");
        click(context,420,410);
        click(context,200,230);
        context.getInput().pressKey(InputConstants.KEY_H);
        context.runOnClient(client->{
            if(AsterClient.modules().get("zoom").key!=InputConstants.KEY_G||AsterClient.modules().get("sprint").key!=-1)
                throw new AssertionError("Clicking another row left binding capture active on the wrong module");
        });
        click(context,100,90);
        context.getInput().typeChars("zoom");
        context.waitTick();context.takeScreenshot("aster-search");
        context.getInput().pressKey(InputConstants.KEY_RETURN);
        context.runOnClient(client->{
            if(AsterClient.modules().get("zoom").enabled)throw new AssertionError("Search result keyboard toggle failed");
        });
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitForScreen(TitleScreen.class);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(AsterScreen.class);
        click(context,150,145);
        context.waitTick();context.takeScreenshot("aster-profiles");
        click(context,320,285);
        context.runOnClient(client->{
            if(!AsterClient.config().activeProfile().equals("Minimal"))throw new AssertionError("Profile did not switch");
            if(AsterClient.modules().get("sprint").enabled)throw new AssertionError("Previous profile leaked into Minimal");
        });
        click(context,120,railHeight(context)-80);
        context.runOnClient(client->{
            if(!AsterClient.config().activeProfile().equals("Custom 1"))throw new AssertionError("Snapshot was not created");
        });
        click(context,100,railHeight(context)-143);
        // Fabric's SDL test helper supplies zero modifier bits; dispatch the real edit shortcut explicitly.
        context.runOnClient(client->client.gui.screen().keyPressed(
            new KeyEvent(InputConstants.KEY_A,'a',InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER)));
        context.getInput().typeChars("Night Build");
        context.getInput().pressKey(InputConstants.KEY_RETURN);
        context.takeScreenshot("aster-named-profiles");
        context.runOnClient(client->{
            if(!AsterClient.config().activeProfile().equals("Night Build"))throw new AssertionError("Profile name did not save through keyboard input: "
                +AsterClient.config().activeProfile()+" / "+AsterClient.config().error());
        });
        click(context,120,railHeight(context)-80);
        click(context,510,railHeight(context)-142);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitForScreen(AsterScreen.class);
        context.runOnClient(client->{
            if(!AsterClient.config().profiles().contains("Custom 1"))throw new AssertionError("Cancelling profile deletion deleted the profile");
        });
        click(context,510,railHeight(context)-142);
        click(context,510,railHeight(context)-142);
        context.runOnClient(client->{
            if(AsterClient.config().profiles().contains("Custom 1"))throw new AssertionError("Confirmed deletion did not remove the profile");
            if(!AsterClient.config().profiles().contains("Night Build"))throw new AssertionError("Deleting a snapshot deleted another profile");
        });
        click(context,285,145);context.waitTick();context.takeScreenshot("aster-settings");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitForScreen(TitleScreen.class);

        KeyBindsScreen bindings=context.computeOnClient(client->new KeyBindsScreen(client.gui.screen(),client.options));
        context.setScreen(()->bindings);
        context.runOnClient(client->bindings.selectedKey=AsterClient.dashboardKey());
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(KeyBindsScreen.class);
        context.runOnClient(client->{
            if(bindings.selectedKey!=null)throw new AssertionError("Aster intercepted Minecraft's key binding capture");
        });
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(AsterScreen.class);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitForScreen(KeyBindsScreen.class);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitForScreen(TitleScreen.class);

        OptionsScreen options = context.computeOnClient(client -> new OptionsScreen(client.gui.screen(),client.options));
        context.setScreen(() -> options);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(AsterScreen.class);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.runOnClient(client -> {
            if(client.gui.screen()!=options)throw new AssertionError("Menu parent was not restored");
        });
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitForScreen(TitleScreen.class);

        context.runOnClient(client -> {AsterClient.config().selectProfile("Everyday");AsterClient.config().resetActiveProfile();});
        try(var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            context.getInput().lookAt(40,12);
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitForScreen(AsterScreen.class);
            context.runOnClient(client -> {
                if(client.gui.screen().isPauseScreen())throw new AssertionError("Gameplay overlay unexpectedly pauses the game");
            });
            context.waitTicks(4);
            context.takeScreenshot("aster-in-game");
            click(context,540,326);
            context.takeScreenshot("aster-in-game-inline");
            context.getInput().resizeWindow(960,540);
            context.waitTicks(2);
            context.takeScreenshot("aster-compact");
            click(context,200,230);
            context.runOnClient(client -> {
                if(!AsterClient.modules().get("sprint").enabled)throw new AssertionError("Compact window toggle failed");
            });
            for(int i=0;i<5;i++)context.getInput().pressKey(InputConstants.KEY_DOWN);
            context.getInput().pressKey(InputConstants.KEY_RETURN);
            context.runOnClient(client -> {
                if(!AsterClient.modules().get("keystrokes").enabled)throw new AssertionError("Keyboard selection did not scroll to and toggle Keystrokes");
                if(!AsterClient.modules().get("sprint").enabled)throw new AssertionError("A stale focused row received the keyboard toggle");
            });
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitForScreen(null);
            verifyRuntime(context);
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitForScreen(PauseScreen.class);
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitForScreen(AsterScreen.class);
            context.runOnClient(client -> {
                if(!client.gui.screen().isPauseScreen())throw new AssertionError("Pause menu overlay lost the paused state");
            });
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitForScreen(PauseScreen.class);
        }
    }

    private static void verifyRuntime(ClientGameTestContext context) {
        context.runOnClient(client->{
            AsterClient.config().animations(false);
            AsterClient.modules().get("zoom").enabled=true;
            AsterClient.modules().get("zoom").setValue(4);
            AsterClient.modules().get("sprint").enabled=false;
            AsterClient.modules().get("fullbright").key=InputConstants.KEY_C;
        });
        context.waitTicks(3);
        float normalFov=context.computeOnClient(client->client.gameRenderer.mainCamera().getFov());
        context.getInput().holdKey(InputConstants.KEY_C);
        context.waitTicks(3);
        context.runOnClient(client->{
            if(client.gameRenderer.mainCamera().getFov()>normalFov*.35f)throw new AssertionError("Zoom did not change the actual camera FOV");
            if(AsterClient.modules().get("fullbright").enabled)throw new AssertionError("Imported reserved key toggled a module while zooming");
        });
        context.getInput().releaseKey(InputConstants.KEY_C);
        context.waitTicks(3);
        context.runOnClient(client->{
            if(Math.abs(client.gameRenderer.mainCamera().getFov()-normalFov)>1)throw new AssertionError("Zoom did not restore the actual camera FOV");
            var fullbright=AsterClient.modules().get("fullbright");
            double gamma=client.options.gamma().get();
            var extractor=new LightmapRenderStateExtractor(client.gameRenderer,client);
            var state=new LightmapRenderState();
            fullbright.enabled=true;fullbright.setValue(.65);
            extractor.tick();extractor.extract(state,0);
            if(state.nightVisionEffectIntensity<.649f)throw new AssertionError("Fullbright did not modify the actual lightmap extraction");
            fullbright.enabled=false;
            extractor.tick();extractor.extract(state,0);
            if(state.nightVisionEffectIntensity!=0)throw new AssertionError("Fullbright did not restore the lightmap");
            if(client.options.gamma().get()!=gamma||client.player.hasEffect(MobEffects.NIGHT_VISION))throw new AssertionError("Fullbright changed persistent gamma or player effects");
            fullbright.key=InputConstants.KEY_G;
        });
        context.getInput().holdKey(InputConstants.KEY_G);
        context.waitTicks(4);
        context.runOnClient(client->{
            if(!AsterClient.modules().get("fullbright").enabled)throw new AssertionError("Held module key did not toggle exactly once");
        });
        context.getInput().releaseKey(InputConstants.KEY_G);
        context.waitTicks(2);
        context.getInput().holdKey(InputConstants.KEY_G);
        context.waitTicks(2);
        context.getInput().releaseKey(InputConstants.KEY_G);
        context.runOnClient(client->{
            if(AsterClient.modules().get("fullbright").enabled)throw new AssertionError("Second module key press did not toggle off");
            AsterClient.modules().get("sprint").enabled=true;
        });
        context.getInput().holdKey(options->options.keyUp);
        context.waitTicks(5);
        context.runOnClient(client->{
            if(!client.player.isSprinting())throw new AssertionError("Auto Sprint did not sprint while moving forward");
        });
        context.getInput().releaseKey(options->options.keyUp);
        context.waitTicks(3);
        context.runOnClient(client->{
            if(client.player.isSprinting())throw new AssertionError("Auto Sprint did not release its sprint state");
            AsterClient.config().animations(true);
        });
        context.takeScreenshot("aster-runtime-hud");
    }

    private static void click(ClientGameTestContext context,double x,double y) {
        double[] point=context.computeOnClient(client->{
            var screen=client.gui.screen();double scale=Math.min(screen.width/1120.0,(screen.height-16.0)/560);
            int railHeight=Math.min(650,(int)((screen.height-16.0)/scale));
            return new double[]{(screen.width*.045+x*scale)*client.getWindow().getScreenWidth()/screen.width,
                ((screen.height-railHeight*scale)/2+y*scale)*client.getWindow().getScreenHeight()/screen.height};
        });
        context.getInput().setCursorPos(point[0],point[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTick();
    }

    private static int railHeight(ClientGameTestContext context) {
        return context.computeOnClient(client->{
            var screen=client.gui.screen();double scale=Math.min(screen.width/1120.0,(screen.height-16.0)/560);
            return Math.min(650,(int)((screen.height-16.0)/scale));
        });
    }
}
