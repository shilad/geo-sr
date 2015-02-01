package org.wikibrain.geosr;

import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.pageview.PageViewDao;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.normalize.PercentileNormalizer;
import org.wikibrain.utils.WpCollectionUtils;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class Simulator {
    private final Env env;
    private final LocalPageDao pageDao;
    private final UniversalPageDao univDao;
    private final Map<Integer, Geometry> points;
    private final NeighborSR nlpSr;
    private final NeighborSR geoSr;
    private final SpatialDataDao spatialDao;

    private TIntIntMap conceptViews;
    private TIntIntMap univToLocal;


    public Simulator(Env env) throws DaoException, IOException, ConfigurationException {
        this.env = env;
        this.pageDao = env.getConfigurator().get(LocalPageDao.class);
        this.univDao = env.getConfigurator().get(UniversalPageDao.class);
        this.spatialDao = env.getConfigurator().get(SpatialDataDao.class);
        this.points = spatialDao.getAllGeometriesInLayer("wikidata");
        this.loadPopularConcepts();
        SRMetric metric = env.getConfigurator().get(SRMetric.class, "ensemble", "language", "en");
        this.nlpSr = new NLPNeighborSR(env, metric, univToLocal);
//        this.geoSr = new NLPNeighborSR(env, metric, univToLocal);
        this.geoSr = new GeospatialNeighborSR(env, nlpSr, univToLocal);
    }

    public void mostSimilar(String title) throws DaoException {
        int localId = pageDao.getIdByTitle(title, Language.EN, NameSpace.ARTICLE);
        if (localId < 0) {
            throw new IllegalArgumentException("No article for title " + title);
        }
        int univId = univDao.getUnivPageId(Language.EN, localId);
        if (univId < 0) {
            throw new IllegalArgumentException("No universal concept for title " + title);
        }
        SRResultList geoResults = normalize(geoSr.mostSimilar(univId));
        SRResultList nlpResults = normalize(nlpSr.mostSimilar(univId));
        System.out.println("\nResults for NLP SR: ");
        print(nlpResults, geoResults);

        System.out.println("\nResults for Geo SR: ");
        print(geoResults, nlpResults);
    }

    private void print(SRResultList list1, SRResultList list2) throws DaoException {
        TIntFloatMap map1 = list1.asTroveMap();
        TIntFloatMap map2 = list2.asTroveMap();
        int i = 0;
        for (int univId : WpCollectionUtils.sortMapKeys(map1, true)) {
            if (!univToLocal.containsKey(univId)) {
                throw new IllegalStateException("Unknown univ id: " + univId);
            }
            int localId = univToLocal.get(univId);
            String title = pageDao.getById(Language.EN, localId).getTitle().getCanonicalTitle();
            System.out.format("%d: %s %.3f (vs %.3f)\n", ++i, title, map1.get(univId), map2.get(univId));
            if (i > 50) {
                break;
            }
        }
    }

    private SRResultList normalize(SRResultList list) {
        PercentileNormalizer normalizer = new PercentileNormalizer();
        // Mostly sample from the top 10% of the list
        for (int i = 0; i < conceptViews.size() / 10; i++) {
            double v = (i < list.numDocs()) ? list.getScore(i) : 0.0;
            normalizer.observe(v);
        }

        // Repeat, with every 10th item
        for (int i = 0; i < conceptViews.size() / 10; i++) {
            double v = (10*i < list.numDocs()) ? list.getScore(10*i) : 0.0;
            normalizer.observe(v);
        }

        // Make sure we have the last score to be clean
        normalizer.observe(list.getScore(list.numDocs() - 1));

        normalizer.observationsFinished();
        return normalizer.normalize(list);
    }

    private void loadPopularConcepts() throws ConfigurationException, DaoException, IOException {
        File f = new File("dat/spatial-views.txt");
        if (!f.isFile()) {
            generateSpatialViews(f);
        }
        conceptViews = new TIntIntHashMap();
        univToLocal = new TIntIntHashMap();
        for (String line : FileUtils.readLines(f)) {
            String tokens[] = line.split("\t");
            int univId = Integer.valueOf(tokens[0]);
            int views = Integer.valueOf(tokens[1]);
            int localId = Integer.valueOf(tokens[2]);
            conceptViews.put(univId, views);
            univToLocal.put(univId, localId);
            if (conceptViews.size() >= 20000) {
                break;
            }
        }
    }

    private void generateSpatialViews(File f) throws ConfigurationException, DaoException, IOException {
        UniversalPageDao upDao = env.getConfigurator().get(UniversalPageDao.class);
        TIntIntMap conceptMap = upDao
                .getAllLocalToUnivIdsMap(new LanguageSet(Language.EN))
                .get(Language.EN);

        PageViewDao dao = env.getConfigurator().get(PageViewDao.class);
        DateTime end = DateTime.now();
        DateTime start = new DateTime(2010, 1, 1, 1, 1, 1);
        TIntIntMap allViews = dao.getAllViews(Language.EN, start, end);
        TIntIntMap geoViews = new TIntIntHashMap();
        TIntIntMap univToLocal = new TIntIntHashMap();

        int missingConcepts = 0;
        int missingGeometry = 0;
        for (int localId : allViews.keys()) {
            if (!conceptMap.containsKey(localId)) {
                missingConcepts++;
                continue;
            }
            int conceptId = conceptMap.get(localId);
            if (getBestGeometry(conceptId) == null) {
                missingGeometry++;
                continue;
            }
            geoViews.put(conceptId, allViews.get(localId));
            univToLocal.put(conceptId, localId);
        }

        System.err.format(
                "%d pages with views. " +
                        "%d had no concept and %d had no geometry.\n" +
                        "Retained %d\n"
                , allViews.size(), missingConcepts, missingGeometry, geoViews.size()
        );

        BufferedWriter writer = WpIOUtils.openWriter(f);
        for (int id : WpCollectionUtils.sortMapKeys(geoViews, true)) {
            writer.write(id + "\t" + geoViews.get(id) + "\t" + univToLocal.get(id) + "\n");
        }
        writer.close();
    }

    private Geometry getBestGeometry(int conceptId) {
        return points.get(conceptId);
    }

    public static void main(String args[]) throws DaoException, ConfigurationException, IOException {
        Env env = EnvBuilder.envFromArgs(args);
        Simulator sim = new Simulator(env);
        List<String> titles = Arrays.asList(
                "Minneapolis",
                "Walker Art Center",
                "Madison Square Garden",
                "Solomon R. Guggenheim Museum",
                "Harvard University",
                "University of Minnesota",
                "Eiffel Tower",
                "CN Tower",
                "Egyptian Pyramids",
                "Taj Mahal",
                "El Paso, Texas",
                "Sault Ste. Marie, Michigan",
                "Sault Ste. Marie, Ontario",
                "Inglewood, California",
                "Compton, California",
                "New York City",
                "San Francisco",
                "Inuvik",
                "McCarthy, Alaska",
                "Bowman, South Dakota",
                "Duluth, Minnesota"
        );
        for (String t : titles) {
            System.out.println("\n\nResults for: " + t);
            sim.mostSimilar(t);
        }
    }
}
