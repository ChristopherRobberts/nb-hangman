package client.net;

import common.FromClient;
import common.FromServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class ServerConnection implements Runnable {
    private OutputHandler outputHandler;
    private SocketChannel clientChannel;
    private Selector selector;
    private boolean connected;
    private String host;
    private int port;
    private final Queue<ByteBuffer> messagesToServer = new ArrayDeque<>();
    private ByteBuffer byteBuffer = ByteBuffer.allocateDirect(2048);
    private String DELIMITER = ":";

    public ServerConnection(OutputHandler outputHandler) {
        this.outputHandler = outputHandler;
    }

    public void run () {
        try {
            establishConnection();

            while (connected) {
                selector.select();
                Iterator<SelectionKey> selectionKeyIterator = this.selector.selectedKeys().iterator();

                while (selectionKeyIterator.hasNext()) {
                    SelectionKey key = selectionKeyIterator.next();
                    if (key.isConnectable()) {
                        finishConnection(key);
                    } else if (key.isWritable()) {
                        sendMessageToServer();
                    } else if (key.isReadable()) {
                        readMessageFromServer();
                    }
                    selectionKeyIterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connect(String host, int port) throws IOException{
        this.host = host;
        this.port = port;
        Thread t = new Thread(this);
        t.start();
    }

    private void establishConnection() throws IOException {
        this.connected = true;
        this.clientChannel = SocketChannel.open();
        this.clientChannel.configureBlocking(false);
        this.clientChannel.connect(new InetSocketAddress(this.host, this.port));
        this.selector = Selector.open();
        this.clientChannel.register(this.selector, SelectionKey.OP_CONNECT);
    }

    private void finishConnection(SelectionKey key) throws IOException {
        this.clientChannel.finishConnect();
        key.interestOps(SelectionKey.OP_WRITE);
        outputHandler.connected(new InetSocketAddress(this.host, this.port));
    }

    public void sendStartCommand(String message) {
        if (!connected) {
            outputHandler.notConnected();
            return;
        }
        synchronized (messagesToServer) {
            messagesToServer.add(ByteBuffer.wrap(message.getBytes()));
        }
        try {
            sendMessageToServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToServer() throws IOException{
        ByteBuffer message;
        synchronized (messagesToServer) {
            while ((message = messagesToServer.peek()) != null) {
                clientChannel.write(message);
                if (message.hasRemaining()) {
                    return;
                }

                messagesToServer.remove();
            }
            clientChannel.keyFor(selector).interestOps(SelectionKey.OP_READ);
            selector.wakeup();
        }
    }

    public void sendGuess(String guess) {
        if (!connected) {
            outputHandler.notConnected();
            return;
        }

        synchronized (messagesToServer) {
            messagesToServer.add(ByteBuffer.wrap(guess.getBytes()));
        }
        try {
            sendMessageToServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (!connected) {
            outputHandler.notConnected();
            return;
        }

        synchronized (messagesToServer) {
            messagesToServer.add(ByteBuffer.wrap(String.valueOf(FromClient.DISCONNECT).getBytes()));
        }
        try {
            sendMessageToServer();
            connected = false;
            notifyDisconnected();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readMessageFromServer() throws IOException {
        byteBuffer.clear();
        int nrBytes = clientChannel.read(byteBuffer);
        if (nrBytes == -1) {
            outputHandler.disconnected();
            return;
        }

        byteBuffer.flip();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        serverMessageParser(new String(bytes));
        clientChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
    }

    private void serverMessageParser(String message) {
        String[] parts = message.split(DELIMITER);
        FromServer type = FromServer.valueOf(parts[0]);
        switch (type) {
            case STATE:
                notifyRetrievedState(parts);
                break;
            case NOT_INITIALIZED:
                notifyUninitializedGame();
                break;
            case NO_VALUE:
                notifyUnknownOperation();
                break;
            default:
                notifyUnknownOperation();
                break;
        }
    }

    private void notifyRetrievedState(String[] parts) {
        String[] state = new String[3];
        for (int i = 0; i < state.length; i++) {
            state[i] = parts[i+1];
        }

        Executor ex = ForkJoinPool.commonPool();
        ex.execute(() -> {
            outputHandler.readWordState(state[0]);
            outputHandler.readAttemptsState(state[1]);
            outputHandler.readScoreState(state[2]);
        });
    }

    private void notifyUninitializedGame() {
        Executor ex = ForkJoinPool.commonPool();
        ex.execute(() -> outputHandler.uninitializedGame());
    }

    private void notifyUnknownOperation() {
        Executor ex = ForkJoinPool.commonPool();
        ex.execute(() -> outputHandler.unknown());
    }

    private void notifyDisconnected() {
        Executor ex = ForkJoinPool.commonPool();
        ex.execute(() -> outputHandler.disconnected());
    }
}
