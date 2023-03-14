package utils;

import java.io.BufferedWriter;
import java.io.IOException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


//author: @balacm

public class ExtractPTStopInfo {

	public static void main(String[] args) throws IOException {
		
			
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createMutableScenario(config);
		
		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[0]);
		
		BufferedWriter writer = IOUtils.getBufferedWriter(args[1]);
		writer.write("personId,accessStop,egressStop\n");
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			boolean first = true;
			Id<TransitStopFacility> accessStop = null;
			Id<TransitStopFacility> egressStop = null;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					if (((Leg) pe).getMode().equals("pt")) {
						if (first) {
							accessStop = ((DefaultTransitPassengerRoute)((Leg) pe).getRoute()).getAccessStopId();
							egressStop = ((DefaultTransitPassengerRoute)((Leg) pe).getRoute()).getEgressStopId();
							first = false;
						}
						else {
							egressStop = ((DefaultTransitPassengerRoute)((Leg) pe).getRoute()).getEgressStopId();
						}
					}
				}
			}
			if (accessStop == null || egressStop == null) {
				writer.write(person.getId().toString() + "," + "nan" + "," + "nan" + "\n");

			} else {
			
			writer.write(person.getId().toString() + "," + accessStop.toString() + "," + egressStop.toString() + "\n");
		
			}
		}
		writer.flush();
		writer.close();
	}
}
