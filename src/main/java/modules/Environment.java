package modules;

import edu.memphis.ccrg.lida.environment.EnvironmentImpl;
import edu.memphis.ccrg.lida.framework.tasks.FrameworkTaskImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ws3dproxy.CommandExecException;
import ws3dproxy.WS3DProxy;
import ws3dproxy.model.Creature;
import ws3dproxy.model.Leaflet;
import ws3dproxy.model.Thing;
import ws3dproxy.model.World;
import ws3dproxy.util.Constants;

public class Environment extends EnvironmentImpl {

    private static final int DEFAULT_TICKS_PER_RUN = 100;
    private int ticksPerRun;
    private WS3DProxy proxy;
    private Creature creature;
    private Thing food;
    private Thing jewel;
    private Thing deliverySpot;
    private List<Thing> thingAhead;
    private Thing leafletJewel;
    private String canCompleteLeaflet = "no";
    public static String currentAction;

    public Environment() {
        this.ticksPerRun = DEFAULT_TICKS_PER_RUN;
        this.proxy = new WS3DProxy();
        this.creature = null;
        this.food = null;
        this.jewel = null;
        this.thingAhead = new ArrayList<>();
        this.leafletJewel = null;
        currentAction = "rotate";
    }

    @Override
    public void init() {
        super.init();
        ticksPerRun = (Integer) getParam("environment.ticksPerRun", DEFAULT_TICKS_PER_RUN);
        taskSpawner.addTask(new BackgroundTask(ticksPerRun));

        try {
            System.out.println("Reseting the WS3D World ...");
            proxy.getWorld().reset();
            creature = proxy.createCreature(100, 100, 0);
            World.createDeliverySpot(200, 200);
            creature.start();
            System.out.println("Starting the WS3D Resource Generator ... ");
            World.grow(1);
            Thread.sleep(4000);
            creature.updateState();
            getDeliverySpotObj();
            System.out.println("DemoLIDA has started...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getDeliverySpotObj() {
        try {
            for (var item : World.getWorldEntities()) {
                if (item.getCategory() == Constants.categoryDeliverySPOT) {
                    deliverySpot = item;
                    break;
                }
            }
        } catch (CommandExecException ex) {
            System.getLogger(Environment.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    private class BackgroundTask extends FrameworkTaskImpl {

        public BackgroundTask(int ticksPerRun) {
            super(ticksPerRun);
        }

        @Override
        protected void runThisFrameworkTask() {
            updateEnvironment();
            performAction(currentAction);
        }
    }

    @Override
    public void resetState() {
        currentAction = "rotate";
    }

    @Override
    public Object getState(Map<String, ?> params) {
        Object requestedObject = null;
        String mode = (String) params.get("mode");
        switch (mode) {
            case "food":
                requestedObject = food;
                break;
            case "jewel":
                requestedObject = jewel;
                break;
            case "thingAhead":
                requestedObject = thingAhead;
                break;
            case "leafletJewel":
                requestedObject = leafletJewel;
                break;
            case "canCompleteLeaflet":
                requestedObject = canCompleteLeaflet;
            default:
                break;
        }
        return requestedObject;
    }

    public void updateEnvironment() {
        creature.updateState();
        food = null;
        jewel = null;
        leafletJewel = null;
        thingAhead.clear();

        for (Thing thing : creature.getThingsInVision()) {
            if (creature.calculateDistanceTo(thing) <= Constants.OFFSET) {
                // Identifica o objeto proximo
                thingAhead.add(thing);
                break;
            } else if (thing.getCategory() == Constants.categoryJEWEL) {
                if (leafletJewel == null) {
                    // Identifica se a joia esta no leaflet
                    boolean found = false;
                    for (Leaflet leaflet : creature.getLeaflets()) {
                        if (leaflet.ifInLeaflet(thing.getMaterial().getColorName())
                                && leaflet.getTotalNumberOfType(thing.getMaterial().getColorName()) > leaflet.getCollectedNumberOfType(thing.getMaterial().getColorName())) {
                            leafletJewel = thing;
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        leafletJewel = thing;
                    }
                } else {
                    // Identifica a joia que nao esta no leaflet
                    jewel = thing;
                }
            } else if (food == null && creature.getFuel() <= 300.0
                    && (thing.getCategory() == Constants.categoryFOOD
                    || thing.getCategory() == Constants.categoryPFOOD
                    || thing.getCategory() == Constants.categoryNPFOOD)) {

                // Identifica qualquer tipo de comida
                food = thing;
            }

        }

        boolean canComplete = false;
        for (Leaflet l : creature.getLeaflets()) {
            if (isLeafletCompleted(l)) {
                System.out.println("COMPLETO\n\n");
                canComplete = true;
                break;
            }
        }

        canCompleteLeaflet = canComplete && thingAhead.isEmpty() ? "yes" : "no";
    }

    @Override
    public void processAction(Object action) {
        String actionName = (String) action;
        currentAction = actionName.substring(actionName.indexOf(".") + 1);
    }

    private void performAction(String currentAction) {
        try {
            System.out.println("AÇÃO: " + currentAction + "\n\nÃ");
            switch (currentAction) {
                case "rotate":
                    creature.rotate(3.0);

                    break;
                case "gotoFood":
                    if (food != null) {
                        while (true) {
                            creature.updateState();
                            if (creature.calculateDistanceTo(food) <= Constants.OFFSET) {
                                break;
                            }
                            creature.moveto(4.0, food.getX1(), food.getY1());
                        }

                    } else {
                        creature.move(0.0, 0.0, 0.0);
                    }
                    break;
                case "gotoJewel":
                    if (leafletJewel != null) {

                        while (true) {
                            creature.updateState();
                            if (creature.calculateDistanceTo(leafletJewel) <= Constants.OFFSET) {
                                break;
                            }
                            creature.moveto(4.0, leafletJewel.getX1(), leafletJewel.getY1());
                        }

                    } else {
                        creature.move(0.0, 0.0, 0.0);
                    }
                    break;
                case "get":
                    creature.move(0.0, 0.0, 0.0);
                    if (thingAhead != null) {
                        for (Thing thing : thingAhead) {
                            if (thing.getCategory() == Constants.categoryJEWEL) {
                                creature.putInSack(thing.getName());
                            } else if (thing.getCategory() == Constants.categoryFOOD || thing.getCategory() == Constants.categoryNPFOOD || thing.getCategory() == Constants.categoryPFOOD) {
                                creature.eatIt(thing.getName());
                            } else if (thing.getCategory() == Constants.categoryDeliverySPOT) {
                                for (Leaflet l : creature.getLeaflets()) {
                                    if (isLeafletCompleted(l)) {
                                        creature.deliverLeaflet(l.getID().toString());
                                    }
                                }
                            }
                        }
                    }
                    this.resetState();
                    break;
                case "gotoDeliverySpot":
                    while (true) {
                        creature.updateState();
                        if (creature.calculateDistanceTo(deliverySpot) <= Constants.OFFSET) {
                            break;
                        }
                        creature.moveto(4.0, deliverySpot.getX1(), deliverySpot.getY1());
                    }

                    break;
                default:
                    creature.move(0.0, 0.0, 0.0);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isLeafletCompleted(Leaflet l) {
        boolean completed = true;
        var items = l.getItems();
        for (var i : items.entrySet()) {
            var array = i.getValue();
            int required = array[0];
            int collected = array[1];

            if (required > collected) {
                completed = false;
                break;
            }

        }
        return completed;
    }
}
