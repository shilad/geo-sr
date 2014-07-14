package srsurvey

import org.wikibrain.spatial.maxima.ExportEnhancer;

/**
 * @author Shilad Sen
 */
class Exporter {
    public static final File PEOPLE = new File("dat/people.tsv");
    public static final File QUESTIONS = new File("dat/questions.tsv");
    public static final File ENHANCED_QUESTIONS = new File("dat/questions.enhanced.tsv");

    def synchronized export() {
        writePeople(PEOPLE.toString())
        writeQuestions(QUESTIONS.toString())
        ExportEnhancer enhancer = new ExportEnhancer();
        enhancer.enhance(PEOPLE, QUESTIONS, ENHANCED_QUESTIONS);
    }


    def writePeople(String path) {
        def w = new File(path).newWriter();

        int maxNumHomes = 0
        Person.all.each { Person p -> maxNumHomes = Math.max(p.homes.size(), maxNumHomes) }

        def header = [
                'grailsId',
                'amazonId',
                'amazonCode',
                'gender',
                'education',
                'complete',
                'numSr',
                'numFamiliarity',
                'numValence'
        ]
        for (int i = 0; i < maxNumHomes; i++) {
            header.add("home_" + i)
        }

        writeTsvLine(w, header)

        Person.all.each({ Person p ->
            def tokens = [
                    p.id,
                    p.workerId,
                    p.code,
                    p.gender,
                    p.education,
                    p.survey.complete(),
                    p.survey.numAnsweredQuestions(),
                    p.survey.numAnsweredFamiliarity(),
                    p.survey.numAnsweredValence(),
            ]

            int i = 0

            for (LivedInLocation home : p.homes) {
                if (home.country != null && home.state != null && home.city != null) {
                    tokens.add(home.country + "|" + home.state + "|" + home.city)
                } else if (home.country != null) {
                    tokens.add(home.country)
                } else {
                    System.err.println("invalid home location for turker id ${p.workerId}")
                }
                i++
            }

            for (; i < maxNumHomes; i++) {
                tokens.add("")
            }

            writeTsvLine(w, tokens)
        })

        w.close()
    }

    def writeQuestions(String path) {
        def w = new File(path).newWriter()

        def fields = [
                'grailsId',
                'amazonId',
                'page',
                'question',
                'location1',
                'location2',
                'relatedness',
                'familiarity1',
                'familiarity2',
                'valence1',
                'valence2',
        ]
        writeTsvLine(w, fields)

        Question.all.each({ Question q ->
            Person p = q.survey.person
            Survey s = q.survey
            def relatedness = q.result == null ? "null" : q.result
            def valence1 = "null"
            def valence2 = "null"
            for (LocationValence v : s.valence) {
                if (v.location == q.location1) {
                    valence1 = v.valence
                }
                if (v.location == q.location2) {
                    valence2 = v.valence
                }
            }
            def familiarity1 = "null"
            def familiarity2 = "null"
            for (LocationFamiliarity f : s.familiarity) {
                if (f.location == q.location1) {
                    familiarity1 = f.familiarity
                }
                if (f.location == q.location2) {
                    familiarity2 = f.familiarity
                }
            }
            def tokens = [
                    p.id,
                    p.workerId,
                    q.page,
                    q.questionNumber,
                    q.location1,
                    q.location2,
                    relatedness,
                    familiarity1,
                    familiarity2,
                    valence1,
                    valence2,
            ]
            writeTsvLine(w, tokens)
        })
        w.close()
    }

    def writeTsvLine(Writer w, List tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) {
                w.write("\t")
            }
            w.write(tokens.get(i).toString().replaceAll("\\s+", " "))
        }
        w.write("\n")
    }
}
