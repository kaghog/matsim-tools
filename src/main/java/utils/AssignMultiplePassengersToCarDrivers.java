package utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author kaghog created on 04.07.2022
 * @project matsim-tools
 */
public class AssignMultiplePassengersToCarDrivers {
    public static void main(String[] args) throws IOException, CommandLine.ConfigurationException {
        CommandLine cmd = (new CommandLine.Builder(args))
                .requireOptions("input-path", "matched-trips", "output-path")
                .allowOptions("scenario-type", "csv-separator")
                .build();

        String population = cmd.getOptionStrict("input-path");
        String matchedTripsPath = cmd.getOptionStrict("matched-trips");
        String outputfile = cmd.getOptionStrict("output-path");
        String scenarioType = cmd.hasOption("scenario-type") ? cmd.getOption("scenario-type").get() : "Group";
        String csvSeparator = cmd.hasOption("csv-separator") ? cmd.getOption("csv-separator").get() : ",";

        //Read in the passenger-driver matching
        BufferedReader reader = IOUtils.getBufferedReader(matchedTripsPath);
        String headerLine = reader.readLine();  //driverID and paxIDs are the headers
        Map<String, String[]> driversPassengers = new HashMap<>();
        String readerString = "";
        String[] readerValues;

        while ((readerString = reader.readLine()) != null) {
            //clean up readerString, need to check how the strings are read in by java
            readerString = readerString.replaceAll("\"", "");
            readerString = readerString.replaceAll("\'", "");

            readerValues = readerString.split(csvSeparator);

            driversPassengers.put(readerValues[0], readerValues);
        }
        System.out.println("Extracted group travelers: " + driversPassengers.size());

        //generate new trip-based population with updated trip attributes
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile(population);

        ScenarioUtils.loadScenario(scenario);

        Population populationData = scenario.getPopulation();

        List<Id<Person> > passengerList = new ArrayList<>();
        Map<Id<Person>, Person> matchedPassengers = new HashMap<>();

        System.out.println("Initial Pop size: " +populationData.getPersons().size());

        for (Person person : populationData.getPersons().values()) {
            String driverId = person.getId().toString();

            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements());
            for (TripStructureUtils.Trip trip : trips) {
                for (Leg leg : trip.getLegsOnly()) {
                    if (leg.getMode().equals("car") || leg.getMode().equals("car_passenger")) {

                        //Assign passengers for group scenario
                        if (scenarioType.equals("Group") && leg.getMode().equals("car")) {
                            if (driversPassengers.get(driverId) != null) {
                                String reservedPassengers = "";
                                //loop through the driver and passengers and convert to string
                                int ind = 1;
                                for (String id: driversPassengers.get(driverId)){
                                    if (ind > 1) {
                                        reservedPassengers = reservedPassengers + "," + id;
                                        passengerList.add(Id.create(id, Person.class));

                                    } else {
                                        reservedPassengers = reservedPassengers + id;
                                    }
                                    ind++;
                                }
                                leg.getAttributes().putAttribute("reservedPassengers", reservedPassengers);

                                //also add this to person attribute since matsim sometimes deletes the leg attributes
                                person.getAttributes().putAttribute("reservedPassengers", reservedPassengers);


                            }
                        }
                        if (scenarioType.equals("Norm") && (driversPassengers.get(driverId) != null)){
                            int ind = 1;
                            for (String id: driversPassengers.get(driverId)){
                                if (ind > 1) {
                                    passengerList.add(Id.create(id, Person.class));

                                    //ToDo create a person with drivers' information
                                    Person newPassenger = null;
                                    person.getPlans();

                                    //matchedPassengers.put(Id.create(id, Person.class),newPassenger);

                                    //ToDo give the person its an attribute of its initial personID?

                                }
                                ind++;
                            }
                        }

                                leg.setMode("drt");
                        leg.getAttributes().removeAttribute("routingMode");
                        leg.getAttributes().putAttribute("routingMode", "drt");

                    }
                }

            }

        }

        //delete passengers in Group scenario
        if (scenarioType.equals("Group")) {
            //get list of persons from dictionary key
            for (Id<Person> id: passengerList){
                populationData.removePerson(id);
            }
            System.out.println("Pop size with car passengers removed for group scenario: " + passengerList.size());
        }

        //toDo update the matched passengers in Norm scenario to have same origin, destination and departure time as it's driver
        if (scenarioType.equals("Norm")){
            //remove passenger with old plan and add passeg again with their corresponding driver information
        }
        //store driver's plan along with passenger list
        //loop through all the passengers and update their plan

        System.out.println("Final Pop size: " +populationData.getPersons().size());

        new PopulationWriter(populationData).write(outputfile);

        System.out.println("Finished");


    }
}
