package ca.yorku.eecs;


import static org.neo4j.driver.v1.Values.parameters;

import java.io.IOException;
import java.io.OutputStream;

import java.util.List;
import java.util.Map;
import org.json.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;

public class addActor implements HttpHandler {
  private Driver neo4jDriver;
  private String name;
  private String ID;
  private Map addResponse;

  //constructor
  public addActor(neo4j database){
    neo4jDriver = database.getDriver();
  }

  public void handle(HttpExchange r) {
    try {
      if (r.getRequestMethod().equals("PUT")) {
        handlePut(r);
      }
      //Undefined HTTP methods used on valid endPoint
      else{
        r.sendResponseHeaders(500, -1);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  private void add(String name, String ID )
  {
    try ( Session session = neo4jDriver.session() )
    {
      addResponse = session.writeTransaction( new TransactionWork<Map>() {
        @Override
        public Map execute(Transaction tx) {
          return createActor(tx, name, ID);
        }
      });
    }
  }

  private static Map createActor(Transaction tx, String name, String ID){
    StatementResult result = tx.run( "MERGE (a:Actor{id:$actorID}) " +
            "SET a.name = $name " +
            "RETURN a.name, a.id",
        parameters("name", name , "actorID", ID));
    //Get values from neo4j StatementResult object
    List<Record> records = result.list();
    Record record = records.get(0);
    Map recordMap = record.asMap();
    return recordMap;
  }

  private void handlePut(HttpExchange r) throws IOException {
	  try {
	  	String body = Utils.convert(r.getRequestBody());
	  	JSONObject deserialized = new JSONObject(body);

	    //If either name or actorID is not given return 400 as BAD REQUEST
	    if (!deserialized.has("name") || !deserialized.has("actorID")) {
	    	r.sendResponseHeaders(400, -1);
	    }
	    //Interacted with database and add actor, then return 200 as OK
	    else {
	    	name = deserialized.getString("name");
	    	ID = deserialized.getString("actorID");
	    	//interaction with database
		    add(name, ID);
		    //result for server-client interaction
		    JSONObject responseJSON = new JSONObject();
		    responseJSON.put("name", addResponse.get("a.name"));
		    responseJSON.put("actorID", addResponse.get("a.id"));
		    byte[] result = responseJSON.toString().getBytes();
		    r.sendResponseHeaders(200, 0);
		    OutputStream os = r.getResponseBody();
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
