package utils;

import java.awt.*;
import java.util.ArrayList;

public class antenna_properties {
    String name;
    double x;
    double y;
    double radius;

    ArrayList<String> connected;

    public antenna_properties(){
        name="";
        x=0.0;
        y=0.0;
        connected=new ArrayList<>();
        radius=50.0;
    }

    public antenna_properties(String name){
        this.name=name;
        x=0.0;
        y=0.0;
        connected=new ArrayList<>();
        radius=50.0;
    }
    public antenna_properties(String name, double x, double y,double radius){
        this.name=name;
        this.x=x;
        this.y=y;
        this.radius=radius;
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

    public double get_position_x(){
        return x;
    }

    public double get_position_y(){
        return y;
    }

    public double get_radius(){
        return radius;
    }

    public  ArrayList<String> get_connected(){
        return connected;
    }

    public boolean name_boolean(String name){
        return this.name.equals(name);
    }

    @Override
    public String toString(){
        return name+" ("+x+","+y+") of radius "+radius+"\nconnexions: "+connected;
    }

    @Override
    public int hashCode(){
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof antenna_properties)) return false;
        antenna_properties  that = (antenna_properties) o;
        return name.equals(that.name) && x == that.x && y == that.y;

    }
}
