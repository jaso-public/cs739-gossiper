package cs739.gossiper.messages;

import java.util.List;

import cs739.gossiper.Application;

public class BootstrapReply implements Message {
    public List<Application> applications;

    public BootstrapReply() {
    }

    public BootstrapReply(List<Application> applications) {
        this.applications = applications;
    }

    @Override
    public MessageType getType() {
        return MessageType.BootstrapReply;
    }
}
