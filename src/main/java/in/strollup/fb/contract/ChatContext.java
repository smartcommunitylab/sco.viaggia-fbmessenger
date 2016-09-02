package in.strollup.fb.contract;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;

import eu.trentorise.smartcampus.mobilityservice.model.TaxiContact;
import eu.trentorise.smartcampus.mobilityservice.model.TimeTable;
import it.sayservice.platform.smartplanner.data.message.otpbeans.Parking;

/**
 * 
 * @author dlico
 *
 */

//classe che permette di avere parametri legati al singolo utente
public class ChatContext {
	
	private String context = "start";
	private String userId;
	private boolean direzione = true;
	private String busID;
	private String trainID;
	private String funiviaID;
	private String text;
	private int index = 0;
	private List<TaxiContact> taxi = new ArrayList<TaxiContact>();
	private TimeTable treni = new TimeTable();
	private TimeTable autobus = new TimeTable();
	private List<Parking> parcheggi = new ArrayList<Parking>();
	private List<Parking> parcheggiBici = new ArrayList<Parking>();
	private String fascia;
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getBusID() {
		return busID;
	}

	public void setContext(String context){
		this.context = context;
	}
	
	public void setDirezione(boolean direzione){
		this.direzione = direzione;
	}
	
	public void setBusID(String busID){
		this.busID = busID;
	}
	
	public void setTrainID(String trainID){
		this.trainID = trainID;
	}
	
	public void setFuniviaID(String funiviaID){
		this.funiviaID = funiviaID;
	}
	
	public void setText(String text){
		this.text = text;
	}
	
	public void setIndex(int index){
		this.index = index;
	}
	
	public void setTaxi(List<TaxiContact> taxi){
		this.taxi = taxi;
	}
	
	public void setTreni(TimeTable treni){
		this.treni = treni;
	}
	
	public void setAutobus(TimeTable autobus){
		this.autobus = autobus;
	}
	
	public void setParcheggi(List<Parking> parcheggi){
		this.parcheggi = parcheggi;
	}
	
	public void setParcheggiBici(List<Parking> parcheggiBici){
		this.parcheggiBici = parcheggiBici;
	}
	
	public void setFascia(String fascia){
		this.fascia = fascia;
	}
	
	public String getContext(){
		return context;
	}
	
	public boolean getDirezione(){
		return direzione;
	}
	
	public String getbusID(){
		return busID;
	}
	
	public String getTrainID(){
		return trainID;
	}
	
	public String getFuniviaID(){
		return funiviaID;
	}
	
	public String getText(){
		return text;
	}
	
	public int getIndex(){
		return index;
	}
	
	public List<TaxiContact> getTaxi(){
		return taxi;
	}
	
	public TimeTable getAutobus(){
		return autobus;
	}
	
	public TimeTable getTreni(){
		return treni;
	}
	
	public List<Parking> getParcheggi(){
		return parcheggi;
	}
	
	public List<Parking> getParcheggiBici(){
		return parcheggiBici;
	}
	
	public String getFascia(){
		return fascia;
	}
}
