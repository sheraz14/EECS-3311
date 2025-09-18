package ca.yorku.eecs;

import static org.neo4j.driver.v1.Values.parameters;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;

public class computeBaconNumber implements HttpHandler {

  private Driver neo4jDriver;
  private String actorID;
  private String baconID = "nm0000102";
  private Map getResponse;
  private byte[] result;
  private boolean baconExist;
  private boolean actorExist;

  //constructor
  public computeBaconNumber(neo4j database){
    neo4jDriver = database.getDriver();
  }

  public void handle(HttpExchange r) {
    try{
      if (r.getRequestMethod().equals("GET")) {
        handleGet(r);
      }
      //Undefined HTTP methods used on valid endPoint
      else{
        r.sendResponseHeaders(500, -1);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleGet(HttpExchange r) throws IOException, JSONException {
    try {
      String body = Utils.convert(r.getRequestBody());
      JSONObject deserialized = new JSONObject(body);
      OutputStream os = r.getResponseBody();
      JSONObject responseJSON;

      System.out.println("getRelationship handler get:");
      System.out.println(deserialized);
      //If actorID is not given return 400 as BAD REQUEST
      if (!deserialized.has("actorID")){
        r.sendResponseHeaders(400, -1);
      }
      //actorID is given, then test for existence
      else {
        actorID = deserialized.getString("actorID");
        //Test if Kevin Bacon is in the database
        baconExist = test(baconID);
        //Test if actorID input is in the database
        actorExist = test(actorID);
        //neither Kevin Bacon or actor is not existed in the database
        //responded 400 as NO ACTOR EXIST
        if ((!baconExist) || (!actorExist)){
          r.sendResponseHeaders(400,-1);
        }
        //normal case to compute a baconNumber&baconPath
        else {
          if (!actorID.equals(baconID)) {
            //interaction with database
            get(actorID, baconID);
            //result for server-client interaction
            responseJSON = new JSONObject(getResponse);
          }
          //actorID given is the same as actorID for Kevin Bacon
          else {
            responseJSON = new JSONObject(getResponse);
            responseJSON.put("baconNumber", "0");
          }
          //valid actorID passed in and valid result responded by database
          if (responseJSON.length() != 0) {
            result = responseJSON.toString().getBytes();
            r.sendResponseHeaders(200, result.length);
            //write to a byte[] for OutputStream
            os.write(result);
          }
          //both actors found but no path in the database and 404 return as NO PATH FOUND
          else {
            r.sendResponseHeaders(404, -1);
          }
        }
        os.close();
      }
    }
    //if deserilized failed, (ex: JSONObeject Null Value)
    catch(JSONException e) {
      r.sendResponseHeaders(400, -1);
    }
    //if server connection / database connection failed
    catch(Exception e) {
      r.sendResponseHeaders(500, -1);
    }
  }

  private void get( final String actorID, final String movieID)
  {
    try ( Session session = neo4jDriver.session() )
    {
      getResponse = session.writeTransaction( new TransactionWork<Map>() {
        @Override
        public Map execute(Transaction tx) {
          return getRelationshipData(tx, actorID, movieID);
        }
      });
    }
  }

  private static Map getRelationshipData(Transaction tx, String actorID, String baconID) {
    StatementResult result = tx.run("MATCH p=shortestPath((a:Actor{id:$actorID})-[*]-" +
            "(b:Actor{id:$baconID})) " +
            "RETURN length(p)/2 as baconNumber",
        parameters("actorID", actorID, "baconID", baconID));
    //Get values from neo4j StatementResult object
    List<Record> records = result.list();
    Map recordMap = new HashMap();
    //valid data responded from database
    if (!records.isEmpty()){
      Record record = records.get(0);
      recordMap = record.asMap();
    }
    return recordMap;
  }

  private boolean test(final String actorID)
  {
    try ( Session session = neo4jDriver.session() )
    {
      boolean exist = session.writeTransaction( new TransactionWork<Boolean>() {
        @Override
        public Boolean execute(Transaction tx) {
          return testActor(tx, actorID);
        }
      });
      return exist;
    }
  }

  private boolean testActor(Transaction tx, String actorID){
    StatementResult result = tx.run("MATCH (a:Actor{id:$actorID}) " +
            "RETURN a.id as actorID",
        parameters("actorID", actorID));
    //Get values from neo4j StatementResult object
    List<Record> records = result.list();

    System.out.println(records.isEmpty());

    return !records.isEmpty();
  }
}
