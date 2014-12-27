package org.wikibrain.geosr;

import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class ResponseReader {
    private static final Logger LOG = Logger.getLogger(ResponseReader.class.getName());

    private final PersonDb personDb;
    private final PageInfoDb pageDb;

    public ResponseReader(PageInfoDb pageDb, PersonDb personDb) {
        this.pageDb = pageDb;
        this.personDb = personDb;
    }
    public List<Response> read(File file) throws IOException {
        ICsvBeanReader reader = new CsvBeanReader(new FileReader(file), CsvPreference.TAB_PREFERENCE);

        // the header columns are used as the keys to the Map
        final String[] header = reader.getHeader(true);

        List<Response> result = new ArrayList<Response>();
        while (true) {
            Response r = reader.read(Response.class, header);
            if (r == null) break;

            // Add actual pages
            PageInfo pi1 = pageDb.getByTitle(r.getLocation1());
            PageInfo pi2 = pageDb.getByTitle(r.getLocation2());
            if (pi1 == null) {
                throw new IllegalArgumentException(r.getLocation1());
            }
            if (pi2 == null) {
                throw new IllegalArgumentException(r.getLocation2());
            }
            r.setPage1(pi1);
            r.setPage2(pi2);

            // Add actual people
            r.setPerson(personDb.getByAmazonId(r.getAmazonId()));
            if (r.getPerson() == null) {
                throw new IllegalArgumentException(r.getAmazonId());
            }

            result.add(r);
        }
        LOG.info("read " + result.size() + " responses");

        return result;
    }
}
