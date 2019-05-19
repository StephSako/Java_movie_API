package java_project;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Information synthetique sur un film.
 */
public class InfoFilm implements Comparable<InfoFilm> {
    private String                 _titre;
    private ArrayList<NomPersonne> _realisateurs;
    private ArrayList<NomPersonne> _acteurs;
    private String                 _pays;
    private int                    _annee;
    private int                    _duree;
    private ArrayList<String>      _autres_titres;

    /**
     * Constructeur.
     * @param titre Titre (francais en general) du film.
     * @param realisateurs Liste des realisateurs (peut etre vide).
     * @param acteurs Liste des acteurs (peut etre vide).
     * @param pays Nom (francais) du pays.
     * @param annee Annee de sortie.
     * @param duree Duree en minutes; 0 ou valeur negative si l'information n'est pas connue.
     * @param autres_titres Liste des titres alternatifs (peut etre vide), type titre original ou titre anglais a l'international.
     */
    InfoFilm(String titre,
             ArrayList<NomPersonne> realisateurs,
             ArrayList<NomPersonne> acteurs,
             String pays,
             int annee,
             int duree,
             ArrayList<String> autres_titres) {
        _titre = titre;
        _realisateurs = realisateurs;
        Collections.sort(_realisateurs);
        _acteurs = acteurs;
        Collections.sort(_acteurs);
        _pays = pays;
        _annee = annee;
        _duree = duree;
        _autres_titres = autres_titres;
        Collections.sort(_autres_titres);
    }

    /**
     * Comparaison par titre, puis annee, puis pays.
     * @return Un entier inferieur, egal ou superieur a zero suivant le cas.
     */
    @Override
    public int compareTo(InfoFilm autre) {
        if (autre == null) {
            return 1;
        }
        int cmp = this._titre.compareTo(autre._titre);
        if (cmp == 0) {
            cmp = (Integer.compare(this._annee, autre._annee));
            if (cmp == 0) {
                cmp = this._pays.compareTo(autre._pays);
            }
        }
        return cmp;
    }

    /**
     * Affiche sous forme d'objet JSON des informations sous un film.
     * <p>Realisateurs et acteurs sont tries par ordre alphabetique, la duree est convertie en heures et minutes.
     * @return Une chaine de caracteres representant un objet JSON.
     */
    @Override
    public String toString() {
        boolean debut = true;
        StringBuilder sb = new StringBuilder();
        sb.append("{\"titre\":\"").append(_titre.replace("\"", "\\\"")).append("\",");
        sb.append("\"realisateurs\":[");
        for (NomPersonne nom: _realisateurs) {
            if (debut) {
                debut = false;
            } else {
                sb.append(',');
            }
            sb.append("\"").append(nom.toString().replace("\"", "\\\"")).append("\"");
        }
        sb.append("],\"acteurs\":[");
        debut = true;
        for (NomPersonne nom: _acteurs) {
            if (debut) {
                debut = false;
            } else {
                sb.append(',');
            }
            sb.append("\"").append(nom.toString().replace("\"", "\\\"")).append("\"");
        }
        sb.append("],\"pays\":\"");
        sb.append(_pays.replace("\"", "\\\""));
        sb.append("\",\"annee\":");
        sb.append(_annee);
        sb.append(",\"duree\":");
        if (_duree > 0) {
            sb.append('"');
            int h = _duree / 60;
            sb.append(h).append("h");
            int mn = _duree % 60;
            if (mn > 0) {
                sb.append(mn).append("mn");
            }
            sb.append('"');
        } else {
            sb.append("null");
        }
        sb.append(",\"autres titres\":[");
        debut = true;
        for (String titre: _autres_titres) {
            if (debut) {
                debut = false;
            } else {
                sb.append(',');
            }
            sb.append("\"").append(titre.replace("\"", "\\\"")).append("\"");
        }
        sb.append("]}");
        return sb.toString();
    }
}
