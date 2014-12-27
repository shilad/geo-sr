package org.wikibrain.geosr;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.dao.SpatialDataDao;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author Shilad Sen
 */
public class GeoEnv {
    public Env env;
    public SpatialDataDao dao;
    public CityDb cityDb;
    public PersonDb personDb;
    public PageInfoDb pageDb;
    public DistanceService distances;

    public GeoEnv(String args[]) throws ConfigurationException, IOException, DaoException {
        this(EnvBuilder.envFromArgs(args));
    }

    public GeoEnv(Env env) throws ConfigurationException, IOException, DaoException {
        this.env = env;
        this.dao = env.getConfigurator().get(SpatialDataDao.class);
        cityDb = new CityDb();
        personDb = new PersonDb();
        pageDb = new PageInfoDb(dao);
        distances = new DistanceService(this);
    }
}
