package org.uga.distributed;
import com.rabbitmq.client.*;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    if(incomming_message.getType()== Message.MessageType.Discovery_reply){
 repliesReceived.put(incomming_message.getSenderId(),incomming_message);
 System.out.println("User"+ id+ " recives singal from Antenna" + incomming_message.getSenderId());

    } 
    else if (incomming_message.getType() == Message.MessageType.SMS){
        System.out.println("SMS from " + incomming_message.getSenderId() + ""+ incomming_message.getContent());


    } };
    channel.basicConsume(userQueueName, true, deliverCallback, consumerTag -> {});   



// setting up discovery message to be brodcasted to all antennas 
String ExchangeName = "discovery";
channel.exchangeDeclare(ExchangeName,"fanout");
Message discovery_message = new Message();



this.userLocationX = Math.random()*100;
this.userLocationY = Math.random()*100;

discovery_message.setX(this.userLocationX);
discovery_message.setY(this.userLocationY);


 channel.basicPublish(ExchangeName,"",null,objectMapper.writeValueAsBytes(discovery_message));
 System.out.println(" user with id " + id + "Sent Broadcast: " + discovery_message.getContent());

 // listening for the replies form the antennas and select the best one based on the distance of the repy 

Thread.sleep(2000);
if(!repliesReceived.isEmpty()){
    Message close_antenna = repliesReceived.values().stream().min(Comparator.comparingDouble(Message::getDistance)).get();
    this.currentAntennaId = close_antenna.getSenderId();
    Message registration_message = new Message(Message.MessageType.Registration,this.id,"Please register me");
    registration_message.setDestinationAntennaId(this.currentAntennaId);
    channel.basicPublish("registration","",null,objectMapper.writeValueAsBytes(registration_message));
    System.out.println(" user with id " + id + "Sent Registration Request to Antenna: " + currentAntennaId);
    
    }
    else {
        System.err.println(" user with id " + id + "all antennas are out of range, try moving to different locaiton");
         
    }

    
}


//  DeliverCallback deliverCallback = (consumerTag, delivery) -> {
//      Message reply_antenna = objectMapper.readValue(delivery.getBody(),Message.class);

//      if(reply_antenna.getType()==Message.MessageType.Discovery_reply){
//          System.out.println("user" + id + "received reply from antenna with id" + reply_antenna.id() + "with distance of " +reply_antenna.getDistance() );
//      }


//  channel.basicConsume(userQueueName,true,deliverCallback);
// };

//     public void Movement_discovery(double nex_locationX, double new_locationY){
//         this.new_locationX = new_locationX;
//         this.new_locationY = new_locationY;

//         //send disvoery message to all antnnas broadcast 
//         broadcastToAntennas(new_locationX,new_locationY);
//         AntennaReply best = findSmallestDistance(repliesReceived);
//         sendJoinRequest(best.antennaId);
//         this.currentAntennaId = best.antennaId;
//     }

    public void sendSMS(String to , String text) throws Exception{
       if(currentAntennaId == null){
        System.err.println("user is not connected so cant send SMS");
       return;}

        Message smsMessage = new Message(Message.MessageType.SMS,this.id,text);
        smsMessage.setReciverId(to);
        String antennaQueue = "antenna_queue\"" + currentAntennaId;
        channel.basicPublish("",antennaQueue,null,objectMapper.writeValueAsBytes(smsMessage));
   
    objectMapper.writeValueAsBytes(smsMessage);
    System.out.println("SMS sent to " + to + " via Antenna " + currentAntennaId );
    
    
    }

    }
//     public void startInbox(){
//     }
// }