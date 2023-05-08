package cs739.gossiper;

public class Application {
    
    public static final String GossipingApp = "gossiper"; 
    
    String type;
    String id;
    Address address;
    long heartbeat;
    Status s = Status.Ok;

    public Application(String type, String id, Address address, long heartbeat) {
        this.type = type;
        this.id = id;
        this.address = address;
        this.heartbeat = heartbeat;
    }

}
