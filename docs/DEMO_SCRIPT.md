# Project demonstration script (about 10 minutes)

**Before the demo:** MySQL running, `database.sql` imported, Tomcat started with `ADMIN_EMAIL` / `ADMIN_PASSWORD` set,
browser open at `http://localhost:8080/ai-chatbot-system/`. Keep two browser profiles (or one normal + one private window)
so you can be a user and the admin at the same time.

| # | Do this | Say this (rubric link) |
|---|---|---|
| 1 | Show the landing page, then the project tree in the IDE. | "MVC-style layers: model, DAO, service, servlet, filter. JSPs only display data." (Problem understanding / design) |
| 2 | Register a new user with a weak password, then a valid one. | "Validation is server-side. The password is stored as a salted PBKDF2 hash - show the `users` table in MySQL." (JDBC, security) |
| 3 | Try to register the same email again. | "UNIQUE constraint plus a friendly message; we catch error 1062 in `DatabaseException.isDuplicateKey()`." |
| 4 | Log in as the user. Try opening `/ai-chatbot-system/admin/dashboard`. | "403 - `AdminAuthorizationFilter` checks the role in the session." (Servlets and web integration) |
| 5 | Open the chatbot. Send: `Hello`, `What services do you provide?`, `How can I contact support?`. | "Question goes to `ChatServlet` -> `ChatbotServiceImpl` -> keyword strategy -> DAO. This is keyword matching, not machine learning." |
| 6 | Send `What is the meaning of life?`. | "No match, so the fallback from the configuration is used and the question is flagged `unanswered`." |
| 7 | Show the `messages` table in MySQL. Restart Tomcat, reopen History -> conversation. | "Both messages are saved in one transaction (commit/rollback in `MessageDAO.saveExchange`). Data survives restarts." (JDBC) |
| 8 | Click "New conversation", send one message, then open History. | "Each conversation belongs to one user; ownership is enforced in SQL." |
| 9 | Try `/user/conversation?id=<id of another user's conversation>` (create one with a second account if time permits). | "Redirected with 'Conversation not found' - ownership check on the server." |
| 10 | Submit feedback: 5 stars, then 1 star with a comment. | "Rating validated 1-5; also enforced by a CHECK constraint in MySQL." |
| 11 | Switch to the admin window and log in. Show the dashboard. | "All numbers come from SQL queries. `AnalyticsService` runs three groups of queries in parallel with an `ExecutorService`." (Core Java: multithreading, collections) |
| 12 | Point at "Frequently unanswered questions" -> click **Add answer**. Save a new entry with keywords such as `meaning of life`. | "This is the improvement loop: admin teaches the bot." |
| 13 | Back in the user window ask `What is the meaning of life?` again. | "Now answered - knowledge base entries are read from MySQL on every question." |
| 14 | Show Knowledge Base: search, edit, disable, delete an entry. | "INSERT, UPDATE, DELETE, SELECT with PreparedStatement." |
| 15 | Show Configuration: change the bot name and fallback; refresh the chat page. | "Stored in the single-row `chatbot_config` table." |
| 16 | Show Conversations (search a word), Feedback (filter 1 star, average), Users (delete the demo user). | "Cascade delete removes the user's conversations and feedback." |
| 17 | Log out. Press the browser Back button and refresh a protected page. | "Session invalidated, so `AuthenticationFilter` redirects to login." |
| 18 | In the IDE run `mvn test` (or open the test folder). | "Unit tests for chatbot logic, auth, KB, feedback, filters; JDBC tests run against MySQL." |

**If something goes wrong:** check `logs/catalina.out`. Common causes: MySQL not running, wrong `DB_PASSWORD`, WAR copied but old folder not deleted, environment variables set in a different terminal than the one running Tomcat.
