package com.synload.talksystem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.synload.framework.Log;
import com.synload.framework.modules.ModuleLoader;
import com.synload.talksystem.systemMessages.ClassNotFoundMessage;
import com.synload.talksystem.systemMessages.UnrecognizedMessage;

public class ExecuteRead implements Runnable{
    public DataInputStream dIn;
    private Client client;
    private boolean keepRunning=true;
    private ClassLoader cl;
    public List<Object> queue = new ArrayList<Object>(); 
    public ExecuteRead(DataInputStream dIn, Client client, ClassLoader cl){
        this.cl = cl;
        this.setClient(client);
        this.setdIn(dIn);
    }
    public Object read(int length) throws IOException{
        int reading = 1024*1024*8;
        ByteArrayOutputStream bdata = new ByteArrayOutputStream(); 
        while(length>0){
            reading = 1024*1024*8;
            if(reading>length){
                reading=length;
            }
            byte[] message = new byte[reading];
            dIn.readFully(message);
            length -= reading;
            bdata.write(message);
        }
        
        /*byte [] m = null;
        String i = (new String(allData));
        String [] data = i.split(":");
        if(data.length<3){
            System.out.println(i);
        }
        try {
            m = this.getClient().decrypt(
                data[0],
                data[1],
                data[2],
                this.getClient().key
            );
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        //Log.debug("read "+bdata.size()+" bytes", this.getClass());
        if(bdata.size()>0){
            ByteArrayInputStream bas = new ByteArrayInputStream(bdata.toByteArray());
            ConnectDocumentLoader in = new ConnectDocumentLoader(new ModuleLoader(cl), bas);
            try {
                Object obj = in.readObject();
                return obj;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void run() {
        Log.debug("New Execute Read "+Thread.currentThread().getName(), this.getClass());
        try {
            while(this.isKeepRunning() && !this.getClient().getSocket().isInputShutdown()){
                if(dIn.available()>0){
                    int length = dIn.readInt();
                    if(length>0){
                        Object data = read(length);
                        if(data!=null){
                            //Log.debug(data.getClass().getName(), Client.class);
                            if(ConnectionDocument.class.isInstance(data)){
                                if(UnrecognizedMessage.class.isInstance(data)){
                                    Log.error("Unrecognized Connection Type", Client.class);
                                }else if(ClassNotFoundMessage.class.isInstance(data)){
                                    Log.error("Unrecognized Connection Type", Client.class);
                                }else if(ConnectionTypeDocument.class.isInstance(data)){
                                    ConnectionTypeDocument c = (ConnectionTypeDocument) data;
                                    if(c.getTypeName().equals("communicationSocket")){
                                        ServerTalk.getConnected().add(this.getClient());
                                    }
                                }else{
                                    ConnectionType type = null;
                                    ConnectionDocument doc = (ConnectionDocument) data;
                                    List<ConnectionType> types = new ArrayList<ConnectionType>(ServerTalk.types);
                                    for(ConnectionType t : types){
                                        if(t.getName().equals(doc.getTypeName())){
                                               type = t;
                                               break;
                                        }
                                    }
                                    if(type!=null){
                                        type.execute(this.getClient(), (ConnectionDocument) data);
                                    }else{
                                        this.getClient().write(new UnrecognizedMessage());
                                    }
                                }
                            }
                        }
                    }
                }
                Thread.sleep(1L);
            }
        } catch (IOException e) {
        	e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return;
    }
    public DataInputStream getdIn() {
        return dIn;
    }
    public void setdIn(DataInputStream dIn) {
        this.dIn = dIn;
    }
    public Client getClient() {
        return client;
    }
    public void setClient(Client client) {
        this.client = client;
    }
    public boolean isKeepRunning() {
        return keepRunning;
    }
    public void setKeepRunning(boolean keepRunning) {
        this.keepRunning = keepRunning;
    }
    public List<Object> getQueue() {
        return queue;
    }
    public void setQueue(List<Object> queue) {
        this.queue = queue;
    }

}
