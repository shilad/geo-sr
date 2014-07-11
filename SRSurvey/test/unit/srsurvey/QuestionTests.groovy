package srsurvey



import grails.test.mixin.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@Mock([Survey])
@TestFor(Question)
class QuestionTests {

    void testQuestion() {
        Question q1 = new Question(12,1,)

        q1.setLocation1("location1")
        q1.setLocation2("location2")

        Survey s = new Survey()

        q1.setSurvey(s)
        q1.save(flush: true, failOnError: true)
    }
}
