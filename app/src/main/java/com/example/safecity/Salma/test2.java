package com.example.safecity.Salma;

public class test2 {
    import java.util.Scanner;

    public class ConversionEuroDollar {
        public static void main(String[] args) {
            Scanner sc = new Scanner(System.in);

            System.out.print("Entrez le montant en euros : ");
            double euros = sc.nextDouble();

            double taux = 1.07; // 1 € = 1.07 $
            double dollars = euros * taux;

            System.out.printf("%.2f euros = %.2f dollars\n", euros, dollars);
            sc.close();
        }
    }

}
