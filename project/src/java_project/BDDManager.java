package java_project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Classe de gestion BDD via JDBC
 * @author Théo Machon 32
 * @author Stephen Sakovitch 32
 * @version 1.0
 */
class BDDManager {

    private Connection co;

    Connection getCo(){ return this.co; }

    /**
     * Ferme la BDD.
     */
    void fermeBase() {
        try {
            this.co.close();
        } catch (SQLException e) {
            System.out.println("{\"erreur\":\"Fermeture impossible\"}");
        }
    }

    /**
     * Constructeur.
     *  @param file Chemin d'acces au fichier BDD.
     */
    BDDManager(String file) {
        String url = "jdbc:sqlite:";
        url += file;
        try {
            co = DriverManager.getConnection(url);
        } catch(Exception err) {
            System.out.println("{\"erreur\":\"Impossible d'accéder à la BDD Sqlite\"}");
        }
    }
}
