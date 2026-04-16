package utils;

public class DebugFile {

    static debug debug_level ;

    public enum debug {
        none,
        visible_rabbit_mq,
        debug
    }

    public DebugFile(debug debug_level){
        this.debug_level = debug_level;
    }

    public static boolean is_debug(){
        return debug_level == debug.debug;
    }

    public static boolean is_visible_rabbit_mq(){
        return debug_level == debug.debug || debug_level== debug.visible_rabbit_mq;
    }

}
