package srsurvey

class Person {

    // Their state in the survey
    boolean hasConsented = false

    String code
    String workerId
    String education
    String gender

    static hasOne = [
            survey:Survey
    ]
    static hasMany = [
            homes:LivedInLocation
    ]

    static constraints = {
        workerId nullable: true
        code nullable: true
        survey nullable: true
        education nullable: true
        gender nullable: true
    }

    Person(String workerId){
        this.workerId = workerId
    }

    public int numAnswers() {
        int n = 0
        for (Question q : survey.questions) {
            if (q.hasAnswer()) {
                n++
            }
        }
        return n
    }
}
