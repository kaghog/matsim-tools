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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author kaghog created on 09.02.2022
 * @project matsim-tools
 * Converts matsim xml plans to csv
 * Specify the file paths as below
 * --input-path "my/file-path/output_plans.xml.gz"
 * --output-path "my/file-path/output_plans.csv"
 * optionally for a detailed transit output of all the stages:
 * --transit-output-path "my/file-path/detailed_transit_plans.csv"
 */

public class ConvertPlansToCSV {
    public static void main(String[] args) throws IOException, CommandLine.ConfigurationException {

        CommandLine cmd = (new CommandLine.Builder(args))
                .requireOptions("input-path", "output-path")
                .allowOptions("transit-output-path" )
                .build();

        String populationPath = cmd.getOptionStrict("input-path");
        String outputfile = cmd.getOptionStrict("output-path");
        boolean detailedTransit = false;
        BufferedWriter writerTransit = null;

        if(cmd.hasOption("transit-output-path")) {
            String transitOutputfile = cmd.getOption("transit-output-path").get();
            detailedTransit = true;
            writerTransit = IOUtils.getBufferedWriter(transitOutputfile);
            writerTransit.write("person_id;trip_id;mode;mode_detailed;preceding_purpose;following_purpose;departure_time;travel_time;origin_x;origin_y;destination_x;destination_y;crowfly_distance\n");
        }

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile(populationPath);
        BufferedWriter writer = IOUtils.getBufferedWriter(outputfile);
        writer.write("person_id;trip_id;mode;mode_detailed;preceding_purpose;following_purpose;departure_time;travel_time;origin_x;origin_y;destination_x;destination_y;crowfly_distance\n");

        for (Person person : scenario.getPopulation().getPersons().values()) {
            Double dep_time = 0.0;
            Double trav_time = 0.0;
            String mode_detailed = "";
            String routing_mode = "";
            Double distance = 0.0;
            ArrayList<String> purpose = new ArrayList<String>(Collections.emptyList());
            String preceding_purpose = "";
            String following_purpose = "";

            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements());
            int n = 11;
            for (TripStructureUtils.Trip trip : trips) {
                for (Leg leg : trip.getLegsOnly()) {
                        preceding_purpose = trip.getOriginActivity().getType();
                        following_purpose = trip.getDestinationActivity().getType();
                        routing_mode = leg.getAttributes().getAttribute("routingMode") != null ? leg.getAttributes().getAttribute("routingMode").toString() : "";
                        if (routing_mode.equals("")){
                            System.out.println("Could not find routing mode for " + leg.getMode());
                        }
                        if (routing_mode.equals("pt")){
                            if(mode_detailed.equals("")){
                                mode_detailed = leg.getMode();
                                dep_time = leg.getDepartureTime().seconds();
                                trav_time = leg.getTravelTime().seconds();
                                distance = leg.getRoute() == null ? Double.NaN : leg.getRoute().getDistance();;
                            }
                            else {
                                mode_detailed = mode_detailed + "_" + leg.getMode();
                                trav_time = trav_time + leg.getTravelTime().seconds();
                                distance = distance + (leg.getRoute() == null ? Double.NaN : leg.getRoute().getDistance());;
                            }
                        } else {
                        mode_detailed = leg.getMode();
                        dep_time = leg.getDepartureTime().seconds();
                        trav_time = leg.getTravelTime().seconds();
                        distance = leg.getRoute() == null ? Double.NaN : leg.getRoute().getDistance();
                        }

                        if(detailedTransit) {
                            writerTransit.write(person.getId().toString() + ";"
                                    + person.getId().toString() + n + ";"
                                    + routing_mode + ";" //captures the main mode
                                    + mode_detailed + ";"
                                    + preceding_purpose + ";"
                                    + following_purpose + ";"
                                    + dep_time + ";"
                                    + trav_time + ";"
                                    + trip.getOriginActivity().getCoord().getX() + ";"
                                    + trip.getOriginActivity().getCoord().getY() + ";"
                                    + trip.getDestinationActivity().getCoord().getX() + ";"
                                    + trip.getDestinationActivity().getCoord().getY() + ";"
                                    + distance + "\n");
                        }

                }
                writer.write(person.getId().toString() + ";"
                        + person.getId().toString() + n + ";"
                        + routing_mode + ";" //captures the main mode
                        + mode_detailed + ";"
                        + preceding_purpose + ";"
                        + following_purpose + ";"
                        + dep_time + ";"
                        + trav_time + ";"
                        + trip.getOriginActivity().getCoord().getX() + ";"
                        + trip.getOriginActivity().getCoord().getY() + ";"
                        + trip.getDestinationActivity().getCoord().getX() + ";"
                        + trip.getDestinationActivity().getCoord().getY() + ";"
                        + distance + "\n");
                n++;

            }

        }

        writer.flush();
        writer.close();

    }
}
