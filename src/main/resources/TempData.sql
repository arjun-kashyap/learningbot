DROP TABLE IF EXISTS QUESTION;
DROP TABLE IF EXISTS ANSWER;
DROP TABLE IF EXISTS unanswered_question;
DROP TABLE IF EXISTS QUESTION_ANSWER_RELATION;
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
                    question_words VARCHAR(2000),
                    max_possible_score_main float,
                    max_possible_score_synsets float,
                    create_date  DATE NOT NULL);

CREATE TABLE QUESTION_ANSWER_RELATION (question_id INT,
                    answer_id INT,
                    votes float,
                    manual BOOLEAN NOT NULL,
                    create_date DATE NOT NULL,
                    PRIMARY KEY(question_id, answer_id));

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

INSERT INTO QUESTION (question_id, question, create_date) values (28,'Can I take my dog to a dog park?', CURRENT_TIMESTAMP());
INSERT INTO ANSWER (answer_id, answer, create_date) values (28,'Look for emergency warning signs* for COVID-19. If someone is showing any of these signs, seek emergency medical care immediately Trouble breathing Persistent pain or pressure in the chest New confusion Inability to wake or stay awake Bluish lips or face *This list is not all possible symptoms. Please call your medical provider for any other symptoms that are severe or concerning to you. Call 911 or call ahead to your local emergency facility: Notify the operator that you are seeking care for someone who has or may have COVID-19.', CURRENT_TIMESTAMP());
INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, create_date) values (28,28,true, CURRENT_TIMESTAMP());