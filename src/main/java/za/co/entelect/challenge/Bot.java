package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.Terrain;

import java.util.*;

import static java.lang.Math.max;

import java.security.SecureRandom;

public class Bot {

    private static final int minSpeed = 0;
    private static final int speedState1 = 3;
    private static final int initialSpeed = 5;
    private static final int speedState2 = 6;
    private static final int speedState3 = 8;
    private static final int maxSpeed = 9;
    private static final int boostSpeed = 15;
    private static final int[] speedState = {minSpeed, speedState1, initialSpeed, speedState2, speedState3, maxSpeed, boostSpeed};

    private List<Command> directionList = new ArrayList<>();

    private final Random random;

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
        this.random = new SecureRandom();
        directionList.add(TURN_LEFT);
        directionList.add(TURN_RIGHT);
    }

    public Command run(GameState gameState) {
        Car myCar = gameState.player;
        Car opponent = gameState.opponent;

        List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState);
        List<Object> rightblocks = getBlocksInRightFront(myCar.position.lane, myCar.position.block, gameState);
        List<Object> leftblocks = getBlocksInLeftFront(myCar.position.lane, myCar.position.block, gameState);

        // Fix if car completely broken
        if (myCar.damage >= 5) {
            return FIX;
        }

        // ACCELERATE IF SPEED 0
        if (myCar.speed == minSpeed) {
            return ACCELERATE;
        }

        // Lizard mechanism and Avoidance mechanism (Left or Right)
        if (blocks.contains(Terrain.MUD) || blocks.contains(Terrain.WALL) || blocks.contains(Terrain.OIL_SPILL)) {
            int SpeedFront = blockReductionSpeed(blocks, gameState);
            int SpeedRight = blockReductionSpeed(rightblocks, gameState);
            int SpeedLeft = blockReductionSpeed(leftblocks, gameState);
            int damageFront = countDamage(blocks);
            int damageRight = countDamage(rightblocks);
            int damageLeft = countDamage(leftblocks);
            if (myCar.position.lane == 1) {
                if (damageFront != damageRight) {
                    if (damageFront < damageRight) {
                        if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                             return LIZARD;
                        }
                        if (checkAcc(gameState)) {
                            return ACCELERATE;
                        }
                        return DO_NOTHING;
                    }
                    return TURN_RIGHT;
                }
                if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                    return LIZARD;
                }
                if (SpeedFront >= SpeedRight) {
                    if (checkAcc(gameState)) {
                        return ACCELERATE;
                    }
                    return DO_NOTHING;
                }
                return TURN_RIGHT;
            }
            if (myCar.position.lane == 4) {
                if (damageFront != damageLeft) {
                    if (damageFront < damageLeft) {
                        if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                             return LIZARD;
                        }
                        if (checkAcc(gameState)) {
                            return ACCELERATE;
                        }
                        return DO_NOTHING;
                    }
                    return TURN_LEFT;
                }
                if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                    return LIZARD;
                }
                if (SpeedFront >= SpeedLeft) {
                    if (checkAcc(gameState)) {
                        return ACCELERATE;
                    }
                    return DO_NOTHING;
                }
                return TURN_LEFT;
            }
            if (damageLeft != 0 && damageRight != 0) {
                if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                    return LIZARD;
                }
                if (damageFront <= damageLeft && damageFront <= damageRight) {
                    if (checkAcc(gameState)) {
                        return ACCELERATE;
                    }
                    return DO_NOTHING;
                }
                if (damageLeft < damageRight) {
                    return TURN_LEFT;
                }
                return TURN_RIGHT;
            }
            if (damageRight == 0) {
                if (damageLeft == 0) {
                    if(leftblocks.contains(Terrain.BOOST)) {
                        return TURN_LEFT;
                    }
                }
                return TURN_RIGHT;
            }
            return TURN_LEFT;
        }

        // Boost mechanism
        if (PowerUp(PowerUps.BOOST, myCar.powerups) > 0) {
            if (myCar.damage > 0) {
                return FIX;
            }
            return BOOST;
        }
        // dari sini kebawah sampe do_nothing kayany masih bisa di fix
        //ACC mechanism
        if (checkAcc(gameState)) {
            return ACCELERATE;
        }

        if (rightblocks.contains(Terrain.BOOST) && countDamage(rightblocks) == 0) {
            return TURN_RIGHT;
        }
        if (leftblocks.contains(Terrain.BOOST) && countDamage(leftblocks) == 0) {
            return TURN_LEFT;
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

        // Tweet mechanism
        if (PowerUp(PowerUps.TWEET, myCar.powerups) > 0) {
            TWEET = new TweetCommand(opponent.position.lane,opponent.position.block + opponent.speed + 3);
            return TWEET;
        }

        // EMP mechanism
        if (PowerUp(PowerUps.EMP, myCar.powerups) > 0 && (myCar.position.block < opponent.position.block)) {
            return EMP;
        }

        // Oil mechanism
        if (PowerUp(PowerUps.OIL, myCar.powerups) > 0 && (myCar.position.block > opponent.position.block)) {
            return OIL;
        }

        // TURN mechanism (pastiin klo turn right atau turn left dia gakena obstacle, klo kena mending do nothing, terus turn right turn leftnya sesuai ada atau engganya powerup)
        // Default return value
        return DO_NOTHING; // Change do DO_NOTHING if avoidance mechanism is finished
    }

    private int PowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        int res = 0;
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                res += 1;
            }
        }
        return res;
    }

    //check ACCELERATE or DO_NOTHING
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
    
    // Check how the blocks reduce the speed
    private int blockReductionSpeed(List<Object> blocks, GameState gameState) {
        int speedNow = gameState.player.speed;
        if (blocks.contains(Terrain.WALL)) {
            speedNow = 3;
        }
        else {
            for (Object i : blocks) {
                if (i == Terrain.MUD || i == Terrain.OIL_SPILL) {
                    speedNow = lowerSpeed(speedNow);
                }
            }
        }
        return speedNow;
    }
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
    // count damage car
    private int countDamage(List<Object> blocks) {
        int count = 0;
        for (Object i : blocks) {
            if (i == Terrain.MUD || i == Terrain.OIL_SPILL) {
                count += 1;
            }
            else if (i == Terrain.WALL) {
                count += 2;
            }
        }
        if (count >= 5) {
            return 5;
        }
        return count;
    }

    /**
     * Returns map of blocks and the objects in the for the current lanes, returns the amount of blocks that can be
     * traversed at max speed.
     **/
    private List<Object> getBlocksInFront(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        int speed;
        if(PowerUp(PowerUps.BOOST, gameState.player.powerups) > 0) {
            speed = Bot.boostSpeed;
        }
        else {
            speed = gameState.player.speed;
            if (checkAcc(gameState)) {
                for (int i = 0;i < 5;i++){
                    if (speed == Bot.speedState1) {
                        speed = Bot.speedState2;
                    } // asumsikan jika maju lurus dia accelerate, cek speedState diatas speed player sekarang
                    else if (speed == Bot.speedState[i]) {
                        speed = Bot.speedState[i+1];
                        break;
                    }
                }
            }
        }
        for (int i = max(block - startBlock, 0); i <= block - startBlock + speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }
    private List<Object> getBlocksInRightFront(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
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
            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }
    private List<Object> getBlocksInLeftFront(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
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
            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }
}