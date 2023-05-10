package cs739.gossiper;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cs739.gossiper.messages.Gossip;
import cs739.gossiper.messages.UpdateConfigRequest;

public class SendUpdatedConfig {
    private static final Logger logger = LogManager.getLogger(SendUpdatedConfig.class);

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        config.backlog = 100;
        UpdateConfigRequest updateRequest = new UpdateConfigRequest(config);

        Gossip gossipRequest = new Gossip(new ArrayList<Application>());

        try (Socket socket = new Socket(Config.get().bootstrapHost, Config.get().bootstrapPort)) {
            logger.info("socket created -- sending message");
            MessageHelper.send(socket.getOutputStream(), gossipRequest);
            Gossip reply = (Gossip) MessageHelper.readMessage(socket.getInputStream());
            System.out.println(reply);
            socket.close();

        } catch (Throwable t) {
            logger.error("failed to get gossip list from:" + Config.get().bootstrapHost, t);
        }

        List<Thread> threads = new ArrayList<>();

        for (Application application : gossipRequest.applications) {
            Thread thread = new Thread(new SendMessage(application.address, updateRequest));
            thread.start();
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.join();
        }

    }

}
