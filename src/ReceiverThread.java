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
   }  
   
}