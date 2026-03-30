import utils.antenna_properties;
import read_network.read_network;

import java.util.List;

public class test_read_network {
    public static void main(String[] args) throws Exception {
        read_network network= new read_network("ressource/exemple_1.net");

        List<antenna_properties> antennas_list= network.get_antennas();
        System.out.println("network 1 :\n");
        for(antenna_properties antenna_property:antennas_list){
            System.out.println(antenna_property);
        }
        System.out.println("\n------------------------------------------------------\nnetwork 2 :\n");
        network= new read_network("ressource/exemple_2.net");

        antennas_list= network.get_antennas();

        for(antenna_properties antenna_property:antennas_list){
            System.out.println(antenna_property);
        }

        network= new read_network("ressource/exemple_3.net");

        antennas_list= network.get_antennas();
        System.out.println("\n------------------------------------------------------\nnetwork 3 :\n");
        for(antenna_properties antenna_property:antennas_list){
            System.out.println(antenna_property);
        }

    }
}
