/**
 * @author Austin Bruch
 * CNT4007C Programming Assignment 2
 * Receiver Class
 */

public class Receiver {
   
   // Drive the Receiver class
   // Should be called by `java Receiver [URL] [portNumber]`
   public static void main(String... args) {
      String url = args[0];
      int port = 0;
      try {
         port = Integer.parseInt(args[1]);
      } catch (NumberFormatException e) {
         System.out.println("The port specified number could not be parsed as an Integer.");
         System.exit(0);
      }
      String messageFile = args[2];

      // Receiver receiver = new Receiver(url, port, messageFile);
      // receiver.run();

   }

   
}