package cs739.gossiper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import cs739.gossiper.messages.BootstrapReply;
import cs739.gossiper.messages.BootstrapRequest;
import cs739.gossiper.messages.IpAddressReply;
import cs739.gossiper.messages.IpAddressRequest;

public class Gossiper implements Handler {
    private static final Logger logger = LogManager.getLogger(Gossiper.class);

    public final String myIpAddress;
    public final String myApplicationId;

    public final DdbInserter ddbInserter;
    public final EventDispatcher eventDispatcher;
    public final DataStore dataStore;
    public final BoundedExecutor executor;

    public Gossiper(String myApplicationId, String myIpAddress) {
        this.myIpAddress = myIpAddress;
        this.myApplicationId = myApplicationId;

        this.ddbInserter = new DdbInserter(myApplicationId);
        this.eventDispatcher = new EventDispatcher();
        this.dataStore = new DataStore(eventDispatcher, ddbInserter);
        this.executor = new BoundedExecutor(Config.get().executorPoolSize);

        HashMap<String, String> map = new HashMap<>();
        map.put("event", "start");
        ddbInserter.Record(map);

        Address address = new Address(myIpAddress, Config.get().listenPort);
        Application application = new Application(Application.GossipingApp, myApplicationId, address, 1);
        dataStore.updateApplication(application);

        BootstrapRequest request = new BootstrapRequest(application);

        try (Socket socket = new Socket(Config.get().bootstrapHost, Config.get().bootstrapPort)) {
            logger.info("socket created -- sending message");
            MessageHelper.send(socket.getOutputStream(), request);
            BootstrapReply reply = (BootstrapReply) MessageHelper.readMessage(socket.getInputStream());
            System.out.println(reply.toString());
            socket.close();
            for (Application app : reply.applications)
                dataStore.updateApplication(app);
        } catch (Throwable t) {
            logger.error("failed to get bootstrap list from:" + Config.get().bootstrapHost, t);
        }

        eventDispatcher.register(Config.get().heartbeatInterval, this);
    }

    void loop() throws IOException, InterruptedException {

        try (ServerSocket ss = new ServerSocket(Config.get().listenPort, Config.get().backlog)) {
            while (true) {
                Socket s = ss.accept();
                logger.info("accepted socket:" + s);
                executor.submitTask(new MessageThread(s, dataStore, executor, ddbInserter));
            }
        }
    }

    @Override
    public void onEvent(long now) {
        logger.info("doing a heartbeat for application:" + myApplicationId + " now:" + now);
        dataStore.incrementHeartbeat(myApplicationId);
        eventDispatcher.register(Config.get().heartbeatInterval, this);

        try {
            executor.submitTask(new SendGossip(dataStore));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private static String getApplicationId() throws Exception {
        File file = new File(Config.get().pathToApplicationId);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String result = br.readLine();
                logger.info("my existing applicationId:" + result);
                return result;
            }
        }

        String applicationId = "gossiper-" + Math.abs(new Random().nextLong());
        try (PrintWriter pw = new PrintWriter(file)) {
            pw.println(applicationId);
        }

        logger.info("my newly created applicationId:" + applicationId);
        return applicationId;
    }

    private static String getMyIpAddress(boolean isBootstrapper) throws Exception {
        String myIpAddress = null;

        if (isBootstrapper) {
            InetAddress[] addresses = InetAddress.getAllByName(Config.get().bootstrapHost);
            for (InetAddress address : addresses) {
                System.out.println(address.getHostAddress());
                myIpAddress = address.getHostAddress();
            }
        } else {
            IpAddressRequest request = new IpAddressRequest();

            try (Socket socket = new Socket(Config.get().bootstrapHost, Config.get().bootstrapPort)) {
                logger.info("socket created -- sending message:" + request);
                MessageHelper.send(socket.getOutputStream(), request);
                IpAddressReply reply = (IpAddressReply) MessageHelper.readMessage(socket.getInputStream());
                System.out.println(reply.toString());
                myIpAddress = reply.ipAddress.ipAddress;
                socket.close();
            }
        }

        logger.info("myIpAddress is:" + myIpAddress);
        return myIpAddress;

    }

    public static void main(String[] args) throws Exception {
        Configurator.initialize(null, "log4j2.xml");
        Configurator.setRootLevel(Level.INFO);

        boolean isBootstrapper = args.length > 0 && "bootstrapper".equals(args[0]);
        String myIpAddress = getMyIpAddress(isBootstrapper);

        Gossiper gossiper = new Gossiper(myIpAddress, myIpAddress);
        try {
            gossiper.loop();
        } catch (Exception e) {
            logger.error("loop threw", e);
        }
    }

}
