/**
 * @author Austin Bruch
 * CNT4007C Programming Assignment 2
 * Sender Thread Class
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;

// reads from sender socket
// writes to the receiver socket

public class SenderThread extends Thread {

   public static String CRLF = "\r\n";

   private Network network;

   private Socket senderSocket;
   private Socket receiverSocket;

   private BufferedReader bufferedReader;
   private DataOutputStream dataOutputStream;
   private DataOutputStream dataOutputStreamToSender;

   public SenderThread(Network network) {
      this.network = network;
   }

   private void initialize() {
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

   @Override
   public void run() {
      System.out.println("Sender started.");

      this.initialize();

      String inputFromSender = null;

      try {
         while ((inputFromSender = this.bufferedReader.readLine()) != null) {
            // Don't always make a packet out of it, this could also be a -1 message indicating it's over
            byte[] fromSender = inputFromSender.getBytes();
            
            if (fromSender.length == 1) {
               if (fromSender[0] == 0xFF) {
                  // -1 was sent, terminate everything
                  // TODO: write -1 to the network then close up shop
               }
            } else {
               Packet packetFromSender = new Packet(fromSender);
               String networkAction = this.network.getRandomNetworkAction();
               System.out.println("Received: Packet" + packetFromSender.getSequenceNumber() + ", " + packetFromSender.getPacketID() + ", " + networkAction);

               if (networkAction.equals("PASS")) {
                  passPacketFromSenderToReceiver(packetFromSender);
               } else if (networkAction.equals("CORRUPT")) {
                  corruptPacketFromSenderToReceiver(packetFromSender);
               } else if (networkAction.equals("DROP")) {
                  // TODO send DROP signal back to Sender to simulate timeout
                  // Do this by sending an ACK2 packet
                  dropPacketFromSenderToReceiver(packetFromSender);
               }
            }
            
         }
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to read from the Sender Socket.");
      }

   }  

   private void sendPacketFromSenderToReceiver(String packet) {
      try {
         this.dataOutputStream.writeBytes(packet + CRLF);
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while attempting to pass the Sender's packet to the Receiver.");
      }
   }

   private void passPacketFromSenderToReceiver(Packet packet) {
      sendPacketFromSenderToReceiver(new String(packet.asByteArray())); // Forward the packet as-is (no corruption)
   }

   private void corruptPacketFromSenderToReceiver(Packet packet) {
      packet.setChecksum((byte) (packet.getChecksum() + 0x1) ); // Corrupt the checksum by adding 1 bit
      sendPacketFromSenderToReceiver(new String(packet.asByteArray()));
   }

   private void dropPacketFromSenderToReceiver(Packet packet) {
      try {
         ACK drop = new ACK((byte) 0x2, (byte) 0x0); // Create an ACK packet that has a sequence number of 2, indicating DROPped packet
         this.dataOutputStreamToSender.writeBytes(new String(drop.asByteArray())+ CRLF);
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while attemping to send an ACK2 packet indicating a DROP to the Sender.");
      }
   }
}