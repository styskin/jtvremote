import java.sql.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;

import java.net.URI;
import java.net.URISyntaxException;

import static spark.Spark.*;
import spark.template.freemarker.FreeMarkerEngine;
import spark.ModelAndView;

import com.heroku.sdk.jdbc.DatabaseUrl;
import com.google.gson.Gson;
import net.glxn.qrgen.javase.QRCode;
import net.glxn.qrgen.core.image.ImageType;

public class Main {
	
	private static boolean initDb() {
		Connection connection = null;
	    try {
	    	connection = DatabaseUrl.extract().getConnection();
	        Statement stmt = connection.createStatement();
	        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS items(id VARCHAR(16000) not null, text VARCHAR(16000) not null, PRIMARY KEY(id))");
	    } catch (Exception e) {
	    	return false;
	    } finally {
	    	if (connection != null) try{connection.close();} catch(SQLException e){}
	    }
	    return true;
	}
	
	private static boolean setDb(String key, String data) {
		Connection connection = null;
	    try {
	    	connection = DatabaseUrl.extract().getConnection();
			PreparedStatement ust = connection.prepareStatement("UPDATE items SET text = ? WHERE id = ?");
			ust.setString(1, data);
			ust.setString(2, key);
			int u = ust.executeUpdate();
			if (u == 0) {
				PreparedStatement ust2 = connection.prepareStatement("INSERT INTO items(text, id) VALUES (?, ?)");
				ust2.setString(1, data);
				ust2.setString(2, key);
				u = ust2.executeUpdate();
			}
			return u > 0;
	    } catch (Exception e) {
	    	return false;
	    } finally {
	    	if (connection != null) try{connection.close();} catch(SQLException e){}
	    }
	}
	
	public static void main(String[] args) {
	    port(Integer.valueOf(System.getenv("PORT")));
	    staticFileLocation("/public");
	    initDb();
	
	    Gson gson = new Gson();    
	    post("/register", "application/json", (req, res) -> {
	    	res.type("application/json");
	    	String remoteId = "";
	    	if (req.cookie("device") != null){
	    		remoteId = req.cookie("device");	    		
	    	}
            if (req.queryParams("device") != null) {
                remoteId = req.queryParams("device");
            }
            String key = "tv-" + req.queryParams("tv");
	        if (setDb(key, remoteId)) {	
	        	return "{done: 'ok'}";
	        } else {
	        	return "{done: 'error'}";
	        }
	    });
	    
	    post("/tv", "application/json", (req, res) -> {
	    	res.type("application/json");
	    	String remoteId = "";
	    	if (req.cookie("device") != null){
	    		remoteId = "remote-" + req.cookie("device");	    		
	    	}
            if (req.queryParams("device") != null) {
            	remoteId = "remote-" + req.queryParams("device");
            }
	    	if (setDb(remoteId, req.body())) {
	    		return req.body();
	    	} else {
	    		return "";
	    	}
	    });
	        
	    get("/", (req, res) -> {
	    	System.out.println(req.cookie("tvid"));
	    	
	        if (req.cookie("tvid") != null) {
	        	System.out.println("TV");
	            res.redirect("/main.html");
	        } else {
	        	System.out.println("REDIRECT");
	            res.redirect("/link");
	        }
	        return "";
	    });
	    
	    
	    get("/link", (req, res) -> {
	    	// TODO: get persistant id and tmp code
	        String id = String.valueOf((new java.util.Date()).getTime()) + String.valueOf(Math.floor(Math.random() * 1000) + 1.);
	        if (req.cookie("tvid") != null) {
	        	id = req.cookie("tvid");
	        } else {
	        	res.cookie("tvid", id);
	        }
			Connection connection = null;
		    try {
		    	connection = DatabaseUrl.extract().getConnection();
				PreparedStatement ust = connection.prepareStatement("DELETE FROM items WHERE id = ?");
				ust.setString(1, "tv-" + id);
				ust.executeUpdate();
		    } catch (Exception e) {
		    } finally {
		    	if (connection != null) try{connection.close();} catch(SQLException e){}
		    }
			System.out.println(id);        
			Map<String, Object> attributes = new HashMap<>();
			String png = Base64.getEncoder().encodeToString(QRCode.from(id).to(ImageType.PNG).stream().toByteArray());
			attributes.put("data", "data:image/png;base64, " + png);
			attributes.put("tvid", id);
			return new ModelAndView(attributes, "link.ftl");
	    }, new FreeMarkerEngine());

	    get("/gettv", (req, res) -> {
	    	res.type("application/json");
	        Connection connection = null;
		    try {
		    	String key;
		        if (req.cookie("tvid") != null) {
		        	key = "tv-" + req.cookie("tvid");
		        } else {
		        	throw new Exception("No key");
		        }
		    	connection = DatabaseUrl.extract().getConnection();
		    	PreparedStatement statement = connection.prepareStatement("SELECT id,text FROM items WHERE id = ?");
		    	statement.setString(1, key);
		    	ResultSet rs = statement.executeQuery();
		    	if (rs.next()) {
		    		String value = rs.getString(2);
		    		String remoteId = "remote-" + value;
			    	PreparedStatement statement2 = connection.prepareStatement("SELECT id,text FROM items WHERE id = ?");
		    		statement2.setString(1, remoteId);
		    		ResultSet rs2 = statement2.executeQuery();
		    		if (rs2.next()) {
		    			String data = rs2.getString(2);
		    			TVMessage message = gson.fromJson(data, TVMessage.class);
		    			TVMessage cm = message.clone();
		    			cm.setForce(0);
		    			PreparedStatement ust = connection.prepareStatement("UPDATE items SET text = ? WHERE id = ?");
		    			ust.setString(1, gson.toJson(cm));
		    			ust.setString(2, remoteId);
		    			ust.executeUpdate();
                                        return message;
		    		}
		    	}
		    	return new TVMessage();
		    } catch (Exception e) {
		    	e.printStackTrace(System.out);
		    	return new TVMessage();
		    } finally {
		    	if (connection != null) try{connection.close();} catch(SQLException e){}
		    }
	    }, gson::toJson);
	}

}
