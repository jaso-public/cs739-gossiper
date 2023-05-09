package cs739.gossiper;

import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cs739.gossiper.messages.Message;

public class SendMessage implements Runnable {
	private static final Logger logger = LogManager.getLogger(SendMessage.class);

	private final Address address;
	private final Message message;
	
	
	public SendMessage(Address address, Message message) {
		this.address = address;
		this.message = message;
	}



	@Override
	public void run() {
	    logger.info("creating socket");
        
		try(Socket socket = new Socket(address.ipAddress, address.port)) {
	        logger.info("socket created -- sending message");
		    MessageHelper.send(socket.getOutputStream(), message);					
		} catch(Throwable t) {
			logger.error("failed to send to "+address, t);
		}
	}}
