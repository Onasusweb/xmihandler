package gov.nih.nci.ncicb.xmiinout.domain;

import java.util.*;

/**
 * Represents the end of an association
 */
public interface UMLAssociationEnd {

  /**
   * @return the element (i.e class) this end points to
   */
  public UMLAssociable getUMLElement();

  /**
   * @return the end role name, or null if there isn't any.
   */
  public String getRoleName();
  
  /**
   * @return the low multiplicity.
   */
  public int getLowMultiplicity();

  /**
   * @return the high multiplicity, -1 represents * (or n)
   */
  public int getHighMultiplicity();

  /**
   * @return true is this end is navigable
   */
  public boolean isNavigable();

  /**
   * @return the visibility (scope)
   */
  public UMLVisibility getVisibility();

  

}