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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.StringTokenizer;


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

   // State for State Machine
   private SenderEnum state;

   public static String CRLF = "\r\n";


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

      this.state = SenderEnum.SEND0;
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

   /**
    * Convert this Sender's message file to an ArrayList of Packets
    * @return ArrayList<Packet> packets
    */
   private ArrayList<Packet> convertMessageToPackets() {
      ArrayList<Packet> packets = new ArrayList<Packet>();

      String message = "";

      String temp = null;

      try {
         while((temp = this.brFromInputFile.readLine()) != null) {
            message += temp;
         }
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while reading the message from the message file.");
         System.exit(0);
      }

      // At this point, the string message should contain the entire file in one variable
      // Separate the message by spaces so that each packet only has 1 word in it.
      StringTokenizer tokenizer = new StringTokenizer(message, " ");

      Packet p;
      boolean sequenceNumber = false;
      byte packetNumber = (byte) 0x1;
      while(tokenizer.hasMoreTokens()) {
         p = new Packet();
         p.setSequenceNumber( (sequenceNumber) ? (byte) 0x1 : (byte) 0x0 );
         p.setPacketID(packetNumber);
         p.setContent(tokenizer.nextToken());
         p.updateChecksum();

         // System.out.println(p);

         packets.add(p);

         // Update for the next packet
         // TODO Check for a period to start a new message and sequence of packets?
         sequenceNumber = !sequenceNumber;
         packetNumber = (byte) (packetNumber + 0x1);
      }

      return packets;
   }

   // Start the sending process
   public void run() {
      // Create and open all of the required connections
      this.initialize();

      // Generate all of the packets that will be sent
      ArrayList<Packet> packets = this.convertMessageToPackets();

      // TEST simply write the first packet as a string from the byte array representation of the packet
      try {
         this.dosToSocket.writeBytes(new String(packets.get(0).asByteArray()) + CRLF);
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while writing to the Network Socket.");
      }
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