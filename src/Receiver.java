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

      int totalPacketsReceived = 0;
      String message = "";
      byte lastSequenceNumber = (byte) 0xFF;

      try {
         String inputFromNetwork = null;

         while ( (inputFromNetwork = this.brFromSocket.readLine()) != null) {
            // Don't always make a packet out of it, this could also be a -1 message indicating it's over
            byte[] fromSender = Network.hexStringToByteArray(inputFromNetwork);
            // System.out.println("Number of bytes: " + fromSender.length);
            // for (int i = 0; i < fromSender.length; i++) {
            //    System.out.print(fromSender[i]);
            //    if (i < fromSender.length-1) {
            //       System.out.print(", ");
            //    } else {
            //       System.out.print("\n");
            //    }
            // }
            
            if (fromSender.length == 1) {
               if (fromSender[0] == (byte) 0xFF) {
                  // -1 was sent, terminate everything
                  // TODO: write -1 to the network then close up shop
                  System.out.println("Received -1, now terminating.");
                  System.exit(0);
               }
            } else {
               Packet packetFromSender = new Packet(fromSender);
               totalPacketsReceived++;
               // System.out.println("Received: Packet" + packetFromSender.getSequenceNumber() + ", " + packetFromSender.getPacketID());
               
               boolean corrupted = this.isIncomingPacketCorrupted(packetFromSender);
               boolean wrongSequenceNumber = this.hasSequenceNumber(packetFromSender, lastSequenceNumber);

               ACK ack = new ACK();
               if (corrupted || wrongSequenceNumber) {
                  if (lastSequenceNumber == (byte)0xFF) {
                     lastSequenceNumber = (byte)0x1;
                  }
                  ack.setSequenceNumber(lastSequenceNumber);
                  ack.setChecksum((byte)0x0);
                  System.out.println(this.generateMessageForTerminal(this.state, totalPacketsReceived, packetFromSender, ack));
                  this.sendACKToNetwork(ack);
               } else {
                  ack.setSequenceNumber(packetFromSender.getSequenceNumber());
                  ack.setChecksum((byte)0x0);
                  // System.out.println("Test2");
                  System.out.println(this.generateMessageForTerminal(this.state, totalPacketsReceived, packetFromSender, ack));
                  this.sendACKToNetwork(ack);
                  lastSequenceNumber = packetFromSender.getSequenceNumber(); // update the new last sequence number
                  this.toggleState(); // move on to the next state
                  message += packetFromSender.getContent() + " ";
                  if (packetFromSender.getContent().endsWith(".")) {
                     // This packet is the end of the message
                     message = message.trim();
                     System.out.println("Message: " + message);
                  }
               }
            }   
         }
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to read from the Network Socket.");
      }
      
   }

   private void toggleState() {
      if (this.state == ReceiverEnum.WAIT0) {
         this.state = ReceiverEnum.WAIT1;
      } else {
         this.state = ReceiverEnum.WAIT0;
      }
   }

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

   private boolean hasSequenceNumber(Packet packet, byte sequenceNumber) {
      boolean hasSeqNum;
      
      if (packet.getSequenceNumber() == sequenceNumber) {
         hasSeqNum = true;
      } else {
         hasSeqNum = false;
      }

      return hasSeqNum;
   }
   
   private void sendACKToNetwork(ACK ack) {
      try {
       this.dosToSocket.writeBytes(new String(ack.asByteArray()) + CRLF);  
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to send an ACK packet to the Network.");
      }   
   }

   private String generateMessageForTerminal(ReceiverEnum currentState, int totalPacketsReceived, Packet packetReceived, ACK ackToSend) {
      String message = "";

      if (currentState == ReceiverEnum.WAIT0) {
         message += "Waiting 0, ";
      } else {
         message += "Waiting 1, ";
      }

      message += Integer.toString(totalPacketsReceived) + ", ";

      message += packetReceived.getSequenceNumber() + " "; 
      message += packetReceived.getPacketID() + " "; 
      message += packetReceived.getChecksum() + " ";
      message += packetReceived.getContent() + ", ";

      message += "ACK";
      message += ackToSend.getSequenceNumber();

      return message;
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