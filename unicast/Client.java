import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class Client {
    // GUI
    JFrame f = new JFrame("Cliente de Testes");
    JButton setupButton = new JButton("Setup");
    JButton playButton = new JButton("Play");
    JButton pauseButton = new JButton("Pause");
    JButton tearButton = new JButton("Teardown");
    JPanel mainPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JLabel iconLabel = new JLabel();
    ImageIcon icon;

    // RTSP variables:
    Socket RTSPsocket; // Socket used to send/receive messages
    static BufferedReader RTSPBufferedReader; // Input stream filters from the socket
    static BufferedWriter RTSPBufferedWriter; // Output stream filters to the socket
    static String state; // RTSP Server state

    // RTP variables:
    DatagramPacket rcvdp; // UDP packet received from the server (to receive)
    DatagramSocket RTPsocket; // socket to be used to send/receive UDP packet
    static int RTP_RCV_PORT = 25000; // port where the client will receive the RTP packets

    Timer timer; // timer used to receive data from the UDP socket
    byte[] buffer; // buffer used to store data received from the server

    // --------------------------
    // Client constructor
    // --------------------------
    public Client(String serverIP) {
        // build GUI

        // Frame
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        // Buttons
        buttonPanel.setLayout(new GridLayout(1, 0));
        buttonPanel.add(setupButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(tearButton);

        setupButton.addActionListener(new setupButtonListener());
        playButton.addActionListener(new playButtonListener());
        pauseButton.addActionListener(new pauseButtonListener());
        tearButton.addActionListener(new tearButtonListener());

        // Image display label
        iconLabel.setIcon(null);

        // frame layout
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        iconLabel.setBounds(0, 0, 380, 280);
        buttonPanel.setBounds(0, 280, 380, 50);

        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.setSize(new Dimension(390, 370));
        f.setVisible(true);

        // init timer
        timer = new Timer(20, new timerListener());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        // allocate enough memory for the buffer used to receive data from the server
        buffer = new byte[15000];

        try {
            System.out.println("Client: Will create the socket");

            // Initiate TCP connection with the server for the RTSP session
            Socket RTSPsocket = new Socket(serverIP, RTP_RCV_PORT);

            System.out.println("Client: socket created");

        } catch (UnknownHostException e) {
            System.out.println("Client: erro no socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Client: erro no socket: " + e.getMessage());
        }
    }

    // ------------------------------------
    // main
    // ------------------------------------
    public static void main(String argv[]) throws Exception {
        Client client = new Client(argv[0]);

        // Set input and output stream filters:
        RTSPBufferedReader = new BufferedReader(new InputStreamReader(client.RTSPsocket.getInputStream()));
        RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(client.RTSPsocket.getOutputStream()));

        // init RTSP state:
        state = "INIT";
    }

    // ------------------------------------
    // Handler for Setup button
    // ------------------------------------
    class setupButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            System.out.println("Setup Button pressed !");

            if (state == "INIT") {
                // Init non-blocking RTPsocket that will be used to receive data
                try {
                    // construct a new DatagramSocket to receive RTP packets from the server
                    RTPsocket = new DatagramSocket(RTP_RCV_PORT);

                    // set TimeOut value of the socket to 5msec.
                    RTPsocket.setSoTimeout(5);

                } catch (SocketException se) {
                    System.out.println("Socket exception: " + se);
                    System.exit(0);
                }

                // Send SETUP message to the server
                send_RTSP_request("SETUP");

                // Wait for the response
                if (parse_server_response() == 200) {
                    state = "READY";
                } else {
                    System.out.println("Invalid Server Response");
                }
            }
        }
    }

    // ------------------------------------
    // Handler for Play button
    // ------------------------------------
    class playButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            System.out.println("Play Button pressed !");

            if (state == "READY") {
                // Send PLAY message to the server
                send_RTSP_request("PLAY");

                // Wait for the response
                if (parse_server_response() == 200) {
                    state = "PLAYING";

                    // start the timer
                    timer.start();
                } else {
                    System.out.println("Invalid Server Response");
                }
            }
        }
    }

    // ------------------------------------
    // Handler for Pause button
    // ------------------------------------
    class pauseButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            System.out.println("Pause Button pressed !");

            if (state == "PLAYING") {
                // Send PAUSE message to the server
                send_RTSP_request("PAUSE");

                // Wait for the response
                if (parse_server_response() == 200) {
                    state = "PAUSE";

                    // stop the timer
                    timer.stop();
                } else {
                    System.out.println("Invalid Server Response");
                }
            }
        }
    }

    // ------------------------------------
    // Handler for tear button
    // ------------------------------------
    class tearButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            System.out.println("Teardown Button pressed !");

            // Send TEARDOWN message to the server
            send_RTSP_request("TEARDOWN");

            // Wait for the response
            if (parse_server_response() == 200) {
                // stop the timer
                timer.stop();

                // exit
                System.exit(0);
            } else {
                System.out.println("Invalid Server Response");
            }
        }
    }

    // ------------------------------------
    // Handler for timer
    // ------------------------------------
    class timerListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            // Construct a DatagramPacket to receive data from the UDP socket
            rcvdp = new DatagramPacket(buffer, buffer.length);

            try {
                // receive the DP from the socket:
                RTPsocket.receive(rcvdp);

                // create an RTPpacket object from the DP
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());

                // print important header fields of the RTP packet received:
                System.out.println("Got RTP packet with SeqNum # " + rtp_packet.getsequencenumber() + " TimeStamp "
                        + rtp_packet.gettimestamp() + " ms, of type " + rtp_packet.getpayloadtype());

                // print header bitstream:
                rtp_packet.printheader();

                // get the payload bitstream from the RTPpacket object
                int payload_length = rtp_packet.getpayload_length();
                byte[] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                // get an Image object from the payload bitstream
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Image image = toolkit.createImage(payload, 0, payload_length);

                // display the image as an ImageIcon object
                icon = new ImageIcon(image);
                iconLabel.setIcon(icon);

            } catch (InterruptedIOException iioe) {
                System.out.println("Nothing to read");
            } catch (IOException ioe) {
                System.out.println("Exception caught: " + ioe);
            }
        }
    }

    // ------------------------------------
    // Parse Server Response
    // ------------------------------------
    private int parse_server_response() {
        int reply_code = 0;

        try {
            String response = RTSPBufferedReader.readLine();
            System.out.println("RTSP Client - Received from Server: " + response);

            StringTokenizer tokens = new StringTokenizer(response);
            tokens.nextToken();
            reply_code = Integer.parseInt(tokens.nextToken());

        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }

        return (reply_code);
    }

    // ------------------------------------
    // Send RTSP Request
    // ------------------------------------
    private void send_RTSP_request(String request_type) {
        try {
            // Writing the RTSP request to the socket
            RTSPBufferedWriter.write(request_type + " RTSP/1.0" + "\n");
            // RTSPBufferedWriter.write("CSeq: " + RTSPSeqNb + "\n");

            // if (request_type.equals("SETUP")){
            // RTSPBufferedWriter.write("Transport: RTP/UDP; client_port= " + RTP_RCV_PORT +
            // "\n");
            // } else {
            // RTSPBufferedWriter.write("Session: " + RTSPid + "\n");
            // }

            RTSPBufferedWriter.flush();
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }
    }
}
