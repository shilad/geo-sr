package srsurvey

import grails.transaction.Transactional
import org.wikibrain.spatial.maxima.OrderQuestions
import org.wikibrain.spatial.maxima.SpatialConcept
import org.wikibrain.spatial.maxima.SpatialConceptPair

import javax.servlet.http.HttpServletRequest

@Transactional
class QuestionService {
    def loggingService
    OrderQuestions orderer;
    Map<String, Set<Integer>> neighbors;

    def init() {
        orderer = new OrderQuestions();
        loadNeighborFile(new File("citiesToNeighbors4.txt"));
        log.info("read neighbors for ${neighbors.size()} cities")
        println("read neighbors for ${neighbors.size()} cities")
    }

    /**
     * Read in a plain-text file whose format consists of the (tab separated) fields
     * city_country,city_state,city_name    city_population  city_neighbor_1  city_neighbor_2  ...
     * for each city.
     * <p/>
     * Also modifies the cityPopulations map.
     *
     * @param file The file we are loading from
     * @return Map from city id (city_country,city_state,city_name) to city neighbors
     */
    public void loadNeighborFile(File file) {
        neighbors = new HashMap<String, Set<Integer>>();
        try {
            Scanner scan = new Scanner(file, "UTF-8");
            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                String[] array = line.split("\t");
                Set<Integer> set = new HashSet<Integer>();
                String id = array[0];
                for (int i = 2; i < array.length; i++) {
                    set.add(Integer.parseInt(array[i]));
                }
                neighbors.put(id, set);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    def setQuestions(Person p, HttpServletRequest req) {
        if (p.survey == null) {
            throw new IllegalArgumentException()
        }
        Set<Integer> knownIds = new HashSet<Integer>();
        for (LivedInLocation lil : p.homes) {
            String key = lil.country +"," + lil.state + "," + lil.city;
            if (!neighbors.containsKey(key)) {
                log.warn("unknown neighbor location: " + key);
                continue;
            }
            knownIds.addAll(neighbors.get(key));
        }

        List<String> messages = []
        List<SpatialConceptPair>[] pages = orderer.getQuestions(knownIds as List, Person.count);
        int i = 0
        for (int pageNum = 0; pageNum < pages.length; pageNum++) {
            for (SpatialConceptPair pair : pages[pageNum]) {
                Question q = new Question(i, pageNum, pair.firstConcept.title, pair.secondConcept.title, p.survey)
                q.maybeSwap()
                p.survey.addToQuestions(q)
                messages.add("question\t${pair.firstConcept.title}\t${pair.secondConcept.title}")
                i++
            }
        }

        p.save(flush: true)
        if (req != null) {
            loggingService.appendMany(p, req, messages)
        }
    }
}
