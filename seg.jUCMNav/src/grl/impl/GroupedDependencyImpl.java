/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package grl.impl;

import grl.GRLspec;
import grl.GroupedDependency;
import grl.GroupedDependencyRef;
import grl.GrlPackage;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.util.EObjectWithInverseResolvingEList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Grouped Dependency</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link grl.impl.GroupedDependencyImpl#getDestMultiplicity <em>Dest Multiplicity</em>}</li>
 *   <li>{@link grl.impl.GroupedDependencyImpl#getGrlspec <em>Grlspec</em>}</li>
 *   <li>{@link grl.impl.GroupedDependencyImpl#getRefs <em>Refs</em>}</li>
 * </ul>
 *
 * @generated NOT
 */
public class GroupedDependencyImpl extends GRLLinkableElementImpl implements GroupedDependency {
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
	 * The cached value of the '{@link #getRefs() <em>Refs</em>}' reference list.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @see #getRefs()
	 * @generated NOT
	 * @ordered
	 */
    protected EList refs;

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
    public GRLspec getGrlspec() {
		if (eContainerFeatureID() != GrlPackage.GROUPED_DEPENDENCY__GRLSPEC) return null;
		return (GRLspec)eInternalContainer();
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public NotificationChain basicSetGrlspec(GRLspec newGrlspec, NotificationChain msgs) {
		msgs = eBasicSetContainer((InternalEObject)newGrlspec, GrlPackage.GROUPED_DEPENDENCY__GRLSPEC, msgs);
		return msgs;
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
    public void setGrlspec(GRLspec newGrlspec) {
		if (newGrlspec != eInternalContainer() || (eContainerFeatureID() != GrlPackage.GROUPED_DEPENDENCY__GRLSPEC && newGrlspec != null)) {
			if (EcoreUtil.isAncestor(this, newGrlspec))
				throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
			NotificationChain msgs = null;
			if (eInternalContainer() != null)
				msgs = eBasicRemoveFromContainer(msgs);
			if (newGrlspec != null)
				msgs = ((InternalEObject)newGrlspec).eInverseAdd(this, GrlPackage.GR_LSPEC__GROUPED_DEPENDENCIES, GRLspec.class, msgs);
			msgs = basicSetGrlspec(newGrlspec, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, GrlPackage.GROUPED_DEPENDENCY__GRLSPEC, newGrlspec, newGrlspec));
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
    public EList getRefs() {
		if (refs == null) {
			refs = new EObjectWithInverseResolvingEList(GroupedDependencyRef.class, this, GrlPackage.GROUPED_DEPENDENCY__REFS, GrlPackage.GROUPED_DEPENDENCY_REF__DEF);
		}
		return refs;
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case GrlPackage.GROUPED_DEPENDENCY__GRLSPEC:
				if (eInternalContainer() != null)
					msgs = eBasicRemoveFromContainer(msgs);
				return basicSetGrlspec((GRLspec)otherEnd, msgs);
			case GrlPackage.GROUPED_DEPENDENCY__REFS:
				return ((InternalEList)getRefs()).basicAdd(otherEnd, msgs);
			default:
				return super.eInverseAdd(otherEnd, featureID, msgs);
		}
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case GrlPackage.GROUPED_DEPENDENCY__GRLSPEC:
				return basicSetGrlspec(null, msgs);
			case GrlPackage.GROUPED_DEPENDENCY__REFS:
				return ((InternalEList)getRefs()).basicRemove(otherEnd, msgs);
			default:
				return super.eInverseRemove(otherEnd, featureID, msgs);
		}
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public NotificationChain eBasicRemoveFromContainerFeature(NotificationChain msgs) {
		switch (eContainerFeatureID()) {
			case GrlPackage.GROUPED_DEPENDENCY__GRLSPEC:
				return eInternalContainer().eInverseRemove(this, GrlPackage.GR_LSPEC__GROUPED_DEPENDENCIES, GRLspec.class, msgs);
			default:
				return super.eBasicRemoveFromContainerFeature(msgs);
		}
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
			case GrlPackage.GROUPED_DEPENDENCY__GRLSPEC:
				return getGrlspec();
			case GrlPackage.GROUPED_DEPENDENCY__REFS:
				return getRefs();
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
			case GrlPackage.GROUPED_DEPENDENCY__GRLSPEC:
				setGrlspec((GRLspec)newValue);
				return;
			case GrlPackage.GROUPED_DEPENDENCY__REFS:
				getRefs().clear();
				getRefs().addAll((Collection)newValue);
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
			case GrlPackage.GROUPED_DEPENDENCY__GRLSPEC:
				setGrlspec((GRLspec)null);
				return;
			case GrlPackage.GROUPED_DEPENDENCY__REFS:
				getRefs().clear();
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
			case GrlPackage.GROUPED_DEPENDENCY__GRLSPEC:
				return getGrlspec() != null;
			case GrlPackage.GROUPED_DEPENDENCY__REFS:
				return refs != null && !refs.isEmpty();
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