package org.wikibrain.geosr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // page -> metric -> distance
    public Map<PageInfo, Map<String, Double>> distances = new HashMap<PageInfo, Map<String, Double>>();

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

    public synchronized void addDistance(PageInfo p, String metric, double distance) {
        if (!distances.containsKey(p)) {
            distances.put(p, new HashMap<String, Double>());
        }
        distances.get(p).put(metric, distance);
    }
}
