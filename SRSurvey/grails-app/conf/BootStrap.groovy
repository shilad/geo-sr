import srsurvey.*

class BootStrap {
    def mturkService
    def questionService
    def cityService

    def init = { servletContext ->
        cityService.init()
        mturkService.init()
        questionService.init()
    }

    def destroy = {

    }
}
