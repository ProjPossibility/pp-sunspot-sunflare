/*
 * Gesture.java
 *
 * Created on March 30, 2008, 2:18 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.sunspotworld.demo;

import java.util.Vector;
/**
 *
 * @author Winnie
 */
public class Gesture {
    private Vector dataset;
    private Vector nodes;
    private Vector pattern;
    private String inactiveAxis;
    private double endTimeStamp;
    
    /** Creates a new instance of Gesture */
    public Gesture() {
        dataset = new Vector();
        nodes = new Vector();
        pattern = new Vector();
        inactiveAxis = "";
        endTimeStamp = 0;
    }
    public Gesture(Vector d){
        dataset = d;
    }
    public Gesture(Vector d, double ts){
        dataset = d;
        endTimeStamp = ts;
    }
    public void setEndTimeStamp(double ts){
        endTimeStamp = ts;
    }
    public void setPattern(Vector p){
        pattern = new Vector(p);
    }
    public void setData(Vector d){
        dataset = new Vector(d);
    }
    public void appendData(Vector d){
        dataset.addAll(d);
    }
    public void combine(Gesture g){
         dataset.addAll(g.getDataset());
         pattern.addAll(g.getPattern());
         endTimeStamp = g.getEndTimeStamp();
    }
    public double getEndTimeStamp(){
        return endTimeStamp;
    }
    public Vector getPattern(){
        return pattern;
    }
    public Vector getDataset(){
        return dataset;
    }
    public void setInactiveAxis(String s){
        inactiveAxis = new String(s);
    }
    public String getInactiveAxis(){
        return inactiveAxis;
    }
    public String toString(){
        StringBuilder result = new StringBuilder();
        result.append("pattern size = "+pattern.size()+ " ");
        for(int i=0; i<pattern.size(); i++){
            result.append(pattern.elementAt(i) + " ");
        }
        return result.toString();
    }
}
