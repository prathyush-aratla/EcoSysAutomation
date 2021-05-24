//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.05.24 at 06:05:58 PM IST 
//


package com.ecosys.putprjwbs;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.ecosys.putprjwbs package. 
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

    private final static QName _MSPPutMppStructureRequest_QNAME = new QName("http://ecosys.net/api/MSPPutMppStructure", "MSPPutMppStructureRequest");
    private final static QName _MSPPutMppStructureResult_QNAME = new QName("http://ecosys.net/api/MSPPutMppStructure", "MSPPutMppStructureResult");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ecosys.putprjwbs
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link MSPPutMppStructureResultType }
     * 
     */
    public MSPPutMppStructureResultType createMSPPutMppStructureResultType() {
        return new MSPPutMppStructureResultType();
    }

    /**
     * Create an instance of {@link MSPPutMppStructureRequestType }
     * 
     */
    public MSPPutMppStructureRequestType createMSPPutMppStructureRequestType() {
        return new MSPPutMppStructureRequestType();
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
     * Create an instance of {@link ResultMessageType }
     * 
     */
    public ResultMessageType createResultMessageType() {
        return new ResultMessageType();
    }

    /**
     * Create an instance of {@link MSPPutMppStructureType }
     * 
     */
    public MSPPutMppStructureType createMSPPutMppStructureType() {
        return new MSPPutMppStructureType();
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
     * Create an instance of {@link JAXBElement }{@code <}{@link MSPPutMppStructureRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ecosys.net/api/MSPPutMppStructure", name = "MSPPutMppStructureRequest")
    public JAXBElement<MSPPutMppStructureRequestType> createMSPPutMppStructureRequest(MSPPutMppStructureRequestType value) {
        return new JAXBElement<MSPPutMppStructureRequestType>(_MSPPutMppStructureRequest_QNAME, MSPPutMppStructureRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MSPPutMppStructureResultType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ecosys.net/api/MSPPutMppStructure", name = "MSPPutMppStructureResult")
    public JAXBElement<MSPPutMppStructureResultType> createMSPPutMppStructureResult(MSPPutMppStructureResultType value) {
        return new JAXBElement<MSPPutMppStructureResultType>(_MSPPutMppStructureResult_QNAME, MSPPutMppStructureResultType.class, null, value);
    }

}
