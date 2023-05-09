package cs739.gossiper;

import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cs739.gossiper.messages.Gossip;

public class SendGossip implements Runnable {
    private static final Logger logger = LogManager.getLogger(SendGossip.class);

    private final DataStore dataStore;
    
    
    public SendGossip(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void run() {
        Address address = dataStore.getRandomPeer();
        logger.info("Gossiping with:"+address);

        try(Socket socket = new Socket(address.ipAddress, address.port)) {
            Gossip request = new Gossip(dataStore.getApplications());
            MessageHelper.send(socket.getOutputStream(), request); 
            Gossip reply = (Gossip)MessageHelper.readMessage(socket.getInputStream());
            for(Application app : reply.applications) dataStore.updateApplication(app);
        } catch(Throwable t) {
            logger.error("failed to send to "+address, t);
        }
    }
}
