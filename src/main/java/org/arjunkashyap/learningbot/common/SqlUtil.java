package org.arjunkashyap.learningbot.common;

import java.nio.charset.Charset;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

@Component
public class SqlUtil {
    @Autowired private ApplicationContext context;
    @Autowired private DataSource datasource;

    // This runs any SQL file
    public void runSqlFile(String sqlFilename) {
        Resource resource = context.getResource(sqlFilename);
        EncodedResource encodedResource = new EncodedResource(resource, Charset.forName("UTF-8"));
        try {
            ScriptUtils.executeSqlScript(datasource.getConnection(), encodedResource);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}