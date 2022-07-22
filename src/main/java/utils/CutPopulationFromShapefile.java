package utils;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.*;
import org.matsim.api.core.v01.Coord;
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
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public class CutPopulationFromShapefile {
    public static void main(String[] args) throws IOException, CommandLine.ConfigurationException, URISyntaxException {

        CommandLine cmd = (new CommandLine.Builder(args))
                .requireOptions("input-path", "output-path", "extent-path")
                .allowOptions("suffix")
                .build();

        String population = cmd.getOptionStrict("input-path");
        String outputDir = cmd.getOptionStrict("output-path");
        String suffix = cmd.hasOption("suffix") ? cmd.getOption("suffix").get() : "population_zurich_";
        String extent = cmd.getOptionStrict("extent-path"); // "C:/Users/kaghog/git/eqasim-java-project-astra16/gis/zurich_city_5km.shp"

        extent = "file:///" + extent;
        DataStore dataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", new URL(extent)));
        SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
        SimpleFeatureCollection featureCollection = featureSource.getFeatures();
        SimpleFeatureIterator featureIterator = featureCollection.features();
        List<Polygon> polygons = new ArrayList<>();

        try {
            while (featureIterator.hasNext()) {
                SimpleFeature feature = featureIterator.next();
                Geometry geometry = (Geometry)feature.getDefaultGeometry();

                if (geometry instanceof MultiPolygon) {
                    MultiPolygon multiPolygon = (MultiPolygon)geometry;
                    if (multiPolygon.getNumGeometries() != 1) {
                        throw new IllegalStateException("Extent shape is non-connected.");
                    }

                    polygons.add((Polygon)multiPolygon.getGeometryN(0));
                } else {
                    if (!(geometry instanceof Polygon)) {
                        throw new IllegalStateException("Expecting polygon geometry!");
                    }
                    polygons.add((Polygon)geometry);
                }
            }
        } finally {
            featureIterator.close();
        }

        Polygon polygon = polygons.get(0);

        ShapeExtent shapeExtent = new ShapeExtent(polygon);

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile(population);

        ScenarioUtils.loadScenario(scenario);

        Population populationData = scenario.getPopulation();
        PopulationFactory populationFactory = populationData.getFactory();


        int popSize = populationData.getPersons().size();

        Population newPop = PopulationUtils.createPopulation(config);
        Population newPopCars = PopulationUtils.createPopulation(config);


        String outputfile = outputDir + "/" + suffix + "" + ".xml";
        String outputfileCars = outputDir + "/" + suffix + "cars" + ".xml";
        String outputfileCsv = outputDir + "/" + suffix + "all" + ".csv";

        //track modal split
        int cars = 0;
        int bikes = 0;
        int pt = 0;
        int car_pax = 0;
        int walk = 0;
        int truck = 0;

        BufferedWriter writer = IOUtils.getBufferedWriter(outputfileCsv);
        writer.write("person_id;person;mode;departure_time;travel_time;preceding_purpose;following_purpose;start_link;end_link;origin_x;origin_y;destination_x;destination_y\n");


        for (Person person : populationData.getPersons().values()) {

            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements());
            int n = 1;
            for (TripStructureUtils.Trip trip : trips) {


                //filter out outside trips
                if ( shapeExtent.isInside(trip.getDestinationActivity().getCoord()) |
                        shapeExtent.isInside(trip.getOriginActivity().getCoord()) ) {

                    for (Leg leg : trip.getLegsOnly()) {
                        String routing_mode = leg.getAttributes().getAttribute("routingMode") == null ? "" : leg.getAttributes().getAttribute("routingMode").toString();

                        //checkign for cases when the pt trips also include pt_interaction - do not consider those trips here
                        if (routing_mode.equals("pt") & !leg.getMode().equals("pt")) {
                            continue;
                        }

                        leg.getAttributes().putAttribute("routingMode", leg.getMode());

                        // Create csv for the car trips
                        writer.write(person.getId().toString() + ";"
                                + person.getId().toString() + n + ";"
                                + leg.getMode() + ";"
                                + leg.getDepartureTime().seconds() + ";"
                                + leg.getTravelTime().seconds() + ";"
                                + trip.getOriginActivity().getType() + ";"
                                + trip.getDestinationActivity().getType() + ";"
                                + trip.getOriginActivity().getLinkId().toString() + ";"
                                + trip.getDestinationActivity().getLinkId().toString() + ";"
                                + trip.getOriginActivity().getCoord().getX() + ";"
                                + trip.getOriginActivity().getCoord().getY() + ";"
                                + trip.getDestinationActivity().getCoord().getX() + ";"
                                + trip.getDestinationActivity().getCoord().getY() + "\n");

                        Person newPerson = populationFactory.createPerson(Id.create(person.getId().toString() + n, Person.class));
                        Plan p = populationFactory.createPlan();


                        //add first activity to plan
                        p.addActivity(trip.getOriginActivity());

                        //remove the route and set the mode to drt
                        if (leg.getRoute() != null) {
                            leg.setRoute(null);
                        }
                        //leg.setMode("drt");
                        leg.getAttributes().putAttribute("routingMode", leg.getMode());

                        p.addLeg(leg);

                        //add second activity
                        p.addActivity(trip.getDestinationActivity());

                        //add plan to person
                        newPerson.addPlan(p);

                        //add original personID
                        newPerson.getAttributes().putAttribute("origPersonId", person.getId().toString());
                        newPop.addPerson(newPerson);

                        if (leg.getMode().equals("car") || leg.getMode().equals("car_passenger")) {
                            newPopCars.addPerson(newPerson);
                        }
                        n++;

                        switch (leg.getMode()) {
                            case "pt":
                                pt++;
                                break;
                            case "car":
                                cars++;
                                break;
                            case "car_passenger":
                                car_pax++;
                                break;
                            case "walk":
                                walk++;
                                break;
                            case "bike":
                                bikes++;
                                break;
                            case "truck":
                                truck++;
                                break;
                        }


                    }
                }
            }
        }
        new PopulationWriter(newPop).write(outputfile);
        new PopulationWriter(newPopCars).write(outputfileCars);
        writer.flush();
        writer.close();


        System.out.println("Total full Population: " + popSize);

        System.out.println("modal shares cutout: ");
        System.out.println("bike: " + bikes);
        System.out.println("car: " + cars);
        System.out.println("car_passenger: " + car_pax);
        System.out.println("pt: " + pt);
        System.out.println("walk: " + walk);
        System.out.println("truck: " + truck);

        int total_trips = bikes + cars + car_pax + pt + walk;
        System.out.println("Total trips for shares: " + total_trips);

        System.out.println("modal shares cutout excluding trucks (proportion): ");
        System.out.println("bike: " + bikes/(double)total_trips);
        System.out.println("car: " + cars/(double)total_trips);
        System.out.println("car_passenger: " + car_pax/(double)total_trips);
        System.out.println("pt: " + pt/(double)total_trips);
        System.out.println("walk: " + walk/(double)total_trips);
        System.out.println("Finished");


    }

}
    class ShapeExtent {
        private final GeometryFactory factory = new GeometryFactory();
        private final Polygon polygon;

        ShapeExtent(Polygon polygon) {
            this.polygon = polygon;
        }

        public boolean isInside(Coord coord) {
            Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
            Point point = this.factory.createPoint(coordinate);
            return this.polygon.contains(point);
        }

    }

