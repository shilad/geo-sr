package org.wikibrain.geosr;

import org.wikibrain.core.dao.DaoException;
import org.wikibrain.sr.SRResultList;

/**
 * @author Shilad Sen
 */
public interface NeighborSR {
    public SRResultList mostSimilar(int conceptId) throws DaoException;
}
