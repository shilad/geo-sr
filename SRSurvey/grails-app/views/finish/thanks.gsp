<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Geographic Concept Survey: Thank you</title>
    <meta name="layout" content="main"/>
    <r:require modules="core" />

</head>
<body>
<div class="thanks rounded-corners" id="main-container">
    <br>
    <h1>Thank You!</h1>
    <p>
        Thanks for participating in our study.
        Over the coming months we will analyze the data you have provided to determine whether systematic differences exist in people's ratings.
        We believe this work will advance scholarly knowledge.
    </p>

    <p>
        Your Amazon Mechanical Turk participation code is <b style="font-weight: bold">${person.code}</b>
        <br/>Please copy and paste this into the HIT to receive credit.
    </p>

    <p>
        <input id="foo" type="checkbox" name="foo">&nbsp;&nbsp;Please email the results of this study when they are published. &nbsp;&nbsp;&nbsp;
        <span class="saved">Your response has been saved!</span>
    </p>
    <p>
        Thanks for your help!
    </p>

    <p>
        Shilad Sen (<a href="mailto:ssen@macalester.edu">ssen@macalester.edu</a>)<br/>
        Brent Hecht (<a href="mailto:bhecht@cs.umn.edu">bhecht@cs.umn.edu</a>)
    </p>
    <p>
        The WikiBrain Team:</br>
        <r:img uri="/images/team.jpg"/><br/>
    Front row: Laura Souza Vonessen, Huy Mai, Ben Mathers, Becca Harper<br/>
    Back row: Matthew Wright, Shilad Sen, Sam Horlbeck Olsen<br/>
    </p>

</div>
<r:script>
$().ready(function() {
    $(".saved").hide();
    $("#foo").click(function() {
        var checked = ($(this).attr("checked") == 'checked');
        $(".saved").fadeIn().delay(1000).fadeOut();
        ajaxLog('emailPublication\t' + checked);

    });
});
</r:script>
</body>
</html>