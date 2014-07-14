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
    <title>Geographic relatedness survey: Places you've lived</title>
    <meta name="layout" content="main"/>
    <r:require modules="core" />

</head>
<body>
<div class="homes rounded-corners expertise" id="main-container">
    <br>
    <h1>Places you've lived in ${country} (page 2 of 14)</h1>
    <g:form controller="demographic" action="saveHomes" method="post">

    <div class="question">
        <div class="prompt">Please list all the cities and towns in <b>${country}</b> you've lived for <b>at least a month:</b></div>
        <div id="city-list" class="response">
            <div class="city template">
                <r:img uri="/images/cancel.gif"/>&nbsp;<span>Fake, Fake</span>
                <input type="hidden" name="city" value="FAKE"/>
            </div>
            <input type="text" size="40" value="" id="cityac" class="ui-autocomplete-input" autocomplete="on"/>
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
<r:script>
    $(document).ready(function () {
        $(".city.template").hide();
        $("#cityac").focus();

        $("#cityac").autocomplete({
            source: "autocompleteCity?country=${country.encodeAsURL()}",
            delay : 50,
            minLength: 1,
            change: function (ev, ui) {
                if (!ui.item)
                    $(this).val("");
            },
            select: function(event, ui) {
                if (!ui || !ui.item) {
                    return false;
                }
                var elem = $(".city.template").clone();
                elem.removeClass("template");
                elem.find("span").text(ui.item.label);
                elem.find("input[type='hidden']").attr("value", ui.item.value);
                elem.find("img").click(function() { $(this).parent().remove(); });
                $("#city-list>:last-child").before(elem);
                elem.show();

                $(this).val("");
                return false;
            }
        });

        $("form").submit(function( e ) {
            var hasError = false;

            if ($("input[name='city']").length <= 1) {
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
</body>
</html>
