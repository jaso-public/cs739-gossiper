package cs739.gossiper;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jsoniter.JsonIterator;

import cs739.gossiper.messages.BootstrapReply;
import cs739.gossiper.messages.BootstrapRequest;
import cs739.gossiper.messages.Gossip;
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

    public long computeCrc(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();      
    }
 
    public byte[] read(InputStream is, int size) throws IOException {
        byte[] result = new byte[size];
        int offset = 0;
        int remaining = size;
        while(remaining > 0) {
            int count = is.read(result, offset, remaining);
            if(count<0) {
                if(offset == 0) return null;
                throw new IOException("premute end of stream");
            }
            offset += count;
            remaining -= offset;
        }
        return result;
    }


    private void doLoop(Socket socket) throws Exception {
        while(true) {
            byte[] header = read(socket.getInputStream(), 16);
            ByteBuffer buffer = ByteBuffer.wrap(header);
            int messageType = buffer.getInt();
            int messageLength = buffer.getInt();
            long messageCrc = buffer.getLong();
            
            byte[] body = read(socket.getInputStream(), messageLength);
            if(body==null) throw new IOException("message doesn't have a body");
            if(messageCrc != computeCrc(body)) throw new IOException("message crc mismatch");
           
            if(messageType == MessageType.BootstrapRequest.getValue()) {
                BootstrapRequest request = JsonIterator.deserialize(body, BootstrapRequest.class);
                Set<Application> apps = dataStore.getBootstrapHosts(config.bootstrapCount);
                List<Application> appList = new ArrayList<>(apps);
                BootstrapReply reply = new BootstrapReply(appList);
                SendMessage.send(socket.getOutputStream(), reply);   
                dataStore.updateApplication(request.application);
            } else if(messageType == MessageType.Gossip.getValue()) {
                Gossip  request = JsonIterator.deserialize(body, Gossip.class);
                for(Application app : request.getApplications()) dataStore.updateApplication(app);
                Gossip reply = new Gossip(dataStore.getApplications());
                SendMessage.send(socket.getOutputStream(), reply);   
            } else if(messageType == MessageType.Rumor.getValue()) {
                Rumor rumor = JsonIterator.deserialize(body, Rumor.class);
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
           } else {
                throw new IOException("don't know how to handle messageType:"+messageType);
            }
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
