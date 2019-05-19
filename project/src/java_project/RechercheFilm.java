package java_project;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Classe de recherche dans la BDD SQLite d'IMDB.<br>
 * @author Stephen Sakovitch 3A-32<br>
 * @author Theo Machon 3A-32<br>
 * @version 0.3
 */
public class RechercheFilm {

    /**
     * Accesseur a la BDD SQLite des films
     */
    public class BDDManager {
        private Connection co;

        private ArrayList<ArrayList<String>> ArrayResultSQL(String sql){
            PreparedStatement ps;
            ResultSet set;
            ArrayList<ArrayList<String>> liste = new ArrayList<>();
            try {
                ps = this.co.prepareStatement(sql);
                set = ps.executeQuery();
                if (set.next()) liste = convertRStoAL(set);
            }
            catch (SQLException e) { e.printStackTrace(); }
            return liste;
        }

        /**
         * Ferme la BDD
         */
        public void fermeBase() {
            try { this.co.close(); }
            catch (SQLException e) { System.out.println("{\"erreur\":\"Fermeture impossible\"}"); }
        }

        /**
         * Constructeur de l'accesseur a la BDD SQLite.
         *  @param file Chemin d'acc&egrave;s au fichier BDD
         */
        public BDDManager(String file) {
            String url = "jdbc:sqlite:";
            url += file;
            try { co = DriverManager.getConnection(url); }
            catch(Exception err) { System.out.println("{\"erreur\":\"Impossible d'accéder à la BDD Sqlite\"}"); }
        }
    }

    private boolean erreur = false;
    private String message_erreur = "";
    private BDDManager bdd;

    /**
     * Constructeur de la classe RechercheFilm et ouvre la BDD.
     * @param nomFichierSQLite Chemin et nom du ficher BDD.
     */
    public RechercheFilm(String nomFichierSQLite) {
        bdd = new BDDManager(nomFichierSQLite);
    }

    /**
     * M&eacute;thode permettant la traduction de la pseudo-requ&ecirc;te en requ&ecirc;te SQL, effectue la recherche dans la BDD SQLite et renvoie les films au format JSON.<br>
     * TITRE suivi d'un titre de film;<br>
     * DE suivi d'un nom de r&eacute;alisateur;<br>
     * AVEC suivi d'un nom d'acteur ou d'actrice;<br>
     * PAYS suivi d'un code (ISO sur deux lettres) ou nom de pays;<br>
     * EN suivi d'une ann&eacute;e de sortie;<br>
     * AVANT suivi d'une ann&eacute;e de sortie (correspond &agrave; &lt;, on ne traite pas &lt;= );<br>
     * APRES (ou APRES) suivi d'une ann&eacute;e de sortie (correspond &agrave; &gt;, on ne traite pas &gt;=).<br>
     * Les conditions ainsi exprim&eacute;es peuvent &ecirc;tre combin&eacute;es soit en les s&eacute;parant par une virgule ("et" implicite), soit avec OU.<br>
     * On peut omettre le mot-clef apr&egrave;s une virgule ou OU, dans ce cas c'est implicitement le m&ecirc;me type de crit&egrave;re que pr&eacute;c&eacute;demment qui s'applique.
     * @param requete Langage de recherce simplif&eacute;e:<br>
     * @return R&eacute;ponse de la recherche au format JSON.
     */
    public String retrouve(String requete) {
        String sql = formatRequest(requete);
        if (!this.erreur) return convertToJSON(getInfoFilmArray(sql));
        else return "{\"erreur\":\"" + this.message_erreur + "\"}";
    }

    /**
     * Convertit la pseudo-requ&ecirc;te en requ&ecirc;te SQL exploitable.
     * @param requete Pseudo-requ&ecirc;te
     * @return String Requete SQL cr&eacute;&eacute;e
     */
    public String formatRequest(String requete) {
        StringBuilder sql = new StringBuilder(), value= new StringBuilder();
        String SELECT = "SELECT f.id_film as id_film_f, prenom, p.nom as nom_p, f.titre as titre_f, duree, annee, py.nom as nom_py, role, (select group_concat(a_t.titre, '#') from autres_titres a_t where a_t.id_film=f.id_film) as liste_autres_titres";
        String FROM = "\nFROM films f NATURAL JOIN generique g NATURAL JOIN personnes p LEFT JOIN pays py ON f.pays = py.code";
        String ORDER_BY_SQL = "\nORDER BY annee DESC, f.titre";
        sql.append(SELECT).append(FROM);
        requete += ",END";
        String field = "";

        boolean where_created = false, TITRE_filled = false, PAYS_filled = false, EN_filled = false, newField = true, or_btwn_kw = false;

        ArrayList<String> tmpStorage = new ArrayList<>();
        String[] possibleTerms = {"TITRE", "DE", "AVEC", "PAYS", "EN", "AVANT", "APRES"};
        ArrayList<ArrayList<String>> array2D = new ArrayList<>();

        String[] list = requete.split(" |((?<=,)|(?=,))");
        label:
        for (int i = 0; i<list.length; i++) {
            String str = list[i].toUpperCase();
            if (newField) {
                if (Arrays.asList(possibleTerms).contains(str)) {
                    if ((str.equals("TITRE") && TITRE_filled) || (str.equals("EN") && EN_filled) || (str.equals("PAYS") && PAYS_filled)) {
                        this.erreur = true;
                        this.message_erreur = "Le mot-clef '" + str + "' n'accepte qu'une seule valeur : utilisez des 'OU'.";
                        break;
                    }
                    else if (list[i+1].equals("OU") || list[i+1].equals(",")) {
                        this.erreur = true;
                        this.message_erreur = "Une valeur ou plusieurs valeurs sont attendues apres le mot-clef " + str + ".";
                        break;
                    }
                    else if (!field.equals(str)) {
                        field = str;
                        newField = false;
                    }
                }
                else {
                    if (str.equals("END") || (i == 0 && str.equals(",") && list[i+1].equals("END"))) break;
                    else if (i == 0){
                        this.erreur = true;
                        this.message_erreur = "Le mot-clef '" + str + "' n'existe pas ...";
                        break;
                    }
                    else if (!field.matches("DE|AVEC") && field.matches("TITRE|PAYS|EN")) {
                        this.erreur = true;
                        this.message_erreur = "Le mot-clef '" + field + "' n'accepte qu'une seule valeur : utilisez des 'OU'.";
                        break;
                    }
                    else {
                        newField=false;
                        i--;
                    }
                }
            }
            else {
                if (str.equals("OU")) {
                    if (list[i+1].equals(",")) {
                        this.erreur = true;
                        this.message_erreur = "Une valeur est attendue apres le mot-clef 'OU'.";
                        break;
                    }
                    else if (value.length() == 0) {
                        this.erreur = true;
                        this.message_erreur = "Une valeur prealable est requise pour le mot-clef 'OU'.";
                        break;
                    }

                    else if(Arrays.asList(possibleTerms).contains(list[i+1]) && !field.equals(list[i+1])) {
                        tmpStorage.add(value.toString().trim());
                        value = new StringBuilder();

                        switch (field) {
                            case "TITRE":
                                if (!where_created) {
                                    where_created = true;
                                    sql.append("\nWHERE ((");
                                }
                                else if (or_btwn_kw){
                                    sql.append("\nOR (");
                                }
                                else sql.append("\nAND ((");

                                for (int j = 0; j < tmpStorage.size(); j++) {
                                    if (j > 0) sql.append(" OR");
                                    sql.append(" f.id_film IN (SELECT id_film FROM recherche_titre rt WHERE rt.titre LIKE '%' || replace('").append(tmpStorage.get(j)).append("', ' ', '%') || '%')");
                                }
                                sql.append(")");
                                TITRE_filled = true;
                                break;
                            case "DE": {
                                ArrayList<String> tmpStorage2 = new ArrayList<>();
                                for (String tmpVal : tmpStorage) {
                                    if (tmpVal.matches(".*\\d.*")) {
                                        this.erreur = true;
                                        this.message_erreur = "Une valeur numerique a ete saisie pour le mot-clef DE";
                                        break;
                                    }
                                    else tmpStorage2.add(tmpVal);
                                }
                                array2D.add(tmpStorage2);

                                for (ArrayList<String> strings : array2D) {
                                    if (!where_created) {
                                        where_created = true;
                                        sql.append("\nWHERE ((");
                                    }
                                    else if (or_btwn_kw){
                                        or_btwn_kw = false;
                                        sql.append("\nOR (");
                                    }
                                    else sql.append("\nAND ((");

                                    for (int k = 0; k < strings.size(); k++) {
                                        if (k > 0) sql.append("\nOR");
                                        sql.append(" f.id_film IN (SELECT id_film FROM personnes NATURAL JOIN generique");
                                        sql.append(" WHERE (prenom_sans_accent || ' ' || nom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent || ' ' || prenom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent LIKE '%").append(strings.get(k)).append("%')");
                                        sql.append(" AND role = 'R')");
                                    }
                                    sql.append(")");
                                }
                                array2D.clear();
                                break;
                            }
                            case "AVEC": {
                                ArrayList<String> tmpStorage2 = new ArrayList<>();
                                for (String tmpVal : tmpStorage) {
                                    if (tmpVal.matches(".*\\d.*")) {
                                        this.erreur = true;
                                        this.message_erreur = "Une valeur numerique a ete saisie pour le mot-clef AVEC";
                                        break;
                                    }
                                    else tmpStorage2.add(tmpVal);
                                }

                                array2D.add(tmpStorage2);
                                for (ArrayList<String> strings : array2D) {
                                    if (!where_created) {
                                        where_created = true;
                                        sql.append("\nWHERE ((");
                                    }
                                    else if (or_btwn_kw){
                                        or_btwn_kw = false;
                                        sql.append("\nOR (");
                                    }
                                    else sql.append("\nAND ((");

                                    for (int k = 0; k < strings.size(); k++) {
                                        if (k > 0) sql.append("\nOR");
                                        sql.append(" f.id_film IN (SELECT id_film FROM personnes NATURAL JOIN generique");
                                        sql.append(" WHERE (prenom_sans_accent || ' ' || nom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent || ' ' || prenom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent LIKE '%").append(strings.get(k)).append("%')");
                                        sql.append(" AND role = 'A')");
                                    }
                                    sql.append(")");
                                }
                                array2D.clear();
                                break;
                            }
                            case "PAYS":
                                if (tmpStorage.get(0).matches(".*\\d.*")) {
                                    this.erreur = true;
                                    this.message_erreur = "Une valeur numerique a ete saisie pour le mot-clef PAYS";
                                    break label;
                                }
                                else {
                                    if (!where_created) {
                                        where_created = true;
                                        sql.append("\nWHERE ((");
                                    }
                                    else if (or_btwn_kw){
                                        sql.append("\nOR (");
                                    }
                                    else sql.append("\nAND ((");

                                    for (int j = 0; j < tmpStorage.size(); j++) {
                                        if (j > 0) sql.append("\nOR");
                                        sql.append(" py.code LIKE '%").append(tmpStorage.get(j)).append("%' OR py.nom LIKE '%").append(tmpStorage.get(j)).append("%'");
                                    }
                                    sql.append(")");
                                }
                                PAYS_filled = true;
                                break;
                            case "EN": {
                                ArrayList<Integer> tmpStorage2 = new ArrayList<>();
                                for (String tmpVal : tmpStorage) {
                                    try {
                                        tmpStorage2.add(Integer.valueOf(tmpVal));
                                    }
                                    catch (NumberFormatException err) {
                                        this.erreur = true;
                                        this.message_erreur = "Une valeur non-numerique a ete saisie pour le mot-clef EN : [" + err.getMessage().replace('\"', '\'') + "]";
                                        break;
                                    }
                                }

                                if (!where_created) {
                                    where_created = true;
                                    sql.append("\nWHERE ((");
                                }
                                else if (or_btwn_kw){
                                    sql.append("\nOR (");
                                }
                                else sql.append("\nAND ((");

                                for (int j = 0; j < tmpStorage2.size(); j++) {
                                    if (j > 0) sql.append("\nOR");
                                    sql.append(" annee = ").append(tmpStorage2.get(j));
                                }
                                sql.append(")");
                                EN_filled = true;
                                break;
                            }
                            case "AVANT":
                                ArrayList<Integer> tmpAvant = new ArrayList<>();
                                for (String tmpVal : tmpStorage) {
                                    try {
                                        tmpAvant.add(Integer.valueOf(tmpVal));
                                    }
                                    catch (NumberFormatException err) {
                                        this.erreur = true;
                                        this.message_erreur = "Une valeur non-numerique a ete saisie pour le mot-clef AVANT : '" + tmpVal + "'.";
                                        break;
                                    }
                                }

                                if (!where_created) {
                                    where_created = true;
                                    sql.append("\nWHERE ((");
                                }
                                else if (or_btwn_kw){
                                    sql.append("\nOR (");
                                }
                                else sql.append("\nAND ((");

                                if (tmpAvant.size() > 0) sql.append(" annee < ").append(Collections.max(tmpAvant)).append(")");
                                break;
                            case "APRES":
                                ArrayList<Integer> tmpApres = new ArrayList<>();
                                for (String tmpVal : tmpStorage) {
                                    try {
                                        tmpApres.add(Integer.valueOf(tmpVal));
                                    }
                                    catch (NumberFormatException err) {
                                        this.erreur = true;
                                        this.message_erreur = "Une valeur non-numerique a ete saisie pour le mot-clef APRES : '" + tmpVal + "'.";
                                        break;
                                    }
                                }
                                if (!where_created) {
                                    where_created = true;
                                    sql.append("\nWHERE ((");
                                }
                                else if (or_btwn_kw){
                                    sql.append("\nOR (");
                                }
                                else sql.append("\nAND ((");

                                if (tmpApres.size() > 0) sql.append(" annee > ").append(Collections.min(tmpApres)).append(")");
                                break;
                        }

                        or_btwn_kw = true;
                        newField = true;
                        tmpStorage.clear();
                    }
                    else if (!Arrays.asList(possibleTerms).contains(str)){
                        tmpStorage.add(value.toString().trim());
                        value = new StringBuilder();
                    }
                }
                else if (str.equals(",")) {
                    if (list[i+1].equals(",")) {
                        this.erreur = true;
                        this.message_erreur = "Surplus d'une virgule avant '" + list[i-1] + "'. Sinon, une valeur est attendue.";
                        break;
                    }
                    else if (list[i+1].equals("OU")) {
                        this.erreur = true;
                        this.message_erreur = "Plusieurs valeurs sont attendues apres une virgule. Sinon, supprimez-la";
                        break;
                    }

                    tmpStorage.add(value.toString().trim());
                    value = new StringBuilder();

                    switch (field) {
                        case "TITRE":
                            if (!where_created) {
                                where_created = true;
                                sql.append("\nWHERE ((");
                            }
                            else if (or_btwn_kw){
                                or_btwn_kw = false;
                                sql.append("\nOR (");
                            }
                            else sql.append("\nAND ((");

                            for (int j = 0; j < tmpStorage.size(); j++) {
                                if (j > 0) sql.append(" OR");
                                sql.append(" f.id_film IN (SELECT id_film FROM recherche_titre rt WHERE rt.titre LIKE '%' || replace('").append(tmpStorage.get(j)).append("', ' ', '%') || '%')");
                            }
                            sql.append(")");
                            TITRE_filled = true;
                            break;
                        case "DE": {
                            ArrayList<String> tmpStorage2 = new ArrayList<>();
                            for (String tmpVal : tmpStorage) {
                                if (tmpVal.matches(".*\\d.*")) {
                                    this.erreur = true;
                                    this.message_erreur = "Une valeur numerique a ete saisie pour le mot-clef DE";
                                    break;
                                }
                                else tmpStorage2.add(tmpVal);
                            }
                            array2D.add(tmpStorage2);

                            for (ArrayList<String> strings : array2D) {
                                if (!where_created) {
                                    sql.append("\nWHERE ((");
                                    where_created = true;
                                }
                                else if (or_btwn_kw) {
                                    or_btwn_kw = false;
                                    sql.append("\nOR (");
                                }
                                else sql.append("\nAND ((");

                                for (int k = 0; k < strings.size(); k++) {
                                    if (k > 0) sql.append("\nOR");
                                    sql.append(" f.id_film IN (SELECT id_film FROM personnes NATURAL JOIN generique");
                                    sql.append(" WHERE (prenom_sans_accent || ' ' || nom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent || ' ' || prenom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent LIKE '%").append(strings.get(k)).append("%')");
                                    sql.append(" AND role = 'R')");
                                }
                                sql.append(")");
                            }
                            array2D.clear();
                            break;
                        }
                        case "AVEC": {
                            ArrayList<String> tmpStorage2 = new ArrayList<>();
                            for (String tmpVal : tmpStorage) {
                                if (tmpVal.matches(".*\\d.*")) {
                                    this.erreur = true;
                                    this.message_erreur = "Une valeur numerique a ete saisie pour le mot-clef AVEC";
                                    break;
                                }
                                else tmpStorage2.add(tmpVal);
                            }

                            array2D.add(tmpStorage2);
                            for (ArrayList<String> strings : array2D) {
                                if (!where_created) {
                                    sql.append("\nWHERE ((");
                                    where_created = true;
                                }
                                else if (or_btwn_kw){
                                    or_btwn_kw = false;
                                    sql.append("\nOR (");
                                }
                                else sql.append("\nAND ((");

                                for (int k = 0; k < strings.size(); k++) {
                                    if (k > 0) sql.append("\nOR");
                                    sql.append(" f.id_film IN (SELECT id_film FROM personnes NATURAL JOIN generique");
                                    sql.append(" WHERE (prenom_sans_accent || ' ' || nom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent || ' ' || prenom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent LIKE '%").append(strings.get(k)).append("%')");
                                    sql.append(" AND role = 'A')");
                                }
                                sql.append(")");
                            }
                            array2D.clear();
                            break;
                        }
                        case "PAYS":
                            if (tmpStorage.get(0).matches(".*\\d.*")) {
                                this.erreur = true;
                                this.message_erreur = "Une valeur numerique a ete saisie pour le mot-clef PAYS.";
                                break label;
                            }
                            else {
                                if (!where_created) {
                                    sql.append("\nWHERE ((");
                                    where_created = true;
                                }
                                else if (or_btwn_kw){
                                    or_btwn_kw = false;
                                    sql.append("\nOR (");
                                }
                                else sql.append("\nAND ((");

                                for (int j = 0; j < tmpStorage.size(); j++) {
                                    if (j > 0) sql.append("\nOR");
                                    sql.append(" py.code LIKE '%").append(tmpStorage.get(j)).append("%' OR py.nom LIKE '%").append(tmpStorage.get(j)).append("%'");
                                }
                                sql.append(")");
                            }
                            PAYS_filled = true;
                            break;
                        case "EN": {
                            ArrayList<Integer> tmpStorage2 = new ArrayList<>();
                            for (String tmpVal : tmpStorage) {
                                try {
                                    tmpStorage2.add(Integer.valueOf(tmpVal));
                                }
                                catch (NumberFormatException err) {
                                    this.erreur = true;
                                    this.message_erreur = "Une valeur non-numerique a ete saisie pour le mot-clef EN : '" + tmpVal + "'.";
                                    break;
                                }
                            }

                            if (!where_created) {
                                where_created = true;
                                sql.append("\nWHERE ((");
                            }
                            else if (or_btwn_kw){
                                or_btwn_kw = false;
                                sql.append("\nOR (");
                            }
                            else sql.append("\nAND ((");

                            for (int j = 0; j < tmpStorage2.size(); j++) {
                                if (j > 0) sql.append("\nOR");
                                sql.append(" annee = ").append(tmpStorage2.get(j));
                            }
                            sql.append(")");
                            EN_filled = true;
                            break;
                        }
                        case "AVANT":
                            ArrayList<Integer> tmpAvant = new ArrayList<>();
                            for (String tmpVal : tmpStorage) {
                                try {
                                    tmpAvant.add(Integer.valueOf(tmpVal));
                                }
                                catch (NumberFormatException err) {
                                    this.erreur = true;
                                    this.message_erreur = "Une valeur non-numerique a ete saisie pour le mot-clef AVANT : '" + tmpVal + "'.";
                                    break;
                                }
                            }

                            if (!where_created) {
                                where_created = true;
                                sql.append("\nWHERE ((");
                            }
                            else if (or_btwn_kw){
                                or_btwn_kw = false;
                                sql.append("\nOR (");
                            }
                            else sql.append("\nAND ((");

                            if (tmpAvant.size() > 0) sql.append(" annee < ").append(Collections.max(tmpAvant)).append(")");
                            break;
                        case "APRES":
                            ArrayList<Integer> tmpApres = new ArrayList<>();
                            for (String tmpVal : tmpStorage) {
                                try {
                                    tmpApres.add(Integer.valueOf(tmpVal));
                                }
                                catch (NumberFormatException err) {
                                    this.erreur = true;
                                    this.message_erreur = "Une valeur non-numerique a ete saisie pour le mot-clef APRES : '" + tmpVal + "'.";
                                    break;
                                }
                            }
                            if (!where_created) {
                                where_created = true;
                                sql.append("\nWHERE ((");
                            }
                            else if (or_btwn_kw){
                                or_btwn_kw = false;
                                sql.append("\nOR (");
                            }
                            else sql.append("\nAND ((");

                            if (tmpApres.size() > 0) sql.append(" annee > ").append(Collections.min(tmpApres)).append(")");
                            break;
                    }
                    if (!list[i+1].equals("OU")) sql.append(")");
                    newField = true;
                    tmpStorage.clear();
                }
                else if (!Arrays.asList(possibleTerms).contains(str)) value.append(str).append(" ");
            }
        }
        sql.append(ORDER_BY_SQL);
        return sql.toString();
    }

    /**
     * Cette fonction permet de retourner un tableau d'InfoFilm &agrave; partir d'une requ&ecirc;te SQL.<br><br>
     * Ordre des colonnes dans le resultSet (faire -1 pour l'ArrayList) :<br>
     * [1] f.id_film (ID du film)<br>
     * [2] prenom [3] p.nom (pr&eacute;nom/nom d'une personne)<br>
     * [4] titre (titre du film)<br>
     * [5] duree (dur&eacute;e du film en minutes)<br>
     * [6] annee (ann&eacute;e de sortie du film)<br>
     * [7] py.nom (nom du pays en entier)<br>
     * [8] role (r&ocirc;le de la personne =&gt; 'A' : acteur, 'R' : r&eacute;alisateur)<br>
     * [9] liste des autres titres sur une ligne<br>
     * @param sql ResultSet de la requ&ecirc;te SQL construite &agrave; partir du pseudo-langage<br>
     * @return ArrayList&lt;java_project.InfoFilm&gt; liste des films
     */
    public ArrayList<InfoFilm> getInfoFilmArray(String sql){
        ArrayList<InfoFilm> filmsList = new ArrayList<>();
        ArrayList<ArrayList<String>> liste = bdd.ArrayResultSQL(sql);
        ArrayList<NomPersonne> realisateurs = new ArrayList<>(), acteurs = new ArrayList<>();
        ArrayList<String> autres_titres = new ArrayList<>();
        int duree, annee;
        String pays, titre;

        for (int i = 0; i < liste.size(); i++) {
            if (liste.get(i).get(7).equals("A")) {
                String prenom_act = (liste.get(i).get(1) != null) ? liste.get(i).get(1) : "";
                acteurs.add(new NomPersonne(prenom_act, liste.get(i).get(2)));
            }
            else if (liste.get(i).get(7).equals("R")) {
                String prenom_real = (liste.get(i).get(1) != null) ? liste.get(i).get(1) : "";
                realisateurs.add(new NomPersonne(prenom_real, liste.get(i).get(2)));
            }

            titre = liste.get(i).get(3);
            duree = (liste.get(i).get(4) != null) ? Integer.valueOf(liste.get(i).get(4)) : 0;
            annee = (liste.get(i).get(5) != null) ? Integer.valueOf(liste.get(i).get(5)) : 0;
            pays = (liste.get(i).get(6) != null) ? liste.get(i).get(6) : "";

            if (liste.get(i).get(8) != null && autres_titres.isEmpty()) {
                String[] autres_titres_list_splited = liste.get(i).get(8).split("#");
                Collections.addAll(autres_titres, autres_titres_list_splited);
            }

            if (i == (liste.size()-1) || !Integer.valueOf(liste.get(i).get(0)).equals(Integer.valueOf(liste.get(i + 1).get(0)))) {
                filmsList.add(new InfoFilm(titre, realisateurs, acteurs, pays, annee, duree, autres_titres));
                acteurs = new ArrayList<>();
                realisateurs = new ArrayList<>();
                autres_titres = new ArrayList<>();
            }
        }

        bdd.fermeBase();
        return filmsList;
    }

    /**
     * Permet de convertir le ResultSet en ArrayList&lt;ArrayList&lt;String&gt;&gt; car le driver jdbc ne permet pas de changer les param&egrave;tres de lecture du ResultSet et il n'est pas possible d'acc&eacute;der a n+1 avec isNext() pour savoir si le prochain film est une nouvelle entr&eacute;e.
     * @param set ResultSet obtenu
     * @return ArrayList&lt;ArrayList&lt;String&gt;&gt; ResultSet converti
     * @throws SQLException Se lance si le ResultSet est vide
     */
    public ArrayList<ArrayList<String>> convertRStoAL(ResultSet set) throws SQLException {
        ArrayList<ArrayList<String>> set_to_at = new ArrayList<>();
        do {
            ArrayList<String> liste_simple = new ArrayList<>();
            for (int i = 1; i <= 9; i++) liste_simple.add(set.getString(i));
            set_to_at.add(liste_simple);
        } while (set.next());
        set.close();
        return set_to_at;
    }

    /**
     * Permet de formatter le JSON final lisible par un lecteur de JSON, comme 'jq' dans le terminal.
     * @param list Tableau d'InfoFilm
     * @return String JSON enfin retourn&eacute;
     */
    public String convertToJSON(ArrayList<InfoFilm> list) {
        StringBuilder result = new StringBuilder();
        int n;
        if (list.size() >= 100) {
            n = 100;
            result.append("{\"info\":\"Resultat limite a 100 films\", \"resultat\":[ ");
        }
        else {
            n = list.size();
            result.append("{\"resultat\":[");
        }

        for (int i = 0; i < n; i++) {
            if (i > 0) result.append(",\n");
            result.append(list.get(i).toString());
        }
        result.append("]}");
        return result.toString();
    }
}
