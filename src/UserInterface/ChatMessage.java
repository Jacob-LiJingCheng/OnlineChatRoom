package UserInterface;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    private String sendTime;
    private String senderName;
    private String messageInString;


    public void setSendTime(String sendTime){
        this.sendTime = sendTime;
    }
    public void setSenderName(String senderName){this.senderName = senderName; }
    public void setMessageInString(String messageInString){
        this.messageInString = messageInString;
    }

    public String getSendTime(){
        return sendTime;
    }
    public String getMessageInString(){
        return messageInString;
    }
    public String getSenderName() { return senderName; }

    public ChatMessage(String sendTime, String senderName, String messageInString){
        this.sendTime = sendTime;
        this.senderName = senderName;
        this.messageInString = messageInString;
    }
}
