package haven.stuff;

import haven.*;
import haven.tasks.HarvestTask;

public class HarvesterSelection extends TilesGrabber {
    private boolean clicked;


    public HarvesterSelection(GameUI gui) {
        super("Select tiles...", gui);
    }

    @Override
    protected void done(Coord tile, Coord tile2) {
        if (tile != null && tile2 != null) {
            ui.gui.map.wdgmsg("itemact", Coord.z, tile.mul(MCache.tilesz).add(MCache.tilesz.div(2)), ui.modflags());
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