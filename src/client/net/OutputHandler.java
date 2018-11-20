package client.net;

import java.net.InetSocketAddress;

public interface OutputHandler {

    void connected(InetSocketAddress inetSocketAddress);

    void notConnected();

    void readWordState(String state);

    void readAttemptsState(String state);

    void readScoreState(String state);

    void unknown();

    void uninitializedGame();

    void disconnected();
}
