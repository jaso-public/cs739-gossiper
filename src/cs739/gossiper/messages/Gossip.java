package cs739.gossiper.messages;

import java.util.List;

import cs739.gossiper.Application;

public class Gossip implements Message {

    public List<Application> applications;
    

    public Gossip() {
    }

    public Gossip(List<Application> applications) {
        this.applications = applications;
    }

    @Override
    public MessageType getType() {
        return MessageType.Gossip;
    }
}
