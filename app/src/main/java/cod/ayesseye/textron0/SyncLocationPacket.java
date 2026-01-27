package cod.ayesseye.textron0;

// Model de date simplu (POJO) utilizat pentru a impacheta informatiile GPS trimise prin retea
public class SyncLocationPacket {
    public String sessionId; // Identificatorul unic al sesiunii multiplayer
    public double lat;       // Latitudinea curenta
    public double lng;       // Longitudinea curenta
    public long timestamp;   // Momentul exact al trimiterii (pentru sincronizare)

    // Constructor pentru initializarea tuturor campurilor inainte de serializarea JSON
    public SyncLocationPacket(String sessionId, double lat, double lng, long timestamp) {
        this.sessionId = sessionId;
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }
}