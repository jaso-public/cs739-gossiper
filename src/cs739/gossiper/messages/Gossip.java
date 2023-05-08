package cs739.gossiper.messages;

import java.util.List;

import cs739.gossiper.Application;

public class Gossip implements Message {
    final MessageType type = MessageType.Gossip;
    
    final List<Application> applications;
    
    public Gossip(List<Application> applications) {
        this.applications = applications;
    }

    @Override
    public MessageType getType() {
        return type;
    }

    public List<Application> getApplications() {
        return applications;
    }
}
