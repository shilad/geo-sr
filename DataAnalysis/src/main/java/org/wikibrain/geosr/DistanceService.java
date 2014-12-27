package org.wikibrain.geosr;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.procedure.TIntFloatProcedure;
import gnu.trove.set.TIntSet;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.distance.*;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpThreadUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class DistanceService {
    private static Logger LOG = Logger.getLogger(DistanceService.class.getName());

    private static final File MATRIX_DIR = new File("distances");
//    public static final String[] METRICS = new String[] {  "spherical", "geodetic", "countries", "states", "graph" };
    public static final String[] METRICS = new String[] {  "graph" };

    private static final int NUM_POINTS = 3000;
    private final GeoEnv env;
    private Map<String, DistanceMatrix>  peopleDistances = new HashMap<String, DistanceMatrix>();
    private Map<String, DistanceMatrix>  pageDistances = new HashMap<String, DistanceMatrix>();

    public DistanceService(GeoEnv env) throws DaoException, IOException {
        this.env = env;
        if (!read()) {
            peopleDistances.clear();
            pageDistances.clear();
            rebuild();;
        }
    }

    private boolean read() throws IOException {
        LOG.info("rebuilding matrices...");
        MATRIX_DIR.mkdirs();
        for (String metric : METRICS) {
            File f1 = FileUtils.getFile(MATRIX_DIR, "people-" + metric + ".bin");
            File f2 = FileUtils.getFile(MATRIX_DIR, "page-" + metric + ".bin");
            if (!f1.isFile() || !f2.isFile()) {
                return false;
            }
            LOG.info("reading people matrix for " + metric);
            peopleDistances.put(metric, new DistanceMatrix());
            peopleDistances.get(metric).read(f1);
            LOG.info("reading page matrix for " + metric);
            pageDistances.put(metric, new DistanceMatrix());
            pageDistances.get(metric).read(f2);
        }
        return true;
    }

    public void rebuild() throws DaoException, IOException {
        TIntSet concepts = env.pageDb.getIds();
        SphericalDistanceMetric spherical = new SphericalDistanceMetric(env.dao);
        spherical.setValidConcepts(concepts);
        spherical.enableCache(true);

        // Distance metrics
        Map<String, SpatialDistanceMetric> metrics = new LinkedHashMap<String, SpatialDistanceMetric>();
        metrics.put("spherical", spherical);
        metrics.put("geodetic", new GeodeticDistanceMetric(env.dao, spherical));

        BorderingDistanceMetric countryMetric = new BorderingDistanceMetric(env.dao, "country");
        countryMetric.setMaxSteps(50);
        metrics.put("countries", countryMetric);

        BorderingDistanceMetric stateMetric = new BorderingDistanceMetric(env.dao, "state");
        stateMetric.setMaxSteps(50);
        metrics.put("states", stateMetric);

        GraphDistanceMetric graphMetric = new GraphDistanceMetric(env.dao);
        graphMetric.setMaxDistance(50);
        metrics.put("graph", graphMetric);

        for (String m : METRICS) {
            metrics.get(m).setValidConcepts(concepts);
            metrics.get(m).enableCache(true);
            File f1 = FileUtils.getFile(MATRIX_DIR, "people-" + m + ".bin");
            File f2 = FileUtils.getFile(MATRIX_DIR, "page-" + m + ".bin");
            peopleDistances.put(m, buildPeopleMatrix(metrics.get(m)));
            peopleDistances.get(m).write(f1);
            pageDistances.put(m, buildPageMatrix(metrics.get(m)));
            pageDistances.get(m).write(f2);
        }
    }

    public double getDistance(Person person, PageInfo page, String metric) {
        return peopleDistances.get(metric).getDistance(person, page);
    }

    public DistanceMatrix buildPageMatrix(final SpatialDistanceMetric metric) {
        final DistanceMatrix matrix = new DistanceMatrix();
        ParallelForEach.loop(env.pageDb.getPages(), WpThreadUtils.getMaxThreads(), new Procedure<PageInfo>() {
            @Override
            public void call(PageInfo p) throws Exception {
                try {
                    TIntFloatMap results = new TIntFloatHashMap();
                    for (SpatialDistanceMetric.Neighbor n : metric.getNeighbors(p.point, NUM_POINTS)) {
                        results.put(n.conceptId, n.conceptId);
                    }
                    synchronized (matrix) {
                        matrix.setDistances(p, results);
                    }
                } catch (Exception e) {
                    LOG.warning(
                            String.format("Calculating metric %s neighbors of %s (id=%d) failed: %s",
                                    metric.getName(), p.getTitle(), p.getId(), e.getMessage()));
                }
            }
        }, 100);
        return matrix;
    }

    public DistanceMatrix buildPeopleMatrix(final SpatialDistanceMetric metric) {
        final DistanceMatrix matrix = new DistanceMatrix();

        Set<City> cities = new HashSet<City>();
        for (Person p : env.personDb.getPeople()) {
            cities.addAll(p.cities);
        }
        final Map<Integer, TIntFloatMap> distances = new HashMap<Integer, TIntFloatMap>();
        ParallelForEach.loop(cities, WpThreadUtils.getMaxThreads(), new Procedure<City>() {
            @Override
            public void call(City c) throws Exception {
                try {
                    TIntFloatMap results = new TIntFloatHashMap();
                    for (SpatialDistanceMetric.Neighbor n : metric.getNeighbors(c.getLocation(), NUM_POINTS)) {
                        results.put(n.conceptId, n.conceptId);
                    }
                    synchronized (distances) {
                        distances.put(c.getId(), results);
                    }
                } catch (Exception e) {
                    LOG.warning(
                            String.format("Calculating metric %s neighbors of %s (id=%d) failed: %s",
                                    metric.getName(), c.getName(), c.getId(), e.getMessage()));
                }
            }
        }, 100);
        for (Person p : env.personDb.getPeople()) {
            final TIntFloatMap results = new TIntFloatHashMap();
            for (City c : p.cities) {
                if (!distances.containsKey(c.getId())) {
                    continue;
                }
                distances.get(c.getId()).forEachEntry(new TIntFloatProcedure() {
                    public boolean execute(int pageId, float dist) {
                        if (!results.containsKey(pageId) || results.get(pageId) > dist) {
                            results.put(pageId, dist);
                        }
                        return true;
                    }
                });
            }
            matrix.setDistances(p, results);
        }

        return matrix;
    }

    public static void main(String args[]) throws DaoException, ConfigurationException, IOException {
        GeoEnv env = new GeoEnv(args);
    }
}
