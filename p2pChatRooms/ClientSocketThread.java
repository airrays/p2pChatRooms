//import jdk.internal.jmod.JmodFile;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientSocketThread extends Thread{

    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private Socket socket;
    private Client client;
    private boolean alive;
    private Peer peer;
    private Gson gson;
    public ClientSocketThread(Client client,Peer peer) throws IOException {
        this.client=client;
        this.peer = peer;
        this.socket=client.getSocket();
        this.bufferedReader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.printWriter=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        alive=client.getAlive();
    }
    @Override
    public void run(){
        while(alive){
            try {
                gson=new Gson();
                String msg=bufferedReader.readLine();
                Msg message=gson.fromJson(msg,Msg.class);
//                System.out.println("received Msg from server "+msg);
                handleMsg(message);
            }catch (IOException e){
                System.out.println("socket Close");
                System.out.println("Server Closed");
                client.peer.chatRoom = "";
                client.setChatRoom("");
                client.setClientId("");
                client.setConnected(false);
                client.close();
                break;
            }
        }
    }
//Receive message from the server and display
    private void handleMsg(Msg msg){
//        System.out.println("["+client.getChatRoom()+"] "+client.getIpAddress()+":"+client.getPort()+" > ");
        try{
            switch (msg.getType()){
                case "message":
                    System.out.println(msg.getIdentity() +"> "+msg.getContent());
                    break;
                case "neighbors":
                    System.out.printf("neighbors:[");
                    for(String neighbors:msg.getNeighbors()){
                        if(!neighbors.equals(peer.getLocalId()))
                            System.out.printf(neighbors+", ");
                    }
                    System.out.printf("]");
                    break;
                case "roomchange":
                    String newroom = msg.getRoomid();
                    String former = msg.getFormer();
                    String identity = msg.getIdentity();
                    String id = client.getClientId();
                    boolean change=true;
                    boolean quit = client.getQuit();

                    if(quit){
                        if(id.equals(identity)) {
                            if(newroom.equals("")) {
                                client.close();
                                client.setConnected(false);
                                client.setChatRoom("");
                                client.peer.chatRoom = "";
                                client.peer.setInRoom(false);
                                System.out.println("Disconnected!");
                            }
                        }
                        else{
                            if (newroom.equals("")) {

                                System.out.println(identity + " Leave room " + former);
                            } else {

                                System.out.println(identity + " moved from " + former + " to " + newroom);
                            }
                        }
                    }
                    else{
                        if(former.equals(newroom)){
                            System.out.println("The requested room is invalid or non existent.");
//                            System.out.print("[" + former + "] " + identity + "> ");
                            change = false;
                        }
                        else{
                            if(id.equals(identity)) {
                                if (newroom.equals("")) {
                                    System.out.println("Leave room " + former);
                                    client.peer.setInRoom(false);
                                    client.peer.chatRoom = "";
                                    client.setChatRoom("");
                                } else {
                                    client.peer.setInRoom(true);
                                    client.peer.chatRoom = newroom;
                                    if(former.equals("")){
                                        System.out.println(identity + " moved to " + newroom);
                                    }
                                    else {
                                        System.out.println(identity + " moved from " + former + " to " + newroom);
                                    }
                                    client.setChatRoom(newroom);
                                }
                            }
                            else{
                                change = false;
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
                    }
//                    if(change){
//                        System.out.print("[" + client.getChatRoom() + "] " + id + "> ");
//                    }
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
//                    System.out.print("[" + client.getChatRoom() + "] " + client.getClientId() + "> ");
                    break;
                case "roomlist":
                    List<Room> rooms = msg.getRooms();
                    System.out.println("room list:");
                    for(int i = 0; i < rooms.size(); i++) {
                        String roomid = rooms.get(i).getName();
                        int count = rooms.get(i).getUserListCtn();
                        System.out.println(roomid + " " + count);
                    }
                    break;
                case "shout":
                    String  shoutid = msg.getIdentity();
                    String shout = msg.getContent();
                    String shoutiden = msg.getIdentifier();
                    Integer identifier = peer.shout_map.get(shoutid);
                    if(identifier == null || !identifier.toString().equals(shoutiden)){
                        peer.shout_map.put(shoutid,Integer.valueOf(shoutiden));
                        if(peer.getInRoom()){
                            System.out.println(shoutid + " " + shout);
                        }
                        peer.server.deliverShout(msg,null);
                    }
                    break;
                default:
                    System.out.println("Error Type");
            }
        }
        catch(Exception e){
            System.out.println("server disconnected.");
            alive = false;
            client.setConnected(false);
            client.peer.setInRoom(false);
            client.peer.chatRoom = "";
            client.setChatRoom("");
            client.close();
        }
    }
}
