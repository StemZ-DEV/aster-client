package dev.aster.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.aster.core.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;


public final class AsterSlider extends AbstractWidget {
    private final Module module;
    private final Runnable save;
    public AsterSlider(int x,int y,int width,Module module,Runnable save) {
        super(x,y,width,22,Ui.component(module.settingLabel));this.module=module;this.save=save;
    }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        float progress=(float)((module.value-module.minimum)/(module.maximum-module.minimum));
        Ui.round(g,getX(),getY()+9,width,4,Ui.BORDER);
        if(progress>0)Ui.round(g,getX(),getY()+9,Math.max(4,(int)(width*progress)),4,Ui.ACCENT);
        Ui.circle(g,getX()+(int)((width-12)*progress),getY()+5,12,isHoveredOrFocused()?Ui.TEXT:Ui.ACCENT_TEXT);
    }
    private void change(double x) {
        module.setValue(module.minimum+Math.clamp((x-getX())/width,0,1)*(module.maximum-module.minimum));
    }
    @Override public void onClick(MouseButtonEvent event,boolean doubleClick) {change(event.x());save.run();}
    @Override protected void onDrag(MouseButtonEvent event,double dx,double dy) {change(event.x());}
    @Override public void onRelease(MouseButtonEvent event) {save.run();}
    @Override public boolean keyPressed(KeyEvent event) {
        if(event.key()==InputConstants.KEY_LEFT||event.key()==InputConstants.KEY_RIGHT){
            double step=module.id.equals("sprint")?1:(module.maximum-module.minimum)/20;
            module.setValue(module.value+(event.key()==InputConstants.KEY_RIGHT?step:-step));save.run();return true;
        }
        return false;
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) {defaultButtonNarrationText(output);}
}
