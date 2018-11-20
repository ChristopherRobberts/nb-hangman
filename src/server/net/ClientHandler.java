package server.net;

import common.FromClient;
import common.FromServer;
import server.controller.Controller;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;

public class ClientHandler implements Runnable {
    private Controller controller = new Controller();
    private SocketChannel socketChannel;
    private ByteBuffer byteBuffer = ByteBuffer.allocateDirect(2048);
    private String receivedMsg;
    private Selector selector;
    private final Queue<ByteBuffer> messagesToClient = new ArrayDeque<>();
    private String DELIMITER = ":";
    private MessageParser parsedMessage;

    public ClientHandler(SocketChannel socketChannel, Selector selector) throws IOException {
        this.socketChannel = socketChannel;
        this.selector = selector;
    }

    public void run() {
        this.parsedMessage = new MessageParser(this.receivedMsg);
        switch (parsedMessage.command) {
            case START:
                this.controller.startGame();
                break;
            case GUESS:
                if (this.controller.gameIsOngoing()) {
                    this.controller.processGuess(parsedMessage.body);
                }
                break;
            case DISCONNECT:
                try {
                    socketChannel.keyFor(selector).cancel();
                    socketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            default:
                break;
        }

        socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
        selector.wakeup();
    }

    private void disconnectClient() {
        try {
            socketChannel.keyFor(selector).cancel();
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void readMessage() throws IOException {
        byteBuffer.clear();
        int readBytes = socketChannel.read(byteBuffer);
        if (readBytes == -1) {
            disconnectClient();
            return;
        }

        byteBuffer.flip();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        this.receivedMsg = new String(bytes);
        new Thread(this).start();
    }

    private void writeMessage() throws IOException {
        ByteBuffer message;
        synchronized (messagesToClient) {
            while ((message = messagesToClient.peek()) != null) {
                socketChannel.write(message);
                messagesToClient.remove();
            }
        }
    }

    void sendToClient() {
        String wordState;
        if ((!this.controller.gameIsOngoing() && this.controller.gameIsWon()) || this.controller.gameIsOngoing()) {
            wordState = stateConstructor();
        } else {
            wordState = String.valueOf(FromServer.NOT_INITIALIZED);
        }

        synchronized (messagesToClient) {
            messagesToClient.add(ByteBuffer.wrap(wordState.getBytes()));
        }
        try {
            writeMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
        socketChannel.keyFor(selector).interestOps(SelectionKey.OP_READ);
        selector.wakeup();

    }

    private String stateConstructor() {
        return String.valueOf(FromServer.STATE) + DELIMITER +
                this.controller.getWordState() + DELIMITER +
                this.controller.getAttemptsState() + DELIMITER +
                this.controller.getScoreState();
    }

    private class MessageParser {
        private FromClient command;
        private String body;

        private MessageParser(String receivedMessage) {
            System.out.println(receivedMessage);
            String[] parts = receivedMessage.split(DELIMITER);
            if (parts.length < 1 || parts.length > 2) {
                this.command = FromClient.UNKNOWN;
                this.body = "";
            } else {
                this.command = FromClient.valueOf(parts[0]);
                this.body = (parts.length > 1) ? parts[1] : "";
            }
        }
    }
}