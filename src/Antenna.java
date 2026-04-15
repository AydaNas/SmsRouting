import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import utils.Message;
import utils.antenna_properties;
import read_network.read_network;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public class Antenna {

    antenna_properties properties;
    private Map<String,String> table_user_position = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    HashMap<String,String> routing_table;


    Antenna(String id, double x, double y,double radius) {
        properties = new antenna_properties(id,x,y,radius);
    }

    Antenna(antenna_properties properties) {
        this.properties = properties;
    }

    public void init(read_network network) {
        List<antenna_properties> antennas =  network.get_antennas();
        routing_table = network.create_routing_table(properties);
    }

    public void start() {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try(Connection connection = factory.newConnection()) {
            Channel channel = connection.createChannel();

            String antennaQueueName = "antenna_queue_"+properties.get_name();
            channel.queueDeclare(antennaQueueName,false,false,false,null);

            channel.exchangeDeclare("Search","fanout");
            channel.exchangeDeclare("Update","direct");
            channel.exchangeDeclare("Connection","direct");
            channel.queueBind(antennaQueueName,"Search","");
            channel.queueBind(antennaQueueName,"Connection", properties.get_name());
            channel.queueBind(antennaQueueName,"Update", properties.get_name());
            System.out.println("Antenna"+properties.get_name()+"is running...");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                Message message = objectMapper.readValue(new String(delivery.getBody()), Message.class);

                switch (message.getType()){
                    case Search:
                        double distance = Math.sqrt(Math.pow(message.getX() - properties.get_position_x() ,2) + Math.pow(message.getY() - properties.get_position_y(),2));
                        System.out.println("This is Antenna "+properties.get_name()+" receive from "+message.getSenderId());
                        if(distance <= properties.get_radius()){

                            Message reply = new Message(Message.MessageType.Search_reply, properties.get_name(), "Greeting from Antenna " + properties.get_name() + " is here in your range ");
                            reply.setDistance(distance);

                            System.out.println("reply senderId : "+reply.getSenderId());
                            channel.basicPublish("","user_queue_" + message.getSenderId(),null, objectMapper.writeValueAsBytes(reply));
                        }
                        break;
                    case Connection:
                        System.out.println("Antenna "+properties.get_name()+" Connection received from "+message.getSenderId()+" Antenna "+message.getDestinationAntennaId());
                        if(message.getDestinationAntennaId().equals(properties.get_name())){

                            table_user_position.put(message.getSenderId(),message.getDestinationAntennaId());
                            System.out.println("table_user_position : "+table_user_position.toString());
                            Message update = new Message(Message.MessageType.Update_Location,message.getSenderId(),properties.get_name(),message.getDestinationAntennaId());
                            System.out.println("Antenna "+properties.get_name()+" Connection received from "+update.getSenderId()+" Antenna "+update.getDestinationAntennaId());
                            for(String dest :properties.get_connected()){
                                channel.basicPublish("Update",dest,null,objectMapper.writeValueAsBytes(update));
                            }

                        }
                        break;
                    case Update_Location:
                        System.out.println("received Update_Location message from "+message.getSenderId());
                        if(!table_user_position.containsKey(message.getSenderId())|| table_user_position.get(message.getSenderId()).equals(properties.get_name())){
                            table_user_position.put(message.getSenderId(),message.getDestinationAntennaId());
                            System.out.println("table_user_position : "+table_user_position.toString());
                            for(String dest :properties.get_connected()){
                                channel.basicPublish("Update",dest,null,objectMapper.writeValueAsBytes(message));
                            }
                        }
                        break;
                    case SMS:
                        String receiver = message.getReciverId();
                        if(Objects.equals(table_user_position.get(receiver),properties.get_name())){
                            System.out.println("antenna"+properties.get_name()+"delivers SMS to Localuser"+ receiver + "content: " + message.getContent());
                            channel.basicPublish("","user_queue_" + receiver,null,objectMapper.writeValueAsBytes(message));
                        }else{
                            String destination =table_user_position.get(receiver);

                            if(destination!= null){
                                String neighbor = routing_table.get(destination);
                                System.out.println("Antenna"+properties.get_name()+"forwards SMS to Antenna"+ neighbor + "for user" + receiver);
                                channel.basicPublish("","antenna_queue_" + neighbor,null,objectMapper.writeValueAsBytes(message));
                            }
                            else {
                                System.err.println("Antenna"+properties.get_name()+"does not have routing info for user"+ receiver);
                            }
                        }
                        break;
                }
            };
            channel.queuePurge(antennaQueueName);
            channel.basicConsume(antennaQueueName,false,deliverCallback,consumerTag -> {});

            Thread.currentThread().join();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
