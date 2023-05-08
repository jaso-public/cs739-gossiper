package cs739.gossiper.messages;

import cs739.gossiper.Address;

public class Heartbeat implements Message {

    public String type;
    public String id;
    public Address address;
         
    
    public Heartbeat() {
    }

    public Heartbeat(String type, String id, Address address) {
        this.type = type;
        this.id = id;
        this.address = address;
    }

    @Override
    public MessageType getType() {
        return MessageType.Heartbeat;
    }

}
