package srsurvey

class MturkService {

    List<String[]> codes = []

    public def init() {
        Set<String> usedCodes = new HashSet<String>(Person.findAllByCodeIsNotNull().code)
        for (String line : new File("dat/codes.txt")) {
            String code = line.trim()
            if (!usedCodes.contains(code)) {
                codes.add(code)
            }
        }
        println("loaded ${codes.size()} remaining codes")
    }

    public synchronized def getCode() {
        return codes.remove(0)
    }
}
