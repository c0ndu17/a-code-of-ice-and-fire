package com.codingame.antiyoy;

// Grants access to Constants.CONSTANTE
import static com.codingame.antiyoy.Constants.*;

import com.codingame.game.Player;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GameState {
    private  Cell[][] map;

    private List<Building> HQs = new ArrayList<>();
    private ArrayList<Building> buildings = new ArrayList<>();
    private Map<Integer, Unit> units = new HashMap<>();

    private ArrayList<AtomicInteger> playerGolds = new ArrayList<>();

    public GameState() {
        // create full map
        this.map = new Cell[MAP_WIDTH][MAP_HEIGHT];
        for(int x = 0; x < MAP_WIDTH; ++x)
            for (int y = 0; y < MAP_HEIGHT; ++y)
                this.map[x][y] = new Cell(x, y);

        for (int i = 0; i < PLAYER_COUNT; ++i)
            this.playerGolds.add(new AtomicInteger(2 * UNIT_COST[1]));
    }

    // getters
    public Cell getCell(int x, int y) { return this.map[x][y]; }

    public Unit getUnit(int id) { return this.units.get(id); }

    public int getGold(int idx) { return playerGolds.get(idx).intValue(); }
    public AtomicInteger getAtomicGold(int idx) { return playerGolds.get(idx); }

    // map creation methods
    private Cell getSymmetricCell(int x, int y) { return this.map[MAP_WIDTH - x - 1][MAP_HEIGHT - y - 1]; }

    private boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < MAP_WIDTH && y >= 0 && y < MAP_HEIGHT;
    }
    public boolean isInside(int x, int y) {
        return isWithinBounds(x, y) && this.map[x][y].getOwner() != VOID;
    }

    private void computeNeighbours() {
        for (int x = 0; x < MAP_WIDTH; ++x) {
            for (int y = 0; y < MAP_HEIGHT; ++y) {
                if (isInside(x, y-1))
                    map[x][y].setNeighbour(UP, map[x][y-1]);
                if (isInside(x+1, y))
                    map[x][y].setNeighbour(RIGHT, map[x+1][y]);
                if (isInside(x, y+1))
                    map[x][y].setNeighbour(DOWN, map[x][y+1]);
                if (isInside(x-1, y))
                    map[x][y].setNeighbour(LEFT, map[x-1][y]);
            }
        }
    }


    /*********************************

     MAP GENERATOR STARTS HERE

     /*******************************/

    public int getValue(int x, int y) {
        if (x < 0 || x >= MAP_WIDTH || y < 0 || y >= MAP_HEIGHT)
            return NEUTRAL;
        else
            return this.map[x][y].getOwner();
    }

    public List<Integer> getNeighbourhood(int x, int y) {
        List<Integer> neighbourhood = new ArrayList<>();

        for (int ix = -1; ix < 2; ++ix) {
            for (int iy = -1; iy < 2; ++iy) {
                neighbourhood.add(getValue(x+ix,y+iy));
            }
        }
        return neighbourhood;
    }

    // debug, à supprimer...
    public void printMap(Cell [][] currentMap)
    {
        for(int x=0; x < currentMap.length; x++)
        {
            for(int y=0; y < currentMap[x].length; y++)
            {
                System.out.print(currentMap[x][y].getOwner()+2);

            }
            System.out.println();
        }
    }

    // Cellular automata
    public void updateMap() {
        // copy current map
        Cell [][] currentMap = new Cell[MAP_WIDTH][MAP_HEIGHT];

        for(int x = 0; x < MAP_WIDTH; x++) {
            for(int y=0; y< MAP_HEIGHT; y++) {
                currentMap[x][y] = this.map[x][y];
            }
        }



        // Then we apply the automata to the copied map 'currentMap'
        // before copying back to this.map
        for (int x = 0; x < MAP_WIDTH; ++x) {
            System.out.println("Starting line" + x);
            printMap(currentMap);
            System.out.println("-\n");
            for (int y = 0; y < MAP_HEIGHT; ++y) {
                List<Integer> neighbourhood = getNeighbourhood(x,y);

                if(Collections.frequency(neighbourhood, NEUTRAL) >= MAPGENERATOR_T)
                    currentMap[x][y].setOwner(NEUTRAL);
                else
                    currentMap[x][y].setOwner(VOID);
            }
        }

        for(int i = 0; i < this.map.length; i++)
            this.map[i] = currentMap[i].clone();
    }

    // we define here functions used in FindConnectedComponents()
    public boolean not_visited(int x, int y, Cell [][] visited) {
        //if coordinate is invalid, we don't want to visit it
        if(!(x >=0 && x < MAP_WIDTH && y >=0 && y < MAP_HEIGHT))
            return false;

        return visited[x][y].getOwner() == NEUTRAL;
    }

    public void dfs(int x, int y, List<Vector2> currentConnectedComponent, Cell [][] visited) {
        Vector2 currentPair = new Vector2(x,y);
        currentConnectedComponent.add(currentPair);

        visited[x][y].setOwner(2);

        if (not_visited(x-1, y, visited))
            dfs(x-1, y, currentConnectedComponent, visited);
        if (not_visited(x+1, y, visited))
            dfs(x+1, y, currentConnectedComponent, visited);
        if (not_visited(x, y-1, visited))
            dfs(x, y-1, currentConnectedComponent, visited);
        if (not_visited(x-1, y+1, visited))
            dfs(x, y+1, currentConnectedComponent, visited);
    }

    public List<List<Vector2>> findConnectedComponents() {
        // A connected component is a list of coordinates
        // And we store them in a list
        List<List<Vector2>> connectedComponents = new ArrayList<>();

        // We find the connected components with a depth first search
        // We copy the current map in a 'visited' variable
        // "seen" tiles will be marked as owner = 2, to differentiate between VOID and NEUTRAL
        Cell [][] visited = new Cell[this.map.length][];

        for(int i = 0; i < this.map.length; i++)
            visited[i] = this.map[i].clone();

        List<Vector2> currentConnectedComponent = new ArrayList<>();

        for (int x = 0; x < MAP_WIDTH; ++x) {
            for (int y = 0; y < MAP_HEIGHT; ++y) {
                if(visited[x][y].getOwner() == NEUTRAL)
                {
                    currentConnectedComponent = new ArrayList<>();
                    dfs(x, y, currentConnectedComponent, visited);
                    connectedComponents.add(currentConnectedComponent);
                }
            }
        }


        return connectedComponents;

    }



    public void generateMap() {
        int seed = 5590;
        Random generator = new Random(seed);

        for (int x = 0; x < MAP_WIDTH; ++x) {
            for (int y = 0; y < MAP_HEIGHT; ++y) {
                if(generator.nextFloat() > MAPGENERATOR_R)
                    this.map[x][y].setOwner(NEUTRAL);
                else
                    this.map[x][y].setOwner(VOID);
            }
        }


        //apply automata
        for (int i=0; i < MAPGENERATOR_ITERATIONSAUTOMATA; ++i)
        {
            updateMap();
        }

        /*
        // invert VOID and TILE as the cellular automata generates more caves like maps
        for (int x = 0; x < MAP_WIDTH; ++x) {
            for (int y = 0; y < MAP_HEIGHT; ++y) {
                // + 2 to be 0 or 1
                // +1 then %2 to invert 0 and 1
                // -2 to get back to -2 or -1
                int inverted = ((this.map[x][y].getOwner()+2 +1)%2) - 2;
                this.map[x][y].setOwner(inverted);
            }
        }
        */
        /*
        // Remove half for symmetry
        for(int x=0; x < MAP_WIDTH; x++)
        {
            for(int y=0; y < x+1; y++)
            {
                this.map[MAP_WIDTH-1-x][MAP_HEIGHT-1-y].setOwner(VOID);
            }
        }
        */

        // Now we need 1 connected component
        // If we have k > 1 connected components, we link them
        //List<List<Vector2>> connectedComponents = findConnectedComponents();




//        for (int x = 0; x < MAP_WIDTH; ++x) {
//            for (int y = 0; y < MAP_HEIGHT; ++y) {
//                // do not modify nearby HQs cells
//                if (x + y <= 4)
//                    continue;
//                if (MAP_WIDTH - x + MAP_HEIGHT - y <= 4)
//                    continue;
//
//                // do not modify center 4x4 square
//                if (Math.abs(MAP_WIDTH/2 - x) < 4 && Math.abs(MAP_HEIGHT/2 - y) < 4)
//                    continue;
//
//                double random = Math.random() * 100;
//                int owner = random < 20 ? VOID : NEUTRAL;
//                this.map[x][y].setOwner(owner);
//                this.getSymmetricCell(x, y).setOwner(owner);
//            }
//        }
        // Restore HQs cells
        this.computeNeighbours();
    }


    /*********************************

        MAP GENERATOR ENDS HERE

    /*******************************/

    public void createHQs(int playersCount) throws Exception {
        if (playersCount != 2) {
            throw new Exception("More than 2 players mode not implemented");
        }

        // Build players HQs
        Building HQ0 = new Building(this.map[0][0], 0, BUILDING_TYPE.HQ);
        Building HQ1 = new Building(this.map[MAP_WIDTH-1][MAP_HEIGHT-1], 1, BUILDING_TYPE.HQ);
        this.HQs.add(HQ0);
        this.HQs.add(HQ1);
        HQ0.getCell().setOwner(0);
        HQ1.getCell().setOwner(1);
    }


    public List<Building> getHQs() { return this.HQs; }


    // kill methods
    private void killUnit(Unit unit) {
        unit.die();
        unit.doDispose();
        this.units.remove(unit.getId());
    }

    private void clearCell(Cell cell) {
        if (cell.getUnit() != null) {
            killUnit(cell.getUnit());
            cell.setUnit(null);
        }
        if (cell.getBuilding() != null) {
            Building building = cell.getBuilding();
            building.doDispose();
            this.buildings.removeIf(building1 -> building1.getX() == building.getX() && building1.getY() == building.getY());
            cell.setBuilding(null);
        }
    }

    private void killUnits(List<Unit> units) {
        units.forEach(unit -> killUnit(unit));
    }

    // init turn methods
    public void initTurn(int playerId) {
        this.computeActiveCells(playerId);
        this.killSeparatedUnits(playerId);
        this.computeGold(playerId);
        if (this.playerGolds.get(playerId).intValue() < 0) {
            negativeGoldWipeout(playerId);
        }
        this.units.forEach( (key, unit) -> { if (unit.getOwner() == playerId) unit.newTurn(); });
    }

    public void computeAllActiveCells() {
        for (int playerId = 0; playerId < PLAYER_COUNT; ++playerId)
            this.computeActiveCells(playerId);
    }

    private void computeActiveCells(int playerId) {
        // Set all inactive
        for (int x = 0; x < MAP_WIDTH; ++x) {
            for (int y = 0; y < MAP_HEIGHT; ++y) {
                if (this.map[x][y].getOwner() == playerId)
                    this.map[x][y].setInactive();
            }
        }

        ArrayList<Cell> queue = new ArrayList<>();
        Cell start = this.HQs.get(playerId).getCell();
        queue.add(start);
        start.setActive();

        // Reactivate cells connected to starting point
        while (!queue.isEmpty()) {
            Cell currentCell = queue.get(0);
            queue.remove(0);
            for (Cell cell : currentCell.getNeighbours()) {
                if (cell != null && !cell.isActive() && cell.getOwner() == playerId) {
                    cell.setActive();
                    queue.add(cell);
                }
            }
        }
    }


    private void killSeparatedUnits(int playerId) {
        List<Unit> toKill = new ArrayList<>();
        this.units.forEach((key, unit)-> {if (unit.isAlive() && unit.getOwner() == playerId && !unit.getCell().isActive()) toKill.add(unit); });
        killUnits(toKill);
    }

    private void computeGold(int playerId) {
        // increments golds of player
        for (int x = 0; x < MAP_WIDTH; ++x) {
            for (int y = 0; y < MAP_HEIGHT; ++y) {
                if (map[x][y].getOwner() == playerId && map[x][y].isActive())
                    this.playerGolds.get(playerId).addAndGet(CELL_INCOME);
            }
        }

        // increments golds for active mines
        for (Building building : this.buildings) {
            if (building.getOwner() == playerId && building.getType() == BUILDING_TYPE.MINE && building.getCell().isActive())
                this.playerGolds.get(playerId).addAndGet(MINE_INCOME);
        }

        // decrement for units
        for (Unit unit : this.units.values()) {
            if (unit.getOwner() == playerId && unit.isAlive())
                this.playerGolds.get(playerId).addAndGet(- UNIT_UPKEEP[unit.getLevel()]);
        }
    }

    private void negativeGoldWipeout(int playerId) {
        // Negative amount of gold: kill all units and reset to 0
        this.playerGolds.get(playerId).set(0);

        List<Unit> toKill = new ArrayList<>();
        this.units.forEach((key, unit)-> {if (unit.isAlive() && unit.getOwner() == playerId) toKill.add(unit); });
        killUnits(toKill);
    }

    // action methods
    public void addUnit(Unit unit) {
        // TRAIN method
        // kill previous unit
        clearCell(unit.getCell());

        this.units.put(unit.getId(), unit);
        unit.getCell().setOwner(unit.getOwner());
        unit.getCell().setUnit(unit);
        this.playerGolds.get(unit.getOwner()).addAndGet(-UNIT_COST[unit.getLevel()]);
    }

    public void moveUnit(Unit unit, Cell newPosition) {
        // MOVE method
        // free current cell
        clearCell(newPosition);

        unit.getCell().setUnit(null);
        unit.moved();
        unit.setX(newPosition.getX());
        unit.setY(newPosition.getY());
        unit.setCell(newPosition);

        newPosition.setOwner(unit.getOwner());
        // occupy new cell
        newPosition.setUnit(unit);
    }

    public void addBuilding(Building building) {
        this.buildings.add(building);
        building.getCell().setBuilding(building);
        this.playerGolds.get(building.getOwner()).addAndGet(-BUILDING_COST(building.getType()));
    }


    // referee methods
    private void sendMap(Player player) {
        for (int y = 0; y < MAP_HEIGHT; ++y) {
            StringBuilder line = new StringBuilder();
            for (int x = 0; x < MAP_WIDTH; ++x) {
                int owner = this.map[x][y].getOwner();
                char data;
                switch (owner) {
                    case VOID:
                        data = '#';
                        break;
                    case NEUTRAL:
                        data = '.';
                        break;
                    default:
                        // o for own player, x for opponent
                        if (owner == player.getIndex()) {
                            data = 'o';
                        } else {
                            data = 'x';
                        }
                        // capital letter iif active cell
                        if (this.map[x][y].isActive()) {
                            data = Character.toUpperCase(data);
                        }
                }
                line.append(data);
            }
            player.sendInputLine(line.toString());
        }
    }

    private void sendUnits(Player player) {
        // send unit count
        player.sendInputLine(String.valueOf(this.units.size()));

        // send units
        this.units.forEach((id, unit) -> {
            StringBuilder line = new StringBuilder();
            line.append(unit.getId())
                    .append(" ")
                    .append( (unit.getOwner() - player.getIndex() + PLAYER_COUNT) % PLAYER_COUNT) // always 0 for the player
                    .append(" ")
                    .append(unit.getLevel())
                    .append(" ")
                    .append(unit.getX())
                    .append(" ")
                    .append(unit.getY());
            player.sendInputLine(line.toString());
        });
    }

    private void sendBuildings(Player player) {
        // send building count
        player.sendInputLine(String.valueOf(this.HQs.size() + this.buildings.size()));

        // send HQ
        this.HQs.forEach(building -> {
            StringBuilder line = new StringBuilder();
            line.append(building.getIntType())
                    .append(" ")
                    .append( (building.getOwner() - player.getIndex() + PLAYER_COUNT) % PLAYER_COUNT) // always 0 for the player
                    .append(" ")
                    .append(building.getX())
                    .append(" ")
                    .append(building.getY());
            player.sendInputLine(line.toString());
        });

        this.buildings.forEach(building -> {
            StringBuilder line = new StringBuilder();
            line.append( (building.getOwner() - player.getIndex() + PLAYER_COUNT) % PLAYER_COUNT) // always 0 for the player
                    .append(" ")
                    .append(building.getIntType())
                    .append(" ")
                    .append(building.getX())
                    .append(" ")
                    .append(building.getY());
            player.sendInputLine(line.toString());
        });
    }

    public void sendState(Player player) {
        // send gold
        player.sendInputLine(String.valueOf(this.playerGolds.get(player.getIndex())));

        sendMap(player);
        sendBuildings(player);
        sendUnits(player);
    }

    public void debugViews() {
        System.err.println(buildings.size() + " buildings, " + units.size() + " units");
        // units.forEach((id, unit) -> {System.err.println(unit.getId() + ": " +unit.getX() + " " + unit.getY());});
    }

    public List<AtomicInteger> getScores() {
        List<AtomicInteger> scores = new ArrayList<>(this.playerGolds);
        this.units.forEach((id, unit) -> {
            scores.get(unit.getOwner()).addAndGet(UNIT_COST[unit.getLevel()]);
        });
        return scores;
    }
}