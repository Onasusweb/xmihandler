package gov.nih.nci.ncicb.xmiinout.handler.impl;


import gov.nih.nci.ncicb.xmiinout.domain.UMLAssociation;
import gov.nih.nci.ncicb.xmiinout.domain.UMLAttribute;
import gov.nih.nci.ncicb.xmiinout.domain.UMLClass;
import gov.nih.nci.ncicb.xmiinout.domain.UMLDatatype;
import gov.nih.nci.ncicb.xmiinout.domain.UMLDependency;
import gov.nih.nci.ncicb.xmiinout.domain.UMLGeneralization;
import gov.nih.nci.ncicb.xmiinout.domain.UMLModel;
import gov.nih.nci.ncicb.xmiinout.domain.UMLPackage;
import gov.nih.nci.ncicb.xmiinout.domain.UMLTaggedValue;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLClassBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLModelBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLPackageBean;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jaxen.JaxenException;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * 
 */
public abstract class ArgoUMLBaseImpl extends DefaultXmiHandler {
  private static Logger logger = Logger.getLogger(ArgoUMLBaseImpl.class.getName());

  protected Element rootElement;

  protected Map<String, UMLClassBean> idClassMap = new HashMap<String, UMLClassBean>();

  public void _load(String filename) {
	  
    try {
      SAXBuilder builder = new SAXBuilder();
      Document doc = builder.build(filename);
      rootElement = doc.getRootElement();

      logger.debug("rootElement.getName(): " + rootElement.getName());      
      System.out.println("rootElement.getName(): " + rootElement.getName());
      readModel(rootElement);
      
    } catch (Exception ex) {
      throw new RuntimeException("Error initializing model", ex);
    }
    
  }

  public void _load(java.net.URI uri) {

    try {
      SAXBuilder builder = new SAXBuilder();
      Document doc = builder.build(uri.toURL());
      rootElement = doc.getRootElement();
      readModel(rootElement);
      
    } catch (Exception ex) {
      throw new RuntimeException("Error initializing model", ex);
    }
    
  }


  public void save(String filename) throws IOException {
    File f = new File(filename);
    
    Writer writer = new OutputStreamWriter
      (new FileOutputStream(f), "UTF-8");
    XMLOutputter xmlout = new XMLOutputter();
    xmlout.setFormat(Format.getPrettyFormat());
    writer.write(xmlout.outputString(rootElement));
    writer.flush();
    writer.close();
    
  }

  public UMLModel getModel() {
    Iterator<UMLModel> it = models.values().iterator();
    if(it.hasNext())
      return it.next();
    else 
      return null;
  }

  public UMLModel getModel(String modelName) {
    return models.get(modelName);
  }

  private void readModel(Element rootElement) throws JaxenException {

    String xpath = "/XMI/XMI.content/*[local-name()='Model']";

    JDOMXPath path = new JDOMXPath(xpath);
    List<Element> elts = path.selectNodes(rootElement);

    System.out.println("Elements Found:  " + elts.size());
    
    for(Element elt : elts) {
      UMLModelBean model = JDomXmiTransformer.toUMLModel(elt);
      models.put(model.getName(), model);
      
      System.out.println("Model Name:  " + model.getName());
      
      for(UMLDatatype type : doDataTypes(elt)) {
        model.addDatatype(type);
        JDomXmiTransformer.addDatatype(type);
      }

      for(UMLPackage pkg : doPackages(elt)) {
        model.addPackage(pkg);
      }

      for(UMLClass clazz : doClasses(elt)) {
        model.addClass(clazz);
      }

      for(UMLGeneralization gen : doGeneralizations(elt)) {
        model.addGeneralization(gen);
      }

      for(UMLDependency dep : doDependencies(elt)) {
        model._addDependency(dep);
      }

      for(UMLAssociation assoc : doAssociations(elt)) {
        model.addAssociation(assoc);
      }      
    }

    doRootTaggedValues(rootElement);

    // Must be done after classes for cross references.
    JDomXmiTransformer.completeAttributes();
  }


  private List<UMLTaggedValue> doRootTaggedValues(Element rootElement) 
    throws JaxenException {
    String xpath = "/XMI/XMI.content/*[local-name()='TaggedValue']";
    JDOMXPath path = new JDOMXPath(xpath);
    List<Element> elts = path.selectNodes(rootElement);
    
    List<UMLTaggedValue> result = new ArrayList<UMLTaggedValue>();

    for(Element tvElt : elts) {
      UMLTaggedValue tv = JDomXmiTransformer.toUMLTaggedValue(tvElt);

      if(tv != null)
        result.add(tv);

      Attribute refAtt = tvElt.getAttribute("modelElement");
      if(refAtt != null) {
        UMLClassBean clazz = idClassMap.get(refAtt.getValue());
        if(clazz != null) {
          clazz.addTaggedValue(tv);
        }
      }
    }

    return result;
    
  }

  private List<UMLPackageBean> doPackages(Element elt) {
    Namespace ns = Namespace.getNamespace("org.omg.xmi.namespace.UML");
    Element ownedElement = elt.getChild("Namespace.ownedElement", ns);
    
    if (ownedElement == null){
    	System.out.println("ownedElement is null for Element " + elt.getAttributeValue("name"));
    	return (List)new ArrayList();
    }
    
    List<Element> packageElements = (List<Element>)ownedElement.getChildren("Package", ns);

    System.out.println("Package size: " + packageElements.size());
    List<UMLPackageBean> result = new ArrayList<UMLPackageBean>();

    for(Element pkgElement : packageElements) {
      System.out.println("Package name: " + pkgElement.getAttributeValue("name"));
      UMLPackageBean umlPkg = JDomXmiTransformer.toUMLPackage(pkgElement);
      result.add(umlPkg);

      Collection<UMLTaggedValue> taggedValues = doTaggedValues(pkgElement);
      for(UMLTaggedValue tv : taggedValues) {
        umlPkg.addTaggedValue(tv);
      }

      for(UMLPackageBean pkg : doPackages(pkgElement)) {
        umlPkg.addPackage(pkg);
      }
      for(UMLClassBean clazz : doClasses(pkgElement)) {
        umlPkg.addClass(clazz);
      }
    }
    
    return result;
    
  }

  private List<UMLClassBean> doClasses(Element elt) {
    Namespace ns = Namespace.getNamespace("org.omg.xmi.namespace.UML");
    Element ownedElement = elt.getChild("Namespace.ownedElement", ns);
    
    
    if (ownedElement == null){
    	System.out.println("ownedElement is null for Element " + elt.getAttributeValue("name"));
    	return (List)new ArrayList();
    }
    
    List<Element> classElements = (List<Element>)ownedElement.getChildren("Class", ns);
    List<UMLClassBean> result = new ArrayList<UMLClassBean>();

    for(Element classElement : classElements) {
      UMLClassBean umlClass = JDomXmiTransformer.toUMLClass(classElement);
      Collection<UMLTaggedValue> taggedValues = doTaggedValues(classElement);
      for(UMLTaggedValue tv : taggedValues) {
        umlClass.addTaggedValue(tv);
      }

      List<UMLAttribute> atts = doAttributes(classElement);
      for(UMLAttribute att : atts) {
        umlClass.addAttribute(att);
      }

      idClassMap.put(umlClass.getModelId(), umlClass);
      result.add(umlClass);
      

    }
    
    return result;

  }


  
  protected abstract List<UMLAttribute> doAttributes(Element elt);

  protected abstract List<UMLTaggedValue> doTaggedValues(Element elt);

  protected abstract List<UMLAssociation> doAssociations(Element elt) throws JaxenException;

  protected abstract List<UMLGeneralization> doGeneralizations(Element elt) throws JaxenException;

  protected abstract List<UMLDependency> doDependencies(Element elt) throws JaxenException;

  protected abstract List<UMLDatatype> doDataTypes(Element elt);
}