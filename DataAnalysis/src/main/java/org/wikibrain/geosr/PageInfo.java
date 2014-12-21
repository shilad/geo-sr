package org.wikibrain.geosr;

/**
 * @author Shilad Sen
 */
public class PageInfo {
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
}
