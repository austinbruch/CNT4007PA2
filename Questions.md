Questions
=========
1) Do we need to modify the Sender FSM?
   -  Because there's not a real timeout, if a corrupted packet or an incorrect ACK comes in, we need to resend it, not just wait.

2) Support message file having > 1 message?
   -  Example: each message is essentially 1 sentence (messages are delimited via string ending with a period).

3) What state should the Sender be in when it prints to the console that there are no more packets to send?

