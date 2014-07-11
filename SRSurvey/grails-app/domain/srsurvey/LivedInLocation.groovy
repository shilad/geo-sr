package srsurvey

class LivedInLocation {
    String country
    String state
    String city
    double latitude
    double longitude

    static constraints = {
        country(nullable: false)
        city(nullable: true)
        state(nullable: true)
        latitude(nullable: true)
        longitude(nullable: true)
    }

    static belongsTo = [person:Person]
}
