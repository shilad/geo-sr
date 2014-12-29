package org.wikibrain.geosr;

import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.StringUtils;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.wikidata.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class PageInfoDb {
    private static final Logger LOG = Logger.getLogger(PageInfoDb.class.getCanonicalName());

    private final Map<String, PageInfo> byTitle = new HashMap<String, PageInfo>();
    private final Map<Integer, PageInfo> byId = new HashMap<Integer, PageInfo>();
    private final SpatialDataDao dao;
    private final WikidataDao wikidataDao;

    public PageInfoDb(SpatialDataDao dao, WikidataDao wikidataDao) throws FileNotFoundException, DaoException {
        this.dao = dao;
        this.wikidataDao = wikidataDao;
        readPopularity();
        readScales();
    }

    private void readPopularity() throws FileNotFoundException, DaoException {
        File file = new File("dat/pagePopularity.txt");
        Scanner scanner = new Scanner(file);
        int rank = 0;
        while(scanner.hasNextLine()){
            String next= scanner.nextLine();
            StringTokenizer st= new StringTokenizer(next,"\t",false);
            int id= Integer.parseInt(st.nextToken());
            String name= st.nextToken();
            PageInfo pi = new PageInfo();
            pi.id = id;
            pi.title = name;
            pi.viewRank = rank++;
            Geometry g = dao.getGeometry(pi.id, "wikidata");
            if (g == null) {
                LOG.info("missing geometry for " + pi.title + " (id " + pi.id + ")");
            } else {
                pi.point = g.getCentroid();
                pi.instanceOf = getInstanceOf(pi.id);
//                System.err.format("%s is %s\n", pi.getTitle(), pi.instanceOf);

            }
            byId.put(id, pi);
            byTitle.put(name, pi);
        }
        scanner.close();
        LOG.info("read " + byId.size() + " pages");
    }

    Map<Integer, String> instanceNames = new ConcurrentHashMap<Integer, String>();
    private String getInstanceOf(int conceptId) throws DaoException {
        WikidataFilter filter = new WikidataFilter.Builder()
                .withEntityType(WikidataEntity.Type.ITEM)
                .withEntityId(conceptId)
                .withPropertyId(31)
                .build();

        List<String> instanceNames = new ArrayList<String>();
        for (WikidataStatement statement  : wikidataDao.get(filter)) {
            int itemId = statement.getValue().getItemValue();
            if (!this.instanceNames.containsKey(itemId)) {
                this.instanceNames.put(itemId, wikidataDao.getLabel(Language.EN, WikidataEntity.Type.ITEM, itemId));
            }
            String n = this.instanceNames.get(itemId);
            if (n != null && !n.equals("unknown")) {
                instanceNames.add(n);
            }
        }
        return StringUtils.join(instanceNames, "|");
    }

    public Collection<PageInfo> getPages() {
        return byId.values();
    }

    public PageInfo getByTitle(String title) {
        return byTitle.get(title);
    }

    public PageInfo getById(int id) {
        return byId.get(id);
    }

    public TIntSet getIds() {
        TIntSet ids = new TIntHashSet();
        for (int id : byId.keySet()) {
            ids.add(id);
        }
        return ids;
    }

    private void readScales() throws FileNotFoundException {
        Scanner scanner= new Scanner(new File("dat/pageScales.txt"));

        //Throw out the first 7 lines because they are information
        for (int i = 0; i <7 ; i++) {
            scanner.nextLine();
        }

        int numMatches = 0;
        while(scanner.hasNextLine()){
            String s= scanner.nextLine();
            String[] info= s.split("\t", -1);
            int id = Integer.parseInt(info[0]);
            int scale = Integer.parseInt(info[1]);
            if (byId.containsKey(id)) {
                byId.get(id).scale = scale;
                numMatches++;
            }
        }
        LOG.info("added scale to " + numMatches + " pages");
    }
}
