package edu.scu.bluetoothchat;

/**
 * Data structure for chat message.
 */
public class ChatMessage {

    static public final int MSG_SENDER_ME = 0;
    static public final int MSG_SENDER_OTHERS = 1;

    public int messageSender;
    public String messageContent;

}
