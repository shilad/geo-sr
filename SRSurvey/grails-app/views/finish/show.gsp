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
<div class= "finish rounded-corners" id="main-container">
    <br>
    <h1>Geographic relatedness survey: Thanks!</h1>
    <g:form controller="finish" action="save" method="post">
        <p>
            Thank you for volunteering your time and expertise!
        </p>
        <div>
            Do you have any comments about this survey or your relatedness ratings?
            <textarea rows="10" cols="80" class ="rounded-corners"  name="comments"></textarea>
        </div>
        <div>
            <g:submitButton name="submit" value="Save comments and get your MTurk HIT code" class="myButton" title="Save comments and get your MTurk HIT code"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        </div>
    </g:form>
</div>
</body>
</html>