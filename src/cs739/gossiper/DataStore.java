package cs739.gossiper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataStore {
    private static final Logger logger = LogManager.getLogger(DataStore.class);

    private final EventDispatcher eventDispatcher;
    private final DdbInserter ddbInserter;
    private final Random rng = new Random();

    private final Map<String, Info> apps = new HashMap<>();

    private class Info implements Handler {
        final String type;
        final String id;
        Address address;
        long heartbeat;

        Status status;
        long lastHeartbeatMillis;
        long eventId;

        public Info(Application app, Status status) {
            this.type = app.type;
            this.id = app.id;
            this.address = app.address;
            this.heartbeat = app.heartbeat;
            this.status = status;
            this.lastHeartbeatMillis = 0;
            this.eventId = -1;
        }

        @Override
        public void onEvent(long now) {
            // this gets called from the dispather
            // so grab the DataStore lock
            synchronized (DataStore.this) {
                logger.info("now:" + now + " lastHeartbeat:" + lastHeartbeatMillis + " id:" + id);
                status = Status.Incommunicado;
                eventId = -1;
                ddbInserter.Record(toMap("WentIncommunicado"));
            }
        }

        public Application toApp() {
            return new Application(type, id, address, heartbeat);
        }

        public Map<String, String> toMap(String event) {
            Map<String, String> result = new HashMap<>();
            result.put("type", type);
            result.put("id", id);
            result.put("ipAddr", address.ipAddress);
            result.put("port", String.valueOf(address.port));
            result.put("heartbeat", String.valueOf(heartbeat));
            result.put("status", status.toString());
            result.put("lastHeartbeatMillis", String.valueOf(lastHeartbeatMillis));
            result.put("event", event);
            return result;
        }
    }

    public DataStore(EventDispatcher eventDispatcher, DdbInserter ddbInserter) {
        this.eventDispatcher = eventDispatcher;
        this.ddbInserter = ddbInserter;
    }

    public synchronized void updateApplication(Application app) {

        Info info = apps.get(app.id);
        if (info == null) {
            info = new Info(app, Status.New);
            apps.put(app.id, info);
            ddbInserter.Record(info.toMap("NewApp"));
            return;
        }

        if (app.heartbeat <= info.heartbeat) {
            // this is old news
            logger.debug("IGNORED updateApplication -- app.heartbeat:" + app.heartbeat + " info.heartbeat:"
                    + info.heartbeat);
            return;
        }

        if (!app.address.equals(info.address)) {
            Address oldAddress = info.address;
            info.address = app.address;
            Map<String, String> map = info.toMap("AddressChanged");
            map.put("old_ipAddr", oldAddress.ipAddress);
            map.put("old_port", String.valueOf(oldAddress.port));
            ddbInserter.Record(map);
        }

        info.lastHeartbeatMillis = System.currentTimeMillis();
        if (info.eventId >= 0)
            eventDispatcher.cancel(info.eventId);
        info.eventId = eventDispatcher.register(Config.get().timeToIncommunicado, info);
        info.heartbeat = app.heartbeat;
        if (info.status != Status.Ok) {
            ddbInserter.Record(info.toMap("BecameOk"));
        }
        info.status = Status.Ok;
    }

    public synchronized List<Application> getApplications() {
        ArrayList<Application> result = new ArrayList<>();
        for (Info i : apps.values())
            result.add(i.toApp());
        return result;
    }

    public synchronized Application getApplication(String id) {
        Info info = apps.get(id);
        if (info == null)
            return null;
        return info.toApp();
    }

    public synchronized Set<Application> getBootstrapHosts(int bootstrapCount) {
        Set<Application> result = new HashSet<>();
        for (Info i : apps.values()) {
            if (!i.type.equals(Application.GossipingApp))
                continue;
            result.add(i.toApp());
        }
        return result;
    }

    public synchronized Address getRandomPeer() {
        while (true) {
            Collection<Info> values = apps.values();
            Optional<Info> info = values.stream().skip(rng.nextInt(values.size())).findFirst();
            if (info.get() == null)
                continue;
            if (!info.get().type.equals(Application.GossipingApp))
                continue;
            return info.get().address;
        }
    }

    public synchronized Set<Application> getRandomApplications(int n) {
        Supplier<Stream<Info>> apps = () -> this.apps.values().stream()
                .filter((info) -> info != null && info.type == Application.GossipingApp);

        Supplier<Stream<Info>> okApps = () -> apps.get().filter((info) -> info.status == Status.Ok);
        Supplier<Stream<Info>> notOkApps = () -> apps.get().filter((info) -> info.status != Status.Ok);

        long okCount = okApps.get().count();
        long notOkCount = notOkApps.get().count();

        Set<Application> applications = new HashSet<>(n);

        while (applications.size() < n && applications.size() < okCount) {
            int index = rng.nextInt(n);
            okApps.get().skip(index).findFirst().ifPresent((info) -> applications.add(info.toApp()));
        }

        while (applications.size() < n && applications.size() < okCount + notOkCount) {
            int index = rng.nextInt(n);
            notOkApps.get().skip(index).findFirst().ifPresent((info) -> applications.add(info.toApp()));
        }

        return applications;
    }

    public void incrementHeartbeat(String id) {
        Info info = apps.get(id);
        if (info == null) {
            logger.warn("app:" + id + " not found");
        } else {
            info.heartbeat++;
            info.lastHeartbeatMillis = System.currentTimeMillis();
            Status oldStatus = info.status;
            info.status = Status.Ok;

            if (info.eventId >= 0)
                eventDispatcher.cancel(info.eventId);
            info.eventId = eventDispatcher.register(Config.get().timeToIncommunicado, info);
            if (oldStatus != Status.Ok) {
                ddbInserter.Record(info.toMap("BecameOk"));
            }
        }
    }
}
