package cs739.gossiper;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.junit.Assert;

import cs739.gossiper.EventDispatcher;
import cs739.gossiper.Handler;

public class EventDispatcherTest {

	@Test(timeout = 1000)
	public void testStartStop() {
		EventDispatcher ed = new EventDispatcher();
		ed.stop();
	}
	
	class TestEvent implements Handler {
		CountDownLatch latch;
		long expectedTime;
		long eventTime = 0;
		
		public TestEvent(CountDownLatch latch, long expectedTime) {
			this.latch = latch;
			this.expectedTime = expectedTime;
		}

		@Override
		public void onEvent(long now) {
			eventTime = System.currentTimeMillis();
			latch.countDown();
		}		
	}
	
	
	@Test(timeout = 1000)
	public void testTwoEvents() throws InterruptedException {
		EventDispatcher ed = new EventDispatcher();
		long now = System.currentTimeMillis();
		
		CountDownLatch latch = new CountDownLatch(2);
		
		TestEvent ev1 = new TestEvent(latch, now + 100);
		ed.register(100, ev1);
		TestEvent ev2 = new TestEvent(latch, now + 50);
		ed.register(50, ev2);
		
		latch.await();
		
		Assert.assertTrue(ev1.eventTime - ev1.expectedTime < 10);
		Assert.assertTrue(ev2.eventTime - ev2.expectedTime < 10);
		ed.stop();
	}
	
	@Test //(timeout = 1000)
	public void testCancel() throws InterruptedException {
		EventDispatcher ed = new EventDispatcher();
		long now = System.currentTimeMillis();
		
		CountDownLatch latch = new CountDownLatch(2);
		
		long elapsedTime = 400;		
		TestEvent ev1 = new TestEvent(latch, now + elapsedTime);
		long eventId1 = ed.register(elapsedTime, ev1);
		Assert.assertTrue(eventId1 > 0);
		
		elapsedTime = 50;	
		TestEvent ev2 = new TestEvent(latch, now + elapsedTime);
		ed.register(elapsedTime, ev2);
		
		elapsedTime = 150;	
		TestEvent ev3 = new TestEvent(latch, now + elapsedTime);
		ed.register(elapsedTime, ev3);

		ed.cancel(eventId1);
		
		latch.await();
		
		Assert.assertEquals(0, ev1.eventTime);
		Assert.assertTrue(ev2.eventTime - ev2.expectedTime < 10);
		Assert.assertTrue(ev3.eventTime - ev3.expectedTime < 10);
		ed.stop();
	}

}
