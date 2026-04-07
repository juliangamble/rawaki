package org.rawaki.core.objects;

import org.rawaki.core.world.World;

public class Explosion extends BoloObject {

    private int lifespan;

    public Explosion(World world) {
        super(world);
        styled = false;
    }

    @Override
    public void spawn(Object... args) {
        if (args.length >= 2 && args[0] instanceof Integer px && args[1] instanceof Integer py) {
            x = px; y = py;
        }
        lifespan = 23;
    }

    @Override
    public void update() {
        if (lifespan-- == 0) {
            world().destroy(this);
        }
    }

    @Override
    public int[] getTile() {
        return switch ((int) Math.floor(lifespan / 3.0)) {
            case 7 -> new int[]{20, 3};
            case 6 -> new int[]{21, 3};
            case 5 -> new int[]{20, 4};
            case 4 -> new int[]{21, 4};
            case 3 -> new int[]{20, 5};
            case 2 -> new int[]{21, 5};
            case 1 -> new int[]{18, 4};
            default -> new int[]{19, 4};
        };
    }

    public int lifespan() { return lifespan; }
    public void setLifespan(int l) { this.lifespan = l; }
}
