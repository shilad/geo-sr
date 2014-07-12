package srsurvey

class LocationFamiliarity {
    String location
    Integer familiarity
    Integer page
    Integer questionNumber

    static belongsTo = [
            survey : Survey
    ]

    static constraints = {
        familiarity(nullable: true)
    }
}
