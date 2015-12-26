package haven.tasks;

import haven.*;

/**
 * Created by Niko on 06.12.2015.
 */
public class HarvestTaskPlus extends FsmTask {

    private static final String PLANT = "gfx/terobjs/plants/";
    private static final String[] PLANTS = {PLANT+"barley", PLANT+"beet", PLANT+"carrot", PLANT+"flax", PLANT+"hemp", PLANT+"lettuce", PLANT+"pipeweed",
            PLANT+"poppy", PLANT+"pumpkin", PLANT+"yellowonion"};
    private static final String[] INVPLANTS = {"carrot", "beet"};

    private Coord startCoord;
    private final Coord start, end;
    private Coord curPos;
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
            stam = context().gui().getmeter("stam", 0);
            inv = context().playerInventory();
            curPos = context().gui().map.player().rc;
            boolean hasFeed = getFreeslots();
            if (slots < 12) {
                if (hasFeed) {
                    setState(new WaitWalk(new WaitFeed()));
                    return;
                }   else
                    stop("Not enough empty inventory slots");
            }
            if (!startHarvest())
                stop("Finished harvesting in " + (int) (totalTasktime / 60) + "m " + (int) (totalTasktime % 60) + "s");
        }
    }
    private boolean startHarvest() {
        Gob object = context().findObjectInBoundingBox(start,end, PLANTS);
        if (object != null) {
            context().gui().map.wdgmsg("click", object.sc, object.rc, 3, 1, 0, (int)object.id, object.rc, 0, 42);
            setState(new WaitCursor());
            return true;
        }
        return false;
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
        private final int threshHold = 40;
        private double t;
        int filledSlots;
        int filledSlotsLast;
        int slotCounter;
        @Override
        public void tick(double dt) {
            t += dt;
            totalTasktime += dt;
            filledSlots=0;
            stam = context().gui().getmeter("stam", 0);
            if (stam != null && stam.a < threshHold) {
                context().gui().lowStam = true;
                setState(new Wait());
                return;
            }
            // check items
            if (t > .2) {
                t=0;
                try {
                    for (Widget w = inv.child; w != null; w = w.next) {
                        if (w instanceof GItem )
                            if (((GItem) w).resname().contains("carrot") || ((GItem)w).resname().contains("beet") ) {
                                filledSlots++;
                                lastItem = (GItem) w;
                            }
                    }
                } catch (Exception e) {
                }
                if (filledSlots >= slots) {
                    setState(new Replant());
                }
                if (filledSlotsLast==filledSlots) {
                    slotCounter++;
                    if (slotCounter > 50) {
                        setState(new Replant());
                    }
                } else {
                    slotCounter = 0;
                }
                filledSlotsLast=filledSlots;
            }
        }
    }
    private class Replant extends State {
        @Override
        public void tick(double dt) {
            dropHand();
            if (lastItem != null) {
                lastItem.wdgmsg("iact", lastItem.c, 1);
                context().gui().map.wdgmsg("sel", start, end, 0);
                lastPC = new Coord(context().gui().map.player().rc);
                setState(new WaitReplant());
            } else {
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
                curPos=context().player().rc;
                if (lastPC.dist(curPos)==0) {
                    doneCheck++;
                    if (doneCheck > 2) {
                        dropHand();
                        context().gui().map.wdgmsg("click", Coord.z, startCoord, 1, 0, 0, 0, startCoord, 0, 0);
                        setState(new WaitWalk(new WaitFeed()));
                    }
                } else {
                    doneCheck = 0;
                    lastPC.x = curPos.x;
                    lastPC.y = curPos.y;
                }
            }
        }
    }

    private class WaitWalk extends State {
        private State next;
        private Coord curPos;
        private Coord lastPos;
        private int fails;
        public WaitWalk(State next) {
            this.next=next;
            curPos = context().gui().map.player().rc;
            lastPos = new Coord(context().gui().map.player().rc);
        }
        private double t;
        public void tick(double dt) {
            t += dt;
            totalTasktime += dt;
            if (t > 1) {
                t = 0;
                curPos = context().gui().map.player().rc;
                // check if at starting position
                if (curPos.dist(startCoord)<=.1) {
                    if (next instanceof WaitFeed)
                        context().gui().feedTrough = true;
                    setState(next);
                }
                // check if still walking
                if (curPos.dist(lastPos)==0) {
                    fails++;
                    dropHand();
                    context().gui().map.wdgmsg("click", Coord.z, startCoord, 1, 0, 0, 0, startCoord, 0, 0);
                    if (fails > 3) {
                        stop("Cant reach starting position");
                    }
                }
                lastPos.x=curPos.x;
                lastPos.y=curPos.y;
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
                        if (((GItem) w).resname().contains("carrot") || ((GItem) w).resname().contains("beet")) {
                            return;
                        }
                }
                dropHand();
                context().gui().map.wdgmsg("click", Coord.z, startCoord, 1, 0, 0, 0, startCoord, 0, 0);
                lastItem = null;
                setState(new WaitWalk(new FindObject()));
            }
        }
    }

    // wait for drink
    private class Wait extends State {
        private final double timeout = 6;
        private double t;
        private final int minThreshhold = 70;
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
                    if (((GItem) w).resname().contains("carrot") || ((GItem) w).resname().contains("beet"))
                        hasFeed=true;
                }
            }
        } catch (Exception e) {
        }
        slots = slots - slots % 3;
        return hasFeed;
    }

    private void dropHand(){
        GItem item = context().getItemAtHand();
        if (item != null) {
            item.wdgmsg("drop", startCoord, startCoord, 0);
        }
    }
}
