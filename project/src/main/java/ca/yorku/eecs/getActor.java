package ca.yorku.eecs;



import static org.neo4j.driver.v1.Values.parameters;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;

public class getActor implements HttpHandler {
  private Driver neo4jDriver;
  private String ID;
  private Map getResponse;

  //constructor
  public getActor(neo4j database) {
    neo4jDriver = database.getDriver();
  }

  @Override
  public void handle(HttpExchange r) throws IOException {
    try {
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

  private void get( final String ID )
  {
    try ( Session session = neo4jDriver.session() )
    {
      getResponse = session.writeTransaction( new TransactionWork<Map>() {
        @Override
        public Map execute(Transaction tx) {
          try {
            return getActorData(tx, ID);
          } catch (JSONException e) {
            e.printStackTrace();
          }
          return null;
        }
      });
    }
  }

  private static Map getActorData(Transaction tx, String ID) throws JSONException {
    System.out.println(ID);
    StatementResult result = tx.run("MATCH (a:Actor{id:$actorID})-" +
            "[ACTED_IN]->(m:Movie) " +
            "RETURN a.id as actorID, a.name as name, collect(m.id) as movies",
        parameters("actorID", ID));
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


  private void handleGet(HttpExchange r) throws IOException{
	  try {
	    String body = Utils.convert(r.getRequestBody());
	    JSONObject deserialized = new JSONObject(body);
	
	    //See body and deserilized
	    System.out.println("addActor-HandelGet get input:");
	    System.out.println(deserialized);
      //If actorID is not given return 400 as BAD REQUEST
	    if (!deserialized.has("actorID")) {
	    	r.sendResponseHeaders(400, -1);
	    }
	    else {
	    	ID = deserialized.getString("actorID");
		    //Interaction with database + assign values to JSONObjects already
		    get(ID);
        JSONObject responseJSON = new JSONObject(getResponse);
        byte[] result = responseJSON.toString().getBytes();
        OutputStream os = r.getResponseBody();
        //valid actorID passed in and valid result responded by database
        if (responseJSON.length() != 0) {
          result = responseJSON.toString().getBytes();
          r.sendResponseHeaders(200, result.length);
          os.write(result);
        }
        //actorID not found in the database and 404 return as NO DATA FOUND
        else{
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

}
