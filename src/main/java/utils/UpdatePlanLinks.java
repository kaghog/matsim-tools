package utils;

/**
 * @author kaghog created on 31.10.2020
 *
 * This class checks if the location links of activities in a population file (plan file) exist in the network file. If they do not, they are removed
 * New link from the network file is assigned if it is the nearest link to the coordinates of the activities
 *
 * Output: population.xml updated
 * Prints our person IDs with changed linkIDs
 *
 * input parameters: "populationfile" (args[0]), "networkFile" (args[1]), "outputfile path (args[2])"
 *
 * The command line arguments should be passed as such:
 * "/my/path/to/population.xml"
 * "/my/path/to/networkfile.xml"
 * "/my/path/to/outputfile.xml"
 *
 */

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class UpdatePlanLinks {

    public static void main(String[] args) throws IOException {

        String population = args[0];
        String networkfile = args[1];
        String outputfile = args[2];

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile(population);

        Network network = scenario.getNetwork();
        new MatsimNetworkReader(network).readFile(networkfile);

        ScenarioUtils.loadScenario(scenario);
        Population populationData = scenario.getPopulation();



        //filter network

        Network newNetwork = NetworkUtils.createNetwork();

        TransportModeNetworkFilter modeNetworkFilter = new TransportModeNetworkFilter(network);
        modeNetworkFilter.filter(newNetwork, Collections.singleton(TransportMode.car));



        //for each person, get their link, check if link in network. if not in the list print the person id and count the number.
        // write a new population file for those not in the network or modify the population file and print a new one after assigning a new link from the network
        int relocatedAgents = 0;
        for (Person person : populationData.getPersons().values()) {

            for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
                if (element instanceof Activity) {
                    Activity activity = (Activity) element;

                    String linkId = activity.getLinkId().toString();
                    //System.out.println(person.getId().toString());

                    if (!(newNetwork.getLinks().containsKey(linkId))) {
                        Id newLinkId = NetworkUtils.getNearestLinkExactly(newNetwork, activity.getCoord()).getId();
                        System.out.println("person_id with changed links: " + person.getId().toString());

                        activity.setLinkId(newLinkId);
                        relocatedAgents++;
                    }


                }
            }

        }
        new PopulationWriter(populationData).write(outputfile);

        System.out.println("Number of relocated agents: " + relocatedAgents);
        System.out.println("Completed");
    }

}