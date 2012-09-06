/**
 *  Starschema Big Query JDBC Driver
 *  Copyright (C) 2012, Starschema Ltd.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 * This class implements the java.sql.Statement interface
 */

package net.starschema.clouddb.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.google.api.services.bigquery.model.Job;

/**
 * This class implements java.sql.Statement
 * 
 * @author Horv�th Attila
 * @author Balazs Gunics
 * 
 */
public class BQStatement implements java.sql.Statement {

    /** String containing the context of the Project */
    private String ProjectId = null;
    /** Reference for the Connection that created this Statement object */
    private BQConnection connection;
    /** Variable that stores the set query timeout */
    private int querytimeout = Integer.MAX_VALUE;
    /** Instance of log4j.Logger */
    Logger logger = Logger.getLogger(BQStatement.class);

    /**
     * Variable stores the time an execute is made
     */
    private long starttime = 0;

    /** Variable that stores the closed state of the statement */
    private boolean closed = false;

    /** Variable that stores the max row number which can be stored in the resultset */ 
    private int resultMaxRowCount = Integer.MAX_VALUE-1;
    
    /** Reference to store the ran Query run by Executequery or Execute */
    ResultSet resset = null;

    /** Variable to Store EscapeProc state */
    boolean EscapeProc = false;

    /**
     * Constructor for BQStatement object just initializes local variables
     * 
     * @param projectid
     * @param bqConnection
     */
    public BQStatement(String projectid, BQConnection bqConnection) {
        this.ProjectId = projectid;
        this.connection = bqConnection;
        this.resultSetType = ResultSet.TYPE_SCROLL_INSENSITIVE;
        this.resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;
    }

    /**These Variables contain information about the type of resultset this statement creates*/
    int resultSetType;
    int resultSetConcurrency;
    
    /**
     * Constructor for BQStatement object just initializes local variables
     * @param projectid2
     * @param bqConnection
     * @param resultSetType
     * @param resultSetConcurrency
     * @throws BQSQLException 
     */
    public BQStatement(String projectid, BQConnection bqConnection,
            int resultSetType, int resultSetConcurrency) throws BQSQLException {
        if (resultSetConcurrency == ResultSet.CONCUR_UPDATABLE)
            throw new BQSQLException("The Resultset Concurrency can't be ResultSet.CONCUR_UPDATABLE");
        
        this.ProjectId = projectid;
        this.connection = bqConnection;
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLException
     */
    @Override
    public void addBatch(String arg0) throws SQLException {
        throw new BQSQLException("Not implemented." + "addBatch(string)");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLFeatureNotSupportedException
     */
    @Override
    public void cancel() throws SQLException {
        throw new BQSQLFeatureNotSupportedException("cancel()");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLException
     */
    @Override
    public void clearBatch() throws SQLException {
        throw new BQSQLException("Not implemented." + "clearBatch()");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLException
     */
    @Override
    public void clearWarnings() throws SQLException {
        throw new BQSQLException("Not implemented." + "clearWarnings()");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Only sets closed boolean to false
     * </p>
     */
    @Override
    public void close() throws SQLException {
        this.closed = false;
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Executes the given SQL statement on BigQuery (note: it returns only 1
     * resultset). This function directly uses executeQuery function
     * </p>
     */
    @Override
    public boolean execute(String arg0) throws SQLException {
        if (this.isClosed())
            throw new BQSQLException("This Statement is Closed");
        this.resset = this.executeQuery(arg0);

        if (this.resset != null)
            return true;
        else
            return false;
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLFeatureNotSupportedException
     */
    @Override
    public boolean execute(String arg0, int arg1) throws SQLException {
        throw new BQSQLFeatureNotSupportedException("execute(String, int)");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLFeatureNotSupportedException
     */
    @Override
    public boolean execute(String arg0, int[] arg1) throws SQLException {
        throw new BQSQLFeatureNotSupportedException("execute(string,int[])");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLFeatureNotSupportedException
     */
    @Override
    public boolean execute(String arg0, String[] arg1) throws SQLException {
        throw new BQSQLFeatureNotSupportedException("execute(string,string[])");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLException
     */
    @Override
    public int[] executeBatch() throws SQLException {
        throw new BQSQLException("Not implemented." + "executeBatch()");
    }

    /** {@inheritDoc} */
    @Override
    public ResultSet executeQuery(String querySql) throws SQLException {
        if (this.isClosed())
            throw new BQSQLException("This Statement is Closed");
        this.starttime = System.currentTimeMillis();
        Job referencedJob;
        try {
            // Gets the Job reference of the completed job with give Query
            referencedJob = BQSupportFuncts.startQuery(
                    this.connection.getBigquery(), this.ProjectId, querySql);
        } catch (IOException e) {
            throw new BQSQLException(e);
        }
        try {
            do {
                if (BQSupportFuncts.getQueryState(referencedJob,
                        this.connection.getBigquery(), this.ProjectId).equals(
                        "DONE"))
                    return new BQResultSet(BQSupportFuncts.GetQueryResults(
                            this.connection.getBigquery(), this.ProjectId,
                            referencedJob), this);
                // Pause execution for half second before polling job status
                // again, to
                // reduce unnecessary calls to the BigQUery API and lower
                // overall
                // application bandwidth.
                Thread.sleep(500);
                this.logger.debug("slept for 500" + "ms, querytimeout is: "
                        + this.querytimeout + "s");
            } while (System.currentTimeMillis() - this.starttime <= (long) this.querytimeout * 1000);
            // it runs for a minimum of 1 time
        } catch (IOException e) {
            throw new BQSQLException(e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // here we should kill/stop the running job, but bigquery doesn't
        // support that :(
        throw new BQSQLException(
                "Query run took more than the specified timeout");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLException
     */
    @Override
    public int executeUpdate(String arg0) throws SQLException {
        throw new BQSQLFeatureNotSupportedException("executeUpdate(string)");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLFeatureNotSupportedException
     */
    @Override
    public int executeUpdate(String arg0, int arg1) throws SQLException {
        throw new BQSQLFeatureNotSupportedException("executeUpdate(String,int)");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLFeatureNotSupportedException
     */
    @Override
    public int executeUpdate(String arg0, int[] arg1) throws SQLException {
        throw new BQSQLFeatureNotSupportedException(
                "executeUpdate(string,int[])");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLFeatureNotSupportedException
     */
    @Override
    public int executeUpdate(String arg0, String[] arg1) throws SQLException {
        throw new BQSQLFeatureNotSupportedException(
                "execute(update(string,string[])");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLException
     */
    @Override
    public Connection getConnection() throws SQLException {
        throw new BQSQLException("Not implemented." + "getConnection()");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Fetch direction is unknown.
     * </p>
     * 
     * @return FETCH_UNKNOWN
     */
    @Override
    public int getFetchDirection() throws SQLException {
        return ResultSet.FETCH_UNKNOWN;
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLException
     */
    @Override
    public int getFetchSize() throws SQLException {
        throw new BQSQLException("Not implemented." + "getFetchSize()");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLException
     */
    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        throw new BQSQLFeatureNotSupportedException("getGeneratedKeys()");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLException
     */
    @Override
    public int getMaxFieldSize() throws SQLException {
        throw new BQSQLException("Not implemented." + "getMaxFieldSize()");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * We store it in an array which indexed through, int
     * </p>
     * We could return Integer.MAX_VALUE too, but i don't think we could get
     * that much row.
     * 
     * @return 0 -
     */
    @Override
    public int getMaxRows() throws SQLException {
        return this.resultMaxRowCount;
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Multiple result sets are not supported currently.
     * </p>
     * 
     * @return false;
     */
    @Override
    public boolean getMoreResults() throws SQLException {
        return false;
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Multiple result sets are not supported currently. we check that the
     * result set is open, the parameter is acceptable, and close our current
     * resultset or throw a FeatureNotSupportedException
     * </p>
     * 
     * @param current
     *            - one of the following Statement constants indicating what
     *            should happen to current ResultSet objects obtained using the
     *            method getResultSet: Statement.CLOSE_CURRENT_RESULT,
     *            Statement.KEEP_CURRENT_RESULT, or Statement.CLOSE_ALL_RESULTS
     * @throws BQSQLException
     */
    @Override
    public boolean getMoreResults(int current) throws SQLException {
        if (this.closed)
            throw new BQSQLException("Statement is closed.");
        if (current == Statement.CLOSE_CURRENT_RESULT
                || current == Statement.KEEP_CURRENT_RESULT
                || current == Statement.CLOSE_ALL_RESULTS) {

            if (BQDatabaseMetadata.multipleOpenResultsSupported
                    && (current == Statement.KEEP_CURRENT_RESULT || current == Statement.CLOSE_ALL_RESULTS))
                throw new BQSQLFeatureNotSupportedException();
            // Statement.CLOSE_CURRENT_RESULT
            this.close();
            return false;
        } else
            throw new BQSQLException("Wrong parameter.");
    }

    /** {@inheritDoc} */
    @Override
    public int getQueryTimeout() throws SQLException {
        if (this.isClosed())
            throw new BQSQLException("This Statement is Closed");
        if (this.starttime == 0)
            return 0;
        if (this.querytimeout == Integer.MAX_VALUE)
            return 0;
        else {
            if (System.currentTimeMillis() - this.starttime > this.querytimeout)
                throw new BQSQLException("Time is over");
            return this.querytimeout;
        }
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Gives back reultset stored in resset
     * </p>
     */
    @Override
    public ResultSet getResultSet() throws SQLException {
        if (this.isClosed())
            throw new BQSQLException("This Statement is Closed");
        if (this.resset != null)
            return this.resset;
        else
            return null;
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * The driver is read only currently.
     * </p>
     * 
     * @return CONCUR_READ_ONLY
     */
    @Override
    public int getResultSetConcurrency() throws SQLException {
        return ResultSet.CONCUR_READ_ONLY;
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Read only mode, no commit.
     * </p>
     * 
     * @return CLOSE_CURSORS_AT_COMMIT
     */
    @Override
    public int getResultSetHoldability() throws SQLException {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
        // TODO
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Updates and deletes not supported.
     * </p>
     * 
     * @return TYPE_SCROLL_INSENSITIVE
     */
    @Override
    public int getResultSetType() throws SQLException {
        return ResultSet.TYPE_SCROLL_INSENSITIVE;
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Result will be a ResultSet object.
     * </p>
     * 
     * @return -1
     */
    @Override
    public int getUpdateCount() throws SQLException {
        return -1;
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * returns null
     * </p>
     * 
     * @return null
     */
    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
        // TODO Implement Warning Handling
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Returns the value of boolean closed
     * </p>
     */
    @Override
    public boolean isClosed() throws SQLException {
        return this.closed;
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLException
     */
    @Override
    public boolean isPoolable() throws SQLException {
        throw new BQSQLException("Not implemented." + "isPoolable()");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @return false
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * FeatureNotSupportedExceptionjpg
     * </p>
     * 
     * @throws BQSQLFeatureNotSupportedException
     */
    @Override
    public void setCursorName(String arg0) throws SQLException {
        throw new BQSQLFeatureNotSupportedException("setCursorName(string)");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Now Only setf this.EscapeProc to arg0
     * </p>
     */
    @Override
    public void setEscapeProcessing(boolean arg0) throws SQLException {
        if (this.isClosed())
            throw new BQSQLException("This Statement is Closed");
        this.EscapeProc = arg0;
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLException
     */
    @Override
    public void setFetchDirection(int arg0) throws SQLException {
        throw new BQSQLException("Not implemented." + "setFetchDirection(int)");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLException
     */
    @Override
    public void setFetchSize(int arg0) throws SQLException {
        throw new BQSQLException("Not implemented." + "setFetchSize(int)");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLException
     */
    @Override
    public void setMaxFieldSize(int arg0) throws SQLException {
        throw new BQSQLException("Not implemented." + "setMaxFieldSize(int)");
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLException
     */
    @Override
    public void setMaxRows(int arg0) throws SQLException {
        this.resultMaxRowCount = arg0==0?arg0:Integer.MAX_VALUE-1;
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Not implemented yet.
     * </p>
     * 
     * @throws BQSQLException
     */
    @Override
    public void setPoolable(boolean arg0) throws SQLException {
        throw new BQSQLException("Not implemented." + "setPoolable(bool)");
    }

    /** {@inheritDoc} */
    @Override
    public void setQueryTimeout(int arg0) throws SQLException {
        if (this.isClosed())
            throw new BQSQLException("This Statement is Closed");
        if (arg0 == 0)
            this.querytimeout = Integer.MAX_VALUE;
        else
            this.querytimeout = arg0;
    }

    /**
     * <p>
     * <h1>Implementation Details:</h1><br>
     * Always throws SQLException
     * </p>
     * 
     * @throws SQLException
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new BQSQLException("not found");
    }

}