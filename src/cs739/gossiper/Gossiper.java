package cs739.gossiper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import cs739.gossiper.messages.IpAddressReply;
import cs739.gossiper.messages.IpAddressRequest;

public class Gossiper {
	private static final Logger logger = LogManager.getLogger(Gossiper.class);
	
	public final Config config;
	public final DdbInserter ddbInserter;
	public final EventDispatcher eventDispatcher;
	public final DataStore dataStore;
	public final BoundedExecutor executor;
	
	
	public Gossiper(Config config) {
	    this.config = config;
        this.ddbInserter = new DdbInserter(config.myApplicationId);
        this.eventDispatcher = new EventDispatcher();
        this.dataStore = new DataStore(config, eventDispatcher, ddbInserter);
        this.executor = new BoundedExecutor(config.executorPoolSize);
    }


    void loop() throws IOException, InterruptedException {
        try (ServerSocket ss = new ServerSocket(config.listenPort, config.backlog)) {
            while(true) {
                Socket s = ss.accept();
                logger.info("accepted socket:"+s);
                executor.submitTask(new MessageThread(s, config, dataStore, executor));               
            }
        }	    
	}
	

	public static void main(String[] args) throws Exception {
		Configurator.initialize(null, "log4j2.xml");
        Config config = new Config();
        
        
        String myIpAddress = null;

		
		if(args.length > 0 && "bootstrapper".equals(args[0])) {
	        InetAddress[] addresses = InetAddress.getAllByName(config.bootstrapHost);
	        for (InetAddress address : addresses) {
	            System.out.println(address.getHostAddress());
	            myIpAddress = address.getHostAddress();
	        }
		} else {
	       IpAddressRequest request = new IpAddressRequest();
	        
	        try(Socket socket = new Socket(config.bootstrapHost, config.bootstrapPort)) {
	            logger.info("socket created -- sending message:"+request);
	            MessageHelper.send(socket.getOutputStream(), request);
	            IpAddressReply reply = (IpAddressReply) MessageHelper.readMessage(socket.getInputStream());
	            System.out.println(reply.toString());
	            myIpAddress = reply.ipAddress.ipAddress;
	            socket.close();
	        }
		}
		
		logger.info("myIpAddress is:"+myIpAddress);
		
		Gossiper gossiper = new Gossiper(config);
		try {
            gossiper.loop();
        } catch (Exception e) {
            logger.error("loop threw", e);
        }
	}
}
