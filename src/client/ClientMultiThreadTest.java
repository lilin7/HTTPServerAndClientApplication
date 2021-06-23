package client;

public class ClientMultiThreadTest {
    public static void main(String[] args) {
        String client1RequestLine = "GET /test.txt";
        String client2RequestLine = "POST /bar.txt Content-Body: test";

        Thread client1 = new ClientMultiThread(client1RequestLine,8005,"127.0.0.2");
        Thread client2 = new ClientMultiThread(client2RequestLine,8006,"127.0.0.3");
        client1.start();
        client2.start();
    }


}
