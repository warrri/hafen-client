package haven.tasks;

import haven.*;

import java.util.List;

/**
 * Created by Niko on 03.12.2015.
 */
public class DigTask extends FsmTask{
    private Coord startCoord;
    private Gob flag;
    private int slots=16;
    private int failAmount;
    private int drinkTries;
    private int threshHold = 30;
    private double failTime;
    Inventory inv;

    @Override
    protected State getInitialState() {
        return new FindObject();
    }

    private class FindObject extends State {
        @Override
        public void tick(double dt) {
            slots = 16;
            if (context().gui().getEquipory().slots[11]!=null && context().gui().getEquipory().slots[11].item.resname().contains("backpack-leather"))
                slots+=8;
            if (context().gui().getEquipory().slots[10]!= null && context().gui().getEquipory().slots[10].item.resname().contains("merchant")) {
                if (slots == 16)
                    slots += 4;
                else
                    slots += 6;
            }
            inv = context().playerInventory();
            try {
                for (Widget w = inv.child; w != null; w = w.next) {
                    if (w instanceof GItem )
                        if (!(((GItem) w).resname().contains("soil") || ((GItem) w).resname().contains("worm") || ((GItem) w).resname().contains("oddtuber")))
                            slots--;
                }
            } catch (Exception e) {
            }
            if (startCoord == null) {
                startCoord = context().player().rc;
            }
            flag = context().findObjectByName(666, false, "survobj");
            if (flag == null) {
                stop("Cant find a flag!");
            }
            if (startCoord != null && flag != null) {
                setState(new DigDirt());
            }
        }
    }

    private class DigDirt extends State {
        @Override
        public void tick(double dt) {
            List<Window> DigWindow = context().findWindows("Land survey");
            if (DigWindow.size() == 0) {
                context().click(flag, 3, 0);
                failAmount++;
                if (failAmount > 10) {
                    stop("Can't get to the flag");
                }
                setState(new Wait(5, this));
            } else {
                DigWindow.get(0).wdgmsg("lvl", 4);
                failAmount =0;
                setState(new CheckStam());
            }
        }
    }

    private class CheckStam extends State {
        @Override
        public void tick(double dt) {
            IMeter.Meter stam = context().gui().getmeter("stam", 0);
            if (stam != null && stam.a < threshHold) {
                context().gui().lowStam = true;
                setState(new DrinkWait());
                return;
            }
            // failsafe
            if (context().gui().prog<0) {
                if (failAmount > 3) {
                    stop("Sum Ting Wong");
                }
                failTime+=dt;
                if (failTime>5) {
                    failAmount++;
                    failTime=0;
                    setState(new FindObject());
                }
            } else
                failTime = 0;
            Integer dirtcount = 0;
            try {
                for (Widget w = inv.child; w != null; w = w.next) {
                    if (w instanceof GItem) {
                        if (((GItem) w).resname().contains("soil") || ((GItem) w).resname().contains("worm") || ((GItem) w).resname().contains("oddtuber"))
                            dirtcount += 1;
                    }
                }
            } catch (Exception e) {
            }
            if (dirtcount >= slots)
                setState(new DropDirt());
        }
    }



    private class DropDirt extends State {
        @Override
        public void tick(double dt) {
            if (context().player().rc.x != startCoord.x && context().player().rc.y != startCoord.y) {
                context().clicktile(startCoord);
                setState(new Wait(5, this));
            } else {
                Inventory inv = context().playerInventory();
                try {
                    for (Widget w = inv.child; w != null; w = w.next) {
                        if (w instanceof GItem ) {
                            if (((GItem) w).resname().contains("soil") || ((GItem) w).resname().contains("worm") || ((GItem) w).resname().contains("oddtuber")){
                                GItem item = (GItem) w;
                                item.wdgmsg("drop", Coord.z);
                            }
                        }
                    }
                } catch (Exception e) {
                }
                setState(new Wait(1,new FindObject()));
            }
        }
    }

    private class Wait extends State {
        private final State next;
        private final double timeout;
        private double t;

        public Wait(double timeout, State next) {
            this.next = next;
            this.timeout = timeout;
        }

        @Override
        public void tick(double dt) {
            t += dt;
            if (t > timeout)
                setState(next);
        }
    }

    // wait for drink
    private class DrinkWait extends State {
        private final double timeout = 6;
        private double t;
        private final int minThreshhold = 50;
        @Override
        public void tick(double dt) {
            t+=dt;
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
                // todo change to 25% to prevent dying
                IMeter.Meter nrj = context().gui().getmeter("nrj", 0);
                if (nrj.a > 1) {
                    drinkTries = 0;
                    setState(new FindObject());
                }
                else
                    stop("No energy left");
            }
        }
    }

}
