package org.uga.distributed;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;


public class Antenna {
private String id;
private double antennaLocationX;
private double antennaLocationY;
private double coverageRadius = 50.0;
private Map<String,String> globaluserRegistry = new HashMap<>();
private final ObjectMapper objectMapper = new ObjectMapper();

public void antennaInit(String id, double x, double y){
    this.id = id;
    this.antennaLocationX = x;
    this.antennaLocationY = y;
}

public void startAntenna() throws Exception {

    User user = new User();
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    String antennaQueueName = "antenna_queue" + id;
    channel.queueDeclare(antennaQueueName,false,false,false,null);
    
    channel.exchangeDeclare("Search","fanout");
    channel.exchangeDeclare("Connection","fanout");
    channel.queueBind(antennaQueueName,"Search","");
    channel.queueBind(antennaQueueName,"Connection","");
    System.out.println("Antenna"+id+"is running...");


    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
    Message message = objectMapper.readValue(delivery.getBody(),Message.class);


switch(message.getType()){
    case Search:
        double distance = Math.sqrt(Math.pow(message.getX() - antennaLocationX ,2) + Math.pow(message.getY() -antennaLocationY,2));
        if(distance <= coverageRadius){
       
         System.out.println("Greetings .. This is Antenna"+id+"replying, user please forward your SMS am listening!");
         Message reply = new Message(Message.MessageType.Search_reply, id, "Greeting from Antenna" + id + " is here in your range ");
         reply.setDistance(distance);
  
        channel.basicPublish("","user_queue" + message.getSenderId(),null,
        objectMapper.writeValueAsBytes(reply));
        }
     break;

        case Connection:
            if(message.getDestinationAntennaId().equals(this.id)){
            globaluserRegistry.put(message.getSenderId(),this.id);
            Message update = new Message(Message.MessageType.Update_Location,this.id,message.getSenderId());

            channel.basicPublish("Connection","",null,objectMapper.writeValueAsBytes(update));
         System.out.println("Antenna" + id + "registers user" + message.getSenderId()); 
            
        }           
            break;

     case Update_Location:
          globaluserRegistry.put(message.content, message.getSenderId());
          
           break;

       case SMS: 
       String receiver = message.getReciverId();
            if(Objects.equals(globaluserRegistry.get(receiver), this.id)){

            System.out.println("antenna"+id+"delivers SMS to Localuser"+ receiver + "content: " + message.getContent());
          channel.basicPublish("","user_queue" + receiver,null,objectMapper.writeValueAsBytes(message));

        }
        else {

            String destination =globaluserRegistry.get(receiver);
            
            if(destination!= null){
              String neighbor = RoutingTable.getNextAntnnaHop(this.id, destination); 
              System.out.println("Antenna"+id+"forwards SMS to Antenna"+ neighbor + "for user" + receiver);
            channel.basicPublish("","antenna_queue" + neighbor,null,objectMapper.writeValueAsBytes(message));
            }
            else {
                System.err.println("Antenna"+id+"does not have routing info for user"+ receiver);
            } }
            
            break;
        }
       
};
channel.basicConsume(antennaQueueName, true, deliverCallback, consumerTag -> {});
}


}