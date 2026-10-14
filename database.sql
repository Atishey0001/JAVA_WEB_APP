-- ============================================================
-- AI Chatbot System - database schema and sample data
-- Run:  mysql -u root -p < database.sql
-- Safe to re-run: it never drops existing tables or data.
-- NOTE: no admin account is created here (a password hash must not be
-- committed in SQL). See README "Creating the first admin account".
-- ============================================================
CREATE DATABASE IF NOT EXISTS ai_chatbot_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_chatbot_db;

CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('ADMIN','USER') NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS conversations (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    title      VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_conversation_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_conversation_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS knowledge_base (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(150) NOT NULL UNIQUE,
    keywords   VARCHAR(500) NOT NULL,          -- comma separated words / phrases
    answer     TEXT NOT NULL,
    category   VARCHAR(50) NOT NULL DEFAULT 'General',
    is_active  TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS messages (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    conversation_id  INT NOT NULL,
    sender           ENUM('USER','BOT') NOT NULL,
    content          TEXT NOT NULL,
    matched_entry_id INT NULL,                  -- knowledge-base entry used for a BOT answer
    unanswered       TINYINT(1) NOT NULL DEFAULT 0, -- 1 on a USER message that got the fallback reply
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_message_entry FOREIGN KEY (matched_entry_id) REFERENCES knowledge_base(id) ON DELETE SET NULL,
    INDEX idx_message_conversation (conversation_id),
    INDEX idx_message_unanswered (sender, unanswered)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS feedback (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL,
    conversation_id INT NULL,
    rating          TINYINT NOT NULL,
    comment         VARCHAR(1000) NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE SET NULL,
    CONSTRAINT chk_feedback_rating CHECK (rating BETWEEN 1 AND 5),
    INDEX idx_feedback_rating (rating)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS chatbot_config (
    id                TINYINT PRIMARY KEY,      -- always 1 (single configuration row)
    bot_name          VARCHAR(50)  NOT NULL,
    welcome_message   VARCHAR(500) NOT NULL,
    fallback_response VARCHAR(500) NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_config_single_row CHECK (id = 1)
) ENGINE=InnoDB;

-- ---------------- sample data (INSERT IGNORE = safe to re-run) ----------------
INSERT IGNORE INTO chatbot_config (id, bot_name, welcome_message, fallback_response) VALUES
(1, 'AI Support',
 'Hello! Welcome to AI Support. Ask me about our services, pricing, orders or how to contact us.',
 'Sorry, I could not find an answer to that. Your question has been saved so the support team can improve my answers. Please try rephrasing it.');

INSERT IGNORE INTO knowledge_base (title, keywords, answer, category) VALUES
('Greeting', 'hello,hi,hey,greetings,good morning,good afternoon,good evening',
 'Hello! Welcome to AI Support.\nHow can I help you today?', 'General'),
('Our Services', 'services,service,what do you do,what do you offer,offer,provide,products',
 'I can help answer questions about our services and common support requests.', 'General'),
('Contact Support', 'contact,contact support,reach you,email support,phone,talk to human,customer care',
 'Please submit your query through this chatbot so the support team can review your request.', 'Support'),
('Business Hours', 'hours,business hours,opening hours,working hours,timing,timings,open',
 'Our support team is available Monday to Friday, 9 AM to 6 PM. You can leave a message here at any time.', 'General'),
('Reset Password', 'password,forgot password,reset password,password reset,change password',
 'To reset your password, open your Profile page and use the Change Password form. If you cannot log in, contact the administrator.', 'Account'),
('Refund Policy', 'refund,refunds,money back,return,cancel order,cancellation',
 'Refunds can be requested within 7 days of purchase. Please share your order number with the support team.', 'Billing'),
('Pricing', 'price,prices,pricing,cost,plans,subscription,fee,fees',
 'Our pricing depends on the plan you choose. Please contact support for a detailed quotation.', 'Billing'),
('Order Tracking', 'track,tracking,order status,where is my order,delivery,shipping',
 'You can track an order using the tracking number in your confirmation email. Share your order number if you need more help.', 'Orders'),
('Thanks and Goodbye', 'thanks,thank you,bye,goodbye,see you',
 'You are welcome! Have a great day.', 'General');
