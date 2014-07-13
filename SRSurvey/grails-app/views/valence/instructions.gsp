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
    <title>Geographic Concept Survey: Rate valence</title>
    <meta name="layout" content="main"/>
    <r:require modules="core" />
</head>
<body>
<div class= "instructions rounded-corners" id="main-container">
    <br>
    <h1>Rate your interest in living near each location.</h1>
    <g:form action="show" method="post">
        <p>
            In the next section you'll rate <b>your agreement</b> with the following statement about each location:
        </p>
        <p>
            <br/><b style="font-size: 1.2em">I would like to live in / near this location.</b>
        </p>
        <p>
            <br/><r:img uri="/images/rate_valence.png"/>
        </p>
        <p>
            You must rate your agreement with the statement for every location.
        </p>
        <p>
            If you <b>don't know</b> a location, you can learn about it by clicking the blue link.
            The Wikipedia article associated with the concept will open in a separate window.
        </p>
        <p>
            <g:submitButton name="continue" value="continue" class="myButton" title="continue"/>
        </p>
    </g:form>
</div>
</body>
</html>