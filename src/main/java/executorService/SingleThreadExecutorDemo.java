package executorService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SingleThreadExecutorDemo {
    public static void main(String[] args) {
        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            for (int i = 0; i < 7; i++)
                executorService.execute(new Task(i));
        }
    }
}

class Task implements Runnable {

    public final Integer taskId;

    public Task(Integer id) {
        taskId = id;
    }

    @Override
    public void run() {
        System.out.println("Running Task - " + taskId + " - by thread - " + Thread.currentThread().getName());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
