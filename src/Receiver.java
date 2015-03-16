/**
 * @author Austin Bruch
 * CNT4007C Programming Assignment 2
 * Receiver Class
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class Receiver {

   // Where to connect to the Network
   private String networkURL;
   private int networkPort;

   // Socket related
   private Socket networkSocket;
   private BufferedReader brFromSocket;
   private DataOutputStream dosToSocket;

   // State for State Machine
   private ReceiverEnum state;

   // Constructor
   public Receiver(String networkURL, int networkPort) {
      this.networkURL = networkURL;
      this.networkPort = networkPort;

      this.state = ReceiverEnum.WAIT0;
   }

   // Setup Socket Readers, Writers, etc.
   private void initialize() {
      // Setup the socket
      try {
         this.networkSocket = new Socket(this.networkURL, this.networkPort);
      } catch (UnknownHostException e) {
         System.out.println("The specified Network Host was unable to be found.");
         System.exit(0);
      } catch (IOException e) {
         System.out.println("Connecting to the Network resulted in an I/O Error.");
         System.exit(0);
      }

      try {
         this.brFromSocket = new BufferedReader(new InputStreamReader(this.networkSocket.getInputStream()));
      } catch (IOException e) {
         System.out.println("Opening a Reader from the Network Socket resulted in an I/O Error.");
         System.exit(0);
      }

      try {
         this.dosToSocket = new DataOutputStream(this.networkSocket.getOutputStream());
      } catch (IOException e) {
         System.out.println("Opening a Writer to the Network Socket resulted in an I/O Error.");
         System.exit(0);  
      }
   }

   public void run() {
      // Create and open all of the required connections
      this.initialize();


      try {
         String inputFromNetwork = null;

         while ( (inputFromNetwork = this.brFromSocket.readLine()) != null) {
            // Don't always make a packet out of it, this could also be a -1 message indicating it's over
               byte[] fromSender = inputFromNetwork.getBytes();
               
               if (fromSender.length == 1) {
                  if (fromSender[0] == 0xFF) {
                     // -1 was sent, terminate everything
                     // TODO: write -1 to the network then close up shop
                  }
               } else {
                  Packet packetFromSender = new Packet(fromSender);
                  System.out.println("Received: Packet" + packetFromSender.getSequenceNumber() + ", " + packetFromSender.getPacketID());
                  System.out.println(packetFromSender.getContent());
               }
               
         }
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to read from the Network Socket.");
      }
      
   }
   
   // Drive the Receiver class
   // Should be called by `java Receiver [URL] [portNumber]`
   public static void main(String... args) {
      String url = args[0];
      int port = 0;
      try {
         port = Integer.parseInt(args[1]);
      } catch (NumberFormatException e) {
         System.out.println("The port specified number could not be parsed as an Integer.");
         System.exit(0);
      }

      Receiver receiver = new Receiver(url, port);
      receiver.run();
   }
   
}