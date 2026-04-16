
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import utils.DebugFile;
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
    private String newAntennaId;
    private Map<String, Message> repliesReceived = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();
    private Channel channel;
    private Scanner sc = new Scanner(System.in);

    User(String id,double userLocationX,double userLocationY,DebugFile debugFile){
        this.id = id;
        this.userLocationX = userLocationX;
        this.userLocationY = userLocationY;
    }

    public void User_start(){
        boolean quit = false;
        String tmp;

        if(DebugFile.is_debug()){
            System.out.println("Debug : launch factory");
        }
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try(Connection connection = factory.newConnection();){
            if(DebugFile.is_debug()){
                System.out.println("Debug : starting queue and creation of channel");
            }
            this.channel = connection.createChannel();
            String queue_name = "user_queue_"+id;
            channel.queueDeclare(queue_name,false,false,false,null);
            String ExchangeName = "Search";
            channel.exchangeDeclare(ExchangeName,"fanout");
            if(DebugFile.is_debug()){
                System.out.println("Debug : DeliverCallback initialisation and basicConsume launch");
            }
            DeliverCallback deliverCallback = (consumerTag,delivery) -> {
                Message incomingMessage = objectMapper.readValue(delivery.getBody(), Message.class);
                if(incomingMessage.getType()==Message.MessageType.Search_reply){
                    repliesReceived.put(incomingMessage.getSenderId(),incomingMessage);
                    if(DebugFile.is_visible_rabbit_mq()) {
                        System.out.println("User" + id + " receives signal from Antenna " + incomingMessage.getSenderId());
                    }
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(),false);
                }else if(incomingMessage.getType()==Message.MessageType.SMS){

                    if(currentAntennaId!=null && currentAntennaId.equals(incomingMessage.getMulti_purpose_AntennaId())){
                        System.out.println("SMS from "+incomingMessage.getSenderId()+":"+incomingMessage.getContent());
                    }
                }
            };

            channel.basicConsume(queue_name,false,deliverCallback,consumerTag -> {});

            //connect_antenna
            connect_antenna();
            if(DebugFile.is_debug()){
                System.out.println("Debug : current_antenna : "+currentAntennaId);
            }
            while(!quit){
                System.out.println("three choices (exit|send <Destination> <message>|move <x_position> <y_position>): ");
                tmp = sc.nextLine();
                if(!tmp.isEmpty()){
                    ArrayList<String> parameter = utils_class.user_itf_terminal_commande(tmp);
                    switch(parameter.getFirst()){
                        case "exit":
                            channel.close();
                            quit = true;
                            break;
                        case "send":
                            if(currentAntennaId==null){
                                System.out.println("Out of range");
                            }else{
                                if(parameter.size()<3){
                                    System.err.println("\"send\" key word as been recognise but it requires at least 2 parameters (the id destination and the message)\n write like : send <Destination> <message>");
                                }else if (parameter.size()>3){
                                    System.err.println("too many parameters. extra parameter ignored");

                                }else {
                                    sendSMS(parameter.get(1), parameter.get(2));
                                }
                            }
                            break;
                        case "move":
                            if(parameter.size()!=3){
                                System.err.println("Need 2 parameters the position_x and y_position in the form move <x_position> <y_position>");
                            }
                            this.userLocationX = Double.parseDouble(parameter.get(1));
                            this.userLocationY = Double.parseDouble(parameter.get(2));
                            connect_antenna();
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

        if(DebugFile.is_visible_rabbit_mq()){
            System.out.println("User with id " + id + ",Sent Broadcast to all antennas");
        }
        channel.basicPublish("Search","",null, objectMapper.writeValueAsBytes(search_message));

        // listening for the replies form the antennas and select the best one based on the distance of the repy
        boolean connected = false;

        // add try var to eject if fail after 5 time
        int attempts_remaining = 5;
        while (!connected && attempts_remaining>0) {

            Thread.sleep(500);
            if(repliesReceived.isEmpty()){
                attempts_remaining--;
            }else{
                Message close_antenna = null;
                double minDistance = Double.MAX_VALUE;
                for ( Message reply : repliesReceived.values()) {
                    if(reply.getDistance() < minDistance){
                        minDistance = reply.getDistance();
                        close_antenna = reply;
                    }
                }

                if(close_antenna != null){
                    this.newAntennaId = close_antenna.getSenderId();
                    if(!newAntennaId.equals(currentAntennaId)) {
                        currentAntennaId=newAntennaId;
                        Message connection_message = new Message(Message.MessageType.Connection, this.id, "Please register me to  your antenna");
                        connection_message.multi_purpose_AntennaId = currentAntennaId;
                        channel.basicPublish("Connection", this.currentAntennaId, null, objectMapper.writeValueAsBytes(connection_message));
                    }
                    connected = true;
                }else{

                    currentAntennaId = null;
                }
            }
        }
        if(attempts_remaining==0){
            System.out.println("No antenna is in range for user" + id);
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
        if(DebugFile.is_visible_rabbit_mq()){
            System.out.println("SMS sent to " + to + " via Antenna " + currentAntennaId );
        }

    }
}
