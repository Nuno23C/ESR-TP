import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class rp {

    private static ReentrantLock rl = new ReentrantLock();
    private static int defaultPort = 25000;
    private static HashMap<String,LinkedList<String>> n = null;
    private static HashMap<String,HashMap<String,String>> topologia = new HashMap<>();
    private static HashMap<String,HashMap<String,Long>> e = new HashMap<>();

    private static boolean isRunning = true;
    public static void main(String[] args) throws IOException {

        topologia = Leitor.readFile("test.yml");
        
        new Thread(new Functional()).start();
        new Thread(new Latency()).start();

        new Thread(() -> {
            while (true) {
                rl.lock();
                for (String host : topologia.keySet()) {
                    for (String no : topologia.get(host).keySet()) {
                        System.out.println(host + " -> " + no);
                        long nano = 1;
                        if (topologia.get(no).get(host) != null) {
                            try {
                                // Ip de entrada
                                String pontoF = topologia.get(no).get(host);
                                // Ip que queremos saber o tempo
                                String pontoI = topologia.get(host).get(no);
                                // Tempo de ida
                                System.out.println("E agora?");
                                System.out.println(pontoI);
                                Socket rSocket = new Socket(pontoI,4040);
                                rSocket.setSoTimeout(1);
                                System.out.println("Nao?");
                                OutputStream out = rSocket.getOutputStream();
                                ObjectOutputStream oos = new ObjectOutputStream(out);
                                oos.writeInt(1);
                                oos.writeInt(pontoF.length());
                                oos.writeBytes(pontoF);
                                oos.flush();
                                ObjectInputStream ois = new ObjectInputStream(rSocket.getInputStream());
                                nano = ois.readLong();
                                ois.close();
                                rSocket.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                                nano = Integer.MAX_VALUE;
                            }
                            if (e.get(host) == null)
                                e.put(host, new HashMap<>());
                            e.get(host).put(no, nano);
                        }
                        
                    }
                }
                System.out.println("Anda la");
                n = Djikstra.route(topologia,e);
                rl.unlock();
                try {
                    Thread.sleep(300000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();
        while (n == null) {
            rl.lock();
            rl.unlock();
        }
        new Thread(new Maintenance(n)).start();

        DatagramSocket rSocket = new DatagramSocket(defaultPort);

        while (true) {
            DatagramPacket p = new DatagramPacket(new byte[15000], 15000);
            rSocket.receive(p);

            new Thread(new Nexus(p.getData(), defaultPort, "10.0.2.2")).start();            
            new Thread(new Nexus(p.getData(), defaultPort, "10.0.1.2")).start();            
            
        }
    }
}

class Maintenance implements Runnable {
    private HashMap<String,LinkedList<String>> n = new HashMap<>();

    public Maintenance(HashMap<String,LinkedList<String>> e) {
        this.n = e;
    }

    @Override
    public void run() {
        HashMap<String,Boolean> ative = new HashMap();
        while (true) {
            System.out.println(n);
            for (String key : n.keySet()) {
                Boolean flag = true;
                int j = 0;
                while (flag && j++ < 3) {
                    try {
                        System.out.println(key);
                        System.out.println(key.length());
                        flag = false;
                            Socket s = new Socket(key,4040);
                            int k = n.get(key).size();
                            ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                            oos.writeInt(2);
                            oos.writeInt(k);
                            for (int i = 0; i < k; i++) {
                                oos.writeInt(n.get(key).get(i).length());
                                oos.writeBytes(n.get(key).get(i));
                            }
                            oos.flush();
                            oos.close();
                            s.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Error with Socket, Retrying in 10 seconds");
                        flag = true;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                    ative.put(key, !flag);
                }
            }
            byte[] buf = new byte[1024];
            buf[0] = (byte) 1;
            int k = 1;
            for (String key : ative.keySet()) {
                for (int i = 0; i < key.length(); i++) {
                    buf[k++] = (byte) key.charAt(i);
                }
                if (ative.get(key))
                    buf[k] = (byte) 1;
                else buf[k] = (byte) 0;
                k++;
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class Nexus implements Runnable {

    private byte[] buf;
    private int port;
    private String ip;

    public Nexus(byte[] buf, int port, String ip) {
        this.buf = buf;
        this.port = port;
        this.ip = ip;
    }

    @Override
    public void run() {
        try {
            DatagramPacket p = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), port);
            DatagramSocket sSocket = new DatagramSocket();
            sSocket.send(p);

            sSocket.close();
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
