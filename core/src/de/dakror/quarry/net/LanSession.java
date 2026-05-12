package de.dakror.quarry.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.Gdx;

import de.dakror.common.Callback;
import de.dakror.common.libgdx.io.NBT;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.CompressionType;
import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.util.NbtIO;

public final class LanSession {
    private static final byte HELLO = 1;
    private static final byte SNAPSHOT = 2;
    private static final byte COMMAND = 3;

    public static final int DEFAULT_PORT = 7777;

    private final Game game;
    private final boolean host;
    private final long localClientId = System.nanoTime();

    private final Object clientsLock = new Object();
    private final List<ClientConnection> clients = new ArrayList<>();

    private volatile boolean running;
    private ServerSocket serverSocket;
    private Socket socket;
    private Thread acceptThread;
    private Thread readThread;
    private Callback<Object> loadCallback;
    private volatile boolean cursorCommandDebugLogged;
    private volatile boolean disconnectNotified;

    private LanSession(Game game, boolean host) {
        this.game = game;
        this.host = host;
        this.running = true;
    }

    public static LanSession startHost(Game game, int port) throws IOException {
        LanSession session = new LanSession(game, true);
        session.serverSocket = new ServerSocket(port);
        session.acceptThread = new Thread(session::acceptLoop, "lan-host-accept");
        session.acceptThread.setDaemon(true);
        session.acceptThread.start();
        return session;
    }

    public static LanSession connect(Game game, String host, int port, Callback<Object> loadCallback) throws IOException {
        LanSession session = new LanSession(game, false);
        session.loadCallback = loadCallback;
        session.socket = new Socket(host, port);
        session.readThread = new Thread(session::clientReadLoop, "lan-client-read");
        session.readThread.setDaemon(true);
        session.readThread.start();
        session.sendHello();
        return session;
    }

    public boolean isHost() {
        return host;
    }

    public boolean isClient() {
        return !host;
    }

    public long getLocalClientId() {
        return localClientId;
    }

    public boolean hasClients() {
        synchronized (clientsLock) {
            for (ClientConnection c : clients) {
                if (c.running) {
                    return true;
                }
            }
        }
        return false;
    }

    public void sendCommand(CompoundTag command) {
        if (host) {
            return;
        }

        try {
            writeCommand(socket, command);
        } catch (IOException e) {
            notifyHostDisconnected();
        }
    }

    public void broadcastCommand(CompoundTag command, long excludeClientId) {
        if (!host) {
            return;
        }

        synchronized (clientsLock) {
            Iterator<ClientConnection> it = clients.iterator();
            while (it.hasNext()) {
                ClientConnection c = it.next();
                if (!c.running) {
                    it.remove();
                    continue;
                }

                if (excludeClientId != 0 && c.clientId == excludeClientId) {
                    continue;
                }

                try {
                    synchronized (c) {
                        if (c.running) {
                            writeCommand(c.socket, command);
                        }
                    }
                } catch (IOException e) {
                    c.close();
                    it.remove();
                }
            }
        }
    }

    public LanSnapshot createSnapshot() {
        return game.exportLanSnapshot();
    }

    public void close() {
        running = false;
        if (acceptThread != null) acceptThread.interrupt();
        if (readThread != null) readThread.interrupt();

        synchronized (clientsLock) {
            for (ClientConnection c : clients) {
                c.close();
            }
            clients.clear();
        }

        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}

        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket accepted = serverSocket.accept();
                ClientConnection client = new ClientConnection(accepted);
                synchronized (clientsLock) {
                    clients.add(client);
                }
                client.start();
            } catch (SocketException e) {
                return;
            } catch (IOException e) {
                if (running) {
                    Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
                }
            }
        }
    }

    private void sendHello() throws IOException {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeByte(HELLO);
        out.writeLong(localClientId);
        out.flush();
    }

    private void clientReadLoop() {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            while (running) {
                byte type = in.readByte();
                if (type == SNAPSHOT) {
                    int metaLen = in.readInt();
                    byte[] meta = new byte[metaLen];
                    in.readFully(meta);
                    int dataLen = in.readInt();
                    byte[] data = new byte[dataLen];
                    in.readFully(data);
                    handleSnapshot(new LanSnapshot(meta, data));
                } else if (type == COMMAND) {
                    long from = in.readLong();
                    int len = in.readInt();
                    byte[] payload = new byte[len];
                    in.readFully(payload);
                    applyCommand(payload, from);
                }
            }
        } catch (IOException e) {
            if (running) {
                notifyHostDisconnected();
            }
        }
    }

    private void handleSnapshot(LanSnapshot snapshot) {
        try {
            String tempName = "__lan_join_" + System.currentTimeMillis();
            File meta = Quarry.Q.file("TheQuarry/saves/" + tempName + ".qmf", true).file();
            File data = Quarry.Q.file("TheQuarry/saves/" + tempName + ".qsf", true).file();
            meta.getParentFile().mkdirs();
            java.nio.file.Files.write(meta.toPath(), snapshot.metaBytes);
            java.nio.file.Files.write(data.toPath(), snapshot.dataBytes);

            game.load(tempName, new Callback<Object>() {
                @Override
                public void call(Object result) {
                    if (loadCallback != null) {
                        loadCallback.call(result);
                    }
                    cleanupTempSnapshot(tempName);
                }
            });
        } catch (Exception e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
            if (loadCallback != null) {
                loadCallback.call(e);
            }
        }
    }

    private void cleanupTempSnapshot(String tempName) {
        try {
            Quarry.Q.file("TheQuarry/saves/" + tempName + ".qmf", true).delete();
        } catch (Exception ignored) {}
        try {
            Quarry.Q.file("TheQuarry/saves/" + tempName + ".qsf", true).delete();
        } catch (Exception ignored) {}
    }

    private void applyCommand(byte[] payload, long fromClientId) {
        try {
            CompoundTag tag = NBT.read(new ByteArrayInputStream(payload), CompressionType.Small);
            if (tag == null) {
                System.out.println("lan command decode returned null payloadLen=" + (payload == null ? -1 : payload.length)
                        + " from=" + fromClientId);
                return;
            }
            if (!tag.has("client")) {
                tag.add(new NBT.LongTag("client", fromClientId));
            }
            String kind = tag.String("kind", "");
            if ("cursor".equals(kind)) {
                if (!cursorCommandDebugLogged) {
                    cursorCommandDebugLogged = true;
                    System.out.println("lan cursor recv host=" + host + " fromPacket=" + fromClientId
                            + " client=" + tag.Long("client", -1) + " layer=" + tag.Int("layer", -1)
                            + " sx=" + tag.Int("sx", -1) + " sy=" + tag.Int("sy", -1)
                            + " wx=" + tag.Float("wx", -1f) + " wy=" + tag.Float("wy", -1f));
                }
                game.applyLanCursor(tag);
                if (host) {
                    broadcastCommand(tag, fromClientId);
                }
                return;
            } else if ("build_preview".equals(kind)) {
                game.applyLanPlacementPreview(tag);
                if (host) {
                    broadcastCommand(tag, fromClientId);
                }
                return;
            }
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    game.applyLanCommand(tag);
                    if (host) {
                        broadcastCommand(tag, fromClientId);
                    }
                }
            });
        } catch (Exception e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
        }
    }

    private void notifyHostDisconnected() {
        if (host || !running || disconnectNotified) {
            return;
        }

        disconnectNotified = true;
        close();

        Runnable action = new Runnable() {
            @Override
            public void run() {
                game.returnToMainMenu();
            }
        };
        if (Gdx.app != null) {
            Gdx.app.postRunnable(action);
        } else {
            action.run();
        }
    }

    private void writeCommand(Socket socket, CompoundTag command) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NbtIO.write(baos, command, CompressionType.Small);

        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeByte(COMMAND);
        out.writeLong(localClientId);
        out.writeInt(baos.size());
        out.write(baos.toByteArray());
        out.flush();
    }

    private final class ClientConnection {
        final Socket socket;
        volatile boolean running = true;
        long clientId;
        Thread thread;

        ClientConnection(Socket socket) {
            this.socket = socket;
        }

        void start() {
            thread = new Thread(() -> {
                try {
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                    byte type = in.readByte();
                    if (type == HELLO) {
                        clientId = in.readLong();
                    }

                    LanSnapshot snapshot = createSnapshot();
                    out.writeByte(SNAPSHOT);
                    out.writeInt(snapshot.metaBytes.length);
                    out.write(snapshot.metaBytes);
                    out.writeInt(snapshot.dataBytes.length);
                    out.write(snapshot.dataBytes);
                    out.flush();

                    while (running) {
                        byte packet = in.readByte();
                        if (packet == COMMAND) {
                            long from = in.readLong();
                            int len = in.readInt();
                            byte[] payload = new byte[len];
                            in.readFully(payload);
                            applyCommand(payload, from);
                            if (!host) {
                                // no-op
                            }
                        }
                    }
                } catch (SocketException | EOFException e) {
                    // Normal client disconnect.
                } catch (IOException e) {
                    if (running && !socket.isClosed()) {
                        Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
                    }
                } finally {
                    game.removeRemoteCursor(clientId);
                    game.removeRemotePlacementPreview(clientId);
                    close();
                }
            }, "lan-host-client");
            thread.setDaemon(true);
            thread.start();
        }

        void close() {
            running = false;
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}
