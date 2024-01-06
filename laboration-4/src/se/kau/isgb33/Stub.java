package se.kau.isgb33;

import static spark.Spark.*;
import static spark.Spark.get;
import static spark.Spark.post;

import java.io.FileInputStream;

import java.io.IOException;
import java.io.InputStream;

import java.util.Arrays;

import java.util.Properties;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.text.WordUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;

public class Stub {

	public static void main(String[] args) {
		port(4567);
        Logger logger = LoggerFactory.getLogger(Stub.class);

        try (InputStream input = new FileInputStream("connection.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            String connString = prop.getProperty("db.connection_string");
            logger.info(connString);

            ConnectionString connectionString = new ConnectionString(connString);
            MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString)
                    .build();

            MongoClient mongoClient = MongoClients.create(settings);
            MongoDatabase database = mongoClient.getDatabase(prop.getProperty("db.name"));
            logger.info(prop.getProperty("db.name"));

            get("/title/:title", (req, res) -> {
                String title = req.params(":title");
                MongoCollection<Document> collection = database.getCollection("movies");
                String filter = title.toLowerCase();
                logger.info("Filtering Movies for title: " + filter);
                Document myDoc = collection.find(Filters.regex("title", title, "i")).first();

                if (myDoc != null) {
                    myDoc.remove("_id");
                    myDoc.remove("poster");
                    myDoc.remove("cast");
                    myDoc.remove("fullplot");
                    
                    res.status(200);
                    res.type("application/json");
                    return myDoc.toJson();
                } else {
                	res.status(404);
                	 res.type("application/json");
                     return "{\"message\": \"No movies found.\"}";
                }
               
            });
            
            get("/fullplot/:title", (req, res) -> {
                String title = req.params(":title");
                MongoCollection<Document> collection = database.getCollection("movies");
                String filter = title.toLowerCase();
                logger.info("Filtering Movies for title: " + filter);
                Document myDoc = collection.find(Filters.regex("title", title, "i")).projection(
                        Projections.fields(
                                Projections.include("fullplot", "title"),
                                Projections.excludeId()
                        )
                ).first();

                if (myDoc != null) {
                    res.status(200);
                    res.type("application/json");
                    return myDoc.toJson();
                } else {
                    res.status(404);
                    res.type("application/json");
                    return "{\"message\": \"No movies found.\"}";
                }
            });
            
            get("/cast/:title", (req, res) -> {
                String title = req.params(":title");
                MongoCollection<Document> collection = database.getCollection("movies");
                String filter = title.toLowerCase();
                logger.info("Filtering Movies for title: " + filter);
                Document myDoc = collection.find(Filters.regex("title", title, "i")).projection(
                        Projections.fields(
                                Projections.include("cast", "title"),
                                Projections.excludeId()
                        )
                ).first();

                if (myDoc == null) {
                    
                    res.status(404);
                    res.type("application/json");
                    return "{\"message\": \"Movie an cast not found.\"}";
                } else {
                	res.status(200);
                    res.type("application/json");
                    return myDoc.toJson();
                }
            });
            
            post("/title", (req, res) -> {
            	
	    	    try {
	    	    	MongoCollection<Document> collection = database.getCollection("movies");
	               collection.insertOne(new Document(Document.parse(req.body())));
	 		      logger.info("Adding Movie ");

	                       
	            } catch (MongoException e) {
	                System.err.println("Unable to insert due to an error: " + e);
	            }
	    	    
	    	    res.status(202);
	    	    
	    		  return ("<html><body><h1>Movie added. </h1></body></html>");
	    	});
            
            
            get("/genre/:genre", (req, res) -> {
                MongoCollection<Document> collection = database.getCollection("movies");
                String filter = req.params(":genre").toLowerCase();
                filter = WordUtils.capitalizeFully(filter);
                logger.info("Finding movie genres: " + filter);

                Bson genre_Filter = Filters.in("genres", filter);
                AggregateIterable<Document> myDocs = collection.aggregate(Arrays.asList(
                        Aggregates.match(genre_Filter),
                        Aggregates.project(Projections.fields(
                                Projections.excludeId(),
                                Projections.exclude("id", "poster", "cast", "fullplot")
                        )),
                        Aggregates.limit(10)
                ));

                JsonArray moviesArray = new JsonArray();

                for (Document document : myDocs) {
                    JsonObject movie = new JsonObject();
                    movie.add("movieData", new Gson().fromJson(document.toJson(), JsonObject.class));
                    moviesArray.add(movie);
                }

                if (!moviesArray.isEmpty()) {
                   
                    JsonObject response = new JsonObject();
                    response.add("movies", moviesArray);

                    res.status(200);
                    res.type("application/json");
                    return response;
                } else {
                  
                	 res.status(404);
                     res.type("application/json");
                     return "{\"message\": \"No movies found for the provided genre.\"}";
                     
                }
            });

  	
            
            get("/actor/:actor", (req, res) -> {
                MongoCollection<Document> collection = database.getCollection("movies");
                String filter = req.params(":actor").toLowerCase();
                filter = WordUtils.capitalizeFully(filter);
                logger.info("Finding movie by actor: " + filter);

                Bson actor_Filter = Filters.in("cast", filter);
                AggregateIterable<Document> myDocs = collection.aggregate(Arrays.asList(
                        Aggregates.match(actor_Filter),
                        Aggregates.project(Projections.fields(
                                Projections.excludeId(),
                                Projections.include("title")
                        )),
                        Aggregates.limit(10)
                ));

                JsonArray moviesArray = new JsonArray();

                for (Document document : myDocs) {
                    JsonObject movie = new JsonObject();
                    movie.add("movieData", new Gson().fromJson(document.toJson(), JsonObject.class));
                    moviesArray.add(movie);
                }

                if (moviesArray.isEmpty()) {
                    res.status(404);
                    res.type("application/json");
                    return "{\"message\": \"No movies found for the actor.\"}";
                } else {
                    JsonObject response = new JsonObject();
                    response.add("movies", moviesArray);

                    res.status(200);
                    res.type("application/json");
                    return response;
                }
            });





        } catch (IOException e) {
            e.printStackTrace();
        }
	}}



