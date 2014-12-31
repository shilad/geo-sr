package org.wikibrain.geosr;

import org.apache.commons.lang3.StringUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
* @author Shilad Sen
*/
public class ExportLocations {
    private static final Logger LOG = Logger.getLogger(ExportLocations.class.getName());

    // Basic components
    private final GeoEnv env;

    public ExportLocations(GeoEnv env) throws IOException, ConfigurationException, DaoException {
        this.env = env;
    }

    private void writeFamiliarity(File questionFile, File familiarityFile) throws IOException, ConfigurationException, DaoException {
        ResponseReader rr = new ResponseReader(env.pageDb, env.personDb);
        List<Response> responses = rr.read(questionFile);
        Set<String> written = new HashSet<String>();

        BufferedWriter writer = WpIOUtils.openWriter(familiarityFile);

        List<String> cols = new ArrayList<String>();
        cols.addAll(Arrays.asList("person", "location", "locationId", "instanceOf", "instanceOfRaw", "familiarity", "valence", "popRank"));
        cols.addAll(Arrays.asList(DistanceService.METRICS));
        writeRow(writer, cols);

        for (Response r : responses) {
            if (!r.getPerson().complete) continue;
            String key1 = r.getAmazonId() + "@" + r.getLocation1();
            String key2 = r.getAmazonId() + "@" + r.getLocation2();
            if (!written.contains(key1)) {
                written.add(key1);
                List<Object> row = new ArrayList<Object>();
                row.add(r.getGrailsId());
                row.add(r.getPage1().getTitle());
                row.add(r.getPage1().getId());
                row.add(r.getPage1().instanceOf);
                row.add(StringUtils.join(r.getPage1().rawInstanceOfNames, "|"));
                row.add(r.getFamiliarity1());
                row.add(r.getValence1());
                row.add(r.getPage1().getViewRank());
                for (String m : DistanceService.METRICS) {
                    row.add(env.distances.getDistance(r.getPerson(), r.getPage1(), m));
                }
                writeRow(writer, row);
            }
            if (!written.contains(key2)) {
                written.add(key2);
                List<Object> row = new ArrayList<Object>();
                row.add(r.getGrailsId());
                row.add(r.getPage2().getTitle());
                row.add(r.getPage2().getId());
                row.add(r.getPage2().instanceOf);
                row.add(StringUtils.join(r.getPage2().rawInstanceOfNames, "|"));
                row.add(r.getFamiliarity2());
                row.add(r.getValence2());
                row.add(r.getPage2().getViewRank());
                for (String m : DistanceService.METRICS) {
                    row.add(env.distances.getDistance(r.getPerson(), r.getPage2(), m));
                }
                writeRow(writer, row);
            }
        }
    }


    private void writeRow(BufferedWriter writer, Collection newCols) throws IOException {
        int i = 0;
        for (Object o : newCols) {
            if (o instanceof Float) {
                o = ((Float)o).doubleValue();
            }
            if (o instanceof Double) {
                o = String.format("%.3f", (Double)o);
            }
            if (o == null) {
                o = "null";
            }
            if (i++ > 0) writer.write("\t");
            writer.write(o.toString());
        }
        writer.write("\n");
    }

    public static void main(String args[]) throws Exception {
        GeoEnv env = new GeoEnv(args);
        ExportLocations enhancer = new ExportLocations(env);
        enhancer.writeFamiliarity(
                new File("dat/questions.tsv"),
                new File("dat/location-responses.tsv")
        );
    }
}
