package cs739.gossiper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cs739.gossiper.messages.BootstrapReply;
import cs739.gossiper.messages.BootstrapRequest;
import cs739.gossiper.messages.GetConfigReply;
import cs739.gossiper.messages.Gossip;
import cs739.gossiper.messages.Heartbeat;
import cs739.gossiper.messages.IpAddressReply;
import cs739.gossiper.messages.IpAddressRequest;
import cs739.gossiper.messages.Message;
import cs739.gossiper.messages.MessageType;
import cs739.gossiper.messages.Rumor;
import cs739.gossiper.messages.UpdateConfigRequest;

public class MessageThread implements Runnable {

    private static final Logger logger = LogManager.getLogger(MessageThread.class);

    private final Socket socket;
    private final DataStore dataStore;
    private final BoundedExecutor executor;
    private final DdbInserter ddbInserter;

    public MessageThread(Socket socket, DataStore dataStore, BoundedExecutor executor,
            DdbInserter ddbInserter) {
        this.socket = socket;
        this.dataStore = dataStore;
        this.executor = executor;
        this.ddbInserter = ddbInserter;
    }

    private void handleMessage(Socket socket) throws Exception {
        Message message = MessageHelper.readMessage(socket.getInputStream());
        if (message == null) {
            logger.info("socket closed:" + socket);
            return;
        }

        if (message.getType() == MessageType.BootstrapRequest) {
            BootstrapRequest request = (BootstrapRequest) message;
            Set<Application> apps = dataStore.getBootstrapHosts(Config.get().bootstrapCount);
            List<Application> appList = new ArrayList<>(apps);
            BootstrapReply reply = new BootstrapReply(appList);
            MessageHelper.send(socket.getOutputStream(), reply);
            dataStore.updateApplication(request.application);
            return;
        }

        if (message.getType() == MessageType.Gossip) {
            Gossip request = (Gossip) message;
            for (Application app : request.applications)
                dataStore.updateApplication(app);

            List<Application> apps;
            if (Config.get().doPullGossip || request.force) {
                apps = dataStore.getApplications();
            } else {
                apps = new ArrayList<>();
            }

            Gossip reply = new Gossip(apps);

            MessageHelper.send(socket.getOutputStream(), reply);
            return;
        }

        if (message.getType() == MessageType.Rumor) {
            Rumor rumor = (Rumor) message;
            Application application = dataStore.getApplication(rumor.application.id);
            if (application == null) {
                dataStore.updateApplication(rumor.application);
                if (rumor.ttl < 1)
                    return;
                rumor.ttl--;
                Set<Application> apps = dataStore.getRandomApplications(Config.get().rumorFanOut);
                for (Application app : apps) {
                    executor.submitTask(new SendMessage(app.address, rumor));
                }
            }
            return;
        }

        if (message.getType() == MessageType.Heartbeat) {
            Heartbeat heartbeat = (Heartbeat) message;
            Application application = dataStore.getApplication(heartbeat.id);
            if (application == null) {
                application = new Application(heartbeat.type, heartbeat.id, heartbeat.address, 1);
                dataStore.updateApplication(application);
                Rumor rumor = new Rumor(application, Config.get().rumorTTL);
                Set<Application> apps = dataStore.getBootstrapHosts(Config.get().rumorFanOut);
                for (Application app : apps) {
                    executor.submitTask(new SendMessage(app.address, rumor));
                }
            } else {
                dataStore.incrementHeartbeat(heartbeat.id);
            }
            return;
        }

        if (message.getType() == MessageType.IpAddressRequest) {
            @SuppressWarnings("unused")
            IpAddressRequest request = (IpAddressRequest) message;
            InetAddress remoteAddress = socket.getInetAddress();
            Address address = new Address(remoteAddress.getHostAddress(), socket.getPort());
            IpAddressReply reply = new IpAddressReply(address);
            logger.info("got request for IpAddress -- reply:" + reply);
            MessageHelper.send(socket.getOutputStream(), reply);
            return;
        }

        if (message.getType() == MessageType.Terminate) {
            HashMap<String, String> map = new HashMap<>();
            map.put("event", "terminate");
            ddbInserter.Record(map);
            ddbInserter.drain();
            System.exit(0);
        }

        if (message.getType() == MessageType.UpdateConfig) {
            Config updatedConfig = ((UpdateConfigRequest) message).config;
            if (updatedConfig == null) {
                logger.warn("UpdateConfigRequest -- Updated config is null");
                return;
            }

            logger.info("UpdateConfigRequest -- Updating config");

            Config.set(updatedConfig);
            return;
        }

        if (message.getType() == MessageType.GetConfigRequest) {
            GetConfigReply reply = new GetConfigReply(Config.get());
            MessageHelper.send(socket.getOutputStream(), reply);
            return;
        }

        throw new IOException("don't know how to handle message:" + message);
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(1000);
            handleMessage(socket);
        } catch (Throwable t) {
            logger.error("handleMessage()", t);
        }

        try {
            socket.close();
        } catch (Throwable t) {
            logger.error("closing socket", t);
        }
    }
}
