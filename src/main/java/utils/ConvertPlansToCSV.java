package utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
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

/**
 * @author kaghog created on 09.02.2022
 * @project matsim-tools
 */
public class ConvertPlansToCSV {
    public static void main(String[] args) throws IOException {
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile(args[0]);
        BufferedWriter writer = IOUtils.getBufferedWriter(args[1]);
        writer.write("person_id;mode;mode_detailed;preceding_purpose;following_purpose;departure_time;travel_time;origin_x;origin_y;destination_x;destination_y;crowfly_distance\n");

        //works for trip based where each person has a single trip
        for (Person person : scenario.getPopulation().getPersons().values()) {
            String linkId = "";
            Double dep_time = 0.0;
            Double dep_time_pt = 0.0;
            Double trav_time = 0.0;
            String mode = "";
            String mode_detailed = "";
            String routing_mode = "";
            Double distance = 0.0;
            String activity_type = "";
            ArrayList<String> purpose = new ArrayList<String>(Collections.emptyList());
            String preceding_purpose = "";
            String following_purpose = "";

            /*for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
                if (element instanceof Activity) {
                    Activity activity = (Activity) element;
                    activity_type = activity.getType();
                    purpose.add(activity_type);

                }
                if (element instanceof Leg) {
                    Leg leg = (Leg) element;
                    mode = leg.getMode();
                    routing_mode = leg.getAttributes().getAttribute("routingMode").toString();
                    if (routing_mode.equals("pt")){
                        mode_detailed = mode_detailed + "-" + mode;
                        trav_time += leg.getTravelTime().seconds();
                        distance += leg.getRoute().getDistance();

                        //keep departure time only for first leg of pt trip
                        if(purpose.size()== 1){
                            dep_time = leg.getDepartureTime().seconds();
                        }
                    }
                    else{
                        mode_detailed = mode;
                        dep_time = leg.getDepartureTime().seconds();
                        trav_time = leg.getTravelTime().seconds();
                        distance = leg.getRoute().getDistance();
                    }
                }

            }*/
            for (final Plan plan : person.getPlans()) {
                for (final TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
                    preceding_purpose = trip.getOriginActivity().getType();
                    following_purpose = trip.getDestinationActivity().getType();
                    for (final PlanElement pe : trip.getTripElements()) {
                        if (pe instanceof Leg) {
                            Leg leg = (Leg) pe;
                            routing_mode = leg.getAttributes().getAttribute("routingMode").toString();
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
                            }
                            else {
                                mode_detailed = leg.getMode();
                                dep_time = leg.getDepartureTime().seconds();
                                trav_time = leg.getTravelTime().seconds();
                                distance = leg.getRoute() == null ? Double.NaN : leg.getRoute().getDistance();
                            }



                        }
                    }
                    writer.write(person.getId().toString() + ";"
                            + routing_mode + ";" //captures the main mode
                            + mode_detailed + ";"
                            + preceding_purpose + ";"
                            + following_purpose + ";"
                            + dep_time + ";"
                            + trav_time + ";"
                            + trip.getOriginActivity().getCoord().getX() + ";"
                            + trip.getOriginActivity().getCoord().getY() + ";"
                            + trip.getDestinationActivity().getCoord().getX()+ ";"
                            + trip.getDestinationActivity().getCoord().getY() + ";"
                            + distance + "\n");
                }

            }


            //remove initial "-" from pt chain
            /*if (routing_mode.equals("pt")){
                mode_detailed = mode_detailed.substring(1);
            }
            preceding_purpose = purpose.get(0);
            following_purpose = purpose.get(purpose.size()-1);*/
           /* writer.write(person.getId().toString() + ","
                    + routing_mode + "," //captures the main mode
                    + mode_detailed + ","
                    + preceding_purpose + ","
                    + following_purpose + ","
                    + dep_time + ","
                    + trav_time + ","
                    + distance + "\n");*/

        }

        writer.flush();
        writer.close();

    }
}
