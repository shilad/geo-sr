package org.wikibrain.geosr;

import java.util.Set;

/**
 * @author Shilad Sen
 */
public class Person {
    public int grailsId;
    public String amazonId;
    public String gender;
    public String education;
    public boolean complete;
    public int numSr;
    public int numFamiliarity;
    public int numValence;
    public Set<City> cities;        // Homes that are specific to cities
    public Set<Country> countries;  // Homes that are unknown cities

    @Override
    public String toString() {
        return "Person{" +
                "grailsId=" + grailsId +
                ", amazonId='" + amazonId + '\'' +
                ", gender='" + gender + '\'' +
                ", education='" + education + '\'' +
                ", complete=" + complete +
                ", numSr=" + numSr +
                ", numFamiliarity=" + numFamiliarity +
                ", numValence=" + numValence +
                ", countries=" + countries +
                ", cities=" + cities +
                '}';
    }
}
