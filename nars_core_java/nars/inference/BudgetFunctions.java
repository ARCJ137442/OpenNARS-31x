/*
 * The MIT License
 *
 * Copyright 2019 The OpenNARS authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package nars.inference;

import nars.entity.*;
import nars.language.*;
import nars.storage.Memory;

/**
 * Budget functions for resources allocation
 */
public final class BudgetFunctions extends UtilityFunctions {

    /* ----------------------- Belief evaluation ----------------------- */
    /**
     * Determine the quality of a judgment by its truth value alone
     * <p>
     * Mainly decided by confidence, though binary judgment is also preferred
     *
     * @param t The truth value of a judgment
     * @return The quality of the judgment, according to truth value only
     */
    public static float truthToQuality(TruthValue t) {
        float exp = t.getExpectation();
        return (float) Math.max(exp, (1 - exp) * 0.75);
    }

    /**
     * Determine the rank of a judgment by its quality and originality (stamp
     * length), called from Concept
     *
     * @param judg The judgment to be ranked
     * @return The rank of the judgment, according to truth value only
     */
    public static float rankBelief(Sentence judg) {
        float confidence = judg.getTruth().getConfidence();
        float originality = 1.0f / (judg.getStamp().length() + 1);
        return or(confidence, originality);
    }

    /* ----- Functions used both in direct and indirect processing of tasks ----- */
    /**
     * Evaluate the quality of a belief as a solution to a problem, then reward
     * the belief and de-prioritize the problem
     *
     * @param problem  The problem (question or goal) to be solved
     * @param solution The belief as solution
     * @param task     The task to be immediately processed, or null for continued
     *                 process
     * @return The budget for the new task which is the belief activated, if
     *         necessary
     */
    static BudgetValue solutionEval(Sentence problem, Sentence solution, Task task, Memory memory) {
        BudgetValue budget = null;
        boolean feedbackToLinks = false;
        if (task == null) { // called in continued processing
            task = memory.currentTask;
            feedbackToLinks = true;
        }
        boolean judgmentTask = task.getSentence().isJudgment();
        float quality = LocalRules.solutionQuality(problem, solution, memory);
        if (judgmentTask) {
            task.incPriority(quality);
        } else {
            float taskPriority = task.getPriority();
            budget = new BudgetValue(or(taskPriority, quality), task.getDurability(),
                    truthToQuality(solution.getTruth()));
            task.setPriority(Math.min(1 - quality, taskPriority));
        }
        if (feedbackToLinks) {
            TaskLink tLink = memory.currentTaskLink;
            tLink.setPriority(Math.min(1 - quality, tLink.getPriority()));
            TermLink bLink = memory.currentBeliefLink;
            bLink.incPriority(quality);
        }
        return budget;
    }

    /**
     * Evaluate the quality of a revision, then de-prioritize the premises
     *
     * @param tTruth The truth value of the judgment in the task
     * @param bTruth The truth value of the belief
     * @param truth  The truth value of the conclusion of revision
     * @return The budget for the new task
     */
    static BudgetValue revise(TruthValue tTruth, TruthValue bTruth, TruthValue truth, boolean feedbackToLinks,
            Memory memory) {
        float difT = truth.getExpDifAbs(tTruth);
        Task task = memory.currentTask;
        task.decPriority(1 - difT);
        task.decDurability(1 - difT);
        if (feedbackToLinks) {
            TaskLink tLink = memory.currentTaskLink;
            tLink.decPriority(1 - difT);
            tLink.decDurability(1 - difT);
            TermLink bLink = memory.currentBeliefLink;
            float difB = truth.getExpDifAbs(bTruth);
            bLink.decPriority(1 - difB);
            bLink.decDurability(1 - difB);
        }
        float dif = truth.getConfidence() - Math.max(tTruth.getConfidence(), bTruth.getConfidence());
        float priority = or(dif, task.getPriority());
        float durability = aveAri(dif, task.getDurability());
        float quality = truthToQuality(truth);
        return new BudgetValue(priority, durability, quality);
    }

    /**
     * Update a belief
     *
     * @param task   The task containing new belief
     * @param bTruth Truth value of the previous belief
     * @return Budget value of the updating task
     */
    static BudgetValue update(Task task, TruthValue bTruth) {
        TruthValue tTruth = task.getSentence().getTruth();
        float dif = tTruth.getExpDifAbs(bTruth);
        float priority = or(dif, task.getPriority());
        float durability = aveAri(dif, task.getDurability());
        float quality = truthToQuality(bTruth);
        return new BudgetValue(priority, durability, quality);
    }

    /* ----------------------- Links ----------------------- */
    /**
     * Distribute the budget of a task among the links to it
     *
     * @param b The original budget
     * @param n Number of links
     * @return Budget value for each link
     */
    public static BudgetValue distributeAmongLinks(BudgetValue b, int n) {
        float priority = (float) (b.getPriority() / Math.sqrt(n));
        return new BudgetValue(priority, b.getDurability(), b.getQuality());
    }

    /* ----------------------- Concept ----------------------- */
    /**
     * Activate a concept by an incoming TaskLink
     *
     * @param concept The concept
     * @param budget  The budget for the new item
     */
    public static void activate(Concept concept, BudgetValue budget) {
        float oldPri = concept.getPriority();
        float priority = or(oldPri, budget.getPriority());
        float durability = aveAri(concept.getDurability(), budget.getDurability());
        float quality = concept.getQuality();
        concept.setPriority(priority);
        concept.setDurability(durability);
        concept.setQuality(quality);
    }

    /* ---------------- Bag functions, on all Items ------------------- */
    /**
     * Decrease Priority after an item is used, called in Bag
     * <p>
     * After a constant time, p should become d*p. Since in this period, the
     * item is accessed c*p times, each time p-q should multiple d^(1/(c*p)).
     * The intuitive meaning of the parameter "forgetRate" is: after this number
     * of times of access, priority 1 will become d, it is a system parameter
     * adjustable in run time.
     *
     * @param budget            The previous budget value
     * @param forgetRate        The budget for the new item
     * @param relativeThreshold The relative threshold of the bag
     */
    public static void forget(BudgetValue budget, float forgetRate, float relativeThreshold) {
        double quality = budget.getQuality() * relativeThreshold; // re-scaled quality
        double p = budget.getPriority() - quality; // priority above quality
        if (p > 0) {
            quality += p * Math.pow(budget.getDurability(), 1.0 / (forgetRate * p));
        } // priority Durability
        budget.setPriority((float) quality);
    }

    /**
     * Merge an item into another one in a bag, when the two are identical
     * except in budget values
     *
     * @param baseValue   The budget value to be modified
     * @param adjustValue The budget doing the adjusting
     */
    public static void merge(BudgetValue baseValue, BudgetValue adjustValue) {
        baseValue.setPriority(Math.max(baseValue.getPriority(), adjustValue.getPriority()));
        baseValue.setDurability(Math.max(baseValue.getDurability(), adjustValue.getDurability()));
        baseValue.setQuality(Math.max(baseValue.getQuality(), adjustValue.getQuality()));
    }

    /* ----- Task derivation in LocalRules and SyllogisticRules ----- */
    /**
     * Forward inference result and adjustment
     *
     * @param truth The truth value of the conclusion
     * @return The budget value of the conclusion
     */
    static BudgetValue forward(TruthValue truth, Memory memory) {
        return budgetInference(truthToQuality(truth), 1, memory);
    }

    /**
     * Backward inference result and adjustment, stronger case
     *
     * @param truth  The truth value of the belief deriving the conclusion
     * @param memory Reference to the memory
     * @return The budget value of the conclusion
     */
    public static BudgetValue backward(TruthValue truth, Memory memory) {
        return budgetInference(truthToQuality(truth), 1, memory);
    }

    /**
     * Backward inference result and adjustment, weaker case
     *
     * @param truth  The truth value of the belief deriving the conclusion
     * @param memory Reference to the memory
     * @return The budget value of the conclusion
     */
    public static BudgetValue backwardWeak(TruthValue truth, Memory memory) {
        return budgetInference(w2c(1) * truthToQuality(truth), 1, memory);
    }

    /* ----- Task derivation in CompositionalRules and StructuralRules ----- */
    /**
     * Forward inference with CompoundTerm conclusion
     *
     * @param truth   The truth value of the conclusion
     * @param content The content of the conclusion
     * @param memory  Reference to the memory
     * @return The budget of the conclusion
     */
    public static BudgetValue compoundForward(TruthValue truth, Term content, Memory memory) {
        return budgetInference(truthToQuality(truth), content.getComplexity(), memory);
    }

    /**
     * Backward inference with CompoundTerm conclusion, stronger case
     *
     * @param content The content of the conclusion
     * @param memory  Reference to the memory
     * @return The budget of the conclusion
     */
    public static BudgetValue compoundBackward(Term content, Memory memory) {
        return budgetInference(1, content.getComplexity(), memory);
    }

    /**
     * Backward inference with CompoundTerm conclusion, weaker case
     *
     * @param content The content of the conclusion
     * @param memory  Reference to the memory
     * @return The budget of the conclusion
     */
    public static BudgetValue compoundBackwardWeak(Term content, Memory memory) {
        return budgetInference(w2c(1), content.getComplexity(), memory);
    }

    /**
     * Common processing for all inference step
     *
     * @param qual       Quality of the inference
     * @param complexity Syntactic complexity of the conclusion
     * @param memory     Reference to the memory
     * @return Budget of the conclusion task
     */
    private static BudgetValue budgetInference(float qual, int complexity, Memory memory) {
        Item t = memory.currentTaskLink;
        if (t == null) {
            t = memory.currentTask;
        }
        float priority = t.getPriority();
        float durability = t.getDurability() / complexity;
        float quality = qual / complexity;
        TermLink bLink = memory.currentBeliefLink;
        if (bLink != null) {
            priority = or(priority, bLink.getPriority());
            durability = and(durability, bLink.getDurability());
            float targetActivation = memory.getConceptActivation(bLink.getTarget());
            bLink.incPriority(or(quality, targetActivation));
            bLink.incDurability(quality);
        }
        return new BudgetValue(priority, durability, quality);
    }
}
