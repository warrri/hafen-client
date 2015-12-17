package haven.stuff;

import haven.*;
import haven.tasks.HarvestTaskPlus;
import haven.tasks.HarvestTask;

public class HarvesterSelection extends TilesGrabber {
    private boolean clicked;
    private boolean plus;

    public HarvesterSelection(GameUI gui, boolean plus) {
        super("Select tiles...", gui);
        this.plus = plus;
    }

    @Override
    protected void done(Coord tile, Coord tile2) {
        if (tile != null && tile2 != null) {
            ui.gui.map.wdgmsg("itemact", Coord.z, tile.mul(MCache.tilesz).add(MCache.tilesz.div(2)), ui.modflags());
            if (plus)
                GUI.tasks.add(new HarvestTaskPlus(ui.gui.map.player().rc,tile,tile2));
            else
                GUI.tasks.add(new HarvestTask(tile,tile2));
            destroy();
            clicked = true;
            hide();
        } else {
            ui.gui.error("Make a selection!");
        }
    }

    @Override
    public void tick(double dt) {
        if (clicked) {
            clicked = false;
        }
    }

    @Override
    public void destroy() {
        // allow widget to be destroyed only when there is no waiting for a click response
        if (!clicked)
            super.destroy();
    }


}