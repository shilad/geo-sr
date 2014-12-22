package org.wikibrain.geosr;

import gnu.trove.set.TIntSet;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.distance.*;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
* @author Shilad Sen
*/
public class ExportFamiliarity {
    private static final Logger LOG = Logger.getLogger(ExportFamiliarity.class.getName());

    // Basic components
    private final Env env;
    private SpatialDataDao dao;

    // Distance metrics
    private final Map<String, SpatialDistanceMetric> metrics = new LinkedHashMap<String, SpatialDistanceMetric>();

    // Person -> page -> metric -> distance
    private final Map<Person, Map<PageInfo, Map<String, Double>>> distances = new HashMap();

    private CityDb cityDb;
    private PageInfoDb pageDb;
    private PersonDb personDb;

    public ExportFamiliarity(Env env) throws IOException, ConfigurationException, DaoException {
        this.env = env;
        dao = env.getConfigurator().get(SpatialDataDao.class);
        cityDb = new CityDb();
        pageDb = new PageInfoDb();
        personDb = new PersonDb();
        TIntSet concepts = pageDb.getIds();

        SphericalDistanceMetric spherical = new SphericalDistanceMetric(dao);
        spherical.setValidConcepts(concepts);
        spherical.enableCache(true);
        metrics.put("spherical", spherical);
        metrics.put("geodetic", new GeodeticDistanceMetric(dao, spherical));
        metrics.put("countries", new BorderingDistanceMetric(dao, "country"));
        metrics.put("states", new BorderingDistanceMetric(dao, "state"));
        metrics.put("graph", new GraphDistanceMetric(dao));

        for (SpatialDistanceMetric m : metrics.values()) {
            if (m != spherical) {
                LOG.info("enabling cache for " + m.getName());
                m.setValidConcepts(concepts);
                m.enableCache(true);
            }
        }
//        ordinal = new OrdinalDistanceMetric(dao);


        addPersonDistances();
    }

    public void addPersonDistances() {
        Set<City> cities = new HashSet<City>();
        for (Person p : personDb.getPeople()) {
            cities.addAll(p.cities);
        }
        final Map<City, Map<PageInfo, Map<String, Double>>> distances = new HashMap<City, Map<PageInfo, Map<String, Double>>>();
        ParallelForEach.iterate(cities.iterator(), new Procedure<City>() {
            @Override
            public void call(City city) throws Exception {
                Map<PageInfo, Map<String, Double>> cityDists = new HashMap<PageInfo, Map<String, Double>>();
                for (String m : metrics.keySet()) {
                    long before = System.currentTimeMillis();
                    for (SpatialDistanceMetric.Neighbor n : metrics.get(m).getNeighbors(city.getLocation(), 1000)) {
                        PageInfo pi = pageDb.getById(n.conceptId);
                        if (pi == null) {
                            System.err.println("metric " + m + " reported a null page");
                        } else {
                            if (!cityDists.containsKey(pi)) {
                                cityDists.put(pi, new HashMap<String, Double>());
                            }
                            cityDists.get(pi).put(m, n.distance);
                        }
                    }
                    long after = System.currentTimeMillis();
                }
                System.err.println("doing " + city + ": " + cityDists.size());
                synchronized (distances) {
                    distances.put(city, cityDists);
                }
            }
        });
    }

    private void writeFamiliarity(File questionFile, File familiarityFile) throws IOException, ConfigurationException, DaoException {
        ResponseReader rr = new ResponseReader(pageDb, personDb);
        List<Response> responses = rr.read(questionFile);

        /*
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

        */
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

    public static void main(String args[]) throws Exception {
        Env env = EnvBuilder.envFromArgs(args);
        ExportFamiliarity enhancer = new ExportFamiliarity(env);
        enhancer.writeFamiliarity(
                new File("dat/questions.tsv"),
                new File("dat/familiarity.tsv")
        );
    }
}
