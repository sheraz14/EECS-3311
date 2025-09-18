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

public class getMovie implements HttpHandler {

  private Driver neo4jDriver;
  private String ID;
  private Map getResponse;

  //constructor
  public getMovie(neo4j database) {
    neo4jDriver = database.getDriver();
  }

  public void handle(HttpExchange r) {
    try {
      if (r.getRequestMethod().equals("GET")) {
        handleGet(r);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleGet(HttpExchange r) throws IOException, JSONException {
	  try {
      String body = Utils.convert(r.getRequestBody());
      JSONObject deserialized = new JSONObject(body);

      System.out.println("addMovie handler get:");
      System.out.println(deserialized);
      //If movieID is not given return 400 as BAD REQUEST
      if (!deserialized.has("movieID")) {
        r.sendResponseHeaders(400, -1);
      } else {
        ID = deserialized.getString("movieID");
        //interaction with database
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

  private void get(String ID){
    try (Session session = neo4jDriver.session())
    {
      getResponse = session.writeTransaction( new TransactionWork<Map>() {
        @Override
        public Map execute(Transaction tx) {
          return getMovieData(tx, ID);
        }
      });
    }
  }

  private Map getMovieData(Transaction tx, String ID){
    StatementResult result = tx.run("MATCH (m:Movie{id:$movieID})<-" +
            "[ACTED_IN]-(a:Actor) " +
            "RETURN m.id as movieID, m.name as name, collect(a.id) as actors",
        parameters("movieID", ID));
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
}

