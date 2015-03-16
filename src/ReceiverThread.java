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

   private Network network;

   private Socket receiverSocket;
   private Socket senderSocket;

   private BufferedReader bufferedReader;
   private DataOutputStream dataOutputStream;

   public ReceiverThread(Network network) {
      this.network = network;
   }

   private void initialize() {
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

   @Override
   public void run() {
      System.out.println("Receiver started.");

      this.initialize();

      String inputFromReceiver = null;

      try {
         while ((inputFromReceiver = this.bufferedReader.readLine()) != null) {
            // Don't always make a packet out of it, this could also be a -1 message indicating it's over
            byte[] fromReceiver = inputFromReceiver.getBytes();
            
            if (fromReceiver.length == 1) {
               if (fromReceiver[0] == 0xFF) {
                  // -1 was sent, terminate everything
                  // TODO: write -1 to the network then close up shop
                  // Nothing has to happen here because the Receiver will never send -1, only receive it.
               }
            } else {
               ACK ackFromReceiver = new ACK(fromReceiver);
               String networkAction = this.network.getRandomNetworkAction();
               System.out.println("Received: ACK" + ackFromReceiver.getSequenceNumber() + ", " + networkAction);

               if (networkAction.equals("PASS")) {
                  passPacketFromReceiverToSender(ackFromReceiver);
               } else if (networkAction.equals("CORRUPT")) {
                  corruptPacketFromReceiverToSender(ackFromReceiver);
               } else if (networkAction.equals("DROP")) {
                  dropPacketFromReceiverToSender(ackFromReceiver);
               }
            }
         }
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while trying to read from the Sender Socket.");
      }
   }  

   private void sendPacketFromReceiverToSender(String ack) {
      try {
         this.dataOutputStream.writeBytes(ack + CRLF);
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while attempting to pass the Receiver's ACK packet to the Sender.");
      }
   }

   private void passPacketFromReceiverToSender(ACK ack) {
      sendPacketFromReceiverToSender(new String(ack.asByteArray()));
   }

   private void corruptPacketFromReceiverToSender(ACK ack) {
      ack.setChecksum((byte) (ack.getChecksum() + 0x1));
      sendPacketFromReceiverToSender(new String(ack.asByteArray()));
   }

   private void dropPacketFromReceiverToSender(ACK ack) {
      try {
         ACK drop = new ACK((byte) 0x2, (byte) 0x0); // Create an ACK packet that has a sequence number of 2, indicating DROPped packet
         this.dataOutputStream.writeBytes(new String(drop.asByteArray())+ CRLF);
      } catch (IOException e) {
         System.out.println("An I/O Error occurred while attemping to send an ACK2 packet indicating a DROP to the Sender.");
      }
   }
}