package srsurvey

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.geom.Point
import com.vividsolutions.jts.geom.PrecisionModel
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence
import edu.macalester.acs.AutocompleteEntry
import edu.macalester.acs.AutocompleteTree
import grails.transaction.Transactional

@Transactional
class CityService {

    private Map<String, Double> cityPopulations;
    private Map<String, Geometry> cityGeometries;

    private Map<String, AutocompleteTree<String, City>> acMap = new HashMap<String, AutocompleteTree<String, City>>();

    public void init() {
        parseGazetteer()
    }

    public List<String> autocomplete(String country, String query) {
        def results = []
        for (AutocompleteEntry<String, City> entry : acMap.get(country).autocomplete(query, 10)) {
            City c = entry.getValue()
            results.add([c.name + ", " + c.state, c.name + "|" + c.state + "|" + c.country + "|" + c.geo.x + "|" + c.geo.y]);
        }
        return results
    }

    /**
     * This method parses the GeoNames Gazetteer Data to get city populations and locations.
     * (http://download.geonames.org/export/dump/)
     * It puts these in the fields cityPopulations and cityGeometries.
     *
     * @throws IOException
     */
    public void parseGazetteer() throws IOException {
        cityPopulations = new HashMap<String, Double>();
        cityGeometries = new HashMap<String, Geometry>();
        // all cities with population 1K+
        Scanner scanner = new Scanner(new File("dat/cities1000.txt"));

        // country matching
        String[] array2 = ["United States of America", "Canada", "Brazil", "Pakistan", "India", "France", "Spain", "United Kingdom", "Australia"]
        String[] countryCodes = ["US", "CA", "BR", "PK", "IN", "FR", "ES", "GB", "AU"]

        for (String c : array2) {
            acMap.put(c, new AutocompleteTree<String, City>());
        }

        // codes to country names
        Map<String, String> countryNames = new HashMap<String, String>();
        for (int i = 0; i < countryCodes.length; i++) {
            countryNames.put(countryCodes[i], array2[i]);
        }
        // set of codes
        Set<String> countries = new HashSet<String>();
        countries.addAll(Arrays.asList(countryCodes));

        // list of cities
        List<City> cityList = new ArrayList<City>();

        // read cities file
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] array = line.split("\t");
            String country = array[8];

            // if it's in a country we care about
            if (countries.contains(country)) {
                String name = array[1];
                String state = array[10];
                Double lat = Double.parseDouble(array[4]);
                Double longitude = Double.parseDouble(array[5]);
                Integer pop = Integer.parseInt(array[14]);

                // convert lat and long to a geometry
                Coordinate[] coords = new Coordinate[1];
                coords[0] = new Coordinate(longitude, lat);
                CoordinateArraySequence coordArraySeq = new CoordinateArraySequence(coords);
                Point current = new Point(coordArraySeq, new GeometryFactory(new PrecisionModel(), 4326));

                // store the information
                City city = new City(name, current, pop, country, country, state);
                cityList.add(city);
            }
        }

        // read in file that gives conversion from state code names to state actual names
        Map<String, String> stateCodesToNames = new HashMap<String, String>();
        Scanner scan = new Scanner(new File("dat/admin1CodesASCII.txt"));
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            String[] array = line.split("\t");
            stateCodesToNames.put(array[0], array[1]);
        }

        // convert state and country names from codes
        for (City city : cityList) {
            city.state = stateCodesToNames.get(city.country + "." + city.state);
            city.country = countryNames.get(city.country);
        }

        // store the information in cityGeometries and cityPopulations

//        File f = new File("dat/cities.tsv")
//        Writer w = f.newWriter("UTF-8")

        double totalPop = 0.0
        for (City city : cityList) {
            cityGeometries.put(city.toString(), city.geo);
            cityPopulations.put(city.toString(), (double) city.pop);
            totalPop += city.pop
            acMap.get(city.country).add(city.toString(), city, city.pop)
//            w.write("${city.country}\t${city.pop}\t${city.state}\t${city.name}\n")
        }
//        w.close()


        println("read ${cityGeometries.size()} cities with total population ${totalPop}")
    }

    // inner class to keep city information temporarily together
    private static class City {
        private String name;
        private Point geo;
        private Integer pop;
        private String country;
        private String countryCode;
        private String state;

        public City(String name, Point geo, Integer pop, String country, String countryCode, String state) {
            this.name = name;
            this.geo = geo;
            this.pop = pop;
            this.country = country;
            this.countryCode = countryCode;
            this.state = state;
        }

        public String toString() {
            return name + "," + state;
        }
    }
}
