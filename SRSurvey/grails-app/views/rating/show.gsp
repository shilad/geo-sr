<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main"/>
    <r:require modules="core" />
    <title>Geographic relatedness survey: Concept relatedness, page ${page + 3} of 15</title>

</head>
<body>
<div class="rounded-corners rating" id="main-container">
    <h1>Rate concept relatedness (page ${page + 3} of 15)</h1>
    <div id="instructions">
        Please rate how related each pair of concepts is.
        When you finish rating all pairs, click "next".<br/><br/>
    </div>
    <g:form action="save" name="rating-form" method="post" params="${[page: page]}">
        <div id="ratings">
            <g:each status="i" in="${questions}" var="q">
                <div class="row ${ (i % 2) == 0 ? 'odd' : 'even'} num${i}" id="${q.questionNumber}" >
                    <table>
                        <tbody>
                        <tr class="first">
                            <td class="interest" colspan="2">
                                <a href="http://en.wikipedia.org/wiki/${q.location1.replaceAll(' ', '_').encodeAsURL()}" target="_blank">${q.location1}</a>
                            </td>
                            <td rowspan="2">
                                <div class="rounded-corners rating-bars">
                                    <table>
                                        <tr>
                                            <td>
                                                <label>0 <input type="radio" name="${"radio_"+q.id}" value="1"
                                                <g:if test="${q.result == 1}">checked</g:if>/></label>
                                            </td>
                                            <td>
                                                <label>1 <input type="radio" name="${"radio_"+q.id}" value="2"
                                                         <g:if test="${q.result == 2}">checked</g:if>/></label>
                                            </td>
                                            <td>
                                                <label>2 <input type="radio" name="${"radio_"+q.id}" value="3"
                                                         <g:if test="${q.result == 3}">checked</g:if>/></label>
                                            </td>
                                            <td>
                                                <label>3 <input type="radio" name ="${"radio_"+q.id}" value="4"
                                                         <g:if test="${q.result == 4}">checked</g:if>/></label>
                                            </td>
                                            <td>
                                                <label>4 <input type="radio" name="${"radio_"+q.id}" value="5"
                                                         <g:if test="${q.result == 5}">checked</g:if>/></label>
                                            </td>
                                        </tr>
                                    </table>
                                </div>
                                <div class="no-assoc">
                                    Not related
                                </div>
                                <div class="str-assoc">
                                    Strongly related
                                </div>
                            </td>
                        </tr>
                        <tr class="second">
                            <td class="interest" colspan="2">
                                <a href="http://en.wikipedia.org/wiki/${q.location2.replaceAll(' ', '_').encodeAsURL()}" target="_blank">${q.location2}</a>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </g:each>
        </div>

        <div class="continue">
            <button href="#" class="myButton" id="continue-button">Next</button>
        </div>
    </g:form>

</div>

</body>
</html>

<r:script>

    // Hack: waits 200 millis for everything to clear
    function logRating(div) {
        window.setTimeout(function() {
            var qid = div.prop("id");
            var interests = div.find("td.interest");
            var checkboxes = div.find("input[type='checkbox']");
            var rating = div.find("input[type='radio']:checked").val();
            ajaxLog("rating",
                    qid,
                    $(interests[0]).text(),
                    $(interests[1]).text(),
                    $(checkboxes.get(0)).prop('checked'),
                    $(checkboxes.get(1)).prop('checked'),
                    rating
            );
        }, 200);
    }

    $(document).ready(function() {
//        $("input[value='1']").attr('checked', 'checked');
        $('form').on('submit', function(e) {
            var isComplete = true;
            $("div.row").each(function () {
                //console.log($(this).find('input:checked'));
                if ($(this).find("input[type='radio']:checked").length == 0) {
                    $(this).addClass("error");
                    isComplete = false;
                }
            });
            if (!isComplete) {
                alert('Please enter a rating for each row\nor check the "I don\'t know" box.');
            }
            return isComplete;
        });
        $("input[type='checkbox']").click(function () {
            var $this = $(this);
            var row = $this.parents(".row");
            logRating(row);
        });
        $("input[type='radio']").click(function () {
            var $this = $(this);
            if ($this.is(':checked')) {
                var row = $this.parents(".row");
                row.removeClass("error");
                row.find("input[type=checkbox]").prop('checked', false);
                logRating(row);
            }
        });
    });
</r:script>

