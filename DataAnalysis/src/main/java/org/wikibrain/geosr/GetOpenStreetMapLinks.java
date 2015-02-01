package org.wikibrain.geosr;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.Title;
import org.wikibrain.utils.WpIOUtils;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataEntity;
import org.wikibrain.wikidata.WikidataFilter;
import org.wikibrain.wikidata.WikidataStatement;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class GetOpenStreetMapLinks {
    public static void main(String args[]) throws DaoException, ConfigurationException, IOException, WikiBrainException {
        GeoEnv env = new GeoEnv(args);
        ResponseReader rr = new ResponseReader(env.pageDb, env.personDb);
        List<Response> responses = rr.read(new File("dat/questions.tsv"));
        Map<PageInfo, Integer> counter = new HashMap<PageInfo, Integer>();
        for (Response r : responses) {
            if (!r.getPerson().complete || r.getRelatedness() <= 0) {
                continue;
            }
            for (PageInfo pi : new PageInfo[] {r.getPage1(), r.getPage2()}) {
                if (!counter.containsKey(pi)) {
                    counter.put(pi, 0);
                }
                counter.put(pi, counter.get(pi) + 1);
            }
        }
        WikidataDao wdd = env.env.getConfigurator().get(WikidataDao.class);
        LocalPageDao lpd = env.env.getConfigurator().get(LocalPageDao.class);
        File file = new File("dat/open-street-map.tsv");
        BufferedWriter writer = WpIOUtils.openWriter(file);
        writer.write("article\twikidata-id\turl\tosm-url\tosm-class\n");
        for (PageInfo pi : counter.keySet()) {
            if (counter.get(pi) < 5) {
                continue;
            }
            WikidataFilter filter = new WikidataFilter.Builder()
                    .withEntityType(WikidataEntity.Type.ITEM)
                    .withEntityId(pi.id)
                    .withPropertyId(402)
                    .build();
            String osmId = null;
            for (WikidataStatement st : wdd.get(filter)) {
                osmId = st.getValue().getStringValue();
            }
            String url = new Title(pi.getTitle(), Language.EN).toUrl();
            String osmUrl = osmId == null ? "" : ("https://www.openstreetmap.org/relation/" + osmId);
            String line =  String.format("%s\t%s\t%s\t%s\t%s\n",
                                        pi.getTitle(), pi.getId(), url, osmUrl, "");
            writer.write(line);
        }
        writer.close();
    }
}
