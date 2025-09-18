package ca.yorku.eecs;

import static org.neo4j.driver.v1.Values.parameters;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
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

public class addMovie implements HttpHandler {

  private Driver neo4jDriver;
  private String name;
  private String ID;
  private Map response;

  //constructor
  public addMovie(neo4j database) {
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

  private void handlePut(HttpExchange r) throws IOException, JSONException {
	  try {
	    String body = Utils.convert(r.getRequestBody());
	    JSONObject deserialized = new JSONObject(body);

      //If either movieID or name is not given return 400 as BAD REQUEST
	    if (!deserialized.has("name") || !deserialized.has("movieID")) {
	    	r.sendResponseHeaders(400, -1);
	    }
	    else {
		    name = deserialized.getString("name");
		    ID = deserialized.getString("movieID");
		    //interaction with database
		    add(name, ID);
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

  private void add(String name, String ID){
    try (Session session = neo4jDriver.session())
    {
      response = session.writeTransaction( new TransactionWork<Map>() {
        @Override
        public Map execute(Transaction tx) {
          return createMovie(tx, name, ID);
        }
      });
    }
  }

  private Map createMovie(Transaction tx, String  name, String ID){
    //if the same movie is added twice, only one node should be created
    StatementResult result = tx.run("MERGE (m:Movie {id:$movieID}) " +
        "SET m.name = $name " +
        "RETURN m.name, m.id",
        parameters("name", name , "movieID", ID));
    //Get values from: StatementResult ->Record ->Map
    List<Record> records = result.list();
    Record record = records.get(0);
    Map recordMap = record.asMap();
    return recordMap;
  }
}