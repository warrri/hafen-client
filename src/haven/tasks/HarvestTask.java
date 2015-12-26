package haven.tasks;

import haven.*;

/**
 * Created by Niko on 01.12.2015.
 */
public class HarvestTask extends FsmTask
{
    private static final String PLANT = "gfx/terobjs/plants/";
    private static final String[] PLANTS = {PLANT+"barley", PLANT+"beet", PLANT+"carrot", PLANT+"flax", PLANT+"hemp", PLANT+"lettuce", PLANT+"pipeweed",
    PLANT+"poppy", PLANT+"pumpkin", PLANT+"yellowonion"};



    private final Coord start, end;
    private double totalTasktime;
    private double failTime;
    private int failAmount;
    private int drinkTries;

    public HarvestTask(Coord start, Coord end) {
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
            // find plant
            if(!startHarvest())
                stop("Nothing to harvest");
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
        private final int threshHold=40;
        private double t;

        @Override
        public void tick(double dt) {
            t+=dt;
            totalTasktime+=dt;
            IMeter.Meter stam = context().gui().getmeter("stam", 0);
            if (stam != null && stam.a < threshHold) {
                context().gui().lowStam = true;
                setState(new Wait());
                return;
            }
            // failsafe
            if (context().gui().prog<0) {
                if (failAmount > 3) {
                    stop("Sum Ting Wong");
                }
                failTime+=dt;
                if (failTime>5) {
                    dropitems();
                    failAmount++;
                    failTime=0;
                    // check if done
                    if (!startHarvest()) {
                        GItem q =context().getItemAtHand();
                        if (q!=null)
                            q.wdgmsg("drop", Coord.z);
                        stop("Finished harvesting in " + (int) (totalTasktime / 60) + "m " + (int) (totalTasktime % 60) + "s");
                    }
                }
            } else
                failTime = 0;
            // drop inventory every 10s
            if (t>10) {
                t=0;
                dropitems();
            }
        }
    }
    private void dropitems() {
        Inventory inv = context().playerInventory();
        for (Widget w = inv.child; w != null; w = w.next) {
            if (w instanceof GItem ) {
                if (!(((GItem) w).resname().contains("keyring") || ((GItem) w).resname().contains("waterflask") || ((GItem) w).resname().contains("travel"))){
                    GItem item = (GItem) w;
                    item.wdgmsg("drop", Coord.z, Coord.z, 0);
                }
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
                if (nrj.a > 25) {
                    drinkTries = 0;
                    setState(new FindObject());
                }
                else
                    stop("No energy left");
            }
        }
    }
}