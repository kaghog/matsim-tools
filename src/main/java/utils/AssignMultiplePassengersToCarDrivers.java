package utils;

import org.matsim.api.core.v01.Id;
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
                .allowOptions("scenario-type", "csv-separator", "update-passengers")
                .build();

        String population = cmd.getOptionStrict("input-path");
        String matchedTripsPath = cmd.getOptionStrict("matched-trips");
        String outputfile = cmd.getOptionStrict("output-path");
        String scenarioType = cmd.hasOption("scenario-type") ? cmd.getOption("scenario-type").get() : "Group";
        String csvSeparator = cmd.hasOption("csv-separator") ? cmd.getOption("csv-separator").get() : ",";
        Boolean isUpdatePassenger = cmd.hasOption("update-passengers") ? Boolean.parseBoolean(cmd.getOption("update-passengers").get()) : false;

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

        PopulationFactory popFactory = populationData.getFactory();

        List<Id<Person> > passengerList = new ArrayList<>();
        Map<Id<Person>, Person> matchedPassengers = new HashMap<>();

        System.out.println("Initial Pop size: " +populationData.getPersons().size());

        for (Person person : populationData.getPersons().values()) {
            String driverId = person.getId().toString();

            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements());
            for (TripStructureUtils.Trip trip : trips) {
                for (Leg leg : trip.getLegsOnly()) {
                    if (leg.getMode().equals("car") || leg.getMode().equals("car_passenger")) {

                        //Assign passengers for group scenario and generate update pax data in norm scenario
                        if (leg.getMode().equals("car") && driversPassengers.get(driverId) != null) {
                                String reservedPassengers = "";
                                //loop through the driver and passengers and convert to Ids
                                int ind = 1;
                                for (String id: driversPassengers.get(driverId)){
                                    if (ind > 1) {
                                        reservedPassengers = reservedPassengers + "," + id;
                                        Id<Person> passengerId = Id.create(id, Person.class);
                                        passengerList.add(passengerId);

                                        if (isUpdatePassenger && scenarioType.equals("Norm")){
                                            // create a new person with drivers' information for passengers
                                            Person newPassenger = popFactory.createPerson(passengerId);

                                            for (Plan plan: person.getPlans()){
                                                newPassenger.addPlan(plan);
                                            }
                                            newPassenger.getAttributes().putAttribute("matchedDriver", person.getId().toString());
                                            matchedPassengers.put(passengerId,newPassenger);
                                        }

                                    } else {
                                        reservedPassengers = reservedPassengers + id;
                                    }
                                    ind++;
                                }
                                if (scenarioType.equals("Group")){
                                    leg.getAttributes().putAttribute("reservedPassengers", reservedPassengers);

                                    //also add this to person attribute since matsim sometimes deletes the leg attributes
                                    person.getAttributes().putAttribute("reservedPassengers", reservedPassengers);
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

        // update the matched passengers in Norm scenario to have same origin, destination and departure time as it's driver
        if (isUpdatePassenger && scenarioType.equals("Norm")){
            for (Id<Person> id: passengerList){
                Person updatedPassenger = matchedPassengers.get(id);

                Person oldPassenger = populationData.getPersons().get(id);
                for (Map.Entry attr: oldPassenger.getAttributes().getAsMap().entrySet()){

                    updatedPassenger.getAttributes().putAttribute(attr.getKey().toString(),attr.getValue());
                }

                //remove passenger with old plan and add again with their corresponding driver information
                populationData.removePerson(id);

                populationData.addPerson(updatedPassenger);
            }

        }

        System.out.println("Final Pop size: " +populationData.getPersons().size());

        new PopulationWriter(populationData).write(outputfile);

        System.out.println("Finished");


    }
}
