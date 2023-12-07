import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;

public class Latency implements Runnable {
    @Override
    public void run() {
        try {
            while (true) {
                ServerSocket serverSocket = new ServerSocket(6060);
                Socket s = serverSocket.accept();
                OutputStream d = s.getOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(d);
                long localDateTime = Calendar.getInstance().getTime().getTime();
                oos.writeLong(localDateTime);
                oos.flush();
                serverSocket.close();
                d.close();
                s.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
