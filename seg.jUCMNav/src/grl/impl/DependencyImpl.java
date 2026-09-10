/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package grl.impl;

import grl.Dependency;
import grl.GrlPackage;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Dependency</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link grl.impl.DependencyImpl#getSrcMultiplicity <em>Src Multiplicity</em>}</li>
 *   <li>{@link grl.impl.DependencyImpl#getDestMultiplicity <em>Dest Multiplicity</em>}</li>
 * </ul>
 *
 * @generated
 */
public class DependencyImpl extends ElementLinkImpl implements Dependency {
    /**
	 * The default value of the '{@link #getSrcMultiplicity() <em>Src Multiplicity</em>}' attribute.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @see #getSrcMultiplicity()
	 * @generated
	 * @ordered
	 */
    protected static final String SRC_MULTIPLICITY_EDEFAULT = null;

    /**
	 * The cached value of the '{@link #getSrcMultiplicity() <em>Src Multiplicity</em>}' attribute.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @see #getSrcMultiplicity()
	 * @generated
	 * @ordered
	 */
    protected String srcMultiplicity = SRC_MULTIPLICITY_EDEFAULT;

    /**
	 * The default value of the '{@link #getDestMultiplicity() <em>Dest Multiplicity</em>}' attribute.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @see #getDestMultiplicity()
	 * @generated
	 * @ordered
	 */
    protected static final String DEST_MULTIPLICITY_EDEFAULT = null;

    /**
	 * The cached value of the '{@link #getDestMultiplicity() <em>Dest Multiplicity</em>}' attribute.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @see #getDestMultiplicity()
	 * @generated
	 * @ordered
	 */
    protected String destMultiplicity = DEST_MULTIPLICITY_EDEFAULT;

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated
	 */
    protected DependencyImpl() {
		super();
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated
	 */
    protected EClass eStaticClass() {
		return GrlPackage.Literals.DEPENDENCY;
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated
	 */
    public String getSrcMultiplicity() {
		return srcMultiplicity;
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated
	 */
    public void setSrcMultiplicity(String newSrcMultiplicity) {
		String oldSrcMultiplicity = srcMultiplicity;
		srcMultiplicity = newSrcMultiplicity;
		eNotify(new ENotificationImpl(this, Notification.SET, GrlPackage.DEPENDENCY__SRC_MULTIPLICITY, oldSrcMultiplicity, srcMultiplicity));
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated
	 */
    public String getDestMultiplicity() {
		return destMultiplicity;
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated
	 */
    public void setDestMultiplicity(String newDestMultiplicity) {
		String oldDestMultiplicity = destMultiplicity;
		destMultiplicity = newDestMultiplicity;
		eNotify(new ENotificationImpl(this, Notification.SET, GrlPackage.DEPENDENCY__DEST_MULTIPLICITY, oldDestMultiplicity, destMultiplicity));
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated
	 */
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case GrlPackage.DEPENDENCY__SRC_MULTIPLICITY:
				return getSrcMultiplicity();
			case GrlPackage.DEPENDENCY__DEST_MULTIPLICITY:
				return getDestMultiplicity();
			default:
				return super.eGet(featureID, resolve, coreType);
		}
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated
	 */
    public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case GrlPackage.DEPENDENCY__SRC_MULTIPLICITY:
				setSrcMultiplicity((String)newValue);
				return;
			case GrlPackage.DEPENDENCY__DEST_MULTIPLICITY:
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
	 * @generated
	 */
    public void eUnset(int featureID) {
		switch (featureID) {
			case GrlPackage.DEPENDENCY__SRC_MULTIPLICITY:
				setSrcMultiplicity(SRC_MULTIPLICITY_EDEFAULT);
				return;
			case GrlPackage.DEPENDENCY__DEST_MULTIPLICITY:
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
	 * @generated
	 */
    public boolean eIsSet(int featureID) {
		switch (featureID) {
			case GrlPackage.DEPENDENCY__SRC_MULTIPLICITY:
				return SRC_MULTIPLICITY_EDEFAULT == null ? srcMultiplicity != null : !SRC_MULTIPLICITY_EDEFAULT.equals(srcMultiplicity);
			case GrlPackage.DEPENDENCY__DEST_MULTIPLICITY:
				return DEST_MULTIPLICITY_EDEFAULT == null ? destMultiplicity != null : !DEST_MULTIPLICITY_EDEFAULT.equals(destMultiplicity);
			default:
				return super.eIsSet(featureID);
		}
	}

} //DependencyImpl