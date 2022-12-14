package org.jobrunr.tests.e2e;

import org.mariadb.jdbc.MariaDbPoolDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

public class Main extends AbstractSqlMain {

    public static void main(String[] args) throws Exception {
        new Main(args);
    }

    public Main(String[] args) throws Exception {
        super(args);
    }

    @Override
    protected DataSource createDataSource(String jdbcUrl, String userName, String password) throws SQLException {
        MariaDbPoolDataSource dataSource = new MariaDbPoolDataSource();
        //JobRunrPlus: disable SSL to fix tests
        dataSource.setUrl(jdbcUrl + "?rewriteBatchedStatements=true&pool=true&useSSL=false&useBulkStmts=false");
        dataSource.setUser(userName);
        dataSource.setPassword(password);
        return dataSource;
    }
}
