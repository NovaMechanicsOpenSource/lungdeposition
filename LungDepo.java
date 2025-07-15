/**
 * This software component has been developed by NovaMechanics Ltd.
 * 
 * Date of creation: 12/02/2025
 * 
 * Related scientific publication:
 * Dimitris Mintis et al., "A web-based dosimetry application for simulating particle deposition in human lungs using ICRP and MPPD models,"
 * Environmental Science: Nano, Royal Society of Chemistry, 2025.
 * https://pubs.rsc.org/en/content/articlehtml/2025/en/d5en00299k
 * 
 * This work has been funded by the European Union’s Horizon research and innovation programme 
 * under the PROPLANET project, Grant Agreement No. 101091842.
 */

package com.lightbend.lagom.maven_archetype_lagom_java;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

/**
 * This class calculates the inhalation flux and deposition flux of particles in different regions
 * of the human respiratory tract based on the International Commission on Radiological Protection (ICRP) model.
 * It prompts the user for input data such as the path to a text file containing particle diameters (Dp) and 
 * probability densities, the volume intake option or custom value, concentration, and exposure duration.
 * 
 */

public class ICRP {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter the path to the text file containing Dp values and probability densities:");
        String filePath = scanner.nextLine();
        
        System.out.println("Enter the concentration (pg/m^3):");
        double concentration = scanner.nextDouble(); // Read concentration

        // Display volume intake options
        System.out.println("Select the volume intake option:");
        System.out.println("1. Female sitting (0.39 m^3/h)");
        System.out.println("2. Female light exercise (1.25 m^3/h)");
        System.out.println("3. Female heavy exercise (2.70 m^3/h)");
        System.out.println("4. Male sitting (0.54 m^3/h)");
        System.out.println("5. Male light exercise (1.50 m^3/h)");
        System.out.println("6. Male heavy exercise (3.00 m^3/h)");
        System.out.println("7. User defined");
        int choice = scanner.nextInt();
        
        double volumeIntake; // Declare the volume intake variable

        // Assign the corresponding volume intake based on the user's selection
        switch (choice) {
            case 1:
                volumeIntake = 0.39;
                break;
            case 2:
                volumeIntake = 1.25;
                break;
            case 3:
                volumeIntake = 2.70;
                break;
            case 4:
                volumeIntake = 0.54;
                break;
            case 5:
                volumeIntake = 1.50;
                break;
            case 6:
                volumeIntake = 3.00;
                break;
            case 7:
                System.out.println("Enter your custom volume intake (m^3/h):");
                volumeIntake = scanner.nextDouble(); // Read the user-defined volume intake
                break;
            default:
                System.out.println("Invalid choice, defaulting to Female sitting (0.39 m^3/h)");
                volumeIntake = 0.39;
        }
        
        System.out.println("Enter the exposure duration (seconds):");
        double exposureDurationSeconds = scanner.nextDouble(); // Read exposure duration in seconds
        double exposureDurationHours = exposureDurationSeconds / 3600.0; // Convert exposure duration to hours

        // Initialize total flux accumulators
        double totalIntakeFlux = 0;
        double totalHADepFlux = 0;
        double totalTBDepFlux = 0;
        double totalARDepFlux = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+"); // Assuming values are separated by whitespace
                double dp = Double.parseDouble(parts[0]);
                double probabilityDensity = Double.parseDouble(parts[1]) / 100; // Convert percentage to a decimal

                double ifValue = calculateInhalableFraction(dp);
                double haEfficiency = calculateHeadAirwayEfficiency(ifValue, dp);
                double tbEfficiency = calculateTracheobronchialEfficiency(dp);
                double arEfficiency = calculateAlveolarEfficiency(dp);
                // Update the flux calculation to include exposure duration
                double flux = volumeIntake * concentration * probabilityDensity * exposureDurationHours; 

                // Calculate deposition flux for HA, TB, and AR
                double haDepositionFlux = flux * haEfficiency;
                double tbDepositionFlux = flux * tbEfficiency;
                double arDepositionFlux = flux * arEfficiency;

                // Accumulate total fluxes
                totalIntakeFlux += flux;
                totalHADepFlux += haDepositionFlux;
                totalTBDepFlux += tbDepositionFlux;
                totalARDepFlux += arDepositionFlux;

                System.out.printf("Dp: %.2f µm - HA: %.4f, TB: %.4f, AR: %.4f, Intake: %.4f pg, HA Dep.: %.4f pg, TB Dep.: %.4f pg, AR Dep.: %.4f pg%n",
                        dp, haEfficiency, tbEfficiency, arEfficiency, flux, haDepositionFlux, tbDepositionFlux, arDepositionFlux);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Calculate total deposition flux
        double totalDepositionFlux = totalHADepFlux + totalTBDepFlux + totalARDepFlux;

        // Calculate percentage contributions
        double haPercentage = (totalHADepFlux / totalDepositionFlux) * 100;
        double tbPercentage = (totalTBDepFlux / totalDepositionFlux) * 100;
        double arPercentage = (totalARDepFlux / totalDepositionFlux) * 100;

        // Output total fluxes and percentages
        System.out.printf("Total Intake Flux: %.4f pg%n", totalIntakeFlux);
        System.out.printf("Total Deposition Flux: %.4f pg (HA: %.2f%%, TB: %.2f%%, AR: %.2f%%)%n",
            totalDepositionFlux, haPercentage, tbPercentage, arPercentage);
        System.out.printf("Total HA Dep. Flux: %.4f pg (%.2f%% of Total Dep.)%n", totalHADepFlux, haPercentage);
        System.out.printf("Total TB Dep. Flux: %.4f pg (%.2f%% of Total Dep.)%n", totalTBDepFlux, tbPercentage);
        System.out.printf("Total AR Dep. Flux: %.4f pg (%.2f%% of Total Dep.)%n", totalARDepFlux, arPercentage);
    }

    private static double calculateInhalableFraction(double dp) {
        return 1 - 0.5 * (1 - 1 / (1 + 0.00076 * Math.pow(dp, 2.8)));
    }

    private static double calculateHeadAirwayEfficiency(double ifValue, double dp) {
        return ifValue * (1 / (1 + Math.exp(6.84 + 1.183 * Math.log(dp))) + 1 / (1 + Math.exp(0.924 - 1.885 * Math.log(dp))));
    }

    private static double calculateTracheobronchialEfficiency(double dp) {
        return (0.00352 / dp) * (Math.exp(-0.234 * Math.pow(Math.log(dp) + 3.40, 2)) + 63.9 * Math.exp(-0.819 * Math.pow(Math.log(dp) - 1.61, 2)));
    }

    private static double calculateAlveolarEfficiency(double dp) {
        return (0.0155 / dp) * (Math.exp(-0.415 * Math.pow(Math.log(dp) + 2.84, 2)) + 19.11 * Math.exp(-0.482 * Math.pow(Math.log(dp) - 1.362, 2)));
    }
}