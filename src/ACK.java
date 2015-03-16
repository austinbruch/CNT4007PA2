/**
 * @author Austin Bruch
 * CNT4007C Programming Assignment 2
 * ACK Class
 */

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ACK {
   private byte sequenceNumber;
   private byte checksum;
   
   // Constructor
   public ACK(byte sequenceNumber, byte checksum) {
      this.sequenceNumber = sequenceNumber;
      this.checksum = checksum;
   }

   public ACK() {
      this.sequenceNumber = (byte) 0x0;
      this.checksum = (byte) 0x0;
   }

   // Constructor based on an ACK in byte array notation
   public ACK(byte[] bytes) {
      this.sequenceNumber = bytes[0];
      this.checksum = bytes[1];
   }

   // Getters and Setters
   public byte getSequenceNumber() {
      return this.sequenceNumber;
   }

   public byte getChecksum() {
      return this.checksum;
   }

   public void setSequenceNumber(byte sequenceNumber) {
      this.sequenceNumber = sequenceNumber;
   }

   public void setChecksum(byte checksum) {
      this.checksum = checksum;
   }

   /**
    * Update this ACK's checksum
    * TODO: not sure how the checksum is created for an ACK
    */
   public void updateChecksum() {
      
   }

   public byte[] asByteArray() {
      byte[] bytes = new byte[2];
      bytes[0] = this.sequenceNumber;
      bytes[1] = this.checksum;

      return bytes;
   }

   @Override
   public String toString() {

      byte[] bytes = new byte[2];
      bytes[0] = this.sequenceNumber;
      bytes[1] = this.checksum;

      String toReturn = "Bytes: " + Arrays.toString(bytes) + "\n";
      toReturn += "Sequence Number: " + this.sequenceNumber + "\n"; 
      toReturn += "Checksum: " + this.checksum + "\n";

      return toReturn;
   }
} 