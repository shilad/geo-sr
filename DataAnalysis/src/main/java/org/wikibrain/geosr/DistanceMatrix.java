package org.wikibrain.geosr;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import org.wikibrain.utils.WpIOUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class DistanceMatrix implements Serializable {
    private Map<Integer, TIntFloatMap> matrix = new HashMap<Integer, TIntFloatMap>();

    public double getDistance(Identifiable i1, Identifiable i2) {
        int id1 = i1.getId();
        int id2 = i2.getId();
        if (!matrix.containsKey(id1)) {
            return Double.NaN;
        }
        if (!matrix.get(id1).containsKey(id2)) {
            return Double.NaN;
        }
        return matrix.get(id1).get(id2);
    }

    public void setDistances(Identifiable i1, TIntFloatMap results) {
        synchronized (matrix) {
            matrix.put(i1.getId(), results);
        }
    }

    public void write(File file) throws IOException {
        WpIOUtils.writeObjectToFile(file, matrix);
    }

    public void read(File file) throws IOException {
        matrix = (Map<Integer, TIntFloatMap>) WpIOUtils.readObjectFromFile(file);
    }
}
