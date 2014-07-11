package srsurvey

class LocationContext {
    String location
    Integer familiarity
    Integer valence

    static belongsTo = [
            person : Person
    ]

    static constraints = {
    }
}
