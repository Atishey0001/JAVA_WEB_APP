<%@ page contentType="text/html;charset=UTF-8" %>
<c:set var="pageTitle" value="Welcome"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<div class="card hero">
    <h1>AI Support Chatbot</h1>
    <p>Get quick answers to common questions, keep your conversation history and tell us how we did.</p>
    <c:choose>
        <c:when test="${empty sessionScope.loggedInUser}">
            <a class="btn" href="${ctx}/login">Login</a>
            <a class="btn btn-secondary" href="${ctx}/register">Create an account</a>
        </c:when>
        <c:when test="${sessionScope.loggedInUser.admin}">
            <a class="btn" href="${ctx}/admin/dashboard">Open admin dashboard</a>
        </c:when>
        <c:otherwise>
            <a class="btn" href="${ctx}/user/chat">Start chatting</a>
        </c:otherwise>
    </c:choose>
</div>
<div class="grid">
    <div class="stat"><div class="lbl">Answers come from a</div><div class="num" style="font-size:1.3rem">Knowledge Base</div><div class="lbl">managed by administrators</div></div>
    <div class="stat"><div class="lbl">Every chat is</div><div class="num" style="font-size:1.3rem">Saved</div><div class="lbl">in MySQL for later review</div></div>
    <div class="stat"><div class="lbl">Your feedback</div><div class="num" style="font-size:1.3rem">Improves it</div><div class="lbl">unanswered questions are tracked</div></div>
</div>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
