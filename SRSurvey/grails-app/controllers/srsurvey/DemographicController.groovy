package srsurvey

import grails.converters.JSON

class DemographicController {
    def personService
    def loggingService
    def questionService
    def cityService

    def showBasic() {
        Person p = personService.getForSession(session)
        if (!p.hasConsented) {
            redirect(url : '/')
            return
        }
        render(view: 'showBasic', model: [person: p])
    }

    def saveBasic() {
        Person p = personService.getForSession(session)
        if (p == null) {
            throw new NullPointerException();
        }
        if (params['gender-no']) {
            p.gender = "NO_RESPONSE"
        } else {
            p.gender = params.gender
        }
        p.education = params.education
        for (String country : params.list('country')) {
            LivedInLocation location = new LivedInLocation(country: country)
            p.addToHomes(location)
        }

        p.save(flush : true)

        for (LivedInLocation l : p.homes) {
            loggingService.append(p, request, "country\t${l.country}")
        }
        loggingService.append(p, request, "gender\t${p.gender}")
        loggingService.append(p, request, "education\t${p.education}")
        redirect(action: 'showHomes')
    }

    def showHomes() {
        Person p = personService.getForSession(session)
        if (!p.hasConsented) {
            redirect(url: '/')
            return
        }


        // Get the next country. If there are done, move onto ratings!
        Map<String, Integer> countryCounts = [:]
        for (LivedInLocation lil : p.homes) {
            if (!countryCounts.containsKey(lil.country)) {
                countryCounts[lil.country] = 0
            }
            if (lil.city != null) {
                countryCounts[lil.country]++
            }
        }

        String nextCountry = null
        for (String country : countryCounts.keySet()) {
            if (countryCounts[country] == 0) {
                nextCountry = country
                break
            }
        }

        if (nextCountry == null) {
            redirect(controller : 'rating', action : 'show')
        }  else {
            render(view: 'showHomes', model: [person: p, country : nextCountry])
        }

    }

    def saveHomes() {
        Person p = personService.getForSession(session)
        if (!p.hasConsented) {
            redirect(url : '/')
            return
        }

        for (String s : params.city) {
            if (s == "FAKE") continue
            String [] tokens = s.split("\\|")
            LivedInLocation ll = new LivedInLocation(
                    city: tokens[0],
                    state: tokens[1],
                    country: tokens[2],
                    latitude: tokens[3] as double,
                    longitude: tokens[4] as double

            )
            p.addToHomes(ll)
            loggingService.append(p, request, "home\t${ll.city}\t${ll.state}\t${ll.country}\t${ll.latitude}\t${ll.longitude}")
        }
        p.save(flush: true)

        redirect(action: 'showHomes')
    }

    def autocompleteCity() {
        def result = []
        for (String [] cityInfo : cityService.autocomplete(params.country, params.term)) {
            result.add([label : cityInfo[0], value: cityInfo[1]])
        }
        render result as JSON
    }

}