package dev.aster.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;


public class AsterButton extends AbstractWidget {
    @FunctionalInterface public interface Painter { void draw(GuiGraphicsExtractor graphics, AsterButton button); }
    private final Runnable action;
    private final Painter painter;

    public AsterButton(int x,int y,int width,int height,String label,Runnable action,Painter painter) {
        super(x,y,width,height,Component.literal(label));this.action=action;this.painter=painter;
    }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        painter.draw(g,this);
        if(isFocused())g.fill(getX(),getY()+height-1,getX()+width,getY()+height,Ui.ACCENT);
    }
    @Override public void onClick(MouseButtonEvent event,boolean doubleClick) { action.run(); }
    @Override public boolean keyPressed(KeyEvent event) {
        if(isFocused() && (event.key()==InputConstants.KEY_RETURN||event.key()==InputConstants.KEY_SPACE)) {
            action.run();return true;
        }
        return false;
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
}
