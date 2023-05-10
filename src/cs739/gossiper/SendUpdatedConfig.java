package cs739.gossiper;

import java.net.Socket;

import cs739.gossiper.messages.UpdateConfigRequest;

public class SendUpdatedConfig {

    public static void main(String[] args) {
        Config config = new Config();
        config.backlog = 100;

        String host = args[0];

        try (Socket socket = new Socket(host, Config.get().bootstrapPort)) {
            MessageHelper.send(socket.getOutputStream(), new UpdateConfigRequest(config));
            socket.close();
        } catch (Throwable t) {
            System.err.println("failed to send UpdateConfigRequest to " + host + " " + t);
        }
    }

}
