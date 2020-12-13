DROP TABLE IF EXISTS QUESTION;
DROP TABLE IF EXISTS ANSWER;
DROP TABLE IF EXISTS unanswered_question;
DROP TABLE IF EXISTS QUESTION_ANSWER_RELATION;
DROP TABLE IF EXISTS PART_OF_SPEECH_QUESTION;
DROP TABLE IF EXISTS VARIABLES;
DROP TABLE IF EXISTS INTERACTION;

CREATE TABLE ANSWER (answer_id INT  PRIMARY KEY,
                    answer VARCHAR(2000) NOT NULL,
                    create_date  DATE NOT NULL);

CREATE TABLE QUESTION (question_id INT  PRIMARY KEY,
                    question VARCHAR(2000) NOT NULL,
                    max_possible_searcher_score float,
                    create_date  DATE NOT NULL);

CREATE TABLE QUESTION_ANSWER_RELATION (question_id INT,
                    answer_id INT,
                    votes INT,
                    manual BOOLEAN NOT NULL,
                    create_date DATE NOT NULL,
                    PRIMARY KEY(question_id, answer_id));

CREATE TABLE PART_OF_SPEECH_QUESTION (pos_id INT,
                    nouns VARCHAR(2000),
                    verbs VARCHAR(2000),
                    question_id INT NOT NULL,
                    score INT NOT NULL,
                    create_date DATE NOT NULL,
                    PRIMARY KEY(pos_id));

CREATE TABLE VARIABLES (variable_name VARCHAR(2000) NOT NULL,
                        variable_value VARCHAR(2000) NOT NULL,
                        comments VARCHAR(2000),
                        create_date  DATE NOT NULL,
                        PRIMARY KEY(variable_name));

CREATE TABLE INTERACTION (interaction_id INT
                        );

CREATE TABLE unanswered_question (id INT AUTO_INCREMENT  PRIMARY KEY,
question VARCHAR(2000) NOT NULL,
times_asked INT NOT NULL);

/*
INSERT INTO ANSWER (answer_id, answer, create_date) values (1, 'John Wilkes Booth', CURRENT_TIMESTAMP());
INSERT INTO ANSWER (answer_id, answer, create_date) values (2, 'blue', CURRENT_TIMESTAMP());
INSERT INTO ANSWER (answer_id, answer, create_date) values (3, 'John Booth', CURRENT_TIMESTAMP());

INSERT INTO QUESTION (question_id, question, max_possible_searcher_score, create_date) values (1, 'Who killed Abraham Lincoln?', 100, CURRENT_TIMESTAMP());
INSERT INTO QUESTION (question_id, question, max_possible_searcher_score, create_date) values (2, 'Who killed the popular president?', 100, CURRENT_TIMESTAMP());
INSERT INTO QUESTION (question_id, question, max_possible_searcher_score, create_date) values (3, 'What color is the sky?', 100, CURRENT_TIMESTAMP());
INSERT INTO QUESTION (question_id, question, max_possible_searcher_score, create_date) values (4, 'Who killed president Abraham?', 100, CURRENT_TIMESTAMP());

INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, create_date) values (1, 1, true, CURRENT_TIMESTAMP());
INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, create_date) values (2, 1, true, CURRENT_TIMESTAMP());
INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, create_date) values (3, 2, true, CURRENT_TIMESTAMP());
INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, votes, create_date) values (4, 3, false, 1, CURRENT_TIMESTAMP());
*/








