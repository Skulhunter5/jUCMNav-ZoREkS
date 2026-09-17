/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package grl;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Grouped Dependency Ref</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link grl.GroupedDependencyRef#getDef <em>Def</em>}</li>
 * </ul>
 *
 * @see grl.GrlPackage#getGroupedDependencyRef()
 * @model
 * @generated NOT
 */
public interface GroupedDependencyRef extends GRLNode {
    /**
	 * Returns the value of the '<em><b>Def</b></em>' reference.
	 * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Def</em>' reference isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
	 * @return the value of the '<em>Def</em>' reference.
	 * @see #setDef(GroupedDependency)
	 * @see grl.GrlPackage#getGroupedDependencyRef_Def()
	 * @model required="true" opposite="refs"
	 * @generated NOT
	 */
    GroupedDependency getDef();

    /**
	 * Sets the value of the '{@link grl.GroupedDependencyRef#getDef <em>Def</em>}' reference.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Def</em>' reference.
	 * @see #getDef()
	 * @generated NOT
	 */
    void setDef(GroupedDependency value);

} // GroupedDependencyRef