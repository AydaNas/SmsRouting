import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import utils.antenna_properties;
import read_network.read_network;

import java.util.*;
import java.util.concurrent.TimeoutException;

public class antenna {

    public static void main(String[] args) throws Exception{
        if(args.length!=2){
            throw new IllegalArgumentException("Wrong number of arguments");
        }

        read_network network=new read_network(args[0]);
        List<antenna_properties> antennas =  network.get_antennas();
        antenna_properties id_properties_tmp = null;



        for (antenna_properties antenna : antennas) {
            if (antenna.get_name().equals(args[1])) {
                id_properties_tmp = antenna;
            }

        }

        if(id_properties_tmp==null){
            throw new NullPointerException("Antenna "+args[0]+" not found");
        }
        final antenna_properties id_properties = id_properties_tmp;

        HashMap<String,String> table_user_position = new HashMap<>();

        HashMap<String,String> routing_table = network.create_routing_table(id_properties);

        HashMap<String,Channel> table_antenna = new HashMap<>();

        HashMap<String,Channel> table_User = new HashMap<>();

        ConnectionFactory cf = new ConnectionFactory();
        try(Connection c = cf.newConnection()){
            Channel channel_in = c.createChannel();
            channel_in.queueDeclare(id_properties.get_name(),false,false,false,null);
            for(String name : id_properties.get_connected()){
                Channel channel_out = c.createChannel();
                table_antenna.put(name,channel_out);
            }

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {

                String message = new String(delivery.getBody());
                System.out.println(id_properties.get_name()+" received "+message);
                if(message.matches("<User_connected,+[a-zA-Z0-9]+>")){
                    System.out.println("User_connected rule start");
                    int start = message.indexOf(",")+1;
                    int end = message.indexOf(">");
                    String user_name = message.substring(start,end);
                    if(!table_User.containsKey(user_name)){
                        Channel user_in = c.createChannel();
                        table_User.put(user_name,user_in);
                        table_user_position.put(user_name, id_properties.get_name());
                        String new_message = "<Antenna_update_user,"+user_name+","+id_properties.get_name()+">" ;
                        for(String antenna : id_properties.get_connected()){
                            Channel tmp =table_antenna.get(antenna);
                            tmp.basicPublish("",antenna,null,new_message.getBytes());
                        }
                        new_message = "Antenna_Ack";
                        user_in.basicPublish("",user_name,null,new_message.getBytes());
                    }else{
                        String new_message = "Antenna_Ack";
                        Channel user_in = table_User.get(user_name);
                        user_in.basicPublish("",user_name,null,new_message.getBytes());
                    }
                }
                if(message.matches("<User_disconnected,+[a-zA-Z0-9]+>")){
                    int start = message.indexOf(",")+1;
                    int end = message.indexOf(">");
                    String user_name = message.substring(start,end);
                    if(table_User.containsKey(user_name)){
                        Channel tmp_channel = table_User.get(user_name);
                        table_User.remove(user_name);
                        table_user_position.remove(user_name, id_properties.get_name());
                        String new_message = "<Antenna_delete_user,"+user_name+">";
                        for(String antenna : id_properties.get_connected()){
                            Channel tmp = table_antenna.get(antenna);
                            tmp.basicPublish("",antenna,null,new_message.getBytes());
                        }
                    }
                }
                if(message.matches("<User_send_message,+[a-zA-Z0-9]+,+[a-zA-Z0-9]+,+[a-zA-Z0-9]+>")){
                    int start_index = message.indexOf(",");
                    int end_index = message.indexOf(">");
                    String[] parameter =  message.substring(start_index + 1, end_index).split(",");
                    String target_antenna = table_user_position.get(parameter[1]);
                    Channel out_channel ;
                    if(target_antenna ==  null){
                        //TODO store the message until further info
                        System.out.println("massage lost -> todo store until further info");
                    } else if (Objects.equals(target_antenna, id_properties.get_name())){
                        out_channel = table_User.get(parameter[1]);
                        out_channel.basicPublish("",parameter[1],null,message.getBytes());
                    }else{
                        String next_antenna = routing_table.get(target_antenna);
                        out_channel = table_antenna.get(next_antenna);
                        out_channel.basicPublish("",parameter[1],null,message.getBytes());
                    }

                }

                if(message.matches("<User_move,+[a-zA-Z0-9]+>")){
                    int start = message.indexOf(",")+1;
                    int end = message.indexOf(">");
                    String user_name = message.substring(start,end);
                    if(table_User.containsKey(user_name)){
                        table_user_position.put(user_name,null);
                        table_User.remove(user_name);
                    }
                }

                if(message.matches("<Antenna_update_user,+[a-zA-Z0-9]+,+[a-zA-Z0-9]+>")){
                    int start_index = message.indexOf(",");
                    int end_index = message.indexOf(">");
                    String[] parameter =  message.substring(start_index + 1, end_index).split(",");
                    if (!table_user_position.containsKey(parameter[0]) || !Objects.equals(parameter[1], table_user_position.get(parameter[0]))) {
                        table_user_position.put(parameter[0],parameter[1]);
                        for(String antenna : id_properties.get_connected()){
                            Channel out =  table_antenna.get(antenna);
                            out.basicPublish("",antenna,null,message.getBytes());
                        }
                    }
                }
                if(message.matches("<Antenna_delete_user,+[a-zA-Z0-9]+>")){
                    int start_index = message.indexOf(",");
                    int end_index = message.indexOf(">");
                    String user_name = message.substring(start_index,end_index);
                    if (table_user_position.containsKey(user_name)) {
                        table_user_position.remove(user_name );
                        for(String antenna : id_properties.get_connected()){
                            Channel tmp = table_antenna.get(antenna);
                            tmp.basicPublish("",antenna,null,message.getBytes());
                        }
                    }
                }
            };

            channel_in.basicConsume(id_properties.get_name(),true,deliverCallback,(consumerTag, delivery) -> {});
            System.out.println("Antenna up");
            Thread.currentThread().join();
        }

    }
}