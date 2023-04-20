import java.util.ArrayList;

public class Room {
    /*
    Chat Room object
     */
    private String roomid;
    private int count;  //connecting users count
    private String owner; //room owner
    private ArrayList<String> users; //list of connecting users
    public Room(String name) {
        this.roomid = name;
        this.count = 0;
        this.users=new ArrayList<>();
    }

    public String getName() {
        return roomid;
    }
    public void setName(String name) {
        this.roomid = name;
    }
    public int getUserCount() {
        return users.size();
    }
    public void setUserCount(int userCount) {
        this.count = userCount;
    }
    public void setOwner(String owner) {
        this.owner = owner;
    }
    public String getOwner() {
        return owner;
    }
    public ArrayList<String> getUsers() {
        return users;
    }
    public void setUsers(ArrayList<String> users) {
        this.users = users;
    }
    public void addUsers(String clientId){
        users.add(clientId);
    }
    public void removeUser(String clientId){
        users.remove(clientId);
    }
    public Room getRoomObj(){
        return this;
    }
    public int getUserListCtn(){
        return users.size();
    }
}