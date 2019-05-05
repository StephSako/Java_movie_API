package java_project;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Classe de recherche simplifiée sur la BDD IMDB.
 * @author Théo Machon 32
 * @author Stephen Sakovitch 32
 * @version 0.1
 */
class RechercheFilm {

    private boolean erreur = false;
    private String message_erreur = "";

    private BDDManager bdd;

    /**
     * Constructeur, ouvre la BDD.
     * @param nomFichierSQLite Nom du ficher BDD.
     */
    RechercheFilm(String nomFichierSQLite) {
        bdd = new BDDManager(nomFichierSQLite);
    }

    /**
     * Effectue une recherche dans la BDD.
     * @param requete Langage de recherce simplifee:<br>
     * TITRE suivi d'un titre de film;<br>
     * DE suivi d'un nom de réalisateur;<br>
     * AVEC suivi d'un nom d'acteur ou d'actrice;<br>
     * PAYS suivi d'un code (ISO sur deux lettres) ou nom de pays;<br>
     * EN suivi d'une annee de sortie;<br>
     * AVANT suivi d'une annee de sortie (correspond a <, on ne traite pas <=);<br>
     * APRES (ou APRÈS) suivi d'une annee de sortie (correspond a >, on ne traite pas >=).<br>
     * Les conditions ainsi exprimees peuvent être combinees soit en les separant par une virgule ("et" implicite), soit avec OU.<br>
     * On peut omettre le mot-clef apres une virgule ou OU, dans ce cas c'est implicitement le meme type de critere que precedemment qui s'applique.
     * @return Reponse de la recherche au format JSON.
     */
    String retrouve(String requete) {

        String sql = formatRequest(requete);
        if (!this.erreur){
            //System.out.println(sql);
            ArrayList<InfoFilm> list = getInfoFilmArray(sql);
            return convertToJSON(list);
        } else return "{\"erreur\":\"" + this.message_erreur + "\"}"; // Envoi de l'erreur

    }

    private String formatRequest(String requete) { // TODO En cours

        StringBuilder sql = new StringBuilder();
        String SELECT = "SELECT f.id_film as id_film_f, prenom, p.nom as nom_p, f.titre as titre_f, duree, annee, py.nom as nom_py, role, (select group_concat(a_t.titre, '#') from autres_titres a_t where a_t.id_film=f.id_film) as liste_autres_titres";
        String FROM = "\nFROM films f NATURAL JOIN generique g NATURAL JOIN personnes p LEFT JOIN pays py ON f.pays = py.code";
        String ORDER_BY_SQL = "\nORDER BY annee DESC, f.titre";

        sql.append(SELECT).append(FROM);

        boolean where_created = false, TITRE_filled = false, PAYS_filled = false, EN_filled = false;
        requete += ",END";

        String field="";
        StringBuilder value= new StringBuilder();
        ArrayList<String> tmpStorage = new ArrayList<>();
        boolean newField = true;
        String[] possibleTerms = {"TITRE", "DE", "AVEC", "PAYS", "EN", "AVANT", "APRES"};
        ArrayList<ArrayList<String>> AVEC = new ArrayList<>();
        ArrayList<ArrayList<String>> DE = new ArrayList<>();

        String[] list = requete.split(" |((?<=,)|(?=,))");
        label:
        for (int i = 0; i<list.length; i++) //pour chaque mot de la recherche
        {
            String str = list[i];
            if (newField) //si on regarde un mot-clef
            {
                if (Arrays.asList(possibleTerms).contains(str)) //si le mot-clef fait partie des mots-clefs valables
                {
                    //s'il s'agit d'un champ qui ne peut pas prendre de ET et que le champ est deja pris
                    if (str.equals("TITRE") && TITRE_filled) {
                        this.erreur = true;
                        this.message_erreur = "multiples champs TITRE";
                        break;
                    }
                    else if (str.equals("PAYS") && PAYS_filled) {
                        this.erreur = true;
                        this.message_erreur = "multiples champs PAYS";
                        break;
                    }
                    else if (str.equals("EN") && EN_filled) {
                        this.erreur = true;
                        this.message_erreur = "multiples champs EN";
                        break;
                    }
                    else //si tout va bien pour le mot clef
                    {
                        field = str;
                        newField = false;
                    }
                }
                else //si le mot n'est pas un mot-clef valable
                {
                    if (str.equals("END")) //si on a atteint le mot-clef de la fin
                    {
                        break;
                    }
                    else if (i==0) //si c'est le 1er mot-clef de la requete
                    {
                        this.erreur = true;
                        this.message_erreur = "1er champ invalide";
                        break;
                    }
                    else if (!field.matches("DE|AVEC")) //si on pas de mot clef et qu'il ne s'agit ni de "DE", ni de "AVEC"
                    {
                        if (field.equals("TITRE") && TITRE_filled) {
                            this.erreur = true;
                            this.message_erreur = "multiples champs TITRE";
                            break;
                        }
                        else if (field.equals("PAYS") && PAYS_filled) {
                            this.erreur = true;
                            this.message_erreur = "multiples champs PAYS";
                            break;
                        }
                        else if (field.equals("EN") && EN_filled) {
                            this.erreur = true;
                            this.message_erreur = "multiples champs EN";
                            break;
                        }
                        else {
                            this.erreur = true;
                            this.message_erreur = "mot-clef de champ invalide : " + str;
                            break;
                        }
                    }
                    else //si on a une autre valeur après un "DE" ou un "AVEC"
                    {
                        newField=false;
                        i--;
                    }
                }
            }
            else //si on regarde la valeur d'un champ
            {
                if (str.equals("OU")) //si le mot actuel est "OU"
                {
                    if (value.length() == 0) {
                        this.erreur = true;
                        this.message_erreur = "champ 'OU' sans valeur préalable";
                        break;
                    }
                    else {
                        tmpStorage.add(value.toString().trim());
                        value = new StringBuilder();
                    }
                }
                else if (list[i].equals(","))  //si le "mot" actuel est une virgule
                {
                    tmpStorage.add(value.toString().trim());
                    value = new StringBuilder();

                    switch (field) {
                        case "TITRE":
                            if (!where_created) {
                                where_created = true;
                                sql.append("\nWHERE (");
                            } else sql.append("\nAND (");

                            for (int j = 0; j < tmpStorage.size(); j++) {
                                if (j > 0) sql.append(" OR");
                                sql.append(" f.id_film IN (SELECT id_film FROM recherche_titre rt WHERE rt.titre LIKE '%' ||	replace('").append(tmpStorage.get(j)).append("', ' ', '%') || '%')");
                            }
                            sql.append(")");
                            TITRE_filled = true;
                            break;
                        case "DE": {
                            ArrayList<String> tmpStorage2 = new ArrayList<>();
                            for (String tmpVal : tmpStorage) {
                                if (tmpVal.matches(".*\\d.*")) //si le valeur contient un nombre
                                {
                                    this.erreur = true;
                                    this.message_erreur = "valeur numerique dans le champ 'DE'";
                                    break;
                                } else tmpStorage2.add(tmpVal);
                            }
                            DE.add(tmpStorage2);

                            for (ArrayList<String> strings : DE) {
                                if (!where_created) {
                                    sql.append("\nWHERE (");
                                    where_created = true;
                                } else sql.append("\nAND (");

                                for (int k = 0; k < strings.size(); k++) {
                                    if (k > 0) sql.append("\nOR");
                                    sql.append(" f.id_film IN (SELECT id_film FROM personnes NATURAL JOIN generique");
                                    sql.append(" WHERE (prenom_sans_accent || ' ' || nom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent || ' ' || prenom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent LIKE '%").append(strings.get(k)).append("%')");
                                    sql.append(" AND role = 'R')");
                                }
                                sql.append(")");
                            }
                            DE.clear();
                            break;
                        }
                        case "AVEC": {
                            ArrayList<String> tmpStorage2 = new ArrayList<>();
                            for (String tmpVal : tmpStorage) {
                                if (tmpVal.matches(".*\\d.*")) //si la valeur contient un nombre
                                {
                                    this.erreur = true;
                                    this.message_erreur = "valeur numerique dans le champ 'AVEC'";
                                    break;
                                } else tmpStorage2.add(tmpVal);
                            }

                            AVEC.add(tmpStorage2);
                            for (ArrayList<String> strings : AVEC) {
                                if (!where_created) {
                                    sql.append("\nWHERE (");
                                    where_created = true;
                                } else sql.append("\nAND (");

                                for (int k = 0; k < strings.size(); k++) {
                                    if (k > 0) sql.append("\nOR");
                                    sql.append(" f.id_film IN (SELECT id_film FROM personnes NATURAL JOIN generique");
                                    sql.append(" WHERE (prenom_sans_accent || ' ' || nom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent || ' ' || prenom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent LIKE '%").append(strings.get(k)).append("%')");
                                    sql.append(" AND role = 'A')");
                                }
                                sql.append(")");
                            }
                            AVEC.clear();
                            break;
                        }
                        case "PAYS":
                            if (tmpStorage.get(0).matches(".*\\d.*")) //si le valeur contient un nombre
                            {
                                this.erreur = true;
                                this.message_erreur = "valeur numerique dans le champ 'DE'";
                                break label;
                            } else {
                                if (!where_created) {
                                    sql.append("\nWHERE (");
                                    where_created = true;
                                } else sql.append("\nAND (");

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
                                } catch (NumberFormatException err) {
                                    this.erreur = true;
                                    this.message_erreur = "champ 'EN' non numerique [" + err.getMessage().replace('\"', '\'') + "]";
                                    break;
                                }
                            }

                            if (!where_created) {
                                where_created = true;
                                sql.append("\nWHERE (");
                            } else sql.append("\nAND (");

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
                                } catch (NumberFormatException err) {
                                    this.erreur = true;
                                    this.message_erreur = "champ 'AVANT' non numerique [" + err.getMessage().replace('\"', '\'') + "]";
                                    break;
                                }
                            }

                            if (!where_created) {
                                where_created = true;
                                sql.append("\nWHERE (");
                            } else sql.append("\nAND (");

                            if (tmpAvant.size() > 0) sql.append(" annee < ").append(Collections.max(tmpAvant)).append(")");
                            break;
                        case "APRES":
                            ArrayList<Integer> tmpApres = new ArrayList<>();
                            for (String tmpVal : tmpStorage) {
                                try {
                                    tmpApres.add(Integer.valueOf(tmpVal));
                                } catch (NumberFormatException err) {
                                    this.erreur = true;
                                    this.message_erreur = "champ 'APRES' non numerique [" + err.getMessage().replace('\"', '\'') + "]";
                                    break;
                                }
                            }
                            if (!where_created) {
                                where_created = true;
                                sql.append("\nWHERE (");
                            } else sql.append("\nAND (");

                            if (tmpApres.size() > 0) sql.append(" annee > ").append(Collections.min(tmpApres)).append(")");
                            break;
                    }

                    newField = true;
                    tmpStorage.clear();

                }
                else //si le mot actuel fait partie de la valeur du champ
                {
                    value.append(str).append(" ");
                }
            }
        }
        sql.append(ORDER_BY_SQL);
        return sql.toString();
    }

    /**
     * Ordre des colonnes dans le resultSet (-1 pour l'ArrayList) :
     * [1] f.id_film (ID du film)
     * [2] prenom [3] p.nom (prénom/nom d'une personne)
     * [4] titre (titre du film)
     * [5] duree (duree du film en minutes)
     * [6] annee (annee de sortie du film)
     * [7] py.nom (nom du pays en entier)
     * [8] role (role de la personne => 'A' : acteur, 'R' : réalisateur)
     * [9] liste des autres titres sur une ligne
     * @param sql resultSet de la requête SQL construite à partir du pseudo-langage
     * @return ArrayList<java_project.InfoFilm> liste des films
     */
    private ArrayList<InfoFilm> getInfoFilmArray(String sql) {
        ArrayList<InfoFilm> filmsList = new ArrayList<>();
        ResultSet set;

        try{
            set = bdd.getCo().createStatement().executeQuery(sql);

            if (set.next()) { // Verifie si le ResultSet contient au moins un résultat
                ArrayList<ArrayList<String>> liste = convertRStoAL(set);

                // Champs de la classe java_project.InfoFilm
                ArrayList<NomPersonne> realisateurs = new ArrayList<>();
                ArrayList<NomPersonne> acteurs = new ArrayList<>();
                ArrayList<String> autres_titres = new ArrayList<>();
                int duree, annee;
                String pays, titre;

                for( int i = 0; i < liste.size(); i++) {

                    if (liste.get(i).get(7).equals("A")) {
                        String prenom_act = "";
                        if (liste.get(i).get(1) != null) prenom_act = liste.get(i).get(1);
                        acteurs.add(new NomPersonne(prenom_act, liste.get(i).get(2)));
                    } else if (liste.get(i).get(7).equals("R")) {
                        String prenom_real = "";
                        if (liste.get(i).get(1) != null) prenom_real = liste.get(i).get(1);
                        realisateurs.add(new NomPersonne(prenom_real, liste.get(i).get(2)));
                    }

                    titre = liste.get(i).get(3);

                    duree = (liste.get(i).get(4) != null) ? Integer.valueOf(liste.get(i).get(4)) : 0;
                    annee = (liste.get(i).get(5) != null) ? Integer.valueOf(liste.get(i).get(5)) : 0;
                    pays = (liste.get(i).get(5) != null) ? liste.get(i).get(6) : "";

                    if (liste.get(i).get(8) != null && autres_titres.isEmpty()) {
                        String[] autres_titres_list_splited = liste.get(i).get(8).split("#");
                        Collections.addAll(autres_titres, autres_titres_list_splited);
                    }

                    // Nouveau film lu ou fin de la liste : on créé et ajoute une nouvelle instance d'java_project.InfoFilm dans l'ArrayList
                    if (i == (liste.size()-1) || !Integer.valueOf(liste.get(i).get(0)).equals(Integer.valueOf(liste.get(i + 1).get(0)))) {
                        filmsList.add(new InfoFilm(titre, realisateurs, acteurs, pays, annee, duree, autres_titres));

                        // On vide les tableaux pour passer au film suivant
                        acteurs = new ArrayList<>();
                        realisateurs = new ArrayList<>();
                        autres_titres = new ArrayList<>();
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        bdd.fermeBase();

        return filmsList;
    }

    private ArrayList<ArrayList<String>> convertRStoAL(ResultSet set) throws SQLException {
        ArrayList<ArrayList<String>> set_to_at = new ArrayList<>();

        do {
            ArrayList<String> liste_simple = new ArrayList<>();
            for (int i = 1; i <= 9; i++) liste_simple.add(set.getString(i));
            set_to_at.add(liste_simple);
        } while (set.next());

        set.close();
        return set_to_at;
    }

    private String convertToJSON(ArrayList<InfoFilm> list) {
        StringBuilder result = new StringBuilder();

        int n;
        if (list.size() >= 100){
            n = 100;
            result.append("{\"info\":\"Résultat limité à 100 films\", \"resultat\":[ ");
        }
        else{
            n = list.size();
            result.append("{\"resultat\":[ ");
        }

        for (int i = 0; i < n; i++) {
            if (i > 0) result.append(",\n");
            result.append(list.get(i).toString());
        }
        result.append("]}");
        return result.toString();
    }
}