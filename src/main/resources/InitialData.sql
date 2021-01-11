DROP TABLE IF EXISTS QUESTION;
DROP TABLE IF EXISTS ANSWER;
DROP TABLE IF EXISTS unanswered_question;
DROP TABLE IF EXISTS QUESTION_ANSWER_RELATION;
DROP TABLE IF EXISTS PART_OF_SPEECH_QUESTION;
DROP TABLE IF EXISTS VARIABLES;
DROP TABLE IF EXISTS INTERACTION;
DROP SEQUENCE IF EXISTS QUESTION_SEQUENCE;
DROP SEQUENCE IF EXISTS ANSWER_SEQUENCE;
DROP SEQUENCE IF EXISTS INTERACTION_SEQUENCE;


CREATE SEQUENCE QUESTION_SEQUENCE START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE ANSWER_SEQUENCE START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE INTERACTION_SEQUENCE START WITH 100 INCREMENT BY 1;


CREATE TABLE ANSWER (answer_id INT  PRIMARY KEY,
                    answer VARCHAR(6000) NOT NULL,
                    create_date  DATE NOT NULL);

CREATE TABLE QUESTION (question_id INT  PRIMARY KEY,
                    question VARCHAR(2000) NOT NULL,
                    max_possible_score_main float,
                    max_possible_score_synsets float,
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

CREATE TABLE INTERACTION (interaction_id INT,
                          interaction_type VARCHAR(2000) NOT NULL,
                          response_time_millis INT NOT NULL,
                          create_date  DATE NOT NULL,
                          PRIMARY KEY(interaction_id));

CREATE TABLE unanswered_question (id INT AUTO_INCREMENT  PRIMARY KEY,
raw_question VARCHAR(2000) NOT NULL,
processed_question VARCHAR(2000) NOT NULL,
problem_type VARCHAR(2000) NOT NULL,
times_asked INT NOT NULL
);

INSERT INTO ANSWER (answer_id, answer, create_date) values (1, 'John Wilkes Booth', CURRENT_TIMESTAMP());
INSERT INTO ANSWER (answer_id, answer, create_date) values (2, 'blue', CURRENT_TIMESTAMP());
INSERT INTO ANSWER (answer_id, answer, create_date) values (3, 'John Booth', CURRENT_TIMESTAMP());

INSERT INTO QUESTION (question_id, question, max_possible_score_main, max_possible_score_synsets, create_date) values (1, 'Who killed Abraham Lincoln?', 100, 100, CURRENT_TIMESTAMP());
INSERT INTO QUESTION (question_id, question, max_possible_score_main, max_possible_score_synsets, create_date) values (2, 'Who killed the popular president?', 100, 100, CURRENT_TIMESTAMP());
INSERT INTO QUESTION (question_id, question, max_possible_score_main, max_possible_score_synsets, create_date) values (3, 'What color is the sky?', 100, 100, CURRENT_TIMESTAMP());
INSERT INTO QUESTION (question_id, question, max_possible_score_main, max_possible_score_synsets, create_date) values (4, 'Who killed president Abraham?', 100, 100, CURRENT_TIMESTAMP());

INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, votes, create_date) values (1, 1, true, 0, CURRENT_TIMESTAMP());
INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, votes, create_date) values (2, 1, true, 0, CURRENT_TIMESTAMP());
INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, votes, create_date) values (3, 2, true, 0, CURRENT_TIMESTAMP());
INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, votes, create_date) values (4, 3, false, 1, CURRENT_TIMESTAMP());









