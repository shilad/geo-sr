package srsurvey

class Question {

    int page    // which page of questions this will appear on

    Double result

    Integer questionNumber
    String location1
    String location2
    Boolean location1Known
    Boolean location2Known

    static belongsTo = [survey:Survey]

    static constraints = {
        result nullable: true
        survey nullable: true
        location1Known nullable: true
        location2Known nullable: true
    }

    Question(Integer questionNumber, Integer page, String location1, String location2, Survey survey) {
        this.questionNumber = questionNumber
        this.page = page
        this.location1 = location1
        this.location2 = location2
        this.survey = survey
    }

    public boolean hasAnswer() {
        return result != null || location1Known != null || location2Known != null
    }

    public void maybeSwap() {
        if (new Random().nextDouble() < 0.5) {
            String iTmp = location1
            Boolean bTmp = location1Known
            location1 = location2
            location1Known = location2Known
            location2 = iTmp
            location2Known = bTmp
        }
    }
}
