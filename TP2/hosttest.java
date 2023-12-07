import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;


public class hosttest {

    private static int defaultPort = 25000;

    private static ArrayList<String> n = new ArrayList<>();
    private static ArrayList<String> serverPath = new ArrayList<>();
    public static void main(String[] args) throws IOException {
        System.out.println("Waiting for connection...");
        new Thread(new Latency()).start();
        new Thread(() -> {
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
                    switch (c) {
                        case 1:
                            int size = ois.readInt();
                            String connect = new String(ois.readNBytes(size));
                            Socket sss = new Socket(connect, 6060);
                            ObjectInputStream oiss = new ObjectInputStream(sss.getInputStream());
                            long x = oiss.readLong();
                            long nano = Calendar.getInstance().getTime().getTime() - x;
                            oos.writeLong(nano);
                            oos.flush();
                            oos.close();
                            ois.close();
                            serverSocket.close();
                            ss.close();
                            oiss.close();
                            sss.close();
                            break;
                        case 2:
                            int numberOfIPs = ois.readInt();
                            for (int j = 0; j < numberOfIPs; j++) {
                                size = ois.readInt();
                                String ip = new String(ois.readNBytes(size));
                                n.add(ip);
                                System.out.println("Connection established with IP: " + ip);
                            }
                            ois.close();

                        case 3:
                            size = ois.readInt();
                            String ip = new String(ois.readNBytes(size));
                            n.add(ip);
                            System.out.println("Connection established with IP: " + ip);

                            ois.close();

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
        }).start(); 

        DatagramSocket rSocket = new DatagramSocket(defaultPort);

        while (true) {
            DatagramPacket p = new DatagramPacket(new byte[15000], 15000);
            rSocket.receive(p);
            for (String s : n) {
                byte[] bytes = p.getData();
                switch (bytes[0]) {
                    case 0:
                        break;
                    default:
                        th t = new th(bytes, defaultPort, s);
                        new Thread(t).start();
                        break;
                }
            }
        }

    }
}

class th implements Runnable {

    private byte[] buf;
    private int port;
    private String ip;

    public th(byte[] buf, int port, String ip) {
        this.buf = buf;
        this.port = port;
        this.ip = ip;
    }

    @Override
    public void run() {
        try {
            DatagramPacket p = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), port);
            System.out.println(buf.length);
            DatagramSocket sSocket = new DatagramSocket();
            sSocket.send(p);
            System.out.println("Enviado");

            sSocket.close();
    
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
