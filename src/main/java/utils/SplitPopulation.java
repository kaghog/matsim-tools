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
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author kaghog
 */
public class SplitPopulation {
    public static void main(String[] args) throws IOException, CommandLine.ConfigurationException {

        CommandLine cmd = (new CommandLine.Builder(args))
                .requireOptions("input-path", "output-path")
                .allowOptions("suffix", "split-num")
                .build();

        String population = cmd.getOptionStrict("input-path");
        String outputDir = cmd.getOptionStrict("output-path");
        String suffix = cmd.hasOption("suffix") ? cmd.getOption("suffix").get() : "population_0.25cut_";
        int splitNum = cmd.hasOption("split-num") ? Integer.parseInt(cmd.getOption("split-num").get()) : 4;


        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile(population);

        ScenarioUtils.loadScenario(scenario);

        Population populationData = scenario.getPopulation();
        PopulationFactory populationFactory = populationData.getFactory();

        Population[] populations = new Population[splitNum];
        for(int i = 0; i < splitNum; i++){
            populations[i] = PopulationUtils.createPopulation(config);
        }

        int popSize = populationData.getPersons().size();
        System.out.println("Total full Population: " + popSize);
        int shareSize  = popSize / splitNum;

        int popCount = 1;
        int n_persons = 1;

        String outputfile = outputDir + "/" + suffix + popCount + ".xml.gz";

        for (Person person : populationData.getPersons().values()) {
            if (popCount < splitNum){
                if ( n_persons <= popCount*shareSize) {
                    populations[popCount - 1].addPerson(person);
                } else {
                    new PopulationWriter(populations[popCount - 1]).write(outputfile);
                    System.out.println(suffix+popCount + " total population: " + (n_persons-1)/popCount);
                    popCount++;
                    outputfile = outputDir + "/" + suffix + popCount + ".xml.gz";
                    populations[popCount - 1].addPerson(person);
                }
            } else {
                populations[popCount - 1].addPerson(person);
            }
            n_persons++;

        }
        new PopulationWriter(populations[popCount - 1]).write(outputfile);
        System.out.println(suffix+popCount + " total population after cut: " + (n_persons-1)/popCount + ", and remainder added: " + (n_persons-1)%popCount );
        System.out.println("total population after cut: " + (n_persons-1));

        System.out.println("Finished");


    }
}
