grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"

// uncomment (and adjust settings) to fork the JVM to isolate classpaths
//grails.project.fork = [
//   run: [maxMemory:1024, minMemory:64, debug:false, maxPerm:256]
//]

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
    }
    log "info" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
    legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility

    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()

        // uncomment these (or add new ones) to enable remote dependency resolution from public Maven repositories
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }

    dependencies {
        runtime 'org.postgresql:postgresql:9.3-1100-jdbc4'

        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes e.g.
        compile("autocomplete-server:autocomplete-server:0.5")

        // runtime 'mysql:mysql-connector-java:5.1.22'
        compile("org.wikibrainapi:wikibrain-spatial:0.2-SNAPSHOT")  {
            excludes(
                    [ group: 'org.hibernate'],
                    [ group: 'org.hibernate.common' ],
                    [ group: 'org.hibernate.javax.persistence' ],
            )
        }
        compile("org.wikibrainapi:wikibrain-sr:0.2-SNAPSHOT") {
            excludes(
                    [ group: 'org.hibernate'],
                    [ group: 'org.hibernate.common' ],
                    [ group: 'org.hibernate.javax.persistence' ],
            )
        }
    }

    plugins {
        runtime ":jquery:1.8.3"
        runtime ":jquery-ui:1.8.24"
        runtime ":resources:1.1.6"

        // Uncomment these (or add new ones) to enable additional resources capabilities
        //runtime ":zipped-resources:1.0"
        //runtime ":cached-resources:1.0"
        //runtime ":yui-minify-resources:0.1.5"
        if (System.getProperty("noTomcat") == null) {
            build ':tomcat:7.0.50.1'
            runtime ":database-migration:1.3.2"
        }
        runtime ':hibernate:3.6.10.8'

        runtime ":compass-sass:0.7"

        compile ':cache:1.0.1'
    }
}
