import com.google.gson.Gson;

import java.io.IOException;
import java.net.*;
import java.util.Optional;
import java.util.*;

//import static jdk.internal.net.http.common.Utils.close;

public class Server extends Thread{
    private int PORT;
    private ServerSocket serverSocket;
    private ServerSocketThread thread;
    private boolean serverAlive=false;
    private List<ServerSocketThread>connectedSockets=new ArrayList<>(); //connecting clients sockets
    private static Map<String, List<ServerSocketThread>> room_clients = new HashMap<String, List<ServerSocketThread>>(); //store relationship between socket thread and roomId
    private static Map<String,Room> rooms_map=new HashMap<>(); // store roomId and room object
    private Peer peer;
    private List<String>blackList;
    public Server(int port) {
        this.PORT=port;
    }

    /*
    Public functions
     */
    public void run(){
        try {
            InetAddress ad = null;
            try{
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress("bing.com", 80),1000);
                ad = socket.getLocalAddress();
                socket.close();
            }
            catch(Exception e){
                ad = InetAddress.getLocalHost();
            }
            serverSocket=new ServerSocket(PORT,10000,ad);
            serverAlive=true;
            System.out.printf("Listening on port %d\n",PORT);
            while (serverAlive){
                Socket socket=serverSocket.accept();
                ServerSocketThread socketThread=new ServerSocketThread(socket,this);
                socketThread.setPeer(this.peer);
                connectedSockets.add(socketThread);
//                System.out.println(connectedSockets.size());
                socketThread.start();
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            System.out.println("Chat Stop");
            close();
        }
    }

    /*
    Getter
     */
    public String getServerAddress() throws UnknownHostException {
        return InetAddress.getLocalHost().toString();
    }
    //ban list aka BlackList
    public synchronized List<String> getBlackList(){return this.blackList;}
    //get all current room objects
    public synchronized List<Room> getRoomList() {
        List<Room> data = new ArrayList<Room>();
        for (Map.Entry<String, Room> entry : rooms_map.entrySet()) {
            data.add(entry.getValue());
        }
        return data;
    }
    public synchronized List<ServerSocketThread> getConnectionsSockets(){return this.connectedSockets;}
    public synchronized Map<String,List<ServerSocketThread>> getRoom_clients(){return room_clients;}
    public synchronized Map<String,Room> getRooms(){return rooms_map;}
    public synchronized Room getSingleRoomObj(String roomName) {return this.rooms_map.get(roomName);}
    public synchronized List<String>getConnections(){
        List<String>temp_connected=new ArrayList<String>();
        for(ServerSocketThread serverSocketThread:connectedSockets){
            temp_connected.add(serverSocketThread.getClient_ownHostId());
        }
        return temp_connected;
    }
    /*
    Setter
     */
    public void setPeer(Peer peer){ this.peer = peer;}
    public void setRoomsObj(String roomName,Room room){
        rooms_map.put(roomName,room);
    }
    //add User Client Id into the Room Object
    public synchronized void addUser_Room(String roomName,String clientId){
        List<Room>tempRoom =new ArrayList<Room>(rooms_map.values());
        Room room=tempRoom.stream().filter(r->r.getName().equals(roomName)).findAny().get();
        room.addUsers(clientId);
        rooms_map.put(roomName,room);
    }
    //ban list aka BlackList
    public void addBlackList(String kick_ip){
        if(blackList!=null && !blackList.contains(kick_ip)){
            blackList.add(kick_ip);
        }
    }
    //add client(ServerSocketThread) into the room
    public synchronized void addSocket_Room(String roomName,ServerSocketThread serverSocketThread){
        List<ServerSocketThread> temp =new ArrayList<ServerSocketThread>();
        if(room_clients.containsKey(roomName)){
            temp=room_clients.get(roomName);
            temp.add(serverSocketThread);
        }
        room_clients.put(roomName,temp);
    }
    //delete client(ServerSocketThread) from the Room
    public synchronized void removeSocket_Room(String roomName,ServerSocketThread serverSocketThread){
        //try {
            List<ServerSocketThread> sockets = room_clients.get(roomName);
//        if (sockets == null) {
//            throw new Exception("ROOM_NOT_EXIST");
//        }
            if (sockets != null) {
                sockets.remove(serverSocketThread);
            }
//        System.out.println("~~REMOVEDD"+sockets);
//        if (!remove) {
//            throw new Exception("CLIENT_NOT_IN_THIS_ROOM");
//        }
//        if (sockets.size() == 0 && getSingleRoomObj(roomName).getOwner().equals("")) {
//            rooms.remove(roomName);
//            roomsObj.remove(roomName);
//            System.out.println(roomsObj);
//            return;
//        }
            room_clients.put(roomName, sockets);
//        }catch(Exception e){
//            System.out.println();
//        }
    }
    public synchronized void removeUser_Room(String roomName,String clientId){
        List<Room>temp =new ArrayList<Room>(rooms_map.values());
        try{
            Optional<Room> tempRoom = temp.stream().filter(r -> r.getName().equals(roomName)).findAny();
            Room room=tempRoom.get();
            room.removeUser(clientId);
        }catch (NullPointerException e){
            System.out.println("user not in the "+roomName);
        }
    }
    /*
    Action for peer connections
     */
    public synchronized void removeSocketConnection(ServerSocketThread serverSocketThread){
        connectedSockets.remove(serverSocketThread);
        //System.out.println("!!-- "+connectedSockets.size());
    }
    /*
    Message delivery functions
     */
    //Send message to all users in the room
    public synchronized void deliverChatMsg(Msg msg,String roomName,ServerSocketThread serverSocketThread) throws Exception {
        List<ServerSocketThread> sockets = room_clients.get(roomName);
//        System.out.println("deliverChatMsg");
        if (sockets == null) {
            throw new Exception("ROOM_NOT_EXIST");
        }
        for (ServerSocketThread socket: sockets) {
            if(socket!=serverSocketThread){
                socket.sendMsg("~deliverChatMsg",msg);
            }
        }
        deliverLocalMessage(msg,roomName);
    }
    public synchronized  void deliverShout(Msg msg,ServerSocketThread serverSocketThread){

        for (ServerSocketThread socket: connectedSockets){
            if(socket!=serverSocketThread){
                    socket.sendMsg("~deliverChatMsg",msg);
                }
        }
    }
    public synchronized void broadCastConnecting(Msg message)throws Exception{
        for(ServerSocketThread serverSocketThread:connectedSockets){
            serverSocketThread.sendMsg("message",message);
        }
    }

    /*
    When Self connect functions
     */
    private boolean selfConnect=true;
    private String selfRoom;
    public boolean getSelfConnect(){return this.selfConnect;}
    public void setSelfConnectStage(boolean selfConnect){
        this.selfConnect=selfConnect;
    }
    public void setSelfRoom(String roomName){
        this.selfRoom=roomName;
    }
    //call by the deliveryChatMessage function(), when room broadcast,receive msg from room server
    public void deliverLocalMessage(Msg msg,String roomName){
        if(selfConnect && selfRoom.equals(roomName)) {
            //System.out.println("[" + selfRoom + "]" + "localhost> ");
            switch (msg.getType()) {
                case "message":
                    if(!msg.getIdentity().equals(peer.localid)) {
                        System.out.println("[" + roomName + "]" + msg.getIdentity() + "> " + msg.getContent());
                    }
                    break;
                case "roomchange":
                    String newroom = msg.getRoomid();
                    String former = msg.getFormer();
                    String identity = msg.getIdentity();
                    String id = peer.localid;
                    if(former.equals(newroom)){
                        System.out.println("The requested room is invalid or non existent.");
                    }
                    else{
                        if(id.equals(identity)) {
                            if (newroom.equals("")) {
                                System.out.println("Leave room " + former);

                            } else {

                                if(former.equals("")){
                                    System.out.println(identity + " moved to " + newroom);
                                }
                                else {
                                    System.out.println(identity + " moved from " + former + " to " + newroom);
                                }

                            }
                        }
                        else{
                            if (newroom.equals("")) {
                                System.out.println(identity + " Leave room " + former);
                            } else {
                                if(former.equals("")){
                                    System.out.println(identity + " moved to " + newroom);
                                }
                                else {
                                    System.out.println(identity + " moved from " + former + " to " + newroom);
                                }
                            }
                        }
                    }
                    break;
                case "quit":
                    System.out.println(" " + msg.getIdentity() + " quit");
                    break;
                default:
                    System.out.println("!setLocalMessage Error Type");
            }
        }
    }
    public void localDelete(String roomName){
        if(rooms_map.containsKey(roomName)) {
            if(!room_clients.get(roomName).isEmpty()) {
                for (ServerSocketThread serverSocketThread : room_clients.get(roomName)) {
                    Msg msg = new Msg();
                    msg.setIdentity(serverSocketThread.getClientId());
                    msg.setType("roomchange");
                    msg.setRoomid("");
                    msg.setFormer(serverSocketThread.getChatRoom());
                    serverSocketThread.setChatRoom("");
                    serverSocketThread.sendMsg("~Host delete Room ", msg);
                }
            }
            room_clients.remove(roomName);
            rooms_map.remove(roomName);
        }else{
            System.out.println("The requested room is invalid or non existent.");
        }
    }
    public void setLocalMessage(Msg msg,String roomName){
        switch (msg.getType()){
            case "roomlist":
                List<Room> rooms = msg.getRooms();
                System.out.println("room list:");
                for(int i = 0; i < rooms.size(); i++) {
                    String roomid = rooms.get(i).getName();
                    int count = rooms.get(i).getUserListCtn();
                    System.out.println(roomid + " " + count);
                }
                break;
            case "roomcontents":
                String room = msg.getRoomid();
                List<String> identities = msg.getIdentities();
                System.out.print(room + " contains");
                for(int i = 0; i < identities.size(); i++){
                    String clientid= identities.get(i);
                    System.out.print(" " + clientid);
                }
                System.out.print("\n");
                break;
            case "neighbors":
                System.out.printf("Neighours:[");
                for(String neighbors:msg.getNeighbors()){
                    System.out.printf(neighbors+", ");
                }
                System.out.printf("]");
                break;
            default:
                System.out.println("!setLocalMessage Error Type");
        }
    }
    //shut down
    private void close() {
        try {
            serverAlive = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
