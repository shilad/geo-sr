package org.wikibrain.geosr;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.distance.GeodeticDistanceMetric;
import org.wikibrain.spatial.distance.GraphDistanceMetric;
import org.wikibrain.utils.WpIOUtils;

import java.io.*;
import java.util.*;

/**
 * @author Shilad Sen
 */
public class ExportEnhancer {
    private final Env env;

    private Map<String, Integer> stringIdMap;
    private Map<Integer,String> idsStringMap;

    private Map<Integer,Integer> idToIndexForDistanceMatrix;
    private Map<Integer,Integer> idToIndexForSRMatrix;
    private Map<Integer,Integer> idToIndexForGraphMatrix;
    private Map<Integer, Integer> idToScaleCategory;

    private Map<String, Geometry> cityGeometries;

    private float[][] distanceMatrix;
    private float[][] srMatrix;
    private float[][] graphMatrix;
    private Map<String, Set<Integer>> neighbors;
    private Map<String, String> oldNeighbors;
    private Map<Integer, Integer> pageRanks;
    private List<String> locations;


    public ExportEnhancer(Env env) throws IOException {
        this.env = env;
        buildIdsStringMap();
        readMatrices();
        readInIdToScaleInfo();
        readNeighbors();
        fixNeighbors();
        readPopularity();
    }

    private void readPopularity() throws IOException {
        pageRanks = new HashMap<Integer, Integer>();
        int rank = 1;
        for (String line : FileUtils.readLines(new File("dat/PageHitListFullEnglish.txt"))) {
            String tokens[] = line.trim().split("\t", -1);
            pageRanks.put(Integer.valueOf(tokens[0]), rank++);
        }
    }

    private void readMatrices() {
        MatrixGenerator.MatrixWithHeader distanceMatrixWithHeader = MatrixGenerator.loadMatrixFile("dat/distancematrix_en");
        distanceMatrix = distanceMatrixWithHeader.matrix;
        idToIndexForDistanceMatrix= distanceMatrixWithHeader.idToIndex;

        MatrixGenerator.MatrixWithHeader srMatrixWithHeader = MatrixGenerator.loadMatrixFile("dat/srmatrix_en");
        srMatrix= srMatrixWithHeader.matrix;
        idToIndexForSRMatrix= srMatrixWithHeader.idToIndex;

        MatrixGenerator.MatrixWithHeader graphMatrixWithHeader = MatrixGenerator.loadMatrixFile("dat/graphmatrix_en");
        graphMatrix= graphMatrixWithHeader.matrix;
        idToIndexForGraphMatrix= graphMatrixWithHeader.idToIndex;
    }

    private void buildIdsStringMap() throws FileNotFoundException {
        stringIdMap = new HashMap<String, Integer>();
        idsStringMap= new HashMap<Integer, String>();
        File file = new File("dat/IDsToTitles.txt");
        Scanner scanner = new Scanner(file);
        while(scanner.hasNextLine()){
            String next= scanner.nextLine();
            java.util.StringTokenizer st= new java.util.StringTokenizer(next,"\t",false);
            int id= Integer.parseInt(st.nextToken());
            String name= st.nextToken();
            idsStringMap.put(id, name);
            stringIdMap.put(name, id);
        }
        scanner.close();
    }


    private void readInIdToScaleInfo() throws FileNotFoundException{
        idToScaleCategory= new HashMap<Integer, Integer>();
        Scanner scanner= new Scanner(new File("dat/geometryToScale.txt"));
        for (int i = 0; i <7 ; i++) { //Throw out the first 7 lines because they are information
            scanner.nextLine();
        }
        while(scanner.hasNextLine()){
            String s= scanner.nextLine();
            String[] info= s.split("\t", -1);
            idToScaleCategory.put(Integer.parseInt(info[0]),Integer.parseInt(info[1]));
        }
    }

    public Map<String, Set<Integer>> readNeighbors() throws FileNotFoundException {
        Scanner scan = new Scanner(new File("dat/citiesToNeighbors4.txt"));
        this.neighbors = new HashMap<String, Set<Integer>>();
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            String[] array = line.split("\t");
            Set<Integer> set = new HashSet<Integer>();
            String id = array[0];
            double population = Double.parseDouble(array[1]);
            for (int i = 2; i < array.length; i++) {
                set.add(Integer.parseInt(array[i]));
            }
            neighbors.put(id, set);
        }
        return neighbors;
    }

    public void fixNeighbors() throws FileNotFoundException {
        cityGeometries = new HashMap<String, Geometry>();
        Map<Integer,String>[] cities = new Map[2];
        String[] files = {"dat/cities1000.txt","dat/cities1000.old.txt"};
        int newCities = 0, oldCities = 1;
        // country matching
        String[] array2 = {"United States of America", "Canada", "Brazil", "Pakistan", "India", "France", "Spain", "United Kingdom", "Australia"};
        String[] countryCodes = {"US", "CA", "BR", "PK", "IN", "FR", "ES", "GB", "AU"};
        // codes to country names
        Map<String, String> countryNames = new HashMap<String, String>();
        for (int i = 0; i < countryCodes.length; i++) {
            countryNames.put(countryCodes[i], array2[i]);
        }
        // set of codes
        Set<String> countries = new HashSet<String>();
        countries.addAll(Arrays.asList(countryCodes));
        // read in file that gives conversion from state code names to state actual names
        Map<String, String> stateCodesToNames = new HashMap<String, String>();
        Scanner scan = new Scanner(new File("dat/admin1CodesASCII.txt"));
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            String[] array = line.split("\t");
            stateCodesToNames.put(array[0], array[1]);
        }
        scan.close();
        // read city names
        for (int i=0; i<2; i++){
            scan = new Scanner(new File(files[i]));
            cities[i] = new HashMap<Integer, String>();
            while(scan.hasNextLine()) {
                String line = scan.nextLine();
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
                    Point geo = new Point(coordArraySeq, new GeometryFactory(new PrecisionModel(), 4326));


                    Integer id = Integer.parseInt(array[0]);

                    // store the information
                    state = stateCodesToNames.get(country + "." + state);
                    country = countryNames.get(country);
                    name = country+","+state+","+name;

                    cityGeometries.put(name, geo);
                    cities[i].put(id,name);
                }
            }
            scan.close();
        }
        oldNeighbors = new HashMap<String, String>();
        for (Integer id: cities[oldCities].keySet()){
            oldNeighbors.put(cities[oldCities].get(id),cities[newCities].get(id));
        }

    }

    public void enhance(File personFile, File questionFile, File familiarityFile, File newQuestionFile) throws IOException, ConfigurationException, DaoException {
        Map<String, Set<Integer>> knownLocations = readKnownLocations(personFile);
        BufferedReader reader = WpIOUtils.openBufferedReader(questionFile);
        String header[] = reader.readLine().trim().split("\t", -1);

        BufferedWriter writer = WpIOUtils.openWriter(newQuestionFile);
        writeRow(writer, header, "km", "graph", "sr", "wpId1", "wpId2", "scale1", "scale2", "pop1", "pop2", "known1", "known2");

        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            try {
                enhanceLine(header, knownLocations, line, writer);
            }catch (Exception e) {
                e.printStackTrace();
                System.err.println("error enhancing line: " + line.trim());
            }
        }
        writer.close();
        reader.close();

        writeFamiliarity(personFile, questionFile, familiarityFile);
    }

    private void writeFamiliarity(File personFile, File questionFile, File familiarityFile) throws IOException, ConfigurationException, DaoException {
        SpatialDataDao dataDao = env.getConfigurator().get(SpatialDataDao.class);
        GeodeticDistanceMetric geoMetric = new GeodeticDistanceMetric(dataDao);
        TIntSet validIds = new TIntHashSet();
        validIds.addAll(stringIdMap.values());
        geoMetric.setValidConcepts(validIds);
        geoMetric.enableCache(true);
        GraphDistanceMetric graphMetric = new GraphDistanceMetric(dataDao, geoMetric);
        graphMetric.setValidConcepts(validIds);
        graphMetric.enableCache(true);

        Map<String, List<Geometry>> homes = getLivedInGeometries(personFile);
        BufferedReader reader = WpIOUtils.openBufferedReader(questionFile);
        String header[] = reader.readLine().trim().split("\t", -1);

        // Find useful column values
        int workerCol = -1;
        int locationCols[] = new int[2];
        int familiarityCols[] = new int[2];
        int valenceCols[] = new int[2];

        for (int i = 0; i < header.length; i++) {
            if (header[i].equals("amazonId")) {
                workerCol = i;
            } else if (header[i].equals("location1")) {
                locationCols[0] = i;
            } else if (header[i].equals("location2")) {
                locationCols[1] = i;
            } else if (header[i].equals("familiarity1")) {
                familiarityCols[0] = i;
            } else if (header[i].equals("familiarity2")) {
                familiarityCols[1] = i;
            } else if (header[i].equals("valence1")) {
                valenceCols[0] = i;
            } else if (header[i].equals("valence2")) {
                valenceCols[1] = i;
            }
        }

        Set<String> written = new HashSet<String>();
        Writer writer = WpIOUtils.openWriter(familiarityFile);
        writer.write("worker\tlocation\tlocationId\tfamiliarity\tvalence\tview-rank\tkms\tgraph\n");
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }

            if (line.endsWith("\n")) { line = line.substring(0, line.length() - 1); }
            String [] tokens = line.split("\t", -1);
            String workerId = tokens[workerCol].trim();
            if (!homes.containsKey(workerId) || homes.get(workerId).isEmpty()) {
                System.err.println("No home geometries for worker " + workerId);
                continue;
            }
            for (int i = 0; i < 2; i++) {
                String location = tokens[locationCols[i]];
                String familiarity = tokens[familiarityCols[i]];
                String valence = tokens[valenceCols[i]];
                int conceptId = stringIdMap.get(location);

                if (written.contains(workerId + "@" + conceptId)) {
                    continue;
                }
                written.add(workerId + "@" + conceptId);

                Geometry locationGeo = dataDao.getGeometry(conceptId, "wikidata");
                double minKms = Double.POSITIVE_INFINITY;
                double minGraph = Double.POSITIVE_INFINITY;
                for (Geometry geo : homes.get(workerId)) {
                    minKms = Math.min(minKms, geoMetric.distance(geo, locationGeo) / 1000.0);
                    minGraph = Math.min(minKms, graphMetric.distance(geo, locationGeo));
                }
                Object [] output = new Object[] {
                        workerId,
                        location,
                        conceptId,
                        familiarity,
                        valence,
                        pageRanks.get(conceptId),
                        minKms,
                        minGraph,
                };
                for (Object o : output) {
                    if (o instanceof  Double && ((Double)o == Double.POSITIVE_INFINITY || (Double)o == Double.MAX_VALUE)) {
                        o = "inf";
                    }
                    if (o != output[0]) {
                        writer.write('\t');
                    }
                    writer.write(o.toString());
                }
                writer.write('\n');
            }
        }
        writer.close();


    }

    private void enhanceLine(String[] header, Map<String, Set<Integer>> knownLocations, String line, BufferedWriter writer) throws IOException {
        // Find useful column values
        int workerCol = -1;
        int location1Col = -1;
        int location2Col = -1;
        for (int i = 0; i < header.length; i++) {
            if (header[i].equals("amazonId")) {
                workerCol = i;
            } else if (header[i].equals("location1")) {
                location1Col = i;
            } else if (header[i].equals("location2")) {
                location2Col = i;
            }
        }

        if (line.endsWith("\n")) { line = line.substring(0, line.length() - 1); }
        String [] tokens = line.split("\t", -1);

        // If you don't trim, bad things happen
        String workerId = tokens[workerCol].trim();
        int wpId1 = -1, wpId2 = -1, pageRank1 = -1, pageRank2 = -1, scale1 = -1, scale2 = -1;
        double graphDist = -1.0, kmDist = -1.0, srDist = -1.0;

        try {

            wpId1 = stringIdMap.get(tokens[location1Col]);
            wpId2 = stringIdMap.get(tokens[location2Col]);

            scale1 = idToScaleCategory.get(wpId1);
            scale2 = idToScaleCategory.get(wpId2);
            pageRank1 = pageRanks.get(wpId1);
            pageRank2 = pageRanks.get(wpId2);

            graphDist = graphMatrix[idToIndexForGraphMatrix.get(wpId1)][idToIndexForGraphMatrix.get(wpId2)];
            kmDist = distanceMatrix[idToIndexForDistanceMatrix.get(wpId1)][idToIndexForDistanceMatrix.get(wpId2)];
            srDist = srMatrix[idToIndexForSRMatrix.get(wpId1)][idToIndexForSRMatrix.get(wpId2)];

        } catch (Exception e) {
            System.err.println("didn't find information for line " + line.trim());
        }

        writeRow(writer, tokens,
                kmDist, graphDist, srDist,
                wpId1, wpId2,
                scale1, scale2,
                pageRank1, pageRank2,
                knownLocations.get(workerId).contains(wpId1),
                knownLocations.get(workerId).contains(wpId2)
        );
    }

    private void writeRow(BufferedWriter writer, String [] originalRow, Object ... newCols) throws IOException {
        for (int i = 0; i < originalRow.length; i++) {
            if (i > 0) {
                writer.write("\t");
            }
            writer.write(originalRow[i]);
        }
        for (Object o : newCols) {
            writer.write("\t" + o.toString());
        }
        writer.write("\n");
    }

    public Map<String, List<Geometry>> getLivedInGeometries(File personFile) throws IOException {
        Map<String, List<Geometry>> result = new HashMap<String, List<Geometry>>();
        Map<String, Set<String>> livedInLocations = readLivedInLocations(personFile);
        for (String personId : livedInLocations.keySet()) {
            List<Geometry> homes = new ArrayList<Geometry>();
            for (String place : livedInLocations.get(personId)) {
                if (cityGeometries.containsKey(place)) {
                    homes.add(cityGeometries.get(place));
                } else {
                    System.err.println("Unknown lived in location '" + place + "' for turker " + personId);
                }
            }
            result.put(personId, homes);
        }
        return result;
    }

    public Map<String, Set<String>> readLivedInLocations(File personFile) throws IOException {
        Map<String, Set<String>> result = new HashMap<String, Set<String>>();

        int numFields = -1;
        int idCol = -1;
        TIntList locationCols = new TIntArrayList();

        locations = new ArrayList<String>();
        for (String line : FileUtils.readLines(personFile)) {
            if (line.endsWith("\n")) {
                line = line.substring(0, line.length()-1);
            }
            String tokens[] = line.split("\t", -1);

            // Grab location columns for header
            if (numFields < 0) {
                numFields = tokens.length;
                for (int i = 0;i < tokens.length; i++) {
                    if (tokens[i].startsWith("home_")) {
                        locationCols.add(i);
                    }
                    if (tokens[i].equals("amazonId")) {
                        idCol = i;
                    }
                }
                continue;
            }

            if (tokens.length != numFields) {
                System.err.println("invalid line: '" + line + "'");
                continue;
            }


            if (line.contains("A18FSRHD10KF0Y")){
                System.out.println(line);
            }

            String id = tokens[idCol].trim();
            Set<String> known = new HashSet<String>();
            for (int i : locationCols.toArray()) {
                String s = tokens[i].trim();
                if (s.length() == 0) {
                    continue;
                }
                if (!s.contains("|")) {
                    continue;   // country
                }
                known.add(s.replaceAll("\\|", ","));
            }

            result.put(id, known);
        }

        return result;
    }
    public Map<String, Set<Integer>> readKnownLocations(File personFile) throws IOException {
        Map<String, Set<Integer>> knownLocations = new HashMap<String, Set<Integer>>();
        Map<String, Set<String>> livedIn = readLivedInLocations(personFile);
        for (String personId : livedIn.keySet()) {
            Set<Integer> known = new HashSet<Integer>();
            for (String placeId : livedIn.get(personId)) {
                if (neighbors.containsKey(placeId)) {
                    locations.add(placeId);
                    known.addAll(neighbors.get(placeId));
                } else {
                    if (oldNeighbors.get(placeId) != null) {
                        locations.add(oldNeighbors.get(placeId));
                        known.addAll(neighbors.get(oldNeighbors.get(placeId)));
                    } else {
                        System.err.println("unknown city "+placeId+" for turker: " + personId);
                    }
                }
            }
            knownLocations.put(personId, known);
        }
        return knownLocations;
    }

    public static void main(String args[]) throws Exception {
        Env env = EnvBuilder.envFromArgs(args);
        ExportEnhancer enhancer = new ExportEnhancer(env);
        enhancer.enhance(
                new File("dat/people.tsv"),
                new File("dat/questions.tsv"),
                new File("dat/familiarity.tsv"),
                new File("dat/questions.enhanced.tsv")
        );
    }

    public List<String> getLocations(){
        return locations;
    }
}
