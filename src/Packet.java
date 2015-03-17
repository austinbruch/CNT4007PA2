/**
 * @author Austin Bruch
 * CNT4007C Programming Assignment 2
 * Packet Class
 */

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Packet {

   // Instance variables
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

   // Default constructor
   public Packet() {
      this.sequenceNumber = (byte) 0x0;
      this.packetID = (byte) 0x0;
      this.checksum = 0;
      this.content = null;
   }

   // Constructor used to create a Packet from a validated Byte Array
   public Packet(byte[] bytes) {
      if(bytes.length >=7) { // Need at least 7 bytes in the byte array to create a proper Packet
         this.sequenceNumber = bytes[0];
         this.packetID = bytes[1];
         
         byte[] checksumBytes = new byte[4]; // Create a byte array for the checksum integer
         checksumBytes[0] = bytes[2];
         checksumBytes[1] = bytes[3];
         checksumBytes[2] = bytes[4];
         checksumBytes[3] = bytes[5];

         this.checksum = ByteBuffer.wrap(checksumBytes).getInt(); // Create the integer for the checksum via ByteBuffer

         byte[] contentBytes = new byte[bytes.length-6]; // Create a byte array for the bytes of the content
         for (int i = 0; i < contentBytes.length; i++) {
            contentBytes[i] = bytes[6 + i];
         }

         this.content = new String(contentBytes); // Create the content string

      } else { // There weren't enough bytes in the array, so create a default Packet 
         this.sequenceNumber = (byte) 0x0;
         this.packetID = (byte) 0x0;
         this.checksum = 0;
         this.content = null;
      }
      
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

   // Update this Packet's checksum value based on it's content string 
   public void updateChecksum() {
      int check = 0;
      for (int i = 0; i < this.content.length(); i++) {
         check += this.content.charAt(i); // Add all of the content string's characters together
      }
      this.checksum = check;
   }

   // Generates a byte array that accurately represents this Packet
   // This byte array can be used with the constructor that takes a byte array
   public byte[] asByteArray() {
      byte[] stringBytes = this.content.getBytes(); // Get the content string in byte array form
      ByteBuffer bb = ByteBuffer.allocate(4);
      bb.putInt(this.checksum);
      byte[] checksumBytes = bb.array();  // Get a byte array of the checksum integer

      byte[] bytes = new byte[1 + 1 + 4 + stringBytes.length]; // Create a new byte array to hold all of this Packet's information
      bytes[0] = this.sequenceNumber;     // Sequence Number goes first
      bytes[1] = this.packetID;           // Packet ID is second
      bytes[2] = checksumBytes[0];        // Then all of the checksum bytes
      bytes[3] = checksumBytes[1];
      bytes[4] = checksumBytes[2];
      bytes[5] = checksumBytes[3];

      for (int i = 0; i < stringBytes.length; i++) {
         bytes[6 + i] = stringBytes[i];   // Then all of the content bytes
      }

      return bytes;
   }

   // Override the toString for debugging purposes
   @Override
   public String toString() {
      
      String toReturn = "Bytes: " + Arrays.toString(this.asByteArray()) + "\n";
      toReturn += "Sequence Number: " + this.sequenceNumber + "\n"; 
      toReturn += "Packet ID: " + this.packetID + "\n";
      toReturn += "Checksum: " + this.checksum + "\n";
      toReturn += "Content: " + this.content + "\n";

      return toReturn;
   }
} 