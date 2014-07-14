package srsurvey

class FamiliarityController {
    static public final int QUESTIONS_PER_PAGE = 15

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
        List<LocationFamiliarity> toAsk = p.survey.familiarity.findAll({it.page == page })
        if (toAsk.isEmpty()) throw new IllegalStateException()
        int maxPage = p.survey.familiarity*.page.max()

        for (LocationFamiliarity lf : toAsk) {
            def tokens = [
                    'showFamiliarity',
                    lf.page,
                    lf.questionNumber,
                    lf.location
            ]
            loggingService.append(p, request, tokens)
        }

        render(view:'show', model:[
                questions: toAsk,
                numPages : (maxPage + 1),
                page: page
        ])
    }

    def redirectToNextUnfinishedPage(Person p) {
        if (p.survey.familiarity == null || p.survey.familiarity.isEmpty()) {
            Set<String> uniques = [] as Set
            for (Question q : p.survey.questions) {
                if (q.result != null && q.result >= 0) {
                    uniques.add(q.location1)
                    uniques.add(q.location2)
                }
            }

            List<String> ordered = uniques as List
            Collections.shuffle(ordered)
            if (ordered.size() > QUESTIONS_PER_PAGE + 4) {
                ordered.add(ordered.size() - 4, ordered.get((QUESTIONS_PER_PAGE / 2) as int))
            }
            for (int i = 0; i < ordered.size(); i++) {
                String location = ordered[i]
                LocationFamiliarity lf = new LocationFamiliarity(
                                                location : location,
                                                page : (i / QUESTIONS_PER_PAGE) as int,
                                                questionNumber: i
                                        )
                p.survey.addToFamiliarity(lf)
                loggingService.append(p, request, ['pickFamiliarity', lf.page, lf.questionNumber, lf.location])
            }
            p.save(flush: true)
            println("here ${p.survey.familiarity.size()}")
        }
        int i = 0
        for (LocationFamiliarity lc : p.survey.familiarity) {
            if (lc.familiarity == null) {
                redirect(action: 'show', params: [page : lc.page])
                return
            }
            i++
        }
        redirect(action: 'show', params: [page : 0])
    }

    def save(){
        Person p = personService.getForSession(session)
        if (!p.hasConsented) {
            redirect(url : '/')
            return
        }

        int page = params.page as int
        List<LocationFamiliarity> toAsk = p.survey.familiarity.findAll({it.page == page })
        if (toAsk.isEmpty()) {
            redirect(controller: 'valence', action: 'instructions')
            return
        }

        for (qparam in params){
            if(qparam.key.startsWith("radio")){
                //"This is the question id "+q.key+" and
                // this is the score "+q.value+". Put these into the database."
                String [] tokens = qparam.key.split("_")
                int lid = tokens[1] as int
                LocationFamiliarity lf = LocationFamiliarity.get(lid)
                lf.familiarity = qparam.value as int
            }
        }

        for (LocationFamiliarity l : toAsk) {
            l.save()
        }

        int maxPage = p.survey.familiarity*.page.max()
        if (page == maxPage) {
            redirect(controller: 'valence', action: 'instructions')
        } else {
            redirect(action: 'show', params: [page: page+1])
        }
    }

    def index() {
        redirect(action: 'instructions')
    }
}
