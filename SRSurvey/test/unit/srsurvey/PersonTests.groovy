package srsurvey
import grails.test.mixin.*
/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@Mock([Survey, Question])
@TestFor(Person)
class PersonTests {
    void testPerson()
    {
        //test variables
        Person p = new Person()
        p.email="johnSmith@aol.com"
        p.save(flush:true)
        assertEquals(p.email,"johnSmith@aol.com")
        assertNotSame(p.email,"derp@derp.com")
    }
}
