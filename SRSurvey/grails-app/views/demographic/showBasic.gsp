<%--
  Created by IntelliJ IDEA.
  User: research
  Date: 6/20/13
  Time: 10:45 AM
  To change this template use File | Settings | File Templates.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Geographic relatedness survey: Background</title>
    <meta name="layout" content="main"/>
    <r:require modules="core" />
    <r:require modules="jquery-ui" />
</head>
<body>
<div class= "rounded-corners expertise" id="main-container">

    <h1>Geographic relatedness survey: Background</h1>

    <g:form action="saveBasic">

    <div class="question">
        <div class="prompt">
            What gender do you identify most strongly with?&nbsp;&nbsp;
        </div>
        <div class="response">
            <g:textField name="gender" size="8"></g:textField>
            <g:checkBox name="gender-no"></g:checkBox> <label for="gender-no">I prefer not to respond</label>
        </div>
    </div>

    <div class="question">
        <div class="prompt">
            What is your highest education level?&nbsp;&nbsp;
        </div>
        <div class="response">
            <g:select name="education" from="${['please choose closest level', 'below high school', 'high school G.E.D or equivalent', 'degree from two-year college', 'degree from four-year college', 'graduate degree']}"/>
        </div>
    </div>

    <div class="question">
        <div class="prompt">Which countries have you lived in for at least one month?</div>
        <div class="response">
            <label><input type="checkbox" name="country" value="United States of America"/>United States of America</label><br/>
            <label><input type="checkbox" name="country" value="Australia"/>Australia</label><br/>
            <label><input type="checkbox" name="country" value="Brazil"/>Brazil</label><br/>
            <label><input type="checkbox" name="country" value="Canada"/>Canada</label><br/>
            <label><input type="checkbox" name="country" value="France"/>France</label><br/>
            <label><input type="checkbox" name="country" value="India"/>India</label><br/>
            <label><input type="checkbox" name="country" value="Pakistan"/>Pakistan</label><br/>
            <label><input type="checkbox" name="country" value="Spain"/>Spain</label><br/>
            <label><input type="checkbox" name="country" value="United Kingdom"/>United Kingdom</label><br/>
        </div>
    </div>
    <div class="question">
        <div class="prompt">&nbsp;</div>
        <div class="response">
            <g:submitButton name="next" class="myButton"/>
        </div>
    </div>
    </g:form>
    <div class="error">
        Please respond to all questions.
    </div>
</div>
</body>
</html>

<r:script>
    $(document).ready(function () {
        var genderField = $( "input[name='gender']" );
        var genderBox = $("input[name='gender-no']");

        genderField.autocomplete({ source: ["Male", "Female"], delay : 50, minLength: 1});
        genderBox.click(function() { genderField.val(''); });
        genderField.focus(function() { genderBox.removeAttr('checked'); })

        $("form").submit(function (e) {
            var hasError = false;
            if ($("input[name='country']:checked").length == 0) {
                hasError = true;
            }
            if (!genderBox.is(':checked') && genderField.val() == '') {
                hasError = true;
            }
            if ($("select").val().substring(0, 6) == 'please') {
                hasError = true;
            }

            if (hasError) {
                $(".error").show();
                return false;
            } else {
                $(".error").hide();
                return true;
            }
        });
    });
</r:script>