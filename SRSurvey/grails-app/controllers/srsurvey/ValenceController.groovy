package srsurvey

class ValenceController {
    static public final int QUESTIONS_PER_PAGE = 20

    def personService
    def loggingService

    def instructions() {
        Person p = personService.getForSession(session)
        if (!p.hasConsented || !p.education) {
            redirect(url : '/')
            return
        }
        render(view : 'instructions')
    }

    def show(){

        Person p = personService.getForSession(session)
        if (!p.hasConsented || !p.education) {
            redirect(url : '/')
            return
        }
        if (params.page == null || params.page == '') {
            redirectToNextUnfinishedPage(p)
            return
        }

        int page = params.page as int
        List<LocationValence> toAsk = p.survey.valence.findAll({it.page == page })
        if (toAsk.isEmpty()) throw new IllegalStateException()
        for (LocationValence lf : toAsk) {
            def tokens = [
                    'showvalence',
                    lf.page,
                    lf.questionNumber,
                    lf.location
            ]
            loggingService.append(p, request, tokens)
        }

        render(view:'show', model:[
                questions: toAsk,
                page: page
        ])
    }

    def redirectToNextUnfinishedPage(Person p) {
        if (p.survey.valence == null || p.survey.valence.isEmpty()) {
            Set<String> uniques = [] as Set
            for (Question q : p.survey.questions) {
                uniques.add(q.location1)
                uniques.add(q.location2)
            }

            List<String> ordered = uniques as List
            Collections.shuffle(ordered)
            for (int i = 0; i < ordered.size(); i++) {
                String location = ordered[i]
                LocationValence lf = new LocationValence(
                                                location : location,
                                                page : (i / QUESTIONS_PER_PAGE) as int,
                                                questionNumber: i
                                        )
                p.survey.addToValence(lf)
                loggingService.append(p, request, ['pickvalence', lf.page, lf.questionNumber, lf.location])
            }
            p.save(flush: true)
        }
        int i = 0
        for (LocationValence lc : p.survey.valence) {
            if (lc.valence == null) {
                redirect(action: 'show', params: [page : lc.page])
                return
            }
            i++
        }
        redirect(controller: 'valence', action: 'show')
    }

    def save(){
        Person p = personService.getForSession(session)
        if (!p.hasConsented) {
            redirect(url : '/')
            return
        }

        int page = params.page as int
        List<Question> toAsk = p.survey.valence.findAll({it.page == page })
        if (toAsk.isEmpty()) throw new IllegalStateException()

        for (qparam in params){
            if(qparam.key.startsWith("radio")){
                //"This is the question id "+q.key+" and
                // this is the score "+q.value+". Put these into the database."
                String [] tokens = qparam.key.split("_")
                int lid = tokens[1] as int
                LocationValence lf = LocationValence.get(lid)
                lf.valence = qparam.value as int
            }
        }

        for (LocationValence l : toAsk) {
            l.save()
        }
        int maxPage = p.survey.valence*.page.max()
        if (page == maxPage) {
            redirect(controller: 'finish', action: 'show')
        } else {
            redirect(action: 'show', params: [page: page+1])
        }
    }

    def index() {
        redirect(action: 'instructions')
    }
}
