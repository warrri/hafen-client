package haven.tasks;

import haven.*;

import java.util.ArrayList;
import java.util.List;

class TaskContext {
    private final UI ui;

    public TaskContext(UI ui) {
        this.ui = ui;
    }

    public GameUI gui() {
        return ui.gui;
    }

    public void info(String message) {
        ui.gui.msg(message);
    }

    public void error(String message) {
        ui.gui.error(message);
    }

    public GItem getItemAtHand() {
        for (GameUI.DraggedItem item : ui.gui.hand)
            return item.item;
        for (GameUI.DraggedItem item : ui.gui.handSave)
            return item.item;
        return null;
    }

    public WItem getItemLeftHand() {
        return ui.gui.getEquipory().slots[6];
    }
    public WItem getItemRightHand() {
        return ui.gui.getEquipory().slots[7];
    }

    public FlowerMenu getMenu() {
        return ui.root.findchild(FlowerMenu.class);
    }

    public void click(Gob gob, int button, int mod) {
        ui.gui.map.wdgmsg("click", Coord.z, gob.rc, button, mod, 0, (int)gob.id, gob.rc, 0, -1);
    }
    public void click(Gob gob, int button, int mod, Coord coord) {
        ui.gui.map.wdgmsg("click", coord, gob.rc, button, mod, 0, (int)gob.id, gob.rc, 0, -1);
    }


    public Gob findObjectById(long id) {
        return ui.sess.glob.oc.getgob(id);
    }

    public Gob findObjectByName(int radius, boolean exact, String name) {
        return findObjectByNames(radius, exact, name);
    }

    public Gob findObjectByNames(int radius, boolean exact, String... names) {
        Coord plc = player().rc;
        double min = radius;
        Gob nearest = null;
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                double dist = gob.rc.dist(plc);
                if (dist < min) {
                    boolean matches = false;
                    for (String name : names) {
                        if (Utils.isObjectName(gob, exact, name)) {
                            matches = true;
                            break;
                        }
                    }
                    if (matches) {
                        min = dist;
                        nearest = gob;
                    }
                }
            }
        }
        return nearest;
    }

    public Gob findObjectInBoundingBox(Coord one, Coord two, String... names) {
        int xlow = one.x<two.x?one.x:two.x;
        int xhigh = one.x>=two.x?one.x:two.x;
        int ylow = one.y<two.y?one.y:two.y;
        int yhigh = one.y>=two.y?one.y:two.y;

        Gob obj = null;
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                for (String name : names) {
                    if (Utils.isObjectName(gob, false, name)) {
                        if (gob.rc.x/11>=xlow && gob.rc.x/11 <=xhigh && gob.rc.y/11 >=ylow && gob.rc.y/11 <= yhigh){
                            if (GobInfo.isHarvestable(gob)) {
                                 return gob;
                            }
                        }
                    }
                }
            }
        }
        return obj;
    }
    public List<Window> findWindows(String name) {
        List<Window> result = new ArrayList<Window>();
        for (Widget w = ui.gui.child; w != null; w = w.next) {
            if (w instanceof Window) {
                Window window = (Window)w;
                if (name.equals(window.cap.text))
                    result.add(window);
            }
        }
        return result;
    }

    public void itemact(Gob gob, int mod) {
        ui.gui.map.wdgmsg("itemact", Coord.z, gob.rc, mod, 0, (int)gob.id, gob.rc, 0, -1);
    }

    public Gob player() {
        return ui.gui.map.player();
    }

    public Inventory playerInventory() {
        return ui.gui.maininv;
    }

    public void clicktile(Coord c){
        ui.gui.map.wdgmsg("click", Coord.z, c, 1, 0);
    }
}
