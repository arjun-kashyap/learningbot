package org.arjunkashyap.learningbot.process;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GeneralProcessor {
    @Autowired
    private JdbcTemplate jtm;

    public void logInteraction(String interactionType, long responseTime, int attemptCount) {
        jtm.update("INSERT INTO INTERACTION (interaction_id, interaction_type, response_time_millis, attempt_count, create_date) values (INTERACTION_SEQUENCE.NEXTVAL, ?, ?, ?, CURRENT_TIMESTAMP())", interactionType, responseTime, attemptCount);
    }
}
