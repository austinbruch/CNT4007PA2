/**
 * @author Austin Bruch
 * CNT4007C Programming Assignment 2
 * Sender Thread Class
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.net.Socket;

// reads from sender socket
// writes to the receiver socket

public class SenderThread extends Thread {

   // Static String used for Carriage Return Line Feeds
   public static String CRLF = "\r\n";

   // The connected Network instance 
   private Network network;

   // Sockets
   private Socket senderSocket;
   private Socket receiverSocket;

   // Reader from the Sender Socket
   // Writer to the Receiver Socket
   // Writer to the Sender Socket (exclusively for DROPs)
   private BufferedReader bufferedReader;
   private DataOutputStream dataOutputStream;
   private DataOutputStream dataOutputStreamToSender;

   // Constructor
   public SenderThread(Network network) {
      this.network = network;
   }

   // Initialize the Sockets and readers and writers
   private void initialize() {
      // Get references to the sockets
      this.senderSocket = this.network.getSenderSocket();
      this.receiverSocket = this.network.getReceiverSocket();

      try {
         this.bufferedReader = new BufferedReader(new InputStreamReader(this.senderSocket.getInputStream()));
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to setup a BufferedReader from the Sender Socket.");
         System.exit(0);
      }

      try {
         this.dataOutputStream = new DataOutputStream(this.receiverSocket.getOutputStream());
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to setup a DataOutputStream to the Receiver Socket.");
         System.exit(0);
      }

      try {
         this.dataOutputStreamToSender = new DataOutputStream(this.senderSocket.getOutputStream());
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to setup a DataOutputStream to the Sender Socket.");
         System.exit(0);
      }

   }

   // What executes when this Thread is executed
   @Override
   public void run() {
      System.out.println("Sender started.");
      // Initialize this Thread
      this.initialize();

      String inputFromSender = null; // Input from the Sender

      try {
         while ((inputFromSender = this.bufferedReader.readLine()) != null) {
            // Don't always make a packet out of it, this could also be a -1 message indicating it's over
            byte[] fromSender = Network.hexStringToByteArray(inputFromSender); // Decode the bytes sent from the Sender
            
            if (fromSender.length == 1) {
               if (fromSender[0] == (byte)0xFF) {
                  // -1 was sent, terminate everything
                  try {
                     this.dataOutputStream.writeBytes(Network.byteArrayToHexString(new byte[]{(byte)0xFF}) + CRLF); // Following convention of encoding a byte array with the termination code
                  } catch (IOException e) {
                     System.out.println("An I/O Error occurred while attempting to pass the Quit Signal -1 to the Receiver.");
                  } 
                  System.out.println("Received -1, now terminating.");
                  System.exit(0);
               }
            } else { // Interpret the bytes as a Packet
               Packet packetFromSender = new Packet(fromSender);           // Create the packet
               String networkAction = Network.getRandomNetworkAction();    // Determine which network action will occur
               System.out.println("Received: Packet" + packetFromSender.getSequenceNumber() + ", " + packetFromSender.getPacketID() + ", " + networkAction); // Print console message

               if (networkAction.equals("PASS")) {
                  passPacketFromSenderToReceiver(packetFromSender);     // Pass the Packet through to the Receiver
               } else if (networkAction.equals("CORRUPT")) {
                  corruptPacketFromSenderToReceiver(packetFromSender);  // Corrupt the Packet and send it to the Receiver
               } else if (networkAction.equals("DROP")) {
                  dropPacketFromSenderToReceiver(packetFromSender);     // Drop the Packet
               }
            }
         }
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to read from the Sender Socket.");
      }

   }  

   // Sends the specified Packet to the Receiver socket
   private void sendPacketFromSenderToReceiver(String packet) {
      try {
         this.dataOutputStream.writeBytes(packet + CRLF);
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while attempting to pass the Sender's packet to the Receiver.");
      }
   }

   // Pass the specified Packet to the Receiver socket
   private void passPacketFromSenderToReceiver(Packet packet) {
      sendPacketFromSenderToReceiver(Network.byteArrayToHexString(packet.asByteArray())); // Forward the packet as-is (no corruption)
   }

   // Corrupt the specified Packet then send it to the Receiver socket
   private void corruptPacketFromSenderToReceiver(Packet packet) {
      packet.setChecksum((packet.getChecksum() + 1)); // Corrupt the checksum by adding 1 bit
      sendPacketFromSenderToReceiver(Network.byteArrayToHexString(packet.asByteArray()));
   }

   // Drop the Packet
   // We simulate a dropped packet by sending an ACK2 (ACK with sequence number 2) to the Sender
   private void dropPacketFromSenderToReceiver(Packet packet) {
      try {
         ACK drop = new ACK((byte) 0x2, (byte) 0x0); // Create an ACK packet that has a sequence number of 2, indicating DROPped packet
         this.dataOutputStreamToSender.writeBytes(Network.byteArrayToHexString(drop.asByteArray())+ CRLF);     // Encode the ACK for safe transfer
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while attemping to send an ACK2 packet indicating a DROP to the Sender.");
      } 
   }
}