package jevc.operations;

import jevc.entities.Block;
import jevc.entities.MotionVector;

public class MotionEstimator {
    // TODO: this class might not be needed
    public MotionEstimator() {

    }

    public MotionVector computeMotionVector(Block block, Block similarBlock) {
        // compute motion vector
        // block:        [x0, y0]
        // similarBlock: [x1, y1]
        // mVector:      [x1-x0, y1-y0]

        // DEBUG
//        System.out.println();
//        System.out.println("MOTION VECTOR FOR BLOCK");
//        int x = similarBlock.getPosX() - block.getPosX();
//        int y = similarBlock.getPosY() - block.getPosY();
//        System.out.println("[" + x + ", " + y + "]");
        return new MotionVector(
                similarBlock.getPosX() - block.getPosX(),
                similarBlock.getPosY() - block.getPosY()
        );
    }
}
