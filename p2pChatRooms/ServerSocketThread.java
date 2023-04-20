import com.google.gson.Gson;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerSocketThread extends Thread{

    private Server server;
    private Socket socket;
    private boolean alive;
    private BufferedReader bufferedReader;
    private Msg msg;
    private PrintWriter printWriter;
    private String chatRoom;
    private String clientId;
    private String client_ownHostId;
    public Peer peer;
    //private boolean searchnetwork;
    public ServerSocketThread(Socket socket, Server server) throws Exception{
        this.socket=socket;
        this.server=server;
        this.alive=true;
//        this.peer = peer;
        //InetSocketAddress sockaddr = (InetSocketAddress)socket.getRemoteSocketAddress();
        //InetAddress inaddr = sockaddr.getAddress();
        //System.out.println(inaddr+" "+socket.getPort()); //same ip and port

        this.clientId=socket.getRemoteSocketAddress().toString().substring(1);
        //192.168.75.1:iport
        //this.clientId=InetAddress.getLocalHost().getHostAddress()+":"+socket.getRemoteSocketAddress().toString().split(":")[1];
        //System.out.println("!id ServerSocketThread clientId "+clientId);
        //192.168.1.101:iport
//        this.clientId=socket.getRemoteSocketAddress().toString().substring(1);
//        System.out.println("!id ServerSocketThread clientId "+clientId); //192.168.1.101:1072
        this.chatRoom="";

        printWriter=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF8"));

    }

    /*
    Public functions
     */
    @Override
    public void run(){
        while(alive){
            try {
                if(server.getBlackList()!=null && server.getBlackList().contains(this.clientId.split(":")[0])){
                    server.removeSocketConnection(this);
                    close();
                }
                bufferedReader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String input=bufferedReader.readLine();
                Gson gson=new Gson();
                msg=new Msg();
                msg=gson.fromJson(input,Msg.class);

                handleMsg(msg);

            }catch (Exception e){
                try {
                    if(getChatRoom() != ""){
                        System.out.println(this.clientId + " quit!");
                        server.removeSocket_Room(getChatRoom(),this);
                        server.removeUser_Room(getChatRoom(),getClientId());
                        msg=new Msg();
                        msg.setType("roomchange");
                        msg.setFormer(getChatRoom());
                        msg.setRoomid("");
                        msg.setIdentity(clientId);
                        server.deliverChatMsg(msg,getChatRoom(),this);
                    }
                    server.removeSocketConnection(this);
                    close();
                    break;
                    //server.setRoomEmptyOwner(getClientId());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
//                e.printStackTrace();
//                System.out.println("!6");
            }
        }

    }
/*
Getter
 */
    public String getChatRoom(){return this.chatRoom;}
    public String getClientId(){return this.clientId;}
    public String getClient_ownHostId(){return this.client_ownHostId;}
    /*
    Setter
     */
    public void setPeer(Peer peer){ this.peer = peer;}
    public void setChatRoom(String roomName){this.chatRoom=roomName;}
    private void setClientOwnHostId(String client_ownHostId){this.client_ownHostId=client_ownHostId;
    }
    //Send message
    public void sendMsg(String type,Msg data){
        try {
            Gson gson=new Gson();
            printWriter=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF8"));
            printWriter.write(gson.toJson(data)+"\n");
            printWriter.flush();
        }catch (Exception e){
            System.out.println("Error "+e.getMessage());
//            System.out.println("!13");
        }
    }
    //kick user
    public void kick(){
            System.out.println("Client " + getClientId() + " Kick");
            server.removeSocketConnection(this);
            close();
    }

    /*
    Private functions
    Handle request from the clients
     */
    private void handleMsg(Msg msg) throws Exception {
        String messageType = msg.getType();
        if ("".equals(msg) || msg == null) {
            return;
        }
        switch (messageType) {
            case "message":
//                System.out.println("ServerReceive MessageType");
                String content = msg.getContent();
                msg = new Msg();
                msg.setType("message");
                msg.setIdentity(getClientId());
                msg.setContent(content);
                server.deliverChatMsg(msg, getChatRoom(), this);
                break;
            case "hostchange":
                //System.out.println(getClientId() + " client hostchange: " + msg.getHost());
                setClientOwnHostId(msg.getHost());
                break;
            case "listneighbors":
                //System.out.println("Server receive List neighbors");
                msg = new Msg();
                msg.setType("neighbors");
                msg.setNeighbors(server.getConnections());
                List<String> t = new ArrayList<String>(server.getConnections());
//                if(t==null){
//                    System.out.println("Null Neighbors");
//                }else System.out.println("List " + t.size());
                sendMsg("~listneighbors", msg);
                break;
            case "list":
                msg = new Msg();
                msg.setType("roomlist");
                msg.setRooms(server.getRoomList());
                sendMsg("~RoomList", msg);
                break;
            case "who":
                String roomid_who = msg.getRoomid();
                msg = new Msg();
                msg.setType("roomcontents");
                msg.setRoomid(roomid_who);
                List<String> tempUser_list = server.getSingleRoomObj(roomid_who).getUsers();
                msg.setIdentities(tempUser_list);
                sendMsg("~WhoRoom", msg);
                break;
            case "join":
                String roomName = msg.getRoomid();
                String formerRoomName = getChatRoom();
                try {
                    //if the client wants to quit the room, stay connected
                    if (roomName.equals("")) {
                        server.removeUser_Room(getChatRoom(), getClientId());
                        server.removeSocket_Room(getChatRoom(), this);
                        msg = new Msg();
                        msg.setType("roomchange");
                        msg.setIdentity(getClientId());
                        msg.setFormer(getChatRoom());
                        msg.setRoomid("");
                        sendMsg("QuitRoom", msg);
                        server.deliverChatMsg(msg, getChatRoom(), this);
                        setChatRoom("");
                    } else if (server.getRooms().containsKey(roomName) && !roomName.equals(getChatRoom())) {
//                        System.out.println("~~Find the Room");
                        msg = new Msg();
                        msg.setType("roomchange");
                        msg.setIdentity(getClientId());
                        msg.setFormer(formerRoomName);
                        msg.setRoomid(roomName);
                        server.addSocket_Room(roomName, this);
                        if (formerRoomName != "") {
                            server.removeSocket_Room(getChatRoom(), this);
                            server.removeUser_Room(getChatRoom(), getClientId());
                            server.deliverChatMsg(msg, getChatRoom(), this);
                        }
                        sendMsg("roomChange", msg);
                        server.addUser_Room(roomName, getClientId());
                        server.deliverChatMsg(msg, roomName, this);
                        setChatRoom(roomName);
//                        System.out.println("~~RoomChanged, currently -> " + getChatRoom());
                        } else {
                            msg = new Msg();
                            msg.setType("roomchange");
                            msg.setIdentity(getClientId());
                            msg.setFormer(getChatRoom());
                            msg.setRoomid(getChatRoom());
                            sendMsg("!FAILROOMCHANGE", msg);
                        }
                    } catch (Exception e) {
                        System.out.println("!Join " + e.getMessage());
                        System.out.println("!14");
                    }
                    break;
                case "quit":
                    formerRoomName = getChatRoom();
                    msg = new Msg();
                    msg.setType("roomchange");
                    msg.setIdentity(getClientId());
                    msg.setFormer(getChatRoom());
                    msg.setRoomid("");
                    sendMsg("~Quit", msg);
                    if (formerRoomName != "") {
                        server.removeUser_Room(getChatRoom(), getClientId());
                        server.removeSocket_Room(getChatRoom(), this);
                        server.deliverChatMsg(msg, getChatRoom(), this);
                    }
                    setChatRoom("");
                    server.removeSocketConnection(this);
//                    System.out.println("Client " + getClientId() + " Quit");
                    close();
                    break;
                case "shout":
                    String shoutid = msg.getIdentity();
                    String shout = msg.getContent();
                    String shoutiden = msg.getIdentifier();
                    Integer identifier = peer.shout_map.get(shoutid);
                    if (identifier == null || !identifier.toString().equals(shoutiden)) {
                        if (peer.getInRoom()) {
                            System.out.println(shoutid + " " + shout);
                        }
                        peer.shout_map.put(shoutid, Integer.valueOf(shoutiden));
                        if (!server.getSelfConnect()) {
                            peer.client.sendMessage(msg);
                        }
                        server.deliverShout(msg, null);
                        break;
                    }
                default:
                    System.out.println("Error msg type");
            }

    }

    //shut down
    private void close() {
        try {
            this.alive = false;
            if (socket != null && !socket.isClosed()) {
                bufferedReader.close();
                printWriter.close();
                socket.close();
            }
        } catch (IOException e) {
//            System.out.println("!7");
//            System.out.println(e.getMessage());
            System.exit(0);
        }
    }
}
