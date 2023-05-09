package cs739.gossiper.messages;

public class IpAddressRequest implements Message {

    @Override
    public MessageType getType() {
        return MessageType.IpAddressRequest;
    }

    @Override
    public String toString() {
        return "IpAddressRequest []";
    }
}
