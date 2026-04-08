import Erreur.Unrecognized_backslash;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import Erreur.MissingQuotationMark;
import utils.antenna_properties;
import utils.State;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.rabbitmq.client.ConnectionFactory;
import read_network.read_network;
import utils.utils_class;

import static utils.State.State_enum.S_ACK_RECEIVE;

public class user {


    public static boolean antenna_id_correct(String antenna_id, List<antenna_properties> antenna_property_list){
        for(antenna_properties antenna_property:antenna_property_list){
            if(antenna_property.name_boolean(antenna_id)){
                return true;
            }
        }
        return false;
    }

    public static void main (String[] args) throws Exception {
        if(args.length!=1){
            throw new IllegalArgumentException("Wrong number of arguments");
        }


        final State receive_ack = new utils.State();
        final utils_class utils_tool = new utils.utils_class();

        String tel="";
        String antenna="";
        //intialisation
        System.out.println("Please enter the telephone number");
        Scanner sc = new Scanner(System.in);
        tel = sc.nextLine();

        //linked to the closest antenna
        read_network network= new read_network(args[0]);

        List<antenna_properties> antennas_list= network.get_antennas();
        boolean antenna_selected = false;
        while(!antenna_selected){
            System.out.println("List of available antennas:");
            for(antenna_properties antenna_property:antennas_list){
                System.out.println(antenna_property);
            }
            System.out.println("Please enter the antenna id");
            antenna = sc.nextLine();
            if(antenna_id_correct(antenna,antennas_list)){
                antenna_selected=true;
            }else{
                System.err.println("Invalid antenna id");
            }
        }


        //rabbitmq init
        ConnectionFactory factory = new ConnectionFactory();
        try(Connection connection = factory.newConnection();){

            //channel init
            Channel channel_in = connection.createChannel();
            Channel channel_out = connection.createChannel();
            channel_in.queueDeclare(tel,false,false,false,null);

            //
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println(" received "+message);
                if(message.matches("<User_send_message,+[a-zA-Z0-9 ]+,+[a-zA-Z0-9 ]+,+[a-zA-Z0-9 ]+>")){
                    int start_index = message.indexOf(",");
                    int end_index = message.indexOf(">");
                    String[] parameter =  message.substring(start_index + 1, end_index).split(",");
                    System.out.println(parameter[0]+":"+parameter[2]);
                }
                if(message.equals("Antenna_Ack")){
                    receive_ack.set_state(S_ACK_RECEIVE);

                }
            };


            String tmp;
            boolean quite = false;
            String message = "<User_connected,"+tel+">";
            channel_in.basicConsume(tel,true,deliverCallback,(consumerTag, delivery) -> {});
            channel_out.basicPublish("",antenna,null,message.getBytes());
            while (!receive_ack.get_state().equals(S_ACK_RECEIVE)) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("Antenna acknowledged you are now connected");
            // user action
            while(!quite){
                System.out.println("three choices (exit|send <Destination> <message>|move <ID_antenna>): ");
                tmp = sc.nextLine();
                try {
                    ArrayList<String> parameter = utils_class.user_itf_terminal_commande(tmp);
                    switch(parameter.getFirst()){
                        case "exit":
                            quite=true;
                            channel_in.close();
                            message = "<User_disconnected,"+tel+">";
                            channel_out.basicPublish("",antenna,null,message.getBytes());
                            break;
                        case "send":
                            if(parameter.size()<3){
                                System.err.println("\"send\" key word as been recognise but it requires at least 2 parameters (the id destination and the message)\n write like : send <Destination> <message>");
                            }else {
                                if (parameter.size()>3){
                                    System.err.println("too many parameters. extra parameter ignored");
                                }
                                message = "<User_send_message," + tel + "," + parameter.get(1) + "," + parameter.get(2) + ">";
                                channel_out.basicPublish("", antenna, null, message.getBytes());
                            }
                            break;
                        case "move":
                            if(parameter.size()==2){
                                tmp = parameter.get(1);
                                if(antenna_id_correct(tmp,antennas_list)){
                                    message = "<User_move,"+tel+">";
                                    channel_out.basicPublish("",antenna,null,message.getBytes());
                                    channel_out.close();
                                    antenna = tmp;
                                    channel_out = connection.createChannel();
                                    message = "<User_connected,"+tel+">";
                                    channel_out.basicPublish("",antenna,null,message.getBytes());
                                }else{
                                    System.err.println("antenna "+parameter.get(1)+" is not a valid antenna");
                                }
                            }else {
                                System.err.println("not enough parameters. requires at least 1 parameter (Id message)");
                            }
                            break;
                        default:
                            System.out.println("Invalid commande");
                    }

                }catch (MissingQuotationMark e){
                    System.out.println("Missing quotation mark");
                }catch (Unrecognized_backslash e){
                    System.out.println("Unrecognized backslash");
                }
            }
            System.out.println("quiting");


        }catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


}