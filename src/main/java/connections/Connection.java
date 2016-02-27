/*
 * Md. Momin Al Aziz momin.aziz.cse @ gmail.com	
 * http://www.mominalaziz.com
 */
package connections;

import java.util.Objects;
import java.util.Random;
import javax.websocket.Session;

/**
 *
 * @author shad942
 */
public class Connection {

    public enum ConnectionParty {

        Hospitals, CSP, Researcher
    }
    private boolean alive;
    private Session session;
    private String ip = "";
    private int port;
    private ConnectionParty cparty;
    private int id =0;

    public Connection(Session session, String ip, int port, ConnectionParty cparty, int id) {
        this.session = session;
        this.port = port;
        this.cparty = cparty;
        this.ip = ip;
        this.id = id;
        alive = true;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.ip);
        hash = 59 * hash + this.port;
        hash = 59 * hash + Objects.hashCode(this.cparty);
        hash = 59 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Connection other = (Connection) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public ConnectionParty getCtype() {
        return cparty;
    }

    public void setCtype(ConnectionParty ctype) {
        this.cparty = ctype;
    }

}
