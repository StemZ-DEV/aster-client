package dev.aster.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public final class Ui {
    public static final int BACKGROUND = 0xFF101010;
    public static final int SURFACE = 0xFF171717;
    public static final int CARD = 0xFF202020;
    public static final int BORDER = 0xFF303030;
    public static final int HOVER = 0xFF282828;
    public static final int TEXT = 0xFFF2F2F2;
    public static final int MUTED = 0xFFA8A8A8;
    public static final int DIM = 0xFF808080;
    public static final int ACCENT = 0xFFD59A6A;
    public static final int ACCENT_HOVER = blend(ACCENT, TEXT, 0.12f);
    public static final int ACCENT_SURFACE = blend(CARD, ACCENT, 0.18f);
    // A lighter tint of the same hue keeps small labels legible on dark surfaces.
    public static final int ACCENT_TEXT = blend(ACCENT, TEXT, 0.10f);
    public static final int HUD_SURFACE = 0xDB171717;
    public static final int HUD_ACTIVE = 0xDCD59A6A;
    public static final int GREEN = 0xFF8AE3BF;
    public static final Style FONT_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(id("ui")));

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("aster", path); }
    public static Component component(String text) { return Component.literal(text).setStyle(FONT_STYLE); }
    public static Font font() { return Minecraft.getInstance().font; }
    public static void round(GuiGraphicsExtractor g, int x, int y, int width, int height, int color) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, id(height <= 32 ? "small" : "panel"), x, y, width, height, color);
    }
    public static void bordered(GuiGraphicsExtractor g, int x, int y, int width, int height, int border, int fill) {
        round(g,x,y,width,height,border);round(g,x+1,y+1,width-2,height-2,fill);
    }
    public static void icon(GuiGraphicsExtractor g, String name, int x, int y, int size, int color) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED,id("icons/"+name),x,y,size,size,color);
    }
    public static void circle(GuiGraphicsExtractor g, int x, int y, int diameter, int color) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED,id("circle"),x,y,diameter,diameter,color);
    }
    public static void text(GuiGraphicsExtractor g, String text, int x, int y, int color) {
        g.text(font(), component(text),x,y,color,false);
    }
    public static void text(GuiGraphicsExtractor g, String text, int x, int y, int color, float size) {
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(size);text(g,text,0,0,color);g.pose().popMatrix();
    }
    public static int width(String text) { return font().width(component(text)); }
    public static void centered(GuiGraphicsExtractor g, String text, int x, int y, int color) {
        text(g,text,x-width(text)/2,y,color);
    }
    public static void wrapped(GuiGraphicsExtractor g, String text, int x, int y, int width, int color) {
        for (var line : font().split(component(text),width)) { g.text(font(),line,x,y,color,false); y+=16; }
    }
    public static int blend(int start, int end, float progress) {
        int a=(int)(((start>>>24)&255)*(1-progress)+((end>>>24)&255)*progress);
        int r=(int)(((start>>>16)&255)*(1-progress)+((end>>>16)&255)*progress);
        int green=(int)(((start>>>8)&255)*(1-progress)+((end>>>8)&255)*progress);
        int b=(int)((start&255)*(1-progress)+(end&255)*progress);
        return a<<24|r<<16|green<<8|b;
    }
    public static void toggle(GuiGraphicsExtractor g,int x,int y,float progress) {
        round(g,x,y,34,19,blend(BORDER,ACCENT,progress));
        circle(g,x+3+(int)(progress*15),y+3,13,TEXT);
    }
    private Ui() {}
}
