package org.wikibrain.geosr;

import org.h2.util.StringUtils;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class PersonDb {
    public static final File PEOPLE_FILE = new File("dat/people.tsv");
    private static final Logger LOG = Logger.getLogger(PersonDb.class.getName());

    private Map<Integer, Person> byGrailsId = new HashMap<Integer, Person>();
    private Map<String, Person> byAmazonId = new HashMap<String, Person>();
    private CityDb cityDb;
    private CountryDb countryDb;

    public PersonDb() throws IOException {
        this(PEOPLE_FILE);
    }

    public PersonDb(File file) throws IOException {
        cityDb = new CityDb();
        countryDb = new CountryDb();

        ICsvMapReader mapReader = new CsvMapReader(new FileReader(file), CsvPreference.TAB_PREFERENCE);

        // the header columns are used as the keys to the Map
        final String[] header = mapReader.getHeader(true);

        while (true) {
            Map<String, String> fields = mapReader.read(header);
            if (fields == null) break;
            Person p = new Person();
            p.grailsId = Integer.valueOf(fields.get("grailsId"));
            p.amazonId = fields.get("amazonId");
            p.gender = fields.get("gender");
            p.education = fields.get("education");
            p.complete = Boolean.valueOf(fields.get("complete"));
            p.numSr = Integer.valueOf(fields.get("numSr"));
            p.numFamiliarity = Integer.valueOf(fields.get("numSr"));
            p.numValence = Integer.valueOf(fields.get("numValence"));
            p.cities = new HashSet<City>();
            p.countries= new HashSet<Country>();
            for (int i = 0; i <= 100; i++) {
                String value = fields.get("home_" + i);
                if (StringUtils.isNullOrEmpty(value)) {
                    break;
                }
                value = value.trim();
                String tokens[] = value.split("\\|");
                if (tokens.length == 1) {
                    p.countries.add(countryDb.getByName(tokens[0]));
                } else if (tokens.length == 3) {
                    p.cities.add(cityDb.getByName(tokens[0], tokens[1], tokens[2]));
                } else {
                    LOG.info("unknown home: '" + value + "'");
                }
            }
            byGrailsId.put(p.grailsId, p);
            byAmazonId.put(p.amazonId, p);
        }

        LOG.info("read " + byGrailsId.size() + " people");

    }

    public static void main(String args[]) throws IOException {
        PersonDb db = new PersonDb();
    }

}
