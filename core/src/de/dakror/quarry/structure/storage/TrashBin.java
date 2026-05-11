package de.dakror.quarry.structure.storage;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
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

    public static final Schema classSchema = new Schema(0, StructureType.TrashBin, true, 1, 1, "trashbin",
            new Items(ItemType.Stone, 20), null,
            new Dock(0, 0, Direction.North, DockType.ItemIn),
            new Dock(0, 0, Direction.East, DockType.ItemIn),
            new Dock(0, 0, Direction.South, DockType.ItemIn),
            new Dock(0, 0, Direction.West, DockType.ItemIn))
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
        for (Dock d : getDocks())
            if (d.type.tex != null)
                spriter.add(d.type.tex, (x + d.x) * 64, (y + d.y) * 64, 0.1f, 20, 6, 40, 13, 1, 1, d.dir.ordinal() * -90);

        spriter.add(trashTex, x * 64, y * 64, 0.2f, 32, 32, 64, 64, 1, 1, 0);
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
