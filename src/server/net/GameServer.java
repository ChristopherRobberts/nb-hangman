package server.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;


public class GameServer {
    private int portNumber = 8080;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private static final int LINGER_TIME = 5000;

    public static void main(String[] args) {
        GameServer gameServer = new GameServer();
        gameServer.start();
    }

    private void start() {
        try {
            configureServerSocketChannel();
            while (true) {
                this.selector.select();
                Iterator<SelectionKey> selectionKeyIterator = this.selector.selectedKeys().iterator();

                while (selectionKeyIterator.hasNext()) {
                    SelectionKey key = selectionKeyIterator.next();

                    if (!key.isValid()) continue;
                    if (key.isAcceptable()) {
                        establishCommunication(key);
                    } else if (key.isWritable()) {
                        sendToClient(key);
                    } else if (key.isReadable()) {
                        readFromClient(key);
                    }
                    selectionKeyIterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configureServerSocketChannel() throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.configureBlocking(false);
        this.serverSocketChannel.bind(new InetSocketAddress(portNumber));
        this.selector = Selector.open();
        this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
    }

    private void establishCommunication(SelectionKey key) throws IOException{
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.setOption(StandardSocketOptions.SO_LINGER, LINGER_TIME);
        socketChannel.configureBlocking(false);

        ClientHandler clientHandler = new ClientHandler(socketChannel, this.selector);
        socketChannel.register(this.selector, SelectionKey.OP_READ, clientHandler);
    }

    private void readFromClient(SelectionKey key) {
        ClientHandler handler = (ClientHandler) key.attachment();
        try {
            handler.readMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendToClient(SelectionKey key) {
        ClientHandler handler = (ClientHandler) key.attachment();
        handler.sendToClient();
    }
}