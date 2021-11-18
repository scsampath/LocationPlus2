package edu.ucsb.ece150.locationplus;

/*
 * This class is provided as a way for you to store information about a single satellite. It can
 * be helpful if you would like to maintain the list of satellites using an ArrayList (i.e.
 * ArrayList<Satellite>). As in Homework 3, you can then use an Adapter to update the list easily.
 *
 * You are not required to implement this if you want to handle satellite information in using
 * another method.
 */
public class Satellite {

    // [TODO] Define private member variables
    int satelliteNum;
    double azimuth;
    double elevation;
    double carrierFrequency;
    double noiseDensity;
    int constellationName;
    int SVID;

    // [TODO] Write the constructor
    public Satellite(int satelliteNum, double azimuth, double elevation, double carrierFrequency, double noiseDensity, int constellationName, int SVID) {
        this.satelliteNum = satelliteNum;
        this.azimuth = azimuth;
        this.elevation = elevation;
        this.carrierFrequency = carrierFrequency;
        this.noiseDensity = noiseDensity;
        this.constellationName = constellationName;
        this.SVID = SVID;
    }

    // [TODO] Implement the toString() method. When the Adapter tries to assign names to items
    // in the ListView, it calls the toString() method of the objects in the ArrayList
    @Override
    public String toString() {
        return "Satellite " + satelliteNum;
    }
}
