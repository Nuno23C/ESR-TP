/* ------------------
   Servidor
   usage: java Servidor [Video file]
   adaptado dos originais pela equipa docente de ESR (nenhumas garantias)
   colocar primeiro o cliente a correr, porque este dispara logo
   ---------------------- */

import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class Server extends JFrame implements ActionListener {

    // GUI:
    // ----------------
    JLabel label;

    // RTP variables:
    // ----------------
    DatagramPacket senddp; // UDP packet containing the video frames (to send)A
    Socket RTSPsocket; // socket to be used to send and receive UDP packet
    int RTP_dest_port = 25000; // destination port for RTP packets
    InetAddress ClientIPAddr; // Client IP address

    static String VideoFileName; // video file to request to the server

    // Video constants:
    // ------------------
    int imagenb = 0; // image nb of the image currently transmitted
    VideoStream video; // VideoStream object used to access video frames
    static int MJPEG_TYPE = 26; // RTP payload type for MJPEG video
    static int FRAME_PERIOD = 100; // Frame period of the video to stream, in ms
    static int VIDEO_LENGTH = 500; // length of the video in frames

    Timer sTimer; // timer used to send the images at the video frame rate
    byte[] sBuf; // buffer used to store the images to send to the client

    // RTSP variables
    // ----------------
    // rtsp states
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;

    // rtsp message types
    final static int SETUP = 3;
    final static int PLAY = 4;
    final static int PAUSE = 5;
    final static int TEARDOWN = 6;
    static int state;

    static BufferedReader RTSPBufferedReader;
    static BufferedWriter RTSPBufferedWriter;

    // --------------------------
    // Constructor
    // --------------------------
    public Server() {
        // init Frame
        super("Server");

        // init para a parte do servidor
        sTimer = new Timer(FRAME_PERIOD, this); // init Timer para servidor
        sTimer.setInitialDelay(0);
        sTimer.setCoalesce(true);
        sBuf = new byte[15000]; // allocate memory for the sending buffer

        // Handler to close the main window
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // stop the timer and exit
                sTimer.stop();
                System.exit(0);
            }
        });

        // GUI:
        label = new JLabel("Send frame #        ", JLabel.CENTER);
        getContentPane().add(label, BorderLayout.CENTER);

        sTimer.start();
        System.out.println("Servidor: vai começar a enviar frames...");

    }

    // ------------------------------------
    // main
    // ------------------------------------
    public static void main(String argv[]) throws Exception {
        // get video filename to request:
        if (argv.length >= 1) {
            VideoFileName = argv[0];
            System.out.println("Servidor: VideoFileName indicado como parametro: " + VideoFileName);
        } else {
            VideoFileName = "movie.Mjpeg";
            System.out.println("Servidor: parametro não foi indicado. VideoFileName = " + VideoFileName);
        }

        File f = new File(VideoFileName);
        if (f.exists()) {
            // Create a Server object
            Server server = new Server();

            // get RTSP socket port from the command line
            int RTPport = Integer.parseInt(argv[0]);

            try {
                // Initiate TCP connection to connect client to server
                ServerSocket listenSocket = new ServerSocket(RTPport);
                server.RTSPsocket = listenSocket.accept();
                listenSocket.close();

                // Get Client IP address
                server.ClientIPAddr = server.RTSPsocket.getInetAddress();

                // Initiate RTSPstate
                state = INIT;

                // Set input and output stream filters:
                RTSPBufferedReader = new BufferedReader(new InputStreamReader(server.RTSPsocket.getInputStream()));
                RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(server.RTSPsocket.getOutputStream()));

                // Wait for the SETUP message from the client
                int request_type;
                boolean done = false;
                while (!done) {
                    request_type = server.parse_RTSP_request(); // blocking

                    if (request_type == SETUP) {
                        done = true;

                        // update RTSP state
                        state = READY;
                        System.out.println("Servidor: SETUP received from Client.");

                        // Send response
                        server.send_RTSP_response();

                        // init the VideoStream object:
                        server.video = new VideoStream(VideoFileName);

                        // init RTP socket
                        server.RTPsocket = new DatagramSocket();
                    }
                }

                // loop to handle RTSP requests
                while (true) {
                    // parse the request
                    request_type = server.parse_RTSP_request(); // blocking

                    if ((request_type == PLAY) && (state == READY)) {
                        // send back response
                        server.send_RTSP_response();
                        // start timer
                        server.sTimer.start();
                        // update state
                        state = PLAYING;
                        System.out.println("Servidor: PLAYING");
                    } else if ((request_type == PAUSE) && (state == PLAYING)) {
                        // send back response
                        server.send_RTSP_response();
                        // stop timer
                        server.sTimer.stop();
                        // update state
                        state = READY;
                        System.out.println("Servidor: READY");
                    } else if (request_type == TEARDOWN) {
                        // send back response
                        server.send_RTSP_response();
                        // stop timer
                        server.sTimer.stop();
                        // close sockets
                        server.RTSPsocket.close();
                        server.RTPsocket.close();

                        System.exit(0);
                    }
                }
            } catch (SocketException e) {
                System.out.println("Servidor: erro no socket: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Servidor: erro no video: " + e.getMessage());
            }

        } else {
            System.out.println("Ficheiro de video não existe: " + VideoFileName);
        }
    }

    // ------------------------
    // Handler for timer
    // ------------------------
    public void actionPerformed(ActionEvent e) {

        System.out.println("=> actionPerformed");

        // if the current image nb is less than the length of the video
        if (imagenb < VIDEO_LENGTH) {
            // update current imagenb
            imagenb++;

            try {
                // get next frame to send from the video, as well as its size
                int image_length = video.getnextframe(sBuf);

                // Builds an RTPpacket object containing the frame
                RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb * FRAME_PERIOD, sBuf, image_length);

                // get to total length of the full rtp packet to send
                int packet_length = rtp_packet.getlength();
                System.out.println("packet length: " + packet_length);

                // retrieve the packet bitstream and store it in an array of bytes
                byte[] packet_bits = new byte[packet_length];
                rtp_packet.getpacket(packet_bits);
                System.out.println("packet_bits : " + packet_bits);

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
            sTimer.stop();
        }
    }

}// end of Class Servidor
