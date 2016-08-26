package in.strollup.fb.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy;

import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;

import eu.trentorise.smartcampus.mobilityservice.MobilityDataService;
import eu.trentorise.smartcampus.mobilityservice.MobilityServiceException;
import eu.trentorise.smartcampus.mobilityservice.model.TaxiContact;
import eu.trentorise.smartcampus.mobilityservice.model.TimeTable;
import it.sayservice.platform.smartplanner.data.message.otpbeans.Parking;
import it.sayservice.platform.smartplanner.data.message.otpbeans.Route;


import in.strollup.fb.contract.Attachment;
import in.strollup.fb.contract.Button;
import in.strollup.fb.contract.Database;
import in.strollup.fb.contract.Element;
import in.strollup.fb.contract.Message;
import in.strollup.fb.contract.Messaging;
import in.strollup.fb.contract.Payload;
import in.strollup.fb.contract.QuickReply;
import in.strollup.fb.contract.Recipient;
import in.strollup.fb.profile.FbProfile;
import in.strollup.fb.servlet.WebHookServlet;

// web_url postback

/**
 * A Utility class which replies to fb messgaes (or postbacks).<br/>
 * If you already have a service which takes string as input and gives some
 * output, you can easily embed that service in this utility to make your own AI
 * bot.<br/>
 * 
 * A Service can be a search engine for music or a help desk of a company.
 * 
 * @author siddharth
 *
 */
public class FbChatHelper {
	private static List<TitleSubTitle> option;
	private static List<Message> messaggiQuick;
	private static List<String> busMonodirezionali;
	private static List<String> menuVoice;
	private static List<TaxiContact> tmpTaxi;
	private static List<Parking> tmpParcheggi;
	private static List<Parking> tmpParcheggibici;
	
	private static List<TitleSubTitle> optiontaxi,optionparcheggi,optionparcheggibici;
	private static List<Message> messaggiQuickTreni;
	
	private static class ChatContext {
		private String context = "start";
		private boolean direzione = true;
		private String busID;
		private String trainID;
		private String FuniviaID;
		private String text;
		private int index = 0;
		private TimeTable autobus = new TimeTable();
		private List<TaxiContact> taxi = new ArrayList<TaxiContact>();
		private TimeTable treni = new TimeTable();
		private List<Parking> parcheggi = new ArrayList<Parking>();
		private List<Parking> parcheggibici = new ArrayList<Parking>();
		private String fascia;
	}

	private static Map<String, ChatContext> chatContexts = new HashMap<String, FbChatHelper.ChatContext>();

	private static String profileLink = "https://graph.facebook.com/v2.6/SENDER_ID?access_token=" + WebHookServlet.PAGE_TOKEN;
	
	public FbChatHelper() {
		option = new ArrayList<>();
		messaggiQuick = new ArrayList<>();
		busMonodirezionali = new ArrayList<>();
		menuVoice = new ArrayList<>();
		
		optiontaxi = new ArrayList<>();
		optionparcheggi=new ArrayList<>();
		optionparcheggibici=new ArrayList<>();
		messaggiQuickTreni = new ArrayList<>();
		
		busMonodirezionali.add("A");
		busMonodirezionali.add("B");
		busMonodirezionali.add("NP");
		busMonodirezionali.add("2");
		
		menuVoice.add("MENU_TAXI");
		menuVoice.add("MENU_AUTOBUS");
		menuVoice.add("MENU_TRENI");
		menuVoice.add("MENU_PARCHEGGI");
		menuVoice.add("MENU_BICI");
		
		//Quick per autobus
		Message AoR = new Message();
		AoR.setText("Scegli andata o ritorno");
		messaggiQuick.add(AoR);
		
		Message FascieO = new Message();
		FascieO.setText("Seleziona la fascia oraria che ti interessa: ");
		messaggiQuick.add(FascieO);
		
		Message stops = new Message();
		stops.setText("Seleziona la fermata desiderata: ");
		messaggiQuick.add(stops);
		
		Message funivia = new Message();
		funivia.setText("Scegli quale percorso: ");
		messaggiQuick.add(funivia);
		
		//schede menu
		TitleSubTitle MENU_AUTOBUS = new TitleSubTitle();
		MENU_AUTOBUS.setTitle("Vuoi accedere alla sezione autobus o funivia?");
		option.add(MENU_AUTOBUS);
		
		//schede per i taxi
		tmpTaxi = new ArrayList<TaxiContact>();
		try {
			tmpTaxi = Database.getTaxiContacts();
		} catch (ExecutionException e) {	
			e.printStackTrace();
		}
		
		for(int i = 0; i < tmpTaxi.size(); i++){
			TitleSubTitle TAXI = new TitleSubTitle();
			TAXI.setTitle(tmpTaxi.get(i).getName());
			optiontaxi.add(TAXI);
		}

		//schede per i parcheggi
		tmpParcheggi = new ArrayList<Parking>();
		try {
			tmpParcheggi = Database.getParkings();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		for(int i = 0; i < tmpParcheggi.size(); i++){
			TitleSubTitle PARCHEGGI = new TitleSubTitle();
			PARCHEGGI.setTitle(tmpParcheggi.get(i).getName()+": "+	tmpParcheggi.get(i).getDescription());
			if(tmpParcheggi.get(i).getSlotsAvailable()==-2){
				PARCHEGGI.setSubTitle("posti totali: " +	tmpParcheggi.get(i).getSlotsTotal());
			}
			else{
				PARCHEGGI.setSubTitle("posti diponibili: " + tmpParcheggi.get(i).getSlotsAvailable() + "    posti totali: " + tmpParcheggi.get(i).getSlotsTotal());
			}
				optionparcheggi.add(PARCHEGGI);
		}

		//schede per il bikesharing 
		tmpParcheggibici = new ArrayList<Parking>();
		try {
			tmpParcheggibici = Database.getBikeSharings();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		for(int i = 0; i < tmpParcheggibici.size(); i++){
			TitleSubTitle PARCHEGGI =new TitleSubTitle();
			PARCHEGGI.setTitle(tmpParcheggibici.get(i).getName() + ": " + tmpParcheggibici.get(i).getDescription());
			PARCHEGGI.setSubTitle("biciclette: " + tmpParcheggibici.get(i).getSlotsAvailable() + "    posti liberi: " + tmpParcheggibici.get(i).getSlotsTotal());
			optionparcheggibici.add(PARCHEGGI);
		}

		//quick linea dei treni
		Message QTRENI = new Message();
		QTRENI.setText("Scegli la linea");
		messaggiQuickTreni.add(QTRENI);
	}

	private class TitleSubTitle {
		private String url;
		private String title;
		private String subTitle;

		public void setUrl(String url) {
			this.url = url;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public void setSubTitle(String subTitle) {
			this.subTitle = subTitle;
		}
	}


	/**
	 * methods which analyze the postbacks ie. the button clicks sent by
	 * senderId and replies according to it.
	 * 
	 * @param senderId
	 * @param text
	 * @return
	 */
	public List<String> getPostBackReplies(String senderId, String text) {
		List<String> postbackReplies = new ArrayList<String>();
		
		ChatContext chatContext = getChatContext(senderId);
		
		if(menuVoice.contains(text)){
			chatContext.context = text;
			menuChoise(chatContext, text);
			
			if(chatContext.context.equals("MENU_AUTOBUS_1"))
				postbackReplies.add(makeCards(senderId, getCardsMenu(0, chatContext)));	
		}
			
		if(chatContext.context.equals("start")){
			chatContext.context = "generale";
			postbackReplies.add(makeMessage(senderId, "Benvenuto al bot di ViaggiaTrento, usa il menù laterale per accedere a tutte le funzioni"));
		}
		
		if(text.equals("MENU_FUNIVIA")){
			chatContext.context = "MENU_FUNIVIA";
			postbackReplies.add(makeQuick(senderId, getQuickMessage(3, chatContext)));
			chatContext.context = "MENU_FUNIVIA_1";
		}
		
		if(text.equals("MENU_AUTOBUS_1")){
			chatContext.context = "MENU_AUTOBUS_1";	
			postbackReplies.add(makeMessage(senderId, "Scegli il bus di cui vuoi sapere gli orari: (1-17)"));
		}
		
		if(chatContext.context.equals("MENU_TAXI_1")){
			postbackReplies.add(makeCards(senderId, getTaxi(chatContext)));
		}
		
		if(chatContext.context.equals("MENU_PARCHEGGI_1")){
			for(; chatContext.index < chatContext.parcheggi.size(); chatContext.index++)
				postbackReplies.add(makeCards(senderId, getParcheggi(chatContext)));
		}
		
		if(chatContext.context.equals("MENU_BICI_1")){
			for(; chatContext.index < chatContext.parcheggibici.size(); chatContext.index++)
				postbackReplies.add(makeCards(senderId, getParcheggiBici(chatContext)));
		}
		
		if(chatContext.context.equals("MENU_TRENI_1")){
			postbackReplies.add(makeQuick(senderId, getTreni(chatContext)));
			chatContext.context = "MENU_TRENI_2";
		}
		
		return postbackReplies;
	}

	public void trainId(String senderId, String text, ChatContext chatContext){
		switch(text){
			case "Bolzano - Verona":{
				chatContext.trainID = "BV_R1_G";
				break;
			}
			case "Verona - Bolzano":{
				chatContext.trainID = "BV_R1_R";
				break;
			}
			case "Trento - Bassano.d.G":{
				chatContext.trainID = "TB_R2_G";
				break;
			}
			case " Bassano.d.G - Trento":{
				chatContext.trainID = "TB_R2_R";
				break;
			}
			case "Trento - Malè":{
				chatContext.trainID = "555";
				break;
			}
			case "Malè - Trento":{
				chatContext.trainID = "556";
				break;
			}
			default:{
				chatContext.trainID = "ERROR";
			}
		}
	}
	
	private ChatContext getChatContext(String senderId) {
		ChatContext chatContext = chatContexts.get(senderId);
		if (chatContext == null) {
			chatContext = new ChatContext();
			chatContexts.put(senderId, chatContext);
		}
		return chatContext;
	}

	public void menuChoise(ChatContext chatContext, String text){	
		switch(text)
		{
			case "MENU_TAXI":{
				chatContext.index = 0;
				
				try {
					chatContext.taxi = Database.getTaxiContacts();
				} catch (ExecutionException e) {	
				e.printStackTrace();
				}
				
				chatContext.context = "MENU_TAXI_1";
				break;
			}
			case "MENU_AUTOBUS":{
				chatContext.index = 0;
				chatContext.context = "MENU_AUTOBUS_1";
				break;
			}
			case "MENU_TRENI":{
				chatContext.index = 0;
				chatContext.context = "MENU_TRENI_1";
				break;
			}
			case "MENU_PARCHEGGI":{
				chatContext.index = 0;
				
				try {
					chatContext.parcheggi = Database.getParkings();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				
				chatContext.context = "MENU_PARCHEGGI_1";
				break;
			}
			case "MENU_BICI":{
				chatContext.index = 0;
				
				try {
					chatContext.parcheggibici = Database.getBikeSharings();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				
				chatContext.context = "MENU_BICI_1";
				break;
			}
			default:{}
		}
	}

	/**
	 * methos which analyze the simple texts sent by senderId and replies
	 * according to it.
	 * 
	 * @param senderId
	 * @param text
	 * @return
	 */
	public List<String> getReplies(String senderId, String text) {
		
		ChatContext chatContext = getChatContext(senderId);
		List<String> replies = new ArrayList<String>();
		List<String> stops = new ArrayList<String>();
		/*String link = StringUtils.replace(profileLink, "SENDER_ID", senderId);
		FbProfile profile = getObjectFromUrl(link, FbProfile.class);*/

		if(chatContext.context.equals("generale")){
			replies.add(makeMessage(senderId, "Comando non riconosciuto"));	
		}
		
		if(chatContext.context.equals("MENU_FUNIVIA_2")){
			chatContext.fascia = text;
			replies.add(makeMessage(senderId, getOrariFunivia(senderId, chatContext, text)));
			chatContext.context = "MENU_FUNIVIA_3";
		}
		
		if(chatContext.context.equals("MENU_FUNIVIA_1")){
			if(text.equals("Trento - Sardagna"))
				chatContext.direzione = true;
			else
				chatContext.direzione = false;
			
			getFuniviaID(chatContext);
			
			replies.add(makeQuick(senderId, getQuickMessage(1, chatContext)));
			chatContext.context = "MENU_FUNIVIA_2";
		}
		
		if(chatContext.context.equals("MENU_AUTOBUS_5") || ((((!(text.equals("Altro.."))) && chatContext.index > 0)) && (chatContext.context.equals("MENU_AUTOBUS_4")))){
			replies.add(makeMessage(senderId, getOrariAutobus(senderId, chatContext, text)));
			chatContext.context = "MENU_AUTOBUS_6";
		}
		
		if(chatContext.context.equals("MENU_AUTOBUS_4")){
			if(chatContext.index == 0)
				chatContext.text = text;
			
			stops = stopsSearcher(chatContext);

			if(!(stops.size() == 0))	
				replies.add(makeQuick(senderId, getQuickMessage(2, chatContext)));
			else
				replies.add(makeMessage(senderId, "Non ho trovato nessun risultato"));
		}
		
		if(chatContext.context.equals("MENU_AUTOBUS_3")){
			chatContext.fascia = text;
			replies.add(makeMessage(senderId, "Di che fermata desideri sapere gli orari?"));
		
			chatContext.context = "MENU_AUTOBUS_4";
		}
		
		if(chatContext.context.equals("MENU_AUTOBUS_2") || busMonodirezionali.contains(text)){
			if(busMonodirezionali.contains(text)){
				chatContext.busID = text;
				chatContext.context = "MENU_AUTOBUS_2";
			}	
			else{
				if(text.equals("Andata"))
					chatContext.direzione = true;
				else
					chatContext.direzione = false;
			}
			
			try {
				String routeId = Database.getAutobusRouteId(chatContext.busID, chatContext.direzione);
				chatContext.autobus = Database.getAutobusTimetable(routeId);
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			replies.add(makeQuick(senderId, getQuickMessage(1, chatContext)));
			chatContext.context = "MENU_AUTOBUS_3";	
		}	
		
		
		if(chatContext.context.equals("MENU_TRENI_5") || ((((!(text.equals("Altro.."))) && chatContext.index > 0)) && (chatContext.context.equals("MENU_TRENI_4")))){
			replies.add(makeMessage(senderId, getOrariTreni(senderId, chatContext, text)));
			chatContext.context = "MENU_AUTOBUS_6";
		}
		
		if(chatContext.context.equals("MENU_TRENI_4")){
			if(chatContext.index == 0)
				chatContext.text = text;
			
			stops = stopsSearcher(chatContext);

			if(!(stops.size() == 0))	
				replies.add(makeQuick(senderId, getQuickMessage(2, chatContext)));
			else
				replies.add(makeMessage(senderId, "Non ho trovato nessun risultato"));
		}
		
		if(chatContext.context.equals("MENU_TRENI_3")){
			chatContext.fascia = text;
			replies.add(makeMessage(senderId, "Di che fermata desideri sapere gli orari?"));
			
			chatContext.context = "MENU_TRENI_4";
		}
		
		if(chatContext.context.equals("MENU_TRENI_2")){
			trainId(senderId, text, chatContext);
			try {
				chatContext.treni = Database.getTrainTimetable(chatContext.trainID);
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			replies.add(makeQuick(senderId, getQuickMessage(1, chatContext)));
			chatContext.context = "MENU_TRENI_3";
		}
		
		if(chatContext.context.equals("MENU_AUTOBUS_1")){
			chatContext.busID = text;		
			replies.add(makeQuick(senderId, getQuickMessage(0, chatContext)));	
			chatContext.context = "MENU_AUTOBUS_2";
		}
		
		return replies;
	}

	public void getFuniviaID(ChatContext chatContext){		
		if(chatContext.direzione)
			chatContext.FuniviaID = "FunA";
		else
			chatContext.FuniviaID = "FunR";		
	}
	
	public String timePrinter(String senderId, ChatContext chatContext, String text){
		String messaggio = "";
		String subString = "";
		
		if(chatContext.context.equals("MENU_FUNIVIA_2")){
			switch (chatContext.fascia){
				case "04-06":{
					for(int i = 0; i < chatContext.autobus.getTimes().size(); i++){
						if(!chatContext.autobus.getTimes().get(i).get(0).isEmpty()){
							subString = chatContext.autobus.getTimes().get(i).get(0).substring(0,2);
							
							if((Integer.parseInt(subString) >= 4 && Integer.parseInt(subString) < 7)){
								messaggio = messaggio + "  " + chatContext.autobus.getTimes().get(i).get(0);
							}
						}
					}
	
					break;
				}
				case "07-09":{
					for(int i = 0; i < chatContext.autobus.getTimes().size(); i++){
						if(!chatContext.autobus.getTimes().get(i).get(0).isEmpty()){
							subString = chatContext.autobus.getTimes().get(i).get(0).substring(0,2);
							
							if((Integer.parseInt(subString) >= 7 && Integer.parseInt(subString) < 10)){
								messaggio = messaggio + "  " + chatContext.autobus.getTimes().get(i).get(0);
							}
						}
					}
	
					break;
				}
				case "10-12":{
					for(int i = 0; i < chatContext.autobus.getTimes().size(); i++){
						if(!chatContext.autobus.getTimes().get(i).get(0).isEmpty()){
							subString = chatContext.autobus.getTimes().get(i).get(0).substring(0,2);
							
							if((Integer.parseInt(subString) >= 10 && Integer.parseInt(subString) < 13)){
								messaggio = messaggio + "  " + chatContext.autobus.getTimes().get(i).get(0);
							}
						}
					}
		
					break;
				}
				case "13-15":{
					for(int i = 0; i < chatContext.autobus.getTimes().size(); i++){
						if(!chatContext.autobus.getTimes().get(i).get(0).isEmpty()){
							subString = chatContext.autobus.getTimes().get(i).get(0).substring(0,2);
							
							if((Integer.parseInt(subString) >= 13 && Integer.parseInt(subString) < 16)){
								messaggio = messaggio + "  " + chatContext.autobus.getTimes().get(i).get(0);
							}
						}
					}
					
					break;
				}
				case "16-18":{
					for(int i = 0; i < chatContext.autobus.getTimes().size(); i++){
						if(!chatContext.autobus.getTimes().get(i).get(0).isEmpty()){
							subString = chatContext.autobus.getTimes().get(i).get(0).substring(0,2);
							
							if((Integer.parseInt(subString) >= 16 && Integer.parseInt(subString) < 19)){
								messaggio = messaggio + "  " + chatContext.autobus.getTimes().get(i).get(0);
							}
						}
					}
				
					break;
				}
				case "19-21":{
					for(int i = 0; i < chatContext.autobus.getTimes().size(); i++){
						if(!chatContext.autobus.getTimes().get(i).get(0).isEmpty()){
							subString = chatContext.autobus.getTimes().get(i).get(0).substring(0,2);
							
							if((Integer.parseInt(subString) >= 19 && Integer.parseInt(subString) < 22)){
								messaggio = messaggio + "  " + chatContext.autobus.getTimes().get(i).get(0);
							}
						}
					}
				
					break;
				}
				case "22-24":{
					for(int i = 0; i < chatContext.autobus.getTimes().size(); i++){
						if(!chatContext.autobus.getTimes().get(i).get(0).isEmpty()){
							subString = chatContext.autobus.getTimes().get(i).get(0).substring(0,2);
							
							if((Integer.parseInt(subString) >= 22 || (Integer.parseInt(subString) >= 0 && Integer.parseInt(subString) < 1))){
								messaggio = messaggio + "  " + chatContext.autobus.getTimes().get(i).get(0);
							}
						}
					}
				
					break;
				}
				default:{
					messaggio="Errore";
				}
			}
		}
		else{
			int j = findStopInList(chatContext, text);
			
			if(chatContext.context.equals("MENU_AUTOBUS_5")){
				switch (chatContext.fascia){
					case "04-06":{
						for(int i = 0; i < chatContext.autobus.getTimes().size(); i++){
							if(!chatContext.autobus.getTimes().get(i).get(j).isEmpty()){
								subString = chatContext.autobus.getTimes().get(i).get(j).substring(0,2);
								
								if((Integer.parseInt(subString) >= 4 && Integer.parseInt(subString) < 7)){
									messaggio = messaggio + "  " + chatContext.autobus.getTimes().get(i).get(j);
								}
							}
						}
		
						break;
					}
					case "07-09":{
						for(int i = 0; i < chatContext.autobus.getTimes().size(); i++){
							if(!chatContext.autobus.getTimes().get(i).get(j).isEmpty()){
								subString = chatContext.autobus.getTimes().get(i).get(j).substring(0,2);
								
								if((Integer.parseInt(subString) >= 7 && Integer.parseInt(subString) < 10)){
									messaggio = messaggio + "  " + chatContext.autobus.getTimes().get(i).get(j);
								}
							}
						}
		
						break;
					}
					case "10-12":{
						for(int i = 0; i < chatContext.autobus.getTimes().size(); i++){
							if(!chatContext.autobus.getTimes().get(i).get(j).isEmpty()){
								subString = chatContext.autobus.getTimes().get(i).get(j).substring(0,2);
								
								if((Integer.parseInt(subString) >= 10 && Integer.parseInt(subString) < 13)){
									messaggio = messaggio + "  " + chatContext.autobus.getTimes().get(i).get(j);
								}
							}
						}
			
						break;
					}
					case "13-15":{
						for(int i = 0; i < chatContext.autobus.getTimes().size(); i++){
							if(!chatContext.autobus.getTimes().get(i).get(j).isEmpty()){
								subString = chatContext.autobus.getTimes().get(i).get(j).substring(0,2);
								
								if((Integer.parseInt(subString) >= 13 && Integer.parseInt(subString) < 16)){
									messaggio = messaggio + "  " + chatContext.autobus.getTimes().get(i).get(j);
								}
							}
						}
						
						break;
					}
					case "16-18":{
						for(int i = 0; i < chatContext.autobus.getTimes().size(); i++){
							if(!chatContext.autobus.getTimes().get(i).get(j).isEmpty()){
								subString = chatContext.autobus.getTimes().get(i).get(j).substring(0,2);
								
								if((Integer.parseInt(subString) >= 16 && Integer.parseInt(subString) < 19)){
									messaggio = messaggio + "  " + chatContext.autobus.getTimes().get(i).get(j);
								}
							}
						}
					
						break;
					}
					case "19-21":{
						for(int i = 0; i < chatContext.autobus.getTimes().size(); i++){
							if(!chatContext.autobus.getTimes().get(i).get(j).isEmpty()){
								subString = chatContext.autobus.getTimes().get(i).get(j).substring(0,2);
								
								if((Integer.parseInt(subString) >= 19 && Integer.parseInt(subString) < 22)){
									messaggio = messaggio + "  " + chatContext.autobus.getTimes().get(i).get(j);
								}
							}
						}
					
						break;
					}
					case "22-24":{
						for(int i = 0; i < chatContext.autobus.getTimes().size(); i++){
							if(!chatContext.autobus.getTimes().get(i).get(j).isEmpty()){
								subString = chatContext.autobus.getTimes().get(i).get(j).substring(0,2);
								
								if((Integer.parseInt(subString) >= 22 || (Integer.parseInt(subString) >= 0 && Integer.parseInt(subString) < 1))){
									messaggio = messaggio + "  " + chatContext.autobus.getTimes().get(i).get(j);
								}
							}
						}
					
						break;
					}
					default:{
						messaggio="Errore";
					}
				}
			}
			else if(chatContext.context.equals("MENU_TRENI_5")){
				switch (chatContext.fascia){
				
					case "04-06":{
						for(int i = 0; i < chatContext.treni.getTimes().size(); i++){
							if(!chatContext.treni.getTimes().get(i).get(j).isEmpty()){
								subString = chatContext.treni.getTimes().get(i).get(j).substring(0,2);
								
								if((Integer.parseInt(subString) >= 4 && Integer.parseInt(subString) < 7)){
									messaggio = messaggio + "  " + chatContext.treni.getTimes().get(i).get(j);
								}
							}
						}
		
						break;
					}
					case "07-09":{
						for(int i = 0; i < chatContext.treni.getTimes().size(); i++){
							if(!chatContext.treni.getTimes().get(i).get(j).isEmpty()){
								subString = chatContext.treni.getTimes().get(i).get(j).substring(0,2);
								
								if((Integer.parseInt(subString) >= 7 && Integer.parseInt(subString) < 10)){
									messaggio = messaggio + "  " + chatContext.treni.getTimes().get(i).get(j);
								}
							}
						}
		
						break;
					}
					case "10-12":{
						for(int i = 0; i < chatContext.treni.getTimes().size(); i++){
							if(!chatContext.treni.getTimes().get(i).get(j).isEmpty()){
								subString = chatContext.treni.getTimes().get(i).get(j).substring(0,2);
								
								if((Integer.parseInt(subString) >= 10 && Integer.parseInt(subString) < 13)){
									messaggio = messaggio + "  " + chatContext.treni.getTimes().get(i).get(j);
								}
							}
						}
			
						break;
					}
					case "13-15":{
						for(int i = 0; i < chatContext.treni.getTimes().size(); i++){
							if(!chatContext.treni.getTimes().get(i).get(j).isEmpty()){
								subString = chatContext.treni.getTimes().get(i).get(j).substring(0,2);
								
								if((Integer.parseInt(subString) >= 13 && Integer.parseInt(subString) < 16)){
									messaggio = messaggio + "  " + chatContext.treni.getTimes().get(i).get(j);
								}
							}
						}
						
						break;
					}
					case "16-18":{
						for(int i = 0; i < chatContext.treni.getTimes().size(); i++){
							if(!chatContext.treni.getTimes().get(i).get(j).isEmpty()){
								subString = chatContext.treni.getTimes().get(i).get(j).substring(0,2);
								
								if((Integer.parseInt(subString) >= 16 && Integer.parseInt(subString) < 19)){
									messaggio = messaggio + "  " + chatContext.treni.getTimes().get(i).get(j);
								}
							}
						}
					
						break;
					}
					case "19-21":{
						for(int i = 0; i < chatContext.treni.getTimes().size(); i++){
							if(!chatContext.treni.getTimes().get(i).get(j).isEmpty()){
								subString = chatContext.treni.getTimes().get(i).get(j).substring(0,2);
								
								if((Integer.parseInt(subString) >= 19 && Integer.parseInt(subString) < 22)){
									messaggio = messaggio + "  " + chatContext.treni.getTimes().get(i).get(j);
								}
							}
						}
					
						break;
					}
					case "22-24":{
						for(int i = 0; i < chatContext.treni.getTimes().size(); i++){
							if(!chatContext.treni.getTimes().get(i).get(j).isEmpty()){
								subString = chatContext.treni.getTimes().get(i).get(j).substring(0,2);
								
								if((Integer.parseInt(subString) >= 22 || (Integer.parseInt(subString) >= 0 && Integer.parseInt(subString) < 1))){
									messaggio = messaggio + "  " + chatContext.treni.getTimes().get(i).get(j);
								}
							}
						}
					
						break;
					}
					default:{
						messaggio="Errore";
					}
				}
			}
		}
		
		return messaggio;
	}
	
	private List<String> stopsSearcher(ChatContext chatcontext){
		List<String> stops = new ArrayList<>();

		if(chatcontext.context.equals("MENU_AUTOBUS_4")){
			for(int i = 0; i < chatcontext.autobus.getStops().size(); i++){
				if(chatcontext.autobus.getStops().get(i).toLowerCase().contains(chatcontext.text.toLowerCase()))
					stops.add(chatcontext.autobus.getStops().get(i));
			}
		} else if(chatcontext.context.equals("MENU_TRENI_4")){
			for(int i = 0; i < chatcontext.treni.getStops().size(); i++){
				if(chatcontext.treni.getStops().get(i).toLowerCase().contains(chatcontext.text.toLowerCase()))
					stops.add(chatcontext.treni.getStops().get(i));
			}
		}
		
		return stops;
	}
	
	private String getOrariFunivia(String senderId, ChatContext chatContext, String text){
		String msg = "Questa funivia non esiste";
		
		try {
			chatContext.autobus = Database.getAutobusTimetable(chatContext.FuniviaID);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		msg = timePrinter(senderId, chatContext, text);
		
		return msg;
	}
	
	private String getOrariAutobus(String senderId, ChatContext chatContext, String text){
		String msg = "Questo autobus non esiste (in caso di autobus  \"/\" digitare solo il bus di riferimento)";
		
		try {
			String routeId = Database.getAutobusRouteId(chatContext.busID, chatContext.direzione);
			chatContext.autobus = Database.getAutobusTimetable(routeId);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		msg = timePrinter(senderId, chatContext, text);
		
		return msg;
	}
	
	private String getOrariTreni(String senderId, ChatContext chatContext, String text){
		String msg = "Questo treno non esiste";
		
		try {
			chatContext.autobus = Database.getTrainTimetable(chatContext.trainID);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		msg = timePrinter(senderId, chatContext, text);
		
		return msg;
	}
	
	private int findStopInList(ChatContext chatContext, String text){
		boolean find = false;
		int i = 0;
		
		if(chatContext.context.equals("MENU_AUTOBUS_5")){
			for(; i < chatContext.autobus.getStops().size() && !find; i++){
				if(chatContext.autobus.getStops().get(i).contains(text)){
					find = true;
				}
			}
		}
		else{
			for(; i < chatContext.treni.getStops().size() && !find; i++){
				if(chatContext.treni.getStops().get(i).contains(text)){
					find = true;
				}
			}
		}	
		return i - 1;
	}
	
	private String makeMessage(String senderId, String msg){
		Message fbMsg = getMsg(msg);
		String fbReply = getJsonReply(senderId, fbMsg);
		
		return fbReply;
	}
	
	private String makeCards(String senderId, Message cards){
		Message fbMsg = cards;
		String fbReply = getJsonReply(senderId, fbMsg);
		
		return fbReply;
	}
	private String makeQuick(String senderId, Message quick){
		Message fbMsg = quick;
		String fbReply = getJsonReply(senderId, fbMsg);
		
		return fbReply;
	}
	private Message getMsg(String msg) {
		Message message = new Message();
		message.setText(msg);
		return message;
	}
	
	private Message getQuestion(Payload payload) {
		Attachment attachment = new Attachment();
		attachment.setPayload(payload);
		attachment.setType("template");

		Message message = new Message();
		message.setAttachment(attachment);

		return message;
	}

	/**
	 * final body which will be sent to fb messenger api through a post call
	 * 
	 * @see WebHookServlet#FB_MSG_URL
	 * @param senderId
	 * @param message
	 * @return
	 */
	private String getJsonReply(String senderId, Message message) {
		Recipient recipient = new Recipient();
		recipient.setId(senderId);
		Messaging reply = new Messaging();
		reply.setRecipient(recipient);
		reply.setMessage(message);

		String jsonReply = new Gson().toJson(reply);
		return jsonReply;
	}
	
	private Message getQuickMessage(int i, ChatContext chatContext) {
		List<QuickReply> quickReplies = getQuick(chatContext);
		Message message = messaggiQuick.get(i);
		message.setQuickReplies(quickReplies);
		
		return message;
	}
	
	private List<QuickReply> getQuick(ChatContext chatContext) {	
 		List<QuickReply> quickReplies =  new ArrayList<>();
 		
 		QuickReply quickReply = new QuickReply();
 		quickReplies.add(quickReply);
		
		List<QuickReply> quickRepliesOption = getQuickReplies(chatContext);
		quickReply.setQuickReplies(quickRepliesOption);

		return quickRepliesOption;		
	}
	
	private List<QuickReply> getQuickReplies(ChatContext chatContext){
		List<QuickReply> quick = new ArrayList<>();
		
		if(chatContext.context.equals("MENU_AUTOBUS_1")){
			QuickReply qAndata = new QuickReply();
			quick.add(qAndata);
			qAndata.setType("text");
			qAndata.setTitle("Andata");
			qAndata.setPayload("Andata");
			
			QuickReply qRitorno = new QuickReply();
			quick.add(qRitorno);
			qRitorno.setType("text");
			qRitorno.setTitle("Ritorno");
			qRitorno.setPayload("Ritorno");
		}
		
		if(chatContext.context.equals("MENU_AUTOBUS_2") || chatContext.context.equals("MENU_TRENI_2") || chatContext.context.equals("MENU_FUNIVIA_1")){
			QuickReply fascia1 = new QuickReply();
			quick.add(fascia1);
			fascia1.setType("text");
			fascia1.setTitle("04-06");
			fascia1.setPayload("04-06");
			
			QuickReply fascia2 = new QuickReply();
			quick.add(fascia2);
			fascia2.setType("text");
			fascia2.setTitle("07-09");
			fascia2.setPayload("07-09");
			
			QuickReply fascia3 = new QuickReply();
			quick.add(fascia3);
			fascia3.setType("text");
			fascia3.setTitle("10-12");
			fascia3.setPayload("10-12");
			
			QuickReply fascia4 = new QuickReply();
			quick.add(fascia4);
			fascia4.setType("text");
			fascia4.setTitle("13-15");
			fascia4.setPayload("13-15");
			
			QuickReply fascia5 = new QuickReply();
			quick.add(fascia5);
			fascia5.setType("text");
			fascia5.setTitle("16-18");
			fascia5.setPayload("16-18");
			
			QuickReply fascia6 = new QuickReply();
			quick.add(fascia6);
			fascia6.setType("text");
			fascia6.setTitle("19-21");
			fascia6.setPayload("19-21");
			
			QuickReply fascia7 = new QuickReply();
			quick.add(fascia7);
			fascia7.setType("text");
			fascia7.setTitle("22-24");
			fascia7.setPayload("22-24");
		}
		
		if(chatContext.context.equals("MENU_AUTOBUS_4") || chatContext.context.equals("MENU_TRENI_4")){
			List<String> stops = new ArrayList<String>();
			
			stops = stopsSearcher(chatContext);
	
			for(int i = 0; i < 9 && chatContext.index < stops.size(); chatContext.index++, i++){
				String title = stops.get(chatContext.index);
				
				if(title.length() >= 20)
					title = stringCut(title);
				
				QuickReply stop = new QuickReply();
				quick.add(stop);
				stop.setType("text");
				stop.setTitle(title);
				stop.setPayload(title);
			}
			
			if(chatContext.index < stops.size() && chatContext.index < stops.size()){
				QuickReply altro = new QuickReply();
				quick.add(altro);
				altro.setType("text");
				altro.setTitle("Altro..");
				altro.setPayload("Altro..");
			}
			else {
				if(chatContext.context.equals("MENU_AUTOBUS_4"))
					chatContext.context = "MENU_AUTOBUS_5";
				else
					chatContext.context = "MENU_TRENI_5";
			}
		}
			
		if(chatContext.context.equals("MENU_TRENI_1")){
			QuickReply qBV = new QuickReply();
			quick.add(qBV);
			qBV.setType("text");
			qBV.setTitle("Bolzano - Verona");
			qBV.setPayload("Bolzano - Verona");
			
			QuickReply qVB = new QuickReply();
			quick.add(qVB);
			qVB.setType("text");
			qVB.setTitle("Verona - Bolzano");
			qVB.setPayload("Verona - Bolzano");

			QuickReply qTB = new QuickReply();
			quick.add(qTB);
			qTB.setType("text");
			qTB.setTitle("Trento - Bassano.d.G");
			qTB.setPayload("Trento - Bassano.d.G");
			
			QuickReply qBT = new QuickReply();
			quick.add(qBT);
			qBT.setType("text");
			qBT.setTitle("Bassano.d.G - Trento");
			qBT.setPayload("Bassano.d.G - Trento");
			
			QuickReply qTM = new QuickReply();
			quick.add(qTM);
			qTM.setType("text");
			qTM.setTitle("Trento - Malè");
			qTM.setPayload("Trento - Malè");
			
			QuickReply qMT = new QuickReply();
			quick.add(qMT);
			qMT.setType("text");
			qMT.setTitle("Malè - Trento");
			qMT.setPayload("Malè - Trento");
		}
		
		if(chatContext.context.equals("MENU_FUNIVIA")){
			QuickReply fAndata = new QuickReply();
			quick.add(fAndata);
			fAndata.setType("text");
			fAndata.setTitle("Trento - Sardagna");
			fAndata.setPayload("Trento - Sardagna");
			
			QuickReply fRitorno = new QuickReply();
			quick.add(fRitorno);
			fRitorno.setType("text");
			fRitorno.setTitle("Sardagna - Trento");
			fRitorno.setPayload("Sardagna - Trento");
		}
		
		return quick;	
	}

	private String stringCut(String title){
		title = title.substring(0, 20);
		
		return title;
	}
	
	private Message getTaxi(ChatContext chatContext) {
		List<Element> elements = getElementsTaxi(chatContext);
		
		Payload payload = new Payload();
		
		payload.setElements(elements);
		payload.setTemplateType("generic");

		return getQuestion(payload);
	}
	
	private Message getTreni(ChatContext chatContext) {
		List<QuickReply> quickReplies = getQuick(chatContext);
		Message message = messaggiQuickTreni.get(0);
		message.setQuickReplies(quickReplies);
		
		return message;
	}
	
	private Message getParcheggi(ChatContext chatContext) {
		List<Element> elements = getElementsParcheggi(chatContext);
		Payload payload = new Payload();
		payload.setElements(elements);
		payload.setTemplateType("generic");

		return getQuestion(payload);
	}
	
	private Message getParcheggiBici(ChatContext chatContext) {
		List<Element> elements = getElementsParcheggiBici(chatContext);
		Payload payload = new Payload();
		payload.setElements(elements);
		payload.setTemplateType("generic");

		return getQuestion(payload);
	}
	
	private List<Element> getElementsTaxi(ChatContext chatContext) {
		List<Element> elements = new ArrayList<>();
		
			for(; chatContext.index < chatContext.taxi.size(); chatContext.index++){
				TitleSubTitle Taxi1 = optiontaxi.get(chatContext.index);
				Element element = new Element();
				
				elements.add(element);
				List<Button> buttons = getButtons(chatContext);
				
				element.setButtons(buttons);
				element.setTitle(Taxi1.title);
				element.setSubtitle(Taxi1.subTitle);
			}
			return elements;
	}
	
	private List<Element> getElementsParcheggi(ChatContext chatContext) {
			List<Element> elements = new ArrayList<>();	
			TitleSubTitle Parcheggi = optionparcheggi.get(chatContext.index);
			
			Element element = new Element();
			
			elements.add(element);
			element.setTitle(Parcheggi.title);
			element.setSubtitle(Parcheggi.subTitle);
			
		return elements;
	}
	private List<Element> getElementsParcheggiBici(ChatContext chatContext) {
			List<Element> elements = new ArrayList<>();	
			TitleSubTitle Parcheggibici = optionparcheggibici.get(chatContext.index);
			
			Element element = new Element();
			
			elements.add(element);
			element.setTitle(Parcheggibici.title);
			element.setSubtitle(Parcheggibici.subTitle);
	
			return elements;
	}
	
		private Message getCardsMenu(int i, ChatContext chatContext) {
		List<Element> elements = getElementsMenu(i, chatContext);
		Payload payload = new Payload();
		payload.setElements(elements);
		payload.setTemplateType("generic");

		return getQuestion(payload);
	}

	private List<Element> getElementsMenu(int i, ChatContext chatContext) {
		List<Element> elements = new ArrayList<>();

			TitleSubTitle image = option.get(i);
			Element element = new Element();
			elements.add(element);

			List<Button> buttons = getButtons(chatContext);
			element.setButtons(buttons);

			element.setTitle(image.title);
			element.setSubtitle(image.subTitle);

		return elements;
	}
	
	private List<Button> getButtons(ChatContext chatContext) {
		List<Button> buttons = new ArrayList<>();


		if(chatContext.context == "MENU_AUTOBUS_1"){
			Button bt = new Button();
			buttons.add(bt);
			bt.setType("postback");
			bt.setTitle("Autobus"); 
			bt.setPayload("MENU_AUTOBUS_1");
			
			Button bt2 = new Button();
			buttons.add(bt2);
			bt2.setType("postback");
			bt2.setTitle("Funivia"); 
			bt2.setPayload("MENU_FUNIVIA");
		}
		
		if(chatContext.context == "MENU_TAXI_1"){
			int i = chatContext.index;
			while(i < chatContext.taxi.size()){
				if(chatContext.taxi.get(i).getPhone() != null){
					if(!chatContext.taxi.get(i).getPhone().get(i).isEmpty() && !chatContext.taxi.get(i).getSms().isEmpty()){
						Button bt1 = new Button();
						buttons.add(bt1);
						bt1.setType("phone_number");
						bt1.setTitle(chatContext.taxi.get(i).getPhone().get(i).toString());
						bt1.setPayload("+39" + chatContext.taxi.get(i).getPhone().get(i));
						Button bt3 = new Button();
						buttons.add(bt3);
						bt3.setType("phone_number");
						bt3.setTitle("sms");
						bt3.setPayload("+39" + chatContext.taxi.get(i).getSms());
						}
					else if(chatContext.taxi.get(i).getPhone().get(1) != null && chatContext.taxi.get(i).getSms().isEmpty()){
						Button bt1 = new Button();
						buttons.add(bt1);
						bt1.setType("phone_number");
						bt1.setTitle(chatContext.taxi.get(i).getPhone().get(0).toString());
						bt1.setPayload("+39" + chatContext.taxi.get(i).getPhone());
						Button bt2 = new Button();
						buttons.add(bt2);
						bt2.setType("phone_number");
						bt2.setTitle(chatContext.taxi.get(i).getPhone().get(1).toString());
						bt2.setPayload("+39" + chatContext.taxi.get(i).getPhone().get(1));
						}
					else if(chatContext.taxi.get(i).getPhone().get(1).isEmpty() && chatContext.taxi.get(i).getSms() != null){
						Button bt1 = new Button();
						buttons.add(bt1);
						bt1.setType("phone_number");
						bt1.setTitle(chatContext.taxi.get(i).getPhone().get(i).toString());
						bt1.setPayload("+39" + chatContext.taxi.get(i).getPhone());
						Button bt2 = new Button();
						buttons.add(bt2);
						bt2.setType("phone_number");
						bt2.setTitle("sms");
						bt2.setPayload("+39" + chatContext.taxi.get(i).getSms());
						}
					else if(chatContext.taxi.get(i).getPhone().get(1).isEmpty() && chatContext.taxi.get(i).getSms().isEmpty()){
						Button bt1 = new Button();
						buttons.add(bt1);
						bt1.setType("phone_number");
						bt1.setTitle(chatContext.taxi.get(i).getPhone().get(i).toString());
						bt1.setPayload("+39" + chatContext.taxi.get(i).getPhone());
						}
					}
				else if(chatContext.taxi.get(i).getSms() != null){
					Button bt1 = new Button();
					buttons.add(bt1);
					bt1.setType("phone_number");
					bt1.setTitle("sms");
					bt1.setPayload("+39" + chatContext.taxi.get(i).getSms());
				}
				i++;
				return buttons;
			}
		}
		return buttons;
	}
		
	/**
	 * Returns object of type clazz from an json api link
	 * 
	 * @param link
	 * @param clazz
	 * @return
	 * @throws Exception
	 */
	private <T> T getObjectFromUrl(String link, Class<T> clazz) {
		T t = null;
		URL url;
		String jsonString = "";
		try {
			url = new URL(link);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					url.openStream()));

			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				jsonString = jsonString + inputLine;
			}
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!StringUtils.isEmpty(jsonString)) {
			Gson gson = new Gson();
			t = gson.fromJson(jsonString, clazz);
		}
		return t;
	}
}
