package se.kau.isgb33;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.bson.conversions.Bson;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;

public class Stub {

    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger(Stub.class);

      
        JFrame f = new JFrame("Filmkväll.nu");
        f.setSize(400, 500);
        f.setLayout(null);

     
        JTextArea area = new JTextArea();
        area.setLineWrap(true);
        area.setBounds(10, 10, 365, 400);

    
        JTextField t = new JTextField("");
        t.setBounds(10, 415, 260, 40);

    
        JButton b = new JButton("Sök");
        b.setBounds(275, 415, 100, 40);


        b.addActionListener(e -> {
            logger.info("Knapp Tryckt!");
            String connString;
            boolean documentsFound = false;
            
            try (InputStream input = new FileInputStream("connection.properties")) {
                Properties prop = new Properties();
                prop.load(input);
                connString = prop.getProperty("db.connection_string");
                logger.info(connString);

                ConnectionString connectionString = new ConnectionString(connString);
                MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString)
                        .build();
                MongoClient mongoClient = MongoClients.create(settings);
                MongoDatabase database = mongoClient.getDatabase(prop.getProperty("db.name"));
                MongoCollection<Document> collection = database.getCollection("movies");

              
               String searchMovie = t.getText();

            
                Bson filter = Filters.in("genres", searchMovie);

                Bson projection = Projections.fields(
                    
                    Projections.include("title", "year")
                );

                AggregateIterable<Document> myDocs = collection.aggregate(Arrays.asList(
                	Aggregates.match(filter),
                	Aggregates.project(projection),
                    Aggregates.limit(10),
                    Aggregates.sort(Sorts.descending("title"))
                ));


                MongoCursor<Document> iterator = myDocs.iterator();

             
                area.setText("");
                
                System.out.print(iterator.hasNext());
                
                while (iterator.hasNext()) {
                    Document myDoc = iterator.next();
                    area.append(myDoc.getString("title") + ", ");
                    area.append(myDoc.getInteger("year") + "\n");
                    documentsFound = true;
                
                }
                
                if (!documentsFound) {
                    area.setText("Ingen film matchade kategorin");
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });


        f.add(area);
        f.add(t);
        f.add(b);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }
}
