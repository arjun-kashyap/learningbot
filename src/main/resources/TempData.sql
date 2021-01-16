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

INSERT INTO QUESTION (question_id, question, create_date) values (38,'What is the difference between COVID-19 and seasonal allergies?', CURRENT_TIMESTAMP());
INSERT INTO ANSWER (answer_id, answer, create_date) values (38,'COVID-19 is a contagious respiratory illness caused by infection with a new coronavirus (called SARS-CoV-2, the virus that causes COVID-19). Seasonal allergies triggered by airborne pollen can lead to seasonal allergic rhinitis, which affects the nose and sinuses, and seasonal allergic conjunctivitis, which affects the eyes. COVID-19 and seasonal allergies share many symptoms, but there are some key differences between the two. For example, COVID-19 can cause fever, which is not a common symptom of seasonal allergies. The image below compares symptoms caused by allergies and COVID-19. Because some of the symptoms of COVID-19 and seasonal allergies are similar, it may be difficult to tell the difference between them, and you may need to get a test to confirm your diagnosis 508 version *Seasonal allergies do not usually cause shortness of breath or difficulty breathing, unless a person has a respiratory condition such as asthma that can be triggered by exposure to pollen. This is not a complete list of all possible symptoms of COVID-19 or seasonal allergies. Symptoms vary from person to person and range from mild to severe. You can have symptoms of both COVID-19 and seasonal allergies at the same time. If you think you have COVID-19, follow CDC’s guidance on ”What to do if you are sick.” If you have an emergency warning sign (including trouble breathing), seek emergency medical care immediately. Get more information on COVID-19 symptoms, or more information on seasonal allergy symptomsexternal icon.', CURRENT_TIMESTAMP());
INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, create_date) values (38,38,true, CURRENT_TIMESTAMP());