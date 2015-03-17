/**
 * @author Austin Bruch
 * CNT4007C Programming Assignment 2
 * Receiver Thread Class
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;

// reads from receiver socket
// writes to the sender socket

public class ReceiverThread extends Thread {

   public static String CRLF = "\r\n";

   // The connected Network instance
   private Network network;

   // Sockets
   private Socket receiverSocket;
   private Socket senderSocket;

   // Reader from the Receiver Socket
   // Writer to the Sender Socket
   private BufferedReader bufferedReader;
   private DataOutputStream dataOutputStream;

   // Constructor
   public ReceiverThread(Network network) {
      this.network = network;
   }

   // Initialize the sockets and the readers and writers
   private void initialize() {
      // Get references to the sockets
      this.senderSocket = this.network.getSenderSocket();
      this.receiverSocket = this.network.getReceiverSocket();

      try {
         this.bufferedReader = new BufferedReader(new InputStreamReader(this.receiverSocket.getInputStream()));
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to setup a BufferedReader from the Sender Socket.");
         System.exit(0);
      }
      try {
         this.dataOutputStream = new DataOutputStream(this.senderSocket.getOutputStream());
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to setup a DataOutputStream to the Receiver Socket.");
         System.exit(0);
      }
   }

   // What executes when this Thread is executed
   @Override
   public void run() {
      System.out.println("Receiver started.");
      // Initialize this Thread
      this.initialize();

      String inputFromReceiver = null; // Input from the Receiver

      try {
         while ((inputFromReceiver = this.bufferedReader.readLine()) != null) {
            byte[] fromReceiver = Network.hexStringToByteArray(inputFromReceiver); // Decode to get the bytes sent from the Receiver
            
            ACK ackFromReceiver = new ACK(fromReceiver);             // Create an ACK from the bytes sent from the Receiver
            String networkAction = Network.getRandomNetworkAction(); // Determine which network action will occur
            System.out.println("Received: ACK" + ackFromReceiver.getSequenceNumber() + ", " + networkAction); // Print console message

            if (networkAction.equals("PASS")) {
               passPacketFromReceiverToSender(ackFromReceiver);      // Pass the ACK through to the Sender
            } else if (networkAction.equals("CORRUPT")) {
               corruptPacketFromReceiverToSender(ackFromReceiver);   // Corrupt the ACK and send it to the Sender
            } else if (networkAction.equals("DROP")) {  
               dropPacketFromReceiverToSender(ackFromReceiver);      // Drop the ACK packet
            }
         }
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to read from the Receiver Socket.");
      }
   }  

   // Sends the specified ACK packet to the Sender socket
   private void sendPacketFromReceiverToSender(String ack) {
      try {
         this.dataOutputStream.writeBytes(ack + CRLF);
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while attempting to pass the Receiver's ACK packet to the Sender.");
      }
   }

   // Pass the specified ACK packet to the Sender socket
   private void passPacketFromReceiverToSender(ACK ack) {
      sendPacketFromReceiverToSender(Network.byteArrayToHexString(ack.asByteArray()));    // Encode the ACK for safe transfer
   }

   // Corrupt the specified ACK packet then send it to the Sender socket
   private void corruptPacketFromReceiverToSender(ACK ack) {
      ack.setChecksum((byte) (ack.getChecksum() + 0x1)); // Corrupt the ACK by adding 1 to the checksum
      sendPacketFromReceiverToSender(Network.byteArrayToHexString(ack.asByteArray()));    // Encode the ACK for safe transfer
   }

   // Drop the ACK packet
   // We simulate a Dropped packet by sending an ACK2 (ACK with sequence number 2) to the Sender
   private void dropPacketFromReceiverToSender(ACK ack) {
      try {
         ACK drop = new ACK((byte) 0x2, (byte) 0x0); // Create an ACK packet that has a sequence number of 2, indicating DROPped packet
         this.dataOutputStream.writeBytes(Network.byteArrayToHexString(drop.asByteArray())+ CRLF);    // Encode the ACK for safe transfer
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while attemping to send an ACK2 packet indicating a DROP to the Sender.");
      }
   }
}