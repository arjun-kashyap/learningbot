DROP TABLE IF EXISTS billionaires;

CREATE TABLE billionaires (
  id INT AUTO_INCREMENT  PRIMARY KEY,
  first_name VARCHAR(250) NOT NULL,
  last_name VARCHAR(250) NOT NULL,
  career VARCHAR(250) DEFAULT NULL
);

INSERT INTO billionaires (first_name, last_name, career) VALUES
  ('Aliko', 'Dangote', 'Billionaire Industrialist'),
  ('Bill', 'Gates', 'Billionaire Tech Entrepreneur'),
  ('Folrunsho', 'Alakija', 'Billionaire Oil Magnate');


DROP TABLE IF EXISTS QUESTION;
DROP TABLE IF EXISTS ANSWER;
DROP TABLE IF EXISTS unanswered_question;

CREATE TABLE ANSWER (answer_id INT AUTO_INCREMENT  PRIMARY KEY,
                  answer VARCHAR(2000) NOT NULL,
                   create_date  DATE not null);
INSERT INTO ANSWER (answer, create_date) values ('John Wilkes Booth', CURRENT_TIMESTAMP());

CREATE TABLE QUESTION (question_id INT AUTO_INCREMENT  PRIMARY KEY,
                  question VARCHAR(2000) NOT NULL,
                   answer_id INT NOT NULL references ANSWER(answer_id),
                   curated_manually boolean NOT NULL,
                   create_date  DATE not null);
insert into QUESTION (question, answer_id, curated_manually, create_date) values ('Who killed Abraham Lincoln?', 1, true, CURRENT_TIMESTAMP());
insert into QUESTION (question, answer_id, curated_manually, create_date) values ('Who killed Abraham Abraham?', 1, false, CURRENT_TIMESTAMP());


CREATE TABLE unanswered_question (id INT AUTO_INCREMENT  PRIMARY KEY,
question VARCHAR(2000) NOT NULL,
times_asked INT NOT NULL);