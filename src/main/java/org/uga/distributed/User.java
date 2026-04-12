package org.uga.distributed;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class User{


    private String id;
    private double userLocationX;
    private double userLocationY;
    private String currentAntennaId;
    private double new_locationX;
    private double new_locationY;
    private Map<String, Message> repliesReceived = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();
private Channel channel;

    public void startUser(String id)throws Exception{
// Initialize RabbitMQ connection and channel 
// creates a unique queue for the user to receive messages
this.id = id; 
ConnectionFactory factory = new ConnectionFactory();
 factory.setHost("localhost");
 Connection connection = factory.newConnection();
 this.channel = connection.createChannel();

// Declaring a queue for the user to recive messages 
String userQueueName = "user_queue" + id;
channel.queueDeclare(userQueueName,false,false,false,null);


DeliverCallback deliverCallback = (consumerTag, delivery) -> {
    Message incomming_message = objectMapper.readValue(delivery.getBody(),Message.class);
    if(incomming_message.getType()== Message.MessageType.Search_reply){
 repliesReceived.put(incomming_message.getSenderId(),incomming_message);
 System.out.println("User"+ id+ " recives singal from Antenna" + incomming_message.getSenderId());

    } 
    else if (incomming_message.getType() == Message.MessageType.SMS){
        System.out.println("SMS from " + incomming_message.getSenderId() + ""+ incomming_message.getContent());


    } };
    channel.basicConsume(userQueueName, true, deliverCallback, consumerTag -> {});   



// searching antenna for first time 
String ExchangeName = "Search";
channel.exchangeDeclare(ExchangeName,"fanout");
Message search_message = new Message();
search_message.setType(Message.MessageType.Search);
search_message.setSenderId(this.id);

search_message.setContent("Hello am searching for antena to connect with");

this.userLocationX = Math.random()*100;
this.userLocationY = Math.random()*100;

search_message.setX(this.userLocationX);
search_message.setY(this.userLocationY);


 channel.basicPublish(ExchangeName,"",null,objectMapper.writeValueAsBytes(search_message));
 System.out.println(" user with id " + id + "Sent Broadcast: " + search_message.getContent());

 // listening for the replies form the antennas and select the best one based on the distance of the repy 

boolean connected = false;
while(!connected){
       
 
    Thread.sleep(2000);
    if(repliesReceived.isEmpty()){
        
        System.out.println("No antenna is in range for user" + id);
        continue; }
         Message close_antenna = null;
        double minDistance = Double.MAX_VALUE;
        for(Message reply : repliesReceived.values())
            if(reply.getDistance() < minDistance){
                minDistance = reply.getDistance();
                close_antenna = reply;
            }
     
     if( close_antenna != null){
        this.currentAntennaId= close_antenna.getSenderId();
        Message connection_message = new Message(Message.MessageType.Connection, this.id, "Please register me to  your antenna");
        connection_message.setDestinationAntennaId(this.currentAntennaId);
        channel.basicPublish("connection" ,"",null,objectMapper.writeValueAsBytes(connection_message));
    connected = true; 
    }
    else {
        repliesReceived.clear();
    }
    
}
}
    



    public void sendSMS(String to , String text) throws Exception{
       if(currentAntennaId == null){
        System.err.println("user is not connected so cant send SMS");
       return;}

        Message smsMessage = new Message(Message.MessageType.SMS,this.id,text);
        smsMessage.setReciverId(to);
        String antennaQueue = "antenna_queue" + currentAntennaId;
        channel.basicPublish("",antennaQueue,null,objectMapper.writeValueAsBytes(smsMessage));
   
    objectMapper.writeValueAsBytes(smsMessage);
    System.out.println("SMS sent to " + to + " via Antenna " + currentAntennaId );
    
    
    }

    }
//     public void startInbox(){
//     }
// }