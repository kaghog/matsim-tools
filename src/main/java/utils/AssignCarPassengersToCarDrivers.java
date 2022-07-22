package utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author kaghog created on 04.07.2022
 * @project matsim-tools
 */
public class AssignCarPassengersToCarDrivers {
    public static void main(String[] args) throws IOException, CommandLine.ConfigurationException {

        CommandLine cmd = (new CommandLine.Builder(args))
                .requireOptions("input-path", "matched-trips", "output-path")
                .build();

        String population = cmd.getOptionStrict("input-path");
        String matchedTripsPath = cmd.getOptionStrict("matched-trips");
        String outputfile = cmd.getOptionStrict("output-path");

        //Read in the passenger-driver matching
        BufferedReader reader = IOUtils.getBufferedReader(matchedTripsPath);
        String headerLine = reader.readLine();
        Map<String, String> driversPassengers = new HashMap<>();
        String readerString = "";
        String[] readerValues;

        while ((readerString = reader.readLine()) != null) {
            readerValues = readerString.split(",");
            driversPassengers.put(readerValues[0], readerValues[1]); //drivers are keys and passengers are values todo confirm
        }
        Set<String> drivers = driversPassengers.keySet();

        //generate new trip-based population with updated trip attributes
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile(population);

        ScenarioUtils.loadScenario(scenario);

        Population populationData = scenario.getPopulation();
        for (Person person : populationData.getPersons().values()) {
            String driverId = person.getId().toString();

            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements());
            for (TripStructureUtils.Trip trip : trips) {
                for (Leg leg : trip.getLegsOnly()) {
                    if (leg.getMode().equals("car")) {

                        leg.setMode("drt");
                        leg.getAttributes().removeAttribute("routingMode");
                        leg.getAttributes().putAttribute("routingMode", "drt");

                        if (driversPassengers.get(driverId) != null) {
                            String reservedPassengers = driverId + "," + driversPassengers.get(driverId);
                            leg.getAttributes().putAttribute("reservedPassengers", reservedPassengers);
                        }
                    }
                }



            }

        }

        new PopulationWriter(populationData).write(outputfile);

        System.out.println("Finished");


    }
}
