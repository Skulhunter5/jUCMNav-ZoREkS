package seg.jUCMNav.strategies;

import fm.Feature;
import grl.ElementLink;
import grl.Evaluation;
import grl.EvaluationStrategy;
import grl.GRLLinkableElement;
import grl.GroupedDependency;
import grl.IntentionalElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import seg.jUCMNav.extensionpoints.IGRLStrategyAlgorithm;
import seg.jUCMNav.model.util.DependencyMultiplicity;
import seg.jUCMNav.model.util.StrategyEvaluationRangeHelper;
import seg.jUCMNav.strategies.util.FeatureUtil;

/**
 * This class contains the common behavior of the qualitative and quantitative GRL evaluation algorithm.
 * 
 * @author gunterm
 * 
 */
public abstract class PropagationGRLStrategyAlgorithm {

    Vector<GRLLinkableElement> evalReady;
    HashMap<GRLLinkableElement, EvaluationCalculation> evaluationCalculation;
    HashMap evaluations;
    
    /*
     * (non-Javadoc)
     * 
     * @see seg.jUCMNav.extensionpoints.IGRLStrategiesAlgorithm#init(java.util.Vector)
     */
    public void init(EvaluationStrategy strategy, HashMap evaluations) {
        evalReady = new Vector<GRLLinkableElement>();
        Vector<GRLLinkableElement> evalReadyFMLeafs = new Vector<GRLLinkableElement>();
        Vector<GRLLinkableElement> evalReadyUserDefined = new Vector<GRLLinkableElement>();
        evaluationCalculation = new HashMap<GRLLinkableElement, EvaluationCalculation>();
        this.evaluations = evaluations;

        // for the evaluation algorithm of Feature Models, the order of the evalReady elements is important! other algorithms do not care.
        // first leaf nodes, then leaf nodes in the Feature Model, and then nodes with user defined evaluation values
        Iterator it = strategy.getGrlspec().getIntElements().iterator();
		while (it.hasNext()) {
		    IntentionalElement element = (IntentionalElement) it.next();
		    if (element.getLinksDest().size() == 0) {
		        evalReady.add(element);
		    } else if (element instanceof Feature && FeatureUtil.isLeafFeature((Feature) element)) {
		    	evalReadyFMLeafs.add(element);
		    }
		    else if (((Evaluation) evaluations.get(element)).getStrategies() != null) {
		    	evalReadyUserDefined.add(element);
		    }
		    else {
		        EvaluationCalculation calculation = new EvaluationCalculation(element, element.getLinksDest().size());
		        evaluationCalculation.put(element, calculation);
		    }
		}
		// grouped dependency hubs are scheduled the same way, waiting for every fan in their
		// linksDest: after the orientation fix those are the target-side fans (drawn box -> target),
		// whose instances are the ones the multiplicity counts.
		it = strategy.getGrlspec().getGroupedDependencies().iterator();
		while (it.hasNext()) {
		    GroupedDependency hub = (GroupedDependency) it.next();
		    if (hub.getLinksDest().size() == 0) {
		        evalReady.add(hub);
		    }
		    else {
		        EvaluationCalculation calculation = new EvaluationCalculation(hub, hub.getLinksDest().size());
		        evaluationCalculation.put(hub, calculation);
		    }
		}
		// this ensures that all leaf nodes are handled before feature leaf nodes, and the user defined nodes are handled last
		evalReady.addAll(evalReadyFMLeafs);
		evalReady.addAll(evalReadyUserDefined);
    }

	/*
     * (non-Javadoc)
     * 
     * @see seg.jUCMNav.extensionpoints.IGRLStrategiesAlgorithm#hasNextNode()
     */
    public boolean hasNextNode() {
        if (evalReady.size() > 0) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see seg.jUCMNav.extensionpoints.IGRLStrategiesAlgorithm#nextNode()
     */
    public GRLLinkableElement nextNode() {
        GRLLinkableElement intElem = (GRLLinkableElement) evalReady.remove(0);

        for (Iterator j = intElem.getLinksSrc().iterator(); j.hasNext();) {
            GRLLinkableElement temp = (GRLLinkableElement) ((ElementLink) j.next()).getDest();
            addToEvalReadyIfCovered(temp);
        }
        return intElem;
    }
    
    protected void addToEvalReadyIfCovered(GRLLinkableElement intElem) {
        if (evaluationCalculation.containsKey(intElem)) {
            EvaluationCalculation calc = (EvaluationCalculation) evaluationCalculation.get(intElem);
            calc.incrementLinkCalc();
            if (calc.hasReachedTotalLink()) {
                evaluationCalculation.remove(intElem);
                // add this new element into the first position of the vector, so that the original order of the evalReady elements is respected 
                // (first all leaf nodes plus all nodes that can be reached from them, then feature model leaf nodes...) 
                evalReady.add(0, calc.getElement());
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see seg.jUCMNav.extensionpoints.IGRLStrategiesAlgorithm#getGroupedDependencyEvaluation(grl.EvaluationStrategy, java.util.HashMap, grl.GroupedDependency)
     */
    public int getGroupedDependencyEvaluation(EvaluationStrategy strategy, HashMap evaluations, GroupedDependency groupedDependency) {
        int[] bounds = DependencyMultiplicity.parseBounds(groupedDependency.getDestMultiplicity());
        // no (or invalid) multiplicity: the grouped dependency imposes no constraint on the source side
        if (bounds == null)
            return IGRLStrategyAlgorithm.SATISFICED;

        int lower = bounds[0];
        int upper = bounds[1];

        int scaleMin = -100 * (StrategyEvaluationRangeHelper.getCurrentRange(strategy.getGrlspec().getUrnspec()) ? 0 : 1);
        if (scaleMin != -100) {
        	System.err.println("[Grouped Dependency Evaluation] Unsupposed evaluation scale");
        	return -102;
        }
        
        int targetCount = groupedDependency.getLinksDest().size();
        List<Integer> targetEvaluations = new ArrayList<>(targetCount);
        
        final int FULLY_SATISFIED = 100;
        final int WEAKLY_SATISFIED = 50;
        final int WEAKLY_DENIED = -50;
        final int FULLY_DENIED = -100;
        
        int fullySatisfied = 0;
        int weaklySatisfied = 0;
        int weaklyDenied = 0;
        int fullyDenied = 0;
        int unknownCount = 0;
        int specialCount = 0;
        for (Iterator iter = groupedDependency.getLinksDest().iterator(); iter.hasNext();) {
            ElementLink link = (ElementLink) iter.next();
            Evaluation targetEval = (Evaluation) evaluations.get(link.getSrc());
            if (targetEval != null) {
            	int evaluation = targetEval.getEvaluation();
            	if (evaluation < -50) {
            		fullyDenied++;
            	} else if (evaluation < 0) {
            		weaklyDenied++;
            	} else if (evaluation > 50) {
            		fullySatisfied++;
            	} else if (evaluation > 0) {
            		weaklySatisfied++;
            	} else if (evaluation == 0){
            		unknownCount++;
            	} else {
            		specialCount++;
            	}
                targetEvaluations.add(evaluation);
            } else {
            	targetEvaluations.add(0);
            }
        }
        int satisfiedCount = fullySatisfied + weaklySatisfied;
        int deniedCount = fullyDenied + weaklyDenied;
        
        if (targetCount - unknownCount == 0) {
        	return 0;
        }

        //System.out.println("targetEvaluations: " + targetEvaluations.toString());
        
        if (specialCount > 0) {
        	System.err.println("[Grouped Dependency Evaluation] Conflict or Unknown qualitative label not supported for grouped dependency evaluation");
        	return -102;
        }
        
        // CASE 1
        if (lower == 1 && upper == targetCount) {
        	int limit = Collections.max(targetEvaluations);
        	return limit;
        }
        // CASE 2
        if (lower == targetCount && upper == targetCount) {
        	int limit = Collections.min(targetEvaluations);
        	return limit;
        }
        // CASE 3
        if (lower >= 1 && 1 <= upper && upper <= targetCount) {
        	//targetCount -= unknownCount;
        	if (lower <= fullySatisfied && fullySatisfied <= upper
        			&& fullyDenied == (targetCount - fullySatisfied)) {
        		return FULLY_SATISFIED;
        	}
        	if (lower <= fullySatisfied && fullySatisfied <= upper
        			&& weaklySatisfied == 0
        			&& weaklyDenied > 0) {
        		return WEAKLY_SATISFIED;
        	}
        	if (lower <= fullySatisfied && fullySatisfied <= upper
        			&& weaklySatisfied == 0
        			&& weaklySatisfied > weaklyDenied) {
        		return WEAKLY_SATISFIED;
        	}
        	if (lower <= fullySatisfied && fullySatisfied <= upper
        			&& weaklySatisfied == 0
        			&& weaklySatisfied <= weaklyDenied) {
        		return WEAKLY_DENIED;
        	}
        	if (lower <= satisfiedCount && satisfiedCount <= upper
        			&& fullySatisfied > weaklySatisfied
        			&& fullyDenied > weaklyDenied) {
        		return WEAKLY_SATISFIED;
        	}
        	if (lower <= satisfiedCount && satisfiedCount <= upper
        			&& fullySatisfied > weaklySatisfied
        			&& fullyDenied <= weaklyDenied) {
        		return WEAKLY_DENIED;
        	}
        	if (lower <= satisfiedCount && satisfiedCount <= upper
        			&& fullySatisfied <= weaklySatisfied
        			&& fullyDenied > weaklyDenied
        			&& fullyDenied > weaklySatisfied) {
        		return WEAKLY_SATISFIED;
        	}
        	if (lower <= satisfiedCount && satisfiedCount <= upper
        			&& fullySatisfied <= weaklySatisfied
        			&& fullyDenied <= weaklyDenied) {
        		return WEAKLY_DENIED;
        	}
        	if (fullySatisfied == 0
        			&& lower <= weaklySatisfied && weaklySatisfied <= upper
        			&& fullyDenied == 0) { // original: 
        		return WEAKLY_SATISFIED;
        	}
        	if (fullySatisfied == 0
        			&& lower <= weaklySatisfied && weaklySatisfied <= upper
        			&& fullyDenied > 0) { // original: weaklyDenied < (targetCount - weaklySatisfied)
        		return WEAKLY_DENIED;
        	}
        	return FULLY_DENIED;
        }
        // CASE 4
        if (lower == 0 && upper == 0) {
        	if (deniedCount == targetCount) {
        		return FULLY_SATISFIED;
        	} else {
        		return FULLY_DENIED;
        	}
        }
        
        System.err.println("[Grouped Dependency Evaluation] Missing case for " + targetEvaluations.toString() + " and " + lower + ".." + upper);
        return -102;
        
//        // the box's linksDest holds the target-side fans; each target instance is the fan's src.
//        // the multiplicity counts how many target instances are satisfied, mirroring how an ordinary
//        // dependency restricts its drawing-start (source) based on its drawing-end (target).
//        int satisfied = 0;
//        for (Iterator iter = groupedDependency.getLinksDest().iterator(); iter.hasNext();) {
//            ElementLink link = (ElementLink) iter.next();
//            Evaluation targetEval = (Evaluation) evaluations.get(link.getSrc());
//            if (targetEval != null && targetEval.getEvaluation() > 0)
//                ++satisfied;
//        }
//
//        // placeholder semantics: the multiplicity is either met or not.
//        // fewer satisfied targets than the required minimum, or more than the upper bound, fully
//        // denies the group: the box evaluates to scaleMin and its value restricts the source-side
//        // copies (each source copy is the dest of a box fan and is clipped to the box's value).
//        if (lower > 0 && satisfied < lower)
//            return scaleMin;
//        if (upper >= 0 && satisfied > upper)
//            return scaleMin;
//        return IGRLStrategyAlgorithm.SATISFICED;
    }

}
