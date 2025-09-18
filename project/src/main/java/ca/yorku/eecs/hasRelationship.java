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

public class hasRelationship implements HttpHandler {

  private Driver neo4jDriver;
  private String actorID;
  private String movieID;
  private Map getResponse;
  private byte[] result;

  //constructor
  public hasRelationship(neo4j database){
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
	    //If either movieID or actorID is not given return 400 as BAD REQUEST
	    if (!deserialized.has("actorID") || !deserialized.has("movieID")) {
	    	r.sendResponseHeaders(400, -1);
	    }
	    else {
		    actorID = deserialized.getString("actorID");
		    movieID = deserialized.getString("movieID");
		    //interaction with database
		    get(actorID, movieID);
		    //result for server-client interaction
		    JSONObject responseJSON = new JSONObject();
		    responseJSON.put("actorID", getResponse.get("a.id"));
        responseJSON.put("movieID", getResponse.get("m.id"));
        responseJSON.put("hasRelationship", getResponse.get("b"));
        OutputStream os = r.getResponseBody();
        //valid actorID passed in and valid result responded by database
        if (responseJSON.length() != 0) {
          result = responseJSON.toString().getBytes();
          r.sendResponseHeaders(200, result.length);
          //write to a byte[] for OutputStream
          os.write(result);
        }
        //actorID not found in the database and 404 return as NO DATA FOUND
        else {
          r.sendResponseHeaders(404, -1);
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

  private static Map getRelationshipData(Transaction tx, String actorID, String movieID) {
    StatementResult result = tx.run("match (a:Actor{id:$actorID}), " +
            "(m:Movie{id:$movieID})" +
            "RETURN a.id, m.id, exists((a)-[:ACTED_IN]->(m)) as b",
        parameters("actorID", actorID, "movieID", movieID));
    //Get values from neo4j StatementResult object
    List<Record> records = result.list();
    Map recordMap = new HashMap();
    //valid data responded from database
    if (!records.isEmpty()){
      Record record = records.get(0);
      recordMap = record.asMap();
    }
    return recordMap;
  }}