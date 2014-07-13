package srsurvey

class RatingController {
    static public final int QUESTIONS_PER_PAGE = 10

    def questionService
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
        List<Question> toAsk = p.survey.questions.findAll({it.page == page })
        if (toAsk.isEmpty()) throw new IllegalStateException()
        for (Question q : toAsk) {
            def tokens = [
                    'showRating',
                    q.id,
                    q.page,
                    q.questionNumber,
                    q.location1,
                    q.location2,
            ]
            loggingService.append(p, request, tokens.collect({it.toString()}).join('\t'))
        }

        render(view:'show', model:[
                questions: toAsk,
                page: page
        ])
    }

    def redirectToNextUnfinishedPage(Person p) {
        if (p.survey.questions == null || p.survey.questions.isEmpty()) {
            questionService.setQuestions(p, request)
        }
        for (Question q : p.survey.questions) {
            if (!q.hasAnswer()) {
                redirect(action: 'show', params: [page : q.page])
                return
            }
        }
        redirect(controller: 'familiarity', action: 'instructions')
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
        redirect(action: 'instructions')
    }
}
