import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import utils.DebugFile;
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
        if(DebugFile.is_debug()){
            System.out.println("Debug : Init antenna ");
        }
        List<antenna_properties> antennas =  network.get_antennas();
        routing_table = network.create_routing_table(properties);
    }

    public void start() {
        if(DebugFile.is_debug()){
            System.out.println("Debug : Start antenna and launch factory ");
        }
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try(Connection connection = factory.newConnection()) {

            if(DebugFile.is_debug()){
                System.out.println("Debug : starting queue and creation of channel");
            }

            Channel channel = connection.createChannel();

            String antennaQueueName = "antenna_queue_"+properties.get_name();
            channel.queueDeclare(antennaQueueName,false,false,false,null);

            channel.exchangeDeclare("Search","fanout");
            channel.exchangeDeclare("Update","direct");
            channel.exchangeDeclare("Connection","direct");
            channel.queueBind(antennaQueueName,"Search","");
            channel.queueBind(antennaQueueName,"Connection", properties.get_name());
            channel.queueBind(antennaQueueName,"Update", properties.get_name());

            if(DebugFile.is_debug()){
                System.out.println("Debug : DeliverCallback initialisation");
            }
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                Message message = objectMapper.readValue(new String(delivery.getBody()), Message.class);

                switch (message.getType()){
                    case Search:
                        double distance = Math.sqrt(Math.pow(message.getX() - properties.get_position_x() ,2) + Math.pow(message.getY() - properties.get_position_y(),2));
                        if(DebugFile.is_visible_rabbit_mq()){
                            System.out.println("This is Antenna "+properties.get_name()+" receive from "+message.getSenderId());
                        }
                        if(distance <= properties.get_radius()){

                            Message reply = new Message(Message.MessageType.Search_reply, properties.get_name(), "Greeting from Antenna " + properties.get_name() + " is here in your range ");
                            reply.setDistance(distance);

                            System.out.println("antenna : "+ properties.get_name() +" reply to : "+reply.getSenderId());
                            channel.basicPublish("","user_queue_" + message.getSenderId(),null, objectMapper.writeValueAsBytes(reply));
                        }
                        break;
                    case Connection:
                        if(DebugFile.is_visible_rabbit_mq()){
                            System.out.println("Antenna "+properties.get_name()+" Connection request receive from "+message.getSenderId());
                        }

                        if(message.getMulti_purpose_AntennaId().equals(properties.get_name())){

                            table_user_position.put(message.getSenderId(),message.getMulti_purpose_AntennaId());
                            if(DebugFile.is_debug()){
                                System.out.println("Debug : table_user_position : "+table_user_position.toString());
                            }

                            Message update = new Message(Message.MessageType.Update_Location,message.getSenderId(),properties.get_name(),message.getMulti_purpose_AntennaId());

                            for(String dest :properties.get_connected()){

                                channel.basicPublish("Update",dest,null,objectMapper.writeValueAsBytes(update));
                            }
                            if(DebugFile.is_visible_rabbit_mq()){
                                System.out.println("Antenna : "+properties.get_name()+" update "+message.getSenderId()+" position to "+properties.get_connected().toString()+" Antennas.");
                            }

                        }
                        break;
                    case Update_Location:
                        if(DebugFile.is_visible_rabbit_mq()){
                            System.out.println("Received Update_Location message from : "+message.getMulti_purpose_AntennaId());
                        }

                        if(!table_user_position.containsKey(message.getSenderId())|| table_user_position.get(message.getSenderId()).equals(properties.get_name())){
                            table_user_position.put(message.getSenderId(),message.getMulti_purpose_AntennaId());
                            if(DebugFile.is_debug()){
                                System.out.println("Debug : table_user_position : "+table_user_position.toString());
                            }

                            for(String dest :properties.get_connected()){
                                channel.basicPublish("Update",dest,null,objectMapper.writeValueAsBytes(message));
                            }
                            if(DebugFile.is_visible_rabbit_mq()){
                                System.out.println("Antenna : "+properties.get_name()+" update "+message.getSenderId()+" position to "+properties.get_connected().toString()+" Antennas.");
                            }
                        }
                        break;
                    case SMS:
                        String receiver = message.getReciverId();
                        String antenna_dest = table_user_position.get(receiver);
                        if(antenna_dest==null){
                            //Save message
                        } else if(Objects.equals(antenna_dest,properties.get_name())){
                            if(DebugFile.is_visible_rabbit_mq()){
                                System.out.println("Antenna "+ properties.get_name()+" Send : "+message.getContent()+" to "+ receiver );
                            }

                            message.setMulti_purpose_AntennaId(properties.get_name());
                            channel.basicPublish("","user_queue_" + receiver,null,objectMapper.writeValueAsBytes(message));

                        }else{
                            String destination =table_user_position.get(receiver);

                            if(destination!= null){
                                String neighbor = routing_table.get(destination);
                                if(DebugFile.is_visible_rabbit_mq()){
                                    System.out.println("Antenna : "+properties.get_name()+" send to "+ neighbor + " antenna ,for the user " + receiver);
                                }

                                channel.basicPublish("","antenna_queue_" + neighbor,null,objectMapper.writeValueAsBytes(message));
                            }
                            else {
                                System.err.println("Antenna"+properties.get_name()+"does not have routing info for user"+ receiver);
                            }
                        }
                        break;
                }
            };
            if(DebugFile.is_debug()){
                System.out.println("Debug : channel purge and basicConsume launch");
            }
            channel.queuePurge(antennaQueueName);
            channel.basicConsume(antennaQueueName,false,deliverCallback,consumerTag -> {});
            if(DebugFile.is_debug()){
                System.out.println("Debug : antenna A launch");
            }
            Thread.currentThread().join();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
