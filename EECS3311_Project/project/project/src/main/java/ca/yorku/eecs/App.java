package ca.yorku.eecs;

import com.sun.net.httpserver.HttpServer;
import org.neo4j.driver.v1.*;
import java.io.IOException;
import java.net.InetSocketAddress;

public class App
{
    static int PORT = 8080;
    private static Driver driver;
    public static void main(String[] args) throws IOException
    {
        String uri = "bolt://localhost:7687";
        String username = "neo4j";
        String password = "12345678";
        Config config = Config.builder().withoutEncryption().build();
        driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password), config);

        try (Session session = driver.session()) {
            StatementResult result = session.run("RETURN 'Connection to Neo4j successful!' AS greeting");
            String greeting = result.single().get("greeting").asString();
            System.out.println(greeting);
        } catch (Exception e) {
            System.err.println("Neo4j connection error: " + e.getMessage());
            return;
        }

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
}
