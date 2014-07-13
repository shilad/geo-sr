package srsurvey

class Survey {

    String comment
    List questions
    List familiarity
    List valence

    static belongsTo = [person: Person]
    static hasMany = [
            questions: Question,
            familiarity: LocationFamiliarity,
            valence: LocationValence,
    ]

    static transients = ['seenPairs']

    Date dateCreated

    static constraints = {
        comment nullable: true
    }

    static mapping = {
        comment sqlType: "text"
    }

    public Survey(Person person){
        this.person = person
    }

    public boolean complete() {
        return numAnsweredQuestions() == questions.size() && numAnsweredFamiliarity() == familiarity.size() && numAnsweredValence() == valence.size()
    }

    public int numAnsweredQuestions() {
        int i = 0
        for (Question q : questions) {
            if (q.hasAnswer()) {
                i++
            }
        }
        return i
    }

    public int numAnsweredFamiliarity() {
        int i = 0
        for (LocationFamiliarity f : familiarity) {
            if (f.familiarity != null) {
                i++
            }
        }
        return i
    }

    public int numAnsweredValence() {
        int i = 0
        for (LocationValence v : valence) {
            if (v.valence != null) {
                i++
            }
        }
        return i
    }
}
