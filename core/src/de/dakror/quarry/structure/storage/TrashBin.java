package de.dakror.quarry.structure.storage;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import de.dakror.common.libgdx.render.BatchDelegate;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.SpriterDelegateBatch;

public class TrashBin extends Structure<Schema> {
    static final TextureRegion trashTex = new TextureRegion(Quarry.Q.assets.get("trashbin.png", com.badlogic.gdx.graphics.Texture.class));
    static final TextureRegion atlasFallbackTex = Quarry.Q.atlas.findRegion("symb_trash");

    public static final Schema classSchema = new Schema(0, StructureType.TrashBin, true, 1, 1, "trashbin",
            new Items(ItemType.Stone, 5), null,
            new Dock(0, 0, Direction.North, DockType.ItemIn))
                    .flags(Flags.ConfirmDestruction);

    public TrashBin(int x, int y) {
        super(x, y, classSchema);
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        if (item.categories.contains(ItemCategory.Fluid)) return false;
        for (Dock d : getDocks()) {
            if (isNextToDock(x, y, dir, d)) return true;
        }
        return false;
    }

    @Override
    public boolean acceptItem(ItemType item, Structure<?> source, Direction dir) {
        if (item.categories.contains(ItemCategory.Fluid)) return false;
        return true;
    }

    @Override
    public void draw(SpriteRenderer spriter) {
        drawDocks(spriter);
        TextureRegion region = spriter instanceof BatchDelegate || atlasFallbackTex == null ? trashTex : atlasFallbackTex;
        spriter.add(region, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_STRUCTURES,
                Const.TILE_SIZE / 2f, Const.TILE_SIZE / 2f, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 0);
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
    }
}
