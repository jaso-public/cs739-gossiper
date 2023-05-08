package cs739.gossiper;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BoundedExecutor {
    private ExecutorService executor;

    public BoundedExecutor(int poolSize) {
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(poolSize);
        executor = new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, queue);
    }

    public void submitTask(Runnable task) throws InterruptedException {
        executor.submit(task);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
