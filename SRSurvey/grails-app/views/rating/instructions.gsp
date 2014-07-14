<%--
  Created by IntelliJ IDEA.
  User: Margaret
  Date: 6/4/13
  Time: 10:46 AM
  To change this template use File | Settings | File Templates.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Geographic Concept Survey: Thanks!</title>
    <meta name="layout" content="main"/>
    <r:require modules="core" />
</head>
<body>
<div class= "instructions rounded-corners" id="main-container">
    <br>
    <h1>Rate how related locations are.</h1>
    <g:form action="show" method="post">
        <p>
            In the next section you'll rate <b>how related</b> pairs of locations are.
        </p>
        <p>
            <b>If you don't recognize a location, click the "I don't know this term" column.</b>
        </p>
        <p>
            <r:img uri="/images/rate_sr.png"/>
        </p>
        <p>
            You must respond to every question by rating it or clicking the "I don't know" box.
        </p>
        <p>
            <g:submitButton name="continue" value="continue" class="myButton" title="continue"/>
        </p>
    </g:form>
</div>
</body>
</html>