import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;

public class Functional implements Runnable {
    public void run() {
        int i = 0;
        while (true) {
            ServerSocket serverSocket = null;
            Socket ss = null;
            try {
                serverSocket = new ServerSocket(4040);
                ss = serverSocket.accept();
                ObjectInputStream ois = new ObjectInputStream(ss.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(ss.getOutputStream());
                int c = ois.readInt();
                String cas = ois.readUTF();
                switch (cas) {
                    case "latency":
                        int size = ois.readInt();
                        String connect = new String(ois.readNBytes(size));
                        Socket sss = new Socket(connect, 6060);
                        ObjectInputStream oiss = new ObjectInputStream(sss.getInputStream());
                        long x = oiss.readLong();
                        long nano = Calendar.getInstance().getTime().getTime() - x;
                        oos.writeLong(nano);
                        oos.flush();
                        oiss.close();
                        sss.close();
                        break;
                    case "request":
                        oos.writeInt("received".length());
                        oos.writeBytes("received");
                        oos.flush();
                        oos.close();
                        break;
                    default:
                        break;
                }
            } catch(Exception e) {
                e.printStackTrace();
            } 
            try {
                serverSocket.close();
                ss.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
