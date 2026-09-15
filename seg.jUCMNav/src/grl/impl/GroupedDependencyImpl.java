/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package grl.impl;

import grl.GroupedDependency;
import grl.GrlPackage;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Grouped Dependency</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link grl.impl.GroupedDependencyImpl#getDestMultiplicity <em>Dest Multiplicity</em>}</li>
 * </ul>
 *
 * @generated NOT
 */
public class GroupedDependencyImpl extends GRLNodeImpl implements GroupedDependency {
    /**
	 * The default value of the '{@link #getDestMultiplicity() <em>Dest Multiplicity</em>}' attribute.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @see #getDestMultiplicity()
	 * @generated NOT
	 * @ordered
	 */
    protected static final String DEST_MULTIPLICITY_EDEFAULT = null;

    /**
	 * The cached value of the '{@link #getDestMultiplicity() <em>Dest Multiplicity</em>}' attribute.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @see #getDestMultiplicity()
	 * @generated NOT
	 * @ordered
	 */
    protected String destMultiplicity = DEST_MULTIPLICITY_EDEFAULT;

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
    protected GroupedDependencyImpl() {
		super();
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
    protected EClass eStaticClass() {
		return GrlPackage.Literals.GROUPED_DEPENDENCY;
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
    public String getDestMultiplicity() {
		return destMultiplicity;
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
    public void setDestMultiplicity(String newDestMultiplicity) {
		String oldDestMultiplicity = destMultiplicity;
		destMultiplicity = newDestMultiplicity;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, GrlPackage.GROUPED_DEPENDENCY__DEST_MULTIPLICITY, oldDestMultiplicity, destMultiplicity));
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case GrlPackage.GROUPED_DEPENDENCY__DEST_MULTIPLICITY:
				return getDestMultiplicity();
			default:
				return super.eGet(featureID, resolve, coreType);
		}
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case GrlPackage.GROUPED_DEPENDENCY__DEST_MULTIPLICITY:
				setDestMultiplicity((String)newValue);
				return;
			default:
				super.eSet(featureID, newValue);
				return;
		}
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public void eUnset(int featureID) {
		switch (featureID) {
			case GrlPackage.GROUPED_DEPENDENCY__DEST_MULTIPLICITY:
				setDestMultiplicity(DEST_MULTIPLICITY_EDEFAULT);
				return;
			default:
				super.eUnset(featureID);
				return;
		}
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case GrlPackage.GROUPED_DEPENDENCY__DEST_MULTIPLICITY:
				return DEST_MULTIPLICITY_EDEFAULT == null ? destMultiplicity != null : !DEST_MULTIPLICITY_EDEFAULT.equals(destMultiplicity);
			default:
				return super.eIsSet(featureID);
		}
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
    public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (destMultiplicity: ");
		result.append(destMultiplicity);
		result.append(')');
		return result.toString();
	}

} //GroupedDependencyImpl