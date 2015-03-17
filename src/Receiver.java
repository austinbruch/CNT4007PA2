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

   // Static String used for Carriage Return Line Feed
   public static String CRLF = "\r\n";

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

      this.state = ReceiverEnum.WAIT0; // The Receiver starts with waiting for a Packet with sequence number 0
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

   // Runs the Receiver
   public void run() {
      // Create and open all of the required connections
      this.initialize();

      int totalPacketsReceived = 0;             // Keeps track of how many Packets have been received
      String message = "";                      // Builds the message that is being transmitted via incoming Packets
      byte lastSequenceNumber = (byte) 0xFF;    // Keeps track of which Packet sequence number was most recently transmitted

      try {
         String inputFromNetwork = null;        // Input from the Network

         while ( (inputFromNetwork = this.brFromSocket.readLine()) != null) {
            // Don't always make a packet out of it, this could also be a -1 message indicating shutdown
            byte[] fromSender = Network.hexStringToByteArray(inputFromNetwork); // Decode the hex-encoded string
            
            if (fromSender.length == 1) {
               if (fromSender[0] == (byte) 0xFF) {
                  // -1 was sent, terminate everything
                  System.out.println("Received -1, now terminating.");
                  System.exit(0);
               }
            } else { // Attempt to interpret the incoming byte array as a Packet
               Packet packetFromSender = new Packet(fromSender);     // Create a Packet based on the incoming bytes
               totalPacketsReceived++;                               // One more Packet has been received
               
               boolean corrupted = this.isIncomingPacketCorrupted(packetFromSender);   // Determine if the incoming Packet has been corrupted
               boolean wrongSequenceNumber = this.hasSequenceNumber(packetFromSender, lastSequenceNumber);  // Determine if the incoming Packet has the wrong sequence number

               ACK ack = new ACK(); // The ACK to be sent to the Network
               if (corrupted || wrongSequenceNumber) {         // If the Packet is either corrupted or the wrong Packet, issue the old ACK
                  if (lastSequenceNumber == (byte)0xFF) {      // Special case to handle the first corrupted/wrong Packet
                     lastSequenceNumber = (byte)0x1;
                  }
                  ack.setSequenceNumber(lastSequenceNumber);   // Set the sequence number of the ACK
                  ack.setChecksum((byte)0x0);                  // Set the checksum of the ACK
                  System.out.println(this.generateMessageForTerminal(this.state, totalPacketsReceived, packetFromSender, ack));  // Print the console message
                  this.sendACKToNetwork(ack);                  // Send the ACK to the Network
               } else { // The Packet is good
                  ack.setSequenceNumber(packetFromSender.getSequenceNumber());   // Set the sequence number of the ACK
                  ack.setChecksum((byte)0x0);                                    // Set the checksum of the ACK
                  System.out.println(this.generateMessageForTerminal(this.state, totalPacketsReceived, packetFromSender, ack));  // Print the console message
                  this.sendACKToNetwork(ack);                                    // Send the ACK to the Network
                  lastSequenceNumber = packetFromSender.getSequenceNumber();     // update the new last sequence number
                  this.toggleState();                                            // move on to the next state
                  message += packetFromSender.getContent() + " ";                // Keep building the message
                  if (packetFromSender.getContent().endsWith(".")) {
                     // This packet is the end of the message
                     message = message.trim();
                     System.out.println("Message: " + message);                  // Display the complete message
                     // TODO if we are handling multiple messages, clear out the message string variable here to start over
                     // Maybe add the existing message to some sort of list to keep track of all messages
                  }
               }
            }   
         }
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to read from the Network Socket.");
      }
      
   }

   // Used to toggle the current state of this Receiver
   private void toggleState() {
      if (this.state == ReceiverEnum.WAIT0) {
         this.state = ReceiverEnum.WAIT1;
      } else {
         this.state = ReceiverEnum.WAIT0;
      }
   }

   // Determines if the incoming Packet has been corrupted or not
   // This is done by manually calculating the checksum of the Packet and comparing that value to the checksum value supplied by the Packet
   private boolean isIncomingPacketCorrupted(Packet packet) {
      boolean corrupted;

      int checksum = 0;
      String content = packet.getContent();

      for (int i = 0; i < content.length(); i++) {
         checksum += content.charAt(i);
      }

      if (checksum == packet.getChecksum()) {
         corrupted = false;
      } else {
         corrupted = true;
      }

      return corrupted;
   }

   // Determine if the incoming Packet has the specified sequence number
   private boolean hasSequenceNumber(Packet packet, byte sequenceNumber) {
      boolean hasSeqNum;
      
      if (packet.getSequenceNumber() == sequenceNumber) {
         hasSeqNum = true;
      } else {
         hasSeqNum = false;
      }

      return hasSeqNum;
   }
   
   // Sends the specified ACK packet to the Network
   private void sendACKToNetwork(ACK ack) {
      try {
       this.dosToSocket.writeBytes(new String(ack.asByteArray()) + CRLF);  
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to send an ACK packet to the Network.");
      }   
   }

   // Creates the required message to be printed the console based on:
   //    the current state of the Receiver
   //    how many packets have been received by the Receiver
   //    the Packet that was received by the Receiver
   //    which ACK will be sent to the Network
   private String generateMessageForTerminal(ReceiverEnum currentState, int totalPacketsReceived, Packet packetReceived, ACK ackToSend) {
      String message = "";

      if (currentState == ReceiverEnum.WAIT0) { // Start the message off with the current state
         message += "Waiting 0, ";
      } else {
         message += "Waiting 1, ";
      }

      message += Integer.toString(totalPacketsReceived) + ", "; // Concatentate the number of packets received thus far

      message += packetReceived.getSequenceNumber() + " ";  // Concatentate the most recently received Packet to the message
      message += packetReceived.getPacketID() + " "; 
      message += packetReceived.getChecksum() + " ";
      message += packetReceived.getContent() + ", ";

      message += "ACK";
      message += ackToSend.getSequenceNumber(); // Concatenate which type of ACK is being sent

      return message;
   }

   // Drive the Receiver class
   // Should be called by `java Receiver [URL] [portNumber]`
   public static void main(String... args) {
      if (args.length != 2) {
         System.out.println("Usage:\njava Receiver [URL] [portNumber]");
         System.exit(0);
      }

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