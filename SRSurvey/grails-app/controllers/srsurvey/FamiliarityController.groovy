package srsurvey

class FamiliarityController {
    static public final int QUESTIONS_PER_PAGE = 20

    def personService
    def loggingService

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
                page: page
        ])
    }

    def redirectToNextUnfinishedPage(Person p) {
        if (p.survey.familiarity == null || p.survey.familiarity.isEmpty()) {
            Set<String> uniques = [] as Set
            for (Question q : p.survey.questions) {
                uniques.add(q.location1)
                uniques.add(q.location2)
            }

            List<String> ordered = uniques as List
            Collections.shuffle(ordered)
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
        }
        int i = 0
        for (LocationFamiliarity lc : p.survey.familiarity) {
            if (lc.familiarity == null) {
                redirect(action: 'show', params: [page : lc.page])
                return
            }
            i++
        }
        redirect(controller: 'familiarity', action: 'show')
    }

    def save(){
        Person p = personService.getForSession(session)
        if (!p.hasConsented) {
            redirect(url : '/')
            return
        }

        int page = params.page as int
        List<Question> toAsk = p.survey.questions.findAll({it.page == page })
        if (toAsk.isEmpty()) throw new IllegalStateException()

        for (Question q : toAsk) {
            q.location1Known = true
            q.location2Known = true
        }

        for (qparam in params){
            if(qparam.key.startsWith("radio")){
                //"This is the question id "+q.key+" and
                // this is the score "+q.value+". Put these into the database."
                String [] tokens = qparam.key.split("_")
                int qid = tokens[1] as int
                Question question = Question.get(qid)
                question.result = qparam.value as int
            }
            if (qparam.key.startsWith("unknown")) {
                String [] tokens = qparam.key.split("_")
                int qid = tokens[1] as int
                int iid = tokens[2] as int
                Question question = Question.get(qid)
                if (iid == question.location1.hashCode().abs()) {
                    question.location1Known = false
                } else if (iid == question.location2.hashCode().abs()) {
                    question.location2Known = false
                } else {
                    throw new IllegalArgumentException("question " + qid + " has no id " + iid)
                }
                question.result = -1.0
                question.save()
            }
        }


        for (Question q : toAsk) {
            q.save()
        }

        if (page == 3) {
            redirect(controller: 'familiarity', action: 'show')
        } else {
            redirect(action: 'show', params: [page: page+1])
        }
    }

    def index() {
        redirect(action: 'show')
    }
}
