package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.Terrain;

import java.util.*;
import java.util.stream.*;

import static java.lang.Math.max;

public class Bot {

    private static final int minSpeed = 0;
    private static final int speedState1 = 3;
    private static final int initialSpeed = 5;
    private static final int speedState2 = 6;
    private static final int speedState3 = 8;
    private static final int maxSpeed = 9;
    private static final int boostSpeed = 15;
    private static final int[] speedState = {minSpeed, speedState1, initialSpeed, speedState2, speedState3, maxSpeed, boostSpeed};
    private final static Command ACCELERATE = new AccelerateCommand();
    private final static Command DECELERATE = new DecelerateCommand(); // NOT USED
    private final static Command DO_NOTHING = new DoNothingCommand();
    private final static Command FIX = new FixCommand();

    private final static Command LIZARD = new LizardCommand();
    private final static Command OIL = new OilCommand();
    private final static Command BOOST = new BoostCommand();
    private final static Command EMP = new EmpCommand();
    private static Command TWEET; // Not final because it can be reassigned

    private final static Command TURN_RIGHT = new ChangeLaneCommand(1);
    private final static Command TURN_LEFT = new ChangeLaneCommand(-1);

    public Bot() {
    }

    public Command run(GameState gameState) {
        Car myCar = gameState.player;
        Car opponent = gameState.opponent;
        //blok maksimal yang dapat ditempuh player di lane yg sama berdasarkan speed dan ketersediaan boost
        List<Lane> blocksMax = getBlocks(myCar.position.lane, myCar.position.block, gameState, 0);
        List<Terrain> terrainMax = blocksMax.stream().map(element -> element.terrain).collect(Collectors.toList());
        //blok yang dapat ditempuh player di lane kanannya
        List<Lane> rightblocks = getBlocks(myCar.position.lane, myCar.position.block, gameState, 1);
        List<Terrain> terrainRight = rightblocks.stream().map(element -> element.terrain).collect(Collectors.toList());
        //blok maksimal yang dapat ditempuh player di lane kirinya
        List<Lane> leftblocks = getBlocks(myCar.position.lane, myCar.position.block, gameState, -1);
        List<Terrain> terrainLeft = leftblocks.stream().map(element -> element.terrain).collect(Collectors.toList());
        //block yang dapat ditempuh player di lane yang sama jika tidak accelerate atau boost
        List<Lane> blocks = (blocksMax.size() >= myCar.speed) ? ((myCar.boostCounter == 1) ? blocksMax.subList(0, Bot.maxSpeed) : blocksMax.subList(0, myCar.speed)) : blocksMax;
        List<Terrain> terrainBlocks = blocks.stream().map(element -> element.terrain).collect(Collectors.toList());
        //block yang dapat ditempuh player di lane yang sama jika accelerate (jika tidak memungkinkan maka sama dengan list lane blocks)
        List<Lane> blocksAcc = (checkAcc(gameState)) ? ((blocksMax.size() >= higherSpeed(myCar.speed)) ? blocksMax.subList(0, higherSpeed(myCar.speed)) : blocksMax) : blocks;

        if (myCar.damage >= 5) {// Fix if car completely broken
            return FIX;
        }

        if (myCar.speed == minSpeed) {// ACCELERATE/BOOST IF SPEED 0
            if (canBoost(gameState)) {
                return BOOST;
            }
            return ACCELERATE;
        }

        if (myCar.position.block >= 1485) { //ACCELERATE atau BOOST jika bisa langsung ke garis finish dan tidak ada cybertruck
            List<Boolean> cybertruck = blocksMax.stream().map(element -> element.cyberTruck).collect(Collectors.toList());
            if (canBoost(gameState) && (!cybertruck.contains(true))) {
                return BOOST;
            }
            if ((myCar.position.block + higherSpeed(myCar.speed) >= 1500) && checkAcc(gameState) && (!cybertruck.contains(true))) {
                return ACCELERATE;
            }
            if (((myCar.position.block + myCar.speed >= 1500) && (!cybertruck.contains(true))) || countDamage(blocks) == 0) {
                return attack(gameState);
            }
        }

        if ((opponent.position.block >= 1400 || myCar.position.block >= 1400) && (countDamage(blocks) == 0)) { //Mechanism END Condition, use EMP to stop enemy
            if (PowerUp(PowerUps.EMP, myCar.powerups) > 0 && (myCar.position.block < opponent.position.block)) {
                if ((Math.abs(myCar.position.lane - opponent.position.lane) <= 1) && ((myCar.speed >= 6) || (opponent.position.block > 1450))) {
                    return EMP;
                }
            }
        }
    /**
     * GREEDY BY POWERUP AND DAMAGE
     * IF BLOCK in our lanes contains blocker (wall,oil_spill,cybertruck,mud,player)
     **/
        if (checkObstacle(blocksMax,gameState)) {// Lizard mechanism and Avoidance mechanism (greedy by powerup and damage)
            int powerFront = countPowerup(terrainMax);
            int powerRight = countPowerup(terrainRight);
            int powerLeft = countPowerup(terrainLeft);
            int damageFront = countDamage(blocksMax);
            int damageRight = countDamage(rightblocks);
            int damageLeft = countDamage(leftblocks);
            if (myCar.position.lane == 1) { //compare damage and powerup right and front
                if (damageFront != damageRight) {
                    if (damageFront < damageRight) {
                        if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                            return jumpORattack(gameState, blocks);
                        }
                        return accelORattack(gameState);
                    }
                    return chooseMoveObstacle(gameState, blocksAcc, blocks, "RIGHT", damageRight);
                }
                if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {  
                    return jumpORattack(gameState, blocks);
                }
                if (powerFront >= powerRight) {
                    return accelORattack(gameState);
                }
                return chooseMoveObstacle(gameState, blocksAcc, blocks, "RIGHT", damageRight);
            }
            if (myCar.position.lane == 4) { // compare damage and powerup left and front
                if (damageFront != damageLeft) {
                    if (damageFront < damageLeft) {
                        if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                            return jumpORattack(gameState, blocks);
                        }
                        return accelORattack(gameState);
                    }
                    return chooseMoveObstacle(gameState, blocksAcc, blocks, "LEFT", damageLeft);
                }
                if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                    return jumpORattack(gameState, blocks);
                }
                if (powerFront >= powerLeft) {
                    return accelORattack(gameState);
                }
                return chooseMoveObstacle(gameState, blocksAcc, blocks, "LEFT", damageLeft);
            }
            if (damageLeft != 0 && damageRight != 0) { //compare damage front, left, and right
                if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                    Command move = jumpORattack(gameState, blocks);
                    if (move == LIZARD) {
                        return LIZARD;
                    }
                }
                Integer[] damageCheck = {damageFront, damageLeft, damageRight};
                Arrays.sort(damageCheck);
                if (damageCheck[0].equals(damageCheck[2])) { //if all damage equal, check by powerup
                    Integer[] powerCheck = {powerFront, powerLeft, powerRight};
                    Arrays.sort(powerCheck, Collections.reverseOrder());
                    return CompareLine(powerCheck, powerFront, powerLeft, powerRight, gameState, blocksAcc, blocks);
                }
                if (damageCheck[0].equals(damageCheck[1])) { // if two lanes has equal damage and less than the other, check powerup two lanes
                    if (damageCheck[2] == damageRight) {
                        if (powerFront >= powerLeft) {
                            return accelORattack(gameState);
                        }
                        return chooseMoveObstacle(gameState, blocksAcc, blocks, "LEFT", damageLeft);
                    }
                    if (damageCheck[2] == damageLeft) {
                        if (powerFront >= powerRight) {
                            return accelORattack(gameState);
                        }
                        return chooseMoveObstacle(gameState, blocksAcc, blocks, "RIGHT", damageRight);
                    }
                    return powerLeftOrRight(powerLeft, powerRight, damageLeft, damageRight, gameState, blocksAcc, blocks);
                }
                return CompareLine(damageCheck, damageFront, damageLeft, damageRight, gameState, blocksAcc, blocks); //compare 3 lanes if highest damage greater than the other two lanes
            }
            if (damageRight == 0) {
                if (damageLeft == 0) {
                    return powerLeftOrRight(powerLeft, powerRight, damageLeft, damageRight, gameState, blocksAcc, blocks); // if both right and left zero damage, check both lanes
                }
                return chooseMoveObstacle(gameState, blocksAcc, blocks, "RIGHT", damageRight);
            }
            return chooseMoveObstacle(gameState, blocksAcc, blocks, "LEFT", damageLeft);
        }
    /**
     * GREEDY BY BOOST or SPEED
     * Boost or find lanes with terrain BOOST or Accelerate (Guarantee block in front of our car doesn't contain blocker)
     **/
        if (PowerUp(PowerUps.BOOST, myCar.powerups) > 0) {// BOOST mechanism
            if (canBoost(gameState)) {
                return BOOST;
            }
            if (myCar.damage > 0) {
                return FIX;
            }
        }
        //FIND LANES WITH BOOST
        if (terrainMax.contains(Terrain.BOOST)) {
            if (checkAcc(gameState)) {
                return ACCELERATE;
            }
            return attack(gameState);
        }
        if (terrainRight.contains(Terrain.BOOST) && countDamage(rightblocks) == 0) {
            if (terrainLeft.contains(Terrain.BOOST) && countDamage(leftblocks) == 0) {
                if ((countPowerup(terrainLeft) > countPowerup(terrainRight)) || ((countPowerup(terrainLeft) == countPowerup(terrainRight)) && (myCar.position.lane - opponent.position.lane >= 0))) {
                    return TURN_LEFT;
                }
            }
            return TURN_RIGHT;
        }
        if (terrainLeft.contains(Terrain.BOOST) && countDamage(leftblocks) == 0) {
            return TURN_LEFT;
        }
        if (checkAcc(gameState)) {
            return ACCELERATE;
        }
    /**
     * FIX CAR
     * fix if damage > 2 (if damage <= 2 then maxspeed = 9 or 8 (okay as long as car can't boost))
     **/
        if (myCar.damage == 4 && myCar.speed == speedState1) { //FIX mechanism
            return FIX;
        }
        if (myCar.damage == 3 && myCar.speed == speedState2) {
            return FIX;
        }
        Command attack = attack(gameState); //use EMP if meet condition
        if (attack == EMP) {
            return attack;
        }
        if (myCar.damage == 2 && myCar.speed == speedState3) {
            return FIX;
        }
    /**
     * GREEDY BY sum powerup
     * FIND LANES with most powerup and doesn't contain blocker
     **/
        if ((countDamage(rightblocks) == 0) || (countDamage(leftblocks) == 0)) {//kalo didepan kosong & gapunya boost & max speed , nyari lane dengan powerup paling banyak
            List<Integer> powerupList = new ArrayList<>();
            powerupList.add(countPowerup(terrainBlocks));
            powerupList.add(countPowerup(terrainRight));
            powerupList.add(countPowerup(terrainLeft));
            powerupList.sort((a, b) -> b - a); //sort powerup descending
            if (powerupList.get(0) == countPowerup(terrainBlocks)) { //prioritize current lanes, guarantee can't use accelerate
                return attack(gameState);
            }
            if (powerupList.get(0) == countPowerup(terrainLeft)) {
                if (myCar.position.lane != 1 && countDamage(leftblocks) == 0) {
                    if (powerupList.get(0) == countPowerup(terrainRight)) { //if left and right has same countpowerup,choose lane such that difference between lane player and opp <= 1
                        if (myCar.position.lane != 4 && countDamage(rightblocks) == 0) {
                            if (myCar.position.lane - opponent.position.lane >= 0) {
                                return TURN_LEFT; //if opponent is on the left of player, turn left
                            }
                            return TURN_RIGHT; // turn right otherwise
                        }
                    }
                    return TURN_LEFT;
                }
                if (powerupList.get(1) == countPowerup(terrainBlocks)) {
                    return attack(gameState);
                }
                if (powerupList.get(1) == countPowerup(terrainRight)) {
                    if (myCar.position.lane != 4 && countDamage(rightblocks) == 0) {
                        return TURN_RIGHT;
                    }
                }
            }
            if (powerupList.get(0) == countPowerup(terrainRight)) {
                if (myCar.position.lane != 4 && countDamage(rightblocks) == 0) {
                    return TURN_RIGHT;
                }
                if (powerupList.get(1) == countPowerup(terrainBlocks)) {
                    return attack(gameState);
                }
                if (powerupList.get(1) == countPowerup(terrainLeft)) {
                    if (myCar.position.lane != 1 && countDamage(leftblocks) == 0) {
                        return TURN_LEFT;
                    }
                }
            }
        }
        return attack(gameState); //default command
    }

    // function to check the best lanes between left and right (if current lane has blocker)
    private Command powerLeftOrRight (int powerLeft, int powerRight, int damageLeft, int damageRight, GameState gameState, List<Lane> blocksAcc, List<Lane> blocks) {
        if (powerLeft > powerRight) {
            return chooseMoveObstacle(gameState, blocksAcc, blocks, "LEFT", damageLeft);
        }
        if (powerLeft == powerRight) {//if left and right has same countpowerup,choose lane such that difference between lane player and opp <= 1
            if (gameState.player.position.lane - gameState.opponent.position.lane >= 0) {
                return chooseMoveObstacle(gameState, blocksAcc, blocks, "LEFT", damageLeft);
            }
        }
        return chooseMoveObstacle(gameState, blocksAcc, blocks, "RIGHT", damageRight);
    }
    //function to check best lanes between three lanes, array sorted
    private Command CompareLine (Integer[] damageCheck, int damageFront, int damageLeft, int damageRight, GameState gameState, List<Lane> blocksAcc, List<Lane> blocks) {
        if (damageCheck[0] == damageFront) {
            return accelORattack(gameState);
        }
        else if (damageCheck[0] == damageLeft) {
            if (damageCheck[0] == damageRight) {
                if (gameState.player.position.lane - gameState.opponent.position.lane < 0) {
                    return chooseMoveObstacle(gameState, blocksAcc, blocks, "RIGHT", damageRight);
                }
            }
            return chooseMoveObstacle(gameState, blocksAcc, blocks, "LEFT", damageLeft);
        }
        return chooseMoveObstacle(gameState, blocksAcc, blocks, "RIGHT", damageRight);
    }
    //function to choose move between accelerate or use powerup
    private Command accelORattack (GameState gameState) {
        if (checkAcc(gameState)) {
            return ACCELERATE;
        }
        return attack(gameState);
    }
    //function to choose move between use lizard or attack(another powerup)
    private Command jumpORattack (GameState gameState, List<Lane> blocks) {
        if (checkObstacle(blocks,gameState)) { //if blocks contains blocker, use lizard if land spot doesn't contain blocker
            Terrain jump = (blocks.size() >= gameState.player.speed) ? blocks.get(gameState.player.speed - 1).terrain : Terrain.EMPTY ;
            if (jump != Terrain.WALL && jump != Terrain.OIL_SPILL && jump != Terrain.MUD) {
                boolean cyberTruck = blocks.size() >= gameState.player.speed && blocks.get(gameState.player.speed - 1).cyberTruck;
                if (!cyberTruck) {
                    return LIZARD;
                }
            }
        }
        return attack(gameState); //use another powerup/do_nothing otherwise
    }
    //function to check current lane before turn left/right
    private Command chooseMoveObstacle (GameState gameState, List<Lane> blocksAcc, List<Lane> blocks, String side, int damage) {
        if (checkAcc(gameState)) {
            if(!checkObstacle(blocksAcc,gameState)) { //if we can use accelerate and no blocks contains blocker, accelerate
                return ACCELERATE;
            }
        }
        if (!checkObstacle(blocks,gameState)) { //if we can't use accelerate and no blocks contains blocker, use powerup/do nothing
            return attack(gameState);
        }
        if (damage == 0) { //if damage in lane right/left (depend on which lane we check), change direction
            if (side.equals("RIGHT")) {
                return TURN_RIGHT;
            }
            return TURN_LEFT;
        }
        if (PowerUp(PowerUps.LIZARD, gameState.player.powerups) > 0) { //if we have lizard and landing spot doesn't contain blocker, use lizard
            Terrain jump = (blocks.size() >= gameState.player.speed) ? blocks.get(gameState.player.speed - 1).terrain : Terrain.EMPTY ;
            if (jump != Terrain.WALL && jump != Terrain.OIL_SPILL && jump != Terrain.MUD) {
                boolean cyberTruck = blocks.size() >= gameState.player.speed && blocks.get(gameState.player.speed - 1).cyberTruck;
                if (!cyberTruck) {
                    return LIZARD;
                }
            }
        }
        //check damage if we accelerate, attack/do_nothing, and turn. choose the best
        int damageAcc = countDamage(blocksAcc);
        int damageNothing = countDamage(blocks);
        Integer[] ArrDamage = {damageAcc, damageNothing, damage};
        Arrays.sort(ArrDamage);
        if (ArrDamage[0] == damageAcc) {
            if (checkAcc(gameState)) {
                return ACCELERATE;
            }
            return attack(gameState);
        }
        if(ArrDamage[0] == damageNothing) {
            return attack(gameState);
        }
        if (side.equals("RIGHT")) {
            return TURN_RIGHT;
        }
        return TURN_LEFT;
    }
    //function to count powerup
    private int countPowerup(List<Terrain> blocks) {
        int count = 0;
        for (Terrain t : blocks) {
            if (t == Terrain.BOOST || t == Terrain.EMP) { //prioritze emp and boost (both of them powerful)
                count += 4;
            }
            else if (t == Terrain.TWEET || t == Terrain.LIZARD) {
                count += 2;
            }
            else if (t == Terrain.OIL_POWER) { //oil_power weakest powerup
                count += 1;
            }
        }
        return count;
    }
    //function to use powerup or do_nothing
    private Command attack(GameState gameState) {
        Car player = gameState.player;
        Car opponent = gameState.opponent;       
        if (PowerUp(PowerUps.EMP, player.powerups) > 0 && (player.position.block < opponent.position.block)) {// EMP mechanism (most powerful)
            if (Math.abs(player.position.lane - opponent.position.lane) <= 1) {
                if ((player.position.lane != opponent.position.lane) || (player.position.block <= opponent.position.block - player.speed + 3)) {
                    return EMP;
                }
                if (PowerUp(PowerUps.EMP, player.powerups) > 13) {
                    return EMP;
                }
            }
        }
        if ((PowerUp(PowerUps.OIL, player.powerups) > 0) && (player.position.block > opponent.position.block) && (player.position.block <= opponent.position.block + opponent.speed)) { // second most powerful in some condition
            return OIL;
        }
        if (PowerUp(PowerUps.TWEET, gameState.player.powerups) > 0) {// Tweet mechanism (second most powerful overall)
            if (Math.abs(player.position.block - opponent.position.block) >= 8) {
                TWEET = new TweetCommand(gameState.opponent.position.lane,gameState.opponent.position.block + higherSpeed(gameState.opponent.speed)  + 3);
                return TWEET;
            }
        }
        if (PowerUp(PowerUps.OIL, gameState.player.powerups) > 0) {// Oil mechanism (weakest powerup overall)
            if (gameState.player.position.block > gameState.opponent.position.block) {
                return OIL;
            }
        }
        return GetPoint(gameState); //if doesn't meet all condition, getpoint using any powerup or do nothing
    }
    private Command GetPoint (GameState gameState) {
        Car player = gameState.player;
        Car opponent = gameState.opponent;
        if (PowerUp(PowerUps.TWEET, player.powerups) > 6) { //if we have too much tweet, use it to gain points
            TWEET = new TweetCommand(opponent.position.lane,opponent.position.block + higherSpeed(opponent.speed)  + 3);
            return TWEET;
        }
        if (PowerUp(PowerUps.OIL, player.powerups) > 5) { // if we have too much oil, use it to gain points
            return OIL;
        }
        if (player.position.block > 1450) { //use any powerup before finish to gain max points
            if (PowerUp(PowerUps.TWEET, player.powerups) > 2) {
                TWEET = new TweetCommand(opponent.position.lane,opponent.position.block + higherSpeed(opponent.speed)  + 3);
                return TWEET;
            }
            if (PowerUp(PowerUps.OIL, player.powerups) > 2) {
                return OIL;
            }
            if (PowerUp(PowerUps.LIZARD, player.powerups) > 2) {
                return LIZARD;
            }
            if (PowerUp(PowerUps.EMP, player.powerups) > 5) {
                return EMP;
            }
        }  
        return DO_NOTHING;// Default return value
    }
    //function to check powerup in list or not
    private int PowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        int res = 0;
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                res += 1;
            }
        }
        return res;
    }
    //function to check whether blocks contains blocker   
    private boolean checkObstacle(List<Lane> blocks, GameState gameState) {
        List<Terrain> terrainBlocks = blocks.stream().map(element -> element.terrain).collect(Collectors.toList());
        List<Boolean> cyberTruckBlocks = blocks.stream().map(element -> element.cyberTruck).collect(Collectors.toList());
        List<Integer> occupiedBlocks = blocks.stream().map(element -> element.occupiedByPlayerId).collect(Collectors.toList());
        Car player = gameState.player;
        Car opponent = gameState.opponent;
        if (terrainBlocks.contains(Terrain.MUD) || terrainBlocks.contains(Terrain.WALL) || terrainBlocks.contains(Terrain.OIL_SPILL) || cyberTruckBlocks.contains(true)) {
            return true;
        }
        if (occupiedBlocks.contains(opponent.id) && (player.speed + player.position.block > higherSpeed(gameState.opponent.speed) + opponent.position.block)) {
            return true;
        }
        return false;
    }
    //function to check whether we can accelerate or not
    private boolean checkAcc(GameState gameState) {
        int damage = gameState.player.damage;
        int speed = gameState.player.speed;
        if (damage < 2) {
            if (speed <= Bot.speedState3) {
                return true;
            }
            return false;
        }
        if (damage == 2) {
            if (speed < Bot.speedState3) {
                return true;
            }
            return false;
        }
        if (damage == 3) {
            if (speed < Bot.speedState2) {
                return true;
            }
            return false;
        }
        if (speed < Bot.speedState1) {
            return true;
        }
        return false;
    }  
    //function return higher state of current speedstate
    private int higherSpeed(int speed) {
        for (int i = 0;i < 5;i++) {
            if (speed == Bot.speedState1) {
                return Bot.speedState2;
            }
            else if (speed == Bot.speedState[i]) {
                return Bot.speedState[i+1];
            }
        }
        return speed;
    }
    //function to count sum of damage from blocker
    private int countDamage(List<Lane> blocks) {
        List<Terrain> terrainBlocks = blocks.stream().map(element -> element.terrain).collect(Collectors.toList());
        List<Boolean> cyberTruckBlocks = blocks.stream().map(element -> element.cyberTruck).collect(Collectors.toList());
        int count = 0;
        if (cyberTruckBlocks.contains(true)) {
            count += 3; //3 damage instead of 2 from cybertruck because they stop the car
        }
        for (Terrain t : terrainBlocks) {
            if (t == Terrain.MUD || t == Terrain.OIL_SPILL) {
                count += 1;
            }
            else if (t == Terrain.WALL) {
                count += 2;
            }
        }
        return Math.min(count, 5);
    }
    //function to check whether we can use boost or not
    private boolean canBoost(GameState gameState) {
        if (PowerUp(PowerUps.BOOST, gameState.player.powerups) > 0) {
            if (gameState.player.damage > 0 || gameState.player.boostCounter > 1) {
                return false;
            }
            return true;
        }
        return false;
    }
    /**
     * FUNCTION TO RETURN ALL BLOCKS IN FRONT OF OUR CAR
     * POS mean what lane we choose, 0 for current lane, -1 for left lane, 1 for right lane
     **/
    private List<Lane> getBlocks(int lane, int block, GameState gameState, int pos) {
        List<Lane[]> map = gameState.lanes;
        List<Lane> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;
        int speed = gameState.player.speed;
        if (pos == 0) {
            if (canBoost(gameState)) {
                speed = Bot.boostSpeed;
            }
            else if (checkAcc(gameState)) {
                speed = higherSpeed(speed);
            }
        }
        else if (pos == -1) {
            if (lane == 1) {
                return blocks;
            }
            block--;
        }
        else {
            if (lane == 4) {
                return blocks;
            }
            block--;
        }
        Lane[] laneList = map.get(lane + pos - 1);
        for (int i = max(block - startBlock, 0) + 1; i <= block - startBlock + speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }
            blocks.add(laneList[i]);
        }
        return blocks;
    }
}