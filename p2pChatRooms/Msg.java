import java.util.List;

public class Msg {
    private String roomid;
    private String content;
    private String identity;
    private List<String> identities;
    private String former;
    private List<String>neighbors;
    private String type;
    private List<Room>rooms;
    private String identifier;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    private String host;
    public void setIdentifier(String a){
        this.identifier = a;
    }
    public String getIdentifier(){return this.identifier;}
    public List<Room> getRooms() {
        return rooms;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoomid() {
        return roomid;
    }

    public void setRoomid(String roomid) {
        this.roomid = roomid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public List<String> getIdentities() {
        return identities;
    }

    public void setIdentities(List<String> identities) {
        this.identities = identities;
    }

    public String getFormer() {
        return former;
    }

    public void setFormer(String former) {
        this.former = former;
    }

    public List<String> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(List<String> neighbors) {
        this.neighbors = neighbors;
    }

}
