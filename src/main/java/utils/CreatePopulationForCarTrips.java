package utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * @author kaghog created on 22.06.2021
 * @project wayne_county
 */
public class CreatePopulationForCarTrips {
    public static void main(String[] args) throws IOException, CommandLine.ConfigurationException {

        CommandLine cmd = (new CommandLine.Builder(args))
                .requireOptions("input-path", "output-path", "csv-output-path")
                .build();

        String population = cmd.getOptionStrict("input-path");
        String outputfile = cmd.getOptionStrict("output-path");
        String outputfileCsv = cmd.getOptionStrict("csv-output-path");


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

            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements());
            int n = 11;
            for (TripStructureUtils.Trip trip : trips) {


                //filter out outside trips
                if (trip.getDestinationActivity().getType().equals("outside") |
                        trip.getOriginActivity().getType().equals("outside")) {
                    continue;
                }
                for (Leg leg : trip.getLegsOnly()) {
                    if (leg.getMode().equals("car") || leg.getMode().equals("car_passenger")) {
                        //add leg to plan and add routingMode for matsim12 and above


                        leg.getAttributes().putAttribute("routingMode", leg.getMode());

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

                        //create population and remove leg routes

                        //Remove route as it causes conflict for the vehicleRefId
                        //Route route = populationFactory.getRouteFactories().createRoute(Route.class, leg.getRoute().getStartLinkId(), leg.getRoute().getEndLinkId());
                        //Route route = populationFactory.getRouteFactories().createRoute(NetworkRoute.class, leg.getRoute().getStartLinkId(), leg.getRoute().getEndLinkId());

                        Person newPerson = populationFactory.createPerson(Id.create(person.getId().toString() + n, Person.class));
                        Plan p = populationFactory.createPlan();


                        //add first activity to plan
                        p.addActivity(trip.getOriginActivity());

                        //remove the route and set the mode to drt
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
                  /*  newPerson.getAttributes().putAttribute("age", person.getAttributes().getAttribute("age"));
                    newPerson.getAttributes().putAttribute("bikeAvailability", person.getAttributes().getAttribute("bikeAvailability"));
                    newPerson.getAttributes().putAttribute("carAvail", person.getAttributes().getAttribute("carAvail"));
                    newPerson.getAttributes().putAttribute("employed", person.getAttributes().getAttribute("employed"));
                    newPerson.getAttributes().putAttribute("hasLicense", person.getAttributes().getAttribute("hasLicense"));
                    newPerson.getAttributes().putAttribute("home_x", person.getAttributes().getAttribute("home_x"));
                    newPerson.getAttributes().putAttribute("home_y", person.getAttributes().getAttribute("home_y"));
                    newPerson.getAttributes().putAttribute("isCarPassenger", person.getAttributes().getAttribute("isCarPassenger"));
                    newPerson.getAttributes().putAttribute("isOutside", person.getAttributes().getAttribute("isOutside"));
                    newPerson.getAttributes().putAttribute("mzHeadId", person.getAttributes().getAttribute("mzHeadId"));
                    newPerson.getAttributes().putAttribute("mzPersonId", person.getAttributes().getAttribute("mzPersonId"));
                    newPerson.getAttributes().putAttribute("ptHasGA", person.getAttributes().getAttribute("ptHasGA"));
                    newPerson.getAttributes().putAttribute("ptHasHalbtax", person.getAttributes().getAttribute("ptHasHalbtax"));
                    newPerson.getAttributes().putAttribute("ptHasStrecke", person.getAttributes().getAttribute("ptHasStrecke"));
                    newPerson.getAttributes().putAttribute("ptHasVerbund", person.getAttributes().getAttribute("ptHasVerbund"));
                    newPerson.getAttributes().putAttribute("spRegion", person.getAttributes().getAttribute("spRegion"));
                    newPerson.getAttributes().putAttribute("statpopHouseholdId", person.getAttributes().getAttribute("statpopHouseholdId"));
                    newPerson.getAttributes().putAttribute("statpopPersonId", person.getAttributes().getAttribute("statpopPersonId"));*/

                        //add original personID
                        newPerson.getAttributes().putAttribute("origPersonId", person.getId().toString());
                        newPerson.getAttributes().putAttribute("isCarPassenger", person.getAttributes().getAttribute("isCarPassenger"));

                        newPop.addPerson(newPerson);



                    }

                }
                n++;
            }

        }

        new PopulationWriter(newPop).write(outputfile);
        writer.flush();
        writer.close();

        System.out.println("Finished");


    }
}
