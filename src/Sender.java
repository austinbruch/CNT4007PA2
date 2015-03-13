/**
 * @author Austin Bruch
 * CNT4007C Programming Assignment 2
 * Sender Class
*/

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class Sender {

   // Where to connect to the Network
   private String networkURL;
   private int networkPort;

   // Socket related
   private Socket networkSocket;
   private BufferedReader brFromSocket;
   private DataOutputStream dosToSocket;

   // Input Message File
   private String messageFileName;
   // private File messageFile;
   private BufferedReader brFromInputFile;


   // Constructor
   public Sender(String networkURL, int networkPort, String messageFileName) {
      this.networkURL = networkURL;
      this.networkPort = networkPort;
      this.messageFileName = messageFileName;

      this.networkSocket = null;
      this.brFromSocket = null;
      this.dosToSocket = null;

      // this.messageFile = null;
      this.brFromInputFile = null;
   }

   // Setup Socket, Readers, Writers, etc.
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

      // Setup the Input File
      try {
         this.brFromInputFile = new BufferedReader(new FileReader(this.messageFileName));
      } catch (FileNotFoundException e) {
         System.out.println("The Message File specified was not found.");
         System.exit(0);
      }

   }

   // Start the sending process
   public void run() {
      this.initialize();

   }

   // Drive the Sender class
   // Should be called via `java Sender [URL] [portNumber] [messageFileName]`
   public static void main(String... args) {
      String url = args[0];
      int port = 0;
      try {
         port = Integer.parseInt(args[1]);
      } catch (NumberFormatException e) {
         System.out.println("The port specified number could not be parsed as an Integer.");
         System.exit(0);
      }
      String messageFile = args[2];

      Sender sender = new Sender(url, port, messageFile);
      sender.run();
   }

}