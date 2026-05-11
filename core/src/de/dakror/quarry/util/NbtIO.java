package de.dakror.quarry.util;

import java.io.IOException;
import java.io.OutputStream;

import de.dakror.common.libgdx.io.NBT;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.CompressionType;

public final class NbtIO {
    private static final Object WRITE_LOCK = new Object();

    private NbtIO() {}

    public static void write(OutputStream os, CompoundTag data, CompressionType compression) throws IOException {
        synchronized (WRITE_LOCK) {
            NBT.write(os, data, compression);
        }
    }
}
