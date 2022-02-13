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
        List<Lane> blocksMax = getBlocksInFrontMax(myCar.position.lane, myCar.position.block, gameState); //blok maksimal yang dapat ditempuh player di lane yg sama
        List<Terrain> terrainMax = blocksMax.stream().map(element -> element.terrain).collect(Collectors.toList());

        List<Lane> rightblocks = getBlocksInRightFront(myCar.position.lane, myCar.position.block, gameState); //blok maksimal yang dapat ditempuh player di lane kanannya
        List<Terrain> terrainRight = rightblocks.stream().map(element -> element.terrain).collect(Collectors.toList());

        List<Lane> leftblocks = getBlocksInLeftFront(myCar.position.lane, myCar.position.block, gameState); //blok maksimal yang dapat ditempuh player di lane kirinya
        List<Terrain> terrainLeft = leftblocks.stream().map(element -> element.terrain).collect(Collectors.toList());

        List<Lane> blocksAcc = getBlocksInFrontAcc(myCar.position.lane, myCar.position.block, gameState); //blok maksimal yang dapat ditempuh player di lane yg sama

        List<Lane> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState);
        List<Terrain> terrainBlocks = blocks.stream().map(element -> element.terrain).collect(Collectors.toList());

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

        // Boost mechanism
        if (PowerUp(PowerUps.BOOST, myCar.powerups) > 0) {
            if (canBoost(gameState)) {
                return BOOST;
            }
            if (myCar.damage > 0) {
                return FIX;
            }
        }

        //CHECK BOOST or ACCELERATE mechanism
        if (terrainMax.contains(Terrain.BOOST)) {
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

        //attack MECHANISM
        Command Attack = attack(gameState);
        if (Attack != DO_NOTHING) {
            return Attack;
        }
        //kalo didepan kosong & gapunya boost & max speed , nyari lane dengan powerup paling banyak
        if ((countDamage(rightblocks) == 0) || (countDamage(leftblocks) == 0)) {
            int powerupFront = countPowerup(terrainBlocks);
            int powerupRight = countPowerup(terrainRight);
            int powerupLeft = countPowerup(terrainLeft);
            if (powerupFront < powerupLeft && powerupFront < powerupRight) {
                if (powerupLeft >= powerupRight) {
                    if (countDamage(leftblocks) == 0) {
                        return TURN_LEFT;
                    }
                    return TURN_RIGHT;
                }
                if(countDamage(rightblocks) == 0) {
                    return TURN_RIGHT;
                }
                return TURN_LEFT;
            }
            else if (powerupFront < powerupRight) {
                if (countDamage(rightblocks) == 0) {
                    return TURN_RIGHT;
                }
            }
            else if (powerupFront < powerupLeft) {
                if (countDamage(leftblocks) == 0) {
                    return TURN_LEFT;
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
        if (checkObstacle(blocks)) {
            return LIZARD;
        }
        return attack(gameState);
    }
    private Command chooseMoveObstacle (GameState gameState, List<Lane> blocksAcc, List<Lane> blocks, String side) {
        if (checkAcc(gameState)) {
            if(!checkObstacle(blocksAcc)) {
                return ACCELERATE;
            }
        }
        if (!checkObstacle(blocks)) {
            return DO_NOTHING;
        }
        if (side == "RIGHT") {
            return TURN_RIGHT;
        }
        return TURN_LEFT;
    }
    //fungsi untuk menghitung banyak power up pada block (selain boost)
    private int countPowerup(List<Terrain> blocks) {
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
    //fungsi untuk melakukan attack (jika memungkinkan)
    private Command attack(GameState gameState) {
        Car player = gameState.player;
        Car opponent = gameState.opponent;
        // EMP mechanism
        if (PowerUp(PowerUps.EMP, player.powerups) > 0 && (player.position.block < opponent.position.block) && (opponent.speed > 3)) {
            if (Math.abs(player.position.lane - opponent.position.lane) <= 1) {
                return EMP;
            }
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
    private boolean checkObstacle(List<Lane> blocks) {
        List<Terrain> terrainBlocks = blocks.stream().map(element -> element.terrain).collect(Collectors.toList());
        List<Boolean> cyberTruckBlocks = blocks.stream().map(element -> element.cyberTruck).collect(Collectors.toList());
        if (terrainBlocks.contains(Terrain.MUD) || terrainBlocks.contains(Terrain.WALL) || terrainBlocks.contains(Terrain.OIL_SPILL) || cyberTruckBlocks.contains(true)) {
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
    
    // fungsi menghitung perkiraan speed setelah bergerak
    private int blockReductionSpeed(List<Lane> blocks, GameState gameState) {
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
    private int countDamage(List<Lane> blocks) {
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
    private List<Lane> getBlocksInFrontMax(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Lane> blocks = new ArrayList<>();
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
            return getBlocksInFrontAcc(lane, block, gameState);
        }
        for (int i = max(block - startBlock, 0); i <= block - startBlock + speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i]);

        }
        return blocks;
    }
    private List<Lane> getBlocksInFrontAcc(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Lane> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        int speed;
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
        for (int i = max(block - startBlock, 0); i <= block - startBlock + speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i]);

        }
        return blocks;
    }
    
    private List<Lane> getBlocksInFront(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Lane> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        int speed = gameState.player.speed;
        for (int i = max(block - startBlock, 0); i <= block - startBlock + speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }
            blocks.add(laneList[i]);

        }
        return blocks;
    }
    private List<Lane> getBlocksInRightFront(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Lane> blocks = new ArrayList<>();

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

            blocks.add(laneList[i]);

        }
        return blocks;
    }
    private List<Lane> getBlocksInLeftFront(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Lane> blocks = new ArrayList<>();

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

            blocks.add(laneList[i]);

        }
        return blocks;
    }
}