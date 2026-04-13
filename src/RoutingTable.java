package org.uga.distributed;
import java.util.HashMap;
import java.util.Map;

public class RoutingTable {
    private static final Map<String,String> ring_toplogy = new HashMap<>();
static {
    ring_toplogy.put("Antenna1","Antenna2");
     ring_toplogy.put("Antenna2","Antenna3");
        ring_toplogy.put("Antenna3","Antenna4");
      ring_toplogy.put("Antenna4","Antenna5");
       ring_toplogy.put("Antenna5","Antenna6");
       ring_toplogy.put("Antenna6","Antenna7");
        ring_toplogy.put("Antenna7","Antenna1");
}

public static String getNextAntnnaHop(String currentAntennaId, String finalDestinationAntennaId){
    return ring_toplogy.get(currentAntennaId);
}
}