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

   private Network network;

   private Socket senderSocket;
   private Socket receiverSocket;

   private BufferedReader bufferedReader;
   private DataOutputStream dataOutputStream;

   public SenderThread(Network network, Socket senderSocket, Socket receiverSocket) {
      this.network = network;
      this.senderSocket = senderSocket;
      this.receiverSocket = receiverSocket;

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
   }

   @Override
   public void run() {
      System.out.println("Sender started.");


   }  

}