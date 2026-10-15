# Viva questions and answers

## A. Core Java (as used in this project)

**1. Where did you use encapsulation?**
In every model class (`User`, `Message`...): fields are `private` and exposed only through getters/setters, so validation or
changes in storage do not affect callers.

**2. Where is abstraction used?**
`ChatbotService` is an interface; servlets depend on it, not on `ChatbotServiceImpl`. `ResponseStrategy` is another interface.
The caller knows *what* is offered, not *how*.

**3. Where is polymorphism used?**
`ChatbotServiceImpl` holds a `List<ResponseStrategy>` containing `KeywordMatchStrategy` and `TokenOverlapStrategy`. It calls
`strategy.findMatch(...)` and the version that runs depends on the object (runtime polymorphism). Adding a new strategy needs no change in the loop.

**4. Where is inheritance used and why?**
All models extend `BaseEntity` (id and createdAt are common to all tables); all servlets extend `BaseServlet` (shared helpers
such as `forward`, `redirect`, `flash`). Both are real code sharing, not inheritance for show.

**5. Which collections did you use?**
`List`/`ArrayList` for messages and knowledge-base entries, `Map`/`HashMap` for the dashboard statistics and rating distribution,
`LinkedHashMap` for validation errors (keeps field order), `Set`/`HashSet` for stop words and word overlap.

**6. How do you handle exceptions?**
`DatabaseException` (unchecked) wraps `SQLException` in the DAOs, with `isDuplicateKey()` for duplicate emails.
`ChatbotException` (checked) carries messages that are safe to show the user. Unexpected errors go to `error.jsp`, which never shows a stack trace.

**7. Why is `DatabaseException` unchecked and `ChatbotException` checked?**
A database failure is usually not recoverable at the point of call, so callers need not catch it. A validation/ownership problem is an
expected situation the caller must handle, so the compiler forces it.

**8. Where is multithreading used?**
`AnalyticsService` creates a fixed `ExecutorService` with 3 threads. The dashboard's three independent query groups are submitted as
`Callable`s with `invokeAll`; the results (`Future`) are merged into one `Map`. The pool is shut down in `AppContextListener.contextDestroyed`.

**9. Is the chatbot using AI/machine learning?**
No. It normalizes the text (lower-case, remove punctuation) and matches administrator-defined keywords/phrases with a score. We say this openly; it is an
explainable rule-based chatbot.

**10. How does the matching work?**
Question -> `TextNormalizer.normalize`. For every active entry, each keyword (word or phrase) that appears as whole words in the question adds its word count to the score.
Highest score wins; no score means the second strategy (word overlap, at least 2 shared words); otherwise the fallback text is returned.

**11. What are the Java 17 features you used?**
`record` (`ChatbotService.Answer`), pattern matching for `instanceof`, switch expressions/arrows, `HexFormat`, `String.repeat` in tests.

**12. What is try-with-resources?**
A `try (...)` block whose resources implement `AutoCloseable` are closed automatically, even on exceptions. All our `Connection`, `PreparedStatement` and `ResultSet` objects use it, so no leaks.

## B. JDBC

**1. What are the steps of JDBC?**
Load driver, get `Connection`, create `PreparedStatement`, set parameters, execute (`executeQuery` / `executeUpdate`), process `ResultSet`, close resources.
(`DBConnection.getConnection()` + any DAO method.)

**2. Why `PreparedStatement` instead of `Statement`?**
It prevents SQL injection (values are sent as data, never concatenated into the SQL), and the database can reuse the compiled statement.

**3. Show INSERT / SELECT / UPDATE / DELETE in your code.**
INSERT `UserDAO.create`; SELECT `UserDAO.findByEmail`, `KnowledgeBaseDAO.findAll`; UPDATE `KnowledgeBaseDAO.update`, `ChatbotConfigDAO.save`; DELETE `KnowledgeBaseDAO.delete`, `UserDAO.deleteUser`.

**4. What is a transaction? Where do you use one?**
A group of statements that succeed or fail together (ACID). `MessageDAO.saveExchange` inserts the user message, the bot message and updates the conversation's `updated_at`
with `setAutoCommit(false)`; `commit()` on success, `rollback()` in the catch block, so we never store a question without its answer.

**5. What does `getGeneratedKeys()` do?**
Returns the auto-increment id of the row just inserted (requested with `Statement.RETURN_GENERATED_KEYS`). We use it to put the new id into the model object.

**6. How do you avoid hard-coding the database password?**
`DBConnection` reads `DB_URL`, `DB_USER`, `DB_PASSWORD` from environment variables, then system properties, then `db.properties` (which has no password).

**7. Why not use one static Connection?**
A shared connection is not thread-safe, can time out, and breaks under concurrent requests. We open one per operation and close it. A connection pool would be the production improvement.

**8. What is the DAO pattern?**
A class that hides all SQL for one entity behind methods like `findById`/`create`. Servlets and services never contain SQL, so the database can change without touching business logic.

**9. How do you detect a duplicate email?**
`users.email` is `UNIQUE`. `AuthService` first checks `existsByEmail`, and also catches `DatabaseException.isDuplicateKey()` (MySQL error 1062) for the race where two people register at once.

**10. What are foreign keys and `ON DELETE CASCADE`?**
A foreign key links a child row to a parent row and prevents orphans. With `ON DELETE CASCADE`, deleting a user deletes their conversations and messages automatically.

**11. `executeQuery` vs `executeUpdate`?**
`executeQuery` runs SELECT and returns a `ResultSet`; `executeUpdate` runs INSERT/UPDATE/DELETE and returns the number of affected rows (we use it to detect "not found").

**12. How do you handle NULL columns?**
`rs.getInt(...)` returns 0 for NULL, so we call `rs.wasNull()` to map it to a Java `null` (e.g. `matched_entry_id`), and `ps.setNull(index, Types.INTEGER)` when writing.

## C. Servlets, JSP and web integration

**1. Describe the request flow for sending a chat message.**
Browser POST `/user/chat` -> `CsrfFilter` (token ok?) -> `AuthenticationFilter` (logged in?) -> `ChatServlet.doPost` -> `ChatbotService.sendMessage` -> DAOs -> MySQL ->
`sendRedirect` to `/user/chat?id=..` -> `doGet` loads messages -> forward to `chatbot.jsp`.

**2. What is the Post/Redirect/Get pattern and why use it?**
After a successful POST we redirect to a GET URL. If the user refreshes, the browser repeats the harmless GET instead of re-submitting the form (which would send the message twice).

**3. `forward` vs `sendRedirect`?**
`forward` happens inside the server, same request, URL unchanged, request attributes preserved. `sendRedirect` tells the browser to make a new request; attributes are lost (so we use a session "flash" message).

**4. How is login/session handled?**
`LoginServlet` verifies the password hash, calls `request.changeSessionId()` (prevents session fixation), and stores the `User` (without hash) in the `HttpSession`. `LogoutServlet` calls `session.invalidate()`.

**5. What is a filter and which ones do you have?**
A component that intercepts requests before servlets. `CsrfFilter`, `AuthenticationFilter` (`/user/*`, `/admin/*`), `AdminAuthorizationFilter` (`/admin/*`). Order is defined by the `filter-mapping` order in `web.xml`.

**6. How does role-based access work?**
The session `User` has role `USER` or `ADMIN`. `AdminAuthorizationFilter` returns 403 unless `user.isAdmin()`. A user can never register as admin because `AuthService.register` always sets `USER`.

**7. What is CSRF and how do you prevent it?**
An attack where another site makes your browser submit a form to our site using your cookies. Each session has a random token; every POST form includes it as a hidden field and `CsrfFilter` rejects POSTs where it is missing or different.

**8. How do you prevent XSS?**
All user-supplied text is printed with `<c:out>`, which HTML-escapes it. Scriptlets are disabled, so output only goes through EL/JSTL.

**9. Why are the JSPs inside `WEB-INF`?**
Files under `WEB-INF` cannot be requested directly by URL. Users must pass through a servlet and its filters, which prepare the data and enforce access.

**10. Why no scriptlets and no SQL in JSP?**
Separation of concerns: JSP is the view only. Logic in Java classes is testable and reusable. `web.xml` has `<scripting-invalid>true</scripting-invalid>` to enforce it.

**11. What are EL and JSTL?**
EL (`${user.fullName}`) reads request/session attributes. JSTL tags (`c:forEach`, `c:if`, `c:choose`, `fmt:formatDate`) handle loops, conditions and formatting without Java code.

**12. Why `jakarta.servlet` and not `javax.servlet`?**
Tomcat 10+ implements Jakarta EE 9+, where packages were renamed to `jakarta.*`. Code using `javax.servlet` will not run on Tomcat 10.1.

**13. Why does Tomcat need JSTL in the WAR?**
Tomcat implements Servlet/JSP/EL but does not ship JSTL, so the JSTL API and implementation are normal Maven dependencies packaged into `WEB-INF/lib`.

**14. What does `doGet` vs `doPost` mean in your design?**
`doGet` only reads data and renders pages. `doPost` changes state (register, chat, delete, save). State changes never happen on GET, which also protects against CSRF via links.

**15. How is conversation ownership verified?**
`ConversationDAO.findByIdAndUser(id, userId)` uses `WHERE id = ? AND user_id = ?`. If it returns null the service throws `ChatbotException("Conversation not found.")` - the same message for "missing" and "not yours", so ids cannot be probed.
