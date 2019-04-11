import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Classe de gestion BDD via JDBC
 * @author Théo Machon 32
 * @author Stephen Sakovitch 32
 * @version 1.0
 */
public class BDDManager {

    private String url="jdbc:sqlite:";
    private Connection co;

    public Connection getCo(){ return this.co; }

    /**
     * Ferme la BDD.
     */
    public void fermeBase() {
        try {
            this.co.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructeur.
     *  @param file Chemin d'acces au fichier BDD.
     */
    public BDDManager(String file) {
        url += file;
        try {
            co = DriverManager.getConnection(url);
            //System.out.println("Connection successful !");
        } catch(Exception err) {
            System.err.println(err.getMessage());
            //TODO handle exception
        }
    }
}
