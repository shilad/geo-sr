package org.wikibrain.geosr;

import com.vividsolutions.jts.geom.Geometry;
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
    public static final String[] METRICS = new String[] {  "spherical", "geodetic", "countries", "states", "graph25", "graph100", "ordinal" };
//    public static final String[] METRICS = new String[] {  "graph" };

    private static final int NUM_POINTS = 3000;
    private final GeoEnv env;
    private Map<String, DistanceMatrix>  peopleDistances = new HashMap<String, DistanceMatrix>();
    private Map<String, DistanceMatrix>  pageDistances = new HashMap<String, DistanceMatrix>();

    public DistanceService(GeoEnv env) throws DaoException, IOException {
        this.env = env;
        List<String> metricsToBuild = read();
        rebuild(metricsToBuild);
    }

    private List<String> read() throws IOException {
        List<String> toBuilds = new ArrayList<String>();
        LOG.info("rebuilding matrices...");
        MATRIX_DIR.mkdirs();
        for (String metric : METRICS) {
            File f1 = FileUtils.getFile(MATRIX_DIR, "people-" + metric + ".bin");
            File f2 = FileUtils.getFile(MATRIX_DIR, "page-" + metric + ".bin");
            if (!f1.isFile() || !f2.isFile()) {
                toBuilds.add(metric);
            } else {
                LOG.info("reading people matrix for " + metric + " from " + f1);
                peopleDistances.put(metric, new DistanceMatrix());
                peopleDistances.get(metric).read(f1);
                LOG.info("reading page matrix for " + metric + " from " + f2);
                pageDistances.put(metric, new DistanceMatrix());
                pageDistances.get(metric).read(f2);
            }
        }
        return toBuilds;
    }

    public void rebuild(List<String> metricNames) throws DaoException, IOException {
        TIntSet concepts = env.pageDb.getIds();

        // Spherical with all points
        SphericalDistanceMetric allSpherical = new SphericalDistanceMetric(env.dao);
        allSpherical.enableCache(true);

        // Spherical with our concept points
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

        GraphDistanceMetric graph100Metric = new GraphDistanceMetric(env.dao, allSpherical);
        graph100Metric.setMaxDistance(50);
        graph100Metric.setNumNeighbors(100);
        metrics.put("graph100", graph100Metric);

        GraphDistanceMetric graph25Metric = new GraphDistanceMetric(env.dao, allSpherical);
        graph25Metric.setMaxDistance(50);
        graph25Metric.setNumNeighbors(25);
        metrics.put("graph25", graph25Metric);

        OrdinalDistanceMetric ordinalMetric = new OrdinalDistanceMetric(env.dao, allSpherical);
        metrics.put("ordinal", ordinalMetric);

        for (String m : metricNames) {
            metrics.get(m).setValidConcepts(concepts);
            metrics.get(m).enableCache(true);
            if (metrics.get(m) instanceof BorderingDistanceMetric) {
                ((BorderingDistanceMetric)metrics.get(m)).setForceContains(true);
            }
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

    public double getDistance(PageInfo page1, PageInfo page2, String metric) {
        return pageDistances.get(metric).getDistance(page1, page2);
    }

    public DistanceMatrix buildPageMatrix(final SpatialDistanceMetric metric) {
        final DistanceMatrix matrix = new DistanceMatrix();
        ParallelForEach.loop(env.pageDb.getPages(), WpThreadUtils.getMaxThreads(), new Procedure<PageInfo>() {
            @Override
            public void call(PageInfo p) throws Exception {
                try {
                    TIntFloatMap results = new TIntFloatHashMap();
                    for (SpatialDistanceMetric.Neighbor n : getNeighbors(metric, p.point)) {
                        results.put(n.conceptId, (float) n.distance);
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

    private List<SpatialDistanceMetric.Neighbor> getNeighbors(SpatialDistanceMetric metric, Geometry g) {
        if (metric instanceof OrdinalDistanceMetric) {
            return metric.getNeighbors(g, Integer.MAX_VALUE);
        } else {
            return metric.getNeighbors(g, NUM_POINTS);
        }
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
                    for (SpatialDistanceMetric.Neighbor n : getNeighbors(metric, c.getLocation())) {
                        results.put(n.conceptId, (float) n.distance);
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
