package org.wikibrain.geosr;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * @author Shilad Sen
 */
public class Utils {
    public static Point makePoint(double lat, double lng) {
        Coordinate[] coords = new Coordinate[1];
        coords[0] = new Coordinate(lng, lat);
        CoordinateArraySequence coordArraySeq = new CoordinateArraySequence(coords);
        return new Point(coordArraySeq, new GeometryFactory(new PrecisionModel(), 4326));
    }
}
