package utils;

import java.io.Serializable;
public class Message implements Serializable {
    public String content;
    public String senderId;
    public String reciverId;
    public double x, y;
    public double distance;
    public String multi_purpose_AntennaId;



    public enum MessageType {
        SMS,
        Leave,
        Search,
        Update_Location,
        Search_reply,
        Connection
    }
    public MessageType type;

    public Message(){

    }
    public Message(MessageType type, String senderId, String content){
        this.type =type;
        this.senderId = senderId;
        this.content = content;
    }

    public Message(MessageType messageType, String id, String s, double userLocationX, double userLocationY) {
        this.type = messageType;
        this.senderId = id;
        this.content = s;
        this.x = userLocationX;
        this.y = userLocationY;
    }
    public Message(MessageType type, String senderId, String content,String destinationAntennaId){
        this.type =type;
        this.senderId = senderId;
        this.content = content;
        this.multi_purpose_AntennaId = destinationAntennaId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReciverId() {
        return reciverId;
    }

    public void setReciverId(String reciverId) {
        this.reciverId = reciverId;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getMulti_purpose_AntennaId() {
        return multi_purpose_AntennaId;
    }

    public void setMulti_purpose_AntennaId(String multi_purpose_AntennaId) {
        this.multi_purpose_AntennaId = multi_purpose_AntennaId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType Type) {
        this.type = Type;
    }
 
    

    public Message(String content){
        this.content = content;
    }
}
