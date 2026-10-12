import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Reproducible, antialiased UI primitives. Run: java tools/GenerateAssets.java */
class GenerateAssets {
    static Path root = Path.of("src/main/resources/assets/aster/textures/gui/sprites");
    public static void main(String[] args) throws Exception {
        Files.createDirectories(root.resolve("icons"));
        rounded("panel", 64, 12);
        rounded("small", 32, 7);
        for (String icon : new String[]{"aster", "grid", "sprint", "sun", "zoom", "compass", "activity", "keyboard", "sliders", "layers", "arrow", "close", "search", "check", "plus"}) {
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = graphics(image);
            g.setStroke(new BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (icon) {
                case "aster" -> {
                    Path2D star = new Path2D.Double();
                    for (int i=0; i<16; i++) {
                        double angle = i * Math.PI / 8 - Math.PI / 2;
                        double radius = i % 2 == 0 ? 27 : 10;
                        double x=32 + Math.cos(angle)*radius, y=32+Math.sin(angle)*radius;
                        if(i==0)star.moveTo(x,y);else star.lineTo(x,y);
                    }
                    star.closePath(); g.fill(star);
                }
                case "grid" -> { for(int y=13;y<45;y+=24)for(int x=13;x<45;x+=24)g.drawRoundRect(x,y,14,14,4,4); }
                case "sprint" -> { line(g,16,19,41,19); line(g,10,32,48,32);line(g,16,45,41,45);line(g,38,24,48,32);line(g,48,32,38,40); }
                case "sun" -> {
                    g.drawOval(22,22,20,20);
                    for(int i=0;i<8;i++){double a=i*Math.PI/4;line(g,32+Math.cos(a)*19,32+Math.sin(a)*19,32+Math.cos(a)*25,32+Math.sin(a)*25);}
                }
                case "zoom", "search" -> { g.drawOval(13,12,28,28);line(g,38,38,51,51);if(icon.equals("zoom")){line(g,21,26,33,26);line(g,27,20,27,32);} }
                case "compass" -> {g.drawOval(10,10,44,44);Path2D p=new Path2D.Double();p.moveTo(41,22);p.lineTo(35,35);p.lineTo(22,41);p.lineTo(28,28);p.closePath();g.draw(p);}
                case "activity" -> {line(g,10,34,21,34);line(g,21,34,28,16);line(g,28,16,36,48);line(g,36,48,43,28);line(g,43,28,54,28);}
                case "keyboard" -> {g.drawRoundRect(8,17,48,31,6,6);for(int y=25;y<38;y+=9)for(int x=17;x<51;x+=10)line(g,x,y,x+2,y);line(g,23,42,41,42);}
                case "sliders" -> {for(int i=0;i<3;i++){int x=17+i*15;line(g,x,12,x,52);int y=i==1?24:39;g.fillRoundRect(x-5,y-6,10,12,5,5);}}
                case "layers" -> {for(int y=23;y<48;y+=10){line(g,11,y,32,y+10);line(g,32,y+10,53,y);}line(g,11,23,32,12);line(g,32,12,53,23);}
                case "arrow" -> {line(g,14,32,50,32);line(g,37,19,50,32);line(g,50,32,37,45);}
                case "close" -> {line(g,20,20,44,44);line(g,44,20,20,44);}
                case "check" -> {line(g,15,32,27,44);line(g,27,44,50,20);}
                case "plus" -> {line(g,32,15,32,49);line(g,15,32,49,32);}
            }
            g.dispose();ImageIO.write(image,"png",root.resolve("icons/"+icon+".png").toFile());
        }
        BufferedImage circle = new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=graphics(circle);g.fillOval(1,1,62,62);g.dispose();ImageIO.write(circle,"png",root.resolve("circle.png").toFile());
        BufferedImage badge = new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
        Graphics2D brand = graphics(badge);
        brand.setColor(new Color(0x171717));
        brand.fillRoundRect(0,0,256,256,64,64);
        brand.setColor(new Color(0xD59A6A));
        Path2D star = new Path2D.Double();
        for(int i=0;i<16;i++) {
            double angle=i*Math.PI/8-Math.PI/2;
            double radius=i%2==0?82:31;
            double x=128+Math.cos(angle)*radius,y=128+Math.sin(angle)*radius;
            if(i==0)star.moveTo(x,y);else star.lineTo(x,y);
        }
        star.closePath();brand.fill(star);brand.dispose();
        ImageIO.write(badge,"png",Path.of("src/main/resources/assets/aster/icon.png").toFile());
    }
    static Graphics2D graphics(BufferedImage image) {
        Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(Color.WHITE);return g;
    }
    static void rounded(String name,int size,int radius)throws Exception {
        BufferedImage image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);Graphics2D g=graphics(image);
        g.fill(new RoundRectangle2D.Float(0,0,size,size,radius*2,radius*2));g.dispose();
        ImageIO.write(image,"png",root.resolve(name+".png").toFile());
        Files.writeString(root.resolve(name+".png.mcmeta"),"{\"gui\":{\"scaling\":{\"type\":\"nine_slice\",\"width\":"+size+",\"height\":"+size+",\"border\":"+radius+",\"stretch_inner\":true}}}");
    }
    static void line(Graphics2D g,double x1,double y1,double x2,double y2){g.draw(new Line2D.Double(x1,y1,x2,y2));}
}
