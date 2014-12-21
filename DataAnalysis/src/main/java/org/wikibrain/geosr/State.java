package org.wikibrain.geosr;

/**
 * @author Shilad Sen
 */
public class State {
    private Country country;
    private String name;
    private String code;

    public State(Country country, String name, String code) {
        this.country = country;
        this.name = name;
        this.code = code;
    }

    public Country getCountry() {
        return country;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        State state = (State) o;

        if (!country.equals(state.country)) return false;
        if (!name.equals(state.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = country.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}
