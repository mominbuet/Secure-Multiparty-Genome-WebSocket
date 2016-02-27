/*
 * Md. Momin Al Aziz momin.aziz.cse @ gmail.com	
 * http://www.mominalaziz.com
 */
package org.umanitoba.smc_genome;

import HomomorphicEncryption.Paillier;
import WebUtils.Query;
import WebUtils.Query.QueryType;
import WebUtils.Utils;
import connections.Connection;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 * @author shad942
 */
@ServerEndpoint("/endpoint_smc_genome")
public class smc_genome {

    static int partyID = 1000;
//    private static Set<Session> peers = Collections.synchronizedSet(new HashSet<Session>());
    static Map<String, Connection> parties = new HashMap<>();
    static int queryID = 1221;
    static int packetNo = 2668;
    static int hospitalCount = 1335;
    static Connection cspConnection;
    static Map<String, Query> queryMap = new HashMap<>();
    static Map<String, JsonObject> packetQuery = new HashMap<>();
    static List<EditDistanceHelper> listEditDistanceHelper = new ArrayList<>();
//    static int partyID = 7887;

    /**
     * message is json having type(ping-p,result-r,initiate query-iq, initiate
     * hospitals-ih,initiate CSP-ic, query-q, result assigned against session
     *
     * @param message
     * @param s
     */
    @OnMessage

    public void onMessage(String message, Session s) {
        if (!message.contains("ping")) {
            System.out.println("Message " + message);

        }
        JsonObject jsonObject = Json.createReader(new StringReader(message)).readObject();
        String type = jsonObject.getString("type");

        try {
            if (type.toCharArray()[0] == 'i') {//be careful as the first char is i for initialize
                Connection connection = null;

                switch (type) {
//                    case "iq":
//                        connection = new Connection(s, "", 0, Connection.ConnectionParty.Researcher);
//                        s.getBasicRemote().sendText("Please insert your query...");
//                        break;
                    case "ih":
                        connection = new Connection(s, jsonObject.getString("msg", ""), 0, Connection.ConnectionParty.Hospitals, partyID++);
                        s.getBasicRemote().sendText(Utils.getMessage("wl", getHospitalCount() + ""));
//                        System.out.println("New hospital ");
                        break;
                    case "ic":
                        connection = new Connection(s, jsonObject.getString("message", ""), 0, Connection.ConnectionParty.CSP, partyID++);
                        cspConnection = connection;
                        cspConnection.getSession().getBasicRemote().sendText(Utils.getMessage("welcome CSP", "msg"));
//                        testCSP(cspConnection);
//                        System.out.println("CSP initiated " + cspConnection.getId());

                        break;
                }
                if (connection != null) {

                    parties.put(connection.getSession().getId(), connection);
                    System.out.println("total parties " + parties.size());
                }
            } else {

                switch (type) {
                    case "ack":
                        String packetString = jsonObject.getString("msg");
                        resendQueries(packetString);
                        break;
                    case "ping":
                        break;
                    case ("q"):
                        Connection currentConnection = new Connection(s, "", 0, Connection.ConnectionParty.Researcher, partyID++);
                        s.getBasicRemote().sendText(Utils.getMessage("Welcome", "Your query is received, processing between <b>" + getHospitalCount() + "</b> parties."));
                        Query.QueryType queryType = getQueryType(jsonObject);
                        queryID += 1;
                        //single most important line maybe
                        Query query = new Query(jsonObject, queryID, queryType, getIsSecure(jsonObject), currentConnection);
                        for (Map.Entry<String, Connection> entry : parties.entrySet()) {
                            try {
                                if (entry.getValue().isAlive() && entry.getValue().getCtype() == Connection.ConnectionParty.Hospitals) {
//                              System.out.println("Trying to send " + jsonObject.get("message").toString());

                                    JsonObject jsonObjectToHospital = Json.createObjectBuilder()
                                            .add("type", "q")
                                            .add("msg", jsonObject.get("msg").toString())
                                            .add("queryID", (queryID) + "-" + s.getId())
                                            .build();

                                    query.results.put(entry.getValue().getSession().getId() + "" + entry.getValue().getId(), "");
                                    queryMap.put(currentConnection.getSession().getId(), query);
                                    entry.getValue().getSession().getBasicRemote().sendText(jsonObjectToHospital.toString());
                                    System.out.println("sent to hospital " + jsonObjectToHospital.toString());
                                }
                            } catch (Exception ex) {
                                System.out.println("Exception in sending query " + ex.getMessage());
                                Logger.getLogger(smc_genome.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                            }
                        }

                        break;
                    case ("GCResult"):
                        System.out.println("GCResult " + jsonObject.toString());

                        Query atLastResult = findQuery(Integer.parseInt(jsonObject.getString("query")));
                        Map<String, String> map = new HashMap<>();
                        map.put("msg", jsonObject.getString("result"));
                        map.put("type", "result");
                        System.out.println("Final output to user " + Utils.getMessage(map));
                        atLastResult.getConnection().getSession().getBasicRemote().sendText(Utils.getMessage(map));
                        break;
                    case "resultEditDistanceHospital":
                        JsonObject jsonObjectResult = Json.createReader(new StringReader(jsonObject.getString("result"))).readObject();
                        System.out.println("resultEditDistanceHospital " + jsonObjectResult.toString());
                        query = findQuery(Integer.parseInt(jsonObject.getString("queryID").split("-")[0]));
                        query.getConnection().getSession().getBasicRemote().sendText(Utils.getMessage("info", "received plaintext from some hospital"));
                        EditDistanceHelper editDistanceHelper = null;
                        for (EditDistanceHelper e : listEditDistanceHelper) {
                            if (e.queryID == query.getQueryID()) {
                                editDistanceHelper = e;
                                break;
                            }
                        }
                        if (editDistanceHelper != null) {

                            //code needs to be changed here
                            //order needs to be maintianed.
                            //insecure protocol is alright
                            for (Map.Entry<String, JsonValue> entrySet : jsonObjectResult.entrySet()) {
                                System.out.println(entrySet.getValue().toString());
                                if (editDistanceHelper.wordMap.get(new BigInteger(entrySet.getKey())) != null) {
                                    editDistanceHelper.wordMap.put(new BigInteger(entrySet.getKey()), entrySet.getValue().toString() + editDistanceHelper.wordMap.get(new BigInteger(entrySet.getKey())));
                                } else {
                                    editDistanceHelper.wordMap.put(new BigInteger(entrySet.getKey()), entrySet.getValue().toString());
                                }
                            }
                            boolean flag = (--query.editDistanceCount == 0);
                            System.out.println("query.editDistanceCount " + query.editDistanceCount);
//                            for (String str : editDistanceHelper.wordMap.values()) {
//
//                                if (str == null) {
//                                    flag = false;
//                                    return;
//                                }
////                                else {
////                                    System.out.println(str);
////                                }
//                            }
                            if (query != null && flag) {
                                JsonArrayBuilder jsonArrayWords = Json.createArrayBuilder();
                                for (String val : editDistanceHelper.wordMap.values()) {
                                    if (val != null) {
                                        jsonArrayWords.add(val);
                                    }
                                }
                                query.getConnection().getSession().getBasicRemote().sendText(Utils.getMessage("result", jsonArrayWords.build()));

                            }
                        }
                        break;
                    case "resultEditDistanceCSP":
                        //here CSP has already sent its values sorted, now we need to send it to the hospitals
                        message = jsonObject.getString("msg");
                        String[] tokens = message.split(";");
                        query = findQuery(Integer.parseInt(tokens[0]));
                        if (query == null) {
                            System.out.println("Query is null");
                        }
                        editDistanceHelper = new EditDistanceHelper(Integer.parseInt(tokens[0]));
                        for (String s1 : tokens[1].split(",")) {
                            editDistanceHelper.wordMap.put(new BigInteger(s1), null);
                        }
                        listEditDistanceHelper.add(editDistanceHelper);
                        int i = 0;
                        /**
                         * sending to hospital for raw data with the encrypted
                         * distance.
                         */
                        for (Map.Entry<String, Connection> entry : parties.entrySet()) {
                            try {
                                if (entry.getValue().isAlive() && entry.getValue().getCtype() == Connection.ConnectionParty.Hospitals) {
                                    JsonObject jsonObjectToHospital = Json.createObjectBuilder()
                                            .add("type", "resultEditDistanceCSP")
                                            .add("packetNo", ++packetNo + "-" + entry.getValue().getId())
                                            .add("msg", tokens[1])
                                            .add("queryID", (query.getQueryID()) + "-" + query.getConnection().getSession().getId())
                                            .build();
                                    System.out.println("Putting into packet query " + packetNo + "-" + entry.getValue().getId());
                                    packetQuery.put(packetNo + "-" + entry.getValue().getId(), jsonObjectToHospital);
                                    entry.getValue().getSession().getBasicRemote().sendText(jsonObjectToHospital.toString());
//                                    entry.getValue().getSession().getAsyncRemote().sendText(jsonObjectToHospital.toString());

                                    System.out.println("sent to hospital " + jsonObjectToHospital.toString());
                                    query.getConnection().getSession().getBasicRemote().sendText(Utils.getMessage("info", "sending to Hospital " + (++i) + " for plaintext"));

                                    //make an ack system here
                                    query.editDistanceCount++;
                                }
                            } catch (Exception ex) {
                                System.out.println("Exception in sending query " + ex.getMessage());
                                Logger.getLogger(smc_genome.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                            }
                        }
                        i = 0;
//                        for(int i=1;i<tokens.length;i++){
//                            
//                        }
//                        query.getConnection().getSession().getBasicRemote().sendText();

                        break;
                    case "decryptionWithoutGC":
                        query = findQuery(jsonObject.getInt("queryID"));
                        map = new HashMap<>();
                        map.put("msg", jsonObject.getInt("result") + "");
                        map.put("type", "result");
                        System.out.println("Final output to user " + Utils.getMessage(map));
                        query.getConnection().getSession().getBasicRemote().sendText(Utils.getMessage(map));
                        break;
                    case ("result"):
                        int currentQueryID = Integer.parseInt(jsonObject.getString("queryID").split("-")[0]);
                        query = findQuery(currentQueryID);

                        if (query != null) {
                            int totalResults = resultCount(query.results);
                            if (totalResults != query.results.size()) {
                                for (Map.Entry<String, String> entrySet : query.results.entrySet()) {
                                    if (entrySet.getValue().equals("")) {
                                        entrySet.setValue(jsonObject.getString("result"));
                                        break;
                                    }
                                }
                            }

                            totalResults = resultCount(query.results);
                            System.out.println("Total res " + totalResults + " query result count " + query.results.size());
//                            resendQueries("");//turn it on later
                            if (totalResults == query.results.size() || totalResults == getHospitalCount()) {
                                JsonObject jsonObjectToCSP = null;
                                /**
                                 * For all the result
                                 */
                                switch (query.getQueryType()) {
                                    case EditOPE:
                                        String distances = "";
                                        int timesSending = 0;
//                                        System.out.println("query.results. " + query.results.size());
                                        for (Map.Entry<String, String> entrySet : query.results.entrySet()) {
//                                            jsonArrayBuilder.add(entrySet.getValue());
//                                            System.out.println("endtry " + entrySet.getValue());

                                            distances += entrySet.getValue();

                                            System.out.println("distances " + distances.split(",").length);
                                            if (!getQueryResultsEmpty(query.results)) {
                                                // regular edit distance counts
                                                String tmpMsg = "";
                                                int[] distanceInt = new int[distances.split(",").length];
                                                for (i = 0; i < distanceInt.length; i++) {
                                                    if (!distances.split(",")[i].isEmpty()) {
                                                        distanceInt[i] = Integer.parseInt(distances.split(",")[i]);
                                                    }
                                                }
                                                Arrays.sort(distanceInt);
//                                                    System.out.println("Sorted Integers " + Arrays.asList(distanceInt));
                                                editDistanceHelper = new EditDistanceHelper(query.getQueryID());
                                                for (Integer in : distanceInt) {
                                                    editDistanceHelper.wordMap.put(new BigInteger(in.toString()), null);
                                                    tmpMsg += in + ",";
                                                }
                                                listEditDistanceHelper.add(editDistanceHelper);
                                                for (Map.Entry<String, Connection> entry : parties.entrySet()) {
                                                    try {
                                                        if (entry.getValue().isAlive() && entry.getValue().getCtype() == Connection.ConnectionParty.Hospitals) {
                                                            JsonObject jsonObjectToHospital = Json.createObjectBuilder()
                                                                    .add("type", "resultEditDistanceCSP")
                                                                    .add("msg", tmpMsg)
                                                                    .add("packetNo", ++packetNo + "-" + entry.getValue().getId())
                                                                    .add("queryID", (query.getQueryID()) + "-" + query.getConnection().getSession().getId())
                                                                    .build();
                                                            packetQuery.put(packetNo + "-" + entry.getValue().getId(), jsonObjectToHospital);
                                                            entry.getValue().getSession().getBasicRemote().sendText(jsonObjectToHospital.toString());
                                                            System.out.println("sent to hospital " + entry.getKey() + jsonObjectToHospital.toString());
                                                            query.getConnection().getSession().getBasicRemote().sendText(Utils.getMessage("info", "sending to Hospital " + ++i + " for plaintext"));
                                                            query.editDistanceCount++;
                                                        }
                                                    } catch (Exception ex) {
                                                        System.out.println("Exception in sending query " + ex.getMessage());
                                                        Logger.getLogger(smc_genome.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                                                    }
                                                }
                                                i = 0;
                                            }
                                        }
                                        break;
                                    case Edit:
                                        distances = "";
                                        timesSending = 0;
//                                        System.out.println("query.results. " + query.results.size());
                                        for (Map.Entry<String, String> entrySet : query.results.entrySet()) {
//                                            jsonArrayBuilder.add(entrySet.getValue());
//                                            System.out.println("endtry " + entrySet.getValue());

                                            distances += entrySet.getValue();

                                            System.out.println("distances " + distances.split(",").length);
                                            if (!getQueryResultsEmpty(query.results)) {
                                                if (!query.isSecure()) {
                                                    // regular edit distance counts
                                                    String tmpMsg = "";
                                                    int[] distanceInt = new int[distances.split(",").length];
                                                    for (i = 0; i < distanceInt.length; i++) {
                                                        if (!distances.split(",")[i].isEmpty()) {
                                                            distanceInt[i] = Integer.parseInt(distances.split(",")[i]);
                                                        }
                                                    }
                                                    Arrays.sort(distanceInt);
//                                                    System.out.println("Sorted Integers " + Arrays.asList(distanceInt));
                                                    editDistanceHelper = new EditDistanceHelper(query.getQueryID());
                                                    for (Integer in : distanceInt) {
                                                        editDistanceHelper.wordMap.put(new BigInteger(in.toString()), null);
                                                        tmpMsg += in + ",";
                                                    }
                                                    listEditDistanceHelper.add(editDistanceHelper);
                                                    for (Map.Entry<String, Connection> entry : parties.entrySet()) {
                                                        try {
                                                            if (entry.getValue().isAlive() && entry.getValue().getCtype() == Connection.ConnectionParty.Hospitals) {
                                                                JsonObject jsonObjectToHospital = Json.createObjectBuilder()
                                                                        .add("type", "resultEditDistanceCSP")
                                                                        .add("msg", tmpMsg)
                                                                        .add("packetNo", ++packetNo + "-" + entry.getValue().getId())
                                                                        .add("queryID", (query.getQueryID()) + "-" + query.getConnection().getSession().getId())
                                                                        .build();
                                                                packetQuery.put(packetNo + "-" + entry.getValue().getId(), jsonObjectToHospital);
                                                                entry.getValue().getSession().getBasicRemote().sendText(jsonObjectToHospital.toString());
                                                                System.out.println("sent to hospital " + entry.getKey() + jsonObjectToHospital.toString());
                                                                query.getConnection().getSession().getBasicRemote().sendText(Utils.getMessage("info", "sending to Hospital " + ++i + " for plaintext"));
                                                                query.editDistanceCount++;
                                                            }
                                                        } catch (Exception ex) {
                                                            System.out.println("Exception in sending query " + ex.getMessage());
                                                            Logger.getLogger(smc_genome.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                                                        }
                                                    }
                                                    i = 0;
                                                } else {
                                                    jsonObjectToCSP = Json.createObjectBuilder()
                                                            .add("type", "editdist")
                                                            .add("times", ++timesSending)
                                                            .add("total", query.results.size())
                                                            .add("queryID", query.getQueryID())
                                                            .add("query", query.getQuery())//though it will be given earlier to initiate the keys
                                                            .add("distances", distances)
                                                            .build();
                                                    if (timesSending == query.results.size()) {
                                                        timesSending = 0;
                                                    }
                                                    System.out.println("Sending to csp " + jsonObjectToCSP.toString());
                                                    if (cspConnection.getSession() != null) {
                                                        if (cspConnection.getSession().getBasicRemote() != null) {
                                                            cspConnection.getSession().getBasicRemote().sendText(jsonObjectToCSP.toString());
                                                            query.getConnection().getSession().getBasicRemote().sendText(Utils.getMessage("info", "Sending data to CSP for sorting the distances"));
                                                        } else {
                                                            query.getConnection().getSession().getBasicRemote().sendText(Utils.getMessage("info", "No connection to CSP"));
                                                        }
                                                        distances = "";
                                                    }
                                                }
                                            }

                                        }
                                        break;
                                    case CountWithoutGC:
                                        Paillier paillier = new Paillier(true);
                                        Iterator<String> iterator = query.results.values().iterator();
                                        BigInteger result = new BigInteger(iterator.next());
                                        while (iterator.hasNext()) {
                                            if (query.isSecure()) {
                                                result = paillier.add(result, new BigInteger(iterator.next()));
                                            } else {
                                                result = result.add(new BigInteger(iterator.next()));
//                                                System.out.println("result plain "+result);
                                            }
                                        }
                                        if (query.isSecure()) {
                                            jsonObjectToCSP = Json.createObjectBuilder()
                                                    .add("type", "decryptionWithoutGC")
                                                    .add("query", query.getQuery())//though it will be given earlier to initiate the keys
                                                    .add("queryID", query.getQueryID())
                                                    .add("result", result.toString())
                                                    .build();
                                            if (cspConnection.getSession().getBasicRemote() != null) {
                                                cspConnection.getSession().getBasicRemote().sendText(jsonObjectToCSP.toString());
                                            } else {
                                                query.getConnection().getSession().getBasicRemote().sendText(Utils.getMessage("info", "CSP is no more present"));
                                            }
                                        } else {
                                            Map<String, String> tmpMap = new HashMap<>();
                                            tmpMap.put("msg", result.toString());
                                            tmpMap.put("type", "result");
//                                            System.out.println("Final output(without security) to user " + Utils.getMessage(tmpMap));
                                            query.getConnection().getSession().getBasicRemote().sendText(Utils.getMessage(tmpMap));
                                        }
//                                            System.out.println(GCOperation.getDecryptionWithGC(result, query.getQueryID() + ""));
                                        break;
                                    case Count:
//                                        
                                        paillier = new Paillier(true);
                                        iterator = query.results.values().iterator();
                                        result = new BigInteger(iterator.next());
                                        while (iterator.hasNext()) {
                                            if (query.isSecure()) {
                                                result = paillier.add(result, new BigInteger(iterator.next()));
                                            } else {
                                                result = result.add(new BigInteger(iterator.next()));
//                                                System.out.println("result plain "+result);
                                            }
                                        }

                                        if (query.isSecure()) {
//                                            System.out.println("Final out " + paillier.Decryption(result));
                                            jsonObjectToCSP = Json.createObjectBuilder()
                                                    .add("type", "decryption")
                                                    .add("query", query.getQuery())//though it will be given earlier to initiate the keys
                                                    .add("queryID", query.getQueryID())
                                                    .build();
                                            if (cspConnection.getSession().getBasicRemote() != null) {
                                                cspConnection.getSession().getBasicRemote().sendText(jsonObjectToCSP.toString());
                                            } else {
                                                query.getConnection().getSession().getBasicRemote().sendText(Utils.getMessage("info", "CSP is no more present"));
                                            }
                                            System.out.println(GCOperation.getDecryptionWithGC(result, query.getQueryID() + ""));
                                        } else {
                                            Map<String, String> tmpMap = new HashMap<>();
                                            tmpMap.put("msg", result.toString());
                                            tmpMap.put("type", "result");
//                                            System.out.println("Final output(without security) to user " + Utils.getMessage(tmpMap));
                                            query.getConnection().getSession().getBasicRemote().sendText(Utils.getMessage(tmpMap));
                                        }
                                        break;
                                }
                            }
                        }
                        break;
                }

            }

        } catch (Exception ex) {
            Logger.getLogger(smc_genome.class
                    .getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }

//        return null;
    }

    public int resultCount(Map<String, String> map) {
        int result = 0;
        for (Map.Entry<String, String> entrySet : map.entrySet()) {
            if (!"".equals(entrySet.getValue())) {
                result++;
            }
        }
        return result;
    }

    public boolean getIsSecure(JsonObject jsonObject) {
        String computationType = (Json.createReader(new StringReader(jsonObject.get("msg").toString())).readObject()).get("secure").toString();
        computationType = computationType.replace("\"", "");
        return computationType.equals("1");
    }

    public int getHospitalCount() {
        int count = 0;
        for (Map.Entry<String, Connection> entry : parties.entrySet()) {
            if (entry.getValue().isAlive() && entry.getValue().getCtype() == Connection.ConnectionParty.Hospitals) {
                count++;
            }
        }
        return count;
    }

    public QueryType getQueryType(JsonObject jsonObject) {

        QueryType queryType = null;
        String computationType = (Json.createReader(new StringReader(jsonObject.get("msg").toString())).readObject()).get("operation").toString();
        computationType = computationType.replace("\"", "");

        switch (computationType) {
            case "count":
                queryType = QueryType.Count;
                break;
            case "count_ngc":
                queryType = QueryType.CountWithoutGC;
                break;
            case "editdist_OPE":
                queryType = QueryType.EditOPE;
                break;
            case "chi":
                queryType = QueryType.Chi;
                break;
            case "maf":
                queryType = QueryType.MAF;
                break;
            case "editdist":
                queryType = QueryType.Edit;
                break;
        }
        return queryType;
    }

    public Query findQuery(int queryId) {
        Query result = null;
        for (Map.Entry<String, Query> entrySet : queryMap.entrySet()) {
            if (entrySet.getValue().getQueryID() == queryId) {
                result = entrySet.getValue();
            }
        }
        return result;
    }

    public Connection findHospital(int id) {

        for (Connection connection : parties.values()) {
            if (connection.getId() == id && connection.isAlive()) {
                return connection;
            }
        }
        return null;
    }

    @OnOpen
    public void onOpen(Session peer) {
//        peers.add(peer);

    }

    @OnClose
    public void onClose(Session peer) {
//        peers.remove(peer);
        try {
            for (String id : parties.keySet()) {
                Connection connection = parties.get(id);
                if (connection.getSession().getId().equals(peer.getId()) && parties.get(id).getCtype() == Connection.ConnectionParty.Hospitals) {
//                parties.put(connection.getId(), false);
                    connection.setAlive(false);
                    System.out.println(new Date().toString() + " Peer " + connection.getCtype() + " is " + connection.getSession().getId() + " gone ");
                }
            }
        } catch (Exception ex) {
        }
    }

    private void testCSP(Connection cspConnection) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        BigInteger em1 = new BigInteger("53555769736293622405746571569502074872892565163758191425148926906308108923424778009774010658363528108546802100251218884218361338315442916261540878137722111450425696420769044920809705892770549189001330865374918232271201395657224335938152408105760797993316636065508024038866906851566278622337149821526181783019");
        JsonObject jsonObjectToCSP = Json.createObjectBuilder()
                .add("type", "decryption")
                //                                            .add("msg", result.toString())
                //                                            .add("queryID", query.getQuery())
                .build();
        cspConnection.getSession().getBasicRemote().sendText(jsonObjectToCSP.toString());
        System.out.println(GCOperation.getDecryptionWithGC(em1, "1"));
    }

    private boolean getQueryResultsEmpty(Map<String, String> results) {
        for (Map.Entry<String, String> entrySet : results.entrySet()) {
            if (entrySet.getValue() == null) {
                return true;
            }
        }
        return false;
    }

    private void resendQueries(String packetString) throws IOException {
        if (!packetString.equals("")) {
            packetQuery.remove(packetString);
        }
        System.out.println("Size of packets " + packetQuery.size());
        for (Map.Entry<String, JsonObject> packet : packetQuery.entrySet()) {
            Connection connection = findHospital(Integer.parseInt(packet.getKey().split("-")[1]));
            if (connection != null) {
                if (connection.getSession() != null) {
                    System.out.println("Sending again to hospital " + connection.getId());
                    connection.getSession().getBasicRemote().sendText(packet.getValue().toString());
                }
            }
        }
    }

    class EditDistanceHelper {

        int queryID;
        public SortedMap<BigInteger, String> wordMap = new TreeMap<>();

        public EditDistanceHelper(int queryID) {
            this.queryID = queryID;
        }

    }
}
