package cs739.gossiper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataStore {
    private static final Logger logger = LogManager.getLogger(DataStore.class);
	
    private final Config config;
    private final EventDispatcher eventDispatcher;
    private final DdbInserter ddbInserter;
    
    private final Map<String,Info> apps = new HashMap<>();
    private final Random rng = new Random();

    private class Info implements Handler {
        final String type;
        final String id;
        Address address;        
        long heartbeat;

        Status status;
        long lastHeartbeatMillis;
        long eventId;
                
        
        public Info(Application app, Status status) {
            this.type = app.type;
            this.id = app.id;
            this.address = app.address;
            this.heartbeat = app.heartbeat;
            this.status = status;
            this.lastHeartbeatMillis = 0;
            this.eventId = -1;
        }

        @Override
        public void onEvent(long now) {
            // this gets called from the dispather
            // so grab the DataStore lock
            synchronized (DataStore.this) {
                status = Status.Incommunicado;
                eventId = -1;
                ddbInserter.Record(toMap("WentIncommunicado"));
            }
        }
        
        public Application toApp() {
            return new Application(type, id, address, heartbeat);
        }
        
        public Map<String,String> toMap(String event) {
            Map<String,String> result = new HashMap<>();
            result.put("type", id);
            result.put("id", id);
            result.put("ipAddr", address.ipAddress);
            result.put("port", String.valueOf(address.port));
            result.put("heartbeat", String.valueOf(heartbeat));
            result.put("status", status.toString());
            result.put("lastHeartbeatMillis",String.valueOf(lastHeartbeatMillis));
            result.put("event", event);
            return result;       
        }
    }
 
	
    public DataStore(Config config, EventDispatcher eventDispatcher, DdbInserter ddbInserter) {
        this.config = config;
        this.eventDispatcher = eventDispatcher;
        this.ddbInserter = ddbInserter;
    }
  
	
    public synchronized void updateApplication(Application app) {
	    
	    Info info = apps.get(app.id);
	    if(info == null) {
	        info = new Info(app, Status.New);
	        apps.put(app.id, info);
	        ddbInserter.Record(info.toMap("NewApp"));
	        return;
	    }
	    
	    if(app.heartbeat <= info.heartbeat) {
	        // this is old news
	        return;
	    }
	    
	    if(! app.address.equals(info.address)) {
	        Address oldAddress = info.address;
	        info.address = app.address;
	        Map<String,String> map = info.toMap("AddressChanged");
	        map.put("old_ipAddr", oldAddress.ipAddress);
	        map.put("old_port", String.valueOf(oldAddress.port));	        
            ddbInserter.Record(map);    
 	    }
	    
	    info.lastHeartbeatMillis = System.currentTimeMillis();
	    if(info.eventId>=0) eventDispatcher.cancel(info.eventId);
	    eventDispatcher.register(config.timeToIncommunicado, info);
	    if(info.status != Status.Ok) {
            ddbInserter.Record(info.toMap("BecameOk"));	        
	    }
	}
	
    public synchronized int getCount(String type) {
        int count = 0;
        for(Info i : apps.values()) {
            if(i.type.equals(type)) count++;
        }
	    return count;
	}
	
    
    public synchronized List<Application> getApplications() {
        ArrayList<Application> result = new ArrayList<>();
        for(Info i : apps.values()) result.add(i.toApp());
        return result;
    }
    

    public synchronized Application getApplication(String id) {
        return apps.get(id).toApp();
    }


    public synchronized Set<Application> getBootstrapHosts(int bootstrapCount) {
        int countNotOk = 0;
        int countOk = 0;
        for(Info i : apps.values()) {
            if(i.type.equals(Application.GossipingApp)) {
                if(i.status == Status.Ok) {
                    countOk++;
                } else {
                    countNotOk++;
                }
            }
        }
         
        
        Set<Application> result = new HashSet<>();
        while(result.size() < countOk) {
            result.add(randomOkApp(Application.GossipingApp));
            if(result.size() >= bootstrapCount) return result;
        }
        while(result.size() < countNotOk) {
            result.add(randomNotOkApp(Application.GossipingApp));
            if(result.size() >= bootstrapCount) return result;
        }
        return result;
    }
    
    private Application randomOkApp(String type) {
        int count = getCount(type);
        if(count==0) return null;
        int select = rng.nextInt(count);
        for(Info i : apps.values()) {
            if(i.type.equals(type)) {
                if(i.status != Status.Ok) continue;
                if(select == 0) return i.toApp();
                select--;
            }
        }
        throw new RuntimeException("WTF? ran out of apps");
    }

    private Application randomNotOkApp(String type) {
        int count = getCount(type);
        if(count==0) return null;
        int select = rng.nextInt(count);
        for(Info i : apps.values()) {
            if(i.type.equals(type)) {
                if(i.status == Status.Ok) continue;
                if(select == 0) return i.toApp();
                select--;
            }
        }
        throw new RuntimeException("WTF? ran out of apps");
    }


    public void incrementHeartbeat(String id) {
        Info info = apps.get(id);
        if(info==null ) {
            logger.warn("app:"+id+" not found");
        } else {
            info.heartbeat++;
        }       
    }


}
