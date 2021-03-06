package haven;

public class MinimapWnd extends Window {

    static final Tex grip = Resource.loadtex("gfx/hud/gripbr");
    static final Coord gzsz = new Coord(19, 18);
    static final Coord minsz = new Coord(150, 125);

    private final MapView map;
    private final LocalMiniMap minimap;
    private IButton vclaimButton;
    private IButton pclaimButton;
    private IButton centerButton;
    private IButton radiusButton;
    private IButton radarButton;
    private IButton gridButton;
    private Coord doff;
    private boolean folded;
    private UI.Grab resizegrab = null;

    public MinimapWnd(Coord c, Coord sz, MapView map, LocalMiniMap minimap) {
        super(sz, "Minimap");
        this.map = map;
        this.minimap = minimap;
        this.c = c;
        add(minimap, 0, 0);
        initbuttons();
        setMargin(Coord.z);
    }

    public void draw(GOut g) {
        super.draw(g);
        if (!folded) {
            g.image(grip, sz.sub(gzsz));
        }
    }

    public boolean mousedown(Coord c, int button) {
        if(folded)
            return super.mousedown(c, button);
        parent.setfocus(this);
        raise();
        if (button == 1) {
            doff = c;
            if(c.isect(sz.sub(gzsz), gzsz)) {
                resizegrab = ui.grabmouse(this);
                return true;
            }
        }
        return super.mousedown(c, button);
    }

    public boolean mouseup(Coord c, int button) {
        if (resizegrab != null) {
            resizegrab.remove();
            resizegrab = null;
            Config.minimapSize.set(minimap.sz);
        } else {
            super.mouseup(c, button);
        }
        return (true);
    }

    public void mousemove(Coord c) {
        if (resizegrab != null) {
            Coord d = c.sub(doff);
            minimap.sz = minimap.sz.add(d);
            minimap.sz.x = Math.max(minsz.x, minimap.sz.x);
            minimap.sz.y = Math.max(minsz.y, minimap.sz.y);
            doff = c;
            pack();
        } else {
            super.mousemove(c);
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if(sender == cbtn) {
            togglefold();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public boolean type(char key, java.awt.event.KeyEvent ev) {
        if(key == 27) {
            wdgmsg(cbtn, "click");
            return(true);
        }
        return(super.type(key, ev));
    }

    public void move(Coord c) {
        super.move(c);
        Config.minimapPosition.set(this.c);
    }

    public void togglefold() {
        folded = !folded;
        // TODO: toolbar widget?
        minimap.visible = !folded;
        vclaimButton.visible = !folded;
        pclaimButton.visible = !folded;
        centerButton.visible = !folded;
        radiusButton.visible = !folded;
        radarButton.visible = !folded;
        gridButton.visible = !folded;
        if (folded) {
            resize(new Coord(minimap.sz.x, 0));
        } else {
            resize(Config.minimapSize.get());
        }
    }

    private void initbuttons() {
        vclaimButton = add(new IButton("gfx/hud/lbtn-vil", "", "-d", "-h") {
            { tooltip = Text.render("Display personal claims");  }

            public void click() {
                if ((map != null) && !map.visol(0))
                    map.enol(0, 1);
                else
                    map.disol(0, 1);
            }
        }, -6, -5);

        pclaimButton = add(new IButton("gfx/hud/lbtn-claim", "", "-d", "-h") {
            { tooltip = Text.render("Display village claims"); }

            public void click() {
                if ((map != null) && !map.visol(2))
                    map.enol(2, 3);
                else
                    map.disol(2, 3);
            }
        }, -6, -10);

        centerButton = add(new IButton("gfx/hud/buttons/center", "-u", "-d", "-d") {
            { tooltip = Text.render("Center map"); }

            public void click() {
                minimap.setOffset(Coord.z);
            }
        }, 53, 3);

        radiusButton = add(new IButton("gfx/hud/buttons/dispradius", "", "", "") {
            { tooltip = Text.render("Toggle view radius"); }

            public void click() {
                minimap.toggleRadius();
            }
        }, 78, 3);

        radarButton = add(new IButton("gfx/hud/buttons/radar", "", "", "") {
            { tooltip = Text.render("Select icons to display"); }

            public void click() {
                getparent(GameUI.class).iconwnd.toggle();
            }
        }, 103, 3);

        gridButton = add(new IButton("gfx/hud/buttons/grid", "", "", "") {
            { tooltip = Text.render("Toggle grid"); }

            public void click() {
                minimap.toggleGrid();
            }
        }, 128, 3);
    }
}