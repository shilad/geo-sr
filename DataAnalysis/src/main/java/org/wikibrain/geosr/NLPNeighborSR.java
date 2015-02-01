package org.wikibrain.geosr;

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
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.Leaderboard;

/**
 * @author Shilad Sen
 */
public class NLPNeighborSR implements NeighborSR {
    private final TIntSet localConcepts;
    private final SRMetric metric;
    private final Env env;
    private final UniversalPageDao univDao;
    private final LocalPageDao pageDao;
    private final TIntIntMap localToUniv;

    public NLPNeighborSR(Env env, SRMetric metric, TIntIntMap univToLocal) throws ConfigurationException {
        this.env = env;
        this.univDao = env.getConfigurator().get(UniversalPageDao.class);
        this.pageDao = env.getConfigurator().get(LocalPageDao.class);
        this.metric = metric;
        this.localConcepts = new TIntHashSet(univToLocal.valueCollection());
        localToUniv = new TIntIntHashMap();
        for (int univId : univToLocal.keys()) {
            localToUniv.put(univToLocal.get(univId), univId);
        }
    }

    @Override
    public SRResultList mostSimilar(int conceptId) throws DaoException {
        int localId = univDao.getLocalId(Language.EN, conceptId);
        if (localId < 0) {
            throw new IllegalArgumentException("No local id for universal concept " + conceptId);
        }
        String title = pageDao.getById(Language.EN, localId).getTitle().getCanonicalTitle();
        int[] rowIds = new int[] { localId };
        int[] colIds = localConcepts.toArray();
        double[][] sims = metric.cosimilarity(rowIds, colIds);
        Leaderboard board = new Leaderboard(colIds.length);
        for (int i = 0; i < colIds.length; i++) {
            board.tallyScore(localToUniv.get(colIds[i]), sims[0][i]);
        }
        SRResultList list = board.getTop();
        list.sortDescending();;
        return list;
    }
}
