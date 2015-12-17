package haven.tasks;

import haven.*;

/**
 * Created by Niko on 06.12.2015.
 */
public class HarvestTaskPlus extends FsmTask {

    private static final String PLANT = "gfx/terobjs/plants/";
    private static final String[] PLANTS = {PLANT+"barley", PLANT+"beet", PLANT+"carrot", PLANT+"flax", PLANT+"hemp", PLANT+"lettuce", PLANT+"pipeweed",
            PLANT+"poppy", PLANT+"pumpkin", PLANT+"yellowonion"};
    private static final String[] INVPLANTS = {"carrot"};

    private Coord startCoord;
    private final Coord start, end;
    private int slots;
    Inventory inv;
    private double totalTasktime;
    private int drinkTries;
    private GItem lastItem;
    private Coord lastPC;
    private IMeter.Meter stam;

    public HarvestTaskPlus(Coord startCoord, Coord start, Coord end) {
        this.startCoord = new Coord(startCoord);
        this.start=start;
        this.end=end;

    }
    @Override
    protected State getInitialState() {
        return new FindObject();
    }

    private class FindObject extends State {
        @Override
        public void tick(double dt) {
            //dropitems();
            stam = context().gui().getmeter("stam", 0);
            inv = context().playerInventory();
            boolean hasFeed = getFreeslots();
            if (slots <= 0) {
                if (hasFeed) {
                    setState(new WaitWalkStart(new WaitFeed()));
                    return;}
                else
                    stop("No inventory slots");
            }
            // find plant
            Gob object = context().findObjectInBoundingBox(start,end, PLANTS);
            if (object != null) {
                context().gui().map.wdgmsg("click", object.sc, object.rc, 3, 1, 0, (int)object.id, object.rc, 0, 42);
                setState(new WaitCursor());
            } else {
                stop("Nothing to harvest");
            }
        }
    }
    private class WaitCursor extends State {
        private final double timer = .5;
        private double t;
        @Override
        public void tick(double dt) {
            t+=dt;
            totalTasktime+=dt;
            if (t>timer) {
                context().gui().map.wdgmsg("sel", start, end, 0);
                setState(new CheckStam());
            }
        }
    }
    private class CheckStam extends State {
        private final int threshHold = 35;
        private double t;
        int filledSlots;

        @Override
        public void tick(double dt) {
            t += dt;
            totalTasktime += dt;
            filledSlots=0;
            stam = context().gui().getmeter("stam", 0);
            if (stam != null && stam.a < threshHold) {
                context().info("Low stamina, trying to drink");
                context().gui().lowStam = true;
                setState(new Wait());
                return;
            }
            // check items every second
            if (t > 1) {
                t=0;
                try {
                    for (Widget w = inv.child; w != null; w = w.next) {
                        if (w instanceof GItem )
                            if (((GItem) w).resname().contains("carrot") ) {
                                filledSlots++;
                                lastItem = (GItem) w;
                            }
                    }
                } catch (Exception e) {
                }
                context().info("Slots left: "+(slots-filledSlots));
                if (filledSlots >= slots) {
                    setState(new Replant());
                }
            }
        }
    }
    private class Replant extends State {
        @Override
        public void tick(double dt) {
            if (context().getItemAtHand()!=null)
                context().getItemAtHand().wdgmsg("drop", startCoord, startCoord, 0);
            if (lastItem != null) {
                lastItem.wdgmsg("iact", lastItem.c, 1);
                context().gui().map.wdgmsg("sel", start, end, 0);
                lastPC = context().gui().map.player().rc;
                setState(new WaitReplant());
            }
            else {
                stop("No carrot found");
            }
        }
    }

    private int doneCheck;
    private class WaitReplant extends State {
        private double t;
        public void tick(double dt) {
            t+=dt;
            totalTasktime+=dt;
            if (t>1) {
                t=0;
                if (lastPC == context().gui().map.player().rc) {
                    doneCheck++;
                    if (doneCheck > 3) {
                        context().gui().map.wdgmsg("click", Coord.z, startCoord, 1, 0, 0, 0, startCoord, 0, 0);
                        setState(new WaitWalkStart(new WaitFeed()));
                    }

                }
                else {
                    doneCheck = 0;
                    lastPC = context().gui().map.player().rc;
                }
            }
        }
    }

    private class WaitWalkStart extends State {
        private State next;
        private Coord start;
        private int fails;
        public WaitWalkStart(State next) {
            this.next=next;
            start = new Coord(context().gui().map.player().rc);

        }
        private double t;
        public void tick(double dt) {
            t += dt;
            totalTasktime += dt;
            if (t > 1) {
                if (context().gui().map.player().rc.x == start.x && context().gui().map.player().rc.y == start.y) {
                    fails++;
                    context().gui().map.wdgmsg("click", Coord.z, startCoord, 1, 0, 0, 0, startCoord, 0, 0);
                }
                if (fails >3) {
                    stop("Cant reach start position");
                }

                t = 0;
                if (context().gui().map.player().rc.x == startCoord.x &&  context().gui().map.player().rc.y == startCoord.y) {
                    if (next instanceof WaitFeed)
                        context().gui().feedTrough = true;
                    setState(next);
                }
            }
        }
    }

    private class WaitFeed extends State {
        private double t;
        public void tick(double dt) {
            t += dt;
            totalTasktime += dt;
            if (t > 1) {
                t=0;
                for (Widget w = inv.child; w != null; w = w.next) {
                    if (w instanceof GItem )
                        if (((GItem) w).resname().contains("carrot") ) {
                            return;
                        }
                }
                context().gui().map.wdgmsg("click", Coord.z, startCoord, 1, 0, 0, 0, startCoord, 0, 0);
                setState(new WaitWalkStart(new FindObject()));
            }
        }
    }

    // wait for drink
    private class Wait extends State {
        private final double timeout = 6;
        private double t;
        private final int minThreshhold = 50;
        @Override
        public void tick(double dt) {
            t+=dt;
            totalTasktime+=dt;
            if (t>timeout) {
                IMeter.Meter stam = context().gui().getmeter("stam", 0);
                if (stam != null && stam.a < minThreshhold) {
                    if (drinkTries > 2)
                        stop("Nothing to drink");
                    else {
                        context().gui().lowStam = true;
                        t = 0;
                        drinkTries++;
                        return;
                    }
                }
                IMeter.Meter nrj = context().gui().getmeter("nrj", 0);
                if (nrj.a > 20) {
                    drinkTries = 0;
                    setState(new FindObject());
                }
                else
                    stop("No energy left");
            }
        }
    }

    private void dropitems() {
        Inventory inv = context().playerInventory();
        for (Widget w = inv.child; w != null; w = w.next) {
            if (w instanceof GItem ) {
                if (!(((GItem) w).resname().contains("keyring") || ((GItem) w).resname().contains("waterflask"))){
                    w.wdgmsg("drop", Coord.z);
                }
            }
        }
    }

    private boolean getFreeslots() {
        boolean hasFeed = false;
        slots = 16;
        if (context().gui().getEquipory().slots[11]!=null && context().gui().getEquipory().slots[11].item.resname().contains("backpack-leather"))
            slots+=8;
        if (context().gui().getEquipory().slots[10]!= null && context().gui().getEquipory().slots[10].item.resname().contains("merchant")) {
            if (slots == 16)
                slots += 4;
            else
                slots += 6;
        }
        try {
            for (Widget w = inv.child; w != null; w = w.next) {
                if (w instanceof GItem) {
                    slots--;
                    if (((GItem) w).resname().contains("carrot"))
                        hasFeed=true;
                }
            }
        } catch (Exception e) {
        }
        context().info("Slots: "+slots);
        return hasFeed;
    }
}
