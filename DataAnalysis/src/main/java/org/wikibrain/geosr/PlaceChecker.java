package org.wikibrain.geosr;

import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.lang3.StringUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.util.ContainmentIndex;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class PlaceChecker {
    private static final Logger LOG = Logger.getLogger(PlaceChecker.class.getName());

    private final GeoEnv env;
    private final ContainmentIndex states;
    private final ContainmentIndex countries;

    public PlaceChecker(GeoEnv env) throws DaoException {
        this.env = env;
        this.states = makeIndex("state");
        this.countries = makeIndex("country");
    }

    private ContainmentIndex makeIndex(String layer) throws DaoException {
        final ContainmentIndex index = new ContainmentIndex();
        final Map<Integer, Geometry> geometries = env.dao.getAllGeometriesInLayer(layer);
        ParallelForEach.loop(geometries.keySet(), new Procedure<Integer>() {
            @Override
            public void call(Integer id) throws Exception {
                index.insert(id, geometries.get(id));
            }
        });
        LOG.info("loaded" + geometries.size() + " elements into index for " + layer);
        return index;
    }

    public void checkPages() throws DaoException, IOException {
        final BufferedWriter writer = WpIOUtils.openWriter(new File("dat/page-check.txt"));
        ParallelForEach.loop(env.pageDb.getPages(), new Procedure<PageInfo>() {
            @Override
            public void call(PageInfo pageInfo) throws Exception {
                checkPage(writer, pageInfo);
            }
        });
        writer.close();
    }

    public void checkCities() throws DaoException, IOException {
        Set<City> cities = new HashSet<City>();
        for (Person p : env.personDb.getPeople()) {
            cities.addAll(p.cities);
        }

        final BufferedWriter writer = WpIOUtils.openWriter(new File("dat/city-check.txt"));
        ParallelForEach.loop(cities, new Procedure<City>() {
            @Override
            public void call(City city) throws Exception {
                checkCity(writer, city);
            }
        });
        writer.close();
    }

    public void checkPage(BufferedWriter writer, PageInfo info) throws DaoException, IOException {
        Map<String, Geometry> layers = env.dao.getGeometries(info.getId());
        Geometry point = layers.get("wikidata");
        boolean hasConcept = info.getId() > 0;
        boolean hasWikidata = point != null;
        boolean hasPolygon = layers.containsKey("state") || layers.containsKey("country");
        boolean inState = point == null ? false : (states.getContainer(point).size() > 0);
        boolean inCountry = point == null ? false : (countries.getContainer(point).size() > 0);
        List<String> tokens = new ArrayList<String>();
        tokens.add("" + info.id);
        tokens.add("" + info.title);
        tokens.add("" + info.point);
        tokens.add("" + info.getScaleString());
        if (!hasConcept) tokens.add("noConcept");
        if (!hasWikidata) tokens.add("noWikidata");
        if (!hasPolygon) tokens.add("noPolygon");
        if (!inCountry) tokens.add("noCountry");
        if (!inState) tokens.add("noState");

        synchronized (writer) {
            writer.write(StringUtils.join(tokens, ", ") + "\n");
        }
    }

    public void checkCity(BufferedWriter writer, City city) throws IOException {
        boolean inState = states.getContainer(city.getLocation()).size() > 0;
        boolean inCountry = countries.getContainer(city.getLocation()).size() > 0;
        List<String> tokens = new ArrayList<String>();
        tokens.add("" + city.getId());
        tokens.add("" + city.getName());
        tokens.add("" + city.getLocation());
        tokens.add("" + city.getCountry());
        if (!inCountry) tokens.add("noCountry");
        if (!inState) tokens.add("noState");

        synchronized (writer) {
            writer.write(StringUtils.join(tokens, ", ") + "\n");
        }
    }

    public static void main(String args[]) throws DaoException, ConfigurationException, IOException {
        GeoEnv env = new GeoEnv(args);
        PlaceChecker checker = new PlaceChecker(env);
//        checker.checkPages();
        checker.checkCities();
    }
}
