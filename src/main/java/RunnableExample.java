public class RunnableExample {
    public static void main(String[] args) {
        Thread one = new Thread(new ThreadOne());
        Thread two = new Thread(new ThreadTwo());
        Thread three = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("ThreadThree");
            }
        });
        Thread four = new Thread(() -> {
            System.out.println("ThreadFour");
        });

        one.start();
        two.start();
        three.start();
        four.start();
    }
}


class ThreadOne implements Runnable {
    @Override
    public void run() {
        System.out.println("ThreadOne");
    }
}

class ThreadTwo implements Runnable {
    @Override
    public void run() {
        System.out.println("ThreadTwo");
    }
}