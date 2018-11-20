package client.view;


import client.net.ServerConnection;
import common.FromClient;

import java.io.IOException;
import java.util.Scanner;

public class CommandLineHandler implements Runnable {
    private Scanner scanner = new Scanner(System.in);
    private boolean waitingForInput;
    private ServerConnection serverConnection;
    private String host = "localhost";
    private int port = 8080;
    private String DELIMITER = ":";

    CommandLineHandler(boolean waitingForInput, ServerConnection server) {
        this.waitingForInput = waitingForInput;
        this.serverConnection = server;
    }

    public void run() {
        try {
            while (waitingForInput) {
                String command = scanner.nextLine();
                CommandParser commandParser = new CommandParser(command);
                switch (commandParser.commandType) {
                    case START:
                        serverConnection.sendStartCommand(String.valueOf(commandParser.commandType));
                        break;
                    case GUESS:
                        serverConnection.sendGuess(commandParser.commandType + DELIMITER + commandParser.commandArgs);
                        break;
                    case CONNECT:
                        serverConnection.connect(this.host, this.port);
                        break;
                    case DISCONNECT:
                        serverConnection.disconnect();
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class CommandParser {
        private FromClient commandType;
        private String commandArgs;

        private CommandParser(String command) {
            String[] entireCommand = command.split(" ");
            if (entireCommand.length > 2) {
                this.commandType = FromClient.UNKNOWN;
                return;
            }
            try {
                this.commandType = FromClient.valueOf(entireCommand[0].toUpperCase());
                if (entireCommand.length > 1) {
                    this.commandArgs = entireCommand[1];
                }
            } catch (IllegalArgumentException e) {
                this.commandType = FromClient.UNKNOWN;
            }
        }
    }
}
