package org.wikibrain.geosr;

import com.vividsolutions.jts.geom.Point;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.spatial.constants.RefSys;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.util.WikiBrainSpatialUtils;
import org.wikibrain.wikidata.WikidataDao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class GeoEnv {

    private static final Logger LOG = Logger.getLogger(GeoEnv.class.getName());

    public Env env;
    public SpatialDataDao dao;
    public CityDb cityDb;
    public PersonDb personDb;
    public PageInfoDb pageDb;
    public DistanceService distances;
    public ContainmentClass containmentClass;
    private LocalPageDao pageDao;
    private UniversalPageDao univDao;

    public GeoEnv(String args[]) throws ConfigurationException, IOException, DaoException {
        this(EnvBuilder.envFromArgs(args));
    }

    public GeoEnv(Env env) throws ConfigurationException, IOException, DaoException {
        this.env = env;
        this.dao = env.getConfigurator().get(SpatialDataDao.class);

        List<MissingGeometry> missing = readMissing();
        if (hasMissingGeometries(missing)) {
            addMissingGeometries(missing);
        }

        cityDb = new CityDb();
        personDb = new PersonDb();
        pageDb = new PageInfoDb(dao, env.getConfigurator().get(WikidataDao.class));
        distances = new DistanceService(this);
        containmentClass = new ContainmentClass(dao);
    }

    private void addMissingGeometries(List<MissingGeometry> missing) throws DaoException {
        LOG.info("detected missing geometries, dropping index...");
        dao.beginSaveGeometries();
        for (MissingGeometry mg : missing) {
            if (dao.getGeometry(mg.id, "wikidata") == null) {
                LOG.info("adding geometry for point " + mg.title);
                dao.saveGeometry(mg.id, "wikidata", RefSys.EARTH, mg.point);
            }
        }
        LOG.info("recreating geometric index...");
        dao.endSaveGeometries();
    }

    private boolean hasMissingGeometries(List<MissingGeometry> missing) throws DaoException {
        for (MissingGeometry mg : missing) {
            if (dao.getGeometry(mg.id, "wikidata") == null) {
                return true;
            }
        }
        return false;
    }

    private List<MissingGeometry> readMissing() throws IOException {
        List<MissingGeometry> results = new ArrayList<MissingGeometry>();
        for (String s : FileUtils.readLines(new File("dat/missing_locations.txt"))) {
            String tokens[] = s.trim().split("\t");
            if (tokens.length != 4) {
                throw new IOException("Invalid line: " + s);
            }
            MissingGeometry g = new MissingGeometry();
            g.title = tokens[0];
            g.id = Integer.valueOf(tokens[1]);
            double lat = Double.valueOf(tokens[2]);
            double lng = Double.valueOf(tokens[3]);
            g.point = WikiBrainSpatialUtils.getPoint(lat, lng);
            results.add(g);
        }
        return results;
    }

    private static class MissingGeometry {
        int id;
        String title;
        Point point;
    }
}
