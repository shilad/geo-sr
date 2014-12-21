package org.wikibrain.geosr;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class StateDb {
    private static Logger LOG = Logger.getLogger(StateDb.class.getName());
    public static final File PATH = new File("dat/stateCodes.txt");

    private Map<String, State> byCode = new HashMap<String, State>();
    private CountryDb countryDb = new CountryDb();

    public StateDb() throws FileNotFoundException {
        Scanner scan = new Scanner(PATH);
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            String[] tokens = line.split("\t");
            String countryCode = tokens[0].split("\\.")[0];
            if (countryDb.isInteresting(countryCode)) {
                Country c = countryDb.getByCode(countryCode);
                State s = new State(c, tokens[1], tokens[0]);
                byCode.put(s.getCode(), s);
            }
        }
        scan.close();
        LOG.info("read " + byCode.size() + " state codes");
    }

    public State getByCode(Country country, String code) {
        return getByCode(country.getCode() + "." + code);
    }

    public State getByCode(String code) {
        if (code.equals("IN.00")) code = "IN.33";
        if (byCode.containsKey(code)) {
            return byCode.get(code);
        } else {
            throw new IllegalArgumentException(code);
        }
    }

    public static void main(String args[]) throws FileNotFoundException {
        StateDb sdb = new StateDb();
    }
}
