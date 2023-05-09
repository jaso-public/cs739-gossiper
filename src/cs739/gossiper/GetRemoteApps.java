package cs739.gossiper;

import java.net.Socket;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import cs739.gossiper.messages.Gossip;
import cs739.gossiper.messages.Message;

public class GetRemoteApps {
    private static final Logger logger = LogManager.getLogger(GetRemoteApps.class);
   
    public static void main(String[] args) throws InterruptedException {
        Configurator.initialize(null, "log4j2.xml");

        Config config = new Config();        
        Gossip request = new Gossip(new ArrayList<Application>());
        
        try(Socket socket = new Socket("54.188.42.102", config.bootstrapPort)) {
            logger.info("socket created -- sending message");
            MessageHelper.send(socket.getOutputStream(), request);
            Message reply = MessageHelper.readMessage(socket.getInputStream());
            System.out.println(reply.toString());
            socket.close();
                       
        } catch(Throwable t) {
            logger.error("failed to get gossip list from:"+config.bootstrapHost, t);
        }        
    }
}
