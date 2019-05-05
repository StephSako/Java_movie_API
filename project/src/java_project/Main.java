package java_project;

public class Main {

    public static void main(String[] args) {

        RechercheFilm r = new RechercheFilm("../bdd/bdfilm.sqlite");
        System.out.println(r.retrouve(String.join(" ", args)));
        //System.out.println(r.retrouve("TITRE Intouchables OU Le Projet Blair Witch, EN 2011, AVEC FRancois cluzet, omar sy, PAYS fr, AVANT 2012 OU 1900, APRES 2010 OU 2009, DE Eric Toledano, Olivier Nakache"));
    }
}