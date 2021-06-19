//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.06.19 at 07:50:37 PM IST 
//


package com.ecosys.ImportBudget;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.msproject.budget package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _MSPImportBudgetRequest_QNAME = new QName("http://ecosys.net/api/MSPImportBudget", "MSPImportBudgetRequest");
    private final static QName _MSPImportBudgetResult_QNAME = new QName("http://ecosys.net/api/MSPImportBudget", "MSPImportBudgetResult");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.msproject.budget
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link MSPImportBudgetResultType }
     * 
     */
    public MSPImportBudgetResultType createMSPImportBudgetResultType() {
        return new MSPImportBudgetResultType();
    }

    /**
     * Create an instance of {@link MSPImportBudgetRequestType }
     * 
     */
    public MSPImportBudgetRequestType createMSPImportBudgetRequestType() {
        return new MSPImportBudgetRequestType();
    }

    /**
     * Create an instance of {@link PerformanceType }
     * 
     */
    public PerformanceType createPerformanceType() {
        return new PerformanceType();
    }

    /**
     * Create an instance of {@link DocumentValueType }
     * 
     */
    public DocumentValueType createDocumentValueType() {
        return new DocumentValueType();
    }

    /**
     * Create an instance of {@link ErrorType }
     * 
     */
    public ErrorType createErrorType() {
        return new ErrorType();
    }

    /**
     * Create an instance of {@link MSPImportBudgetType }
     * 
     */
    public MSPImportBudgetType createMSPImportBudgetType() {
        return new MSPImportBudgetType();
    }

    /**
     * Create an instance of {@link ResultMessageType }
     * 
     */
    public ResultMessageType createResultMessageType() {
        return new ResultMessageType();
    }

    /**
     * Create an instance of {@link ObjectResultType }
     * 
     */
    public ObjectResultType createObjectResultType() {
        return new ObjectResultType();
    }

    /**
     * Create an instance of {@link DocumentLinkType }
     * 
     */
    public DocumentLinkType createDocumentLinkType() {
        return new DocumentLinkType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MSPImportBudgetRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ecosys.net/api/MSPImportBudget", name = "MSPImportBudgetRequest")
    public JAXBElement<MSPImportBudgetRequestType> createMSPImportBudgetRequest(MSPImportBudgetRequestType value) {
        return new JAXBElement<MSPImportBudgetRequestType>(_MSPImportBudgetRequest_QNAME, MSPImportBudgetRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MSPImportBudgetResultType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ecosys.net/api/MSPImportBudget", name = "MSPImportBudgetResult")
    public JAXBElement<MSPImportBudgetResultType> createMSPImportBudgetResult(MSPImportBudgetResultType value) {
        return new JAXBElement<MSPImportBudgetResultType>(_MSPImportBudgetResult_QNAME, MSPImportBudgetResultType.class, null, value);
    }

}
