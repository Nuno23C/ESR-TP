public class Packet {
    private String dest; // ip destino
    private String source; // ip da origem
    private int message; // tipo do packet

    public Packet(int message, String dest, String source) {
        this.message = message;
        this.dest = dest;
        this.source = source;
    }
    public String getDest() {
        return dest;
    }

    public String getSource() {
        return source;
    }
        
    public int getMessage() {
        return message;
    }
}