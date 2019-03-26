import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe de gestion BDD via JDBC
 * @author Machon
 * @author Sakovitch
 * @version 1.0
 */
public class BDDManager {

    private String url="jdbc:sqlite:";
    private Connection co;

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

    /**
     * Retourne le resultat d'une requete SQL.
     * @param reqSQL Requete SQL sous forme de String.
     * @return ResultSet de la reponse a la requete.
     */
    public ResultSet requete(String reqSQL) {

        try (ResultSet rs = co.createStatement().executeQuery(reqSQL)) {
            return rs;
        }
        catch (Exception err) {
            System.err.println(err.getMessage());
            return null;
            //TODO handle exception
        }
    }

    /**
     * Retourne le resultat d'une requete SQL sous forme d'un tableau 2D de String.
     * @param reqSQL Requete SQL sous forme de String.
     * @return Tableau 2D de String.<br>
     * La premiere ligne contient le nom des colonnes.<br>
     * Les autres lignes contiennent les valeurs.
     */
    public ArrayList<ArrayList<String>> requeteArray(String reqSQL) {

        try (ResultSet rs = co.createStatement().executeQuery(reqSQL)) {

            //variables
            ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
            ArrayList<String> tmp = new ArrayList<String>();
            int size = rs.getMetaData().getColumnCount();

            //1ere ligne: nom colonnes
            for (int i=0; i<size; i++) {
                tmp.add(rs.getMetaData().getColumnName(i+1));
            }
            result.add(new ArrayList<>(tmp));
            tmp.clear();

            //autres colonnes: valeurs
            while (rs.next()) {
                for (int i=0; i<size; i++) {
                    tmp.add(rs.getString(i+1));
                }
                result.add(new ArrayList<>(tmp));
                tmp.clear();
            }
            return result;
        }
        catch (Exception err) {
            System.err.println(err.getMessage());
            return null;
            //TODO handle exception
        }
    }
}
