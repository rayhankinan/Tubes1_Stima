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
        List<Object> nextBlocks = blocks.subList(0,1);

        // Fix mechanism (When reach maximum speed for damage, fix the car)
        if (myCar.damage >= 5) {
            return FIX;
        }
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

        // Avoidance mechanism (Accelerate or Decelerate)

        // Lizard mechanism and Avoidance mechanism (Left or Right)
        if (blocks.contains(Terrain.MUD) || blocks.contains(Terrain.WALL) || blocks.contains(Terrain.OIL_SPILL)) {
            if (myCar.position.lane == 1) {
                return TURN_RIGHT;
            }
            if (myCar.position.lane == 4) {
                return TURN_LEFT;
            }
            if (PowerUp(PowerUps.LIZARD, myCar.powerups) > 0) {
                return LIZARD;
            }
            int i = random.nextInt(directionList.size());
            return directionList.get(i);
        }

        // Boost mechanism
        if (PowerUp(PowerUps.BOOST, myCar.powerups) > 0) {
            return BOOST;
        }

        // Oil mechanism
        if (myCar.speed == maxSpeed) {
            if (PowerUp(PowerUps.OIL, myCar.powerups) > 0) {
                return OIL;
            }
        }

        // EMP mechanism
        if (myCar.speed == maxSpeed) {
            if (PowerUp(PowerUps.EMP, myCar.powerups) > 0) {
                return EMP;
            }
        }

        // Tweet mechanism
        if (PowerUp(PowerUps.TWEET, myCar.powerups) > 0) {
            if (myCar.position.block > opponent.position.block) {
                TWEET = new TweetCommand(opponent.position.lane,opponent.position.block + opponent.speed - 1);
                return TWEET;
            }
        }

        // Default return value
        return ACCELERATE; // Change do DO_NOTHING if avoidance mechanism is finished
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

    /**
     * Returns map of blocks and the objects in the for the current lanes, returns the amount of blocks that can be
     * traversed at max speed.
     **/
    private List<Object> getBlocksInFront(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + Bot.maxSpeed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }
}
