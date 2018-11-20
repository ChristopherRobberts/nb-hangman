package client.view;

import client.net.OutputHandler;
import client.net.ServerConnection;
import common.FromClient;

import java.net.InetSocketAddress;

public class CommandLine implements OutputHandler {
    private ServerConnection serverConnection;
    private boolean waitingForInput = false;

    public void start() {
        if (waitingForInput) {
            return;
        }

        this.waitingForInput = true;
        this.serverConnection = new ServerConnection(this);
        CommandLineHandler commandLineHandler = new CommandLineHandler(waitingForInput, serverConnection);
        new Thread(commandLineHandler).start();
    }

    private synchronized void println(String message) {
        System.out.println(message);
    }

    public void connected(InetSocketAddress inetSocketAddress) {
        println("connected to: " + inetSocketAddress.getHostName() + " port: " + inetSocketAddress.getPort());
    }

    public void readWordState(String state) {
        println("Word: " + state);
    }

    public void readAttemptsState(String state) {
        println("Remaining attempts: " + state);
    }

    public void readScoreState(String state) {
        println("Current score: " + state);
    }

    public void notConnected() {
        println("you must connect to the game server to play");
    }

    public void unknown() {
        println("error");
    }

    public void uninitializedGame() {
        println("you must initialize a game before guessing");
    }

    public void disconnected() { println("Bye Bye"); }
}
