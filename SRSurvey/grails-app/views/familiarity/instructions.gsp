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
    <title>Geographic Concept Survey: Rate familiarity!</title>
    <meta name="layout" content="main"/>
    <r:require modules="core" />
</head>
<body>
<div class= "instructions rounded-corners" id="main-container">
    <br>
    <h1>Rate your familiarity.</h1>
    <g:form action="show" method="post">
        <p>
            In the next section you'll rate <b>how familiar </b>you are with each location
            <b>before beginning the survey</b>.
        </p>
        <p>
        </p>
        <p>
            <r:img uri="/images/rate_familiarity.png"/>
        </p>
        <p>
            <b>You must rate your familiarity of every location.</b>
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