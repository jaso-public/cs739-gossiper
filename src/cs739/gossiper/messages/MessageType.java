package cs739.gossiper.messages;

public enum MessageType {
    Gossip(10),
    Rumor(21),
    BootstrapRequest(31),
    BootstrapReply(32),
    Heartbeat(41),
    IpAddressRequest(101),
    IpAddressReply(101),
    Dump(1001);

    private int value;
    

    private MessageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
