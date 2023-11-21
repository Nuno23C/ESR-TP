import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class Server extends JFrame implements ActionListener {
    // GUI:
    JLabel label;

    // RTSP variables:
    Socket RTSPsocket; // Socket used to send/receive messages
    static BufferedReader RTSPBufferedReader; // Input stream filters from the socket
    static BufferedWriter RTSPBufferedWriter; // Output stream filters to the socket
    static String state; // RTSP Server state

    // RTP variables:
    DatagramPacket senddp; // UDP packet containing the video frames (to send)
    DatagramSocket RTPsocket; // socket to be used to send/receive UDP packet
    int RTP_dest_port = 25000; // destination port for RTP packets
    InetAddress ClientIPAddr; // Client IP address

    // Video constants:
    static String VideoFileName; // video file to request to the server
    VideoStream video; // VideoStream object used to access video frames
    int imagenb = 0; // image nb of the image currently transmitted
    static int MJPEG_TYPE = 26; // RTP payload type for MJPEG video
    static int FRAME_PERIOD = 100; // Frame period of the video to stream, in ms
    static int VIDEO_LENGTH = 500; // length of the video in frames

    Timer timer; // timer used to send the images at the video frame rate
    byte[] buffer; // buffer used to store the images to send to the client

    // --------------------------
    // Server constructor
    // --------------------------
    public Server() {
        // init Frame
        super("Server");

        // init Timer
        timer = new Timer(FRAME_PERIOD, this);
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        // allocate memory for the sending buffer
        buffer = new byte[15000];

        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            System.out.println("Endereço IP do Servidor: " + inetAddress.getHostAddress());

            System.out.println("Server: Waiting for connection");
            // Initiate TCP connection to connect client to server
            ServerSocket listenSocket = new ServerSocket(RTP_dest_port);
            RTSPsocket = listenSocket.accept();
            listenSocket.close();

            // Get Client IP address
            ClientIPAddr = RTSPsocket.getInetAddress();

            System.out.println("Server: socket " + ClientIPAddr);

            // init the VideoStream object
            video = new VideoStream(VideoFileName);

        } catch (SocketException e) {
            System.out.println("Server: error on socket: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Server: error on video: " + e.getMessage());
        }

        // Handler to close the main window
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // stop the timer and exit
                timer.stop();
                System.exit(0);
            }
        });

        // GUI:
        label = new JLabel("Send frame #        ", JLabel.CENTER);
        getContentPane().add(label, BorderLayout.CENTER);

        timer.start();
    }

    // ------------------------------------
    // main
    // ------------------------------------
    public static void main(String argv[]) throws Exception {
        if (argv.length == 1) {
            VideoFileName = argv[0];
            System.out.println("Server: Video file passed as parameter");
        } else {
            VideoFileName = "movie.Mjpeg";
            System.out.println("Server: Video file name wasn't passed as a parameter");
        }
        System.out.println("VideoFileName = " + VideoFileName);

        File f = new File(VideoFileName);
        if (!f.exists()) {
            System.out.println("Server: Video file doesn't exist");
            System.exit(0);
        }

        Server server = new Server();

        // Set input and output stream filters
        RTSPBufferedReader = new BufferedReader(new InputStreamReader(server.RTSPsocket.getInputStream()));
        RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(server.RTSPsocket.getOutputStream()));

        // Initial RTSP state
        state = "INIT";

        // Wait for the SETUP message from the client
        String request_type;
        boolean done = false;
        while (!done) {
            // read the request
            request_type = RTSPBufferedReader.readLine();
            System.out.println("Servidor: request_type: " + request_type);

            if (request_type == "SETUP") {
                System.out.println("Server: SETUP received from Client.");

                done = true;
                state = "READY";

                // Send response
                server.send_RTSP_response();

                // init the VideoStream object:
                server.video = new VideoStream(VideoFileName);

                // init RTP socket
                server.RTPsocket = new DatagramSocket();
            }
        }
    }

    // ------------------------
    // Handler for timer
    // ------------------------
    public void actionPerformed(ActionEvent e) {

        System.out.println("actionPerformed");

        // if the current image nb is less than the length of the video
        if (imagenb < VIDEO_LENGTH) {
            // update current imagenb
            imagenb++;

            try {
                // get next frame to send from the video, as well as its size
                int image_length = video.getnextframe(buffer);

                // Builds an RTPpacket object containing the frame
                RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb * FRAME_PERIOD, buffer, image_length);

                // get to total length of the full rtp packet to send
                int packet_length = rtp_packet.getlength();

                // retrieve the packet bitstream and store it in an array of bytes
                byte[] packet_bits = new byte[packet_length];
                rtp_packet.getpacket(packet_bits);

                // send the packet as a DatagramPacket over the UDP socket
                senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);
                RTPsocket.send(senddp);

                System.out.println("Send frame #" + imagenb);
                // print the header bitstream
                rtp_packet.printheader();

                // update GUI
                // label.setText("Send frame #" + imagenb);
            } catch (Exception ex) {
                System.out.println("Exception caught: " + ex);
                System.exit(0);
            }
        } else {
            // if we have reached the end of the video file, stop the timer
            timer.stop();
        }
    }

    // ------------------------------------
    // Send RTSP Response
    // ------------------------------------
    private void send_RTSP_response() {
        try {
            RTSPBufferedWriter.write("200 OK");

            System.out.println("RTSP Server - Sent response to Client.");

            RTSPBufferedWriter.flush();
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }
    }
}
