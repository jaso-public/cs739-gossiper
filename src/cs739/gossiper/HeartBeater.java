package cs739.gossiper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cs739.gossiper.messages.Heartbeat;

public class HeartBeater implements Handler {
    private static final Logger logger = LogManager.getLogger(HeartBeater.class);
  
    public final EventDispatcher eventDispatcher;
    public final BoundedExecutor executor;
    
     
    public HeartBeater() {
        this.eventDispatcher = new EventDispatcher();
        executor = new BoundedExecutor(10);
        sendHeartbeat();
        eventDispatcher.register(1000, this);
    }
    
    private void sendHeartbeat() {
        logger.info("sending hearbeat");
        Heartbeat heartbeat = new Heartbeat("FooApp", "hb-1234", new Address("127.0.0.1", 56678));
        Address serverAddress = new Address("newjaso.com", 3001);
        try {
            executor.submitTask(new SendMessage(serverAddress, heartbeat));
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            logger.error("send heartbeat");
        }
    }


    @Override
    public void onEvent(long now) {
        sendHeartbeat();
        eventDispatcher.register(1000, this);
    }
    
    public static void main(String[] args) throws InterruptedException {
        @SuppressWarnings("unused")
        HeartBeater heartbeater = new HeartBeater();
        while(true) {
            logger.info("Heartbeater still running");
            Thread.sleep(30);
        }
    }
}
