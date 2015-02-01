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
    /**
     * Compute the longest substring between two strings.
     * From http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Longest_common_substring#Java
     *
     * @param s
     * @param t
     * @return
     */
    public static int longestSubstring(String s, String t) {
        if (s.isEmpty() || t.isEmpty()) {
            return 0;
        }

        int m = s.length();
        int n = t.length();
        int cost = 0;
        int maxLen = 0;
        int[] p = new int[n];
        int[] d = new int[n];

        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < n; ++j) {
                // calculate cost/score
                if (s.charAt(i) != t.charAt(j)) {
                    cost = 0;
                } else {
                    if ((i == 0) || (j == 0)) {
                        cost = 1;
                    } else {
                        cost = p[j - 1] + 1;
                    }
                }
                d[j] = cost;

                if (cost > maxLen) {
                    maxLen = cost;
                }
            } // for {}

            int[] swap = p;
            p = d;
            d = swap;
        }

        return maxLen;
    }
}
