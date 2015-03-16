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
import java.io.UnsupportedEncodingException;
import java.lang.StringBuffer;
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
   private int totalPacketsSent;
   private ACK fromNetwork;

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
      this.totalPacketsSent = 0;
      this.fromNetwork = null;
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

      boolean first = true;
      // Here's the FSM
      while (packets.size() != 0) {
         this.advanceState();
         if(!first) {
            System.out.println(this.generateMessageForTerminal(this.state, this.totalPacketsSent, this.fromNetwork, "send", 0));
         }
         this.sendPacketToNetwork(packets.get(0));
         this.totalPacketsSent++;
         first = false;

         while (waitState(packets.get(0), (byte)0x0)) {}
         this.advanceState();
         packets.remove(0);

         if(packets.size() != 0) {
            this.advanceState();
            System.out.println(this.generateMessageForTerminal(this.state, this.totalPacketsSent, this.fromNetwork, "send", 1));
            this.sendPacketToNetwork(packets.get(0));
            this.totalPacketsSent++;


            while (waitState(packets.get(0), (byte)0x1)) {}
            this.advanceState();
            packets.remove(0);
         } else {
            this.advanceState();
            System.out.println(this.generateMessageForTerminal(this.state, this.totalPacketsSent, this.fromNetwork, "none", -1));
            this.sendTerminateToNetwork();
            break;
         }

      }
   }

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

   private boolean isIncomingAckCorrupted(ACK ack) {
      boolean corrupted;

      if (ack.getChecksum() == (byte)0x0) {
         corrupted = false;
      } else {
         corrupted = true;
      }

      return corrupted;
   }

   private boolean isTimeoutAck(ACK ack) {
      boolean isTimeout;

      if (ack.getSequenceNumber() == (byte)0x2) {
         isTimeout = true;
      } else {
         isTimeout = false;
      }

      return isTimeout;
   }

   private boolean hasSequenceNumber(ACK ack, byte sequenceNumber) {
      boolean hasSeqNum;
      
      if (ack.getSequenceNumber() == sequenceNumber) {
         hasSeqNum = true;
      } else {
         hasSeqNum = false;
      }

      return hasSeqNum;
   }
   
   private void sendPacketToNetwork(Packet packet) {
      try {        
         // Encode the bytes in hex, then write that to a string
         String toSend = Network.byteArrayToHexString(packet.asByteArray());
         // System.out.println("sendPacketToNetwork string: [" + toSend + "]");
         this.dosToSocket.writeBytes( toSend + CRLF);
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to send a Packet to the Network.");
      } 
   }

   private void sendTerminateToNetwork() {
      try{
         this.dosToSocket.writeBytes(Network.byteArrayToHexString(new byte[]{(byte)0xFF}) + CRLF);
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to send the Terminate Sequence to the Network.");
      }
   }

   private String generateMessageForTerminal(SenderEnum currentState, int totalPacketsSent, ACK ackReceived, String action, int seqToSend) {
      String message = "";

      if (currentState == SenderEnum.WAIT0) {
         message += "Waiting ACK0, ";
      } else if (currentState == SenderEnum.WAIT1) {
         message += "Waiting ACK1, ";
      }

      message += Integer.toString(totalPacketsSent) + ", ";

      if (ackReceived.getSequenceNumber() == (byte)0x2) {
         message += "DROP, ";
      } else if (ackReceived.getSequenceNumber() == (byte)0x0) {
         message += "ACK0, ";
      } else if (ackReceived.getSequenceNumber() == (byte)0x1) {
         message += "ACK1, ";
      }

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