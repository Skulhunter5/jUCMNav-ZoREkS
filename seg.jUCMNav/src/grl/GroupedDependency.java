/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package grl;

import org.eclipse.emf.common.util.EList;

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
 *   <li>{@link grl.GroupedDependency#getGrlspec <em>Grlspec</em>}</li>
 *   <li>{@link grl.GroupedDependency#getRefs <em>Refs</em>}</li>
 * </ul>
 *
 * @see grl.GrlPackage#getGroupedDependency()
 * @model
 * @generated NOT
 */
public interface GroupedDependency extends GRLLinkableElement {
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

    /**
	 * Returns the value of the '<em><b>Grlspec</b></em>' container reference.
	 * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Grlspec</em>' container reference isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
	 * @return the value of the '<em>Grlspec</em>' container reference.
	 * @see #setGrlspec(GRLspec)
	 * @see grl.GrlPackage#getGroupedDependency_Grlspec()
	 * @model opposite="groupedDependencies" transient="false" changeable="false"
	 * @generated NOT
	 */
    GRLspec getGrlspec();

    /**
	 * Sets the value of the '{@link grl.GroupedDependency#getGrlspec <em>Grlspec</em>}' container reference.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Grlspec</em>' container reference.
	 * @see #getGrlspec()
	 * @generated NOT
	 */
    void setGrlspec(GRLspec value);

    /**
	 * Returns the value of the '<em><b>Refs</b></em>' reference list.
	 * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Refs</em>' reference list isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
	 * @return the value of the '<em>Refs</em>' reference list.
	 * @see grl.GrlPackage#getGroupedDependency_Refs()
	 * @model opposite="def"
	 * @generated NOT
	 */
    EList<GroupedDependencyRef> getRefs();

} // GroupedDependency