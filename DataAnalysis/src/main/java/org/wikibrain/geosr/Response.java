package org.wikibrain.geosr;

/**
 * @author Shilad Sen
 */
public class Response {
    // Information about subject
    private Person person;
    private int grailsId;
    private String amazonId;

    // Information about question order
    private int page;
    private int question;

    // Information about locations
    private PageInfo page1;
    private String location1;
    private PageInfo page2;
    private String location2;

    // User responses
    private double relatedness;
    private int familiarity1;
    private int familiarity2;
    private int valence1;
    private int valence2;

    public int getGrailsId() {
        return grailsId;
    }

    public String getAmazonId() {
        return amazonId;
    }

    public int getPage() {
        return page;
    }

    public int getQuestion() {
        return question;
    }

    public String getLocation1() {
        return location1;
    }

    public String getLocation2() {
        return location2;
    }

    public double getRelatedness() {
        return relatedness;
    }

    public int getFamiliarity1() {
        return familiarity1;
    }

    public int getFamiliarity2() {
        return familiarity2;
    }

    public int getValence1() {
        return valence1;
    }

    public int getValence2() {
        return valence2;
    }

    public Person getPerson() {
        return person;
    }

    public PageInfo getPage1() {
        return page1;
    }

    public PageInfo getPage2() {
        return page2;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public void setPage2(PageInfo page2) {
        this.page2 = page2;
    }

    public void setPage1(PageInfo page1) {
        this.page1 = page1;
    }

    public void setGrailsId(String grailsId) {
        this.grailsId = Integer.valueOf(grailsId);
    }

    public void setAmazonId(String amazonId) {
        this.amazonId = amazonId;
    }

    public void setPage(String page) {
        this.page = Integer.valueOf(page);
    }

    public void setQuestion(String question) {
        this.question = Integer.valueOf(question);
    }

    public void setLocation1(String location1) {
        this.location1 = location1;
    }

    public void setLocation2(String location2) {
        this.location2 = location2;
    }

    public void setRelatedness(String relatedness) {
        if (!relatedness.equals("null")) {
            this.relatedness = Double.valueOf(relatedness);
        }
    }

    public void setFamiliarity1(String familiarity1) {
        if (!familiarity1.equals("null")) {
            this.familiarity1 = Integer.valueOf(familiarity1);
        }
    }

    public void setFamiliarity2(String familiarity2) {
        if (!familiarity2.equals("null")) {
            this.familiarity2 = Integer.valueOf(familiarity2);
        }
    }

    public void setValence1(String valence1) {
        if (!valence1.equals("null")) {
            this.valence1 = Integer.valueOf(valence1);
        }
    }

    public void setValence2(String valence2) {
        if (!valence2.equals("null")) {
            this.valence2 = Integer.valueOf(valence2);
        }
    }
}
