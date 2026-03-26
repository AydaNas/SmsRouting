import utils.antenna_properties;

import java.util.Collections;
import java.util.List;

public class test_read_network {
    public static void main(String[] args) throws Exception {
        read_network network= new read_network("ressource/exemple_1.net");

        List<antenna_properties> antennas_list= network.get_antennas();

        for(antenna_properties antenna_property:antennas_list){
            System.out.println(antenna_property);
        }

    }
}
