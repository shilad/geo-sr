package org.wikibrain.geosr;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Shilad Sen
 */
public class CountryDb {
    public static Country[] COUNTRIES = new Country[]{
            new Country("United States of America", "US"),
            new Country("Canada", "CA"),
            new Country("Brazil", "BR"),
            new Country("Pakistan", "PK"),
            new Country("India", "IN"),
            new Country("France", "FR"),
            new Country("Spain", "ES"),
            new Country("United Kingdom", "GB"),
            new Country("Australia", "AU")
    };

    public boolean isInteresting(String nameOrCode) {
        for (Country c : COUNTRIES) {
            if (c.getName().equals(nameOrCode) || c.getCode().equals(nameOrCode)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getCodes() {
        Set<String> result = new HashSet<String>();
        for (Country c : COUNTRIES) {
            result.add(c.getCode());
        }
        return result;
    }

    public Set<String> getNames() {
        Set<String> result = new HashSet<String>();
        for (Country c : COUNTRIES) {
            result.add(c.getName());
        }
        return result;
    }

    public Country getByName(String name) {
        for (Country c : COUNTRIES) {
            if (c.getName().equalsIgnoreCase(name)) {
                return c;
            }
        }
        throw new IllegalArgumentException(name);
    }

    public Country getByCode(String code) {
        for (Country c : COUNTRIES) {
            if (c.getCode().equalsIgnoreCase(code)) {
                return c;
            }
        }
        throw new IllegalArgumentException(code);
    }
};
