package haven;

import java.awt.*;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.*;

public class AimProgress extends Widget {
    private static final DecimalFormat format = new DecimalFormat("#.##");

    private final Widget aim;
    private double lastValue = 0;
    private Field valueField = null;
    private Text text;
    private Inventory maininv;
    private Equipory equipory;
    double aDmg = 0, bDmg = 0, tDmg = 0;

    public AimProgress(Widget aim, Inventory maininv, Equipory equipory) {
        this.aim = aim;
        this.maininv=maininv;
        this.equipory=equipory;

        try {
            for (Widget w = maininv.child; w != null; w = w.next) {
                if (w instanceof GItem ) {
                    if (((GItem) w).resname().contains("arrow-bone") || ((GItem) w).resname().contains("arrow-stone")){
                        java.util.List<ItemInfo> infos = ((GItem) w).info();
                        for (ItemInfo info : infos) {
                            if ("QBuff".equals(info.getClass().getSimpleName())) {
                                String name = (String)info.getClass().getDeclaredField("name").get(info);
                                double value = (Double)info.getClass().getDeclaredField("q").get(info);
                                if ("Substance".equals(name)) {
                                    if (((GItem) w).resname().contains("arrow-bone"))
                                        aDmg = 100 * Math.sqrt(value / 10);
                                    else
                                        aDmg = 80 * Math.sqrt(value / 10);
                                }
                            }
                        }
                    }
                }
            }
            java.util.List<ItemInfo> infos = equipory.slots[6].item.info();
            for (ItemInfo info : infos) {
                if ("QBuff".equals(info.getClass().getSimpleName())) {
                    String name = (String) info.getClass().getDeclaredField("name").get(info);
                    double value = (Double) info.getClass().getDeclaredField("q").get(info);
                    if ("Substance".equals(name)) {
                            bDmg = 80 * Math.sqrt(value / 10);
                    }
                }
            }
            tDmg = aDmg + bDmg;

            Class cl = aim.getClass();
            valueField = cl.getDeclaredField("val");
            valueField.setAccessible(true);
            text = Text.renderstroked("0%", Color.WHITE, Color.BLACK);
        } catch (Exception e) {
            text = null;
            e.printStackTrace();
        }
        resize(aim.sz);
    }

    @Override
    public void draw(GOut g) {
        if (valueField == null)
            return;
        try {
            this.c = aim.c.add(0, 2);
            double value = (Double)valueField.get(aim);
            if (value != lastValue) {
                lastValue = value;
                text = Text.renderstroked(format.format(value*value * tDmg),  Color.WHITE, Color.BLACK);
            }
            g.aimage(text.tex(), sz.div(2), 0.5, 0.5);
        } catch (IllegalAccessException ignore) {
        }
    }
}
