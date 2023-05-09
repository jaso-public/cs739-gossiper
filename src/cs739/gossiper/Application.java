package cs739.gossiper;

public class Application {
    
    public static final String GossipingApp = "gossiper"; 
    
    public String type;
    public String id;
    public Address address;
    public long heartbeat;
    public Status s = Status.Ok;

    
    public Application() {
    }


    public Application(String type, String id, Address address, long heartbeat) {
        this.type = type;
        this.id = id;
        this.address = address;
        this.heartbeat = heartbeat;
    }


    @Override
    public String toString() {
        return "Application [type=" + type + ", id=" + id + ", address=" + address + ", heartbeat=" + heartbeat + ", s="
                + s + "]";
    }
 }
