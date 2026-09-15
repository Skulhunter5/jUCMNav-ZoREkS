/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package grl;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Grouped Dependency</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link grl.GroupedDependency#getDestMultiplicity <em>Dest Multiplicity</em>}</li>
 * </ul>
 *
 * @see grl.GrlPackage#getGroupedDependency()
 * @model
 * @generated NOT
 */
public interface GroupedDependency extends GRLNode {
    /**
	 * Returns the value of the '<em><b>Dest Multiplicity</b></em>' attribute.
	 * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Dest Multiplicity</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
	 * @return the value of the '<em>Dest Multiplicity</em>' attribute.
	 * @see #setDestMultiplicity(String)
	 * @see grl.GrlPackage#getGroupedDependency_DestMultiplicity()
	 * @model
	 * @generated NOT
	 */
    String getDestMultiplicity();

    /**
	 * Sets the value of the '{@link grl.GroupedDependency#getDestMultiplicity <em>Dest Multiplicity</em>}' attribute.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Dest Multiplicity</em>' attribute.
	 * @see #getDestMultiplicity()
	 * @generated NOT
	 */
    void setDestMultiplicity(String value);

} // GroupedDependency