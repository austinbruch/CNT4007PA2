/**
 * @author Austin Bruch
 * CNT4007C Programming Assignment 2
 * Network Class
 */

import java.io.IOException;
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
   public Network() {
      this.listenSocket = null;
      this.senderSocket = null;
      this.receiverSocket = null;
      this.port = 0;
   }

   // Constructor
   public Network(int port) {
      this.listenSocket = null;
      this.senderSocket = null;
      this.receiverSocket = null;
      this.port = port;
   }

   private void initialize() {
      try {
         this.listenSocket = new ServerSocket(port);
      } catch (IOException e) {
         System.out.println("An I/O exception occurred while trying to establish a ServerSocket on port " + Integer.toString(this.port) + ".");
         System.exit(0);
      }
      System.out.println("Network listening on port " + Integer.toString(this.port));
   }

   public void run() throws IOException {
      this.initialize();

      int numberOfConnections = 0;

      ReceiverThread receiverThread = null;
      SenderThread senderThread = null;

      while (true) {
         // if(numberOfConnections == 2) {
         //    break;
         // }

         Socket socket = this.listenSocket.accept();

         if (numberOfConnections == 0) {
            this.receiverSocket = socket; // connect the receiver first
            receiverThread = new ReceiverThread(this, this.receiverSocket, this.senderSocket);
         } else if (numberOfConnections == 1) {
            this.senderSocket = socket; // connect the sender second
            senderThread = new SenderThread(this, this.senderSocket, this.receiverSocket);
            receiverThread.start();
            senderThread.start();
         }

         numberOfConnections++;

      }
   }

   private String getRandomNetworkAction() {
      Random random = new Random();
      String pass = "PASS", corrupt = "CORRUPT", drop = "DROP";
      String networkAction;
      double rand = random.nextDouble();
      
      if (rand <= 0.5) {
         networkAction = pass;
      } else if (rand <= 0.75) {
         networkAction = corrupt;
      } else {
         networkAction = drop;
      }

      return networkAction;
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