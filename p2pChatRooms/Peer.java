import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

import com.google.gson.Gson;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;


//The peer class which
public class Peer {
    public Client client;
    private BufferedReader bufferedReader;
    public Server server;
    private boolean flag;
    public String chatRoom;
    private int iport;
    private int pport;
    private boolean inRoom;
    public String localid;
    public String remoteid;
    private Random rand;
    private boolean searchNetworkStatus;
    public static Map<String,Integer> shout_map=new HashMap<String,Integer>();
    public Peer(Server server, int iport, int pport) throws UnknownHostException {
        this.client=null;
        this.bufferedReader=new BufferedReader(new InputStreamReader(System.in));
        this.server=server;
        this.chatRoom="";
        this.iport = iport;
        this.pport = pport;
        this.inRoom = false;
        try{
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("bing.com", 80),1000);
            this.localid = socket.getLocalAddress().getHostAddress()+":"+ String.valueOf(pport);
            socket.close();
        }
        catch(Exception e){
            try{
                this.localid = InetAddress.getLocalHost().toString().split("/")[1] + ":" + String.valueOf(pport);
            }
            catch(Exception e1){
                this.localid = InetAddress.getLocalHost().toString() + ":" + String.valueOf(pport);
            }
        }

        this.remoteid = null;
        this.rand = new Random();
    }
    public void setInRoom(boolean in){this.inRoom = in;}
    public boolean getInRoom(){return this.inRoom;}
    public Client getClient(){return this.client;}
    public String getLocalId(){return this.localid;}
    public void setClient(String clientId){
//        System.out.println("+++++++++++");
        if(clientId.equals("")){
            this.client=null;
            this.chatRoom="";
            setInRoom(false);
            server.setSelfRoom("");
            server.setSelfConnectStage(false);
        }
    }
    public boolean getSearchNetworkStatus(){return this.searchNetworkStatus;}
    public int getIport(){return this.iport;}

    /*
    Start get user input text in terminal
     */
    public void startGetPeerInput() {
        flag=true;
        while(flag) {
            try {
                if(client == null || client.getConnected() == false){
                    System.out.print("["+this.chatRoom+"]"+ this.localid +"> ");
                }
                else{
                    System.out.print("["+this.chatRoom+"]"+ this.remoteid+"> ");
                }
                String input = bufferedReader.readLine();
                String[] inputValues = input.split(" ");
                if (client != null && client.getConnected()==true) {
                    server.setSelfConnectStage(false);
                } else {
                    //null client, self connect to the server
                    server.setSelfConnectStage(true);
                    server.setSelfRoom(chatRoom);
                }
                if (inputValues.length != 0 && input.charAt(0) == '#') { //dealing with the command, separate command
                    if(inputValues[0].equals("#help")){
                        handleLocalCommand(inputValues);
                    }
                    else if(inputValues[0].equals("#delete")){
                        handleLocalCommand(inputValues);
                    }
                    else if(inputValues[0].equals("#kick")){
                        handleLocalCommand(inputValues);
                    }
                    else if(inputValues[0].equals("#searchnetwork")){
                        handleLocalCommand(inputValues);
                    }
                    else if(inputValues[0].equals("#shout")){
                        if(inRoom){
                            if (client == null || client.getConnected() == false){
                                localShout(inputValues,this.localid);
                            }
                            else{
                                localShout(inputValues,this.client.getClientId());
                                Integer identifier = shout_map.get(this.client.getClientId() + " shouted");
                                remoteShout(inputValues, identifier);
                            }
                        }
                        else{
                            System.out.println("Join a room to shout!");
                        }
                    }
                    else if(inputValues[0].equals("#createroom")){
                        handleLocalCommand(inputValues);
                    }
                    else if (inputValues[0].equals("#connect")) {
//                        System.out.println("~remote Command");
                        handleRemoteCommand(inputValues, input);
                    } else if (client == null || client.getConnected() == false) {
                        handleLocalCommand(inputValues);
                    } else if (client != null && client.getConnected() == true) {
                        handleRemoteCommand(inputValues, input);
                    }
//                if (client == null || localCommand.contains(inputValues[0])) {
//                    handleLocalCommand(inputValues);
//                } else {
//                    //System.out.println("client is not null");
//                    client.startGetUserInput(client, input);
//                }
                } else {
                    //dealing with chat message
                    if (client != null && client.getConnected() == true && !client.getChatRoom().equals("")) {
                        client.startGetUserInput(client, input);
                    } else if (client == null || client.getConnected() == false) {
                        if (!chatRoom.equals("")) {
                            try {
                                localChatMode(input);
                            }catch (Exception e){
                                System.out.println("!3");;
                            }
                        }
                    } else {
                        System.out.println("Join a room to chat");//
                    }
                }
            }catch (IOException e){
                //server close
                System.out.println("Server Closed");
                this.client.setConnected(false);
                setClient("");
                break;
            }
            catch(NullPointerException e){
                System.out.println("Application Close");
                System.exit(0);
                flag = false;
                break;
            }

        }
    }
    private void localShout(String[] inputValues, String id){
        Msg msg = new Msg();
        msg.setType("shout");
        msg.setIdentity(id + " shouted");
        String content = "";
        for(int i = 1; i < inputValues.length; i++){
            if(i == inputValues.length-1){
                content += inputValues[i];
            }
            else{
                content += inputValues[i] + " ";
            }
        }
        msg.setContent(content);
        Integer identifier = Integer.valueOf(this.rand.nextInt(10000));
        msg.setIdentifier(identifier.toString());
        shout_map.put(id + " shouted",identifier);
        server.deliverShout(msg,null);
    }
    private void remoteShout(String[] inputValues, Integer identifier){
        Msg msg = new Msg();
        msg.setType("shout");
        msg.setIdentity(this.client.getClientId() + " shouted");
        String content = "";
        for(int i = 1; i < inputValues.length; i++){
            if(i == inputValues.length-1){
                content += inputValues[i];
            }
            else{
                content += inputValues[i] + " ";
            }
        }
        msg.setContent(content);
        msg.setIdentifier(identifier.toString());
        this.client.sendMessage(msg);
    }
    private void handleLocalCommand(String[] inputValues){
        switch (inputValues[0]) {
            case "#createroom":
                boolean create = true;
                if(inputValues.length > 1){
                    if (inputValues[1].length() >= 3 && inputValues[1].length() <= 32) {
                        for (int i = 0; i < inputValues[1].length(); i++) {
                            if (i == 0) {
                                if (Character.isLetter(inputValues[1].charAt(i))) {
                                    continue;
                                } else {
                                    System.out.println("Roomid should start with a letter.");
                                    create = false;
                                    break;
                                }
                            } else {
                                if (Character.isLetterOrDigit(inputValues[1].charAt(i))) {
                                    continue;
                                } else {
                                    System.out.println("Roomid should only contains letter and digits.");
                                    create = false;
                                    break;
                                }
                            }
                        }
                    } else {
                        System.out.println("Upper and lower case letters and digits only, and should be at least 3 characters and no more than 32.");
                        create = false;
                    }
                    if (create) {
                        if (server.getRoom_clients().containsKey(inputValues[1])) {
                            System.out.println("Room " + inputValues[1] + " is invalid or already in use.");
                        } else {
                            Room newRoom = new Room(inputValues[1]);
                            newRoom.setUsers(new ArrayList<>());
                            server.addSocket_Room(inputValues[1], null);
                            server.setRoomsObj(inputValues[1], newRoom);
                            System.out.println("Room " + inputValues[1] + " created.");
                        }
                    }
                }
                else{
                    System.out.println("Please enter room name to create a room!");
                }
                break;
            case "#kick":
                //System.out.println("Kick");
                String[] kick_id=inputValues[1].split(":");
                String kick_ip=kick_id[0];
                String kick_port=kick_id[1];
                for(ServerSocketThread serverSocketThread:server.getConnectionsSockets()){
                    if(serverSocketThread.getClientId().equals(inputValues[1])){
                        server.removeSocket_Room(serverSocketThread.getChatRoom(),serverSocketThread);
                        server.removeUser_Room(serverSocketThread.getChatRoom(),serverSocketThread.getClientId());
                        serverSocketThread.kick();
                    }
                }
                server.addBlackList(kick_ip);
                //System.out.println("~Kick "+kick_id[0]+" "+kick_id[1]);
                break;
            case "#join":
                if(inputValues.length==1){
                    if(!chatRoom.equals("")) {
                        Msg msg = new Msg();
                        msg.setIdentity(this.localid);
                        msg.setType("roomchange");
                        msg.setRoomid("");
                        msg.setFormer(this.chatRoom);
                        try{
                            server.deliverChatMsg(msg,chatRoom,null);
                        }
                        catch(Exception e){
                            System.out.println("Fail to deliver room change for yourself!");
                        }
                        server.removeUser_Room(chatRoom, this.localid);
                        server.setSelfRoom("");
                        chatRoom = "";
                        inRoom = false;
                    }
                }else {
                    if (server.getRooms().containsKey(inputValues[1])) {
                        if(inputValues[1].equals(this.chatRoom)){
                            System.out.println("The requested room is invalid or non existent.");
                        }
                        else {
                            Msg msg = new Msg();
                            msg.setIdentity(this.localid);
                            msg.setType("roomchange");
                            msg.setRoomid(inputValues[1]);
                            msg.setFormer(this.chatRoom);
                            try{
                                server.deliverChatMsg(msg,inputValues[1],null);
                            }
                            catch(Exception e){
                                System.out.println("Fail to deliver room change for yourself!");
                            }
                            if(chatRoom.equals("")){
                                System.out.println(this.localid + " moved to " + inputValues[1]);
                            }
                            else{
                                server.removeUser_Room(chatRoom, this.localid);
                                System.out.println(this.localid + " moved from " + chatRoom + " to " + inputValues[1]);
                            }
                            server.addUser_Room(inputValues[1], this.localid);
                            chatRoom = inputValues[1];
                            server.setSelfRoom(inputValues[1]);
                            inRoom = true;
                        }
                    } else {
                        System.out.println("No room");
                    }
                }
                break;
            case "#delete":
//                System.out.println("~LocalDeleate");
                if(chatRoom.equals(inputValues[1])){
                    chatRoom="";
                    server.setSelfRoom("");
                }
                server.localDelete(inputValues[1]);
                break;
            case "#list":
                Msg msg=new Msg();
                msg.setType("roomlist");
                msg.setRooms(server.getRoomList());
                server.setLocalMessage(msg,chatRoom);
                break;
            case "#who":
                String roomid_who=inputValues[1];
                msg=new Msg();
                msg.setType("roomcontents");
                msg.setRoomid(roomid_who);
                List<String> tempUser_list=server.getSingleRoomObj(roomid_who).getUsers();
                msg.setIdentities(tempUser_list);
                server.setLocalMessage(msg,chatRoom);
                break;
            case "listneighbors":
                msg=new Msg();
                msg.setType("neighbors");
                msg.setNeighbors(server.getConnections());
                List<String>t=new ArrayList<String>(server.getConnections());
                System.out.println("List "+ t.get(0));
                server.setLocalMessage(msg,chatRoom);
                break;
            case "#quit":
                System.exit(0);
            case "#help":
                System.out.println("#help - list this information");
                System.out.println("#connect IP[:port] [local port] - connect to another peer");
                System.out.println("#quit - disconnect from a peer");
                System.out.println("#list - list room name and user count");
                System.out.println("#createroom - create room locally, cannot join that room when you connecting to other peer");
                System.out.println("#join [room name]- join room which is in connecting server,cannot join the room in your own server when you connecting to other peer");
                System.out.println("#join [''] - leave the room");
                System.out.println("#searchnetwork - search whole network and get room list");
                System.out.println("#shout [message] - message to be delivered to all rooms on all peers of the network");
                System.out.println("#kick [ip:port] - when you are the host, you can kick and ban");
                System.out.println("#listbeighbors - list connecting peer's neighbors host address");
                System.out.println("#who - show room users");
                break;
            case "#searchnetwork":
                /*
                Variables
                 */
                Client temp_clientConnection=null;
                ServerSocketThread current_serverSocketThread=null;
                String current_HostIp="";
                Map<String,List<Room>>peer_rooms=new HashMap<String,List<Room>>();
                Queue<String> queue=new LinkedList<String>();
                List<String> visited_peerId=new ArrayList<String>();
                int iterationCtn=0;
                searchNetworkStatus=true;
                /*
                functions
                 */
                //peer_rooms.put(localid,server.getRoomList()); //own peer room list
                for(ServerSocketThread serverSocketThread:server.getConnectionsSockets()){
                    String peer_hostIp=serverSocketThread.getClient_ownHostId();
//                    System.out.println(peer_hostIp);
                    queue.offer(peer_hostIp);
                }
                /*
                BFS
                 */
                while(!queue.isEmpty()){
//                    System.out.println("~~Network Searching");
                    current_HostIp=queue.poll();
                    //System.out.println("**Current HostIP: "+current_HostIp);
                    String[] temp=current_HostIp.split(":");//ip:port
//                    temp_clientConnection=new Client("localhost",Integer.valueOf(temp[1]),this,pport,-1,true);
//                    temp_clientConnection.start();
                    List<String>neighbors=new ArrayList<String>();
                    List<Room>rooms=new ArrayList<Room>();
                    try {
                        Socket temp_socket = new Socket(temp[0], Integer.valueOf(temp[1]));
                        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(temp_socket.getOutputStream()));
                        Gson gson=new Gson();
                        Msg msg_obj=new Msg();
                        msg_obj.setType("hostchange");
                        msg_obj.setHost(localid);
                        printWriter.write(gson.toJson(msg_obj)+"\n");
                        printWriter.flush();

                        printWriter.close();
                        temp_socket.close();
                        temp_socket = new Socket(temp[0], Integer.valueOf(temp[1]));
                        printWriter = new PrintWriter(new OutputStreamWriter(temp_socket.getOutputStream()));

                        msg_obj=new Msg();
                        msg_obj.setType("listneighbors");
                        printWriter.write(gson.toJson(msg_obj)+"\n");
                        //System.out.println(gson.toJson(msg_obj));
                        printWriter.flush();

                        BufferedReader bufferedReader1 = new BufferedReader(new InputStreamReader(temp_socket.getInputStream()));
                        gson = new Gson();
                        String temp_msg = bufferedReader1.readLine();
                        //System.out.println("Receive Neighbors  " + temp_msg);

                        Msg message = gson.fromJson(temp_msg, Msg.class);
                        neighbors = message.getNeighbors();
                        //System.out.println("!! waiting neighbors list " + neighbors);
                        msg_obj = new Msg();
                        msg_obj.setType("list");
                        printWriter.write(gson.toJson(msg_obj) + "\n");
                        printWriter.flush();
                        temp_msg = bufferedReader1.readLine();
                        message = gson.fromJson(temp_msg, Msg.class);
                        rooms = message.getRooms();
                        //System.out.println("!25 " + rooms.size());

                        printWriter.close();
                        bufferedReader1.close();
                        temp_socket.close();
                        //if(temp_socket.isClosed()){
                        //System.out.println("Temp socket closed");
                        //}

                    }catch (IOException e){
                        System.out.println("!21");
                        //System.out.println("ERR "+e.getMessage());
                    }
                    for(String newPeer_Ip: neighbors){
                        if(newPeer_Ip!=null) {
                                if (!visited_peerId.contains(newPeer_Ip) && !newPeer_Ip.equals(localid)) {
                                    queue.offer(newPeer_Ip);
//                                    System.out.println("NewPeer_ip " + newPeer_Ip);
                                }

                        }
                    }
                    peer_rooms.put(current_HostIp,rooms);
                    visited_peerId.add(current_HostIp);
                    //temp_clientConnection.sendQuit();
                    iterationCtn++;
                    System.out.println(current_HostIp);
                    for(Room room:rooms){
                        System.out.println(room.getName()+" "+room.getUserCount()+" users ");
                    }
                }
//                for(String hostip:peer_rooms.keySet()){
//                    System.out.println("hostip "+hostip);
//                    for(Room room:peer_rooms.get(hostip)){
//                        System.out.println(room.getName()+" "+room.getUserCount()+" users ");
//                    }
//                }
                //System.out.println("Iteration "+iterationCtn);
                searchNetworkStatus=false;
                break;
            default:
                System.out.println("Error local command, re-type please");

        }

    }
    private void localChatMode(String input) throws Exception {
        Msg msg=new Msg();
        msg.setType("message");
        msg.setIdentity(this.localid);
        msg.setContent(input);
        if(chatRoom!="") {
            server.deliverChatMsg(msg, chatRoom, null);
        }
    }
    private void handleRemoteCommand(String[] inputValues,String input){
        switch (inputValues[0]) {
            case "#connect":
                try{
                    String[] address = inputValues[1].split(":");
                    String targetServerIp = address[0];
                    String targetServerPort = address[1];
                    if(inputValues.length == 3){
                        iport = Integer.valueOf(inputValues[2]);
                    }
                    //System.out.println(targetServerIp + " : " + targetServerPort);
                    if(client == null || client.getConnected() == false){
                        if(!chatRoom.equals("")){
                            Msg msg = new Msg();
                            msg.setIdentity(this.localid);
                            msg.setType("roomchange");
                            msg.setRoomid("");
                            msg.setFormer(this.chatRoom);
                            try{
                                server.deliverChatMsg(msg,chatRoom,null);
                            }
                            catch(Exception e){
                                System.out.println("Fail to deliver room change for yourself!");
                            }
                            server.removeUser_Room(chatRoom, this.localid);
                            server.setSelfRoom("");
                            chatRoom = "";
                            inRoom = false;
                        }
                        client = new Client(targetServerIp, Integer.valueOf(targetServerPort),this,pport);
                        //Start Client Thread
                        client.start();
                        System.out.println("Connected to " + targetServerIp + ":" + targetServerPort);
                    }
                    else{
                        client.startGetUserInput(client,"#quit");
                        client = new Client(targetServerIp, Integer.valueOf(targetServerPort),this,pport);
                        //Start Client Thread
                        client.start();
                        System.out.println("Connected to " + targetServerIp + ":" + targetServerPort);
                    }
                    inRoom = false;
                    chatRoom = "";
                    server.setSelfConnectStage(false);

                }
                catch (Exception e){
                    System.out.println("Unable to connect!");
                }
                break;
            case "#join":
            case "#who":
            case "#list":
            case "#quit":
            case "#listneighbors":
                client.startGetUserInput(client,input);
                break;
            default:
                System.out.println("!remotecommadn Error Type");
        }
    }

}
