package cod.ayesseye.textron0;

public class SyncLocationPacket {
    public String sessionId;
    public double lat;
    public double lng;
    public long timestamp;

    public SyncLocationPacket(String sessionId, double lat, double lng, long timestamp) {
        this.sessionId = sessionId;
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }
}