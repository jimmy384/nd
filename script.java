import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

public void executeSqlString(String sql, DataSource dataSource) {

    ByteArrayResource resource =
            new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8));

    try (Connection connection = dataSource.getConnection()) {

        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            ScriptUtils.executeSqlScript(connection, resource);
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }

    } catch (Exception e) {
        throw new RuntimeException("执行SQL脚本失败", e);
    }
}
