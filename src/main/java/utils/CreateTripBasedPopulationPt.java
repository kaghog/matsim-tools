package utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CreateTripBasedPopulationPt {
    public static void main(String[] args)throws IOException, CommandLine.ConfigurationException {

    	CommandLine cmd = (new CommandLine.Builder(args))
                .requireOptions("input-path", "output-path")
                .allowOptions("min-distance")
                .build();

		
		 String population = cmd.getOptionStrict("input-path"); String outputfile =
		 cmd.getOptionStrict("output-path");
		 
		Boolean setMinTripLength = cmd.hasOption("min-distance");
		double minDistance = setMinTripLength ? Integer.parseInt(cmd.getOption("min-distance").get()) : 0.0;


        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile(population);

        ScenarioUtils.loadScenario(scenario);

        Population populationData = scenario.getPopulation();
        PopulationFactory populationFactory = populationData.getFactory();

        Population newPop = PopulationUtils.createPopulation(scenario.getConfig());

        //filter out trips less than 1km
        for (final Person person : scenario.getPopulation().getPersons().values()) {
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements());
            int n = 11;

            for (TripStructureUtils.Trip trip : trips) {
                double distance = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord());
                if (distance >= minDistance) {


                    //remove the route and set the mode
                    for (Leg leg : trip.getLegsOnly()) {
                    	
                    	//skip freight transport
                    	if(leg.getMode().equals("truck")) {
                    		continue;
                    	}

                        Person newPerson = populationFactory.createPerson(Id.create(person.getId().toString() + n, Person.class));
                        Plan p = populationFactory.createPlan();

                        //add first activity to plan
                        p.addActivity(trip.getOriginActivity());

                        if (leg.getRoute() != null) {
                            leg.setRoute(null);
                        }
                        leg.setMode("pt");
                        leg.getAttributes().putAttribute("routingMode", leg.getMode());

                        p.addLeg(leg);


                        //add second activity
                        p.addActivity(trip.getDestinationActivity());

                        //add plan to person
                        newPerson.addPlan(p);

                        //add the person attributes
                        Map<String, Object> attributes = person.getAttributes().getAsMap();
                        
                        for (Entry<String, Object> entry: attributes.entrySet()) {
                        	newPerson.getAttributes().putAttribute(entry.getKey(), entry.getValue());
                        }
               
                        //add original personID
                        newPerson.getAttributes().putAttribute("origPersonId", person.getId().toString());

                        newPop.addPerson(newPerson);
                        n++;
                    }
                }

            }
        }

        new PopulationWriter(newPop).write(outputfile);

        System.out.println("Finished");


    }
}