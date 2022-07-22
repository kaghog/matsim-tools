package utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author kaghog created on 26.10.2021
 * @project matsim-tools
 */
public class GenerateSamplePopulation {

    public static void main(String[] args) throws IOException {

        String populationFile = args[0];
        String outputfile = args[1];
        double sample_size = Double.parseDouble(args[2]);

        if (sample_size<=0||sample_size>=1){
            throw new IllegalStateException("Invalid Sample size. sample size should be specified as a percentage fraction between 0 and 1");
        }

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile(populationFile);

        ScenarioUtils.loadScenario(scenario);
        Population populationData = scenario.getPopulation();

        List<Id<Person>> allPersons = new ArrayList<>();


        for (Person person : populationData.getPersons().values()) {
            allPersons.add(person.getId());
        }

        List<Id<Person>> removePerson = selectNonSamplePersons(allPersons, sample_size);

        System.out.println(removePerson.size() + " persons (" + removePerson.size()/allPersons.size() +"%) of total population will be removed");

        for (Id<Person> id: removePerson){
            populationData.removePerson(id);
        }


        new PopulationWriter(populationData).write(outputfile);

        System.out.println("Finished");


    }

    public static List<Id<Person>>  selectNonSamplePersons(List<Id<Person>> personIdList, double sample_size, Random r){
        int totalPop = personIdList.size();
        System.out.println("Total number of people in the 100% sample is: " + totalPop);

        int removeNum = (int)(totalPop * (1-sample_size));

        for (int i = totalPop-1; i>=totalPop-removeNum;--i){
            Collections.swap(personIdList, i, r.nextInt(i+1));
        }


        return personIdList.subList(totalPop - removeNum, totalPop);
    }

    public static List<Id<Person>>  selectNonSamplePersons(List<Id<Person>> personIdList, double sample_size){

        return selectNonSamplePersons(personIdList, sample_size, ThreadLocalRandom.current());
    }

}
