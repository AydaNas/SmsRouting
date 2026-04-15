package read_network;

import utils.antenna_properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;


public class read_network {

    Map<String, antenna_properties> antenna_map = new HashMap<>();

    public HashMap<String,String> create_routing_table(antenna_properties id_properties){
        HashMap<String,String> routing_table = new HashMap<>();

        Queue<antenna_properties> to_visit =new LinkedList<>();
        HashSet<String> visited = new HashSet<>();

        to_visit.add(id_properties);

        while(!to_visit.isEmpty()){

            antenna_properties antenna = to_visit.poll();
            String name=antenna.get_name();

            for(String connected_name :antenna.get_connected()){
                if (!visited.contains(connected_name)) {  // cohérent maintenant
                    visited.add(connected_name);           // marquer dès l'ajout
                    if (name.equals(id_properties.get_name())) {
                        routing_table.put(connected_name, connected_name);
                    } else {
                        routing_table.put(connected_name, routing_table.get(name));
                    }

                    to_visit.add(antenna_map.get(connected_name));
                }
            }
        }
        routing_table.put(id_properties.get_name(), id_properties.get_name());
        return routing_table;
    }

    public read_network(String args) throws Exception{
        File network_file = new File(args);

        String line;
        int nb_antenna;
        int nb_conexion;


        try (Scanner input = new Scanner(network_file)){

            // skip comment
            do {
                line = input.nextLine();
            } while (line.startsWith("#"));

            //get number of antenna
            nb_antenna = Integer.parseInt(line);


            int i=0;

            if(nb_antenna==0){return;}
            //get the name and location of a antenna
            line = input.nextLine();
            while(i<nb_antenna){
                if (!line.startsWith("#")) {
                    String[] part = line.split(" ");
                    if(part.length!=4){
                        throw new Exception("Invalid number of elements for antenna property request: name x y ");
                    }
                    double x =  Double.parseDouble(part[1]);
                    double y =  Double.parseDouble(part[2]);
                    double radius = Double.parseDouble(part[3]);

                    antenna_map.put(part[0],new antenna_properties(part[0],x,y,radius));

                    i++;
                }
                if(!input.hasNextLine()){
                    return;
                }
                line = input.nextLine();
            }

            //skip comment
            while (line.startsWith("#")){
                if(input.hasNextLine()){
                    line = input.nextLine();
                }else{
                    return;
                }

            }

            //get nb connexion
            nb_conexion = Integer.parseInt(line);

            i=0;

            //get connexion from antenna to antenna
            if(nb_conexion==0){return;}
            line = input.nextLine();
            while(i<nb_conexion){
                if (!line.startsWith("#")) {
                    String[] part = line.split(" ");
                    if(part.length!=2){
                        throw new Exception("Invalid number of elements for antenna property request: Name_antenna_From Name_antenna_to ");
                    }
                    if(!antenna_map.containsKey(part[0])){
                        throw new NullPointerException();
                    }

                    if(!antenna_map.containsKey(part[1])){
                        throw new NullPointerException();
                    }
                    antenna_map.get(part[0]).add_connected(part[1]);
                    antenna_map.get(part[1]).add_connected(part[0]);
                    i++;
                }
                if(!input.hasNextLine()){
                    return;
                }
                line = input.nextLine();
            }

        } catch (FileNotFoundException e){
            System.out.println("Error: "+ args +" not found");

        }
    }

    public List<antenna_properties> get_antennas(){
        return new ArrayList<>(antenna_map.values());
    }
}