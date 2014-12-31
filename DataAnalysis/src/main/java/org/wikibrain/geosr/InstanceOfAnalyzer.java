package org.wikibrain.geosr;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataEntity;
import org.wikibrain.wikidata.WikidataFilter;
import org.wikibrain.wikidata.WikidataStatement;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Shilad Sen
 */
public class InstanceOfAnalyzer {
    private final WikidataDao dao;
    private Map<Integer, InstanceOf> instanceOfs = new HashMap<Integer, InstanceOf>();
    private static Set<String> BLACKLIST = new HashSet<String>();

    public InstanceOfAnalyzer(WikidataDao dao) {
        this.dao = dao;
    }

    public void analyze(Collection<PageInfo> pages) throws DaoException {
        for (PageInfo pi : pages) {
            if (pi.rawInstanceOfNames == null) continue;
            for (int i = 0; i < pi.rawInstanceOfNames.size(); i++) {
                String s = pi.rawInstanceOfNames.get(i);
                int id = pi.rawInstanceOfIds.get(i);
                getInstanceOf(s, id).increment();
            }
        }
    }

    public String getBest(PageInfo pi) {
        double bestScore = -1.0;
        InstanceOf bestIo = null;

        // only go up to three levels deep
        for (int ioId : pi.rawInstanceOfIds) {
            InstanceOf io1 = instanceOfs.get(ioId);
            if (io1 == null) continue;

            double s1 = io1.directCount;
            if (!io1.isBlacklisted() && s1 > bestScore) {
                bestScore = s1;
                bestIo = io1;
            }

            // Second level
            for (InstanceOf io2 : io1.getParents()) {
                double s2 = io2.directCount / 3;
                if (!io2.isBlacklisted() && s2 > bestScore) {
                    bestScore = s2;
                    bestIo = io2;
                }

                // Third level
                for (InstanceOf io3 : io2.getParents()) {
                    double s3 = io3.directCount / 9;
                    if (!io3.isBlacklisted() && s3 > bestScore) {
                        bestScore = s3;
                        bestIo = io3;
                    }
                }
            }
        }
        if (bestIo == null) {
            if (pi.rawInstanceOfNames.isEmpty()) {
                return null;
            } else {
                // pick randomly...
                int i = new Random().nextInt(pi.rawInstanceOfNames.size());
                return pi.rawInstanceOfNames.get(i);
            }
        } else {
            return bestIo.getName();
        }
    }

    public void dump() {
        List<Integer> ids = new ArrayList<Integer>(instanceOfs.keySet());
        Collections.sort(ids, new Comparator<Integer>() {
            @Override
            public int compare(Integer id1, Integer id2) {
                return instanceOfs.get(id2).getDirectCount() - instanceOfs.get(id1).getDirectCount();
            }
        });
        for (int id : ids) {
            System.out.println("================================================");
            print(instanceOfs.get(id), 0, new TIntHashSet());
        }
    }

    private void print(InstanceOf io, int indents, TIntSet visited) {
        if (visited.contains(io.getId())) {
            return;
        }
        visited.add(io.getId());
        for (int i = 0; i < indents; i++) {
            System.out.print(" ");
        }
        System.out.println("instance of " + io);
        for (InstanceOf io2 : io.getParents()) {
            print(io2, indents + 4, visited);
        }
    }

    private InstanceOf getInstanceOf(String name, int id) throws DaoException {
        if (instanceOfs.containsKey(id)) {
            return instanceOfs.get(id);
        }
        if (name == null || name.equals("unknown") || name.trim().isEmpty()) {
            return null;
        }

        InstanceOf root = new InstanceOf(id, name);
        instanceOfs.put(id, root);

        // Create parents if necessary
        WikidataFilter filter = new WikidataFilter.Builder()
                .withEntityId(id)
                .withEntityType(WikidataEntity.Type.ITEM)
                .withPropertyId(279)
                .build();
        for (WikidataStatement ws : dao.get(filter)) {
            int parentId = ws.getValue().getItemValue();
            String parentName = dao.getLabel(Language.EN, WikidataEntity.Type.ITEM, parentId);
            InstanceOf parent = getInstanceOf(parentName, parentId);
            if (parent != null) {
                root.addParent(parent);
            }
        }

        return root;
    }

    public static class InstanceOf {
        private int id;
        private String name;
        private Set<InstanceOf> parents = new HashSet<InstanceOf>();
        private int directCount;
        private int traversalCount;

        public InstanceOf(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public void increment() {
            this.directCount += 1;
            incrementTraversal(new TIntHashSet());
        }

        private void incrementTraversal(TIntSet visited) {
            if (visited.contains(id)) {
                return;
            }
            visited.add(id);
            this.traversalCount += 1;
            for (InstanceOf io : parents) {
                io.incrementTraversal(visited);
            }
        }

        public void addParent(InstanceOf parent) {
            this.parents.add(parent);
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Set<InstanceOf> getParents() {
            return parents;
        }

        public int getDirectCount() {
            return directCount;
        }

        public int getTraversalCount() {
            return traversalCount;
        }

        public boolean isBlacklisted() {
            return InstanceOfAnalyzer.isBlacklisted(name);
        }

        @Override
        public String toString() {
            return "InstanceOf{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", directCount=" + directCount +
                    ", traversalCount=" + traversalCount +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            InstanceOf that = (InstanceOf) o;

            if (id != that.id) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    public static boolean isBlacklisted(String name) {
        if (BLACKLIST.isEmpty()) {
            try {
                for (String line : FileUtils.readLines(new File("dat/instance-of-blacklist.txt"))) {
                    BLACKLIST.add(line.trim());
                }
            } catch (IOException e) {
                throw new IllegalStateException("Missing instance of blacklist");
            }
        }
        return name.contains(" of ") || name.contains(" in ") || name.contains(" with ") || BLACKLIST.contains(name);
    }

}
