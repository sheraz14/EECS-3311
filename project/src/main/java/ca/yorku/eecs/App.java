package ca.yorku.eecs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class App
{
    static int PORT = 8080;

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

        String url = "bolt://localhost:7687";
        String user = "neo4j";
        String password = "123456";
        neo4j database = new neo4j(url, user, password);

        server.createContext("/api/v1/addActor", new addActor(database));
        server.createContext("/api/v1/addMovie", new addMovie(database));
        server.createContext("/api/v1/addRelationship", new addRelationship(database));
        server.createContext("/api/v1/getActor", new getActor(database));
        server.createContext("/api/v1/getMovie", new getMovie(database));
        server.createContext("/api/v1/hasRelationship", new hasRelationship(database));
        server.createContext("/api/v1/computeBaconPath", new computeBaconPath(database));
        server.createContext("/api/v1/computeBaconNumber", new computeBaconNumber(database));

        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
}