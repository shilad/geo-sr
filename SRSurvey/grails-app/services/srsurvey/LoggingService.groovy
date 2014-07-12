package srsurvey

import javax.servlet.http.HttpServletRequest

class LoggingService {

    private static File output = new File("./phrasepairs-log.txt")

    def append(Person p, HttpServletRequest request, List tokens) {
        append(p, request, tokens.collect({it.toString()}).join('\t'))
    }

    def append(Person p, HttpServletRequest request, String message) {
        appendMany(p, request, [message])
    }

    def appendMany(Person p, HttpServletRequest request, List<String> messages) {
        String id = (p.id) ? p.id : "unknown"
        String workerId = (p.workerId) ? p.workerId : "unknown"
        String ip = getIpAddress(request)
        String tstamp = new Date().format("yyyy-MM-dd hh:mm:ss")

        synchronized (output) {
            for (String message : messages) {
                message = message.replace("\n", " ")
                output.append("$tstamp\t$ip\t$id\t$workerId\t$message\n")
            }
        }
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddr = request.getHeader("X-Forwarded-For")
        if (ipAddr == null) {
            ipAddr = request.getRemoteAddr()
        }
        return ipAddr
    }
}
