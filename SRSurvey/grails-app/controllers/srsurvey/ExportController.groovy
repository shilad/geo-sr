package srsurvey

class ExportController {

    def index() {
        Exporter exporter = new Exporter()
        exporter.export()
        render('exported!')
    }
}
