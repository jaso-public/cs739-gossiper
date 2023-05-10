package cs739.gossiper.messages;

import java.util.List;

import cs739.gossiper.Application;

public class Gossip implements Message {

    public List<Application> applications;
    public boolean force;

    public Gossip() {
    }

    public Gossip(List<Application> applications) {
        this.applications = applications;
    }

    public Gossip(List<Application> applications, boolean force) {
        this.applications = applications;
        this.force = force;
    }

    @Override
    public MessageType getType() {
        return MessageType.Gossip;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GossipMessage\n");
        for (Application app : applications) {
            sb.append(app + "\n");
        }

        return sb.toString();
    }

}
