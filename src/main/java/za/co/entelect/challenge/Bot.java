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
        List<Map<String, Object>> blocksMax = getBlocksInFrontMax(myCar.position.lane, myCar.position.block, gameState); //blok maksimal yang dapat ditempuh player di lane yg sama
        List<Object> terrainBlocksMax = blocksMax.stream().map(element -> element.get("terrain")).collect(Collectors.toList());
        List<Object> cyberTruckBlocksMax = blocksMax.stream().map(element -> element.get("cyberTruck")).collect(Collectors.toList());

        List<Map<String, Object>> rightblocks = getBlocksInRightFront(myCar.position.lane, myCar.position.block, gameState); //blok maksimal yang dapat ditempuh player di lane kanannya
        List<Object> terrainRightBlocks = blocksMax.stream().map(element -> element.get("terrain")).collect(Collectors.toList());
        List<Object> cyberTruckRightBlocks = blocksMax.stream().map(element -> element.get("cyberTruck")).collect(Collectors.toList());

        List<Map<String, Object>> leftblocks = getBlocksInLeftFront(myCar.position.lane, myCar.position.block, gameState); //blok maksimal yang dapat ditempuh player di lane kirinya
        List<Object> terrainLeftBlocks = blocksMax.stream().map(element -> element.get("terrain")).collect(Collectors.toList());
        List<Object> cyberTruckLeftBlocks = blocksMax.stream().map(element -> element.get("cyberTruck")).collect(Collectors.toList());

        List<Map<String, Object>> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState);
        List<Object> terrainBlocks = blocksMax.stream().map(element -> element.get("terrain")).collect(Collectors.toList());
        List<Object> cyberTruckBlocks = blocksMax.stream().map(element -> element.get("cyberTruck")).collect(Collectors.toList());

        // Fix if car completely broken
        if (myCar.damage >= 5) {
            return FIX;
        }

        // ACCELERATE IF SPEED 0
        if (myCar.speed == minSpeed) {
            return ACCELERATE;
        }

        // Lizard mechanism and Avoidance mechanism
        if (checkObstacle(blocksMax)) {
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
                            if(checkObstacle(blocks)) {
                                return LIZARD;
                            }
                            return attack(gameState);
                        }
                        if (checkAcc(gameState)) {
                            return ACCELERATE;
                        }
                        return attack(gameState);
                    }
                    return TURN_RIGHT;
                }
                if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {  
                    if (checkObstacle(blocks)) {
                        return LIZARD;
                    }
                    return attack(gameState);
                }
                if (SpeedFront >= SpeedRight) {
                    if (checkAcc(gameState)) {
                        return ACCELERATE;
                    }
                    return attack(gameState);
                }
                return TURN_RIGHT;
            }
            if (myCar.position.lane == 4) {
                if (damageFront != damageLeft) {
                    if (damageFront < damageLeft) {
                        if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                            if (checkObstacle(blocks)) {
                                return LIZARD;
                            }
                            return attack(gameState);
                        }
                        if (checkAcc(gameState)) {
                            return ACCELERATE;
                        }
                        return attack(gameState);
                    }
                    return TURN_LEFT;
                }
                if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                    if (checkObstacle(blocks)) {
                        return LIZARD;
                    }
                    return attack(gameState);
                }
                if (SpeedFront >= SpeedLeft) {
                    if (checkAcc(gameState)) {
                        return ACCELERATE;
                    }
                    return attack(gameState);
                }
                return TURN_LEFT;
            }
            if (damageLeft != 0 && damageRight != 0) {
                if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                    if (checkObstacle(blocks)) {
                        return LIZARD;
                    }
                    return attack(gameState);
                }
                if (damageFront <= damageLeft && damageFront <= damageRight) {
                    if (checkAcc(gameState)) {
                        return ACCELERATE;
                    }
                    return attack(gameState);
                }
                if (damageLeft < damageRight) {
                    return TURN_LEFT;
                }
                return TURN_RIGHT;
            }
            if (damageRight == 0) {
                if (damageLeft == 0) {
                    if (terrainLeftBlocks.contains(Terrain.BOOST)) {
                        return TURN_LEFT;
                    }
                }
                return TURN_RIGHT;
            }
            return TURN_LEFT;
        }

        // Boost mechanism
        if (PowerUp(PowerUps.BOOST, myCar.powerups) > 0) {
            if (canBoost(gameState)) {
                return BOOST;
            }
            return FIX;
        }

        //CHECK BOOST or ACCELERATE mechanism
        if (terrainBlocksMax.contains(Terrain.BOOST) && countDamage(blocksMax) == 0) {
            if (checkAcc(gameState)) {
                return ACCELERATE;
            }
            return DO_NOTHING;
        }
        if (terrainRightBlocks.contains(Terrain.BOOST) && countDamage(rightblocks) == 0) {
            return TURN_RIGHT;
        }
        if (terrainLeftBlocks.contains(Terrain.BOOST) && countDamage(leftblocks) == 0) {
            return TURN_LEFT;
        }
        if (checkAcc(gameState)) {
            return ACCELERATE;
        }

        //FIX mechanism
        if (myCar.damage == 4 && myCar.speed == speedState1) {
            return FIX;
        }
        if (myCar.damage == 3 && myCar.speed == speedState2) {
            return FIX;
        }
        if (myCar.damage == 2 && myCar.speed == speedState3) {
            return FIX;
        }
        if (myCar.damage == 1 && myCar.speed == maxSpeed) {
            return FIX;
        }

        //attack MECHANISM
        Command Attack = attack(gameState);
        if (Attack != DO_NOTHING) {
            return Attack;
        }

        //kalo didepan kosong & gapunya boost & max speed , nyari lane dengan powerup paling banyak
        if (terrainBlocks.contains(Terrain.OIL_POWER) || terrainBlocks.contains(Terrain.LIZARD) || terrainBlocks.contains(Terrain.TWEET) || terrainBlocks.contains(Terrain.EMP))
        {
            return DO_NOTHING;
        }
        if ((terrainRightBlocks.contains(Terrain.OIL_POWER) || terrainRightBlocks.contains(Terrain.LIZARD) || terrainRightBlocks.contains(Terrain.TWEET) || terrainRightBlocks.contains(Terrain.EMP)) && countDamage(rightblocks) == 0)
        {
            return TURN_RIGHT;
        }
        if ((terrainLeftBlocks.contains(Terrain.OIL_POWER) || terrainLeftBlocks.contains(Terrain.LIZARD) || terrainLeftBlocks.contains(Terrain.TWEET) || terrainLeftBlocks.contains(Terrain.EMP)) && countDamage(leftblocks) == 0)
        {
            return TURN_LEFT;
        }
        return DO_NOTHING;
    }

    //fungsi untuk melakukan attack (jika memungkinkan)
    private Command attack(GameState gameState) {
        // EMP mechanism
        if (PowerUp(PowerUps.EMP, gameState.player.powerups) > 0 && (gameState.player.position.block < gameState.opponent.position.block) && (gameState.opponent.speed > 3)) {
            return EMP;
        }
        // Tweet mechanism
        if (PowerUp(PowerUps.TWEET, gameState.player.powerups) > 0) {
            TWEET = new TweetCommand(gameState.opponent.position.lane,gameState.opponent.position.block + gameState.opponent.speed + 3);
            return TWEET;
        }

        // Oil mechanism
        if (PowerUp(PowerUps.OIL, gameState.player.powerups) > 0 && (gameState.player.position.block > gameState.opponent.position.block)) {
            return OIL;
        }
        // Default return value
        return DO_NOTHING;
    }

    //fungsi untuk menghitung jumlah powerup tertentu yang dimiliki
    private int PowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        int res = 0;
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                res += 1;
            }
        }
        return res;
    }

    // fungsi check obstacle
    private boolean checkObstacle(List<Map<String, Object>> blocksMax) {
        List<Object> terrainBlocksMax = blocksMax.stream().map(element -> element.get("terrain")).collect(Collectors.toList());
        List<Object> cyberTruckBlocksMax = blocksMax.stream().map(element -> element.get("cyberTruck")).collect(Collectors.toList());

        if (terrainBlocksMax.contains(Terrain.MUD) || terrainBlocksMax.contains(Terrain.WALL) || terrainBlocksMax.contains(Terrain.OIL_SPILL) || cyberTruckBlocksMax.contains(true)) {
            return true;
        }
        return false;
    }

    //check bisa accelerate atau tidak
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
        if (damage <= 3) {
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
    
    // fungsi menghitung perkiraan speed setelah bergerak
    private int blockReductionSpeed(List<Map<String, Object>> blocks, GameState gameState) {
        List<Object> terrainBlocksMax = blocks.stream().map(element -> element.get("terrain")).collect(Collectors.toList());
        List<Object> cyberTruckBlocksMax = blocks.stream().map(element -> element.get("cyberTruck")).collect(Collectors.toList());

        int speedNow = gameState.player.speed;
        if (terrainBlocksMax.contains(Terrain.WALL) || cyberTruckBlocksMax.contains(true)) {
            speedNow = 3;
        }
        else {
            for (Object t : terrainBlocksMax) {
                if (t == Terrain.MUD || t == Terrain.OIL_SPILL) {
                    speedNow = lowerSpeed(speedNow);
                }
            }
        }
        return speedNow;
    }

    //fungsi cek pengurangan state speed
    private int lowerSpeed(int speed) {
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

    // fungsi menghitung perkiraan damage car
    private int countDamage(List<Map<String, Object>> blocks) {
        List<Object> terrainBlocksMax = blocks.stream().map(element -> element.get("terrain")).collect(Collectors.toList());
        List<Object> cyberTruckBlocksMax = blocks.stream().map(element -> element.get("cyberTruck")).collect(Collectors.toList());

        int count = 0;
        for (Object t : terrainBlocksMax) {
            if (t == Terrain.MUD || t == Terrain.OIL_SPILL) {
                count += 1;
            }
            else if (t == Terrain.WALL) {
                count += 2;
            }
        }
        for (Object c : cyberTruckBlocksMax) {
            if (c.equals(true)) {
                count += 2;
            }
        }

        if (count >= 5) {
            return 5;
        }
        return count;
    }

    //fungsi melakukan cek apakah bisa melakukan boost hingga maxspeed
    private boolean canBoost(GameState gameState) {
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
    private List<Map<String, Object>> getBlocksInFrontMax(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Map<String, Object>> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        int speed;
        if (canBoost(gameState)) {
            speed = Bot.boostSpeed;
        }
        else if (!checkAcc(gameState)) {
            return getBlocksInFront(lane,block,gameState);
        }
        else {
            speed = gameState.player.speed;
            for (int i = 0;i < 5;i++) {
                if (speed == Bot.speedState1) {
                    speed = Bot.speedState2;
                } // asumsikan jika maju lurus dia accelerate, cek speedState diatas speed player sekarang
                else if (speed == Bot.speedState[i]) {
                    speed = Bot.speedState[i+1];
                    break;
                }
            }
        }
        for (int i = max(block - startBlock, 0); i <= block - startBlock + speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            Map<String, Object> obj = new HashMap<>();
            obj.put("position", laneList[i].position);
            obj.put("terrain", laneList[i].terrain);
            obj.put("occupiedByPlayerId", laneList[i].occupiedByPlayerId);
            obj.put("cyberTruck", laneList[i].cyberTruck);

            blocks.add(obj);

        }
        return blocks;
    }
    private List<Map<String, Object>> getBlocksInFront(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Map<String, Object>> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        int speed = gameState.player.speed;
        for (int i = max(block - startBlock, 0); i <= block - startBlock + speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }
            Map<String, Object> obj = new HashMap<>();
            obj.put("position", laneList[i].position);
            obj.put("terrain", laneList[i].terrain);
            obj.put("occupiedByPlayerId", laneList[i].occupiedByPlayerId);
            obj.put("cyberTruck", laneList[i].cyberTruck);

            blocks.add(obj);

        }
        return blocks;
    }
    private List<Map<String, Object>> getBlocksInRightFront(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Map<String, Object>> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;
        if (lane == 4) {
            return blocks;
        }
        Lane[] laneList = map.get(lane);
        int speed = gameState.player.speed;
        for (int i = max(block - startBlock - 1, 0); i <= block - startBlock + speed - 1; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }
            Map<String, Object> obj = new HashMap<>();
            obj.put("position", laneList[i].position);
            obj.put("terrain", laneList[i].terrain);
            obj.put("occupiedByPlayerId", laneList[i].occupiedByPlayerId);
            obj.put("cyberTruck", laneList[i].cyberTruck);

            blocks.add(obj);

        }
        return blocks;
    }
    private List<Map<String, Object>> getBlocksInLeftFront(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Map<String, Object>> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;
        if (lane == 1) {
            return blocks;
        }
        Lane[] laneList = map.get(lane - 2);
        int speed = gameState.player.speed;
        for (int i = max(block - startBlock - 1, 0); i <= block - startBlock + speed - 1; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }
            Map<String, Object> obj = new HashMap<>();
            obj.put("position", laneList[i].position);
            obj.put("terrain", laneList[i].terrain);
            obj.put("occupiedByPlayerId", laneList[i].occupiedByPlayerId);
            obj.put("cyberTruck", laneList[i].cyberTruck);

            blocks.add(obj);

        }
        return blocks;
    }
}