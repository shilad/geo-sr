package srsurvey

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(QuestionService)
@Mock([Person, Survey, LivedInLocation, Question])
public class QuestionServiceSpec {

    @Before
    public void setup() {
        service.init()
    }

    @After
    public void cleanup() {
    }

    @Test
    public void testSetQuestions() {
        Person p = new Person()
        p.survey = new Survey()
        p.homes = [
            new LivedInLocation(
                country : "United States of America",
                state : "Minnesota",
                city : "Minneapolis"
            )
        ]
        service.setQuestions(p, null)
        for (Question q : p.survey.questions) {
            System.out.println("question is ${q.location1}, ${q.location2}")
        }
    }
}
