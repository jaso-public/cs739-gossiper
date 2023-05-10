package cs739.gossiper.messages;

public enum MessageType {
    Gossip(10),
    Rumor(21),
    BootstrapRequest(31),
    BootstrapReply(32),
    Heartbeat(41),
    IpAddressRequest(101),
    IpAddressReply(102),
    Dump(1001),
    Terminate(1002),
    UpdateConfig(1003),
    GetConfigRequest(1004),
    GetConfigReply(1005);

    private int value;

    private MessageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
