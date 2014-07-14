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
    <title>Geographic Concept Survey: Rate familiarity</title>
    <meta name="layout" content="main"/>
    <r:require modules="core" />
</head>
<body>
<div class= "instructions rounded-corners" id="main-container">
    <br>
    <h1>Rate your familiarity with locations.</h1>
    <g:form action="show" method="post">
        <p>
            In the next section you'll rate <b>how familiar </b>you are with each location.
        </p>
        <p>
            As a guide, use the following scale:
        </p>

        <ul>
            <li><b>Rating 0</b>: Don't recognize the location.</li>
            <li><b>Rating 1</b>: Recognize the location, but know almost nothing about it.</li>
            <li><b>Rating 2</b>: Know basic information about the location.</li>
            <li><b>Rating 3</b>: Know this location well.</li>
            <li><b>Rating 4</b>: Consider myself "a local" for this location.</li>
        </ul>
        <p>
            <r:img uri="/images/rate_familiarity.png"/>
        </p>
        <p>
            <b>You must rate your familiarity for every location.</b>
        </p>
        <p>
            <g:submitButton name="continue" value="continue" class="myButton" title="continue"/>
        </p>
    </g:form>
</div>
</body>
</html>