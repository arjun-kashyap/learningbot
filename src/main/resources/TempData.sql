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

INSERT INTO QUESTION (question_id, question, create_date) values (18,'What is the risk of my child becoming sick with COVID-19?', CURRENT_TIMESTAMP());
INSERT INTO ANSWER (answer_id, answer, create_date) values (18,'Children can be infected with the virus that causes COVID-19 and can get sick with COVID-19. Most children with COVID-19 have mild symptoms or they may have no symptoms at all (“asymptomatic”). Fewer children have been sick with COVID-19 compared to adults. However, children with certain underlying medical conditions and infants (less than 1 year old) might be at increased risk for severe illness from COVID-19. Some children have developed a rare but serious disease that is linked to COVID-19 called multisystem inflammatory syndrome (MIS-C). For more information for parents or caregivers of children, see Children and Teens  and the COVID-19 Parental Resources Kit. For more information about how people get sick with the virus that causes COVID-19, see How COVID-19 Spreads.', CURRENT_TIMESTAMP());
INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, create_date) values (18,18,true, CURRENT_TIMESTAMP());