package utils;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProcessStuckTrips {
    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {

        CommandLine cmd = (new CommandLine.Builder(args))
                .requireOptions("input-path", "output-path")
                .allowOptions("" )
                .build();

        String eventFilePath = cmd.getOptionStrict("input-path");
        String outputfile = cmd.getOptionStrict("output-path");

        BufferedWriter writer = IOUtils.getBufferedWriter(outputfile);
        writer.write("person_id;mode;link_id\n");

        EventsManager eventsManager = EventsUtils.createEventsManager();
        // create an object for the EventHandler and add to the event manager
        FindStuckTrips myEventHandler = new FindStuckTrips();
        eventsManager.addHandler(myEventHandler);

        // read the events from our events file
        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);

        // the events manager will automatically process the read events
        matsimEventsReader.readFile(eventFilePath);

        for (Map.Entry<String, String[]> stuckItem: myEventHandler.getStuckPersons().entrySet()){

            writer.write(stuckItem.getKey() + ";"
                            + stuckItem.getValue()[0] + ";"
                            + stuckItem.getValue()[1] + "\n");

        }
        writer.flush();
        writer.close();


    }
}

class FindStuckTrips implements PersonStuckEventHandler {

    private final Map<String, String[]> stuckPersons = new HashMap<>();

    @Override
    public void handleEvent(PersonStuckEvent event) {
        String[] modeLink = new String[2];
        modeLink[0] = event.getLegMode();
        modeLink[1] = event.getLinkId() != null ? event.getLinkId().toString() : "";

        stuckPersons.put(event.getPersonId().toString(), modeLink);


    }

    public Map<String, String[]> getStuckPersons(){
        return stuckPersons;
    }
}