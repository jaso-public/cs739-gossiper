package cs739.gossiper;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cs739.gossiper.messages.BootstrapReply;
import cs739.gossiper.messages.BootstrapRequest;
import cs739.gossiper.messages.Gossip;
import cs739.gossiper.messages.Heartbeat;
import cs739.gossiper.messages.Message;
import cs739.gossiper.messages.MessageType;
import cs739.gossiper.messages.Rumor;

public class MessageThread implements Runnable {

    private static final Logger logger = LogManager.getLogger(MessageThread.class);

    private final Socket socket;
    private final Config config;
    private final DataStore dataStore;
    private final BoundedExecutor executor;
        
    
    public MessageThread(Socket socket, Config config, DataStore dataStore, BoundedExecutor executor) {
        this.socket = socket;
        this.config = config;
        this.dataStore = dataStore;
        this.executor = executor;
    }


    private void doLoop(Socket socket) throws Exception {
        while(true) {
            Message message = MessageHelper.readMessage(socket.getInputStream());
                       
            if(message.getType() == MessageType.BootstrapRequest) {
                BootstrapRequest request = (BootstrapRequest) message;
                Set<Application> apps = dataStore.getBootstrapHosts(config.bootstrapCount);
                List<Application> appList = new ArrayList<>(apps);
                BootstrapReply reply = new BootstrapReply(appList);
                MessageHelper.send(socket.getOutputStream(), reply);   
                dataStore.updateApplication(request.application);
                continue;
            }
            
            if(message.getType() == MessageType.Gossip) {
                Gossip  request = (Gossip) message;
                for(Application app : request.applications) dataStore.updateApplication(app);
                Gossip reply = new Gossip(dataStore.getApplications());
                MessageHelper.send(socket.getOutputStream(), reply);   
                continue;
            }          
                
            if(message.getType() == MessageType.Rumor) {
                Rumor rumor = (Rumor) message;
                Application application = dataStore.getApplication(rumor.application.id);
                if(application == null) {
                    dataStore.updateApplication(rumor.application);
                    if(rumor.ttl<1) continue;
                    rumor.ttl--;
                    Set<Application> apps = dataStore.getBootstrapHosts(config.rumorFanOut);
                    for(Application app : apps) {
                        executor.submitTask(new SendMessage(app.address, rumor));
                    }                    
                }
                return;
            }
            
            
            if(message.getType() == MessageType.Heartbeat) {
                Heartbeat heartbeat = (Heartbeat) message;
                Application application = dataStore.getApplication(heartbeat.id);
                if(application == null) {
                    application = new Application(heartbeat.type, heartbeat.id, heartbeat.address, 1);
                    dataStore.updateApplication(application);
                    Rumor rumor = new Rumor(application, config.rumorTTL);
                    Set<Application> apps = dataStore.getBootstrapHosts(config.rumorFanOut);
                    for(Application app : apps) {
                        executor.submitTask(new SendMessage(app.address, rumor));
                    }                    
                } else {
                    dataStore.incrementHeartbeat(heartbeat.id);
                }
                return;
            } 
             
            throw new IOException("don't know how to handle message:"+message);
        }        
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(1000);
            doLoop(socket);
        } catch(Throwable t) {
            logger.error("doLoop()", t);
            closeQuietly(socket);
        }
    }
    
    private void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch(Throwable t) {
            logger.error("closing socket", t);
        }
    }

}
