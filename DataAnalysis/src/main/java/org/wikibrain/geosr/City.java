package org.wikibrain.geosr;

import com.vividsolutions.jts.geom.Point;

/**
 * @author Shilad Sen
 */
public class City {
    private String name;
    private State state;
    private Point location;
    private int population;

    public City(String name, State state, Point location, int population) {
        this.name = name;
        this.state = state;
        this.location = location;
        this.population = population;
    }

    public String getName() {
        return name;
    }

    public Country getCountry() {
        return state.getCountry();
    }

    public State getState() {
        return state;
    }

    public Point getLocation() {
        return location;
    }

    public int getPopulation() {
        return population;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        City city = (City) o;

        if (!name.equals(city.name)) return false;
        if (!state.equals(city.state)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + state.hashCode();
        return result;
    }
}
