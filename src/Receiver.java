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