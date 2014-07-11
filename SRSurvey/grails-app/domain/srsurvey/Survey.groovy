package srsurvey

class Survey {

    String comment
    List questions

    static belongsTo = [person: Person]
    static hasMany = [
            questions: Question,
            locations: LocationContext
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
