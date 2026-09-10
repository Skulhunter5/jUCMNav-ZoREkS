/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package grl;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Dependency</b></em>'.
 * <!-- end-user-doc -->
 *
 *
 * @see grl.GrlPackage#getDependency()
 * @model
 * @generated
 */
public interface Dependency extends ElementLink {
    /**
	 * Returns the value of the '<em><b>Src Multiplicity</b></em>' attribute.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @return the value of the '<em>Src Multiplicity</em>' attribute.
	 * @see #setSrcMultiplicity(String)
	 * @see grl.GrlPackage#getDependency_SrcMultiplicity()
	 * @model
	 * @generated
	 */
    String getSrcMultiplicity();

    /**
	 * Sets the value of the '{@link grl.Dependency#getSrcMultiplicity <em>Src Multiplicity</em>}' attribute.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Src Multiplicity</em>' attribute.
	 * @see #getSrcMultiplicity()
	 * @generated
	 */
    void setSrcMultiplicity(String value);

    /**
	 * Returns the value of the '<em><b>Dest Multiplicity</b></em>' attribute.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @return the value of the '<em>Dest Multiplicity</em>' attribute.
	 * @see #setDestMultiplicity(String)
	 * @see grl.GrlPackage#getDependency_DestMultiplicity()
	 * @model
	 * @generated
	 */
    String getDestMultiplicity();

    /**
	 * Sets the value of the '{@link grl.Dependency#getDestMultiplicity <em>Dest Multiplicity</em>}' attribute.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Dest Multiplicity</em>' attribute.
	 * @see #getDestMultiplicity()
	 * @generated
	 */
    void setDestMultiplicity(String value);

} // Dependency
