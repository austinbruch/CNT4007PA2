/**
 * @author Austin Bruch
 * CNT4007C Programming Assignment 2
 * Network Class
 */

import java.io.IOException;
import java.lang.StringBuffer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class Network {

   // ServerSocket that listens for the Sender and Receiver to connect
   private ServerSocket listenSocket;

   // Sockets used to manage the Sender and the Receiver
   private Socket senderSocket;
   private Socket receiverSocket;

   // Port number to listen on
   private int port;

   // Constructor
   public Network(int port) {
      this.listenSocket = null;
      this.senderSocket = null;
      this.receiverSocket = null;
      this.port = port;
   }

   // Getters for Sender and Receiver sockets
   public Socket getSenderSocket() {
      return this.senderSocket;
   }

   public Socket getReceiverSocket() {
      return this.receiverSocket;
   }

   // Initialize the ServerSocket to listen for incoming connections
   private void initialize() {
      try {
         this.listenSocket = new ServerSocket(port);
      } catch (IOException e) {
         System.out.println("An I/O exception occurred while trying to establish a ServerSocket on port " + Integer.toString(this.port) + ".");
         System.exit(0);
      }
      System.out.println("Network listening on port " + Integer.toString(this.port));
   }

   // Run the Network, which accepts a Receiver and a Sender (only two total client sockets)
   public void run() throws IOException {
      this.initialize(); // Initialize the ServerSocket

      int numberOfConnections = 0; // Keeps track of the number of connections that have been made

      ReceiverThread receiverThread = null; // Thread that handles data from the Receiver
      SenderThread senderThread = null; // Thread that handles data from the Sender

      while (true) {
         Socket socket = this.listenSocket.accept(); // Block until an incoming connection occurs

         if (numberOfConnections == 0) {
            this.receiverSocket = socket; // connect the receiver first
            receiverThread = new ReceiverThread(this); // Create the Receiver Thread with reference to the Network instance
         } else if (numberOfConnections == 1) {
            this.senderSocket = socket; // connect the sender second
            senderThread = new SenderThread(this); // Create the Sender Thread with reference to the Network instance
            receiverThread.start(); // Only now do we start both threads
            senderThread.start();
         }

         numberOfConnections++;
      }
   }

   // Returns a Network Action at "random"
   // Probabilities are: 
   //    PASS : 0.5
   //    CORRUPT : 0.25
   //    DROP : 0.25
   protected static String getRandomNetworkAction() {
      Random random = new Random(); // Random number generator
      String pass = "PASS", corrupt = "CORRUPT", drop = "DROP"; // All possible network actions
      String networkAction; // The resulting network action
      double rand = random.nextDouble(); // Get a random number between 0 and 1
      
      if (rand <= 0.5) {
         networkAction = pass;
      } else if (rand <= 0.75) {
         networkAction = corrupt;
      } else {
         networkAction = drop;
      }

      return networkAction;
   }

   // Converts a string made up of hexadecimal characters into a byte array
   // Each set of 2 characters is mapped to a new byte in the byte array
   protected static byte[] hexStringToByteArray(String hexString) {
      int length = hexString.length();
      
      if (length % 2 == 1) {
         hexString = "0" + hexString; // Pad the string with a leading 0 if needed
         length++;
      }
      
      byte[] bytes = new byte[length / 2];
      for (int i = 0; i < length; i += 2) {
         bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + (Character.digit(hexString.charAt(i+1), 16)));
      }
      return bytes;
   }

   // Converts a byte array to a String of hexadecimal characters
   // Each byte in the array is mapped to 2 hex characters
   protected static String byteArrayToHexString(byte[] bytes) {
      StringBuffer stringBuffer = new StringBuffer();
      for(int i=0; i < bytes.length; i++){ 
         stringBuffer.append(Character.forDigit((bytes[i] >> 4) & 0xF, 16)); 
         stringBuffer.append(Character.forDigit((bytes[i] & 0xF), 16)); 
      }
      return stringBuffer.toString();
   }


   // Drive the Network class
   // Should be called by `java Network [portNumber]`
   public static void main(String... args) throws IOException {
      if(args.length != 1) {
         System.out.println("Usage:\n java Network [portNumber]");
         System.exit(0);
      }

      int port = 0;
      try {
         port = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
         System.out.println("The port specified number could not be parsed as an Integer.");
         System.exit(0);
      }

      Network network = new Network(port);
      network.run();
   }
}