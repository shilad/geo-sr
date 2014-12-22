package org.wikibrain.geosr;

import com.vividsolutions.jts.geom.*;
import org.wikibrain.spatial.util.WikiBrainSpatialUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class CityDb {
    private static Logger LOG = Logger.getLogger(CityDb.class.getName());

    private CountryDb countryDb = new CountryDb();
    private StateDb stateDb = new StateDb();
    private Map<String, City> cities = new HashMap<String, City>();

    public CityDb() throws FileNotFoundException {
        this(
                new File("dat/cities1000.txt"),
                new File("dat/cities1000.old.txt")
        );
    }

    /**
     * @param cityPaths
     */
    public CityDb(File... cityPaths) throws FileNotFoundException {
        for (File f : cityPaths) {
            readCities(f);
        }
        LOG.info("matched " + cities.size() + " cities");
    }

    private void readCities(File f) throws FileNotFoundException {
        Scanner scan = new Scanner(f);
        while(scan.hasNextLine()) {
            String line = scan.nextLine();
            String[] array = line.split("\t");
            String countryCode = array[8];

            // if it's in a country we care about
            if (countryDb.isInteresting(countryCode)) {
                Country country = countryDb.getByCode(countryCode);
                String name = array[1];
                State state;
                try {
                    state = stateDb.getByCode(country, array[10]);
                } catch (IllegalArgumentException e) {
                    System.err.println("invalid line: " + line);
                    continue;
                }

                Double lat = Double.parseDouble(array[4]);
                Double longitude = Double.parseDouble(array[5]);
                Integer pop = Integer.parseInt(array[14]);
                Point location = WikiBrainSpatialUtils.getPoint(lat, longitude);
                Integer id = Integer.parseInt(array[0]);
                City city = new City(name, state, location, pop);

                String key = country.getName() + "," + state.getName() + "," + name;

                cities.put(key, city);
            }
        }
        scan.close();
    }

    public City getByName(String countryName, String stateName, String cityName) {
        String key = countryName + "," + stateName + "," + cityName;
        return cities.get(key);
    }

    public static void main(String args[]) throws FileNotFoundException {
        CityDb g = new CityDb();

    }
}
