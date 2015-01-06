package com.synload.framework;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.spdy.server.http.HTTPSPDYServerConnector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.synload.eventsystem.Handler;
import com.synload.framework.handlers.Request;
import com.synload.framework.http.DefaultHTTPPages;
import com.synload.framework.http.HTTPHandler;
import com.synload.framework.http.HTTPResponse;
import com.synload.framework.http.HTTPRouting;
import com.synload.framework.js.Javascript;
import com.synload.framework.menu.MenuItem;
import com.synload.framework.modules.ModuleLoader;
import com.synload.framework.modules.ModuleLoader.TYPE;
import com.synload.framework.sql.SQLRegistry;
import com.synload.framework.ws.DefaultWSPages;
import com.synload.framework.ws.WSHandler;
import com.synload.framework.ws.WSRequest;
import com.synload.framework.ws.WSResponse;
import com.synload.framework.ws.WSRouting;
import com.synload.framework.ws.WebsocketHandler;

public class SynloadFramework{
	public SynloadFramework(){}
	public static HashMap<String,HashMap<String,Object>> htmlFiles = new HashMap<String,HashMap<String,Object>>();
	public static List<Session> users = new ArrayList<Session>();
	//public static Map<String,DashboardGroup> dashboardGroups = new HashMap<String,DashboardGroup>();
	public static List<Object> plugins = new ArrayList<Object>();
	public static List<String> bannedIPs = new ArrayList<String>();
	public static Connection sql = null;
	public static int totalFailures = 10;
	public static boolean debug = false;
	public static long maxUploadSize = 26214400;
	public static boolean handleUpload = false;
	public static String uploadPath = "uploads/";
	public static boolean siteDefaults = false;
	public static Server server = null;
	public static Properties prop = new Properties();
	public static List<WSHandler> clients = new ArrayList<WSHandler>();
	public static Map<String,List<Long>> failedAttempts = new HashMap<String,List<Long>>();
	public static List<Javascript> javascripts = new ArrayList<Javascript>();
	public static String authKey = "s9V0l3v1GsrE2j50VrUp1Elp1jY4Xh97bNkuHBnOVCL28I"+
			"TyH17u5TRD25UDsRrb2Bny61y1XXv0zZSWq4O9gARzO881amS3lAgy";
	public static List<MenuItem> menus = new ArrayList<MenuItem>();
	public static ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
	public static HashMap<String,MenuItem> referenceMenus = new HashMap<String,MenuItem>();
	public static int port = 80;
	public static Level loglevel = null;
	public static String path = "modules/";
	public static void main(String[] args) {
		Log.info("Starting Synload Development Framework Server",SynloadFramework.class);
		try {
			
			if((new File("config.ini")).exists()){
				prop.load(new FileInputStream("config.ini"));
				authKey = prop.getProperty("authKey");
				port = Integer.valueOf(prop.getProperty("port"));
				handleUpload = Boolean.valueOf(prop.getProperty("handleUploads"));
				siteDefaults = Boolean.valueOf(prop.getProperty("siteDefaults"));
				path = prop.getProperty("modulePath");
			}else{
				prop.setProperty("authKey", authKey);
				prop.setProperty("jdbc", "jdbc:mysql://localhost:3306/db?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true");
				prop.setProperty("dbuser", "root");
				prop.setProperty("dbpass", "pass");
				prop.setProperty("port", "80");
				prop.setProperty("loglevel", "INFO");
				prop.setProperty("modulePath", "modules/");
				prop.setProperty("siteDefaults", "false");
				prop.setProperty("debug", "false");
				prop.setProperty("handleUploads", "false");
				prop.setProperty("maxUploadSize", "26214400");
				prop.setProperty("uploadPath", "uploads/");
				prop.store(new FileOutputStream("config.ini"), null);
			}
			loglevel = Level.toLevel(prop.getProperty("loglevel"));
			debug = Boolean.valueOf(prop.getProperty("debug"));
			uploadPath = prop.getProperty("uploadPath");
			maxUploadSize = Long.valueOf(prop.getProperty("maxUploadSize"));
			
	        sql = DriverManager.getConnection( 
					prop.getProperty("jdbc"), 
					prop.getProperty("dbuser"), 
					prop.getProperty("dbpass") );
	        
			SynloadFramework.buildMenu();
			
			ModuleLoader.register(DefaultWSPages.class, Handler.EVENT, TYPE.METHOD, null);
			
			SynloadFramework.buildDefaultHTTP();
			SynloadFramework.buildJavascript();
			
	        File folder = new File(path);
	        if(!folder.exists()){
	            folder.mkdir();
	        }
	        ModuleLoader.load(path);

	        SQLRegistry.checkVersions();
	        
			if(args.length>=1){
				port = Integer.valueOf(args[0]);
			}
			
			LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(150);
			ExecutorThreadPool pool = new ExecutorThreadPool(50, 200, 10, TimeUnit.MILLISECONDS, queue);
			server = new Server(pool);
			HTTPSPDYServerConnector connector = new HTTPSPDYServerConnector(server) ;
			connector.setPort(port);
			Connector[] g = new Connector[]{connector};
			server.setConnectors(g);
			HandlerCollection handlerCollection = new HandlerCollection();
			handlerCollection.addHandler(new HTTPHandler());
			handlerCollection.addHandler(new WebsocketHandler());
			server.setHandler(handlerCollection);
			
			Log.info("Loaded all aspects running on port "+port,SynloadFramework.class);
			
			server.start();
			server.join();
			
		} catch (Exception e) {
			if(SynloadFramework.debug){
				e.printStackTrace();
			}
		}
	}
	
	public static String randomString(int length){
		SecureRandom random = new SecureRandom();
	    return new BigInteger(130, random).toString(length);
	}
	
	public static void buildJavascript(){
		Javascript jsUser = new Javascript();
		jsUser.addCallBack("user.msg", "recieve");
		jsUser.addCallBack("user.load", "init");
		jsUser.addCallBack("user.event", "login");
		jsUser.addCallBack("user.event", "logout");
		jsUser.addCallBack("user.event", "session");
		jsUser.addCallBack("user.event", "register");
		jsUser.setScript("/js/user.js");
		SynloadFramework.registerJavascriptFile(jsUser,"User Account System");
		Javascript jsPage = new Javascript();
		jsPage.setScript("/js/page.js");
		SynloadFramework.registerJavascriptFile(jsPage,"Page Handler");
		Javascript jsForm = new Javascript();
		jsForm.setScript("/js/form.js");
		SynloadFramework.registerJavascriptFile(jsForm,"Form Handler");
	}
	
	public static void buildDefaultHTTP(){
		
		SynloadFramework.registerHTTPPage("/", DefaultHTTPPages.class, "getIndex");
		if(handleUpload){
			Log.info("Upload handler enabled!",SynloadFramework.class);
			SynloadFramework.registerHTTPPage("/system/uploads", DefaultHTTPPages.class, "handleUploads");
		}
	}
	
	public static void buildMenu(){
		MenuItem menu = SynloadFramework.createMenu("Account", "account", new Request("", ""), null, "r", 999);
		SynloadFramework.createMenu("Edit Profile", "usersettings", new Request("get", "userSettings"), menu, "r", 7);
		SynloadFramework.registerMenuItem(menu);
		
	}
	
	public static void registerMenuItem(MenuItem mi){
		SynloadFramework.menus.add(mi);
	}
	
	public static MenuItem createMenu(String name, String uname, Request request, MenuItem parent, String flag, Integer priority){
		//System.out.println("[MU] Added "+name+" with parent <"+((parent!=null)?parent.getName():"")+">");
		MenuItem m = new MenuItem();
		m.setName(name);
		m.setUname(uname);
		m.setFlag(flag);
		m.setPriority(priority);
		m.setRequest(request);
		if(parent!=null){
			parent.addMenus(m);
		}
		return m;
	}
	public static void broadcast(String data){
		for(WSHandler user: SynloadFramework.clients){
			user.send(data);
		}
	}
	public static void broadcast(List<WSHandler> _cs, String data){
		for(WSHandler user: _cs){
			user.send(data);
		}
	}
	public static void registerJavascriptFile(Javascript js, String name){
		//System.out.println("[JS] Registered javascript <"+name+">");
		SynloadFramework.javascripts.add(js);
	}
	
	public static void registerHTTPPage(String page, Class<?> listenerClass, String method){
		try {
			if(HTTPRouting.addRoutes(page, new HTTPResponse(listenerClass, method))){
				//System.out.println("[WB] Registered path <"+page+">");
			}else{
				//System.out.println("[WB][E] Failed to add <"+page+"> path");
			}
		} catch (Exception e) {
			if(SynloadFramework.debug){
				e.printStackTrace();
			}
		}
	}
	
	public static void registerElement(WSRequest page, Class<?> listenerClass, String method, List<String> flags){
		try {
			if(WSRouting.addRoutes(page, new WSResponse(listenerClass, method, flags))){
				//System.out.println("[PG] Registered page <"+page.getPri()+"> with action <"+page.getRequest()+">");
			}else{
				//System.out.println("[PG][E] Failed to add <"+page.getPri()+"> with action <"+page.getRequest()+">");
			}
		} catch (JsonProcessingException e) {
			if(SynloadFramework.debug){
				e.printStackTrace();
			}
		}
	}
	public static HashMap<String, HashMap<String, Object>> getHtmlFiles() {
		return htmlFiles;
	}
	public static int getTimestamp(){
		return (int) (System.currentTimeMillis() / 1000L);
	}
	public static void setHtmlFiles(
			HashMap<String, HashMap<String, Object>> htmlFiles) {
		SynloadFramework.htmlFiles = htmlFiles;
	}

	public static List<Object> getPlugins() {
		return plugins;
	}

	public static void setPlugins(List<Object> plugins) {
		SynloadFramework.plugins = plugins;
	}

	public static List<String> getBannedIPs() {
		return bannedIPs;
	}

	public static void setBannedIPs(List<String> bannedIPs) {
		SynloadFramework.bannedIPs = bannedIPs;
	}

	public static boolean isDebug() {
		return debug;
	}

	public static void setDebug(boolean debug) {
		SynloadFramework.debug = debug;
	}

	public static long getMaxUploadSize() {
		return maxUploadSize;
	}

	public static void setMaxUploadSize(long maxUploadSize) {
		SynloadFramework.maxUploadSize = maxUploadSize;
	}

	public static boolean isHandleUpload() {
		return handleUpload;
	}

	public static void setHandleUpload(boolean handleUpload) {
		SynloadFramework.handleUpload = handleUpload;
	}

	public static boolean isSiteDefaults() {
		return siteDefaults;
	}

	public static void setSiteDefaults(boolean siteDefaults) {
		SynloadFramework.siteDefaults = siteDefaults;
	}

	public static Server getServer() {
		return server;
	}

	public static void setServer(Server server) {
		SynloadFramework.server = server;
	}

	public static Properties getProp() {
		return prop;
	}

	public static void setProp(Properties prop) {
		SynloadFramework.prop = prop;
	}

	public static List<WSHandler> getClients() {
		return clients;
	}

	public static void setClients(List<WSHandler> clients) {
		SynloadFramework.clients = clients;
	}

	public static Map<String, List<Long>> getFailedAttempts() {
		return failedAttempts;
	}

	public static void setFailedAttempts(Map<String, List<Long>> failedAttempts) {
		SynloadFramework.failedAttempts = failedAttempts;
	}

	public static List<Javascript> getJavascripts() {
		return javascripts;
	}

	public static void setJavascripts(List<Javascript> javascripts) {
		SynloadFramework.javascripts = javascripts;
	}

	public static String getAuthKey() {
		return authKey;
	}

	public static void setAuthKey(String authKey) {
		SynloadFramework.authKey = authKey;
	}

	public static List<MenuItem> getMenus() {
		return menus;
	}

	public static void setMenus(List<MenuItem> menus) {
		SynloadFramework.menus = menus;
	}

	public static ObjectWriter getOw() {
		return ow;
	}

	public static void setOw(ObjectWriter ow) {
		SynloadFramework.ow = ow;
	}

	public static HashMap<String, MenuItem> getReferenceMenus() {
		return referenceMenus;
	}

	public static void setReferenceMenus(HashMap<String, MenuItem> referenceMenus) {
		SynloadFramework.referenceMenus = referenceMenus;
	}
}