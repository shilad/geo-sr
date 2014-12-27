package org.wikibrain.geosr;

import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.dao.SpatialDataDao;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class PageInfoDb {
    private static final Logger LOG = Logger.getLogger(PageInfoDb.class.getCanonicalName());

    private final Map<String, PageInfo> byTitle = new HashMap<String, PageInfo>();
    private final Map<Integer, PageInfo> byId = new HashMap<Integer, PageInfo>();
    private final SpatialDataDao dao;

    public PageInfoDb(SpatialDataDao dao) throws FileNotFoundException, DaoException {
        this.dao = dao;
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
                pi.point = dao.getGeometry(pi.id, "wikidata").getCentroid();
            }
            byId.put(id, pi);
            byTitle.put(name, pi);
        }
        scanner.close();
        LOG.info("read " + byId.size() + " pages");
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
