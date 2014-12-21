//package org.wikibrain.geosr;
//
//import com.vividsolutions.jts.geom.*;
//import gnu.trove.list.TIntList;
//import gnu.trove.list.array.TIntArrayList;
//import gnu.trove.set.TIntSet;
//import gnu.trove.set.hash.TIntHashSet;
//import org.apache.commons.io.FileUtils;
//import org.wikibrain.conf.ConfigurationException;
//import org.wikibrain.core.cmd.Env;
//import org.wikibrain.core.cmd.EnvBuilder;
//import org.wikibrain.core.dao.DaoException;
//import org.wikibrain.spatial.dao.SpatialDataDao;
//import org.wikibrain.spatial.distance.GeodeticDistanceMetric;
//import org.wikibrain.spatial.distance.GraphDistanceMetric;
//import org.wikibrain.utils.WpIOUtils;
//
//import java.io.*;
//import java.util.*;
//
///**
// * @author Shilad Sen
// */
//public class ExportFamiliarity {
//    private final Env env;
//
//    private CityDb cityGeometries;
//    private PageInfoDb pages;
//
//
//    public ExportFamiliarity(Env env) throws IOException {
//        this.env = env;
//        cityGeometries = new CityDb();
//        pages = new PageInfoDb();
//    }
//
//    public void enhance(File personFile, File questionFile, File familiarityFile, File newQuestionFile) throws IOException, ConfigurationException, DaoException {
//        Map<String, Set<Integer>> knownLocations = readKnownLocations(personFile);
//        BufferedReader reader = WpIOUtils.openBufferedReader(questionFile);
//        String header[] = reader.readLine().trim().split("\t", -1);
//
//        BufferedWriter writer = WpIOUtils.openWriter(newQuestionFile);
//        writeRow(writer, header, "km", "graph", "sr", "wpId1", "wpId2", "scale1", "scale2", "pop1", "pop2", "known1", "known2");
//
//        while (true) {
//            String line = reader.readLine();
//            if (line == null) {
//                break;
//            }
//            try {
//                enhanceLine(header, knownLocations, line, writer);
//            }catch (Exception e) {
//                e.printStackTrace();
//                System.err.println("error enhancing line: " + line.trim());
//            }
//        }
//        writer.close();
//        reader.close();
//
//        writeFamiliarity(personFile, questionFile, familiarityFile);
//    }
//
//    private void writeFamiliarity(File personFile, File questionFile, File familiarityFile) throws IOException, ConfigurationException, DaoException {
//        SpatialDataDao dataDao = env.getConfigurator().get(SpatialDataDao.class);
//        GeodeticDistanceMetric geoMetric = new GeodeticDistanceMetric(dataDao);
//        TIntSet validIds = new TIntHashSet();
//        validIds.addAll(stringIdMap.values());
//        geoMetric.setValidConcepts(validIds);
//        geoMetric.enableCache(true);
//        GraphDistanceMetric graphMetric = new GraphDistanceMetric(dataDao, geoMetric);
//        graphMetric.setValidConcepts(validIds);
//        graphMetric.enableCache(true);
//
//        Map<String, List<Geometry>> homes = getLivedInGeometries(personFile);
//        BufferedReader reader = WpIOUtils.openBufferedReader(questionFile);
//        String header[] = reader.readLine().trim().split("\t", -1);
//
//        // Find useful column values
//        int workerCol = -1;
//        int locationCols[] = new int[2];
//        int familiarityCols[] = new int[2];
//        int valenceCols[] = new int[2];
//
//        for (int i = 0; i < header.length; i++) {
//            if (header[i].equals("amazonId")) {
//                workerCol = i;
//            } else if (header[i].equals("location1")) {
//                locationCols[0] = i;
//            } else if (header[i].equals("location2")) {
//                locationCols[1] = i;
//            } else if (header[i].equals("familiarity1")) {
//                familiarityCols[0] = i;
//            } else if (header[i].equals("familiarity2")) {
//                familiarityCols[1] = i;
//            } else if (header[i].equals("valence1")) {
//                valenceCols[0] = i;
//            } else if (header[i].equals("valence2")) {
//                valenceCols[1] = i;
//            }
//        }
//
//        Set<String> written = new HashSet<String>();
//        Writer writer = WpIOUtils.openWriter(familiarityFile);
//        writer.write("worker\tlocation\tlocationId\tfamiliarity\tvalence\tview-rank\tkms\tgraph\n");
//        while (true) {
//            String line = reader.readLine();
//            if (line == null) {
//                break;
//            }
//
//            if (line.endsWith("\n")) { line = line.substring(0, line.length() - 1); }
//            String [] tokens = line.split("\t", -1);
//            String workerId = tokens[workerCol].trim();
//            if (!homes.containsKey(workerId) || homes.get(workerId).isEmpty()) {
//                System.err.println("No home geometries for worker " + workerId);
//                continue;
//            }
//            for (int i = 0; i < 2; i++) {
//                String location = tokens[locationCols[i]];
//                String familiarity = tokens[familiarityCols[i]];
//                String valence = tokens[valenceCols[i]];
//                int conceptId = stringIdMap.get(location);
//
//                if (written.contains(workerId + "@" + conceptId)) {
//                    continue;
//                }
//                written.add(workerId + "@" + conceptId);
//
//                Geometry locationGeo = dataDao.getGeometry(conceptId, "wikidata");
//                double minKms = Double.POSITIVE_INFINITY;
//                double minGraph = Double.POSITIVE_INFINITY;
//                for (Geometry geo : homes.get(workerId)) {
//                    minKms = Math.min(minKms, geoMetric.distance(geo, locationGeo) / 1000.0);
//                    minGraph = Math.min(minKms, graphMetric.distance(geo, locationGeo));
//                }
//                Object [] output = new Object[] {
//                        workerId,
//                        location,
//                        conceptId,
//                        familiarity,
//                        valence,
//                        pageRanks.get(conceptId),
//                        minKms,
//                        minGraph,
//                };
//                for (Object o : output) {
//                    if (o instanceof  Double && ((Double)o == Double.POSITIVE_INFINITY || (Double)o == Double.MAX_VALUE)) {
//                        o = "inf";
//                    }
//                    if (o != output[0]) {
//                        writer.write('\t');
//                    }
//                    writer.write(o.toString());
//                }
//                writer.write('\n');
//            }
//        }
//        writer.close();
//
//
//    }
//
//    private void writeRow(BufferedWriter writer, String [] originalRow, Object ... newCols) throws IOException {
//        for (int i = 0; i < originalRow.length; i++) {
//            if (i > 0) {
//                writer.write("\t");
//            }
//            writer.write(originalRow[i]);
//        }
//        for (Object o : newCols) {
//            writer.write("\t" + o.toString());
//        }
//        writer.write("\n");
//    }
//
//    public Map<String, List<Geometry>> getLivedInGeometries(File personFile) throws IOException {
//        Map<String, List<Geometry>> result = new HashMap<String, List<Geometry>>();
//        Map<String, Set<String>> livedInLocations = readLivedInLocations(personFile);
//        for (String personId : livedInLocations.keySet()) {
//            List<Geometry> homes = new ArrayList<Geometry>();
//            for (String place : livedInLocations.get(personId)) {
//                if (cityGeometries.containsKey(place)) {
//                    homes.add(cityGeometries.get(place));
//                } else {
//                    System.err.println("Unknown lived in location '" + place + "' for turker " + personId);
//                }
//            }
//            result.put(personId, homes);
//        }
//        return result;
//    }
//
//    public Map<String, Set<String>> readLivedInLocations(File personFile) throws IOException {
//        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
//
//        int numFields = -1;
//        int idCol = -1;
//        TIntList locationCols = new TIntArrayList();
//
//        locations = new ArrayList<String>();
//        for (String line : FileUtils.readLines(personFile)) {
//            if (line.endsWith("\n")) {
//                line = line.substring(0, line.length()-1);
//            }
//            String tokens[] = line.split("\t", -1);
//
//            // Grab location columns for header
//            if (numFields < 0) {
//                numFields = tokens.length;
//                for (int i = 0;i < tokens.length; i++) {
//                    if (tokens[i].startsWith("home_")) {
//                        locationCols.add(i);
//                    }
//                    if (tokens[i].equals("amazonId")) {
//                        idCol = i;
//                    }
//                }
//                continue;
//            }
//
//            if (tokens.length != numFields) {
//                System.err.println("invalid line: '" + line + "'");
//                continue;
//            }
//
//
//            if (line.contains("A18FSRHD10KF0Y")){
//                System.out.println(line);
//            }
//
//            String id = tokens[idCol].trim();
//            Set<String> known = new HashSet<String>();
//            for (int i : locationCols.toArray()) {
//                String s = tokens[i].trim();
//                if (s.length() == 0) {
//                    continue;
//                }
//                if (!s.contains("|")) {
//                    continue;   // country
//                }
//                known.add(s.replaceAll("\\|", ","));
//            }
//
//            result.put(id, known);
//        }
//
//        return result;
//    }
//    public Map<String, Set<Integer>> readKnownLocations(File personFile) throws IOException {
//        Map<String, Set<Integer>> knownLocations = new HashMap<String, Set<Integer>>();
//        Map<String, Set<String>> livedIn = readLivedInLocations(personFile);
//        for (String personId : livedIn.keySet()) {
//            Set<Integer> known = new HashSet<Integer>();
//            for (String placeId : livedIn.get(personId)) {
//                if (neighbors.containsKey(placeId)) {
//                    locations.add(placeId);
//                    known.addAll(neighbors.get(placeId));
//                } else {
//                    if (oldNeighbors.get(placeId) != null) {
//                        locations.add(oldNeighbors.get(placeId));
//                        known.addAll(neighbors.get(oldNeighbors.get(placeId)));
//                    } else {
//                        System.err.println("unknown city "+placeId+" for turker: " + personId);
//                    }
//                }
//            }
//            knownLocations.put(personId, known);
//        }
//        return knownLocations;
//    }
//
//    public static void main(String args[]) throws Exception {
//        Env env = EnvBuilder.envFromArgs(args);
//        ExportFamiliarity enhancer = new ExportFamiliarity(env);
//        enhancer.enhance(
//                new File("dat/people.tsv"),
//                new File("dat/questions.tsv"),
//                new File("dat/familiarity.tsv"),
//                new File("dat/questions.enhanced.tsv")
//        );
//    }
//
//    public List<String> getLocations(){
//        return locations;
//    }
//}
