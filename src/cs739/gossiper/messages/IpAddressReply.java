package cs739.gossiper.messages;

import cs739.gossiper.Address;

public class IpAddressReply implements Message {
    
    public Address ipAddress;

    public IpAddressReply() {
    }

    public IpAddressReply(Address ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public MessageType getType() {
        return MessageType.IpAddressReply;
    }

    @Override
    public String toString() {
        return "IpAddressReply [ipAddress=" + ipAddress + "]";
    }
}
