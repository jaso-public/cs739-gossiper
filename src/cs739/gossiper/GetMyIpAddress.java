package cs739.gossiper;

import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import cs739.gossiper.messages.IpAddressRequest;
import cs739.gossiper.messages.Message;

public class GetMyIpAddress {
    private static final Logger logger = LogManager.getLogger(GetMyIpAddress.class);

    public static void main(String[] args) throws Exception {
        Configurator.initialize(null, "log4j2.xml");

        IpAddressRequest request = new IpAddressRequest();

        try (Socket socket = new Socket(Config.get().bootstrapHost, Config.get().bootstrapPort)) {
            logger.info("socket created -- sending message:" + request);
            MessageHelper.send(socket.getOutputStream(), request);
            Message reply = MessageHelper.readMessage(socket.getInputStream());
            System.out.println(reply.toString());
            socket.close();

        } catch (Throwable t) {
            logger.error("failed to get gossip list from:" + Config.get().bootstrapHost, t);
        }
    }
}
