package org.wikibrain.geosr;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.distance.BorderingDistanceMetric;
import org.wikibrain.spatial.distance.OrdinalDistanceMetric;
import org.wikibrain.spatial.distance.SpatialDistanceMetric;
import org.wikibrain.spatial.distance.SphericalDistanceMetric;
import org.wikibrain.spatial.util.ContainmentIndex;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.Leaderboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Shilad Sen
 */
public class GeospatialNeighborSR implements NeighborSR {

    private final Map<Integer, String> titles;
    private final Map<Integer, Geometry> countries;
    private final Map<Integer, Geometry> states;
    private final Map<Integer, Geometry> points;
    private final Env env;
    private final TIntIntMap univToLocal;
    private final TIntIntMap localToUniv;
    private final TIntSet localConcepts;
    private final TIntSet univConcepts;

    private final BorderingDistanceMetric countryMetric;
    private final BorderingDistanceMetric stateMetric;
    private final OrdinalDistanceMetric ordinalMetric;

    private final ContainmentIndex countryIndex;
    private final ContainmentIndex stateIndex;
    private final UniversalPageDao univDao;
    private final LocalPageDao pageDao;
    private final SpatialDataDao spatialDao;
    private final NeighborSR baseSR;

    public GeospatialNeighborSR(Env env, NeighborSR base, TIntIntMap univToLocal) throws DaoException, ConfigurationException {
        this.env = env;
        this.spatialDao = env.getConfigurator().get(SpatialDataDao.class);
        this.univDao = env.getConfigurator().get(UniversalPageDao.class);
        this.pageDao = env.getConfigurator().get(LocalPageDao.class);
        this.baseSR = base;

        this.univToLocal = univToLocal;
        this.localConcepts = new TIntHashSet(univToLocal.valueCollection());
        this.univConcepts = univToLocal.keySet();

        this.points = spatialDao.getAllGeometriesInLayer("wikidata");
        this.states = spatialDao.getAllGeometriesInLayer("state");
        this.countries = spatialDao.getAllGeometriesInLayer("country");

        // Spherical with our concept points
        SphericalDistanceMetric spherical = new SphericalDistanceMetric(spatialDao);
        spherical.enableCache(true);

        this.ordinalMetric = new OrdinalDistanceMetric(spatialDao, spherical);
        ordinalMetric.setValidConcepts(univConcepts);
        ordinalMetric.enableCache(true);

        /*for (Geometry p : points.values()) {
            int i = 0;
            // ordinal
            for (SpatialDistanceMetric.Neighbor n : ordinalMetric.getNeighbors(p, univConcepts.size())) {
                System.out.println(i + ". distance =" + n.distance);
                if (++i > 20) {
                    break;
                }
            }
        } */

        this.countryMetric = new BorderingDistanceMetric(spatialDao, "country");
        countryMetric.setMaxSteps(50);
        countryMetric.setValidConcepts(univConcepts);
        countryMetric.enableCache(true);

        this.stateMetric = new BorderingDistanceMetric(spatialDao, "state");
        stateMetric.setMaxSteps(50);
        stateMetric.setValidConcepts(univConcepts);
        stateMetric.enableCache(true);

        this.countryIndex = new ContainmentIndex();
        this.stateIndex = new ContainmentIndex();

        populateIndex(countryIndex, countries);
        populateIndex(stateIndex, states);

        titles = new HashMap<Integer, String>();
        localToUniv = new TIntIntHashMap();
        for (int id : univToLocal.keys()) {
            localToUniv.put(univToLocal.get(id), id);
            titles.put(id, getTitleForConcept(id));
        }
    }

    @Override
    public SRResultList mostSimilar(int conceptId) throws DaoException {
        Map<Integer, double[]> features = buildFeatures(conceptId);
        Leaderboard leaderboard = new Leaderboard(univConcepts.size());
        for (int id2 : features.keySet()) {
            double v[] = features.get(id2);
            double sum = 0.0;
            for (Feature f : Feature.values()) {
                sum += f.coefficient * v[f.index];
            }
            leaderboard.tallyScore(id2, sum);
        }
        SRResultList list = leaderboard.getTop();
        list.sortDescending();
        return list;
    }

    private String getTitleForConcept(int conceptId) throws DaoException {
        int localId;
        if (univToLocal.containsKey(conceptId)) {
            localId = univToLocal.get(conceptId);
        } else {
            localId = univDao.getLocalId(Language.EN, conceptId);
        }
        LocalPage page = pageDao.getById(Language.EN, localId);
        if (page == null) {
            System.err.println("No title for local id " + localId);
            return "Unknown (id " + localId + ")";
        } else {
            return page.getTitle().getCanonicalTitle();
        }
    }

    private void populateIndex(ContainmentIndex index, Map<Integer, Geometry> geoms) {
        for (int conceptId : geoms.keySet()) {
            if (univConcepts.contains(conceptId)) {
                index.insert(conceptId, geoms.get(conceptId));
            }
        }
    }


    /**
     *
     * From Weka:
     *

     For things that are far away:

     0.1165 * lcs +
     -0.058  * states +
     -0.1279 * ordinal +
     2.4324 * sr +
     -0.9937 * cc-country-state-false +
     1.8358 * cc-country-point-true +
     0.8212 * cc-state-point-true +
     2.0482

     For things that are close:

     -0.0269 * ordinal +
     1.001  * sr +
     -0.463  * cc-country-point-false +
     0.7886 * cc-country-point-true +
     -0.3201 * cc-state-state-false +
     0.4517 * cc-state-point-true +
     -0.2142 * cc-point-point-false +
     3.1617



     *
     * @author Shilad Sen
     */
    public  enum Feature {
//        LCS(0),
//        STATES(0),
//        COUNTRIES(0),
//        ORDINAL(-0.0269),
//        SR(1.001),
//        COUNTRY_COUNTRY_FALSE(0),
//        COUNTRY_STATE_TRUE(0),
//        COUNTRY_STATE_FALSE(0),
//        COUNTRY_POINT_TRUE(0.7886),
//        COUNTRY_POINT_FALSE(-.463),
//        STATE_STATE_FALSE(-.3201),
//        STATE_POINT_TRUE(.4517),
//        STATE_POINT_FALSE(0),
//        POINT_POINT_FALSE(-.2142),
//        INTERCEPT(3.1617);
        /*LCS(0.1165),
        STATES(-0.058),
        COUNTRIES(0),
        ORDINAL(-0.1279),
        SR(2.4324),
        COUNTRY_COUNTRY_FALSE(0),
        COUNTRY_STATE_TRUE(0),
        COUNTRY_STATE_FALSE(-0.9937),
        COUNTRY_POINT_TRUE(1.8358),
        COUNTRY_POINT_FALSE(0),
        STATE_STATE_FALSE(0),
        STATE_POINT_TRUE(0.8212),
        STATE_POINT_FALSE(0),
        POINT_POINT_FALSE(0),
        INTERCEPT(2.2223);
        */
        LCS(0),
        COUNTRIES(0.3002),
        STATES(-0.206),
        ORDINAL(-0.1564),
        SR(2.5285),
        COUNTRY_COUNTRY_FALSE(-0.8315),
        COUNTRY_STATE_TRUE(0),
        COUNTRY_STATE_FALSE(-1.1938),
        COUNTRY_POINT_TRUE(1.7921),
        COUNTRY_POINT_FALSE(-0.34),
        STATE_STATE_FALSE(0),
        STATE_POINT_TRUE(0.8172),
        STATE_POINT_FALSE(0),
        POINT_POINT_FALSE(0),
        INTERCEPT(2.4694);

        public final int index;
        public final double coefficient;

        Feature(double c) {
            this.coefficient = c;
            this.index = this.ordinal();
        }
    }


    private static final double IMPUTED_STATE_VAL = 3.714;
    private static final double IMPUTED_COUNTRY_VAL = 2.890;
    private static final double IMPUTED_TYPE_SR_VAL = 0.529;
    private static final double IMPUTED_SR_VAL = 0.2;   // hack

    private Map<Integer, double[]> buildFeatures(int conceptId) throws DaoException {
        Map<String, Geometry> geometries = spatialDao.getGeometries(conceptId);
        if (geometries.size() != 1 || !geometries.containsKey("wikidata")) {
            throw new IllegalArgumentException("right now geospatial SR only implemented for points");
        }
        Point point = (Point) geometries.get("wikidata");

        String title1 = getTitleForConcept(conceptId);
        if (title1 == null) {
            throw new IllegalArgumentException("no title for " + conceptId);
        }


        Map<Integer, double[]> result = new HashMap<Integer, double[]>();
        for (int id2 : univConcepts.toArray()) {
            double v[] = new double[Feature.INTERCEPT.index+1];
            v[Feature.INTERCEPT.index] = 1.0;
            result.put(id2, v);
        }

        // lcs
        for (int id2 : titles.keySet()) {
            double lcs = Utils.longestSubstring(title1, titles.get(id2));
            result.get(id2)[Feature.LCS.index] = Math.log(1 + lcs);
        }

        // Build up mapping of concept -> containment index that should be set to 1
        Map<Integer, Integer> containment = new HashMap<Integer, Integer>();
        for (ContainmentIndex.Result r : countryIndex.getContainer(point)) {
            containment.put(r.id, Feature.COUNTRY_POINT_TRUE.index);
        }
        for (ContainmentIndex.Result r : stateIndex.getContainer(point)) {
            if (!containment.containsKey(r.id)) {
                containment.put(r.id, Feature.STATE_POINT_TRUE.index);
            }
        }
        for (int id2 : univConcepts.toArray()) {
            if (!containment.containsKey(id2) && countries.containsKey(id2)) {
                containment.put(id2, Feature.COUNTRY_POINT_FALSE.index);
            }
            if (!containment.containsKey(id2) && states.containsKey(id2)) {
                containment.put(id2, Feature.STATE_POINT_FALSE.index);
            }
            if (!containment.containsKey(id2)) {
                containment.put(id2, Feature.POINT_POINT_FALSE.index);
            }
            result.get(id2)[containment.get(id2)] = 1.0;
        }

        // Imputed values
        for (int id2 : univConcepts.toArray()) {
            result.get(id2)[Feature.SR.index] = IMPUTED_SR_VAL;
            result.get(id2)[Feature.STATES.index] = IMPUTED_STATE_VAL;
            result.get(id2)[Feature.COUNTRIES.index] = IMPUTED_COUNTRY_VAL;
            result.get(id2)[Feature.ORDINAL.index] = Math.log(1 + univConcepts.size());
        }

        // countries
        for (SpatialDistanceMetric.Neighbor n : countryMetric.getNeighbors(point, univConcepts.size())) {
            result.get(n.conceptId)[Feature.COUNTRIES.index] = Math.log(1 + n.distance);
        }

        // states
        for (SpatialDistanceMetric.Neighbor n : stateMetric.getNeighbors(point, univConcepts.size())) {
            result.get(n.conceptId)[Feature.STATES.index] = Math.log(1 + n.distance);
        }

        // ordinal
        for (SpatialDistanceMetric.Neighbor n : ordinalMetric.getNeighbors(point, univConcepts.size())) {
            result.get(n.conceptId)[Feature.ORDINAL.index] = Math.log(500 + n.distance);
        }

        // sr
        for (SRResult r : baseSR.mostSimilar(conceptId)) {
            result.get(r.getId())[Feature.SR.index] = r.getScore();
        }

        return result;
    }
}
