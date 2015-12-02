package haven.stuff;

import haven.*;

public class TilesGrabber extends GrabberWnd {
    static final String title = "Tile selection";
    static final String defaultText = "Selected tiles: (none) (none)";
    Coord one;
    Coord two;
    MCache.Overlay ol;
    MCache.Overlay ol2;
    final MCache map;
    final GameUI GUI;

    public TilesGrabber(String title, GameUI gui) {
        super(title, defaultText, gui);
        this.map = gui.ui.sess.glob.map;
        this.GUI = gui;
    }

    @Override
    public void destroy() {
        if (this.ol != null)
            this.ol.destroy();
        if (this.ol2 != null)
            this.ol2.destroy();
        super.destroy();
    }

    @Override
    protected void done() {
        done(one,two);
    }

    protected void done(Coord tile, Coord tile2) {
    }

    @Override
    public boolean mmousedown(Coord mc, int button) {
        if (isDone())
            return false;
        one = mc.div(MCache.tilesz);
        if (this.ol != null)
            this.ol.destroy();
        this.ol = map.new Overlay(one, one, 1 << 16);
        setLabel(String.format("Selected tile: (%d, %d)", one.x, one.y ));
        return true;
    }

    @Override
    public boolean mmouseup(Coord mc, int button) {
        two = mc.div(MCache.tilesz);
        if (this.ol2 != null)
            this.ol2.destroy();
        this.ol2 = map.new Overlay(two, two, 1 << 16);
        setLabel(String.format("Selected tiles: (%d, %d) (%d, %d)", one.x,one.y,two.x, two.y ));
        return false;
    }

    @Override
    public boolean mmousewheel(Coord mc, int amount) {
        return false;
    }

    @Override
    public void mmousemove(Coord mc) {
    }

    @Override
    public void uimsg(String msg, Object... args) {
        if (msg.equals("reset")) {
            this.ol.destroy();
            this.ol2.destroy();
            this.ol = null;
            this.ol2 = null;
            this.one = null;
            this.two = null;
        }
    }
}