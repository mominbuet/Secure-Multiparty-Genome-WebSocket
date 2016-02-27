/*
 * Md. Momin Al Aziz momin.aziz.cse @ gmail.com	
 * http://www.mominalaziz.com
 */
package WebUtils;

import connections.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.json.JsonObject;

/**
 *
 * @author shad942
 */
public class Query {

    public enum QueryType {

        Count, Chi, MAF, Edit,CountWithoutGC, EditOPE
    }
    JsonObject query;
    int queryID;
    public Map<String, String> results;
    QueryType queryType;
    boolean isSecure;
    Connection connection;
    public int editDistanceCount = 0;

    public boolean isSecure() {
        return isSecure;
    }

    public Query(JsonObject query, int queryID, QueryType queryType, boolean isSecure, Connection connection) {
        this.query = query;
        this.connection = connection;
        this.isSecure = isSecure;
        this.queryID = queryID;
        this.queryType = queryType;
        this.results = new HashMap<>();
    }

    public Connection getConnection() {
        return connection;
    }

    public JsonObject getQuery() {
        return query;
    }

    public void setQuery(JsonObject query) {
        this.query = query;
    }

    public int getQueryID() {
        return queryID;
    }

    public void setQueryID(int queryID) {
        this.queryID = queryID;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public void setQueryType(QueryType queryType) {
        this.queryType = queryType;
    }

}
