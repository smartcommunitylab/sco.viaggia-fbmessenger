package in.strollup.fb.contract;

import com.mongodb.MongoClient;

import eu.trentorise.smartcampus.mobilityservice.model.TaxiContact;
import eu.trentorise.smartcampus.mobilityservice.model.TimeTable;
import it.sayservice.platform.smartplanner.data.message.otpbeans.Parking;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import in.strollup.fb.contract.ChatContext;

/**
 * 
 * @author dlico
 *
 */
public class MongoDBJDBC {
	 private MongoClient mongoClient;
	 private DB db;
	 private DBCollection coll = null;
   public MongoDBJDBC() {
	   
	   
      try{   
		
         // To connect to mongodb server
         mongoClient = new MongoClient("localhost", 27017 );
			
         // Now connect to your databases
         db = mongoClient.getDB("test");
         coll = db.createCollection("chatContext", null);
         System.out.println("Connect to database successfully");
			
      }catch(Exception e){
         System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      } 
   }
   
   public ChatContext createContext(ChatContext context) {
	   DBObject obj = convertToDBObject(context);
	   
       coll.save(obj);
       return getUserContext(context.getUserId());
   }

private DBObject convertToDBObject(ChatContext context) {
	DBObject obj = new BasicDBObject();
	   obj.put("senderId", context.getUserId());
	   obj.put("context", context.getContext());
	   obj.put("direzione", context.getDirezione());
	   obj.put("busID", context.getbusID());
	   obj.put("trainID", context.getTrainID());
	   obj.put("funiviaID", context.getFuniviaID());
	   obj.put("text", context.getText());
	   obj.put("index", context.getIndex());
	   obj.put("taxi", setListTCToDBObject(context.getTaxi()));
	   obj.put("autobus", setTimeTableToDBObject(context.getAutobus()));
	   obj.put("treni", setTimeTableToDBObject(context.getTreni()));
	   obj.put("parcheggi", setListPDBObject(context.getParcheggi()));
	   obj.put("parcheggiBici", setListPDBObject(context.getParcheggiBici()));
	   obj.put("fascia", context.getFascia());
	return obj;
}

   public ChatContext replace(ChatContext context){
	   DBObject q = new BasicDBObject();
	   q.put("senderId", context.getUserId());
	   coll.update(q, convertToDBObject(context));
	   return getUserContext(context.getUserId());
   }
   
   public ChatContext getUserContext(String senderId) {
	   DBObject  queryobj = new BasicDBObject();
	   queryobj.put("senderId", senderId);
	   
	   ChatContext chatcontexts = new ChatContext();
	   
	   if(coll.findOne() != null){
		   DBObject contextDBObject = coll.findOne();
		   chatcontexts.setUserId((String) contextDBObject.get("senderId"));
		   chatcontexts.setContext((String) contextDBObject.get("context"));
		   chatcontexts.setDirezione((boolean) contextDBObject.get("direzione"));
		   chatcontexts.setBusID((String) contextDBObject.get("busID"));
		   chatcontexts.setTrainID((String) contextDBObject.get("trainID"));
		   chatcontexts.setFuniviaID((String) contextDBObject.get("funiviaID"));
		   chatcontexts.setText((String) contextDBObject.get("text"));
		   chatcontexts.setIndex((int) contextDBObject.get("index"));
		   chatcontexts.setTaxi(setDBObjectToListTCT(contextDBObject.get("taxi")));
		   chatcontexts.setAutobus(setDBObjectToTimetable(contextDBObject.get("autobus")));
		   chatcontexts.setTreni(setDBObjectToTimetable(contextDBObject.get("treni")));
		   chatcontexts.setParcheggi(setDBObjectToListP(contextDBObject.get("parcheggi")));
		   chatcontexts.setParcheggiBici(setDBObjectToListP(contextDBObject.get("parcheggiBici")));
		   chatcontexts.setFascia((String) contextDBObject.get("fascia"));
		   }
	   else
		   chatcontexts = null;
	   return chatcontexts;
   }
   
   private DBObject setTimeTableToDBObject(TimeTable timeTable){
	   ObjectMapper mapper = new ObjectMapper();
	   DBObject  DBOBJTimeTable = mapper.convertValue(timeTable, BasicDBObject.class);
	   
	   return DBOBJTimeTable;
   }
   
   private List<DBObject> setListTCToDBObject(List<TaxiContact> taxi){
	   ObjectMapper mapper = new ObjectMapper();
	   List<DBObject> result = Lists.newArrayList();
	   for(TaxiContact t : taxi){
		   DBObject DBOBJTaxiContact = mapper.convertValue(t, BasicDBObject.class);
		   result.add(DBOBJTaxiContact);
	   }
	   
	   return result;
   }
   
   private List<DBObject> setListPDBObject(List<Parking> parcheggi){
	   ObjectMapper mapper = new ObjectMapper();
	   List<DBObject> result = Lists.newArrayList();
	   for(Parking p : parcheggi){
		   DBObject DBOBJTaxiContact = mapper.convertValue(p, BasicDBObject.class);
		   result.add(DBOBJTaxiContact);
	   }
	   
	   return result;
   }
   
   private TimeTable setDBObjectToTimetable(java.lang.Object object){
	   ObjectMapper mapper = new ObjectMapper();
	   TimeTable  DBOBJTimeTable = mapper.convertValue(object, TimeTable.class);
	   
	   return DBOBJTimeTable;
   }
   
   private List<TaxiContact> setDBObjectToListTCT(java.lang.Object object){
	   ObjectMapper mapper = new ObjectMapper();
	   List<TaxiContact>  DBOBJTaxiContact = new ArrayList<>();
		
	   for(int i = 0; i < DBOBJTaxiContact.size(); i++)
		   DBOBJTaxiContact.add(mapper.convertValue(object, TaxiContact.class));
	   
	   return DBOBJTaxiContact;
   }
   
   private List<Parking> setDBObjectToListP(java.lang.Object object){
	   ObjectMapper mapper = new ObjectMapper();
	   List<Parking>  DBOBJTaxiContact = new ArrayList<>();
		
	   for(int i = 0; i < DBOBJTaxiContact.size(); i++)
		   DBOBJTaxiContact.add(mapper.convertValue(object, Parking.class));
	   
	   return DBOBJTaxiContact;
   }
}