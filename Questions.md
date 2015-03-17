#Questions

1) Do we need to modify the Sender FSM?
   -  Because there's not a real timeout, if a corrupted packet or an incorrect ACK comes in, we need to resend it, not just wait.

   __Answer:__ Yes, you can modify the FSM since real timeouts aren't used.

2) Support message file having > 1 message?
   -  Example: each message is essentially 1 sentence (messages are delimited via string ending with a period).

   __Answer:__ No, you only need to support 1 message (1 sentence).

3) What state should the Sender be in when it prints to the console that there are no more packets to send?

4) Is it ok to require that the Receiver connects to the Network before the Sender connects?
   -  Possible workaround:
      -  have sender send the line "sender" when it connects
      -  have receiver send the line "receiver" when it connects
      -  have the network determine which socket belongs to which, and setup the threads appropriately
   -  Second possible workaround:
      -  have all three programs specify 1 port number, as usual
      -  in reality, the receiver program (and Network respectively) uses the specified port number + 1 for receiver
      -  that way, Network listens on 2 ports, 1 dedicated to the receiver, the other dedicated to the sender

   __Answer:__ It is ok to enforce that the Receiver connects first (won't lose points), but in reality it should not be this way. 
      -  Implement both in a "Client" class



