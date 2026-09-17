/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package grl.impl;

import grl.GroupedDependency;
import grl.GroupedDependencyRef;
import grl.GrlPackage;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Grouped Dependency Ref</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link grl.impl.GroupedDependencyRefImpl#getDef <em>Def</em>}</li>
 * </ul>
 *
 * @generated NOT
 */
public class GroupedDependencyRefImpl extends GRLNodeImpl implements GroupedDependencyRef {
    /**
	 * The cached value of the '{@link #getDef() <em>Def</em>}' reference.
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @see #getDef()
	 * @generated NOT
	 * @ordered
	 */
    protected GroupedDependency def;

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
    protected GroupedDependencyRefImpl() {
		super();
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
    protected EClass eStaticClass() {
		return GrlPackage.Literals.GROUPED_DEPENDENCY_REF;
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
    public GroupedDependency getDef() {
		if (def != null && def.eIsProxy()) {
			InternalEObject oldDef = (InternalEObject)def;
			def = (GroupedDependency)eResolveProxy(oldDef);
			if (def != oldDef) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, GrlPackage.GROUPED_DEPENDENCY_REF__DEF, oldDef, def));
			}
		}
		return def;
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
    public GroupedDependency basicGetDef() {
		return def;
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
    public NotificationChain basicSetDef(GroupedDependency newDef, NotificationChain msgs) {
		GroupedDependency oldDef = def;
		def = newDef;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, GrlPackage.GROUPED_DEPENDENCY_REF__DEF, oldDef, newDef);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
    public void setDef(GroupedDependency newDef) {
		if (newDef != def) {
			NotificationChain msgs = null;
			if (def != null)
				msgs = ((InternalEObject)def).eInverseRemove(this, GrlPackage.GROUPED_DEPENDENCY__REFS, GroupedDependency.class, msgs);
			if (newDef != null)
				msgs = ((InternalEObject)newDef).eInverseAdd(this, GrlPackage.GROUPED_DEPENDENCY__REFS, GroupedDependency.class, msgs);
			msgs = basicSetDef(newDef, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, GrlPackage.GROUPED_DEPENDENCY_REF__DEF, newDef, newDef));
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case GrlPackage.GROUPED_DEPENDENCY_REF__DEF:
				if (def != null)
					msgs = ((InternalEObject)def).eInverseRemove(this, GrlPackage.GROUPED_DEPENDENCY__REFS, GroupedDependency.class, msgs);
				return basicSetDef((GroupedDependency)otherEnd, msgs);
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
			case GrlPackage.GROUPED_DEPENDENCY_REF__DEF:
				return basicSetDef(null, msgs);
			default:
				return super.eInverseRemove(otherEnd, featureID, msgs);
		}
	}

    /**
	 * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case GrlPackage.GROUPED_DEPENDENCY_REF__DEF:
				if (resolve) return getDef();
				return basicGetDef();
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
			case GrlPackage.GROUPED_DEPENDENCY_REF__DEF:
				setDef((GroupedDependency)newValue);
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
			case GrlPackage.GROUPED_DEPENDENCY_REF__DEF:
				setDef((GroupedDependency)null);
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
			case GrlPackage.GROUPED_DEPENDENCY_REF__DEF:
				return def != null;
			default:
				return super.eIsSet(featureID);
		}
	}

} //GroupedDependencyRefImpl