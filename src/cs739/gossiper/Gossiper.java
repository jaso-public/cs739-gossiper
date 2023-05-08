package cs739.gossiper;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class Gossiper {
	private static final Logger logger = LogManager.getLogger(Gossiper.class);
	
	public final Config config;
	public final DdbInserter ddbInserter;
	public final EventDispatcher eventDispatcher;
	public final DataStore dataStore;
	public final BoundedExecutor executor;
	
	
	public Gossiper() {
        this.config = new Config();
        this.ddbInserter = new DdbInserter(config.myApplicationId);
        this.eventDispatcher = new EventDispatcher();
        this.dataStore = new DataStore(config, eventDispatcher, ddbInserter);
        this.executor = new BoundedExecutor(config.executorPoolSize);
    }


    void loop() throws IOException, InterruptedException {
        try (ServerSocket ss = new ServerSocket(config.listenPort, config.backlog)) {
            while(true) {
                Socket s = ss.accept();
                executor.submitTask(new MessageThread(s, config, dataStore, executor));               
            }
        }	    
	}
	

	public static void main(String[] args) {
		Configurator.initialize(null, "log4j2.xml");
		Gossiper gossiper = new Gossiper();
		try {
            gossiper.loop();
        } catch (Exception e) {
            logger.error("loop threw", e);
        }
	}
}
