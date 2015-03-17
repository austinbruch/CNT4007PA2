/**
 * @author Austin Bruch
 * CNT4007C Programming Assignment 2
 * Sender Class
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
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
   private BufferedReader brFromInputFile;

   // State for State Machine
   private SenderEnum state;

   // Keeps track of how many packets have been sent by this sender; scoped by instance
   private int totalPacketsSent;

   // Keeps track of the most recently acquired ACK packet from the Network
   private ACK fromNetwork;

   // Carriage Return Line Feed Constant
   public static String CRLF = "\r\n";

   // Constructor
   public Sender(String networkURL, int networkPort, String messageFileName) {
      this.networkURL = networkURL;
      this.networkPort = networkPort;
      this.messageFileName = messageFileName;

      this.networkSocket = null;
      this.brFromSocket = null;
      this.dosToSocket = null;

      this.brFromInputFile = null;

      this.state = SenderEnum.SEND0; // Initially start at the first state, sending the 0 packet
      this.totalPacketsSent = 0; // Haven't sent any packets yet
      this.fromNetwork = null; // Haven't received any ACKs yet
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
      ArrayList<Packet> packets = new ArrayList<Packet>(); // Container for the new Packet objects

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
      boolean sequenceNumber = false; // Alternate false --> true to simulate 0 and 1
      byte packetNumber = (byte) 0x1;

      // Keep doing this until there aren't any more packets to create
      while(tokenizer.hasMoreTokens()) {
         p = new Packet();
         p.setSequenceNumber( (sequenceNumber) ? (byte) 0x1 : (byte) 0x0 ); // Start with 0, then go to 1
         p.setPacketID(packetNumber);
         p.setContent(tokenizer.nextToken()); 
         p.updateChecksum(); // Make the checksum reflect the content of the Packet
         packets.add(p); // Add the newly created packet to the ArrayList of Packets

         // Update for the next packet
         // TODO Check for a period to start a new message and sequence of packets?
         sequenceNumber = !sequenceNumber; // Toggle the sequence number 
         packetNumber = (byte) (packetNumber + 0x1); // Add 1 to the packet number
      }

      return packets;
   }

   // Start the sending process
   public void run() {
      // Create and open all of the required connections
      this.initialize();

      // Generate all of the packets that will be sent
      ArrayList<Packet> packets = this.convertMessageToPackets();

      boolean first = true;
      boolean doneFlag = false;
      // Here's the FSM
      while (packets.size() != 0) {
         this.advanceState();
         if(!first) {
            // Only print this message out when it's not the first Packet
            System.out.println(this.generateMessageForTerminal(this.state, this.totalPacketsSent, this.fromNetwork, "send", 0));
         }
         this.sendPacketToNetwork(packets.get(0)); // Send the Packet
         this.totalPacketsSent++; // Increment how many Packets we've sent
         first = false; // Any time after this, it's not the first anymore

         while (waitState(packets.get(0), (byte)0x0)) {} // Wait for a response from the Network
         this.advanceState(); // Once we get an acceptable response, advance to sending the next Packet, if there are any
         packets.remove(0); // Remove the leading Packet, so that the new first Packet is the Packet that needs to be sent next

         if (packets.size() != 0) { // If there are any more Packets to send
            this.advanceState(); // Advance to the next state
            // We now need to send the next Packet, so generate the console message
            System.out.println(this.generateMessageForTerminal(this.state, this.totalPacketsSent, this.fromNetwork, "send", 1));
            this.sendPacketToNetwork(packets.get(0)); // Send the next Packet
            this.totalPacketsSent++; // Increment how many Packets we've sent

            while (waitState(packets.get(0), (byte)0x1)) {} // Wait for an response from the network
            this.advanceState(); // Once we receive an acceptable response, advance to sending the next Packet, if there are any
            packets.remove(0); // Remove the leading Packet, so that the new first Packet is the Packet that needs to be sent next
         } else { // There are no more packets to send
            this.advanceState(); // Advance to the next state
            System.out.println(this.generateMessageForTerminal(this.state, this.totalPacketsSent, this.fromNetwork, "none", -1));
            this.sendTerminateToNetwork(); // Terminate the Network
            doneFlag = true; 
            break; // Break out of this sending loop
         }

      }
      // If the last Packet had sequence number 1, terminate the Network and Receiver
      if (!doneFlag) {
         System.out.println(this.generateMessageForTerminal(this.state, this.totalPacketsSent, this.fromNetwork, "none", -1));
         this.sendTerminateToNetwork();
         doneFlag = true;
      }

   }

   // Waits for an ACK from the Network
   // Returns false if the Sender received the correct ACK and can move on to the next Packet
   // Returns true if a timeout occurred, or the received ACK was corrupted or for the wrong sequence number
   private boolean waitState(Packet packet, byte sequenceNumber) {
      // Wait for an ACK from the Receiver
      try {
         String inputFromNetwork = null;
         while( (inputFromNetwork = this.brFromSocket.readLine()) != null) {
            this.fromNetwork = new ACK(Network.hexStringToByteArray(inputFromNetwork));
            boolean corrupted = this.isIncomingAckCorrupted(this.fromNetwork);
            boolean isTimeout = this.isTimeoutAck(this.fromNetwork);
            boolean rightSequenceNumber = hasSequenceNumber(this.fromNetwork, (byte) sequenceNumber);
            byte wrongSeq = (sequenceNumber == (byte) 0x0) ? (byte) 0x1 : (byte) 0x0;
            boolean wrongSequenceNumber = hasSequenceNumber(this.fromNetwork, wrongSeq);

            if (isTimeout) {
               System.out.println(this.generateMessageForTerminal(this.state, this.totalPacketsSent, this.fromNetwork, "resend", packet.getSequenceNumber()));
               this.sendPacketToNetwork(packet); // resend the packet and wait again
               this.totalPacketsSent++;
               return true; // indicate that we need to iterate again
            }

            if (!corrupted && !isTimeout && rightSequenceNumber) {
               return false; // The ACK indicates that the packet was delivered successfully
            }

            if (wrongSequenceNumber) {
               System.out.println(this.generateMessageForTerminal(this.state, this.totalPacketsSent, this.fromNetwork, "resend", packet.getSequenceNumber()));
               this.sendPacketToNetwork(packet); // resend the packet and wait again
               this.totalPacketsSent++;
               return true;
            }

            if (corrupted) {
               System.out.println(this.generateMessageForTerminal(this.state, this.totalPacketsSent, this.fromNetwork, "resend", packet.getSequenceNumber()));
               this.sendPacketToNetwork(packet); // resend the packet and wait again
               this.totalPacketsSent++;
               return true;
            }
            
         }
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while reading from the Network Socket.");
      }
      return true;
   }

   // Used to advance the state of this Sender
   private void advanceState() {
      if (this.state == SenderEnum.SEND0) {
         this.state = SenderEnum.WAIT0;
      } else if (this.state == SenderEnum.WAIT0) {
         this.state = SenderEnum.SEND1;
      } else if (this.state == SenderEnum.SEND1) {
         this.state = SenderEnum.WAIT1;
      } else if (this.state == SenderEnum.WAIT1) {
         this.state = SenderEnum.SEND0;
      }
   }

   // Determines if the incoming ACK packet is corrupted or not
   // Since ACKs are basic, the checksum of an ACK is supposed to be 0
   // If it's nonzero, corruption has occurred
   private boolean isIncomingAckCorrupted(ACK ack) {
      boolean corrupted;

      if (ack.getChecksum() == (byte)0x0) {
         corrupted = false;
      } else {
         corrupted = true;
      }

      return corrupted;
   }

   // Determines if the incoming ACK packet is a Timeout ACK (ACK2)
   private boolean isTimeoutAck(ACK ack) {
      boolean isTimeout;

      if (ack.getSequenceNumber() == (byte)0x2) {
         isTimeout = true;
      } else {
         isTimeout = false;
      }

      return isTimeout;
   }

   // Determines if the incoming ACK packet has the specified sequence number
   private boolean hasSequenceNumber(ACK ack, byte sequenceNumber) {
      boolean hasSeqNum;
      
      if (ack.getSequenceNumber() == sequenceNumber) {
         hasSeqNum = true;
      } else {
         hasSeqNum = false;
      }

      return hasSeqNum;
   }
   
   // Send the specified Packet to the Network
   private void sendPacketToNetwork(Packet packet) {
      try {        
         // Encode the bytes in hex, then write that hex  to a string
         String toSend = Network.byteArrayToHexString(packet.asByteArray());
         this.dosToSocket.writeBytes( toSend + CRLF); // Send the hex string to the Network, followed by a CRLF since BufferedReader#readLine is used
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to send a Packet to the Network.");
      } 
   }

   // Send the special terminate byte to the Network (-1 or 0xFF)
   private void sendTerminateToNetwork() {
      try{
         // Simply following convention throughout the code of converting a byte array to a hex-encoded string
         this.dosToSocket.writeBytes(Network.byteArrayToHexString(new byte[]{(byte)0xFF}) + CRLF); 
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to send the Terminate Sequence to the Network.");
      }
   }

   // Creates the message to be sent to the console based on:
   //    the current state of the Sender
   //    how many packets have been sent so far
   //    which kind of ACK was received
   //    which action will be performed next (from the Sender FSM)
   //    which sequence number will be sent next, if any
   private String generateMessageForTerminal(SenderEnum currentState, int totalPacketsSent, ACK ackReceived, String action, int seqToSend) {
      String message = "";

      // The Sender only prints messages when it is in Waiting states
      if (currentState == SenderEnum.WAIT0) {
         message += "Waiting ACK0, ";
      } else if (currentState == SenderEnum.WAIT1) {
         message += "Waiting ACK1, ";
      }

      // Concatentate how many packets have been sent thus far
      message += Integer.toString(totalPacketsSent) + ", ";

      // Concatentate which kind of ACK was received (ACK0, ACK1, or ACK2 (DROP))
      if (ackReceived.getSequenceNumber() == (byte)0x2) {
         message += "DROP, ";
      } else if (ackReceived.getSequenceNumber() == (byte)0x0) {
         message += "ACK0, ";
      } else if (ackReceived.getSequenceNumber() == (byte)0x1) {
         message += "ACK1, ";
      }

      // Concatenate the specific action message
      if (action.equals("none")) {
         message += "no more packets to send";
      } else {
         message += action + " Packet" + Integer.toString(seqToSend);
      }

      return message;
   }

   // Drive the Sender class
   // Should be called via `java Sender [URL] [portNumber] [messageFileName]`
   public static void main(String... args) {
      if (args.length != 3) {
         System.out.println("Usage:\njava Sender [URL] [portNumber] [messageFileName]");
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
      String messageFile = args[2];

      Sender sender = new Sender(url, port, messageFile);
      sender.run();
   }

}