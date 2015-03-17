/**
 * @author Austin Bruch
 * CNT4007C Programming Assignment 2
 * Sender Enumeration Class
 */

// Sender has 4 states, Waiting for ACK with Sequence Number 0, and 1, and Sending a Packet with Sequence Number 0, and 1.
public enum SenderEnum {
   SEND0, SEND1, WAIT0, WAIT1
}