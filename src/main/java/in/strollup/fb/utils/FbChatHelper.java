package in.strollup.fb.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import com.google.gson.Gson;

import eu.trentorise.smartcampus.mobilityservice.model.TaxiContact;
import eu.trentorise.smartcampus.mobilityservice.model.TimeTable;
import it.sayservice.platform.smartplanner.data.message.otpbeans.Parking;


import in.strollup.fb.contract.Attachment;
import in.strollup.fb.contract.Button;
import in.strollup.fb.contract.ServerData;
import in.strollup.fb.contract.Element;
import in.strollup.fb.contract.Message;
import in.strollup.fb.contract.Messaging;
import in.strollup.fb.contract.Payload;
import in.strollup.fb.contract.QuickReply;
import in.strollup.fb.contract.Recipient;
import in.strollup.fb.servlet.WebHookServlet;
import in.strollup.fb.contract.MongoDBJDBC;
import in.strollup.fb.contract.ChatContext;

/**
 * A Utility class which replies to fb messgaes (or postbacks).
 * 
 * @author dlico & Ayoub50
 *
 */
public class FbChatHelper {
	private static List<TitleSubTitle> option;
	private static List<Message> messaggiQuick;
	private static List<String> menuVoice, busMonodirezionali;
	MongoDBJDBC mongoDBJDBC;
	
	private static List<TitleSubTitle> optiontaxi,optionparcheggi,optionparcheggibici;
	private static List<Message> messaggiQuickTreni;

	private static String profileLink = "https://graph.facebook.com/v2.6/SENDER_ID?access_token=" + WebHookServlet.PAGE_TOKEN;
	
	public FbChatHelper(MongoDBJDBC mongoDBJDBC) {
		this.mongoDBJDBC = mongoDBJDBC;
		
		option = new ArrayList<>();
		messaggiQuick = new ArrayList<>();
		busMonodirezionali = new ArrayList<>();
		menuVoice = new ArrayList<>();
		
		optiontaxi = new ArrayList<>();
		optionparcheggi=new ArrayList<>();
		optionparcheggibici=new ArrayList<>();
		messaggiQuickTreni = new ArrayList<>();
		
		//ArrayList che contiene i bus che hanno solo l'andata
		busMonodirezionali.add("A");
		busMonodirezionali.add("B");
		busMonodirezionali.add("NP");
		busMonodirezionali.add("2");
		
		//ArrayList che contiene tutti i postback del menù
		menuVoice.add("MENU_TAXI");
		menuVoice.add("MENU_AUTOBUS");
		menuVoice.add("MENU_TRENI");
		menuVoice.add("MENU_PARCHEGGI");
		menuVoice.add("MENU_BICI");
		
		//QuickReplies per autobus
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
		
		//Schede menu
		TitleSubTitle MENU_AUTOBUS = new TitleSubTitle();
		MENU_AUTOBUS.setTitle("Vuoi accedere alla sezione autobus o funivia?");
		option.add(MENU_AUTOBUS);

		//QuickReplies per i treni
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
	 * Inizializza le schede per i parcheggi
	 * @param parcheggi
	 */
	private void setParcheggi(List<Parking> parcheggi){
		for(int i = 0; i < parcheggi.size(); i++){
			TitleSubTitle PARCHEGGI = new TitleSubTitle();
			PARCHEGGI.setTitle(parcheggi.get(i).getName()+": "+	parcheggi.get(i).getDescription());
			
			if(parcheggi.get(i).getSlotsAvailable()==-2)
				PARCHEGGI.setSubTitle("posti totali: " +	parcheggi.get(i).getSlotsTotal());
			else
				PARCHEGGI.setSubTitle("posti diponibili: " + parcheggi.get(i).getSlotsAvailable() + "    posti totali: " + parcheggi.get(i).getSlotsTotal());
			
			optionparcheggi.add(PARCHEGGI);
		}
	}
	
	/**
	 * Inizializza le schede per i taxi
	 * @param taxi
	 */
	private void setTaxi(List<TaxiContact> taxi){
		for(int i = 0; i < taxi.size(); i++){
			TitleSubTitle TAXI = new TitleSubTitle();
			TAXI.setTitle(taxi.get(i).getName());
			optiontaxi.add(TAXI);
		}
	}
	
	/**
	 * Inizializza le schede per i parcheggi delle bici condivise
	 * @param parcheggiBici
	 */
	private void setParcheggiBici(List<Parking> parcheggiBici){
		for(int i = 0; i < parcheggiBici.size(); i++){
			TitleSubTitle PARCHEGGI =new TitleSubTitle();
			PARCHEGGI.setTitle(parcheggiBici.get(i).getName() + ": " + parcheggiBici.get(i).getDescription());
			PARCHEGGI.setSubTitle("Biciclette: " + parcheggiBici.get(i).getSlotsAvailable() + "    posti liberi: " + parcheggiBici.get(i).getSlotsTotal());
			optionparcheggibici.add(PARCHEGGI);
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
		
		//se viene selezionato il pulsante di avvio stampa un messagio di benvenuto
		if(text.equals("START")){
			chatContext.setContext("generale");
			postbackReplies.add(makeMessage(senderId, "Benvenuto al bot di ViaggiaTrento, usa il menù laterale per accedere a tutte le funzioni"));
		}
		
		//se il text equivale ad una delle voci del menù allora setto il contesto uguale a text, inizializzo i parametri nescessari al contesto selezionato e in caso il contesto fosse MENU_AUTOBUS_1 stampo una scheda specifica
		if(menuVoice.contains(text)){
			chatContext.setContext(text);
			menuChoise(chatContext, text);
			
			if(chatContext.getContext().equals("MENU_AUTOBUS_1"))
				postbackReplies.add(makeCards(senderId, getCardsMenu(0, chatContext)));	
		}
		
		//stampa delle quikreplies che chiedono di scegliere la direzione
		if(text.equals("MENU_FUNIVIA")){
			chatContext.setContext("MENU_FUNIVIA");
			postbackReplies.add(makeQuick(senderId, getQuickMessage(3, chatContext)));
			chatContext.setContext("MENU_FUNIVIA_1");
		}
		
		//Chiede di scegliere un bus
		if(text.equals("MENU_AUTOBUS_1")){	
			postbackReplies.add(makeMessage(senderId, "Scegli il bus di cui vuoi sapere gli orari: (1-17)"));
		}
		
		//stampa le card relative ai taxi disponibili
		if(chatContext.getContext().equals("MENU_TAXI_1")){
			postbackReplies.add(makeCards(senderId, getTaxi(chatContext)));
			
			postbackReplies.add(makeMessage(senderId, "Seleziona una voce del menù per scegliere che servizio utilizzare"));
		}
		
		//stampa le card relative ai parcheggi disponibili
		if(chatContext.getContext().equals("MENU_PARCHEGGI_1")){
			for(; chatContext.getIndex() < chatContext.getParcheggi().size(); chatContext.setIndex(chatContext.getIndex()+1))
				postbackReplies.add(makeCards(senderId, getParcheggi(chatContext)));
			
			postbackReplies.add(makeMessage(senderId, "Seleziona una voce del menù per scegliere che servizio utilizzare"));
		}
		
		//stampa le card relative ai parcheggi delle bici condivise disponibili
		if(chatContext.getContext().equals("MENU_BICI_1")){
			for(; chatContext.getIndex() < chatContext.getParcheggiBici().size(); chatContext.setIndex(chatContext.getIndex()+1))
				postbackReplies.add(makeCards(senderId, getParcheggiBici(chatContext)));
			
			postbackReplies.add(makeMessage(senderId, "Seleziona una voce del menù per scegliere che servizio utilizzare"));
		}
		
		//stampa delle quikreplies che chiedono di scegliere la linea e la direzione
		if(chatContext.getContext().equals("MENU_TRENI_1")){
			postbackReplies.add(makeQuick(senderId, getTreni(chatContext)));
			chatContext.setContext("MENU_TRENI_2");
		}
		
		updateChatContext(chatContext);
		
		return postbackReplies;
	}

	/**
	 * Restituisce l'Id del treno
	 */
	public void trainId(String senderId, String text, ChatContext chatContext){
		switch(text){
			case "Bolzano - Verona":{
				chatContext.setTrainID("BV_R1_G");
				break;
			}
			case "Verona - Bolzano":{
				chatContext.setTrainID("BV_R1_R");
				break;
			}
			case "Trento - Bassano.d.G":{
				chatContext.setTrainID("TB_R2_G");
				break;
			}
			case "Bassano.d.G - Trento":{
				chatContext.setTrainID("TB_R2_R");
				break;
			}
			case "Trento - Malè":{
				chatContext.setTrainID("555");
				break;
			}
			case "Malè - Trento":{
				chatContext.setTrainID("556");
				break;
			}
			default:{
				chatContext.setTrainID("ERROR");
			}
		}
	}
	
	private ChatContext getChatContext(String senderId) {
		ChatContext chatContext = mongoDBJDBC.getUserContext(senderId);
		if (chatContext == null) {
			ChatContext context = new ChatContext();
			context.setUserId(senderId);
			chatContext = mongoDBJDBC.createContext(context);
		}
		
		return chatContext;
	}

	private void updateChatContext(ChatContext context) {
		mongoDBJDBC.replace(context);
	}


	
	/**
	 * Inizializza il contesto selezionato dall'utente
	 * @param chatContext
	 * @param text
	 */
	public void menuChoise(ChatContext chatContext, String text){	
		switch(text)
		{
			case "MENU_TAXI":{
				chatContext.setIndex(0);
				
				try {
					chatContext.setTaxi(ServerData.getTaxiContacts()); 
				} 
				catch (ExecutionException e) {	
					e.printStackTrace();
				}
				
				setTaxi(chatContext.getTaxi());
				chatContext.setContext("MENU_TAXI_1");
				break;
			}
			case "MENU_AUTOBUS":{
				chatContext.setIndex(0);
				chatContext.setContext("MENU_AUTOBUS_1");
				break;
			}
			case "MENU_TRENI":{
				chatContext.setIndex(0);
				chatContext.setContext("MENU_TRENI_1");
				break;
			}
			case "MENU_PARCHEGGI":{
				chatContext.setIndex(0);
				
				try {
					chatContext.setParcheggi(ServerData.getParkings());
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				
				setParcheggi(chatContext.getParcheggi());
				chatContext.setContext("MENU_PARCHEGGI_1");
				break;
			}
			case "MENU_BICI":{
				chatContext.setIndex(0);
				
				try {
					chatContext.setParcheggiBici(ServerData.getBikeSharings());
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				
				setParcheggiBici(chatContext.getParcheggiBici());
				chatContext.setContext("MENU_BICI_1");
				break;
			}
			default:{}
		}
		updateChatContext(chatContext);
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
		
		//Se l'utente manda un messaggio prima di aver scelto qualcosa dal menu genera un messaggio di errore
		if(chatContext.getContext().equals("generale")){
			replies.add(makeMessage(senderId, "Comando non riconosciuto"));	
		}
		
		//dopo aver scelto la fascia oraria stampa gli orari
		if(chatContext.getContext().equals("MENU_FUNIVIA_2")){
			chatContext.setFascia(text);
			replies.add(makeMessage(senderId, getOrariFunivia(senderId, chatContext, text)));
			
			replies.add(makeMessage(senderId, "Seleziona una voce del menù per scegliere che servizio utilizzare"));
			chatContext.setContext("MENU_FUNIVIA_3");
		}
		
		//dopo aver scelto la direzione stampa della quickReplies che chiedono di scegliere una fascia oraria
		if(chatContext.getContext().equals("MENU_FUNIVIA_1")){
			if(text.equals("Trento - Sardagna"))
				chatContext.setDirezione(true);
			else
				chatContext.setDirezione(false);
			
			getFuniviaID(chatContext);
			
			replies.add(makeQuick(senderId, getQuickMessage(1, chatContext)));
			chatContext.setContext("MENU_FUNIVIA_2");
		}
		
		//Stampo gli orari controllando che non sia necessario stampare altre quickReplies(in caso le fermate fossero più di 10) 
		if(chatContext.getContext().equals("MENU_AUTOBUS_5") || ((((!(text.equals("Altro.."))) && chatContext.getIndex() > 0)) && (chatContext.getContext().equals("MENU_AUTOBUS_4")))){
			replies.add(makeMessage(senderId, getOrariAutobus(senderId, chatContext, text)));
			
			replies.add(makeMessage(senderId, "Seleziona una voce del menù per scegliere che servizio utilizzare"));
			chatContext.setContext("MENU_AUTOBUS_6");
		}
		
		//Cerco nell'elenco di fermate del bus quelle in cui è contenuta la parola  inserita precedentemente e stampo ogni fermata in una quikReply, in caso non ce ne fossero stampo un messaggio di errore
		if(chatContext.getContext().equals("MENU_AUTOBUS_4")){
			if(chatContext.getIndex() == 0)
				chatContext.setText(text);
			
			stops = stopsSearcher(chatContext);

			if(!(stops.size() == 0))	
				replies.add(makeQuick(senderId, getQuickMessage(2, chatContext)));
			else
				replies.add(makeMessage(senderId, "Non ho trovato nessun risultato"));
		}
		
		//Salvo la fascia desiderata e chuiedo per che fermata si vogliono sapere gli orari
		if(chatContext.getContext().equals("MENU_AUTOBUS_3")){
			chatContext.setFascia(text);
			replies.add(makeMessage(senderId, "Di che fermata desideri sapere gli orari?"));
		
			chatContext.setContext("MENU_AUTOBUS_4");
		}
		
		//Una volta scelta la direzione la salvo e creo l'oggetto autobus, chiedo per che fascia oraria stampare gli orari ed in caso in bus fosse monodirezionale salto entro direttamente senza fare il punto precedente 
		if(chatContext.getContext().equals("MENU_AUTOBUS_2") || busMonodirezionali.contains(text)){
			if(busMonodirezionali.contains(text)){
				chatContext.setBusID(text); 
				chatContext.setContext("MENU_AUTOBUS_2");
			}	
			else{
				if(text.equals("Andata"))
					chatContext.setDirezione(true); 
				else
					chatContext.setDirezione(false); 
			}
			
			try {
				String routeId = ServerData.getAutobusRouteId(chatContext.getbusID(), chatContext.getDirezione());
				chatContext.setAutobus(ServerData.getAutobusTimetable(routeId));
			} catch (ExecutionException e) {
				
				e.printStackTrace();
			}
			
			replies.add(makeQuick(senderId, getQuickMessage(1, chatContext)));
			chatContext.setContext("MENU_AUTOBUS_3"); 	
		}	
		
		//Salva il bus scelto e stampa delle Quick che chiedono di scegliere la direzione
		if(chatContext.getContext().equals("MENU_AUTOBUS_1")){
			chatContext.setBusID(text); 	
			replies.add(makeQuick(senderId, getQuickMessage(0, chatContext)));	
			chatContext.setContext("MENU_AUTOBUS_2"); 
		}
		
		//Stampo gli orari controllando che non sia necessario stampare altre quickReplies(in caso le fermate fossero più di 10) 
		if(chatContext.getContext().equals("MENU_TRENI_5") || ((((!(text.equals("Altro.."))) && chatContext.getIndex() > 0)) && (chatContext.getContext().equals("MENU_TRENI_4")))){
			replies.add(makeMessage(senderId, getOrariTreni(senderId, chatContext, text)));
			
			replies.add(makeMessage(senderId, "Seleziona una voce del menù per scegliere che servizio utilizzare"));
			chatContext.setContext("MENU_AUTOBUS_6");
		}
		
		//Cerco nell'elenco di fermate del treno quelle in cui è contenuta la parola  inserita precedentemente e stampo ogni fermata in una quikReply, in caso non ce ne fossero stampo un messaggio di errore
		if(chatContext.getContext().equals("MENU_TRENI_4")){
			if(chatContext.getIndex() == 0)
				chatContext.setText(text);
			
			stops = stopsSearcher(chatContext);

			if(!(stops.size() == 0))	
				replies.add(makeQuick(senderId, getQuickMessage(2, chatContext)));
			else
				replies.add(makeMessage(senderId, "Non ho trovato nessun risultato"));
		}
		
		//Salvo la fascia desiderata e chuiedo per che fermata si vogliono sapere gli orari
		if(chatContext.getContext().equals("MENU_TRENI_3")){
			chatContext.setFascia(text);
			replies.add(makeMessage(senderId, "Di che fermata desideri sapere gli orari?"));
			
			chatContext.setContext("MENU_TRENI_4"); 
		}
		
		//Trovo l'ID del treno, creo l'oggetto treni e stampo delle QuickRplies in cui chiedo di scegliere una fascia oraria
		if(chatContext.getContext().equals("MENU_TRENI_2")){
			trainId(senderId, text, chatContext);
			try {
				chatContext.setTreni( ServerData.getTrainTimetable(chatContext.getTrainID()));
			} catch (ExecutionException e) {
				
				e.printStackTrace();
			}
			replies.add(makeQuick(senderId, getQuickMessage(1, chatContext)));
			chatContext.setContext("MENU_TRENI_3");
		}
		
		updateChatContext(chatContext);
		
		return replies;
	}

	/**
	 * In base alla direzione assegno l'ID alla funivia
	 * @param chatContext
	 */
	public void getFuniviaID(ChatContext chatContext){		
		if(chatContext.getDirezione())
			chatContext.setFuniviaID("FunA");
		else
			chatContext.setFuniviaID("FunR");	
	}
	
	
	
	/**
	 * Stampa gli orari in base alla fascia scelta e ritorna un messaggio di errore in caso non ci fossero risultati
	 * @param senderId
	 * @param mezzo
	 * @param inizioFascia
	 * @param fineFascia
	 * @param text
	 * @return
	 */
	public String timePrinter(String senderId, TimeTable mezzo, int inizioFascia, int fineFascia, String text){
		String messaggio = "";
		String subString = "";
		
		int j = findStopInList(mezzo, text);
		
		if(inizioFascia != 22){
			for(int i = 0; i < mezzo.getTimes().size(); i++){
				if(!mezzo.getTimes().get(i).get(j).isEmpty()){
					subString = mezzo.getTimes().get(i).get(j).substring(0,2);
					
					if(Integer.parseInt(subString) >= inizioFascia && Integer.parseInt(subString) < fineFascia)
						messaggio += "  " + mezzo.getTimes().get(i).get(j);
				}
			}
		}
		else{
			for(int i = 0; i < mezzo.getTimes().size(); i++){
				if(!mezzo.getTimes().get(i).get(j).isEmpty()){
					subString = mezzo.getTimes().get(i).get(j).substring(0,2);
					
					if((Integer.parseInt(subString) >= 22 || (Integer.parseInt(subString) >= 0 && Integer.parseInt(subString) < 1)))
						messaggio += "  " + mezzo.getTimes().get(i).get(j);
				}
			}
		}
		
		if(messaggio.equals(""))
			messaggio = "Non ci sono autobus disponibili";
		
		return messaggio;
	}
	
	/**
	 * Cerco tutte le fermate che contengono il testo inserito dall'utente
	 * @param chatcontext
	 * @return
	 */
	private List<String> stopsSearcher(ChatContext chatcontext){
		List<String> stops = new ArrayList<>();

		if(chatcontext.getContext().equals("MENU_AUTOBUS_4")){
			for(int i = 0; i < chatcontext.getAutobus().getStops().size(); i++){
				if(chatcontext.getAutobus().getStops().get(i).toLowerCase().contains(chatcontext.getText().toLowerCase()))
					stops.add(chatcontext.getAutobus().getStops().get(i));
			}
		} else if(chatcontext.getContext().equals("MENU_TRENI_4")){
			for(int i = 0; i < chatcontext.getTreni().getStops().size(); i++){
				if(chatcontext.getTreni().getStops().get(i).toLowerCase().contains(chatcontext.getText().toLowerCase()))
					stops.add(chatcontext.getTreni().getStops().get(i));
			}
		}
		
		return stops;
	}
	
	/**
	 * Ritorna gli orari della funivia
	 * @param senderId
	 * @param chatContext
	 * @param text
	 * @return
	 */
	private String getOrariFunivia(String senderId, ChatContext chatContext, String text){
		String msg = "Questa funivia non esiste";
		
		try {
			chatContext.setAutobus(ServerData.getAutobusTimetable(chatContext.getFuniviaID()));
		} catch (ExecutionException e) {
			
			e.printStackTrace();
		}
		msg = timePrinter(senderId, chatContext.getAutobus(), Integer.parseInt(chatContext.getFascia().substring(0, 2)), Integer.parseInt(chatContext.getFascia().substring(3, 5)), text);
		
		return msg;
	}
	
	/**
	 * Ritorna gli orari dell'autobus
	 * @param senderId
	 * @param chatContext
	 * @param text
	 * @return
	 */
	private String getOrariAutobus(String senderId, ChatContext chatContext, String text){
		String msg = "Questo autobus non esiste (in caso di autobus  \"/\" digitare solo il bus di riferimento)";
		
		try {
			String routeId = ServerData.getAutobusRouteId(chatContext.getbusID(), chatContext.getDirezione());
			chatContext.setAutobus(ServerData.getAutobusTimetable(routeId));
		} catch (ExecutionException e) {
			
			e.printStackTrace();
		}
		msg = timePrinter(senderId, chatContext.getAutobus(), Integer.parseInt(chatContext.getFascia().substring(0, 2)), Integer.parseInt(chatContext.getFascia().substring(3, 5)), text);
		
		return msg;
	}
	
	/**
	 * Ritorna gli orari del treno
	 * @param senderId
	 * @param chatContext
	 * @param text
	 * @return
	 */
	private String getOrariTreni(String senderId, ChatContext chatContext, String text){
		String msg = "Questo treno non esiste";
		
		try {
			chatContext.setAutobus(ServerData.getTrainTimetable(chatContext.getTrainID())); 
		} catch (ExecutionException e) {
			
			e.printStackTrace();
		}
		msg = timePrinter(senderId, chatContext.getTreni(), Integer.parseInt(chatContext.getFascia().substring(0, 2)), Integer.parseInt(chatContext.getFascia().substring(3, 5)), text);
		
		return msg;
	}
	
	/**
	 * Tramite il nome della fermata restituisco l'indice della sua posizione all'interno della lista delle fermate
	 * @param mezzo
	 * @param text
	 * @return
	 */
	private int findStopInList(TimeTable mezzo, String text){
		boolean find = false;
		int i = 0;
		
		for(; i < mezzo.getStops().size() && !find; i++){
			if(mezzo.getStops().get(i).contains(text)){
				find = true;
			}
		}
	
		return i - 1;
	}
	
	/**
	 * Ritorna una stringa che permette di formare il messaggio Json
	 * @param senderId
	 * @param msg
	 * @return
	 */
	private String makeMessage(String senderId, String msg){
		Message fbMsg = getMsg(msg);
		String fbReply = getJsonReply(senderId, fbMsg);
		
		return fbReply;
	}
	
	/**
	 * Ritorna una stringa che permette di formare il messaggio Json
	 * @param senderId
	 * @param cards
	 * @return
	 */
	private String makeCards(String senderId, Message cards){
		Message fbMsg = cards;
		String fbReply = getJsonReply(senderId, fbMsg);
		
		return fbReply;
	}
	
	/**
	 * Ritorna una stringa che permette di formare il messaggio Json
	 * @param senderId
	 * @param quick
	 * @return
	 */
	private String makeQuick(String senderId, Message quick){
		Message fbMsg = quick;
		String fbReply = getJsonReply(senderId, fbMsg);
		
		return fbReply;
	}
	
	/**
	 * Ritorna un messaggio partendo da una stringa
	 * @param msg
	 * @return
	 */
	private Message getMsg(String msg) {
		Message message = new Message();
		message.setText(msg);
		return message;
	}
	
	/**
	 * Aggiunge al Messaggio un attachment contenente il payload
	 * @param payload
	 * @return
	 */
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
	
	/**
	 * Restituisce un messaggio contenente delle quickReplies
	 * @param i
	 * @param chatContext
	 * @return
	 */
	private Message getQuickMessage(int i, ChatContext chatContext) {
		List<QuickReply> quickReplies = getQuick(chatContext);
		Message message = messaggiQuick.get(i);
		message.setQuickReplies(quickReplies);
		
		return message;
	}
	
	/**
	 * Restituisce una lista di quickReplies oraganizzate in una lista
	 * @param chatContext
	 * @return
	 */
	private List<QuickReply> getQuick(ChatContext chatContext) {	
 		List<QuickReply> quickReplies =  new ArrayList<>();
 		
 		QuickReply quickReply = new QuickReply();
 		quickReplies.add(quickReply);
		
		List<QuickReply> quickRepliesOption = getQuickReplies(chatContext);
		quickReply.setQuickReplies(quickRepliesOption);

		return quickRepliesOption;		
	}
	
	/**
	 * Retorna una lista di quickReplies in base al contesto
	 * @param chatContext
	 * @return
	 */
	private List<QuickReply> getQuickReplies(ChatContext chatContext){
		List<QuickReply> quick = new ArrayList<>();
		
		if(chatContext.getContext().equals("MENU_AUTOBUS_1")){
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
		
		if(chatContext.getContext().equals("MENU_AUTOBUS_2") || chatContext.getContext().equals("MENU_TRENI_2") || chatContext.getContext().equals("MENU_FUNIVIA_1")){
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
		
		if(chatContext.getContext().equals("MENU_AUTOBUS_4") || chatContext.getContext().equals("MENU_TRENI_4")){
			List<String> stops = new ArrayList<String>();
			
			stops = stopsSearcher(chatContext);
	
			for(int i = 0; i < 9 && chatContext.getIndex() < stops.size(); chatContext.setIndex(chatContext.getIndex() + 1), i++){
				String title = stops.get(chatContext.getIndex());
				
				if(title.length() >= 20)
					title = stringCut(title);
				
				QuickReply stop = new QuickReply();
				quick.add(stop);
				stop.setType("text");
				stop.setTitle(title);
				stop.setPayload(title);
			}
			
			if(chatContext.getIndex() < stops.size() && chatContext.getIndex() < stops.size()){
				QuickReply altro = new QuickReply();
				quick.add(altro);
				altro.setType("text");
				altro.setTitle("Altro..");
				altro.setPayload("Altro..");
			}
			else {
				if(chatContext.getContext().equals("MENU_AUTOBUS_4"))
					chatContext.setContext("MENU_AUTOBUS_5"); 
				else
					chatContext.setContext("MENU_TRENI_5");
			}
		}
			
		if(chatContext.getContext().equals("MENU_TRENI_1")){
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
		
		if(chatContext.getContext().equals("MENU_FUNIVIA")){
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

	/**
	 * Ritorna le prime 20 lettere di una stringa
	 * @param title
	 * @return
	 */
	private String stringCut(String title){
		title = title.substring(0, 20);
		
		return title;
	}
	
	/**
	 * Ritorna un messaggio con quickReplies specifiche
	 * @param chatContext
	 * @return
	 */
	private Message getTaxi(ChatContext chatContext) {
		List<Element> elements = getElementsTaxi(chatContext);
		
		Payload payload = new Payload();
		
		payload.setElements(elements);
		payload.setTemplateType("generic");

		return getQuestion(payload);
	}
	
	/**
	 * Ritorna un messaggio con quickReplies specifiche
	 * @param chatContext
	 * @return
	 */
	private Message getTreni(ChatContext chatContext) {
		List<QuickReply> quickReplies = getQuick(chatContext);
		Message message = messaggiQuickTreni.get(0);
		message.setQuickReplies(quickReplies);
		
		return message;
	}
	
	/**
	 * Ritorna un messaggio con quickReplies specifiche
	 * @param chatContext
	 * @return
	 */
	private Message getParcheggi(ChatContext chatContext) {
		List<Element> elements = getElementsParcheggi(chatContext);
		Payload payload = new Payload();
		payload.setElements(elements);
		payload.setTemplateType("generic");

		return getQuestion(payload);
	}
	
	/**
	 * Ritorna un messaggio con quickReplies specifiche
	 * @param chatContext
	 * @return
	 */
	private Message getParcheggiBici(ChatContext chatContext) {
		List<Element> elements = getElementsParcheggiBici(chatContext);
		Payload payload = new Payload();
		payload.setElements(elements);
		payload.setTemplateType("generic");

		return getQuestion(payload);
	}
	
	/**
	 * Ritorna una lista di schede
	 * @param chatContext
	 * @return
	 */
	private List<Element> getElementsTaxi(ChatContext chatContext) {
		List<Element> elements = new ArrayList<>();
		
			for(; chatContext.getIndex() < chatContext.getTaxi().size(); chatContext.setIndex(chatContext.getIndex() + 1)){
				TitleSubTitle Taxi1 = optiontaxi.get(chatContext.getIndex());
				Element element = new Element();
				
				elements.add(element);
				List<Button> buttons = getButtons(chatContext);
				
				element.setButtons(buttons);
				element.setTitle(Taxi1.title);
				element.setSubtitle(Taxi1.subTitle);
			}
			return elements;
	}
	
	/**
	 * Ritorna una lista di schede
	 * @param chatContext
	 * @return
	 */
	private List<Element> getElementsParcheggi(ChatContext chatContext) {
			List<Element> elements = new ArrayList<>();	
			TitleSubTitle Parcheggi = optionparcheggi.get(chatContext.getIndex());
			
			Element element = new Element();
			
			elements.add(element);
			element.setTitle(Parcheggi.title);
			element.setSubtitle(Parcheggi.subTitle);
			
		return elements;
	}
	
	/**
	 * Ritorna una lista di schede
	 * @param chatContext
	 * @return
	 */
	private List<Element> getElementsParcheggiBici(ChatContext chatContext) {
			List<Element> elements = new ArrayList<>();	
			TitleSubTitle Parcheggibici = optionparcheggibici.get(chatContext.getIndex());
			
			Element element = new Element();
			
			elements.add(element);
			element.setTitle(Parcheggibici.title);
			element.setSubtitle(Parcheggibici.subTitle);
	
			return elements;
	}
	
	/**
	 * Ritorna un messaggio contenente delle schede con bottoni
	 * @param i
	 * @param chatContext
	 * @return
	 */
	private Message getCardsMenu(int i, ChatContext chatContext) {
		List<Element> elements = getElementsMenu(i, chatContext);
		Payload payload = new Payload();
		payload.setElements(elements);
		payload.setTemplateType("generic");
	
		return getQuestion(payload);
	}

	/**
	 * Aggiunge i bottoni ad una determinata scheeda
	 * @param i
	 * @param chatContext
	 * @return
	 */
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
	
	/**
	 * Ritorna dei bottoni in base al contesto
	 * @param chatContext
	 * @return
	 */
	private List<Button> getButtons(ChatContext chatContext) {
		List<Button> buttons = new ArrayList<>();

		if(chatContext.getContext() == "MENU_AUTOBUS_1"){
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
		
		if(chatContext.getContext() == "MENU_TAXI_1"){
			int i = chatContext.getIndex();
			while(i < chatContext.getTaxi().size()){
				if(chatContext.getTaxi().get(i).getPhone() != null){
					if(!chatContext.getTaxi().get(i).getPhone().get(i).isEmpty() && !chatContext.getTaxi().get(i).getSms().isEmpty()){
						Button bt1 = new Button();
						buttons.add(bt1);
						bt1.setType("phone_number");
						bt1.setTitle(chatContext.getTaxi().get(i).getPhone().get(i).toString());
						bt1.setPayload("+39" + chatContext.getTaxi().get(i).getPhone().get(i));
						Button bt3 = new Button();
						buttons.add(bt3);
						bt3.setType("phone_number");
						bt3.setTitle("sms");
						bt3.setPayload("+39" + chatContext.getTaxi().get(i).getSms());
						}
					else if(chatContext.getTaxi().get(i).getPhone().get(1) != null && chatContext.getTaxi().get(i).getSms().isEmpty()){
						Button bt1 = new Button();
						buttons.add(bt1);
						bt1.setType("phone_number");
						bt1.setTitle(chatContext.getTaxi().get(i).getPhone().get(0).toString());
						bt1.setPayload("+39" + chatContext.getTaxi().get(i).getPhone());
						Button bt2 = new Button();
						buttons.add(bt2);
						bt2.setType("phone_number");
						bt2.setTitle(chatContext.getTaxi().get(i).getPhone().get(1).toString());
						bt2.setPayload("+39" + chatContext.getTaxi().get(i).getPhone().get(1));
						}
					else if(chatContext.getTaxi().get(i).getPhone().get(1).isEmpty() && chatContext.getTaxi().get(i).getSms() != null){
						Button bt1 = new Button();
						buttons.add(bt1);
						bt1.setType("phone_number");
						bt1.setTitle(chatContext.getTaxi().get(i).getPhone().get(i).toString());
						bt1.setPayload("+39" + chatContext.getTaxi().get(i).getPhone());
						Button bt2 = new Button();
						buttons.add(bt2);
						bt2.setType("phone_number");
						bt2.setTitle("sms");
						bt2.setPayload("+39" + chatContext.getTaxi().get(i).getSms());
						}
					else if(chatContext.getTaxi().get(i).getPhone().get(1).isEmpty() && chatContext.getTaxi().get(i).getSms().isEmpty()){
						Button bt1 = new Button();
						buttons.add(bt1);
						bt1.setType("phone_number");
						bt1.setTitle(chatContext.getTaxi().get(i).getPhone().get(i).toString());
						bt1.setPayload("+39" + chatContext.getTaxi().get(i).getPhone());
						}
					}
				else if(chatContext.getTaxi().get(i).getSms() != null){
					Button bt1 = new Button();
					buttons.add(bt1);
					bt1.setType("phone_number");
					bt1.setTitle("sms");
					bt1.setPayload("+39" + chatContext.getTaxi().get(i).getSms());
				}
				i++;
				
				break;
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
			
			e.printStackTrace();
		}
		if (!StringUtils.isEmpty(jsonString)) {
			Gson gson = new Gson();
			t = gson.fromJson(jsonString, clazz);
		}
		return t;
	}
}