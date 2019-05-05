package java_project;

public class Main {

    public static void main(String[] args) {

        RechercheFilm r = new RechercheFilm("../bdd/bdfilm.sqlite");
        System.out.println(r.retrouve(String.join(" ", args)));
        //System.out.println(r.retrouve("AVEC omar sy cluzet, AVEC jason statham"));
    }
}