package org.wikibrain.geosr;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.dao.SpatialDataDao;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shilad Sen
 */
public class ScaleAwareContains {
    private final SpatialDataDao dao;
    private ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<String, String>();

    public ScaleAwareContains(SpatialDataDao dao) {
        this.dao = dao;
    }

    public String getCategory(int conceptId1, int conceptId2) {
        String key = conceptId1 + ":" + conceptId2;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        Layers l1 = getLayers(conceptId1);
        Layers l2 = getLayers(conceptId2);
        if (l1 == null || l2 == null) {
            return "unknown";
        }
        String result = null;
        if (l1.country != null && l2.country != null) {
            result = "country-country-false";
        } else if (l1.country != null && l2.state != null) {
            result = "country-state-" + cleanContains(l1.country, l2.state);
        } else if (l1.state != null && l2.country != null) {
            result = "country-state-" + cleanContains(l2.country, l1.state);
        } else if (l1.country != null) {
            result = "country-point-" + cleanContains(l1.country, l2.wikidata);
        } else if (l2.country != null) {
            result = "country-point-" + cleanContains(l2.country, l1.wikidata);
        } else if (l1.state != null && l2.state != null) {
            result = "state-state-false";
        } else if (l1.state != null) {
            result = "state-point-" + cleanContains(l1.state, l2.wikidata);
        } else if (l2.state != null) {
            result = "state-point-" + cleanContains(l2.state, l1.wikidata);
        } else {
            result = "point-point-false";
        }
        cache.put(key, result);
        return result;
    }

    private boolean cleanContains(Geometry g1, Geometry g2) {
        if (g2 instanceof Point) {
            return g1.contains(g2);
        } else {
            double area = g2.getArea();
            return g1.intersection(g2).getArea() >= 0.9 * area;
        }
    }

    private Layers getLayers(int id) {
        try {
            Layers layers = new Layers();
            layers.wikidata = dao.getGeometry(id, "wikidata");
            if (layers.wikidata == null) {
                return null;
            }
            layers.state = dao.getGeometry(id, "state");
            layers.country = dao.getGeometry(id, "country");

            // Treat e.g. Vatican City as a country.
            if (layers.country != null && layers.state != null) {
                layers.state = null;
            }
            return layers;
        } catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    private class Layers {
        Geometry wikidata;
        Geometry state;
        Geometry country;
    }
}
