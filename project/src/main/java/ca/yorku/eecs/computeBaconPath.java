package ca.yorku.eecs;



import static org.neo4j.driver.v1.Values.parameters;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.internal.InternalPath;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.driver.v1.types.Node;

public class computeBaconPath implements HttpHandler {

  private Driver neo4jDriver;
  private String actorID;
  private String baconID = "nm0000102";
  private Map getResponse;
  private byte[] result;
  private boolean baconExist;
  private boolean actorExist;

  //constructor
  public computeBaconPath(neo4j database){
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
      //get Request variables
      String body = Utils.convert(r.getRequestBody());
      JSONObject deserialized = new JSONObject(body);
      JSONObject responseJSON = new JSONObject();
      OutputStream os = r.getResponseBody();

      //If actorID is not given return 400 as BAD REQUEST
      if (!deserialized.has("actorID")){
        r.sendResponseHeaders(400, -1);
      }
      //actorID is given, then to test existence
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
        //both actors existed, try to find the baconPath
        else {
          if (!actorID.equals(baconID)) {
            //interaction with database to calculate baconPath
            get(actorID, baconID);
            try {
              //add baconNumber response
              responseJSON.put("baconNumber", getResponse.get("baconNumber"));
              //add baconPath in a list<JSONObject> form
              responseJSON.put("baconPath", createBaconPath(getResponse));
            }
            //actorID found but path not found in the database and 404 return as NO PATH FOUND
            catch (NullPointerException e) {
              r.sendResponseHeaders(404, -1);
            }
          }
          //actorID given is the same as actorID for Kevin Bacon
          else {
            responseJSON.put("baconNumber", "0");
            responseJSON.put("baconPath", "[]");
          }
          //valid actorID passed in and valid result responded by database
          if (responseJSON.length() != 0) {
            result = responseJSON.toString().getBytes();
            r.sendResponseHeaders(200, result.length);
            //write to a byte[] for OutputStream
            os.write(result);
          }
        }
      }
      os.close();
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


  private List<JSONObject> createBaconPath(Map response) throws JSONException {
    System.out.println("createBaconPath is running:");
    //change format of data from: Path ->Iterable<Node> ->List<JSONObject>
    InternalPath path = (InternalPath) response.get("baconPath");
    Iterable<Node> nodeIterable = path.nodes();
    List<JSONObject> baconPath = new ArrayList<JSONObject>();//for baconPath return
    //change format of data from: Nodes ->Two Lists
    List<String> actorsINPATH = new ArrayList<String>();
    List<String> moviesINPATH = new ArrayList<String>();
    for (Node node: nodeIterable
    ) {
      Map nodeMap = node.asMap();
      String aID = "";
      String mID = "";
      if (node.hasLabel("Actor")){
        aID = nodeMap.get("id").toString();
        actorsINPATH.add(aID);
      }
      else {
        mID = nodeMap.get("id").toString();
        moviesINPATH.add(mID);
      }
    }
    //implementing baconPath
    Integer index = 0;
    while (index < actorsINPATH.size()) {
      JSONObject pathPoint = new JSONObject();
      pathPoint.put("actorID", actorsINPATH.get(index));

      //movie list always has one item less than actor node
      if (index == moviesINPATH.size()) {
        //put the last movie again with Kevin Bacon
        pathPoint.put("movieID", moviesINPATH.get(index - 1));
      }
      //normal combination as a slice in baconPath
      else { pathPoint.put("movieID", moviesINPATH.get(index)); }
      index += 1;
      //add slice of baconPath to 'complete path' list
      baconPath.add(pathPoint);
    }
    return baconPath;
  }

  private void get( final String actorID, final String movieID)
  {
    try ( Session session = neo4jDriver.session() )
    {
      getResponse = session.writeTransaction( new TransactionWork<Map>() {
        @Override
        public Map execute(Transaction tx) {
          return getBaconPath(tx, actorID, movieID);
        }
      });
    }
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
    return !records.isEmpty();
  }

  private static Map getBaconPath(Transaction tx, String actorID, String baconID) {
    System.out.println("private method getBaconPath: is running");
    StatementResult result = tx.run("MATCH p=shortestPath((a:Actor{id:$actorID})-[*]-" +
            "(b:Actor{id:$baconID})) " +
            "RETURN length(p)/2 as baconNumber, p as baconPath",
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
}


