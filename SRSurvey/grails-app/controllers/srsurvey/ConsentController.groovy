package srsurvey

class ConsentController {
    def personService
    def loggingService
    def mturkService

    def show() {
        Person p = personService.getForSession(session)
        String workerId = params.workerId ? params.workerId : p?.workerId
        if (workerId != null) {
            p.workerId = workerId
        }

        p.save(flush : true)
        render(view:'show', model: [person : p])
    }

    def save() {
        if (!params.workerId) {
            redirect(action: 'show')
            return
        }
        Person p = personService.getForSession(session)
        p.workerId = params.workerId
        p.hasConsented = true
        p.code = mturkService.getCode()
        p.save()

        loggingService.append(p, request, "consent")

        redirect(controller: 'demographic', action: 'showBasic')
    }
}
