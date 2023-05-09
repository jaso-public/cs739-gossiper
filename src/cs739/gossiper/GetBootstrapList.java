package cs739.gossiper;

import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import cs739.gossiper.messages.BootstrapRequest;
import cs739.gossiper.messages.Message;

public class GetBootstrapList {
    private static final Logger logger = LogManager.getLogger(GetBootstrapList.class);
   
    public static void main(String[] args) throws InterruptedException {
        Configurator.initialize(null, "log4j2.xml");

        Config config = new Config();
        
        while(true) {
            Application app = new Application(Application.GossipingApp, "28934234", new Address("127.0.0.1", 56678), 1);
            System.out.println(app);
            BootstrapRequest request = new BootstrapRequest(app);
            
            try(Socket socket = new Socket("54.188.42.102", config.bootstrapPort)) {
                logger.info("socket created -- sending message");
                MessageHelper.send(socket.getOutputStream(), request);
                Message reply = MessageHelper.readMessage(socket.getInputStream());
                System.out.println(reply.toString());
                socket.close();
                           
            } catch(Throwable t) {
                logger.error("failed to get bootstrap list from:"+config.bootstrapHost, t);
            }
            
            Thread.sleep(1000);    

        }
        
    }

}
