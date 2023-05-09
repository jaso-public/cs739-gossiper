package cs739.gossiper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import cs739.gossiper.messages.BootstrapReply;
import cs739.gossiper.messages.BootstrapRequest;
import cs739.gossiper.messages.Gossip;
import cs739.gossiper.messages.IpAddressReply;
import cs739.gossiper.messages.IpAddressRequest;

public class Gossiper implements Handler {
	private static final Logger logger = LogManager.getLogger(Gossiper.class);
	
	public final Config config;
    public final String myIpAddress;
    public final String myApplicationId;
	
	public final DdbInserter ddbInserter;
	public final EventDispatcher eventDispatcher;
	public final DataStore dataStore;
	public final BoundedExecutor executor;
	
	
	public Gossiper(Config config, String myApplicationId, String myIpAddress) {
	    this.config = config;
	    this.myIpAddress = myIpAddress;
	    this.myApplicationId = myApplicationId;
	    
        this.ddbInserter = new DdbInserter(myApplicationId);
        this.eventDispatcher = new EventDispatcher();
        this.dataStore = new DataStore(config, eventDispatcher, ddbInserter);
        this.executor = new BoundedExecutor(config.executorPoolSize);
        
        HashMap<String,String> map = new HashMap<>();
        map.put("event", "start");
        ddbInserter.Record(map);
        
        Address address = new Address(myIpAddress, config.listenPort);
        Application application = new Application(Application.GossipingApp, myApplicationId, address, 1);
        dataStore.updateApplication(application);
        
        BootstrapRequest request = new BootstrapRequest(application);
        
        try(Socket socket = new Socket(config.bootstrapHost, config.bootstrapPort)) {
            logger.info("socket created -- sending message");
            MessageHelper.send(socket.getOutputStream(), request);
            BootstrapReply reply = (BootstrapReply) MessageHelper.readMessage(socket.getInputStream());
            System.out.println(reply.toString());
            socket.close();
            for(Application app : reply.applications) dataStore.updateApplication(app);
        } catch(Throwable t) {
            logger.error("failed to get bootstrap list from:"+config.bootstrapHost, t);
        }

        
        eventDispatcher.register(config.heartbeatInterval, this);
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
    
    @Override
    public void onEvent(long now) {
        logger.info("doing a heartbeat for application:"+myApplicationId+" now:"+now);
        dataStore.incrementHeartbeat(myApplicationId);
        eventDispatcher.register(config.heartbeatInterval, this);
        
        try {
            Address peer = dataStore.getRandomPeer();
            executor.submitTask(new SendMessage(peer, new Gossip(dataStore.getApplications())));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    
    private static String getApplicationId(Config config) throws Exception {
        File file = new File(config.pathToApplicationId);
        if(file.exists()) {
           try(BufferedReader br = new BufferedReader( new FileReader(file))) {
               String result = br.readLine();
               logger.info("my existing applicationId:"+result);
               return result;
           }
        }
        
        String applicationId = "gossiper-"+Math.abs(new Random().nextLong());
        try(PrintWriter pw = new PrintWriter(file)) {
            pw.println(applicationId);
        }
        
        logger.info("my newly created applicationId:"+applicationId);
        return applicationId;
    }
    
    
    private static String getMyIpAddress(Config config, boolean isBootstrapper) throws Exception {
        String myIpAddress = null;

        
        if(isBootstrapper ) {
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
        return myIpAddress;

    }

	

	public static void main(String[] args) throws Exception {
		Configurator.initialize(null, "log4j2.xml");
        Config config = new Config();
        
        boolean isBootstrapper = args.length > 0 && "bootstrapper".equals(args[0]);
        String myIpAddress = getMyIpAddress(config, isBootstrapper);        
        String myApplicationId = getApplicationId(config);       
		
		Gossiper gossiper = new Gossiper(config, myApplicationId, myIpAddress);
		try {
            gossiper.loop();
        } catch (Exception e) {
            logger.error("loop threw", e);
        }
	}

}
