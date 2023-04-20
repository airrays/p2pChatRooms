import com.google.gson.Gson;

import java.io.*;
import java.net.*;
import java.util.List;

public class Client extends Thread{
    private int localPort;
    private Socket socket;
    private String ipAddress;
    private int PORT;
    private boolean alive;
    private Msg msg_obj;
    private String chatRoom;
    private boolean quit = false;
    private boolean connected=false;
    private String clientId;
    public Peer peer;
    private int pport;
    public Client(String ipAddress,int PORT,Peer peer,int pport){
        this.ipAddress=ipAddress;
        this.PORT=PORT;
        this.peer=peer;
        this.chatRoom="";
        this.localPort = peer.getIport();
        this.pport=pport;
        //this.pport=pport;
        //this.networkSearch=networkSearch; //if running the networkSearch and it will be a temp client
    }
    public void run(){
        connect();
//        startGetUserInput(this);
        //boolean flag = true;
//        if(networkSearch){
//            while (flag){
//                networkSearch();
//                sendList_request();
//                flag=false;
//            }
//        }
    }
    public void connect(){
        try {
            InetAddress ad = null;
//            System.out.println(InetAddress.getLocalHost());
            try{
                Socket testsocket = new Socket();
                testsocket.connect(new InetSocketAddress("bing.com", 80),1000);
                ad = testsocket.getLocalAddress();
                testsocket.close();
            }
            catch(Exception e){
                ad = InetAddress.getLocalHost();
            }
            if(ipAddress.equals("localhost")){
                ipAddress = peer.getLocalId().split(":")[0];
            }
            if(this.localPort != -1){
                socket=new Socket(ipAddress,PORT,ad,this.localPort);
            }
            else{

                socket=new Socket(ipAddress,PORT);
            }
//            socket = new Socket(ipAddress, PORT);
            this.clientId=socket.getLocalAddress().getHostAddress()+":"+String.valueOf(socket.getLocalPort());
//            System.out.println(this.clientId);
            this.peer.remoteid = this.clientId;
            //System.out.println("!id CLIENT ID "+clientId);
            //System.out.println("!id HOSTCHANGE SEND "+peer.getLocalId());
            alive = true;
            ClientSocketThread clientSocketThread = new ClientSocketThread(this,this.peer);
            clientSocketThread.start();
            //System.out.println("Connect to the " + socket.getInetAddress().getHostAddress() + socket.getPort());
            //System.out.println(getClientId());
            System.out.print(" seen as ["+this.chatRoom+"]"+ this.peer.remoteid +"> ");
            setConnected(true);
            Msg msg = new Msg();
            msg.setType("hostchange");
            msg.setHost(peer.getLocalId());
            sendMessage(msg);
        }catch (Exception e){
            //server close
            System.out.println("Unable to connect!"+ e.getMessage());
            e.printStackTrace();
            setChatRoom("");
            setClientId("");
            setConnected(false);
            peer.setClient("");
        }
    }
    public void startGetUserInput(Client client,String message){
//        BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(System.in));

//        System.out.printf("["+chatRoom+"]"+client.getIpAddress()+":"+client.getPort()+"> ");
//            String message=null;
//            try {
//                message=bufferedReader.readLine();
//                System.out.println("user input"+message);
//            }catch (IOException e){
//                e.printStackTrace();
//            }
        if(alive){
            handleMsg(message);
        }else {
            //reconnect
            System.out.println("Reconnect");
        }
    }
    public void networkSearch(){
        msg_obj=new Msg();
        msg_obj.setType("listneighbors");
        sendMessage(msg_obj);
    }
    public synchronized void sendList_request(){
        msg_obj=new Msg();
        msg_obj.setType("list");
        sendMessage(msg_obj);
    }
    public void sendQuit(){
        msg_obj=new Msg();
        msg_obj.setType("quit");
        setQuit();
        sendMessage(msg_obj);
    }
    /*
    Network Search Variables
     */
    private List<String>neighbors;
    private List<Room> networkSearch_rooms;
    /*
    Getter
     */
    public Socket getSocket() {
        return socket;
    }
    public boolean getAlive() {
        return alive;
    }
    public String getIpAddress(){
        return socket.getLocalAddress().getHostAddress();
    }
    public String getPort(){
        return String.valueOf(socket.getLocalPort());
    }
    public synchronized String getChatRoom(){return this.chatRoom;}
    public String getClientId(){return this.clientId;}
    public synchronized List<String> getNeighbors() {return this.neighbors;}
    public synchronized List<Room>getNetworkSearch_rooms(){return this.networkSearch_rooms;}
    /*
    Setter
     */
    public synchronized void setChatRoom(String room){ this.chatRoom = room;}
    public synchronized boolean getQuit(){return this.quit;}
    public synchronized void setQuit(){this.quit = true;}
    public void setClientId(String clientId){this.clientId = clientId;}
    public synchronized void setNeighbors(List<String>neighbors){this.neighbors = neighbors;}
    public synchronized void setNetworkSearch_rooms(List<Room>rooms){this.networkSearch_rooms=rooms;}

    //TODO add code comment here
    public boolean getConnected(){return this.connected;}
    public void setConnected(boolean c){this.connected=c;}

    //Send local Message (Meaningless)
    public void sendLocalMessage(Msg msg){
        try {
            Gson gson=new Gson();
            System.out.println(gson.toJson(msg));
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            printWriter.write(gson.toJson(msg)+"\n");
            printWriter.flush();
        }catch (IOException e){
            alive=false;
            setConnected(false);
            System.out.println("!Client sendMessage Exc");
            //TODO reconnect()
        }
    }
    /*
    Private Functions
     */
    /*
    Send message to the server
     */
    public void handleMsg(String msg){
        if("".equals(msg)){
            System.out.println("empty message");
            return;
        }

        if (msg.charAt(0) == '#') {
            String[] command = msg.split(" ");
            switch (command[0]) {
                case "#listneighbors":
                    msg_obj=new Msg();
                    msg_obj.setType("listneighbors");
                    sendMessage(msg_obj);
                    break;
                case "#join":
                    String newRoomId="";
                    if(command.length==2){
                        newRoomId=command[1];
                    }
                    msg_obj=new Msg();
                    msg_obj.setType("join");
                    msg_obj.setRoomid(newRoomId);
                    sendMessage(msg_obj);
                    break;
                case "#who":
                    if(command.length==1){
                        System.out.printf("Retype who roomId");
                    }else {
                        msg_obj = new Msg();
                        msg_obj.setType("who");
                        msg_obj.setRoomid(command[1]);
                        sendMessage(msg_obj);
                    }
                    break;
                case "#list":
                    msg_obj=new Msg();
                    msg_obj.setType("list");
                    sendMessage(msg_obj);
                    break;
                case "#quit":
                    msg_obj=new Msg();
                    msg_obj.setType("quit");
                    setQuit();
                    sendMessage(msg_obj);
                    break;
            }
        }else {
            msg_obj = new Msg();
            msg_obj.setType("message");
//            msg_obj.setIdentity(getClientId());
            msg_obj.setContent(msg);
            sendMessage(msg_obj);
        }
    }
    public synchronized void sendMessage(Msg msg){
        try {
            Gson gson=new Gson();
//            System.out.println("~~sendMessage function");
//            System.out.println(gson.toJson(msg));
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            printWriter.write(gson.toJson(msg)+"\n");
            printWriter.flush();
        }catch (IOException e){
            alive=false;
            System.out.println("!Client sendMessage Exc");
            //TODO reconnect()
        }
    }

    public void close() {
        try {
            alive = false;
            setConnected(false);
            peer.server.setSelfConnectStage(true);
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            System.out.println("!2");;
        }
    }
}
