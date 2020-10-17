DROP TABLE IF EXISTS QUESTION;
DROP TABLE IF EXISTS ANSWER;
DROP TABLE IF EXISTS unanswered_question;
DROP TABLE IF EXISTS QUESTION_ANSWER_RELATION;

CREATE TABLE ANSWER (answer_id INT  PRIMARY KEY,
                    answer VARCHAR(2000) NOT NULL,
                    create_date  DATE NOT NULL);

CREATE TABLE QUESTION (question_id INT  PRIMARY KEY,
                    question VARCHAR(2000) NOT NULL,
                    create_date  DATE NOT NULL);

CREATE TABLE QUESTION_ANSWER_RELATION (question_id INT,
                    answer_id INT,
                    votes INT,
                    manual BOOLEAN NOT NULL,
                    create_date DATE NOT NULL,
                    PRIMARY KEY(question_id, answer_id));

INSERT INTO ANSWER (answer_id, answer, create_date) values (1, 'John Wilkes Booth', CURRENT_TIMESTAMP());
INSERT INTO ANSWER (answer_id, answer, create_date) values (2, 'blue', CURRENT_TIMESTAMP());

INSERT INTO QUESTION (question_id, question, create_date) values (1, 'Who killed Abraham Lincoln?', CURRENT_TIMESTAMP());
INSERT INTO QUESTION (question_id, question, create_date) values (2, 'Who killed the popular president?', CURRENT_TIMESTAMP());
INSERT INTO QUESTION (question_id, question, create_date) values (3, 'What color is the sky?', CURRENT_TIMESTAMP());

INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, create_date) values (1, 1, true, CURRENT_TIMESTAMP());
INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, create_date) values (2, 1, true, CURRENT_TIMESTAMP());
INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, create_date) values (3, 2, true, CURRENT_TIMESTAMP());


CREATE TABLE unanswered_question (id INT AUTO_INCREMENT  PRIMARY KEY,
question VARCHAR(2000) NOT NULL,
times_asked INT NOT NULL);