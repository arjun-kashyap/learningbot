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


CREATE TABLE unanswered_question (id INT AUTO_INCREMENT  PRIMARY KEY,
question VARCHAR(2000) NOT NULL,
times_asked INT NOT NULL);