package srsurvey

import grails.transaction.Transactional
import org.wikibrain.spatial.maxima.SpatialConcept
import org.wikibrain.spatial.maxima.SpatialConceptPair

import javax.servlet.http.HttpServletRequest

@Transactional
class QuestionService {
    def loggingService

    List<String[]> titles = []

    def init() {
        for (String line : new File("dat/IDsToTitles.txt").readLines()) {
            String [] tokens = line.split(":", 2)
            titles.add(tokens)
        }
        log.info("read information for ${titles.size()} spatial concepts")
    }

    def setQuestions(Person p, HttpServletRequest req) {
        if (p.survey == null) {
            throw new IllegalArgumentException()
        }
        List<SpatialConceptPair> pairs = []
        List<String> messages = []
        for (int i = 0; i < 50; i++) {
            SpatialConceptPair pair = new SpatialConceptPair(pickConcept(), pickConcept())
            pairs.add(pair)
            messages.add("p\t${pair.firstConcept}\t${pair.secondConcept}")
        }
        int i = 0
        for (SpatialConceptPair pair : pairs) {
            Question q = new Question(i, (i / 10) as int, pair.firstConcept.title, pair.secondConcept.title, p.survey)
            p.survey.addToQuestions(q)
            i++
        }
        p.save(flush: true)
        loggingService.appendMany(p, req, messages)
    }

    def pickConcept() {
        Random random = new Random()
        String [] tokens = titles[random.nextInt(titles.size())]
        SpatialConcept concept = new SpatialConcept(tokens[0] as int, tokens[1])
        return concept
    }
}
