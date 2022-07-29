package utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author kaghog created on 22.06.2021
 * @project wayne_county
 */
public class CreatePopulationForCarTripsCustom {
    public static void main(String[] args) throws IOException, CommandLine.ConfigurationException {

        CommandLine cmd = (new CommandLine.Builder(args))
                .requireOptions("input-path", "output-path","filteredTrips-path")
                .allowOptions("suffix", "csv-separator","network-path")
                .build();

        String population = cmd.getOptionStrict("input-path");

        String outputDir = cmd.getOptionStrict("output-path");
        String filteredTrips = cmd.getOptionStrict("filteredTrips-path");
        String suffix = cmd.hasOption("suffix") ? cmd.getOption("suffix").get() : "population_filtered";
        String csvSeparator = cmd.hasOption("csv-separator") ? cmd.getOption("csv-separator").get() : ",";

        String outputfile = outputDir + "/" + suffix + "cars.xml.gz";
        String outputfileCsv = outputDir + "/" + suffix + "cars.csv";


        //Read in the filtered trips
        BufferedReader reader = IOUtils.getBufferedReader(filteredTrips);
        String headerLine = reader.readLine();
        System.out.println(headerLine);
        List<String> headers = Arrays.asList(headerLine.split(csvSeparator));
        int personIndex = headers.indexOf("person_id");
        int tripIndex = headers.indexOf("trip_id");
        String readerString = "";
        String[] readerValues;
        Map<String, ArrayList<String>> personsAndTrips = new HashMap<>();

        while ((readerString = reader.readLine()) != null) {
            readerValues = readerString.split(csvSeparator);
            String tripIdCsv = readerValues[tripIndex];
            String personIdCsv = readerValues[personIndex];
            if (personsAndTrips.containsKey(personIdCsv)){
                personsAndTrips.get(personIdCsv).add(tripIdCsv);
            } else {
                personsAndTrips.put(personIdCsv,new ArrayList<>());
                personsAndTrips.get(personIdCsv).add(tripIdCsv);
            }
        }

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile(population);

        ScenarioUtils.loadScenario(scenario);

        Population populationData = scenario.getPopulation();
        PopulationFactory populationFactory = populationData.getFactory();

        Population newPop = PopulationUtils.createPopulation(config);

        //also print car trips in csv
        BufferedWriter writer = IOUtils.getBufferedWriter(outputfileCsv);
        writer.write("person_id;person;mode;departure_time;travel_time;preceding_purpose;following_purpose;start_link;end_link;origin_x;origin_y;destination_x;destination_y\n");

        for (Person person : populationData.getPersons().values()) {

            if (personsAndTrips.containsKey(person.getId().toString())) {
                List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements());
                int n = 11;
                for (TripStructureUtils.Trip trip : trips) {

                    for (Leg leg : trip.getLegsOnly()) {
                        if (leg.getMode().equals("car") || leg.getMode().equals("car_passenger")) {
                            //add leg to plan and add routingMode for matsim12 and above

                            //check if the trip itself is within region scope
                            String tripIdXml = person.getId().toString() + n;
                            if(!personsAndTrips.get(person.getId().toString()).contains(tripIdXml)){
                                continue;
                            }


                            // Create csv for the car trips
                            writer.write(person.getId().toString() + ";"
                                    + person.getId().toString() + n + ";"
                                    + leg.getMode() + ";"
                                    + leg.getDepartureTime().seconds() + ";"
                                    + leg.getTravelTime().seconds() + ";"
                                    + trip.getOriginActivity().getType() + ";"
                                    + trip.getDestinationActivity().getType() + ";"
                                    + trip.getOriginActivity().getLinkId().toString() + ";"
                                    + trip.getDestinationActivity().getLinkId().toString() + ";"
                                    + trip.getOriginActivity().getCoord().getX() + ";"
                                    + trip.getOriginActivity().getCoord().getY() + ";"
                                    + trip.getDestinationActivity().getCoord().getX() + ";"
                                    + trip.getDestinationActivity().getCoord().getY() + "\n");

                            Person newPerson = populationFactory.createPerson(Id.create(person.getId().toString() + n, Person.class));
                            Plan p = populationFactory.createPlan();


                            //add first activity to plan
                            p.addActivity(trip.getOriginActivity());

                            //remove the route
                            if (leg.getRoute() != null) {
                                leg.setRoute(null);
                            }
                            //leg.setMode("drt");
                            leg.getAttributes().putAttribute("routingMode", leg.getMode());

                            p.addLeg(leg);

                            //add second activity
                            p.addActivity(trip.getDestinationActivity());

                            //add plan to person
                            newPerson.addPlan(p);

                            //add the person attributes

                            //add original personID
                            newPerson.getAttributes().putAttribute("origPersonId", person.getId().toString());
                            newPerson.getAttributes().putAttribute("isCarPassenger", person.getAttributes().getAttribute("isCarPassenger"));

                            newPop.addPerson(newPerson);

                        }

                    }
                    n++;
                }
            }


        }

        //check if all are within the network
        if(cmd.hasOption("network-path")) {
            String networkfile = cmd.getOption("network-path").get();
            Network network = scenario.getNetwork();
            new MatsimNetworkReader(network).readFile(networkfile);

            //filter network

            Network newNetwork = NetworkUtils.createNetwork();

            TransportModeNetworkFilter modeNetworkFilter = new TransportModeNetworkFilter(network);
            modeNetworkFilter.filter(newNetwork, Collections.singleton(TransportMode.car));

            //for each person, get their link, check if link in network. if not in the list print the person id and count the number.
            // write a new population file for those not in the network or modify the population file and print a new one after assigning a new link from the network
            int relocatedAgents = 0;
            for (Person person : newPop.getPersons().values()) {

                for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
                    if (element instanceof Activity) {
                        Activity activity = (Activity) element;

                        String linkId = activity.getLinkId().toString();
                        //System.out.println(person.getId().toString());

                        if (!(newNetwork.getLinks().containsKey(linkId))) {
                            Id newLinkId = NetworkUtils.getNearestLinkExactly(newNetwork, activity.getCoord()).getId();

                            activity.setLinkId(newLinkId);
                            relocatedAgents++;
                        }


                    }
                }

            }
            System.out.println("Number of relocated activities: " + relocatedAgents);
        }

        new PopulationWriter(newPop).write(outputfile);
        writer.flush();
        writer.close();

        System.out.println("Size of old population is: " + populationData.getPersons().size());
        System.out.println("Size of new population is: " + newPop.getPersons().size());


        System.out.println("Finished");


    }
}
