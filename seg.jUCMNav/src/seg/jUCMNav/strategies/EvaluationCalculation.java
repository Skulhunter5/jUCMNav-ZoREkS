package seg.jUCMNav.strategies;

import grl.GRLLinkableElement;

/**
 * Data container object used by the propagation mechanism.
 * 
 * @author Jean-Fran�ois Roy, Yanji Liu, gunterm
 * 
 */
public class EvaluationCalculation {
    private GRLLinkableElement element;
    private int linkCalc;
    private int totalLinkDest;

    public EvaluationCalculation(GRLLinkableElement element, int totalLink) {
        this.element = element;
        this.totalLinkDest = totalLink;
        linkCalc = 0;
    }

	public GRLLinkableElement getElement() {
		return element;
	}
    
    public boolean hasReachedTotalLink() {
   		return this.linkCalc >= this.totalLinkDest;
    }
    
    public void incrementLinkCalc() {
    	this.linkCalc++;
    }
}
