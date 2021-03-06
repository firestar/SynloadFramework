package com.synload.framework;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.synload.talksystem.Client;
import com.synload.talksystem.eventShare.EventShare;
import com.synload.talksystem.statistics.Statistics;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.spdy.server.http.HTTPSPDYServerConnector;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.synload.framework.http.DefaultHTTPPages;
import com.synload.framework.http.HTTPHandler;
import com.synload.framework.http.HTTPRouting;
import com.synload.framework.http.modules.HTTPResponse;
import com.synload.framework.js.Javascript;
import com.synload.framework.modules.ModuleClass;
import com.synload.framework.modules.ModuleLoader;
import com.synload.framework.modules.ModuleRegistry;
import com.synload.framework.modules.annotations.Module;
import com.synload.framework.modules.annotations.Module.LogLevel;
import com.synload.framework.sql.SQLRegistry;
import com.synload.framework.ws.WSHandler;
import com.synload.framework.ws.WSRequest;
import com.synload.framework.ws.WSResponse;
import com.synload.framework.ws.WSRouting;
import com.synload.framework.ws.WebsocketHandler;
import com.synload.talksystem.ServerTalk;

@Module(author="Nathaniel Davidson", name="SynloadFramework", version="1.4.8.1", depend = { "" }, log = LogLevel.INFO)
public class SynloadFramework extends ModuleClass {
    public SynloadFramework() {
    }
    public static String version="1.4.8.1";
    public static HashMap<String, HashMap<String, Object>> htmlFiles = new HashMap<String, HashMap<String, Object>>();
    public static List<Session> users = new ArrayList<Session>();
    // public static Map<String,DashboardGroup> dashboardGroups = new
    // HashMap<String,DashboardGroup>();
    public static List<ModuleClass> plugins = new ArrayList<ModuleClass>();
    public static List<String> bannedIPs = new ArrayList<String>();
    public static Connection sql = null;
    public static int totalFailures = 10;
    public static String serverTalkKey;
    public static boolean debug = false;
    public static long maxUploadSize = 26214400;
    public static boolean handleUpload = false;
    public static String uploadPath = "uploads/";
    public static boolean siteDefaults = false;
    public static boolean graphDBEnable = false;
    public static String graphDBPath = "";
    public static boolean sqlManager = false;
    public static String graphDBConfig = "";
    public static GraphDatabaseService graphDB = null;
    public static Server server = null;
    public static boolean dbEnabled = false;
    public static boolean encryptEnabled;
    public static int encryptLevel;
    public static Properties prop = new Properties();
    public static List<WSHandler> clients = new ArrayList<WSHandler>();
    public static Map<String, List<Long>> failedAttempts = new HashMap<String, List<Long>>();
    public static List<HashMap<String, String>> pubkeyServers = new ArrayList<HashMap<String, String>>();
    public static List<Javascript> javascripts = new ArrayList<Javascript>();
    public static ObjectWriter ow = new ObjectMapper().writer();
    public static int port = 80;
    public static boolean serverTalkEnable = false;
    public static int serverTalkPort = 8081;
    public static Level loglevel = null;
    public static String eventShareServers;
    public static String modulePath = "modules/";
    public static String configPath = "configs/";
    public static String dbPath = "databases/";
    public static Client masterControl;
    public static String defaultPath = "./";
    public static String identifier = "";

    public static void main(String[] args) {
    	// PARSE ARGUMENTS USING COMMON CLI
    	CLIParser parser = new CLIParser(args);
    	// DONE parsing arguments
    	// DEFAULT CONFIG FILE
        if(parser.getCmd().hasOption("id")){
            identifier = parser.getCmd().getOptionValue("id");
        }
    	if(parser.getCmd().hasOption("sitepath")){
    		defaultPath = parser.getCmd().getOptionValue("sitepath"); // user selected different app root
    		if(!defaultPath.substring(defaultPath.length()-1).equals("/")){
    			defaultPath=defaultPath+"/";
    		}
    	}
    	String configFile = defaultPath+"config.ini";
    	if(parser.getCmd().hasOption("config")){
    		configFile = parser.getCmd().getOptionValue("config"); // user selected different config file
    	}
        if(parser.getCmd().hasOption("cb")){
            String[] connectElements = parser.getCmd().getOptionValue("cb").split("&");
            String[] addressElements = connectElements[0].split(":");
            try {
                masterControl = Client.createConnection(addressElements[0], Integer.valueOf(addressElements[1]), false, connectElements[1]);
            }catch(Exception e){ e.printStackTrace(); }
        }
        if(parser.getCmd().hasOption("scb") && parser.getCmd().hasOption("cb")){
            new Thread(new Statistics()).start();
        }
        Log.info( "Starting Synload Development Framework Server", SynloadFramework.class );
        try {
            if ((new File(configFile)).exists()) {
                prop.load(new FileInputStream(configFile));
                port = Integer.valueOf(prop.getProperty("port"));
                handleUpload = Boolean.valueOf(prop.getProperty("enableUploads"));
                siteDefaults = Boolean.valueOf(prop.getProperty("siteDefaults"));
                modulePath = defaultPath+prop.getProperty("modulePath", modulePath);
                dbPath = defaultPath+prop.getProperty("dbPath", dbPath);
                configPath = defaultPath+prop.getProperty("configPath", configPath);
                sqlManager = Boolean.valueOf(prop.getProperty("sqlManager"));
                encryptEnabled = Boolean.valueOf(prop.getProperty("encrypt"));
                encryptLevel = Integer.valueOf(prop.getProperty("encryptLevel"));
                graphDBPath = defaultPath+prop.getProperty("graphDBPath");
                graphDBConfig = prop.getProperty("graphDBConfig");
                loglevel = Level.toLevel(prop.getProperty("loglevel"));
                debug = Boolean.valueOf(prop.getProperty("debug"));
                dbEnabled = Boolean.valueOf(prop.getProperty("dbenabled"));
                uploadPath = defaultPath+prop.getProperty("uploadPath");
                maxUploadSize = Long.valueOf(prop.getProperty("maxUploadSize"));
                serverTalkEnable = Boolean.valueOf(prop.getProperty("serverTalkEnable"));
                serverTalkKey = prop.getProperty("serverTalkKey");
                serverTalkPort = Integer.valueOf(prop.getProperty("serverTalkPort"));
                graphDBEnable = Boolean.valueOf(prop.getProperty("graphDBEnable"));
                eventShareServers = prop.getProperty("eventShareServers","");
                pubkeyServers = parsePubKeyServers(prop.getProperty("pubkeyservers"));
            } else {
                InputStream is = SynloadFramework.class.getClassLoader().getResourceAsStream("config.ini");
                FileOutputStream os = new FileOutputStream(new File(configFile));
                IOUtils.copy(is, os);
                os.close();
                is.close();
                System.exit(0);
            }
            if(parser.getCmd().hasOption("port")){
                port = Integer.valueOf(parser.getCmd().getOptionValue("port")); // user selected different app root

            }
            if(!new File(defaultPath+"log4j.properties").exists()){
                InputStream is = SynloadFramework.class.getClassLoader().getResourceAsStream("log4j.properties");
                FileOutputStream os = new FileOutputStream(new File(defaultPath+"log4j.properties"));
                IOUtils.copy(is, os);
                os.close();
                is.close();
            }
            if(!new File(defaultPath+"bbcodes.xml").exists()){
                InputStream is = SynloadFramework.class.getClassLoader().getResourceAsStream("bbcodes.xml");
                FileOutputStream os = new FileOutputStream(new File(defaultPath+"bbcodes.xml"));
                IOUtils.copy(is, os);
                os.close();
                is.close();
            }
            Log.info("CONF", SynloadFramework.class);
            if(!dbEnabled){
                sql=null;
            }else{
                sql = DriverManager.getConnection(prop.getProperty("jdbc"), prop.getProperty("dbuser"), prop.getProperty("dbpass"));
                if(sql.isClosed()){
                    Log.error("MySQL failed to connect!",SynloadFramework.class);
                    //return;
                }
            }

            createFolder(modulePath);
            createFolder(configPath);
            createFolder(dbPath);

            ServerTalk.defaultTypes();
            
            Log.info("Modules loading", SynloadFramework.class);

            ModuleLoader.load(modulePath);

            Log.info("Modules loaded", SynloadFramework.class);

            if(graphDBEnable){
                Log.info("Neo4J enabled, starting up!", SynloadFramework.class);
                if (!(new File(graphDBPath)).exists()) {
                    (new File(graphDBPath)).mkdir();
                }
                graphDB = new GraphDatabaseFactory()
                    .newEmbeddedDatabaseBuilder(new File(defaultPath+graphDBPath))
                    .loadPropertiesFromFile( graphDBConfig )
                    .newGraphDatabase();
            }else{
                Log.info("Neo4J disabled, skipping!", SynloadFramework.class);
            }
            
            for (Entry<String, ModuleClass> mod : ModuleRegistry.getLoadedModules().entrySet()) {
                Log.info("Module ["+mod.getKey()+"] Initializing", SynloadFramework.class);
                mod.getValue().initialize();
            }

            Log.info("SQL versions", SynloadFramework.class);
            SQLRegistry.checkVersions();
            
            Log.info("Setting up http/websocket server", SynloadFramework.class);
            LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(150);
            ExecutorThreadPool pool = new ExecutorThreadPool(50, 200, 10,TimeUnit.MILLISECONDS, queue);
            server = new Server(pool);
            HTTPSPDYServerConnector connector = new HTTPSPDYServerConnector(server);
            connector.setPort(port);
            Connector[] g = new Connector[] { connector };
            server.setConnectors(g);
            HandlerCollection handlerCollection = new HandlerCollection();
            handlerCollection.addHandler(new HTTPHandler());
            handlerCollection.addHandler(new WebsocketHandler());
            server.setHandler(handlerCollection);

            Log.info("Loaded all aspects running on port " + port, SynloadFramework.class);
            if(!eventShareServers.equals("")){
                String[] eventShareConnections = eventShareServers.split(",");
                for(String eventShareConnection: eventShareConnections) {
                    String[] connectElements = eventShareConnection.split("&");
                    String[] addressElements = connectElements[0].split(":");
                    if (connectElements.length == 4 && addressElements.length == 2) {
                        try {
                            EventShare eventShare = new EventShare(addressElements[0], Integer.valueOf(addressElements[1]), connectElements[1], Boolean.valueOf(connectElements[2]), Boolean.valueOf(connectElements[3]));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if(serverTalkEnable){
                Log.info("Server talk enabled, starting up!", SynloadFramework.class);
                new Thread (new ServerTalk()).start();
            }else{
                Log.info("Server talk system disabled, skipping", SynloadFramework.class);
            }
            server.start();
            server.join();

        } catch (Exception e) {
            if (SynloadFramework.debug) {
                e.printStackTrace();
            }
        }
    }
    public static void createFolder(String folderPath){
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    public static String randomString(int length) {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(length);
    }

    public static void broadcast(String data) {
        for (WSHandler user : SynloadFramework.clients) {
            user.send(data);
        }
    }

    public static void broadcast(List<WSHandler> _cs, String data) {
        for (WSHandler user : _cs) {
            user.send(data);
        }
    }

    public static void registerJavascriptFile(Javascript js, String name) {
        // System.out.println("[JS] Registered javascript <"+name+">");
        SynloadFramework.javascripts.add(js);
    }

    public static HashMap<String, HashMap<String, Object>> getHtmlFiles() {
        return htmlFiles;
    }

    public static int getTimestamp() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    public static void setHtmlFiles(
            HashMap<String, HashMap<String, Object>> htmlFiles) {
        SynloadFramework.htmlFiles = htmlFiles;
    }

    public static List<ModuleClass> getPlugins() {
        return plugins;
    }
    public static ModuleClass getPlugin(String plugin) {
        for(Object plug : SynloadFramework.plugins){
            if(plug.getClass().getName().equalsIgnoreCase(plugin)){
                return (ModuleClass) plug;
            }
        }
        return null;
    }
    public static void setPlugins(List<ModuleClass> plugins) {
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

    public static ObjectWriter getOw() {
        return ow;
    }

    public static void setOw(ObjectWriter ow) {
        SynloadFramework.ow = ow;
    }

    public static boolean isEncryptEnabled() {
        return encryptEnabled;
    }

    public static void setEncryptEnabled(boolean encrypt) {
        SynloadFramework.encryptEnabled = encrypt;
    }
	@Override
	public void initialize() {
		Log.info("Synload Framework Loaded", SynloadFramework.class);
	}
	@Override
	public void crossTalk(Object... obj) {
	}
	public static int getEncryptLevel() {
		return encryptLevel;
	}
	public static void setEncryptLevel(int encryptLevel) {
		SynloadFramework.encryptLevel = encryptLevel;
	}

	private static List<HashMap<String, String>> parsePubKeyServers(String pubKeyServerList){
		List<HashMap<String, String>> servers = new ArrayList<HashMap<String, String>>();
		String[] pubKeyServers = pubKeyServerList.split("&");
		for(String pubKeyServer: pubKeyServers){
			HashMap<String, String> server = new HashMap<String, String>();
			String[] serverData =  pubKeyServer.split(",");
			server.put("address", serverData[0]);
			server.put("username", serverData[1]);
			server.put("password", serverData[2]);
			servers.add(server);
		}
		return servers;
		
		
	}
}