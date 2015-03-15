/**
 * @author Austin Bruch
 * CNT4007C Programming Assignment 2
 * Packet Class
 */

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Packet {
   private byte sequenceNumber;
   private byte packetID;
   private int checksum;
   private String content;

   // Constructor
   public Packet(byte sequenceNumber, byte packetID, int checksum, String content) {
      this.sequenceNumber = sequenceNumber;
      this.packetID = packetID;
      this.checksum = checksum;
      this.content = content;
   }

   public Packet() {
      this.sequenceNumber = (byte) 0x0;
      this.packetID = (byte) 0x0;
      this.checksum = 0;
      this.content = null;
   }

   // Getters and Setters
   public byte getSequenceNumber() {
      return this.sequenceNumber;
   }

   public byte getPacketID() {
      return this.packetID;
   }

   public int getChecksum() {
      return this.checksum;
   }

   public String getContent() {
      return this.content;
   }

   public void setSequenceNumber(byte sequenceNumber) {
      this.sequenceNumber = sequenceNumber;
   }

   public void setPacketID(byte packetID){
      this.packetID = packetID;
   }

   public void setChecksum(int checksum) {
      this.checksum = checksum;
   }

   public void setContent(String content) {
      this.content = content;
   }

   /**
    * Update this Packet's checksum value based on it's content string
    *
    */
   public void updateChecksum() {
      int check = 0;
      for (int i = 0; i < this.content.length(); i++) {
         check += this.content.charAt(i);
      }
      this.checksum = check;
   }

   @Override
   public String toString() {
      byte[] stringBytes = this.content.getBytes();
      ByteBuffer bb = ByteBuffer.allocate(4);
      bb.putInt(this.checksum);
      byte[] checksumBytes = bb.array();

      byte[] bytes = new byte[1 + 1 + 4 + stringBytes.length];
      bytes[0] = this.sequenceNumber;
      bytes[1] = this.packetID;
      bytes[2] = checksumBytes[0];
      bytes[3] = checksumBytes[1];
      bytes[4] = checksumBytes[2];
      bytes[5] = checksumBytes[3];

      for (int i = 0; i < stringBytes.length; i++) {
         bytes[6 + i] = stringBytes[i];
      }
      String toReturn = "Bytes: " + Arrays.toString(bytes) + "\n";
      toReturn += "Sequence Number: " + this.sequenceNumber + "\n"; 
      toReturn += "Packet ID: " + this.packetID + "\n";
      toReturn += "Checksum: " + this.checksum + "\n";
      toReturn += "Content: " + this.content + "\n";

      return toReturn;
   }
} 