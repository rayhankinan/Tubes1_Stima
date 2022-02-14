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
    private final static Command DECELERATE = new DecelerateCommand();
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
        List<Lane> blocksMax = getBlocks(myCar.position.lane, myCar.position.block, gameState, 0); //blok maksimal yang dapat ditempuh player di lane yg sama
        List<Terrain> terrainMax = blocksMax.stream().map(element -> element.terrain).collect(Collectors.toList());
        List<Lane> rightblocks = getBlocks(myCar.position.lane, myCar.position.block, gameState, 1); //blok maksimal yang dapat ditempuh player di lane kanannya
        List<Terrain> terrainRight = rightblocks.stream().map(element -> element.terrain).collect(Collectors.toList());
        List<Lane> leftblocks = getBlocks(myCar.position.lane, myCar.position.block, gameState, -1); //blok maksimal yang dapat ditempuh player di lane kirinya
        List<Terrain> terrainLeft = leftblocks.stream().map(element -> element.terrain).collect(Collectors.toList());
        List<Lane> blocks = (blocksMax.size() >= myCar.speed) ? blocksMax.subList(0, myCar.speed) : blocksMax;
        List<Terrain> terrainBlocks = blocks.stream().map(element -> element.terrain).collect(Collectors.toList());
        List<Lane> blocksAcc = (checkAcc(gameState)) ? ((blocksMax.size() >= higherSpeed(myCar.speed)) ? blocksMax.subList(0, higherSpeed(myCar.speed)) : blocksMax) : blocks;
        if (myCar.damage >= 5) {// Fix if car completely broken
            return FIX;
        }

        if (myCar.speed == minSpeed) {// ACCELERATE IF SPEED 0
            return ACCELERATE;
        }

        if (checkObstacle(blocksMax,gameState)) {// Lizard mechanism and Avoidance mechanism
            int SpeedFront = blockReductionSpeed(blocksMax, gameState);
            int SpeedRight = blockReductionSpeed(rightblocks, gameState);
            int SpeedLeft = blockReductionSpeed(leftblocks, gameState);
            int damageFront = countDamage(blocksMax);
            int damageRight = countDamage(rightblocks);
            int damageLeft = countDamage(leftblocks);
            if (myCar.position.lane == 1) {
                if (damageFront != damageRight) {
                    if (damageFront < damageRight) {
                        if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                            return jumpORattack(gameState, blocks);
                        }
                        return accelORattack(gameState);
                    }
                    return chooseMoveObstacle(gameState, blocksAcc, blocks, "RIGHT");
                }
                if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {  
                    return jumpORattack(gameState, blocks);
                }
                if (SpeedFront >= SpeedRight) {
                    return accelORattack(gameState);
                }
                return chooseMoveObstacle(gameState, blocksAcc, blocks, "RIGHT");
            }
            if (myCar.position.lane == 4) {
                if (damageFront != damageLeft) {
                    if (damageFront < damageLeft) {
                        if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                            return jumpORattack(gameState, blocks);
                        }
                        return accelORattack(gameState);
                    }
                    return chooseMoveObstacle(gameState, blocksAcc, blocks, "LEFT");
                }
                if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                    return jumpORattack(gameState, blocks);
                }
                if (SpeedFront >= SpeedLeft) {
                    return accelORattack(gameState);
                }
                return chooseMoveObstacle(gameState, blocksAcc, blocks, "LEFT");
            }
            if (damageLeft != 0 && damageRight != 0) {
                if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                    return jumpORattack(gameState, blocks);
                }
            int[] damageCheck = {damageFront, damageLeft, damageRight};
            Arrays.sort(damageCheck);
            if (damageCheck[0] == damageFront) {
                return accelORattack(gameState);
            }
            else if (damageCheck[0] == damageLeft) {
                return chooseMoveObstacle(gameState, blocksAcc, blocks, "LEFT");
            }
            return chooseMoveObstacle(gameState, blocksAcc, blocks, "RIGHT");
            }
            if (damageRight == 0) {
                if (damageLeft == 0) {
                    if (terrainLeft.contains(Terrain.BOOST)) {
                        return TURN_LEFT;
                    }
                }
                return chooseMoveObstacle(gameState, blocksAcc, blocks, "RIGHT");
            }
            return chooseMoveObstacle(gameState, blocksAcc, blocks, "LEFT");
        }
    
        if (PowerUp(PowerUps.BOOST, myCar.powerups) > 0) {// Boost mechanism
            if (canBoost(gameState)) {
                return BOOST;
            }
            if (myCar.damage > 0) {
                return FIX;
            }
        }

        if (terrainMax.contains(Terrain.BOOST)) {//CHECK BOOST or ACCELERATE mechanism
            if (checkAcc(gameState)) {
                return ACCELERATE;
            }
            return DO_NOTHING;
        }
        if (terrainRight.contains(Terrain.BOOST) && countDamage(rightblocks) == 0) {
            return TURN_RIGHT;
        }
        if (terrainLeft.contains(Terrain.BOOST) && countDamage(leftblocks) == 0) {
            return TURN_LEFT;
        }
        if (checkAcc(gameState)) {
            return ACCELERATE;
        }

        if (myCar.damage == 4 && myCar.speed == speedState1) {//FIX mechanism
            return FIX;
        }
        if (myCar.damage == 3 && myCar.speed == speedState2) {
            return FIX;
        }
        if (myCar.damage == 2 && myCar.speed == speedState3) {
            return FIX;
        }

        Command Attack = attack(gameState);//attack MECHANISM
        if (Attack != DO_NOTHING) {
            return Attack;
        }
        
        if ((countDamage(rightblocks) == 0) || (countDamage(leftblocks) == 0)) {//kalo didepan kosong & gapunya boost & max speed , nyari lane dengan powerup paling banyak
            List<Integer> powerupList = new ArrayList<>();
            powerupList.add(countPowerup(terrainBlocks));
            powerupList.add(countPowerup(terrainRight));
            powerupList.add(countPowerup(terrainLeft));

            powerupList.sort((a, b) -> b - a);

            if (powerupList.get(0) == countPowerup(terrainLeft)) {
                if (countDamage(leftblocks) == 0) {
                    return TURN_LEFT;
                }
                if (powerupList.get(1) == countPowerup(terrainRight)) {
                    if (countDamage(rightblocks) == 0) {
                        return TURN_RIGHT;
                    }
                }
            }

            if (powerupList.get(0) == countPowerup(terrainRight)) {
                if (countDamage(rightblocks) == 0) {
                    return TURN_RIGHT;
                }
                if (powerupList.get(1) == countPowerup(terrainLeft)) {
                    if (countDamage(leftblocks) == 0) {
                        return TURN_LEFT;
                    }
                }
            }
        }

        return DO_NOTHING;
    }
    private Command accelORattack (GameState gameState) {
        if (checkAcc(gameState)) {
            return ACCELERATE;
        }
        return attack(gameState);
    }
    private Command jumpORattack (GameState gameState, List<Lane> blocks) {
        if (checkObstacle(blocks,gameState)) {
            return LIZARD;
        }
        return attack(gameState);
    }
    private Command chooseMoveObstacle (GameState gameState, List<Lane> blocksAcc, List<Lane> blocks, String side) {
        if (checkAcc(gameState)) {
            if(!checkObstacle(blocksAcc,gameState)) {
                return ACCELERATE;
            }
        }
        if (!checkObstacle(blocks,gameState)) {
            return DO_NOTHING;
        }
        if (side.equals("RIGHT")) {
            return TURN_RIGHT;
        }
        return TURN_LEFT;
    }
    
    private int countPowerup(List<Terrain> blocks) {//fungsi untuk menghitung banyak power up pada block
        int count = 0;
        for (Terrain t : blocks) {
            if (t == Terrain.EMP || t == Terrain.TWEET || t == Terrain.BOOST) {
                count += 2;
            }
            else if (t == Terrain.LIZARD || t == Terrain.OIL_POWER) {
                count += 1;
            }
        }
        return count;
    }
    private Command attack(GameState gameState) {//fungsi untuk melakukan attack (jika memungkinkan)
        Car player = gameState.player;
        Car opponent = gameState.opponent;       
        if (PowerUp(PowerUps.EMP, player.powerups) > 0 && (player.position.block < opponent.position.block) && (opponent.speed > 3)) {// EMP mechanism
            if (Math.abs(player.position.lane - opponent.position.lane) <= 1) {
                return EMP;
            }
        }
        if (PowerUp(PowerUps.TWEET, gameState.player.powerups) > 0) {// Tweet mechanism
            TWEET = new TweetCommand(gameState.opponent.position.lane,gameState.opponent.position.block + gameState.opponent.speed + 3);
            return TWEET;
        }
        if (PowerUp(PowerUps.OIL, gameState.player.powerups) > 0 && (gameState.player.position.block > gameState.opponent.position.block)) {// Oil mechanism
            return OIL;
        }  
        return DO_NOTHING;// Default return value
    }
    private int PowerUp(PowerUps powerUpToCheck, PowerUps[] available) {//fungsi untuk menghitung jumlah powerup tertentu yang dimiliki
        int res = 0;
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                res += 1;
            }
        }
        return res;
    }   
    private boolean checkObstacle(List<Lane> blocks, GameState gameState) {// fungsi check obstacle
        List<Terrain> terrainBlocks = blocks.stream().map(element -> element.terrain).collect(Collectors.toList());
        List<Boolean> cyberTruckBlocks = blocks.stream().map(element -> element.cyberTruck).collect(Collectors.toList());
        List<Integer> occupiedBlocks = blocks.stream().map(element -> element.occupiedByPlayerId).collect(Collectors.toList());
        if (terrainBlocks.contains(Terrain.MUD) || terrainBlocks.contains(Terrain.WALL) || terrainBlocks.contains(Terrain.OIL_SPILL) || cyberTruckBlocks.contains(true)) {
            return true;
        }
        if (Collections.frequency(occupiedBlocks, 0) != occupiedBlocks.size() && (gameState.player.speed > gameState.opponent.speed || gameState.opponent.speed <= 3)) {
            return true;
        }
        return false;
    }
    private boolean checkAcc(GameState gameState) {//check bisa accelerate atau tidak
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
    private int blockReductionSpeed(List<Lane> blocks, GameState gameState) {// fungsi menghitung perkiraan speed setelah bergerak
        List<Terrain> terrainBlocks = blocks.stream().map(element -> element.terrain).collect(Collectors.toList());
        List<Boolean> cyberTruckBlocks = blocks.stream().map(element -> element.cyberTruck).collect(Collectors.toList());

        int speedNow = gameState.player.speed;
        
        if (cyberTruckBlocks.contains(true)) {
            speedNow = 0;
        }
        else if (terrainBlocks.contains(Terrain.WALL)) {
            speedNow = 3;
        }
        else {
            for (Terrain t : terrainBlocks) {
                if (t == Terrain.MUD || t == Terrain.OIL_SPILL) {
                    speedNow = lowerSpeed(speedNow);
                }
            }
        }
        return speedNow;
    }
    private int lowerSpeed(int speed) {//fungsi cek pengurangan state speed
        if (speed == Bot.speedState2) {
            return Bot.speedState1;
        }
        for (int i = 4; i < 7;i++) {
            if (speed == Bot.speedState[i]) {
                return Bot.speedState[i-1];
            }
        }
        return Bot.speedState1;
    }
    private int higherSpeed(int speed) {
        for (int i = 0;i < 5;i++) {
            if (speed == Bot.speedState1) {
                return Bot.speedState2;
            }
            else if (speed == Bot.speedState[i]) {
                return Bot.speedState[i+1];
            }
        }
        return Bot.maxSpeed;
    }
    private int countDamage(List<Lane> blocks) {// fungsi menghitung perkiraan damage car
        List<Terrain> terrainBlocks = blocks.stream().map(element -> element.terrain).collect(Collectors.toList());
        List<Boolean> cyberTruckBlocks = blocks.stream().map(element -> element.cyberTruck).collect(Collectors.toList());
        int count = 0;
        if (cyberTruckBlocks.contains(true)) {
            return 10;
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
    private boolean canBoost(GameState gameState) {//fungsi melakukan cek apakah bisa melakukan boost hingga maxspeed
        if (PowerUp(PowerUps.BOOST, gameState.player.powerups) > 0) {
            if (gameState.player.damage > 0 || gameState.player.boosting) {
                return false;
            }
            return true;
        }
        return false;
    }
    /**
     * Returns map of blocks and the objects in the for the current lanes, returns the amount of blocks that can be
     * traversed at max speed.
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