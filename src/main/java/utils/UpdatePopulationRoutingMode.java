package utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;

/**
 * @author kaghog created on 26.10.2021
 * @project matsim-tools
 */
public class UpdatePopulationRoutingMode {
    public static void main(String[] args) throws IOException {

        String populationFile = args[0];
        String outputfile = args[1];


        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile(populationFile);

        ScenarioUtils.loadScenario(scenario);
        Population populationData = scenario.getPopulation();


        for (Person person : populationData.getPersons().values()) {

            for (final Plan plan : person.getPlans()) {
                for (final TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
                    for (final PlanElement pe : trip.getTripElements()) {
                        if (pe instanceof Leg) {
                            ((Leg) pe).setRoute(null);
                            switch (((Leg) pe).getMode()) {
                                case "transit_walk":
                                case "access_walk":
                                case "egress_walk":
                                    ((Leg) pe).setMode("walk");
                                    break;
                            }

                            if (trip.getLegsOnly().size() <= 1) {
                                pe.getAttributes().putAttribute("routingMode", ((Leg) pe).getMode());
                            } else {
                                pe.getAttributes().putAttribute("routingMode", "pt");
                            }
                        }
                    }
                }

            }

        }
        new PopulationWriter(populationData).write(outputfile);

        System.out.println("Finished");
    }
}
