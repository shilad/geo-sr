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
            For example, the image below asks you how related the state of "Minnesota" is to "Macalester College."
            Please rate each of the 37 pairs on a scale of 0 (not related) to 4 (strongly related).
        </p>
        <p>
            <r:img uri="/images/rate_sr.png"/>
        </p>
        <p>
            <b>You must rate the relatedness of every pair of locations.</b>
        </p>
        <p>
            If you <b>don't know</b> a location, you can learn about it by clicking the blue link.
            The Wikipedia article associated with the concept will open in a separate window.
        </p>
        <p>

        </p>
        <p>
            <g:submitButton name="continue" value="continue" class="myButton" title="continue"/>
        </p>
    </g:form>
</div>
</body>
</html>