package de.dakror.quarry.net;

public class LanSnapshot {
    public final byte[] metaBytes;
    public final byte[] dataBytes;

    public LanSnapshot(byte[] metaBytes, byte[] dataBytes) {
        this.metaBytes = metaBytes;
        this.dataBytes = dataBytes;
    }
}
