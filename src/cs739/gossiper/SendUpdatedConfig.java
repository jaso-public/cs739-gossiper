package cs739.gossiper;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import cs739.gossiper.messages.Gossip;
import cs739.gossiper.messages.UpdateConfigRequest;

public class SendUpdatedConfig {
    private static final Logger logger = LogManager.getLogger(SendUpdatedConfig.class);

    public static void main(String[] args) throws Exception {
        Configurator.initialize(null, "log4j2.xml");
        Configurator.setRootLevel(Level.INFO);

        Config config = new Config();
        config.doPushGossip = false;
        config.doPullGossip = true;
        config.timeToIncommunicado = 15000;
        UpdateConfigRequest updateRequest = new UpdateConfigRequest(config);

        Gossip gossipRequest = new Gossip(new ArrayList<Application>(), true);
        Gossip gossipReply = new Gossip(new ArrayList<Application>(), true);

        try (Socket socket = new Socket(Config.get().bootstrapHost, Config.get().bootstrapPort)) {
            logger.info("socket created -- sending message");
            MessageHelper.send(socket.getOutputStream(), gossipRequest);
            gossipReply = (Gossip) MessageHelper.readMessage(socket.getInputStream());
            System.out.println(gossipReply);
            socket.close();

        } catch (Throwable t) {
            logger.error("failed to get gossip list from:" + Config.get().bootstrapHost, t);
        }

        List<Thread> threads = new ArrayList<>();

        for (Application application : gossipReply.applications) {
            logger.info("Sending to " + application.address);
            Thread thread = new Thread(
                    new SendMessage(application.address, updateRequest));
            thread.start();
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.join();
        }

    }

}
