package org.wikibrain.geosr;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
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
public class ExportPairs {
    private static final Logger LOG = Logger.getLogger(ExportPairs.class.getName());

    // Basic components
    private final GeoEnv env;
    private final SRMetric metric;

    public ExportPairs(GeoEnv env) throws IOException, ConfigurationException, DaoException {
        this.env = env;
        this.metric = env.env.getConfigurator().get(SRMetric.class, "ensemble", "language", "en");
    }

    private void writeFamiliarity(File questionFile, File familiarityFile) throws IOException, ConfigurationException, DaoException {
        ResponseReader rr = new ResponseReader(env.pageDb, env.personDb);
        List<Response> responses = rr.read(questionFile);

        final BufferedWriter writer = WpIOUtils.openWriter(familiarityFile);

        List<String> cols = new ArrayList<String>();
        cols.addAll(Arrays.asList("person",
                "location1", "locationId1", "location1Class",
                "location2", "locationId2", "location2Class",
                "familiarity1", "familiarity2", "valence1", "valence2", "relatedness",
                "popRank1", "popRank2"
        ));
        cols.addAll(Arrays.asList(DistanceService.METRICS));
        cols.add("sr");
        cols.add("typeSr");
        writeRow(writer, cols);

        ParallelForEach.loop(responses, new Procedure<Response>() {
            @Override
            public void call(Response r) throws Exception {
                if (!r.getPerson().complete) return;
                List<Object> row = new ArrayList<Object>();
                row.add(r.getGrailsId());
                row.add(r.getPage1().getTitle());
                row.add(r.getPage1().getId());
                row.add(r.getPage1().instanceOf);
                row.add(r.getPage2().getTitle());
                row.add(r.getPage2().getId());;
                row.add(r.getPage2().instanceOf);
                row.add(r.getFamiliarity1());;
                row.add(r.getFamiliarity2());
                row.add(r.getValence1());
                row.add(r.getValence2());
                row.add(r.getRelatedness());
                row.add(r.getPage1().getViewRank());
                row.add(r.getPage2().getViewRank());
                for (String m : DistanceService.METRICS) {
                    row.add(env.distances.getDistance(r.getPerson(), r.getPage1(), m));
                }
                SRResult result = metric.similarity(r.getPage1().getId(), r.getPage2().getId(), false);
                row.add(result == null ? Double.NaN : result.getScore());
                result = null;
                if (r.getPage1().instanceOf != null && r.getPage2().instanceOf != null) {
                    result = metric.similarity(r.getPage1().instanceOf, r.getPage2().instanceOf, false);
                }
                row.add(result == null ? Double.NaN : result.getScore());
                synchronized (writer) {
                    writeRow(writer, row);
                }
            }
        });
    }


    private void writeRow(BufferedWriter writer, Collection newCols) throws IOException {
        int i = 0;
        for (Object o : newCols) {
            if (o instanceof Float) {
                o = ((Float)o).doubleValue();
            }
            if (o instanceof Double) {
                o = String.format("%.3f", (Double) o);
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
        ExportPairs enhancer = new ExportPairs(env);
        enhancer.writeFamiliarity(
                new File("dat/questions.tsv"),
                new File("dat/pair-responses.tsv")
        );
    }
}
