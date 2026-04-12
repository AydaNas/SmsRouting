import java.util.*;

public class Config {
    //create ring and user map
    public static final Map<String, String> nextMap = new HashMap<>();
    public static final Map<String, Set<String>> userMap = new HashMap<>(); // use set

    static {
        // ring
        nextMap.put("A", "B");
        nextMap.put("B", "C");
        nextMap.put("C", "A");

        // users
        userMap.put("A", new HashSet<>(Set.of("U1", "U2")));
        userMap.put("B", new HashSet<>(Set.of("U3", "U4")));
        userMap.put("C", new HashSet<>(Set.of("U5", "U6")));
    }
}
