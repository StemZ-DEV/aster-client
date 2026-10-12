package dev.aster.client.ui;

import dev.aster.client.AsterClient;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class AsterHud {
    public static void render(GuiGraphicsExtractor g,DeltaTracker delta) {
        Minecraft client=Minecraft.getInstance();
        if(client.player==null||client.gui.hud.isHidden()||client.gui.screen() instanceof AsterScreen)return;
        var modules=AsterClient.modules();
        int nextY=10;
        if(modules.get("fps").enabled) {
            float scale=(float)modules.get("fps").value;
            g.pose().pushMatrix();g.pose().translate(10,nextY);g.pose().scale(scale);
            String label=client.getFps()+" FPS";
            Ui.round(g,0,0,Ui.width(label)+49,26,Ui.HUD_SURFACE);Ui.icon(g,"aster",8,7,12,Ui.ACCENT_TEXT);Ui.text(g,label,28,10,Ui.TEXT,.9f);
            g.pose().popMatrix();nextY+=(int)(31*scale);
        }
        if(modules.get("coordinates").enabled) {
            float scale=(float)modules.get("coordinates").value;
            var position=client.player.blockPosition();String label=String.format(Locale.ROOT,"X  %d     Y  %d     Z  %d",position.getX(),position.getY(),position.getZ());
            g.pose().pushMatrix();g.pose().translate(10,nextY);g.pose().scale(scale);
            Ui.round(g,0,0,Ui.width(label)+23,25,Ui.HUD_SURFACE);Ui.text(g,label,10,9,Ui.TEXT,.9f);g.pose().popMatrix();
        }
        if(modules.get("keystrokes").enabled) {
            float scale=(float)modules.get("keystrokes").value;
            g.pose().pushMatrix();g.pose().translate(g.guiWidth()-88*scale,10);g.pose().scale(scale);
            key(g,"W",26,0,client.options.keyUp.isDown());key(g,"A",0,26,client.options.keyLeft.isDown());
            key(g,"S",26,26,client.options.keyDown.isDown());key(g,"D",52,26,client.options.keyRight.isDown());
            Ui.round(g,0,52,37,22,client.options.keyAttack.isDown()?Ui.HUD_ACTIVE:Ui.HUD_SURFACE);Ui.centered(g,"LMB",18,59,Ui.TEXT);
            Ui.round(g,41,52,37,22,client.options.keyUse.isDown()?Ui.HUD_ACTIVE:Ui.HUD_SURFACE);Ui.centered(g,"RMB",60,59,Ui.TEXT);
            Ui.round(g,0,78,78,18,client.options.keyJump.isDown()?Ui.HUD_ACTIVE:Ui.HUD_SURFACE);Ui.round(g,24,86,30,2,Ui.MUTED);g.pose().popMatrix();
        }
    }
    private static void key(GuiGraphicsExtractor g,String label,int x,int y,boolean down) {
        Ui.round(g,x,y,23,23,down?Ui.HUD_ACTIVE:Ui.HUD_SURFACE);Ui.centered(g,label,x+12,y+8,down?Ui.TEXT:Ui.MUTED);
    }
    private AsterHud(){}
}
