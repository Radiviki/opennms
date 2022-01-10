/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012-2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 ******************************************************************************/

package org.opennms.core.schema;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.change.ChangeFactory;
import liquibase.database.DatabaseConnection;
import liquibase.database.core.PostgresDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.ext.opennms.autoincrement.AddNamedAutoIncrementChange;
import liquibase.ext.opennms.createindex.CreateIndexWithWhereChange;
import liquibase.ext.opennms.createindex.CreateIndexWithWhereGenerator;
import liquibase.ext.opennms.dropconstraint.DropForeignKeyConstraintCascadeChange;
import liquibase.ext.opennms.dropconstraint.DropForeignKeyConstraintCascadeGenerator;
import liquibase.ext.opennms.setsequence.SetSequenceChange;
import liquibase.ext.opennms.setsequence.SetSequenceGenerator;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO: consolidate common parts with Migrator
 */
public class ClassLoaderBasedMigrator {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(ClassLoaderBasedMigrator.class);

    private Logger log = DEFAULT_LOGGER;

    public static final String LIQUIBASE_CHANGELOG_FILENAME = "changelog.xml";

    private static final Logger LOG = LoggerFactory.getLogger(ClassLoaderBasedMigrator.class);
    private static final Pattern POSTGRESQL_VERSION_PATTERN = Pattern.compile("^(?:PostgreSQL|EnterpriseDB) (\\d+\\.\\d+)");

    private static final String IPLIKE_SQL_RESOURCE = "iplike.sql";

    private final ClassLoader m_classLoader;

    private DataSource m_dataSource;
    private DataSource m_adminDataSource;
    private Float m_databaseVersion;

    private String m_databaseName;
    private String m_schemaName;
    private String m_databaseUser;
    private String m_databasePassword;
    private String m_adminUser;
    private String m_adminPassword;

    public ClassLoaderBasedMigrator() {
        this.m_classLoader = this.getClass().getClassLoader();
    }

    public ClassLoaderBasedMigrator(ClassLoader classLoader) {
        this.m_classLoader = classLoader;
    }

    /**
     * <p>getDataSource</p>
     *
     * @return a {@link DataSource} object.
     */
    public DataSource getDataSource() {
        return m_dataSource;
    }

    /**
     * <p>setDataSource</p>
     *
     * @param dataSource a {@link DataSource} object.
     */
    public void setDataSource(final DataSource dataSource) {
        m_dataSource = dataSource;
    }

    /**
     * <p>getAdminDataSource</p>
     *
     * @return a {@link DataSource} object.
     */
    public DataSource getAdminDataSource() {
        return m_adminDataSource;
    }

    /**
     * <p>setAdminDataSource</p>
     *
     * @param dataSource a {@link DataSource} object.
     */
    public void setAdminDataSource(final DataSource dataSource) {
        m_adminDataSource = dataSource;
    }

    /**
     * <p>getDatabaseName</p>
     *
     * @return a {@link String} object.
     */
    public String getDatabaseName() {
        return m_databaseName;
    }
    /**
     * <p>setDatabaseName</p>
     *
     * @param databaseName a {@link String} object.
     */
    public void setDatabaseName(String databaseName) {
        m_databaseName = databaseName;
    }

    public String getSchemaName() {
    	return m_schemaName;
    }

    public void setSchemaName(final String schemaName) {
    	m_schemaName = schemaName;
    }

    /**
     * <p>getDatabaseUser</p>
     *
     * @return a {@link String} object.
     */
    public String getDatabaseUser() {
        return m_databaseUser;
    }
    /**
     * <p>setDatabaseUser</p>
     *
     * @param databaseUser a {@link String} object.
     */
    public void setDatabaseUser(String databaseUser) {
        m_databaseUser = databaseUser;
    }

    /**
     * <p>getDatabasePassword</p>
     *
     * @return a {@link String} object.
     */
    public String getDatabasePassword() {
        return m_databasePassword;
    }
    /**
     * <p>setDatabasePassword</p>
     *
     * @param databasePassword a {@link String} object.
     */
    public void setDatabasePassword(String databasePassword) {
        m_databasePassword = databasePassword;
    }

    /**
     * <p>getAdminUser</p>
     *
     * @return a {@link String} object.
     */
    public String getAdminUser() {
        return m_adminUser;
    }
    /**
     * <p>setAdminUser</p>
     *
     * @param adminUser a {@link String} object.
     */
    public void setAdminUser(String adminUser) {
        m_adminUser = adminUser;
    }

    /**
     * <p>getAdminPassword</p>
     *
     * @return a {@link String} object.
     */
    public String getAdminPassword() {
        return m_adminPassword;
    }
    /**
     * <p>setAdminPassword</p>
     *
     * @param adminPassword a {@link String} object.
     */
    public void setAdminPassword(String adminPassword) {
        m_adminPassword = adminPassword;
    }

    /**
     * <p>getDatabaseVersion</p>
     *
     * @return a {@link Float} object.
     * @throws MigrationException if any.
     */
    public Float getDatabaseVersion() throws MigrationException {
        if (m_databaseVersion == null) {
            String versionString = null;
            Statement st = null;
            ResultSet rs = null;
            Connection c = null;
            try {
                c = m_adminDataSource.getConnection();
                st = c.createStatement();
                rs = st.executeQuery("SELECT version()");
                if (!rs.next()) {
                    throw new MigrationException("Database didn't return any rows for 'SELECT version()'");
                }

                versionString = rs.getString(1);

                rs.close();
                st.close();
            } catch (final SQLException e) {
                throw new MigrationException("an error occurred getting the version from the database", e);
            } finally {
                cleanUpDatabase(c, null, st, rs);
            }
            final Matcher m = POSTGRESQL_VERSION_PATTERN.matcher(versionString);

            if (!m.find()) {
                throw new MigrationException("Could not parse version number out of version string: " + versionString);
            }

            m_databaseVersion = Float.parseFloat(m.group(1));
        }

        return m_databaseVersion;
    }

    /**
     * Get the expected shared object/JNI library extension for this platform.
     * @return
     */
    private String getSharedObjectExtension(final boolean jni) {
        final String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("windows")) {
            return "dll";
        } else if (osName.startsWith("mac")) {
            if (jni) {
                return "jnilib";
            } else {
                return "so";
            }
        }
        return "so";
    }

    /**
     * <p>createLangPlPgsql</p>
     *
     * @throws MigrationException if any.
     */
    public void createLangPlPgsql() throws MigrationException {
        LOG.info("adding PL/PgSQL support to the database, if necessary");
        Statement st = null;
        ResultSet rs = null;
        Connection c = null;
        try {
            c = m_dataSource.getConnection();
            st = c.createStatement();
            rs = st.executeQuery("SELECT oid FROM pg_proc WHERE " + "proname='plpgsql_call_handler' AND " + "proargtypes = ''");
            if (rs.next()) {
                LOG.info("PL/PgSQL call handler exists");
            } else {
                LOG.info("adding PL/PgSQL call handler");
                st.execute("CREATE FUNCTION plpgsql_call_handler () " + "RETURNS OPAQUE AS '$libdir/plpgsql." + getSharedObjectExtension(false) + "' LANGUAGE 'c'");
            }
            rs.close();

            rs = st.executeQuery("SELECT pg_language.oid "
                    + "FROM pg_language, pg_proc WHERE "
                    + "pg_proc.proname='plpgsql_call_handler' AND "
                    + "pg_proc.proargtypes = '' AND "
                    + "pg_proc.oid = pg_language.lanplcallfoid AND "
                    + "pg_language.lanname = 'plpgsql'");
            if (rs.next()) {
                LOG.info("PL/PgSQL language exists");
            } else {
                LOG.info("adding PL/PgSQL language");
                //noinspection SqlNoDataSourceInspection,Duplicates
                st.execute("CREATE TRUSTED PROCEDURAL LANGUAGE 'plpgsql' "
                        + "HANDLER plpgsql_call_handler LANCOMPILER 'PL/pgSQL'");
            }
        } catch (final SQLException e) {
            throw new MigrationException("an error occurred getting the version from the database", e);
        } finally {
            cleanUpDatabase(c, null, st, rs);
        }
    }

    /**
     * <p>checkUnicode</p>
     *
     * @throws Exception if any.
     */
    public void checkUnicode() throws Exception {
        LOG.info("checking if database \"" + getDatabaseName() + "\" is unicode");

        Statement st = null;
        ResultSet rs = null;
        Connection c = null;

        try {
            c = m_adminDataSource.getConnection();
            st = c.createStatement();
            rs = st.executeQuery("SELECT encoding FROM pg_database WHERE LOWER(datname)='" + getDatabaseName().toLowerCase() + "'");
            if (rs.next()) {
                if (rs.getInt(1) == 5 || rs.getInt(1) == 6) {
                    return;
                }
            }

            throw new MigrationException("OpenNMS requires a Unicode database.  Please delete and recreate your\ndatabase and try again.");
        } finally {
            cleanUpDatabase(c, null, st, rs);
        }
    }

    /**
     * <p>databaseSetUser</p>
     *
     * @throws SQLException if any.
     */
    public void databaseSetOwner() throws MigrationException {
        PreparedStatement st = null;
        ResultSet rs = null;
        Connection c = null;

        try {
            c = m_adminDataSource.getConnection();

            final String[] tableTypes = {"TABLE"};
            rs = c.getMetaData().getTables(null, "public", "%", tableTypes);

            final HashSet<String> objects = new HashSet<String>();
            while (rs.next()) {
                objects.add(rs.getString("TABLE_NAME"));
            }

            //
            // NOTE: have seen problems using parameters here; using string manipulation instead; note that sql-injection
            //       should not be an issue because the table names are received from the DB, and the user-name is
            //       configured.  Still, using parameter injection would be preferable.
            //
//            st = c.prepareStatement("ALTER TABLE ? OWNER TO ?");
//            for (final String objName : objects) {
//                st.setString(1, objName);
//                st.setString(2, getDatabaseUser());
//                st.execute();
//            }

            for (final String objName : objects) {
                st = c.prepareStatement("ALTER TABLE \"" + objName + "\" OWNER TO \"" + getDatabaseUser() + "\"");
                st.execute();
            }

        } catch (SQLException e) {
            throw new MigrationException("an error occurred setting table ownership " + st, e);
        } finally {
            cleanUpDatabase(c, null, st, rs);
        }
    }

    /**
     * <p>vacuumDatabase</p>
     *
     * @param full a boolean.
     * @throws SQLException if any.
     */
    public void vacuumDatabase(final boolean full) throws MigrationException {
        Connection c = null;
        Statement st = null;

        try {
            c = m_dataSource.getConnection();

            st = c.createStatement();
            LOG.info("optimizing database (VACUUM ANALYZE)");
            st.execute("VACUUM ANALYZE");

            if (full) {
                LOG.info("recovering database disk space (VACUUM FULL)");
                st.execute("VACUUM FULL");
            }
        } catch (SQLException e) {
            throw new MigrationException("an error occurred vacuuming the databse", e);
        } finally {
            cleanUpDatabase(c, null, st, null);
        }
    }

    /**
     * <p>updateIplike</p>
     *
     * @throws Exception if any.
     */
    public void updateIplike() throws MigrationException {

        boolean insert_iplike = !isIpLikeUsable();

        if (insert_iplike) {
            dropExistingIpLike();

            if (!installCIpLike("foo")) {
                setupPlPgsqlIplike();
            }
        }

        // XXX This error is generated from Postgres if eventtime(text)
        // does not exist:
        // ERROR: function eventtime(text) does not exist
        LOG.info("checking for stale eventtime.so references");
        Connection c = null;
        Statement st = null;
        try {
            c = m_dataSource.getConnection();
            st = c.createStatement();
            st.execute("DROP FUNCTION eventtime(text)");
        } catch (final SQLException e) {
            /*
             * SQL Status code: 42883: ERROR: function %s does not exist
             */
            if (e.toString().indexOf("does not exist") != -1
                    || "42883".equals(e.getSQLState())) {
            } else {
                throw new MigrationException("error checking for stale eventtime.so references", e);
            }
        } finally {
            cleanUpDatabase(c, null, st, null);
        }
    }

    /**
     * <p>isIpLikeUsable</p>
     *
     * @return a boolean.
     */
    public boolean isIpLikeUsable() throws MigrationException {
        Connection c = null;
        Statement st = null;

        try {
            LOG.info("checking if iplike is usable");
            c = m_dataSource.getConnection();
            st = c.createStatement();

            try {
                st.execute("SELECT IPLIKE('127.0.0.1', '*.*.*.*')");
            } catch (final SQLException selectException) {
                return false;
            }

            st.close();

            LOG.info("checking if iplike supports IPv6");
            st = c.createStatement();
            st.execute("SELECT IPLIKE('fe80:0000:5ab0:35ff:feee:cecd', 'fe80:*::cecd')");
        } catch (final SQLException e) {
            throw new MigrationException("error checking if iplike is usable", e);
        } finally {
            cleanUpDatabase(c, null, st, null);
        }

        return true;
    }

    private boolean installCIpLike(String pgIplikeLocation) throws MigrationException {
        if (pgIplikeLocation == null) {
            LOG.info("Skipped inserting C iplike function (location of iplike function not set)");
            return false;
        }

        LOG.info("inserting C iplike function");

        Statement st = null;
        Connection c = null;

        try {
            c  = m_dataSource.getConnection();
            st = c.createStatement();
            try {
                st.execute("CREATE FUNCTION iplike(text,text) RETURNS bool " + "AS '" + pgIplikeLocation + "' LANGUAGE 'c' WITH(isstrict)");
                return true;
            } catch (final SQLException e) {
                return false;
            }
        } catch (final SQLException e) {
            throw new MigrationException("error installing C iplike function", e);
        } finally {
            cleanUpDatabase(c, null, st, null);
        }
    }

    public void dropExistingIpLike() throws MigrationException {
        Connection c = null;
        Statement st = null;

        LOG.info("removing existing iplike definition (if any)");
        try {
            c = m_dataSource.getConnection();
            st = c.createStatement();
            st.execute("DROP FUNCTION iplike(text,text)");
        } catch (final SQLException dropException) {
            if (dropException.toString().contains("does not exist")
                    || "42883".equals(dropException.getSQLState())) {
            } else {
                throw new MigrationException("could not remove existing iplike definition (if it exists)", dropException);
            }
        } finally {
            cleanUpDatabase(c, null, st, null);
        }
    }

    /**
     * <p>setupPlPgsqlIplike</p>
     *
     * @throws Exception if any.
     */
    public void setupPlPgsqlIplike() throws MigrationException {
        LOG.info("inserting PL/pgSQL iplike function");

        InputStream sqlfile = null;
        final StringBuffer createFunction = new StringBuffer();
        try {
            sqlfile = getClass().getResourceAsStream(IPLIKE_SQL_RESOURCE);
            if (sqlfile == null) {
                throw new MigrationException("unable to locate " + IPLIKE_SQL_RESOURCE + " from class " + getClass());
            }

            final BufferedReader in = new BufferedReader(new InputStreamReader(sqlfile, StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                createFunction.append(line).append("\n");
            }
        } catch (final IOException e) {
            throw new MigrationException("error reading PL/pgSQL iplike function from file " + IPLIKE_SQL_RESOURCE, e);
        } finally {
            // don't forget to close the input stream
            try {
                if (sqlfile != null) {
                    sqlfile.close();
                }
            } catch (final IOException e) {
                // purposefully eat it so we don't hide any exceptions that occurred earlier (and matter more)
            }
        }

        Connection c = null;
        Statement st = null;
        try {
            c = m_dataSource.getConnection();
            st = c.createStatement();

            st.execute(createFunction.toString());
        } catch (final SQLException e) {
            throw new MigrationException("could not insert PL/pgSQL iplike function", e);
        } finally {
            cleanUpDatabase(c, null, st, null);
        }
    }

    /**
     * <p>migrate</p>
     *
     * @param changelogUri
     * @throws MigrationException if any.
     */
    public void migrate(String changelogUri) throws MigrationException {
        Connection connection = null;

        try {
            connection = m_dataSource.getConnection();

            Map<String, String> changeLogParameters = this.getChangeLogParameters();

            Liquibase liquibase = this.prepareLiquibase(connection, changelogUri, changeLogParameters);

            String contexts = getLiquibaseContexts();

            liquibase.update(new Contexts(contexts));
        } catch (final Throwable e) {
            throw new MigrationException("unable to migrate the database: " + e.getMessage(), e);
        } finally {
            cleanUpDatabase(connection, null, null, null);
        }
    }

    public static String getLiquibaseContexts() {
        return System.getProperty("opennms.contexts", "production");
    }

    private Map<String, String> getChangeLogParameters() {
        final Map<String,String> parameters = new HashMap<>();
        parameters.put("install.database.admin.user", getAdminUser());
        parameters.put("install.database.admin.password", getAdminPassword());
        parameters.put("install.database.user", getDatabaseUser());
        return parameters;
    }

    private void cleanUpDatabase(final Connection c, DatabaseConnection dbc, final Statement st, final ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (final SQLException e) {
                LOG.warn("Failed to close result set.", e);
            }
        }
        if (st != null) {
            try {
                st.close();
            } catch (final SQLException e) {
                LOG.warn("Failed to close statement.", e);
            }
        }
        if (dbc != null) {
            try {
                dbc.close();
            } catch (final DatabaseException e) {
                LOG.warn("Failed to close database connection.", e);
            }
        }
        if (c != null) {
            try {
                c.close();
            } catch (final SQLException e) {
                LOG.warn("Failed to close connection.", e);
            }
        }
    }

    // Ensures that the database time and the system time running the installer match
    // If the difference is greater than 1s, it fails
    public void checkTime() throws Exception {
        LOG.info("checking if time of database \"" + getDatabaseName() + "\" is matching system time");

        try (Statement st = m_adminDataSource.getConnection().createStatement()) {
            final long beforeQueryTime = System.currentTimeMillis();
            try (ResultSet rs = st.executeQuery("SELECT NOW()")) {
                if (rs.next()) {
                    final Timestamp currentDatabaseTime = rs.getTimestamp(1);
                    final long currentSystemTime = System.currentTimeMillis();
                    final long diff = currentDatabaseTime.getTime() - currentSystemTime;
                    final long queryExecuteDelta = Math.abs(currentSystemTime - beforeQueryTime);
                    if (Math.abs(diff) > 1000 + queryExecuteDelta) {
                        LOG.info("NOT OK");
                        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
                        final String databaseDateString = simpleDateFormat.format(new Date(currentDatabaseTime.getTime()));
                        final String systemTimeDateString = simpleDateFormat.format(new Date(currentSystemTime));
                        throw new Exception("Database time and system time differ."
                                + "System time: " + systemTimeDateString + ", database time: " + databaseDateString
                                + ", diff: " + Math.abs(diff) + "ms. The maximum allowed difference is 1000ms."
                                + " Please update either the database time or system time");
                    }
                    LOG.info("OK");
                }
            }
        }
    }

    public void setupDatabase(boolean updateDatabase, boolean vacuum, boolean fullVacuum, boolean iplike, boolean timescaleDB) throws MigrationException, Exception, IOException {
        if (updateDatabase) {
            this.createLangPlPgsql();
        }

        checkUnicode();
        checkTime();

        if (updateDatabase) {
            databaseSetOwner();

            for (String changelogUri : getLiquibaseChangelogs(true)) {
                LOG.info("- Running migration for changelog: {}", changelogUri);
                migrate(changelogUri);
            }
        }

        if (vacuum) {
            vacuumDatabase(fullVacuum);
        }

        if (iplike) {
            updateIplike();
        }
    }

    public Collection<String> getLiquibaseChangelogs(boolean required) throws IOException, Exception {

        List<String> located = new LinkedList<>();

        // TODO: support multiple "changelog.xml" files?
        URL url = this.m_classLoader.getResource(LIQUIBASE_CHANGELOG_FILENAME);
        if (url != null) {
            located.add(url.toString());
        }

        if (( required ) && ( located.isEmpty() )) {
            throw new MigrationException("Could not find the '" + LIQUIBASE_CHANGELOG_FILENAME + "' file.");
        }

        return located;
    }

//========================================
// Miscellaneous Internals
//========================================

    protected Liquibase prepareLiquibase(Connection databaseConnection, String changelogUri, Map<String, String> changelogParameters) throws LiquibaseException {
        this.registerLiquibaseExtensions();

        DatabaseConnection liquibaseDatabaseConnection = new JdbcConnection(databaseConnection);

        PostgresDatabase postgresDatabase = new PostgresDatabase();
        postgresDatabase.setConnection(liquibaseDatabaseConnection);
        postgresDatabase.setDefaultSchemaName(this.m_schemaName);

        ClassLoaderResourceAccessor classLoaderResourceAccessor =
                new MyBundleClassLoaderResourceAccessor(this.m_classLoader);

        Liquibase liquibase = new Liquibase(changelogUri, classLoaderResourceAccessor, postgresDatabase);

        //
        // Populate Liquibase with the changelog parameters.
        //
        changelogParameters.forEach(liquibase::setChangeLogParameter);

        return liquibase;
    }

    private void registerLiquibaseExtensions() {
        SqlGeneratorFactory.getInstance().register(new DropForeignKeyConstraintCascadeGenerator());
        SqlGeneratorFactory.getInstance().register(new CreateIndexWithWhereGenerator());
        SqlGeneratorFactory.getInstance().register(new SetSequenceGenerator());

        ChangeFactory.getInstance().register(AddNamedAutoIncrementChange.class);
        ChangeFactory.getInstance().register(CreateIndexWithWhereChange.class);
        ChangeFactory.getInstance().register(DropForeignKeyConstraintCascadeChange.class);
        ChangeFactory.getInstance().register(SetSequenceChange.class);
    }

    /**
     * Resource Accessor that extracts the path from a resource URL for use with getResourceAsStream
     */
    private class MyBundleClassLoaderResourceAccessor extends ClassLoaderResourceAccessor {
        public MyBundleClassLoaderResourceAccessor(ClassLoader classLoader) {
            super(classLoader);
        }

        @Override
        public Set<InputStream> getResourcesAsStream(String path) throws IOException {
            if (path.startsWith("bundle://")) {
                String updPath = path.replaceFirst("bundle://[^/]*", "");

                ClassLoaderBasedMigrator.this.log.debug("MAPPED BUNDLE PATH {} => {}", path, updPath);

                path = updPath;
            }

            return super.getResourcesAsStream(path);
        }



        @Override
        public Set<String> list(String relativeTo, String path, boolean includeFiles, boolean includeDirectories, boolean recursive) throws IOException {
            if (path.startsWith("bundle://")) {
                String updPath = path.replaceFirst("bundle://[^/]*", "");

                ClassLoaderBasedMigrator.this.log.debug("MAPPED BUNDLE PATH {} => {}", path, updPath);

                path = updPath;
            }

            return super.list(relativeTo, path, includeFiles, includeDirectories, recursive);
        }
    };
}
