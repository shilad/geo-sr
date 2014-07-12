package srsurvey

class LocationValence {
    String location
    Integer valence
    Integer page
    Integer questionNumber

    static belongsTo = [
            survey : Survey
    ]

    static constraints = {
        valence(nullable: true)
    }
}
