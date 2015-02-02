package org.wikibrain.geosr;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;

import java.io.IOException;

/**
 * @author Shilad Sen
 */
public class Sandbox {
    public static void main(String args[]) throws DaoException, ConfigurationException, IOException {
        Env env = EnvBuilder.envFromArgs(args);

    }
}
