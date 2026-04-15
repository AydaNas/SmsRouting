import read_network.read_network;
import utils.antenna_properties;

import java.util.List;

/**
 * @author ensimag
 */

public class SMSRouting {

    static debug debug_level ;
    static role_enum role ;
    String network;

    public enum debug {
        none,
        debug
    }

    public enum role_enum {
        antenna,
        user
    }

    public static void main(String[] args) throws Exception{
        if(args.length <2){
            System.err.println("Usage: java SMSRouting [none|debug] antenna network_file antenna_id \nor java SMSRouting [none|debug] user id x_position y_position");
            System.err.println("Usage: network_file and antenna_id is required if role = antenna and antenna_id need to be present in the network file");
            throw new  Exception("Invalid arguments");
        }

        switch (args[0].toLowerCase()){
            case "none":
                debug_level = debug.none;
                break;
            case "debug":
                debug_level = debug.debug;
                break;
            default:
                throw new Exception("Unknown debug level");
        }

        switch (args[1].toLowerCase()){
            case "antenna":
                role = role_enum.antenna;
                break;
            case "user":
                role = role_enum.user;
                break;
            default:
                throw new Exception("Unknown role");
        }

        if(role==role_enum.antenna){

            String name_file;
            String antenna_id;

            if(args.length != 4){
                System.err.println("Usage: java SMSRouting [none|debug] antenna network_file antenna_id \nor java SMSRouting [none|debug] user id x_position y_position");
                System.err.println("Usage: network_file and antenna_id is required if role = antenna and antenna_id need to be present in the network file");
                throw new  Exception("Invalid arguments");
            }
            name_file = args[2];
            antenna_id = args[3];

            System.out.println(name_file);
            read_network network=new read_network(name_file);
            List<antenna_properties> antennas =  network.get_antennas();
            antenna_properties id_properties_tmp = null;


            for (antenna_properties antenna : antennas) {
                if (antenna.get_name().equals(antenna_id)) {
                    id_properties_tmp = antenna;
                }

            }

            if(id_properties_tmp==null){
                throw new NullPointerException("Antenna "+antenna_id+" not found");
            }

            Antenna antenna = new Antenna(id_properties_tmp);
            try {
                antenna.init(network);
                antenna.start();
            }catch (Exception e){
                System.err.println("Erreur : "+e.getMessage());
            }

        }else {

            if(args.length != 5){
                System.err.println("Usage: java SMSRouting [none|debug] antenna network_file antenna_id \nor java SMSRouting [none|debug] user id x_position y_position");
                System.err.println("Usage: network_file and antenna_id is required if role = antenna and antenna_id need to be present in the network file");
                throw new  Exception("Invalid arguments");
            }
            String user_id = args[2];
            double x_position = Double.parseDouble(args[3]);
            double y_position = Double.parseDouble(args[4]);

            User user = new User(user_id,x_position,y_position);
            user.User_start();

        }
    }

}
