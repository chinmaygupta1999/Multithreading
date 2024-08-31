import java.util.concurrent.*;

public class CallableDemo {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try(ExecutorService executorService = Executors.newFixedThreadPool(3)) {
            Future<String> returned = executorService.submit(new ReturnValueTask());
            System.out.println(returned.get(500, TimeUnit.MILLISECONDS));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.println(e);
        }
    }
}

class ReturnValueTask implements Callable<String> {
    @Override
    public String call() throws Exception {
        Thread.sleep(1000);
        return "Returned from ReturnValueTask";
    }
}
