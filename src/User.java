
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import utils.Message;
import utils.utils_class;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class User {
    private String id;
    private double userLocationX;
    private double userLocationY;
    private String currentAntennaId;
    private double new_locationX;
    private double new_locationY;
    private Map<String, Message> repliesReceived = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();
    private Channel channel;
    private Scanner sc = new Scanner(System.in);

    User(String id,double userLocationX,double userLocationY){
        this.id = id;
        this.userLocationX = userLocationX;
        this.userLocationY = userLocationY;
    }

    public void User_start(){
        boolean quit = false;
        String tmp;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try(Connection connection = factory.newConnection();){
            this.channel = connection.createChannel();
            String queue_name = "user_queue_"+id;
            channel.queueDeclare(queue_name,false,false,false,null);
            String ExchangeName = "Search";
            channel.exchangeDeclare(ExchangeName,"fanout");

            DeliverCallback deliverCallback = (consumerTag,delivery) -> {
                Message incomingMessage = objectMapper.readValue(delivery.getBody(), Message.class);
                if(incomingMessage.getType()==Message.MessageType.Search_reply){
                    repliesReceived.put(incomingMessage.getSenderId(),incomingMessage);
                    System.out.println("User"+ id+ " recives singal from Antenna " + incomingMessage.getSenderId());
                }else if(incomingMessage.getType()==Message.MessageType.SMS){
                    System.out.println("SMS from "+incomingMessage.getSenderId()+":"+incomingMessage.getContent());
                }
            };

            channel.basicConsume(queue_name,true,deliverCallback,consumerTag -> {});

            //connect_antenna
            connect_antenna();
            System.out.println("current_antenna : "+currentAntennaId);

            while(!quit){
                System.out.println("three choices (exit|send <Destination> <message>|move <ID_antenna>): ");
                tmp = sc.nextLine();
                if(!tmp.isEmpty()){
                    ArrayList<String> parameter = utils_class.user_itf_terminal_commande(tmp);
                    switch(parameter.getFirst()){
                        case "exit":
                            channel.close();
                            connection.close();
                            quit = true;
                            break;
                        case "send":
                            if(parameter.size()<3){
                                System.err.println("\"send\" key word as been recognise but it requires at least 2 parameters (the id destination and the message)\n write like : send <Destination> <message>");
                            }else if (parameter.size()>3){
                                System.err.println("too many parameters. extra parameter ignored");

                            }else {
                                sendSMS(parameter.get(1), parameter.get(2));
                            }
                            break;
                        case "move":
                            //todo
                            break;
                    }
                }
            }

        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    private void connect_antenna() throws IOException, InterruptedException {
        repliesReceived.clear();
        Message search_message = new Message(Message.MessageType.Search,id,"Hello am searching for antena to connect with",userLocationX,userLocationY);

        channel.basicPublish("Search","",null, objectMapper.writeValueAsBytes(search_message));
        System.out.println(" user with id " + id + "Sent Broadcast: " + search_message.getContent());
        // listening for the replies form the antennas and select the best one based on the distance of the repy
        boolean connected = false;
        while (!connected) {

            Thread.sleep(500);
            if(repliesReceived.isEmpty()){
                System.out.println("No antenna is in range for user" + id);
                continue;
            }
            Message close_antenna = null;
            double minDistance = Double.MAX_VALUE;
            for ( Message reply : repliesReceived.values()) {
                if(reply.getDistance() < minDistance){
                    minDistance = reply.getDistance();
                    close_antenna = reply;
                }
            }

            if(close_antenna != null){
                this.currentAntennaId = close_antenna.getSenderId();
                Message connection_message = new Message(Message.MessageType.Connection, this.id, "Please register me to  your antenna");
                connection_message.destinationAntennaId = currentAntennaId;
                channel.basicPublish("Connection",this.currentAntennaId,null, objectMapper.writeValueAsBytes(connection_message));
                connected = true;
            } else {
                repliesReceived.clear();
            }
        }
    }

    private void sendSMS(String to , String text) throws Exception{
        if(currentAntennaId == null){
            System.err.println("user is not connected so cant send SMS");
            return;}

        Message smsMessage = new Message(Message.MessageType.SMS,this.id,text);
        smsMessage.setReciverId(to);
        String antennaQueue = "antenna_queue_" + currentAntennaId;
        channel.basicPublish("",antennaQueue,null,objectMapper.writeValueAsBytes(smsMessage));

        objectMapper.writeValueAsBytes(smsMessage);
        System.out.println("SMS sent to " + to + " via Antenna " + currentAntennaId );


    }
}
