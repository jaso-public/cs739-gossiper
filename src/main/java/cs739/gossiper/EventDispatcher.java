package cs739.gossiper;

import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EventDispatcher implements Runnable {
	
	private static final Logger logger = LogManager.getLogger(EventDispatcher.class);

	private HashMap<Long, Event> byId = new HashMap<>();	
	private SortedSet<Event> events = new TreeSet<>();	
	private BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
	private AtomicLong nextId = new AtomicLong();
	private boolean running = true;
	private Thread thread;
		

	public EventDispatcher() {
		thread = new Thread(this);
		thread.start();
	}

	static class Event implements Comparable <Event> {
		final long when;
		final Handler handler;
		final long id;				
	
		public Event(long when, Handler handler, long id) {
			super();
			this.when = when;
			this.handler = handler;
			this.id = id;
		}

		@Override
		public int compareTo(Event other) {
			if(this == other) return 0;
			if(this.when < other.when) return -1; 
			if(this.when > other.when) return +1; 
			if(this.id < other.id) return -1; 
			if(this.id > other.id) return +1; 
			throw new RuntimeException("WTF? duplicate request");
		}
	}			
	
	@Override
	public void run() {
		while(running) {
			try {
				doLoop();
			} catch(Throwable t) {
				logger.error("error during run", t);
			}
		}		
	}
	
	private void doLoop() throws Exception {
		while(running) {
			long now = System.currentTimeMillis();
			long waitTime = 1000;
			if(events.size() > 0) {
				waitTime = Math.max(0, Math.min(waitTime, events.first().when - now));
			}
			
			Event event = queue.poll(waitTime, TimeUnit.MILLISECONDS);
			if(event != null) {
				events.add(event);
				byId.put(event.id, event);
			}
			
			while(true) {
				now = System.currentTimeMillis();
				if(events.size() == 0 || events.first().when > now) break;
				event = events.first();
				events.remove(event);
				byId.remove(event.id);
				event.handler.onEvent(now);
			}
		}
	}
		
	public long register(long elapsed, Handler handler) {
		long futureTime = System.currentTimeMillis() + elapsed;
		Event event = new Event(futureTime, handler, nextId.incrementAndGet());
		queue.add(event);
		return event.id;
	}
	
	public void cancel(long eventId) {
		register(0, new Handler() { public void onEvent(long now) { 
			Event event = byId.remove(eventId);
			if(event != null) events.remove(event); }} );	
	}
	
	public void stop() {
		register(0, new Handler() { public void onEvent(long now) { running = false; }} );	
		try {
			thread.join();
		} catch (InterruptedException e) {
			logger.error("joining thread", e);
		}
	}	
}
