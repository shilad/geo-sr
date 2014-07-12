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
}
