package utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;

/**
 * @author kaghog created on 22.07.2022
 * @project matsim-tools
 */
public class CombinePopulation {
    public static void main(String[] args) throws IOException, CommandLine.ConfigurationException {

        CommandLine cmd = (new CommandLine.Builder(args))
                .requireOptions("input-path", "populations", "output-path")
                .allowOptions("suffix")
                .build();

        String popPath = cmd.getOptionStrict("input-path");
        String outputDir = cmd.getOptionStrict("output-path");
        String suffix = cmd.hasOption("suffix") ? cmd.getOption("suffix").get() : "population";
        suffix.split(",");
        String[] populations = cmd.getOptionStrict("populations").split(",");




        Config config = ConfigUtils.createConfig();
        Population newPop = PopulationUtils.createPopulation(config);

        //PopulationReader popReader = new PopulationReader(scenario);

        int popSize = 0;

        for(int i = 0; i < populations.length; i++){
            Scenario scenario = ScenarioUtils.createScenario(config);
            PopulationReader popReader = new PopulationReader(scenario);
            popReader.readFile(popPath + "/" + populations[i]);
            ScenarioUtils.loadScenario(scenario);
            Population populationData = scenario.getPopulation();
            for (Person person : populationData.getPersons().values()) {
                newPop.addPerson(person);
            }
            popSize = popSize + populationData.getPersons().size();
        }

        String outputfile = outputDir + "/" + suffix +".xml.gz";

        assert(newPop.getPersons().size() == popSize);

        new PopulationWriter(newPop).write(outputfile);

        System.out.println("New population size: " + popSize);

        System.out.println("Finished");


    }
}
