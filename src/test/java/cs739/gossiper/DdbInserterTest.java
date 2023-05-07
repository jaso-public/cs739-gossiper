package cs739.gossiper;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import cs739.gossiper.DdbInserter;

public class DdbInserterTest {

	@Test(timeout = 1000)
	public void testStartStop() {
		DdbInserter di = new DdbInserter("DdbInserterTest");
		di.drain();
	}

	
	@Test
	public void test() {
		DdbInserter di = new DdbInserter("DdbInserterTest");
		Map<String,String> map = new HashMap<String,String>();
		map.put("jaso", "was here");
		di.Record(map);
		di.drain();
	}

}
