package srsurvey

class FinishController {
    def personService
    def loggingService

    def show() {
        Person p = personService.getForSession(session)
        render(view: "show", model: [person:p])
    }

    def save() {
        Person p = personService.getForSession(session)
        p.survey.comment = params.comments
        p.save(flush : true)
        loggingService.append(p, request, "comments\t" + params.comments)
        loggingService.append(p, request, 'finished')
        render(view: "thanks", model: [person: p])
    }

    // for testing only
    def thanks() {
        render(view: "thanks")
    }
}


