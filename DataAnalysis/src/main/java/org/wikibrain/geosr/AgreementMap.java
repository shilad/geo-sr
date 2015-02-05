package org.wikibrain.geosr;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shilad Sen
 */
public class AgreementMap {
    private static final Envelope NORTH_AMERICA = new Envelope(-170, -50, 12, 75);

    private final Env env;
    private final LocalPageDao pageDao;
    private final UniversalPageDao univDao;
    private final Map<Integer, Geometry> points;
    private final NeighborSR nlpSr;
    private final NeighborSR geoSr;
    private final SpatialDataDao spatialDao;

    private TIntIntMap conceptViews;
    private TIntIntMap univToLocal;


    public AgreementMap(Env env) throws DaoException, IOException, ConfigurationException {
        this.env = env;
        this.pageDao = env.getConfigurator().get(LocalPageDao.class);
        this.univDao = env.getConfigurator().get(UniversalPageDao.class);
        this.spatialDao = env.getConfigurator().get(SpatialDataDao.class);
        this.points = new ConcurrentHashMap<Integer, Geometry>(
                            spatialDao.getAllGeometriesInLayer("wikidata"));
        this.loadPopularConcepts();
        SRMetric metric = env.getConfigurator().get(SRMetric.class, "ensemble", "language", "en");
        this.nlpSr = new NLPNeighborSR(env, metric, univToLocal);
//        this.geoSr = new NLPNeighborSR(env, metric, univToLocal);
        this.geoSr = new GeospatialNeighborSR(env, nlpSr, univToLocal);
    }

    public void writeAgreement(File file, Envelope bounds, int n) throws IOException {
        final BufferedWriter writer = WpIOUtils.openWriter(file);
        List<Integer> sample = pickSample(bounds, n);
        ParallelForEach.loop(sample, new Procedure<Integer>() {
            @Override
            public void call(Integer id) throws Exception {
                processPoint(writer, id);
            }
        });
        writer.close();
    }

    private void processPoint(BufferedWriter writer, int id) throws DaoException, IOException {
        String title = getTitle(id);
        if (title == null) {
            return;
        }
        StringBuilder line = new StringBuilder();
        line.append(id);
        line.append("\t");
        line.append(title);
        line.append("\t");
        line.append(getIdStr(nlpSr.mostSimilar(id), 200));
        line.append("\t");
        line.append(getIdStr(geoSr.mostSimilar(id), 200));
        line.append("\n");
        synchronized (writer) {
            writer.write(line.toString());
        }
    }

    private String getIdStr(SRResultList list, int n) {
        list.sortDescending();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < n &&  i < list.numDocs(); i++) {
            if (i > 0) {
                buffer.append("|");
            }
            buffer.append(list.getId(i));
            if (i++ > 100) {
                break;
            }
        }
        return buffer.toString();
    }

    private String getTitle(int conceptId) throws DaoException {
        int localId = univDao.getLocalId(Language.EN, conceptId);
        if (localId < 0) {
            return null;
        }
        LocalPage page = pageDao.getById(Language.EN, localId);
        if (page == null) {
            return null;
        } else {
            return page.getTitle().getCanonicalTitle();
        }
    }

    private List<Integer> pickSample(Envelope bounds, int n) {
        List<Integer> sampleIds = new ArrayList<Integer>();
        for (int id : conceptViews.keys()) {
            if (points.containsKey(id)) {
                Point p = points.get(id).getCentroid();
                if (bounds.contains(p.getCoordinate())) {
                    sampleIds.add(id);
                }
            }
        }
        Collections.shuffle(sampleIds);
        if (sampleIds.size() > n) {
            sampleIds = sampleIds.subList(0, n);
        }
        return sampleIds;
    }



    private void loadPopularConcepts() throws ConfigurationException, DaoException, IOException {
        File f = new File("dat/spatial-views.txt");
        if (!f.isFile()) {
            throw new IllegalArgumentException("Create spatial views by running Simulator.java");
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
            if (conceptViews.size() >= 50000) {
                break;
            }
        }
    }

    public static void main(String args[]) throws ConfigurationException, IOException, DaoException {
        Env env = EnvBuilder.envFromArgs(args);
        AgreementMap map = new AgreementMap(env);
        map.writeAgreement(new File("dat/agreement.txt"), NORTH_AMERICA, 10000);
    }

}
