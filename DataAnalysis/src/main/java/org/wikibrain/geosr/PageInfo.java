package org.wikibrain.geosr;

import com.vividsolutions.jts.geom.Point;

/**
 * @author Shilad Sen
 */
public class PageInfo implements Identifiable {
    public static final int SCALE_WEIRD = 0;
    public static final int SCALE_LANDMARK = 1;
    public static final int SCALE_COUNTY = 2;
    public static final int SCALE_COUNTRY = 3;
    public static final int SCALE_STATE = 4;
    public static final int SCALE_CITY = 5;
    public static final int SCALE_NATURAL = 6;

    public int id;
    public String title;
    public int viewRank;
    public int scale;
    public Point point;

    @Override
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getViewRank() {
        return viewRank;
    }

    public int getScale() {
        return scale;
    }

    public String getScaleString() {
        switch (scale) {
            case SCALE_WEIRD: return "weird";
            case SCALE_LANDMARK: return "landmark";
            case SCALE_COUNTY: return "county";
            case SCALE_COUNTRY: return "country";
            case SCALE_STATE: return "state";
            case SCALE_CITY: return "city";
            case SCALE_NATURAL: return "natural";
            default: throw new IllegalArgumentException("unonwn scale: " + scale);
        }
    }
}
