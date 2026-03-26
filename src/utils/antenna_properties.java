package utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.SequencedSet;

public class antenna_properties {
    String name;
    Point position;

    ArrayList<String> connected;

    public antenna_properties(){
        name="";
        position=new Point();
        connected=new ArrayList<>();
    }

    public antenna_properties(String name){
        this.name=name;
        position=new Point();
        connected=new ArrayList<>();
    }
    public antenna_properties(String name,int x,int y){
        this.name=name;
        position=new Point(x,y);
        connected=new ArrayList<>();
    }

    public void add_connected(String connected){
        this.connected.add(connected);
    }

    public boolean in_connected(String connected){
        return this.connected.contains(connected);
    }

    public String get_name(){
        return name;
    }

    public Point get_position(){
        return position;
    }

    public String toString(){
        return name+" ("+position.x+","+position.y+")\nconnexions: "+connected;
    }
}
