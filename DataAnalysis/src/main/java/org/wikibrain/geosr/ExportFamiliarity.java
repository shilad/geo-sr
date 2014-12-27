package org.wikibrain.geosr;

import gnu.trove.set.TIntSet;
import org.apache.commons.lang3.ArrayUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.distance.*;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.logging.Logger;

/**
* @author Shilad Sen
*/
public class ExportFamiliarity {
    private static final Logger LOG = Logger.getLogger(ExportFamiliarity.class.getName());

    // Basic components
    private final GeoEnv env;

    public ExportFamiliarity(GeoEnv env) throws IOException, ConfigurationException, DaoException {
        this.env = env;
    }

    private void writeFamiliarity(File questionFile, File familiarityFile) throws IOException, ConfigurationException, DaoException {
        ResponseReader rr = new ResponseReader(env.pageDb, env.personDb);
        List<Response> responses = rr.read(questionFile);
        Set<String> written = new HashSet<String>();

        BufferedWriter writer = WpIOUtils.openWriter(familiarityFile);

        List<String> cols = new ArrayList<String>();
        cols.addAll(Arrays.asList("worker", "location", "locationId", "locationPopRank"));
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
            if (i++ > 0) writer.write("\t");
            writer.write(o.toString());
        }
        writer.write("\n");
    }

    public static void main(String args[]) throws Exception {
        GeoEnv env = new GeoEnv(args);
        ExportFamiliarity enhancer = new ExportFamiliarity(env);
        enhancer.writeFamiliarity(
                new File("dat/questions.tsv"),
                new File("dat/familiarity.tsv")
        );
    }
}
