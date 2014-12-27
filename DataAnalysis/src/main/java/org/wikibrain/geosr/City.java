package org.wikibrain.geosr;

import com.vividsolutions.jts.geom.Point;

/**
 * @author Shilad Sen
 */
public class City implements Identifiable {
    private String name;
    private State state;
    private Point location;
    private int population;
    private int id;

    public City(String name, State state, Point location, int population, int id) {
        this.name = name;
        this.state = state;
        this.location = location;
        this.population = population;
        this.id = id;
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

    @Override
    public String toString() {
        return "City{" +
                "name='" + name + '\'' +
                ", state=" + state.getName() +
                ", country=" + state.getCountry().getName() +
                ", location=" + location +
                ", population=" + population +
                '}';
    }

    @Override
    public int getId() {
        return id;
    }
}
